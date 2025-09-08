package com.example.mapadezonaspeligrosas;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private AuthManager auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = new AuthManager(this);

        TextInputEditText etName   = findViewById(R.id.etName);
        TextInputEditText etEmail  = findViewById(R.id.etEmailReg);
        TextInputEditText etPass   = findViewById(R.id.etPassReg);
        TextInputEditText etPass2  = findViewById(R.id.etPass2Reg);
        MaterialButton btnCreate   = findViewById(R.id.btnCreateAccount);

        btnCreate.setOnClickListener(v -> {
            String name  = text(etName);
            String email = text(etEmail);
            String pass  = text(etPass);
            String pass2 = text(etPass2);

            if (name.length() < 2) {
                toast("Ingresa tu nombre.");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                toast("Correo no válido.");
                return;
            }
            if (pass.length() < 6) {
                toast("La contraseña debe tener al menos 6 caracteres.");
                return;
            }
            if (!pass.equals(pass2)) {
                toast("Las contraseñas no coinciden.");
                return;
            }

            boolean ok = auth.register(name, email, pass);
            if (!ok) {
                toast("El correo ya está registrado o hubo un error.");
                return;
            }

            // Registro exitoso: va directo al mapa
            startActivity(new Intent(this, MainActivity.class));
            finishAffinity(); // limpia el back stack
        });
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
