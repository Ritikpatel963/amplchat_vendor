package com.agromarket.ampl_chat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class PendingApprovalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pending_approval);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Show registered contact info
        String email = getIntent().getStringExtra("email");
        String phone = getIntent().getStringExtra("phone");
        TextView tvEmail = findViewById(R.id.tvRegisteredEmail);

        if (tvEmail != null) {
            if (email != null) {
                tvEmail.setText("We'll notify you at: " + email);
            } else if (phone != null) {
                tvEmail.setText("We'll notify you at: " + phone);
            }
        }

        // Back to Login button
        MaterialButton btnBackToLogin = findViewById(R.id.btnBackToLogin);
        btnBackToLogin.setOnClickListener(v -> goToLogin());
    }

    // Override back button — always go to Login, never back to Register
    @Override
    public void onBackPressed() {
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}