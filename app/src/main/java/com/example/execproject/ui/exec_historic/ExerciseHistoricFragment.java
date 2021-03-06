package com.example.execproject.ui.exec_historic;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.execproject.LatLgnDTO;
import com.example.execproject.R;
import com.example.execproject.databinding.FragmentExcerciseHistoricBinding;
import com.example.execproject.databinding.FragmentExcerciseMonitoringBinding;
import com.example.execproject.ui.configuration.ConfigurationViewModel;
import com.example.execproject.ui.exec_monitoring.ExerciseMonitoringFragment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExerciseHistoricFragment extends Fragment implements OnMapReadyCallback {

    public static ExerciseHistoricFragment newInstance() {
        ExerciseHistoricFragment fragment = new ExerciseHistoricFragment();
        return fragment;
    }

    private FragmentExcerciseHistoricBinding binding;

    private GoogleMap gMap;
    private SupportMapFragment mapFragment;

    private  List<HashMap<String, Double>> tra;

    String speedUnit, speed, mapOrientation, mapType, exerciseType, distance, totalTime;



    //Firebase
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ConfigurationViewModel configViewModel =
                new ViewModelProvider(this).get(ConfigurationViewModel.class);

        binding = FragmentExcerciseHistoricBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mapFragment = (SupportMapFragment) this.getChildFragmentManager().findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);



        return root;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        consultaUltimoRegistro();
    }

    private void consultaUltimoRegistro(){

        firestore.collection("exec_monit")
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> dados = document.getData();
                            System.out.println("DADOS >>>>>>> " + dados);
                            setFieldsInformationsSaved(dados);
                        }
                    } else {
                        Toast.makeText(getContext(), "Erro ao salvar", Toast.LENGTH_LONG).show();
                    }
                }
            });

    }

    private void setFieldsInformationsSaved(Map<String, Object> dadosMap) {

        exerciseType = (String) dadosMap.get("tipoExercicio");
        speedUnit = (String) dadosMap.get("unidadeVelocidade");
        mapOrientation = (String) dadosMap.get("orientacaoMapa");
        mapType = (String) dadosMap.get("tipoMapa");
        speed = (String) dadosMap.get("velocidade");
        distance = (String) dadosMap.get("distancia");
        totalTime = (String) dadosMap.get("tempo");
        Double calorias = (Double) dadosMap.get("calorias");
        tra = (List<HashMap<String, Double>>) dadosMap.get("coordenadas");

        List<LatLng> listLat = new ArrayList<>();

       for(int i = 0; i < tra.size(); i++){
           LatLng ll = new LatLng(tra.get(i).get("lat"), tra.get(i).get("lon"));
           listLat.add(ll);
       }

        System.out.println("LATLONG >>>>>>> " + tra.toString());

        String distanceType = speedUnit.equalsIgnoreCase("km/h") ? " km" : " m";

        binding.inputDistance.setText(distance  + distanceType);
        binding.inputTime.setText(totalTime);
        binding.inputSpeed.setText(speed + " " + speedUnit);
        binding.inputDate.setText(String.valueOf(calorias));

        if("walking".equalsIgnoreCase(exerciseType)){
            binding.imageView4.setBackgroundResource(R.color.red);
            binding.imageView4.setImageResource(R.drawable.ic_walking);
            binding.styleExec.setText(exerciseType);

        } else if ("running".equalsIgnoreCase(exerciseType)){
            binding.imageView4.setBackgroundResource(R.color.green);
            binding.imageView4.setImageResource(R.drawable.ic_running);
            binding.styleExec.setText(exerciseType);
        } else {
            binding.imageView4.setBackgroundResource(R.color.blue);
            binding.imageView4.setImageResource(R.drawable.ic_baseline_directions_bike_24);
            binding.styleExec.setText(exerciseType);
        }

        gMap.addPolyline(new PolylineOptions().addAll(listLat));

        double latmin = tra.get(0).get("lat");
        double latmax = tra.get(0).get("lat");
        double lgnmin = tra.get(0).get("lon");
        double lgnmax = tra.get(0).get("lon");

        for(int i = 1; i < tra.size(); i++ ){
            double lat, lon;
            lat = tra.get(i).get("lat");
            lon = tra.get(i).get("lon");

            latmin = latmin < lat ? latmin : lat;
            latmax = latmax > lat ? latmax : lat;
            lgnmin = lgnmin < lon ? lgnmin : lat;
            lgnmax = lgnmax > lon ? lgnmax : lat;
        }

        LatLng southWest = new LatLng(latmin, lgnmin);
        LatLng northEast = new LatLng(latmax, lgnmax);

        LatLngBounds bound = new LatLngBounds(southWest, northEast);

       //gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bound, 50));
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(tra.get(0).get("lat"), tra.get(tra.size() - 1).get("lon")), 12));


    }
}
