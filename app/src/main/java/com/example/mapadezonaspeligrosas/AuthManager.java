package com.example.mapadezonaspeligrosas;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthManager {
    private static final String PREF = "auth_prefs";
    private static final String KEY_USERS = "users_json";
    private static final String KEY_SESSION = "session_email";

    private final SharedPreferences sp;

    public AuthManager(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    // Registro simple: guarda {email: {name, passHash}}
    public boolean register(String name, String email, String password) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            return false;
        }
        try {
            JSONObject users = getUsersJson();
            if (users.has(email)) return false; // ya existe
            JSONObject u = new JSONObject();
            u.put("name", name);
            u.put("passHash", sha256(password));
            users.put(email, u);
            sp.edit().putString(KEY_USERS, users.toString()).apply();
            // sesión activa tras registro
            sp.edit().putString(KEY_SESSION, email).apply();
            return true;
        } catch (Exception e) { return false; }
    }

    public boolean login(String email, String password) {
        try {
            JSONObject users = getUsersJson();
            if (!users.has(email)) return false;
            JSONObject u = users.getJSONObject(email);
            String saved = u.optString("passHash", "");
            if (saved.equals(sha256(password))) {
                sp.edit().putString(KEY_SESSION, email).apply();
                return true;
            }
            return false;
        } catch (Exception e) { return false; }
    }

    public void logout() { sp.edit().remove(KEY_SESSION).apply(); }
    public boolean isLoggedIn() { return sp.contains(KEY_SESSION); }
    public String getSessionEmail() { return sp.getString(KEY_SESSION, null); }

    private JSONObject getUsersJson() throws JSONException {
        String s = sp.getString(KEY_USERS, "{}");
        return new JSONObject(s);
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) { return ""; }
    }
}
