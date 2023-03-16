package com.ixxc.myuit;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ixxc.myuit.API.APIManager;
import com.ixxc.myuit.Model.Device;
import com.ixxc.myuit.Model.User;

public class HomeFragment extends Fragment {
    TextView tv_username;
    ProgressBar pb_username;

    Handler handler = new Handler(message -> {
        Bundle bundle = message.getData();
        tv_username.setText(bundle.getString("USER"));
        tv_username.setVisibility(View.VISIBLE);
        pb_username.setVisibility(View.GONE);

        return false;
    });

    public HomeFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tv_username = view.findViewById(R.id.tv_username);
        pb_username = view.findViewById(R.id.pb_username);

        new Thread(() -> {
            if (User.getMe() == null) {
                APIManager.getUserInfo();
                APIManager.getUserRoles();
            }
            if (Device.getAllDevices() == null || Device.getAllDevices().size() == 0) {
                String queryString = "{ \"realm\": { \"name\": \"master\" }}";
                JsonParser jsonParser = new JsonParser();
                JsonObject query = (JsonObject)jsonParser.parse(queryString);
                APIManager.queryDevices(query);
            }

            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putString("USER", User.getMe().username);
            msg.setData(bundle);
            handler.sendMessage(msg);
        }).start();

        super.onViewCreated(view, savedInstanceState);
    }
}