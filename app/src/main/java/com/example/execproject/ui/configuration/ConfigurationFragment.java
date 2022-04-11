package com.example.execproject.ui.configuration;

import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.execproject.R;
import com.example.execproject.databinding.FragmentConfigurationBinding;
import com.example.execproject.databinding.FragmentHomeBinding;
import com.example.execproject.ui.home.HomeViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class ConfigurationFragment extends Fragment {

    private FragmentConfigurationBinding binding;

    public static ConfigurationFragment newInstance() {
        return new ConfigurationFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ConfigurationViewModel configViewModel =
                new ViewModelProvider(this).get(ConfigurationViewModel.class);

        binding = FragmentConfigurationBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        initSpinner();

        initMap();

        return root;
    }

    private void initMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapView);

        //Async map
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {

                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
                    @Override
                    public void onMapClick(LatLng latLgn){

                        MarkerOptions markerOptions = new MarkerOptions();
                        //Set position
                        markerOptions.position(latLgn);
                        //Market title
                        markerOptions.title(latLgn.latitude + " : " + latLgn.longitude);
                        //Remove all markers
                        googleMap.clear();
                        //Anmiating zoom
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                latLgn, 10
                        ));
                        //Add Marker on Map
                        googleMap.addMarker(markerOptions);
                    }
                });
            }
        });
    }

    private void initSpinner() {
        //ConfigurationSpinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.config_spinner, android.R.layout.simple_spinner_item);
        Spinner spinner = binding.spinnerConfig;
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((adapter));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}