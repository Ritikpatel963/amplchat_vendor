package com.agromarket.ampl_chat;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.agromarket.ampl_chat.models.api.VendorRegisterResponse;
import com.agromarket.ampl_chat.utils.ApiClient;
import com.agromarket.ampl_chat.utils.ApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    // Account
    TextInputEditText etName, etEmail, etPassword, etConfirmPassword;

    // Firm
    TextInputEditText etFirmName, etGstNumber;
    TextInputEditText etFertilizerLicense, etSeedsLicense, etPesticideLicense;

    // Contact
    TextInputEditText etPhone, etAlternatePhone, etAddress;

    // Layouts
    TextInputLayout tilFertilizer, tilSeeds, tilPesticide;

    Spinner spLicenseType;

    // Buttons
    MaterialButton btnGstDoc, btnLicenseDoc, btnAadharFront, btnAadharBack, btnRegister;

    // Status
    TextView tvGstStatus, tvLicenseStatus, tvAadharFrontStatus, tvAadharBackStatus;

    // URIs
    Uri gstUri, licenseUri, aadharFrontUri, aadharBackUri;

    static final int GST_PICK = 1;
    static final int LICENSE_PICK = 2;
    static final int AADHAR_FRONT_PICK = 3;
    static final int AADHAR_BACK_PICK = 4;

    private int currentPickCode;

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final String PHONE_REGEX = "^[6-9][0-9]{9}$";
    private static final String GST_REGEX = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$";

    private static final String[] DOC_MIME_TYPES = {
            "application/pdf",
            "image/jpeg",
            "image/png"
    };

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri == null)
                        return;

                    if (!isValidFile(uri)) {
                        Toast.makeText(this, "Only PDF or Image allowed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    switch (currentPickCode) {
                        case GST_PICK:
                            gstUri = uri;
                            tvGstStatus.setText("File selected");
                            break;
                        case LICENSE_PICK:
                            licenseUri = uri;
                            tvLicenseStatus.setText("File selected");
                            break;
                        case AADHAR_FRONT_PICK:
                            aadharFrontUri = uri;
                            tvAadharFrontStatus.setText("File selected");
                            break;
                        case AADHAR_BACK_PICK:
                            aadharBackUri = uri;
                            tvAadharBackStatus.setText("File selected");
                            break;
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupSpinner();
        setupClicks();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        etFirmName = findViewById(R.id.etFirmName);
        etGstNumber = findViewById(R.id.etGstNumber);

        etFertilizerLicense = findViewById(R.id.etFertilizerLicense);
        etSeedsLicense = findViewById(R.id.etSeedsLicense);
        etPesticideLicense = findViewById(R.id.etPesticideLicense);

        tilFertilizer = (TextInputLayout) etFertilizerLicense.getParent().getParent();
        tilSeeds = (TextInputLayout) etSeedsLicense.getParent().getParent();
        tilPesticide = (TextInputLayout) etPesticideLicense.getParent().getParent();

        etPhone = findViewById(R.id.etPhone);
        etAlternatePhone = findViewById(R.id.etAlternatePhone);
        etAddress = findViewById(R.id.etAddress);

        spLicenseType = findViewById(R.id.spLicenseType);

        btnGstDoc = findViewById(R.id.btnGstDoc);
        btnLicenseDoc = findViewById(R.id.btnLicenseDoc);
        btnAadharFront = findViewById(R.id.btnAadharFront);
        btnAadharBack = findViewById(R.id.btnAadharBack);
        btnRegister = findViewById(R.id.btnRegisterVendor);

        tvGstStatus = findViewById(R.id.tvGstStatus);
        tvLicenseStatus = findViewById(R.id.tvLicenseStatus);
        tvAadharFrontStatus = findViewById(R.id.tvAadharFrontStatus);
        tvAadharBackStatus = findViewById(R.id.tvAadharBackStatus);
    }

    private void setupSpinner() {
        String[] licenses = { "Select License Type", "Fertilizer", "Seeds", "Pesticides" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                licenses);
        spLicenseType.setAdapter(adapter);

        spLicenseType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                tilFertilizer.setVisibility(View.GONE);
                tilSeeds.setVisibility(View.GONE);
                tilPesticide.setVisibility(View.GONE);

                if (position == 1)
                    tilFertilizer.setVisibility(View.VISIBLE);
                else if (position == 2)
                    tilSeeds.setVisibility(View.VISIBLE);
                else if (position == 3)
                    tilPesticide.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void setupClicks() {
        btnGstDoc.setOnClickListener(v -> pickFile(GST_PICK));
        btnLicenseDoc.setOnClickListener(v -> pickFile(LICENSE_PICK));
        btnAadharFront.setOnClickListener(v -> pickFile(AADHAR_FRONT_PICK));
        btnAadharBack.setOnClickListener(v -> pickFile(AADHAR_BACK_PICK));

        // btnRegister.setOnClickListener(v -> {
        // if (validate())
        // registerVendor();
        // });

        // ad start
        btnRegister.setOnClickListener(v -> {
            if (validate())
                sendOtp();
        });

        // ad end
    }

    private void pickFile(int code) {
        currentPickCode = code;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, DOC_MIME_TYPES);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Select file"));
    }

    private boolean validate() {
        if (isEmpty(etName, "Name required"))
            return false;
        if (isEmpty(etEmail, "Email required"))
            return false;
        if (isInvalid(etEmail, EMAIL_REGEX, "Invalid email"))
            return false;

        if (isEmpty(etPassword, "Password required"))
            return false;
        if (etPassword.getText().toString().length() < 6) {
            etPassword.setError("Minimum 6 characters");
            return false;
        }

        if (!etPassword.getText().toString()
                .equals(etConfirmPassword.getText().toString())) {
            etConfirmPassword.setError("Passwords do not match");
            return false;
        }

        if (isEmpty(etFirmName, "Firm name required"))
            return false;
        if (isInvalid(etGstNumber, GST_REGEX, "Invalid GST"))
            return false;
        if (isInvalid(etPhone, PHONE_REGEX, "Invalid phone"))
            return false;
        if (spLicenseType.getSelectedItemPosition() == 0)
            return false;

        if (gstUri == null || licenseUri == null ||
                aadharFrontUri == null || aadharBackUri == null) {
            Toast.makeText(this, "Upload all documents", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean isEmpty(TextInputEditText et, String error) {
        if (et.getText().toString().trim().isEmpty()) {
            et.setError(error);
            return true;
        }
        return false;
    }

    private boolean isInvalid(TextInputEditText et, String regex, String error) {
        if (!et.getText().toString().trim().matches(regex)) {
            et.setError(error);
            return true;
        }
        return false;
    }

    private boolean isValidFile(Uri uri) {
        String type = getContentResolver().getType(uri);
        if (type == null)
            return false;
        for (String mime : DOC_MIME_TYPES) {
            if (mime.equals(type))
                return true;
        }
        return false;
    }

    private MultipartBody.Part fileToPart(String key, Uri uri) {
        ContentResolver resolver = getContentResolver();
        String mimeType = resolver.getType(uri);

        try (InputStream inputStream = resolver.openInputStream(uri)) {
            if (inputStream == null)
                throw new IOException("Unable to open input stream");
            byte[] bytes = readBytes(inputStream);
            RequestBody requestBody = RequestBody.create(bytes, MediaType.parse(mimeType));
            return MultipartBody.Part.createFormData(
                    key,
                    key + "_" + System.currentTimeMillis(),
                    requestBody);
        } catch (Exception e) {
            throw new RuntimeException("File upload failed", e);
        }
    }

    private byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = inputStream.read(data)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private RequestBody toRequestBody(String value) {
        return RequestBody.create(value, MediaType.parse("text/plain"));
    }

    // otp method
    private void sendOtp() {
        btnRegister.setEnabled(false);
        btnRegister.setText("Sending OTP...");

        ApiService api = ApiClient.getClient().create(ApiService.class);

        // 🔥 Add "91" prefix to correctly send via WhatsApp
        String fullPhone = "91" + etPhone.getText().toString().trim();
        String altPhone = etAlternatePhone.getText().toString().trim().isEmpty() ? ""
                : "91" + etAlternatePhone.getText().toString().trim();

        api.sendOtp(
                toRequestBody(etName.getText().toString()),
                toRequestBody(etEmail.getText().toString()),
                toRequestBody(etPassword.getText().toString()),
                toRequestBody(etConfirmPassword.getText().toString()),
                toRequestBody(etFirmName.getText().toString()),
                toRequestBody(etGstNumber.getText().toString()),
                toRequestBody(spLicenseType.getSelectedItem().toString()),
                toRequestBody(etFertilizerLicense.getText().toString()),
                toRequestBody(etSeedsLicense.getText().toString()),
                toRequestBody(etPesticideLicense.getText().toString()),
                toRequestBody(etAddress.getText().toString()),
                toRequestBody(fullPhone), // 🔥 Use fullPhone
                toRequestBody(altPhone), // 🔥 Use altPhone
                fileToPart("gst_doc", gstUri),
                fileToPart("license_doc", licenseUri),
                fileToPart("aadhar_front_path", aadharFrontUri),
                fileToPart("aadhar_back_path", aadharBackUri)).enqueue(new Callback<VendorRegisterResponse>() {
                    @Override
                    public void onResponse(Call<VendorRegisterResponse> call,
                            Response<VendorRegisterResponse> response) {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Register");

                        if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                            showOtpDialog();
                        } else {
                            String msg = response.body() != null ? response.body().getMessage() : "Error sending OTP";
                            Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<VendorRegisterResponse> call, Throwable t) {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Register");
                        Toast.makeText(RegisterActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // oth dialogue box
    private void showOtpDialog() {
        android.widget.EditText etOtp = new android.widget.EditText(this);
        etOtp.setHint("Enter 6-digit OTP");
        etOtp.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etOtp.setPadding(60, 40, 60, 40);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Verify WhatsApp OTP")
                .setMessage("We sent an OTP to: " + etPhone.getText().toString())
                .setView(etOtp)
                .setCancelable(false)
                .setPositiveButton("Verify & Register", (dialog, which) -> {
                    String otp = etOtp.getText().toString().trim();
                    if (otp.length() == 6) {
                        performFinalRegistration(otp); // 🔥 Call the final registration
                    } else {
                        Toast.makeText(this, "Enter 6 digits", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // registration otp
    private void performFinalRegistration(String otp) {
        btnRegister.setEnabled(false);
        btnRegister.setText("Verifying...");

        ApiService api = ApiClient.getClient().create(ApiService.class);

        String fullPhone = "91" + etPhone.getText().toString().trim();
        String altPhone = etAlternatePhone.getText().toString().trim().isEmpty() ? ""
                : "91" + etAlternatePhone.getText().toString().trim();

        api.registerVendor(
                toRequestBody(otp), // 🔥 Pass the OTP here
                toRequestBody(etName.getText().toString()),
                toRequestBody(etEmail.getText().toString()),
                toRequestBody(etPassword.getText().toString()),
                toRequestBody(etConfirmPassword.getText().toString()),
                toRequestBody(etFirmName.getText().toString()),
                toRequestBody(etGstNumber.getText().toString()),
                toRequestBody(spLicenseType.getSelectedItem().toString()),
                toRequestBody(etFertilizerLicense.getText().toString()),
                toRequestBody(etSeedsLicense.getText().toString()),
                toRequestBody(etPesticideLicense.getText().toString()),
                toRequestBody(etAddress.getText().toString()),
                toRequestBody(etPhone.getText().toString()),
                toRequestBody(etAlternatePhone.getText().toString()),
                fileToPart("gst_doc", gstUri),
                fileToPart("license_doc", licenseUri),
                fileToPart("aadhar_front_path", aadharFrontUri),
                fileToPart("aadhar_back_path", aadharBackUri)).enqueue(new Callback<VendorRegisterResponse>() {
                    @Override
                    public void onResponse(Call<VendorRegisterResponse> call,
                            Response<VendorRegisterResponse> response) {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Register");

                        if (response.isSuccessful() && response.body() != null && response.body().isStatus()) {
                            Intent intent = new Intent(RegisterActivity.this, PendingApprovalActivity.class);
                            intent.putExtra("email", etEmail.getText().toString().trim());
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            String msg = response.body() != null ? response.body().getMessage() : "Verification failed";
                            Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<VendorRegisterResponse> call, Throwable t) {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Register");
                    }
                });
    }

    // private void registerVendor() {
    // // ✅ Disable button to prevent double submission
    // btnRegister.setEnabled(false);
    // btnRegister.setText("Submitting...");

    // ApiService api = ApiClient.getClient().create(ApiService.class);

    // Call<VendorRegisterResponse> call = api.registerVendor(
    // toRequestBody(etName.getText().toString()),
    // toRequestBody(etEmail.getText().toString()),
    // toRequestBody(etPassword.getText().toString()),
    // toRequestBody(etConfirmPassword.getText().toString()),
    // toRequestBody(etFirmName.getText().toString()),
    // toRequestBody(etGstNumber.getText().toString()),
    // toRequestBody(spLicenseType.getSelectedItem().toString()),
    // toRequestBody(etFertilizerLicense.getText().toString()),
    // toRequestBody(etSeedsLicense.getText().toString()),
    // toRequestBody(etPesticideLicense.getText().toString()),
    // toRequestBody(etAddress.getText().toString()),
    // toRequestBody(etPhone.getText().toString()),
    // toRequestBody(etAlternatePhone.getText().toString()),
    // fileToPart("gst_doc", gstUri),
    // fileToPart("license_doc", licenseUri),
    // fileToPart("aadhar_front_path", aadharFrontUri),
    // fileToPart("aadhar_back_path", aadharBackUri));

    // call.enqueue(new Callback<VendorRegisterResponse>() {
    // @Override
    // public void onResponse(Call<VendorRegisterResponse> call,
    // Response<VendorRegisterResponse> response) {

    // btnRegister.setEnabled(true);
    // btnRegister.setText("Register");

    // if (response.isSuccessful() && response.body() != null
    // && response.body().isStatus()) {

    // // ✅ Registration successful → go to Pending Approval screen
    // Intent intent = new Intent(RegisterActivity.this,
    // PendingApprovalActivity.class);
    // intent.putExtra("email", etEmail.getText().toString().trim());
    // // Clear back stack — user cannot go back to register form
    // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
    // Intent.FLAG_ACTIVITY_CLEAR_TASK);
    // startActivity(intent);
    // finish();

    // } else {
    // // ❌ Server error
    // String msg = response.body() != null ? response.body().getMessage() :
    // "Registration failed";
    // Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_LONG).show();
    // }
    // }

    // @Override
    // public void onFailure(Call<VendorRegisterResponse> call, Throwable t) {
    // Log.e("RegisterActivity", "API error", t);
    // btnRegister.setEnabled(true);
    // btnRegister.setText("Register");
    // Toast.makeText(RegisterActivity.this, "Network error: " + t.getMessage(),
    // Toast.LENGTH_LONG).show();
    // }
    // });
    // }
}