package com.agromarket.ampl_chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.agromarket.ampl_chat.models.api.LoginRequest;
import com.agromarket.ampl_chat.models.api.LoginResponse;
import com.agromarket.ampl_chat.utils.ApiClient;
import com.agromarket.ampl_chat.utils.ApiService;
import com.agromarket.ampl_chat.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    // private TextInputEditText emailInput, passwordInput;
    // ad start
    private TextInputEditText phoneInput;
    private Button btnLogin;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        sessionManager = new SessionManager(this);

        // 🔥 Auto-login check
        if (!TextUtils.isEmpty(sessionManager.getToken())) {
            if ("vendor".equals(sessionManager.getUserRole())) {
                openVendorDashboard();
            } else {
                openAgentDashboard();
            }
            return;
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        initViews();
        apiService = ApiClient.getClient().create(ApiService.class);
        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void initViews() {
        // emailInput = findViewById(R.id.text_input_email_edit);
        // passwordInput = findViewById(R.id.text_input_pwd_edit);
        // ad start
        phoneInput = findViewById(R.id.text_input_phone_edit);
        // ad close
        btnLogin = findViewById(R.id.btnLogin);
    }

    private void loginUser() {
        // ad start
        String phone = getText(phoneInput);
        if (phone.length() != 10) {
            toast("Enter valid 10-digit phone");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Sending OTP...");

        apiService.sendLoginOtp("91" + phone).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Send OTP");
                if (response.isSuccessful() && response.body() != null && response.body().status) {
                    showOtpDialog(phone);
                } else {
                    String msg = (response.body() != null) ? response.body().message : "User not found";
                    toast(msg);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Send OTP");
                toast("Network error");
            }
        });
        // ad close

        /*
         * --- OLD LOGIN LOGIC COMMENTED OUT ---
         * String email = getText(emailInput);
         * String password = getText(passwordInput);
         * 
         * if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
         * toast("Please enter email and password");
         * return;
         * }
         * 
         * btnLogin.setEnabled(false);
         * 
         * LoginRequest request = new LoginRequest(email, password);
         * 
         * apiService.login(request).enqueue(new Callback<LoginResponse>() {
         * 
         * @Override
         * public void onResponse(Call<LoginResponse> call, Response<LoginResponse>
         * response) {
         * btnLogin.setEnabled(true);
         * 
         * if (!response.isSuccessful() || response.body() == null) {
         * toast("Invalid server response");
         * return;
         * }
         * 
         * LoginResponse data = response.body();
         * 
         * // ✅ STEP 1 — Check approval_status FIRST (before checking data.status)
         * // Backend returns status=false for pending/rejected vendors
         * // So we must intercept approval_status before the status check below
         * if ("pending".equalsIgnoreCase(data.approval_status)) {
         * // ⏳ Blocked — show pending screen
         * Intent intent = new Intent(LoginActivity.this,
         * PendingApprovalActivity.class);
         * intent.putExtra("email", getText(emailInput));
         * startActivity(intent);
         * return;
         * }
         * 
         * if ("rejected".equalsIgnoreCase(data.approval_status)) {
         * // ❌ Blocked — inform user
         * toast("Your account has been rejected. Please contact support.");
         * return;
         * }
         * 
         * // ✅ STEP 2 — Normal status check for wrong password etc.
         * if (!data.status) {
         * toast(data.message);
         * return;
         * }
         * 
         * // ✅ STEP 3 — Approved vendor or other roles — proceed
         * handleLoginSuccess(data);
         * }
         * 
         * @Override
         * public void onFailure(Call<LoginResponse> call, Throwable t) {
         * btnLogin.setEnabled(true);
         * toast("Network error: " + t.getLocalizedMessage());
         * }
         * });
         */
    }

    private void handleLoginSuccess(LoginResponse data) {
        sessionManager.saveToken(data.token);

        int userId = data.user != null ? data.user.id : 0;
        String role = data.user != null ? data.user.role : "";
        String name = data.user != null ? data.user.name : "";
        int agentId = data.agent_id;

        sessionManager.saveUserId(userId);
        sessionManager.saveUserRole(role);

        if ("customer".equalsIgnoreCase(role)) {
            openCustomerChat(userId, agentId, name);
        } else if ("agent".equalsIgnoreCase(role)) {
            openAgentDashboard();
        } else {
            openVendorDashboard();
        }
    }

    private void openCustomerChat(int customerId, int agentId, String name) {
        Intent intent = new Intent(this, ChatScreenActivity.class);
        intent.putExtra("customer_id", customerId);
        intent.putExtra("agent_id", agentId);
        intent.putExtra("name", name);
        startActivity(intent);
        finish();
    }

    private void openAgentDashboard() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void openVendorDashboard() {
        startActivity(new Intent(this, VendorActivity.class));
        finish();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ad start: New methods for OTP Verification
    private void showOtpDialog(String phone) {
        android.widget.EditText etOtp = new android.widget.EditText(this);
        etOtp.setHint("6-digit OTP");
        etOtp.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etOtp.setPadding(60, 40, 60, 40);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Verify Login")
                .setMessage("OTP sent to: " + phone)
                .setView(etOtp)
                .setCancelable(false)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String otp = etOtp.getText().toString().trim();
                    if (otp.length() == 6) {
                        verifyOtp(phone, otp);
                    } else {
                        toast("Enter 6 digits");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void verifyOtp(String phone, String otp) {
        apiService.verifyLoginOtp("91" + phone, otp).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse data = response.body();

                    if ("pending".equalsIgnoreCase(data.approval_status)) {
                        Intent intent = new Intent(LoginActivity.this, PendingApprovalActivity.class);
                        intent.putExtra("phone", phone);
                        startActivity(intent);
                        return;
                    }

                    if (data.status) {
                        handleLoginSuccess(data);
                    } else {
                        toast(data.message);
                    }
                } else {
                    toast("Invalid OTP or server error");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                toast("Connection error");
            }
        });
    }
    // ad close
}