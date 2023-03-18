package com.ixxc.myuit.Model;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ixxc.myuit.GlobalVars;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Role {
    public String id;
    public String name;
    public String description;
    public boolean composite;
    public boolean assigned;
    public ArrayList<String> compositeRoleIds;

    private static final List<Role> roleList = new ArrayList<>();
    private static final List<Role> compositeRoleList = new ArrayList<>();
    private static List<Role> realmRoleList = new ArrayList<>();

    public static List<Role> getRoleList() {
        return roleList;
    }

    public static void setRoleList(List<Role> roles, boolean isRealm) {
        if (isRealm) {
            realmRoleList.clear();
            realmRoleList = roles;
        } else {
            roleList.clear();
            compositeRoleList.clear();

            for (Role role : roles) {
                if (role.composite) {
                    compositeRoleList.add(role);
                } else {
                    roleList.add(role);
                }
            }
        }
    }

    public static Role getCompositeRoleByName(String roleName) {
        return compositeRoleList.stream().filter(r -> r.name.equals(roleName)).collect(Collectors.toList()).get(0);
    }

    public static List<Role> getCompositeRoleList() {
        return compositeRoleList;
    }

    public static String getNameByID(List<Role> roles, String id){
        for (Role role : roles) {
            if(role.id.equals(id)) { return role.name; }
        }

        return null;
    }

    public static String getIdByDescription(String description){
        for (Role role : Role.getRoleList()) {
            if(role.description.equals(description)){ return role.id; }
        }

        return null;
    }

    public JsonObject toJSON() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("name", name);
        o.addProperty("description", description);
        o.addProperty("composite", composite);
        if (compositeRoleIds != null) {
            JsonArray arr = new JsonArray();
            for (String id : compositeRoleIds) arr.add(id);
            o.add("compositeRoleIds", arr);
        }

        return o;
    }
}
