package com.example.mapadezonaspeligrosas;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_REQ = 1001;

    private GoogleMap mMap;
    private TileOverlay heatmapOverlay;
    private AuthManager auth;

    private ActivityResultLauncher<Intent> registerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = new AuthManager(this);

        // Launcher para abrir el formulario y refrescar al volver
        registerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        refreshHeatmap();
                        Toast.makeText(MainActivity.this, "Incidente registrado", Toast.LENGTH_SHORT).show();
                    }
                });

        // Cargar mapa
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // FAB Logout (arriba-izquierda en tu layout)
        FloatingActionButton fabLogout = findViewById(R.id.fabLogout);
        fabLogout.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que deseas salir?")
                .setPositiveButton("Sí", (d, w) -> {
                    auth.logout();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finishAffinity();
                })
                .setNegativeButton("Cancelar", null)
                .show());

        // Botón Registrar (barra inferior)
        findViewById(R.id.btnRegistrar).setOnClickListener(v -> {
            Intent i = new Intent(this, RegisterIncidentActivity.class);
            registerLauncher.launch(i);
        });

        // Botón Info (barra inferior)
        findViewById(R.id.btnInfo).setOnClickListener(v ->
                startActivity(new Intent(this, InfoActivity.class)));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Centrar en Yanahuara
        LatLng yanahuara = new LatLng(-16.3876, -71.5446);
        CameraPosition cam = new CameraPosition.Builder()
                .target(yanahuara)
                .zoom(14f)
                .build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cam));

        enableMyLocation();
        refreshHeatmap();
    }

    /** Vuelve a leer RAW + locales y repinta el heatmap */
    private void refreshHeatmap() {
        List<WeightedLatLng> data = new ArrayList<>();
        data.addAll(readIncidentesFromRaw());     // base en res/raw
        data.addAll(readLocalIncidentsCsv());     // registros nuevos
        drawWeightedHeatmap(data);
    }

    // -------- Heatmap --------
    private void drawWeightedHeatmap(List<WeightedLatLng> data) {
        if (data == null || data.isEmpty()) return;

        HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                .weightedData(data)
                .radius(40) // Ajusta 20–50 para más/menos difuminado
                .build();

        if (heatmapOverlay != null) heatmapOverlay.remove();
        heatmapOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
    }

    // -------- Lectores CSV --------
    // RAW: requiere res/raw/incidentes_yanahuara.csv
    private List<WeightedLatLng> readIncidentesFromRaw() {
        List<WeightedLatLng> list = new ArrayList<>();
        try (InputStream is = getResources().openRawResource(R.raw.incidentes_yanahuara);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            String line; boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; } // cabecera
                String[] p = line.split(",");
                if (p.length < 5) continue;

                double lat = Double.parseDouble(p[0].trim());
                double lon = Double.parseDouble(p[1].trim());
                int sev = Integer.parseInt(p[4].trim()); // 1–5
                list.add(new WeightedLatLng(new LatLng(lat, lon), clampWeight(sev)));
            }
        } catch (Exception ignored) { }
        return list;
    }

    // Lee el archivo local (si existe): filesDir/incidentes_local.csv
    private List<WeightedLatLng> readLocalIncidentsCsv() {
        List<WeightedLatLng> list = new ArrayList<>();
        try {
            File f = new File(getFilesDir(), "incidentes_local.csv");
            if (!f.exists()) return list;

            try (InputStream is = new FileInputStream(f);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

                String line; boolean first = true;
                while ((line = br.readLine()) != null) {
                    if (first) { first = false; continue; }
                    String[] p = line.split(",");
                    if (p.length < 5) continue;

                    double lat = Double.parseDouble(p[0].trim());
                    double lon = Double.parseDouble(p[1].trim());
                    int sev = Integer.parseInt(p[4].trim());
                    list.add(new WeightedLatLng(new LatLng(lat, lon), clampWeight(sev)));
                }
            }
        } catch (Exception ignored) { }
        return list;
    }

    private double clampWeight(int sev) {
        return Math.max(1, Math.min(sev, 5)); // limita a 1–5
    }

    // -------- Ubicación --------
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && mMap != null) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQ);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQ && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show();
        }
    }
}



