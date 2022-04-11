package com.example.execproject.ui.profile;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.ui.AppBarConfiguration;

import com.example.execproject.R;
import com.example.execproject.databinding.ActivityMainBinding;
import com.example.execproject.databinding.FragmentConfigurationBinding;
import com.example.execproject.databinding.FragmentHomeBinding;
import com.example.execproject.databinding.FragmentProfileBinding;
import com.example.execproject.ui.home.HomeViewModel;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment implements View.OnClickListener {

    private FragmentProfileBinding binding;
    private AppBarConfiguration mAppBarConfiguration;
    private DatePickerDialog datePickerDialog;
    private Button dateButton;

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

        return root;
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
}
