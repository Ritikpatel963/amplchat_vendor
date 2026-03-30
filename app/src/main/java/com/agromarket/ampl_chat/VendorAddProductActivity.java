package com.agromarket.ampl_chat;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.agromarket.ampl_chat.models.api.VendorProductCreateResponse;
import com.agromarket.ampl_chat.models.api.VendorProductResponse;//ad change 

import com.agromarket.ampl_chat.utils.ApiClient;

import com.agromarket.ampl_chat.utils.ApiService;
import com.agromarket.ampl_chat.utils.SessionManager;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*
TODO:
1. Fetch Categories from API
2. Improve Image upload UI
3. Multi Image uploading issues
4. Add proper validations
 */

public class VendorAddProductActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private LinearLayout imageUploadContainer;
    private ImageView uploadIcon;
    private ImageView productImagePreview;
    private TextInputEditText productNameInput;
    private AutoCompleteTextView productCategoryInput;
    private TextInputEditText brandNameInput;
    private AutoCompleteTextView productWeightInput;
    private TextInputEditText productQtyInput;
    private TextInputEditText productRateInput;
    private TextInputEditText productExpiryInput;
    private Button btnUploadProduct;
    private Button btnDownloadCsv;
    private Button btnAddProduct;

    private List<Uri> selectedImageUris = new ArrayList<>();
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // ad change start

    private boolean isEditMode = false;
    private int productId = -1;

    // ad change close
    private SessionManager session;
    private ApiService apiService;
    private ProgressDialog progressDialog;

    // Category ID mapping (adjust based on your backend)
    private static final int CATEGORY_GRAINS = 1;
    private static final int CATEGORY_VEGETABLES = 2;
    private static final int CATEGORY_FRUITS = 3;
    private static final int CATEGORY_SEEDS = 4;
    private static final int CATEGORY_FERTILIZERS = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_add_product);
        // ad change start
        isEditMode = getIntent().getBooleanExtra("EDIT_MODE", false);
        productId = getIntent().getIntExtra("PRODUCT_ID", -1);

        // ad chnage close
        // Initialize services
        session = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        initViews();
        setupWindowInsets();
        setupToolbar();
        setupNavigationDrawer();
        setupDropdowns();
        setupImagePicker();
        setupClickListeners();

        // ad chnage start
        if (isEditMode && productId != -1) {
            loadProductDetails();
            btnAddProduct.setText("Update Product");
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Product");
            }
        }
        // ad chnage close

    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);

        imageUploadContainer = findViewById(R.id.imageUploadContainer);
        uploadIcon = findViewById(R.id.uploadIcon);
        productImagePreview = findViewById(R.id.productImagePreview);
        productNameInput = findViewById(R.id.productNameInput);
        productCategoryInput = findViewById(R.id.productCategoryInput);
        brandNameInput = findViewById(R.id.brandNameInput);
        productWeightInput = findViewById(R.id.productWeightInput);
        productQtyInput = findViewById(R.id.productQtyInput);
        productRateInput = findViewById(R.id.productRateInput);
        productExpiryInput = findViewById(R.id.productExpiryInput);
        btnUploadProduct = findViewById(R.id.btnUploadProduct);
        btnDownloadCsv = findViewById(R.id.btnDownloadCsv);
        btnAddProduct = findViewById(R.id.btnAddProduct);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Adding product...");
        progressDialog.setCancelable(false);
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            LinearLayout mainLayout = findViewById(R.id.mainLinearLayout);
            mainLayout.setPadding(0, systemBars.top, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Product");
        }
    }

    private void setupNavigationDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, VendorActivity.class));
                finish();
            } else if (id == R.id.nav_products) {
                startActivity(new Intent(this, VendorProductListActivity.class));
                finish();
            } else if (id == R.id.nav_logout) {
                logout();
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void setupDropdowns() {
        // Product Categories
        String[] categories = { "Grains", "Vegetables", "Fruits", "Seeds", "Fertilizers" };
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categories);
        productCategoryInput.setAdapter(categoryAdapter);

        // Product Weights (unit_type and unit_size combined)
        String[] weights = { "1 Kg", "5 Kg", "10 Kg", "25 Kg", "50 Kg", "100 Kg",
                "1 Liter", "5 Liter", "10 Liter", "20 Liter" };
        ArrayAdapter<String> weightAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, weights);
        productWeightInput.setAdapter(weightAdapter);
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            selectedImageUris.clear();
                            selectedImageUris.add(imageUri);

                            productImagePreview.setImageURI(imageUri);
                            productImagePreview.setVisibility(View.VISIBLE);
                            uploadIcon.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void setupClickListeners() {
        imageUploadContainer.setOnClickListener(v -> openImagePicker());
        productExpiryInput.setOnClickListener(v -> showDatePicker());

        btnUploadProduct.setOnClickListener(v -> {
            Toast.makeText(this, "Upload Product feature coming soon", Toast.LENGTH_SHORT).show();
        });

        btnDownloadCsv.setOnClickListener(v -> {
            Toast.makeText(this, "Download CSV feature coming soon", Toast.LENGTH_SHORT).show();
        });

        btnAddProduct.setOnClickListener(v -> validateAndAddProduct());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        imagePickerLauncher.launch(intent);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Format as yyyy-MM-dd for backend
                    String date = String.format(Locale.US, "%04d-%02d-%02d",
                            selectedYear, selectedMonth + 1, selectedDay);
                    productExpiryInput.setText(date);
                },
                year, month, day);

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void validateAndAddProduct() {
        String productName = productNameInput.getText().toString().trim();
        String category = productCategoryInput.getText().toString().trim();
        String brandName = brandNameInput.getText().toString().trim();
        String weight = productWeightInput.getText().toString().trim();
        String qty = productQtyInput.getText().toString().trim();
        String rate = productRateInput.getText().toString().trim();
        String expiry = productExpiryInput.getText().toString().trim();

        // Validation
        if (productName.isEmpty()) {
            productNameInput.setError("Please enter product name");
            productNameInput.requestFocus();
            return;
        }
        if (category.isEmpty()) {
            productCategoryInput.setError("Please select category");
            productCategoryInput.requestFocus();
            return;
        }
        if (weight.isEmpty()) {
            productWeightInput.setError("Please select weight");
            productWeightInput.requestFocus();
            return;
        }
        if (qty.isEmpty()) {
            productQtyInput.setError("Please enter quantity");
            productQtyInput.requestFocus();
            return;
        }
        if (rate.isEmpty()) {
            productRateInput.setError("Please enter rate");
            productRateInput.requestFocus();
            return;
        }

        // Get category ID
        int categoryId = getCategoryId(category);

        // Parse weight to get unit_type and unit_size
        String[] weightParts = parseWeight(weight);
        String unitSize = weightParts[0];
        String unitType = weightParts[1];

        // ad chnage start
        if (isEditMode) {
            updateProductOnServer(productName, categoryId, brandName, unitType, unitSize, rate, qty, expiry);
        } else {
            addProductToServer(productName, categoryId, brandName, unitType, unitSize, rate, qty, expiry);
        }
        // ad change close
    }

    private int getCategoryId(String categoryName) {
        switch (categoryName) {
            case "Grains":
                return CATEGORY_GRAINS;
            case "Vegetables":
                return CATEGORY_VEGETABLES;
            case "Fruits":
                return CATEGORY_FRUITS;
            case "Seeds":
                return CATEGORY_SEEDS;
            case "Fertilizers":
                return CATEGORY_FERTILIZERS;
            default:
                return CATEGORY_GRAINS;
        }
    }

    private String[] parseWeight(String weight) {
        // Example: "5 Kg" -> ["5", "Kg"]
        String[] parts = weight.split(" ");
        if (parts.length == 2) {
            return new String[] { parts[0], parts[1] };
        }
        return new String[] { "1", "Kg" };
    }

    private void addProductToServer(String productName, int categoryId, String brandName,
            String unitType, String unitSize, String rate,
            String qty, String expiry) {
        String token = session.getToken();
        if (token == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            goToLogin();
            return;
        }

        progressDialog.show();

        // Create RequestBody instances
        RequestBody nameBody = RequestBody.create(MediaType.parse("text/plain"), productName);
        RequestBody categoryBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(categoryId));
        RequestBody brandBody = RequestBody.create(MediaType.parse("text/plain"),
                brandName.isEmpty() ? "" : brandName);
        RequestBody unitTypeBody = RequestBody.create(MediaType.parse("text/plain"), unitType);
        RequestBody unitSizeBody = RequestBody.create(MediaType.parse("text/plain"), unitSize);
        RequestBody rateBody = RequestBody.create(MediaType.parse("text/plain"), rate);
        RequestBody qtyBody = RequestBody.create(MediaType.parse("text/plain"), qty);
        RequestBody expiryBody = RequestBody.create(MediaType.parse("text/plain"),
                expiry.isEmpty() ? "" : expiry);

        // Prepare image parts
        MultipartBody.Part[] imageParts = prepareImageParts();

        // Make API call
        apiService.createVendorProduct(
                "Bearer " + token,
                nameBody,
                categoryBody,
                brandBody,
                unitTypeBody,
                unitSizeBody,
                rateBody,
                expiryBody,
                qtyBody,
                imageParts).enqueue(new Callback<VendorProductCreateResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<VendorProductCreateResponse> call,
                            @NonNull Response<VendorProductCreateResponse> response) {
                        progressDialog.dismiss();

                        if (response.isSuccessful() && response.body() != null) {
                            VendorProductCreateResponse data = response.body();

                            if (data.status) {
                                Toast.makeText(VendorAddProductActivity.this,
                                        data.message, Toast.LENGTH_SHORT).show();

                                // Navigate back to dashboard
                                Intent intent = new Intent(VendorAddProductActivity.this, VendorActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(VendorAddProductActivity.this,
                                        "Failed to add product", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(VendorAddProductActivity.this,
                                    "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<VendorProductCreateResponse> call,
                            @NonNull Throwable t) {
                        progressDialog.dismiss();
                        Toast.makeText(VendorAddProductActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ad change start
    private void updateProductOnServer(String productName, int categoryId, String brandName,
            String unitType, String unitSize, String rate,
            String qty, String expiry) {
        String token = session.getToken();
        progressDialog.setMessage("Updating product...");
        progressDialog.show();

        // Create RequestBody instances
        RequestBody methodBody = RequestBody.create(MediaType.parse("text/plain"), "PUT");
        RequestBody nameBody = RequestBody.create(MediaType.parse("text/plain"), productName);
        RequestBody categoryBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(categoryId));
        RequestBody brandBody = RequestBody.create(MediaType.parse("text/plain"), brandName.isEmpty() ? "" : brandName);
        RequestBody unitTypeBody = RequestBody.create(MediaType.parse("text/plain"), unitType);
        RequestBody unitSizeBody = RequestBody.create(MediaType.parse("text/plain"), unitSize);
        RequestBody rateBody = RequestBody.create(MediaType.parse("text/plain"), rate);
        RequestBody qtyBody = RequestBody.create(MediaType.parse("text/plain"), qty);
        RequestBody expiryBody = RequestBody.create(MediaType.parse("text/plain"), expiry.isEmpty() ? "" : expiry);

        // Prepare image parts (optional: only if user selected new images)
        MultipartBody.Part[] imageParts = prepareImageParts();

        // Make API call
        apiService.updateVendorProduct(
                "Bearer " + token,
                productId,
                methodBody,
                nameBody,
                categoryBody,
                brandBody,
                unitTypeBody,
                unitSizeBody,
                rateBody,
                expiryBody,
                qtyBody,
                imageParts).enqueue(new Callback<VendorProductResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<VendorProductResponse> call,
                            @NonNull Response<VendorProductResponse> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(VendorAddProductActivity.this, "Product updated successfully!",
                                    Toast.LENGTH_SHORT).show();
                            finish(); // Close activity and go back
                        } else {
                            Toast.makeText(VendorAddProductActivity.this, "Update failed: " + response.code(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<VendorProductResponse> call, @NonNull Throwable t) {
                        progressDialog.dismiss();
                        Toast.makeText(VendorAddProductActivity.this, "Network error: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ad change close

    private MultipartBody.Part[] prepareImageParts() {
        List<MultipartBody.Part> parts = new ArrayList<>();

        for (Uri imageUri : selectedImageUris) {
            try {
                File file = getFileFromUri(imageUri);
                if (file != null && file.exists()) {
                    RequestBody requestFile = RequestBody.create(
                            MediaType.parse("image/*"), file);
                    MultipartBody.Part part = MultipartBody.Part.createFormData(
                            "images[]", file.getName(), requestFile);
                    parts.add(part);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return parts.toArray(new MultipartBody.Part[0]);
    }

    private File getFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null)
                return null;

            String fileName = getFileName(uri);
            File file = new File(getCacheDir(), fileName);

            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = "image_" + System.currentTimeMillis() + ".jpg";
        }
        return result;
    }

    private void logout() {
        String token = session.getToken();
        if (token == null) {
            goToLogin();
            return;
        }

        apiService.logout("Bearer " + token).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                handleLogout();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                handleLogout();
            }
        });
    }

    private void handleLogout() {
        session.clear();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    // ad change start
    private void loadProductDetails() {
        String token = session.getToken();
        progressDialog.setMessage("Loading product details...");
        progressDialog.show();

        apiService.getVendorProduct("Bearer " + token, productId).enqueue(new Callback<VendorProductResponse>() {
            @Override
            public void onResponse(@NonNull Call<VendorProductResponse> call,
                    @NonNull Response<VendorProductResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    VendorProductResponse.Product product = response.body().product;

                    // Fill the UI fields
                    productNameInput.setText(product.product_name);
                    brandNameInput.setText(product.brand_name);
                    productQtyInput.setText(String.valueOf(product.quantity));
                    productRateInput.setText(String.valueOf(product.product_rate));
                    productExpiryInput.setText(product.product_expiry);

                    // Set Category name in dropdown
                    String categoryName = getCategoryName(product.category_id);
                    productCategoryInput.setText(categoryName, false);

                    // Set Weight/Unit in dropdown
                    String weightTxt = product.unit_size + " " + product.unit_type;
                    productWeightInput.setText(weightTxt, false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<VendorProductResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(VendorAddProductActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper to convert ID back to Name for the dropdown
    private String getCategoryName(int id) {
        if (id == 1)
            return "Grains";
        if (id == 2)
            return "Vegetables";
        if (id == 3)
            return "Fruits";
        if (id == 4)
            return "Seeds";
        if (id == 5)
            return "Fertilizers";
        return "Grains";
    }
    // ad change close
}