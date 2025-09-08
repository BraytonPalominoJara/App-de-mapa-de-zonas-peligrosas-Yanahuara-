package com.example.mapadezonaspeligrosas;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;

public class RegisterIncidentActivity extends AppCompatActivity {

    private TextInputEditText etTipo, etSev, etFecha, etHora, etLat, etLon, etDesc;
    private ImageView imgPreview;
    private Uri fotoUri = null;

    private FusedLocationProviderClient fused;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> { if (uri != null) { fotoUri = uri; imgPreview.setImageURI(uri); } });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_incident);

        fused = LocationServices.getFusedLocationProviderClient(this);

        etTipo  = findViewById(R.id.etTipo);
        etSev   = findViewById(R.id.etSeveridad);
        etFecha = findViewById(R.id.etFecha);
        etHora  = findViewById(R.id.etHora);
        etLat   = findViewById(R.id.etLat);
        etLon   = findViewById(R.id.etLon);
        etDesc  = findViewById(R.id.etDesc);
        imgPreview = findViewById(R.id.imgPreview);

        // Fecha
        etFecha.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (dp, y, m, d) ->
                    etFecha.setText(String.format("%04d-%02d-%02d", y, m + 1, d)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        // Hora
        etHora.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (tp, h, m) ->
                    etHora.setText(String.format("%02d:%02d", h, m)),
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });

        // Usar mi ubicación
        ((MaterialButton)findViewById(R.id.btnUbicacion)).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Activa el permiso de ubicación", Toast.LENGTH_SHORT).show();
                return;
            }
            fused.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    etLat.setText(String.valueOf(loc.getLatitude()));
                    etLon.setText(String.valueOf(loc.getLongitude()));
                } else {
                    Toast.makeText(this, "No se obtuvo ubicación", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Foto
        ((MaterialButton)findViewById(R.id.btnFoto)).setOnClickListener(v -> pickImage.launch("image/*"));

        // Guardar
        ((MaterialButton)findViewById(R.id.btnGuardar)).setOnClickListener(v -> saveAndFinish());
    }

    private void saveAndFinish() {
        try {
            String tipo = text(etTipo);
            int sev = Integer.parseInt(text(etSev));
            String fecha = text(etFecha);
            String hora  = text(etHora);
            double lat = Double.parseDouble(text(etLat));
            double lon = Double.parseDouble(text(etLon));
            String desc = text(etDesc);
            if (tipo.isEmpty() || fecha.isEmpty() || hora.isEmpty()) {
                Toast.makeText(this, "Completa tipo, fecha y hora", Toast.LENGTH_SHORT).show();
                return;
            }
            if (sev < 1 || sev > 5) { Toast.makeText(this, "Severidad 1–5", Toast.LENGTH_SHORT).show(); return; }

            File f = new File(getFilesDir(), "incidentes_local.csv");
            boolean newFile = !f.exists();
            try (FileWriter fw = new FileWriter(f, true)) {
                if (newFile) fw.write("lat,lon,tipo,fecha,severidad,hora,fotoUri,descripcion\n");
                fw.write(lat + "," + lon + "," + tipo + "," + fecha + "," + sev + "," + hora + "," +
                        (fotoUri != null ? fotoUri.toString() : "") + "," +
                        desc.replace(",", " ") + "\n");
            }
            setResult(RESULT_OK, new Intent());
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Revisa los datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
