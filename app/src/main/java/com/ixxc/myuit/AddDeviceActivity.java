package com.ixxc.myuit;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;
import com.ixxc.myuit.API.APIManager;
import com.ixxc.myuit.Model.Attribute;
import com.ixxc.myuit.Model.CreateAssetReq;
import com.ixxc.myuit.Model.CreateAssetRes;
import com.ixxc.myuit.Model.Device;
import com.ixxc.myuit.Model.Model;
import com.ixxc.myuit.Model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AddDeviceActivity extends AppCompatActivity {
    AutoCompleteTextView act_type, act_device, act_parent;

    TextInputLayout til_type;

    TextInputEditText ti_name;

    Button btn_optional, btn_add;

    List<String> modelsType, modelsName, parentList;

    List<Model> models;

    List<Device> deviceList;

    ArrayAdapter typeAdapter, devicesAdapter, parentAdapter;

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        InitVars();
        InitViews();
        InitEvents();

        new Thread(() -> {
            models = APIManager.getDeviceModels();
            deviceList = Device.getAllDevices();

            for (Model model : models) {
                String name = model.assetDescriptor.get("name").getAsString();
                modelsName.add(name);
            }

            for (Device d : deviceList) {
                parentList.add(d.name);
            }

            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putBoolean("GET_DEV", true);
            msg.setData(bundle);
            handler.sendMessage(msg);
        }).start();
    }

    private void InitVars() {
        modelsType = new ArrayList<>();
        modelsName = new ArrayList<>();
        parentList = new ArrayList<>();

        modelsType.add("Agent");
        modelsType.add("Asset");

        handler = new Handler(message -> {
            Bundle bundle = message.getData();

            boolean getDevice = bundle.getBoolean("GET_DEV");
            boolean createDevice = bundle.getBoolean("CREATE_DEV");

            if (getDevice) {
                typeAdapter = new ArrayAdapter(this, R.layout.dropdown_item, modelsType);
                act_type.setAdapter(typeAdapter);

                devicesAdapter = new ArrayAdapter(this, R.layout.dropdown_item, modelsName);
                act_device.setAdapter(devicesAdapter);

                parentAdapter = new ArrayAdapter(this, R.layout.dropdown_item, parentList);
                act_parent.setAdapter(parentAdapter);
            } else if (createDevice) {
                finish();
                Toast.makeText(this, "CREATED!", Toast.LENGTH_SHORT).show();
            }

            return false;
        });
    }

    private void InitViews() {
        act_type = findViewById(R.id.act_type);
        act_device = findViewById(R.id.act_device);
        act_parent = findViewById(R.id.act_parent);

        ti_name = findViewById(R.id.ti_name);

        til_type = findViewById(R.id.til_type);

        btn_optional = findViewById(R.id.btn_optional);
        btn_add = findViewById(R.id.btn_add);
    }

    private void InitEvents() {
        act_type.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String type = modelsType.get(i);
                List<String> newList =  modelsName.stream().filter(name -> name.contains(type)).collect(Collectors.toList());
                devicesAdapter = new ArrayAdapter(AddDeviceActivity.this, R.layout.dropdown_item, newList);
                act_device.setAdapter(devicesAdapter);
            }
        });

        btn_optional.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Model result = models.stream()
                        .filter(item -> item.assetDescriptor.get("name").getAsString().equals(act_device.getText().toString()))
                        .collect(Collectors.toList()).get(0);

                List<Attribute> optional = result.attributeDescriptors.stream()
                        .filter(item -> item.optional)
                        .collect(Collectors.toList());

                CharSequence[] optionalName = new CharSequence[optional.size()];

                for (Attribute a :
                        optional) {
                    optionalName[optional.indexOf(a)] = a.name;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(AddDeviceActivity.this);
                builder.setTitle("Select optional attribute");
                builder.setMultiChoiceItems(optionalName, null, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i, boolean isChecked) {
                        if (isChecked) {
                            Toast.makeText(AddDeviceActivity.this, optional.get(i).name, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(AddDeviceActivity.this, "OK", Toast.LENGTH_SHORT).show();
                    }
                });

                builder.create();
                builder.show();
            }
        });

        btn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Model result = models.stream()
                        .filter(item -> item.assetDescriptor.get("name").getAsString().equals(act_device.getText().toString()))
                        .collect(Collectors.toList()).get(0);

                List<Attribute> require = result.attributeDescriptors.stream()
                        .filter(item -> !item.optional)
                        .collect(Collectors.toList());

                JsonObject attributes = new JsonObject();

                for (Attribute a : require) {
                    String name = a.name;
                    String type = a.type;
                    JsonObject meta = a.meta;

                    JsonObject attribute = new JsonObject();
                    attribute.addProperty("name", name);
                    attribute.addProperty("type", type);
                    if (meta != null) {
                        attribute.add("meta", meta);
                    }

                    attributes.add(name, attribute);
                }

                new Thread(() -> {
                    CreateAssetReq req = new CreateAssetReq(ti_name.getText().toString(), act_device.getText().toString(), "master", attributes);
                    APIManager.createDevice(req.getJsonObj());

                    Message msg = handler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("CREATE_DEV", true);
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                }).start();
            }
        });
    }
}