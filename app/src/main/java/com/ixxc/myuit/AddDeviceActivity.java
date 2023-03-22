package com.ixxc.myuit;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;
import com.ixxc.myuit.API.APIManager;
import com.ixxc.myuit.Model.Attribute;
import com.ixxc.myuit.Model.CreateAssetReq;
import com.ixxc.myuit.Model.Device;
import com.ixxc.myuit.Model.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AddDeviceActivity extends AppCompatActivity {
    Toolbar toolbar;
    ActionBar actionBar;
    AutoCompleteTextView act_type, act_device, act_parent;
    TextInputLayout til_type;
    TextInputEditText ti_name;
    Button btn_add, btn_add_optional;
    ImageView iv_clear_parent;
    List<String> modelsType, modelsName, parentNames;

    List<Model> models;

    List<Device> parentDevices;

    List<Attribute> selectedOptional;

    ArrayAdapter typeAdapter, devicesAdapter, parentAdapter;

    Handler handler = new Handler(message -> {
        Bundle bundle = message.getData();

        boolean getDevice = bundle.getBoolean("GET_DEV");
        boolean createDevice = bundle.getBoolean("CREATE_DEV");

        if (getDevice) {
            typeAdapter = new ArrayAdapter(this, R.layout.dropdown_item, modelsType);
            act_type.setAdapter(typeAdapter);

            devicesAdapter = new ArrayAdapter(this, R.layout.dropdown_item, modelsName);
            act_device.setAdapter(devicesAdapter);

            parentAdapter = new ArrayAdapter(this, R.layout.dropdown_item, parentNames);
            act_parent.setAdapter(parentAdapter);
        } else if (createDevice) {
            HomeActivity.devicesFrag.refreshDevices();
            finish();
            Toast.makeText(this, "Created!", Toast.LENGTH_SHORT).show();
        }

        return false;
    });;

    String parentId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        InitVars();
        InitViews();
        InitEvents();

        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Add Device");

        new Thread(() -> {
            models = APIManager.getDeviceModels();

            for (Model model : models) {
                String name = model.assetDescriptor.get("name").getAsString();
                modelsName.add(name);
            }

            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putBoolean("GET_DEV", true);
            msg.setData(bundle);
            handler.sendMessage(msg);
        }).start();
    }

    private void InitVars() {
        selectedOptional = new ArrayList<>();
        modelsType = new ArrayList<>();
        modelsName = new ArrayList<>();
        parentNames = new ArrayList<>();

        parentDevices = Device.getAllDevices();

        for (Device d : parentDevices) {
            parentNames.add(d.name);
        }

        modelsType.add("Agent");
        modelsType.add("Asset");
    }

    private void InitViews() {
        act_type = findViewById(R.id.act_type);
        act_device = findViewById(R.id.act_device);
        act_parent = findViewById(R.id.act_parent);
        ti_name = findViewById(R.id.et_device_name);
        til_type = findViewById(R.id.til_type);
        btn_add_optional = findViewById(R.id.btn_add_optional);
        btn_add = findViewById(R.id.btn_add);
        toolbar = findViewById(R.id.action_bar);
        iv_clear_parent = findViewById(R.id.iv_clear_parent_1);
    }

    private void InitEvents() {
        iv_clear_parent.setOnClickListener(view -> {
            act_parent.setText("");
            act_parent.clearFocus();
        });
        act_type.setOnItemClickListener((adapterView, view, i, l) -> {
            String type = modelsType.get(i);
            List<String> newList =  modelsName.stream().filter(name -> name.contains(type)).collect(Collectors.toList());
            devicesAdapter = new ArrayAdapter(AddDeviceActivity.this, R.layout.dropdown_item, newList);
            act_device.setAdapter(devicesAdapter);
        });

        act_device.setOnItemClickListener((adapterView, view, i, l) -> selectedOptional.clear());

        act_parent.setOnItemClickListener((adapterView, view, i, l) -> parentId = parentDevices.get(i).id);

        btn_add_optional.setOnClickListener(view -> {
            List<Model> result = models.stream()
                    .filter(item -> item.assetDescriptor.get("name").getAsString().equals(act_device.getText().toString()))
                    .collect(Collectors.toList());

            if (result.size() == 0) {
                Toast.makeText(AddDeviceActivity.this, "Please select a device first!", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Attribute> optionalAttributes = result.get(0).attributeDescriptors.stream()
                    .filter(item -> item.optional)
                    .collect(Collectors.toList());

            CharSequence[] optionalName = new CharSequence[optionalAttributes.size()];

            for (Attribute a : optionalAttributes) {
                optionalName[optionalAttributes.indexOf(a)] = a.name;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(AddDeviceActivity.this);
            builder.setTitle("Select optional attribute");
            builder.setMultiChoiceItems(optionalName, null, (dialog, i, isChecked) -> {
                if (isChecked) {
                    Toast.makeText(AddDeviceActivity.this, optionalAttributes.get(i).name, Toast.LENGTH_SHORT).show();
                    selectedOptional.add(optionalAttributes.get(i));
                }
            });

            builder.setPositiveButton("Add", (dialogInterface, i) -> Toast.makeText(AddDeviceActivity.this, "OK", Toast.LENGTH_SHORT).show());
            builder.create();
            builder.show();
        });

        btn_add.setOnClickListener(view -> {
            List<Model> result = models.stream()
                    .filter(item -> item.assetDescriptor.get("name").getAsString().equals(act_device.getText().toString()))
                    .collect(Collectors.toList());

            if(result.size() == 0 || String.valueOf(ti_name.getText()).equals("")) {
                Toast.makeText(AddDeviceActivity.this, "Device type and device name fields are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Attribute> requireAttributes = result.get(0).attributeDescriptors.stream()
                    .filter(item -> !item.optional)
                    .collect(Collectors.toList());

            List<Attribute> finalAttributes = Stream.concat(requireAttributes.stream(), selectedOptional.stream())
                    .collect(Collectors.toList());

            JsonObject attributes = new JsonObject();

            for (Attribute a : finalAttributes) {
                String name = a.name;
                String type = a.type;
                JsonObject meta = a.meta;

                JsonObject attribute = new JsonObject();
                attribute.addProperty("name", name);
                attribute.addProperty("type", type);
                if (meta != null) attribute.add("meta", meta);
                attributes.add(name, attribute);
            }

            new Thread(() -> {
                CreateAssetReq req = new CreateAssetReq();
                req.setName(ti_name.getText().toString());
                req.setType(act_device.getText().toString());
                req.setParentId(parentId);
                req.setAttributes(attributes);

                APIManager.createDevice(req.getJsonObj());

                Message msg = handler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putBoolean("CREATE_DEV", true);
                msg.setData(bundle);
                handler.sendMessage(msg);
            }).start();
        });
    }
}