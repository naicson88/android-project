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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.execproject.LatLgnDTO;
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
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    long tempoInicial;
    private UiSettings mUiSettings;

    //Botoes
    private Button startBtn, pauseBtn, saveBtn, cleanBtn;
    private boolean started = false;

    //Cronometro
    private long pauseOffset;
    private Chronometer cron;

    String speedUnit, mapOrientation, mapType, exerciseType;
    DecimalFormat df = null;
    List<Map<String, List<LatLgnDTO>>> latLgn = new ArrayList<>();
    List<LatLgnDTO> listLatLgn = new ArrayList<>();
    int count = 0;

    //Firebase
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();


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

        readInformationsSaved();
        readInformationsSavedProfile();
        buscaLocalizacaoAtual();


        saveBtn = binding.saveBtn;
        startBtn = binding.startBtn;
        pauseBtn = binding.pauseBtn;
        cleanBtn = binding.cleanBtn;
        cleanBtn();
        startBtn();
        pauseBtn();
        saveExecMonit();

        tempoInicial = System.currentTimeMillis();

        cron = binding.chronometer;

        return root;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        mUiSettings = gMap.getUiSettings();

        UiSettings mapUI = gMap.getUiSettings();

        mapUI.setCompassEnabled(false);
        mapUI.setRotateGesturesEnabled(false);

        if("satellite".equalsIgnoreCase(mapType))
            gMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        else
            gMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

        mUiSettings.setCompassEnabled(false);

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


        if(count == 2){
            LatLgnDTO dto = new LatLgnDTO();
            dto.setLat(location.getLatitude());
            dto.setLon(location.getLongitude());
            listLatLgn.add(dto);
            count = 0;

        } else {count++;}


        if(gMap != null) {
            if(mapMarker == null){
                mapMarker = gMap.addMarker(new MarkerOptions().position(userPosition));
            } else {
                mapMarker.setPosition(userPosition);
            }

            if("north up".equalsIgnoreCase(mapOrientation) || "none".equalsIgnoreCase(mapOrientation)){
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 17f));

            } else {
                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                gMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition(ll, 22, 25, 0)));
            }
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
        } else {
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

       } else if("km/h".equalsIgnoreCase(speedUnit) && distanciaAcumulada> 0 && started == true){

           distanciaAcumuladaKm = distanciaAcumulada / 1000;
           df =  new DecimalFormat("0.000");
           DecimalFormat sp =  new DecimalFormat("0.0");
           df.setRoundingMode(RoundingMode.HALF_UP);
           double time = distanciaAcumulada / ((SystemClock.elapsedRealtime() - cron.getBase()) /1000);
           time = time * 3.6;
           binding.inputSpeed.setText(sp.format(time));
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
                cleanBtn.setEnabled(false);
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
                cleanBtn.setEnabled(true);
                started = false;
            }
        });
    }

    private void saveExecMonit(){

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Map<String, Object> exercicio = new HashMap<>();
                exercicio.put("distancia", binding.inputDistance.getText().toString());
                exercicio.put("tempo", cron.getText().toString());
                exercicio.put("velocidade", binding.inputSpeed.getText().toString());
                exercicio.put("tipoExercicio", exerciseType);
                exercicio.put("tipoMapa", mapType);
                exercicio.put("orientacaoMapa", mapOrientation);
                exercicio.put("unidadeVelocidade", speedUnit);
                exercicio.put("calorias",  readInformationsSavedProfile());
                exercicio.put("coordenadas", listLatLgn);

                firestore.collection("exec_monit").add(exercicio).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Toast.makeText(getContext(), documentReference.getId(), Toast.LENGTH_LONG).show();
                       String manter = documentReference.getId();
                        deletaTodosAntesDeSalvar(manter);
                    }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getContext(), "Erro ao salvar", Toast.LENGTH_LONG).show();
                            System.out.println("Erro ao salvar: " + e.getMessage());
                        }
                    });


                binding.inputDistance.setText("");
                distanciaAcumulada = 0;
                distanciaAcumuladaKm = 0;
                cron.getText();
                binding.inputSpeed.setText("");
                cron.setBase(SystemClock.elapsedRealtime());
                pauseOffset = 0;
                started = false;
                cleanBtn.setEnabled(false);

            }
        });
    }

        private void deletaTodosAntesDeSalvar(String manter){

            firestore.collection("exec_monit")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {

                                    if(!document.getId().equalsIgnoreCase(manter)){
                                        firestore.collection("exec_monit").document(document.getId())
                                                .delete()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        System.out.println("Excluido");
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        System.out.println("Não excluido");
                                                    }
                                                });
                                    }
                                }
                            } else {
                                Toast.makeText(getContext(), "Erro ao salvar", Toast.LENGTH_LONG).show();
                            }
                        }
                    });


        }
        private void cleanBtn(){

            cleanBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    binding.inputDistance.setText("");
                    distanciaAcumulada = 0;
                    distanciaAcumuladaKm = 0;
                    cron.getText();
                    binding.inputSpeed.setText("");
                    cron.setBase(SystemClock.elapsedRealtime());
                    pauseOffset = 0;
                    started = false;

                }
            });
        }

        @Override
        public  void onDestroy() {
            super.onDestroy();
            if(fusedLocation != null){
                fusedLocation.removeLocationUpdates(locationCallback);

            }
        }

        private String curDate() {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date();
            String strDate = formatter.format(date);

            return strDate;
        }

        private Double calculaGastoCalorias(String infos){
            String[]  informations = infos.split(";");

            String peso = informations[2];

            double vel = 0;

            if("m/s".equalsIgnoreCase(speedUnit)){
                double ms = Double.parseDouble(binding.inputSpeed.getText().toString());
                vel = ms * 3.6;
            } else {
                vel = Double.parseDouble(binding.inputSpeed.getText().toString());
            }
            double cal = 0;

            if("walking".equalsIgnoreCase(exerciseType)){
               cal = 0.0140;
            } else if ("running".equalsIgnoreCase(exerciseType)){
                cal = 0.0175;
            } else {
                cal = 0.0199;
            }

            cal = (Double.parseDouble(peso) * vel) * cal;
            cal = round(cal, 2);

            String[] minSec = cron.getText().toString().split(":");
            String min = minSec[0];
            String sec = minSec[1];
            double totalCal = 0;

            if(!"00".equals(min)){
                totalCal = cal * Double.parseDouble(min);
            } else if(!"00".equals(sec)){
                totalCal += cal * (Double.parseDouble(sec) / 60);
            }

            return round(totalCal, 2);

        }

    public Double readInformationsSavedProfile() {
        Double totalCal = 0.0;

        try{

            FileInputStream fileInputStream = getActivity().openFileInput("Profile File.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

            BufferedReader bufferedReader = new BufferedReader((inputStreamReader));
            StringBuffer stringBuffer = new StringBuffer();

            String line;
            while((line = bufferedReader.readLine()) != null){
                stringBuffer.append(line + "\n");
               totalCal = calculaGastoCalorias(line);
                Toast.makeText(getContext(), line, Toast.LENGTH_LONG).show();
            }

        }catch(FileNotFoundException e){
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }

        return totalCal;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
