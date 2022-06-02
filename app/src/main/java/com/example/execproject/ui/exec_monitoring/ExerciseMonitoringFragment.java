package com.example.execproject.ui.exec_monitoring;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.execproject.R;
import com.example.execproject.databinding.FragmentExcerciseMonitoringBinding;
import com.example.execproject.ui.configuration.ConfigurationViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Duration;


public class ExerciseMonitoringFragment extends Fragment implements OnMapReadyCallback {

    private FragmentExcerciseMonitoringBinding binding;
    private GoogleMap gMap;
    private SupportMapFragment mapFragment;

    //GPS Atributos
    private static final int REQUEST_LOCATION_UPDATES = 2;
    private FusedLocationProviderClient fusedLocation;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    //Atributos do Map
    private Marker mapMarker;
    private Location localizacaoAtual, ultimaLocalizacao;
    private boolean firstFix = true;
    private boolean cronometro = false;
    private double distanciaAcumulada, distanciaAcumuladaKm;
    long tempoInicial , tempoAtual, tempoTranscorrido;

    //Botoes
    private Button startBtn, pauseBtn, saveBtn;
    private boolean started = false;

    //Cronometro
    private long pauseOffset;
    private Chronometer cron;

    String speedUnit, mapOrientation, mapType, exerciseType;
    DecimalFormat df = null;

    public static ExerciseMonitoringFragment newInstance() {
        ExerciseMonitoringFragment fragment = new ExerciseMonitoringFragment();
        return fragment;
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
     //   View view = inflater.inflate(R.layout.fragment_excercise_monitoring, null, false);
        ConfigurationViewModel configViewModel =
                new ViewModelProvider(this).get(ConfigurationViewModel.class);

        binding = FragmentExcerciseMonitoringBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mapFragment = (SupportMapFragment) this.getChildFragmentManager().findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);
        buscaLocalizacaoAtual();

        saveBtn = binding.saveBtn;
        startBtn = binding.startBtn;
        pauseBtn = binding.pauseBtn;
        startBtn();
        pauseBtn();
        saveExecMonit();
        tempoInicial = System.currentTimeMillis();

        cron = binding.chronometer;

        readInformationsSaved();
        return root;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        LatLng sydney = new LatLng(-34, 151);
        gMap.addMarker(new MarkerOptions().position(sydney).title("Market in Sydney"));
        gMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

    }

    private void buscaLocalizacaoAtual() {

        if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            fusedLocation = LocationServices.getFusedLocationProviderClient(getContext());
            locationRequest = LocationRequest.create();

            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5*1000);
            locationRequest.setFastestInterval(1*1000);

            locationCallback = new LocationCallback(){
                @Override
                public void onLocationResult(LocationResult locationResult){
                    super.onLocationResult(locationResult);
                    Location location = locationResult.getLastLocation();
                    atualizaPosicaoNoMapa(location);

                }
            };

            fusedLocation.requestLocationUpdates(locationRequest, locationCallback, null );

        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_UPDATES);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_LOCATION_UPDATES){
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                buscaLocalizacaoAtual();
            }
        } else {
            Toast.makeText( getContext(), "Sem permissão para mostrar atualização de localização", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public  void onDestroy() {
        super.onDestroy();
        if(fusedLocation != null){
            fusedLocation.removeLocationUpdates(locationCallback);
        }
    }

    public void atualizaPosicaoNoMapa(Location location) {
        if(firstFix){
            firstFix = false;
            localizacaoAtual = ultimaLocalizacao = location;
            distanciaAcumulada = 0;

        } else {

            ultimaLocalizacao = localizacaoAtual;
            localizacaoAtual = location;
            distanciaAcumulada += localizacaoAtual.distanceTo(ultimaLocalizacao);
        }

        setDistanciaTempoEVelocidade();
        LatLng userPosition = new LatLng( location.getLatitude(), location.getLongitude());

        if(gMap != null) {
            if(mapMarker == null){
                mapMarker = gMap.addMarker(new MarkerOptions().position(userPosition));
            } else {
                mapMarker.setPosition(userPosition);
            }

            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 17f));

        }
    }

    public void readInformationsSaved(){

        try{

            FileInputStream fileInputStream = getActivity().openFileInput("Monitoring File.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

            BufferedReader bufferedReader = new BufferedReader((inputStreamReader));
            StringBuffer stringBuffer = new StringBuffer();

            String line;
            while((line = bufferedReader.readLine()) != null){
                stringBuffer.append(line + "\n");
                setFieldsInformationsSaved(line);
                Toast.makeText(getContext(), line, Toast.LENGTH_LONG).show();
            }

        }catch(FileNotFoundException e){
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private void setFieldsInformationsSaved(String infos) {
        String[]  informations = infos.split(";");

        exerciseType = informations[0];
        speedUnit = informations[1];
        mapOrientation = informations[2];
        mapType = informations[3];

        binding.speedUnit.setText(speedUnit);
        binding.styleExec.setText(exerciseType);
        String unt = "km/h".equalsIgnoreCase(speedUnit) ? "(Km)" : "(m)";
        binding.dist.setText(unt);

        if("walking".equalsIgnoreCase(exerciseType)){
            binding.imageView4.setBackgroundResource(R.color.red);
            binding.imageView4.setImageResource(R.drawable.ic_walking);

        } else if ("running".equalsIgnoreCase(exerciseType)){
            binding.imageView4.setBackgroundResource(R.color.green);
            binding.imageView4.setImageResource(R.drawable.ic_running);
        }else {
            binding.imageView4.setBackgroundResource(R.color.blue);
            binding.imageView4.setImageResource(R.drawable.ic_baseline_directions_bike_24);
        }

    }

   private void setDistanciaTempoEVelocidade(){

       if("m/s".equalsIgnoreCase(speedUnit) && distanciaAcumulada > 0 && started == true){
           cronometro = true;
           df =  new DecimalFormat("0.00");
           df.setRoundingMode(RoundingMode.HALF_UP);
           binding.inputDistance.setText(df.format(distanciaAcumulada));
           double time = distanciaAcumulada / ((SystemClock.elapsedRealtime() - cron.getBase()) /1000);
           binding.inputSpeed.setText(df.format(time));

       } else if(distanciaAcumuladaKm > 0 && started == true){
           distanciaAcumuladaKm = distanciaAcumulada / 1000;
           df =  new DecimalFormat("0.000");
           df.setRoundingMode(RoundingMode.HALF_UP);

           binding.inputDistance.setText(df.format(distanciaAcumuladaKm));
       }
    }

    private void startBtn(){

        startBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                cron.setBase(SystemClock.elapsedRealtime() - pauseOffset);
                cron.start();
                Toast.makeText(getContext(), "Started", Toast.LENGTH_SHORT).show();
                pauseBtn.setEnabled(true);
                saveBtn.setEnabled(false);
                startBtn.setEnabled(false);
                started = true;
            }
        });
    }

    private void pauseBtn(){

        pauseBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                cron.stop();
               pauseOffset = SystemClock.elapsedRealtime() - cron.getBase();
                Toast.makeText(getContext(), "Paused", Toast.LENGTH_SHORT).show();
                saveBtn.setEnabled(true);
                startBtn.setEnabled(true);
                started = false;
            }
        });
    }

    private void saveExecMonit(){

        saveBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                binding.inputDistance.setText("");
                distanciaAcumulada = 0;
                distanciaAcumuladaKm = 0;
                cron.getText();
                binding.inputSpeed.setText("");
                Toast.makeText(getContext(), "Saved", Toast.LENGTH_SHORT).show();
                cron.setBase(SystemClock.elapsedRealtime());
                pauseOffset = 0;
                started = false;

            }
        });
    }


}
