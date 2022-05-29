package com.example.execproject.ui.configuration;

import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.execproject.R;
import com.example.execproject.databinding.FragmentConfigurationBinding;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConfigurationFragment extends Fragment {

    private FragmentConfigurationBinding binding;
    private View viewAux;
    private Button submitButton;
    private RadioGroup radioGroupExercises, radioGroupMapType;
    private RadioButton radioButtonExercises, radioButtonMapType;
    private Spinner speedUnitSpinner, mapOrientationSpinner;

    int exerciseRadioId, mapTypeId;
    long speedUnitId,mapOrientationId;

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

        radioGroupExercises = binding.radioGroupExercises;

        initSpinner();
        initSpinnerMapOrientation();

        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        viewAux = getView();

        submitButton = (Button) viewAux.findViewById(R.id.configSubmitButton);

       onRadioButtonExercisesClicked();
        onRadioButtonMapTypeClicked();
        initSubmitButton();
        readInformationsSaved();

    }

    private void initSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.config_spinner, android.R.layout.simple_spinner_item);
        Spinner spinner = binding.speedUnitSpinner;
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter((adapter));
    }

    private void initSpinnerMapOrientation(){
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.mapOrientation_spinner, android.R.layout.simple_spinner_item);
        Spinner spinner = binding.mapOrientationSpinner;
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    public void onRadioButtonExercisesClicked (){

        radioGroupExercises = binding.radioGroupExercises;
        radioGroupExercises.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                radioButtonExercises = viewAux.findViewById(checkedId);
//
            }
        });
    }

    public void onRadioButtonMapTypeClicked (){

        radioGroupMapType = binding.radioGroupMapType;
        radioGroupMapType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                radioButtonMapType= viewAux.findViewById(checkedId);
//
            }
        });
    }

    private void initSubmitButton(){

        int selectedId = radioGroupExercises.getCheckedRadioButtonId();
        radioButtonExercises =  (RadioButton) viewAux.findViewById(selectedId);
        int selectedIdMapType = radioGroupMapType.getCheckedRadioButtonId();
        radioButtonMapType = (RadioButton) viewAux.findViewById((selectedIdMapType));


        submitButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v){
                speedUnitSpinner =  (Spinner) viewAux.findViewById(R.id.speedUnitSpinner);
                mapOrientationSpinner = (Spinner) viewAux.findViewById(R.id.mapOrientationSpinner);

                exerciseRadioId = radioButtonExercises.getId();
                speedUnitId = speedUnitSpinner.getSelectedItemId();
                mapOrientationId = mapOrientationSpinner.getSelectedItemId();
                mapTypeId = radioButtonMapType.getId();

                 saveInformations( exerciseRadioId, mapTypeId, speedUnitId, mapOrientationId);

            }
        });
    }

    public void saveInformations(int  exerciseRadioId, int  mapTypeId, long speedUnitId, long mapOrientationId){
        try {

            String attributesToBeSaved =  exerciseRadioId +";"+ speedUnitId +";"+ mapOrientationId + ";" + mapTypeId;

            FileOutputStream fileOutputStream = getActivity().openFileOutput("Configuration File.txt", Context.MODE_PRIVATE);
            fileOutputStream.write(attributesToBeSaved.getBytes());

            fileOutputStream.close();

            Toast.makeText(getContext(), R.string.savedMessage, Toast.LENGTH_SHORT).show();

        }catch(FileNotFoundException e){
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void readInformationsSaved(){
        try{

            FileInputStream fileInputStream = getActivity().openFileInput("Configuration File.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

            BufferedReader bufferedReader = new BufferedReader((inputStreamReader));
            StringBuffer stringBuffer = new StringBuffer();

            String line;
            while((line = bufferedReader.readLine()) != null){
                stringBuffer.append(line + "\n");
                setFieldsInformationsSaved(line);
            }


        }catch(FileNotFoundException e){
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void setFieldsInformationsSaved(String informations) {

        if (informations == null || informations.length() == 0 || informations.isEmpty())
            return;

        String[] configInformations = informations.split(";");

        radioButtonExercises = (RadioButton) viewAux.findViewById(Integer.parseInt(configInformations[0]));
        radioButtonExercises.setChecked(true);

        binding.speedUnitSpinner.setSelection(Integer.parseInt(configInformations[1]));
        binding.mapOrientationSpinner.setSelection(Integer.parseInt(configInformations[2]));

        radioButtonMapType = (RadioButton) viewAux.findViewById(Integer.parseInt(configInformations[3]));
        radioButtonMapType.setChecked(true);

    }
}