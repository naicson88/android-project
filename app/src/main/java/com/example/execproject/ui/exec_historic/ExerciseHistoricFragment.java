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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
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
        List<LatLgnDTO> dto = (List<LatLgnDTO>) dadosMap.get("coordenadas");

        System.out.println("LATLONG >>>>>>> " + dto.toString());

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

    }
}
