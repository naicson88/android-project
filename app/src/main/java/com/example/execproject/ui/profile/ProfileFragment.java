package com.example.execproject.ui.profile;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.ui.AppBarConfiguration;

import com.example.execproject.R;
import com.example.execproject.databinding.ActivityMainBinding;
import com.example.execproject.databinding.FragmentConfigurationBinding;
import com.example.execproject.databinding.FragmentHomeBinding;
import com.example.execproject.databinding.FragmentProfileBinding;
import com.example.execproject.ui.home.HomeViewModel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment implements View.OnClickListener {

    private FragmentProfileBinding binding;
    private DatePickerDialog datePickerDialog;
    private Button dateButton, submitButton;
    private RadioGroup radioGroup;
    private RadioButton radioButton;
    private View viewAux;

    EditText userNameInput, userHeightInput, userWeightInput;

    String  userName, userHeight, userWeight, userGender, userBirthDate;

    public ProfileFragment(){

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ProfileViewModel homeViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);
        initDatePicker();

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        dateButton = binding.datePickerButton;
        dateButton.setOnClickListener(this);

        radioGroup = binding.radioGroupProfile;

        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        viewAux = getView();
        onRadioButtonClicked();

        userNameInput = (EditText) view.findViewById(R.id.names);
        userHeightInput = (EditText) view.findViewById(R.id.inputHeight);
        userWeightInput = (EditText) view.findViewById(R.id.inputWeight);
        int selectedId = radioGroup.getCheckedRadioButtonId();
        radioButton =  (RadioButton) view.findViewById(selectedId);
        dateButton =  (Button) view.findViewById(R.id.datePickerButton);
        submitButton = (Button) viewAux.findViewById(R.id.submitButtonProfile);

        initSubmitButton();

        readInformationsSaved();

    }

    private void initSubmitButton(){

        submitButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                userName = userNameInput.getText().toString();
                userHeight = userHeightInput.getText().toString();
                userWeight = userWeightInput.getText().toString();
                userGender = radioButton.getText().toString();
                userBirthDate = dateButton.getText().toString();

                //Toast.makeText(getContext(), userName + " " + userHeight + " " + userWeight + " " + userGender +" " + userBirthDate, Toast.LENGTH_SHORT).show();

               Boolean isFormValid =  isValidProfileInformations(userName,  userHeight,  userWeight,  userGender,  userBirthDate);

               if(!isFormValid)
                   return;

                saveInformations(userName,  userHeight,  userWeight,  userGender,  userBirthDate);

            }
        });
    }

    private Boolean isValidProfileInformations(String userName, String userHeight, String userWeight, String userGender, String userBirthDate){

           try{

               Float heightFloat = Float.valueOf(userHeight);
               Float weightFloat =  Float.valueOf(userWeight);

               if(userName == null || userName.isEmpty() || userName.length() < 2){
                   Toast.makeText(getContext(), R.string.errorUserName, Toast.LENGTH_SHORT).show();
                   return false;
               }
               if(heightFloat < 0.60 || heightFloat > 3.0){
                   Toast.makeText(getContext(), R.string.errorUserHeight, Toast.LENGTH_SHORT).show();
                   return false;
               }

               if(weightFloat < 20.0 || weightFloat > 500){
                   Toast.makeText(getContext(), R.string.errorUserWeight, Toast.LENGTH_SHORT).show();
                   return false;
               }
               if(userGender == null || userGender.isEmpty()){
                   Toast.makeText(getContext(), R.string.errorGender, Toast.LENGTH_SHORT).show();
                   return false;
               }

           } catch(Exception e){
               e.printStackTrace();
               Toast.makeText(getContext(), R.string.errorProfileExceptionValidations, Toast.LENGTH_SHORT).show();
           }

           return true;
    }

    public void saveInformations(String userName, String userHeight, String userWeight, String userGender, String userBirthDate){
        try {

            FileOutputStream fileOutputStream = getActivity().openFileOutput("Profile File.txt", Context.MODE_PRIVATE);
            fileOutputStream.write(userName.getBytes());
            fileOutputStream.write(userHeight.getBytes());
            fileOutputStream.write(userWeight.getBytes());
            fileOutputStream.write(userGender.getBytes());
            fileOutputStream.write(userBirthDate.getBytes());

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

            FileInputStream fileInputStream = getActivity().openFileInput("Profile File.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);

            BufferedReader bufferedReader = new BufferedReader((inputStreamReader));
            StringBuffer stringBuffer = new StringBuffer();

            String line;
            while((line = bufferedReader.readLine()) != null){
                stringBuffer.append(line + "\n");
            }
            Toast.makeText(getContext(), stringBuffer.toString(), Toast.LENGTH_SHORT).show();


        }catch(FileNotFoundException e){
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private void initDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month = month + 1;
                String date = makeDateString(day, month, year);

                dateButton.setText(date);
            }
        };

        int styleDialog = AlertDialog.THEME_HOLO_LIGHT;

        datePickerDialog = new DatePickerDialog(getContext(), styleDialog, dateSetListener, 1990,1,1);
    }

    private String makeDateString(int day, int month, int year) {
        return getMonthFormat(month) + " " + day + " " + year;
    }

    private String getMonthFormat(int month){
        Map<Integer, String> calendarMap = new HashMap<Integer, String>(){{
            put(1, "JAN");put(2, "FEV");put(3, "MAR");put(4, "APR");put(5, "MAY");put(6, "JUN");
            put(1, "JUL");put(2, "AUG");put(3, "SEP");put(4, "OCT");put(5, "NOV");put(6, "DEC");
        }};

        String monthString = calendarMap.get(month) != null ? calendarMap.get(month) : "JAN";

        return monthString;
    }

    public void openDatePicker(View view){
        datePickerDialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.datePickerButton:
                this.openDatePicker(view);

                break;
        }
    }


    public void onRadioButtonClicked (){

        radioGroup = binding.radioGroupProfile;
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                radioButton = viewAux.findViewById(checkedId);
                String resp = radioButton.getText().toString();
                Toast.makeText(getContext(), resp, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
