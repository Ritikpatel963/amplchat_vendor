package com.agromarket.ampl_chat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.agromarket.ampl_chat.models.api.VendorProductResponse;
import com.agromarket.ampl_chat.utils.ApiClient;
import com.agromarket.ampl_chat.utils.ApiService;
import com.agromarket.ampl_chat.utils.SessionManager;
import com.bumptech.glide.Glide;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {

    private int productId;
    private ApiService apiService;
    private SessionManager session;
    private ImageView ivProductImage;
    private TextView tvName, tvBrand, tvPrice, tvStock, tvCategory, tvWeight, tvExpiry;
    private Button btnEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        productId = getIntent().getIntExtra("PRODUCT_ID", -1);
        if (productId == -1) {
            finish();
            return;
        }

        initViews();
        apiService = ApiClient.getClient().create(ApiService.class);
        session = new SessionManager(this);
        loadProductDetails();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        ivProductImage = findViewById(R.id.ivProductDetailImage);
        tvName = findViewById(R.id.tvDetailName);
        tvBrand = findViewById(R.id.tvDetailBrand);
        tvPrice = findViewById(R.id.tvDetailPrice);
        tvStock = findViewById(R.id.tvDetailStock);
        tvCategory = findViewById(R.id.tvDetailCategory);
        tvWeight = findViewById(R.id.tvDetailWeight);
        tvExpiry = findViewById(R.id.tvDetailExpiry);
        btnEdit = findViewById(R.id.btnEditFromDetail);

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, VendorAddProductActivity.class);
            intent.putExtra("EDIT_MODE", true);
            intent.putExtra("PRODUCT_ID", productId);
            startActivity(intent);
        });
    }

    private void loadProductDetails() {
        String token = session.getToken();
        apiService.getVendorProduct("Bearer " + token, productId).enqueue(new Callback<VendorProductResponse>() {
            @Override
            public void onResponse(@NonNull Call<VendorProductResponse> call,
                    @NonNull Response<VendorProductResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayProduct(response.body().product);
                }
            }

            @Override
            public void onFailure(@NonNull Call<VendorProductResponse> call, @NonNull Throwable t) {
                Toast.makeText(ProductDetailActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayProduct(VendorProductResponse.Product product) {
        tvName.setText(product.product_name);
        tvBrand.setText(product.brand_name != null ? product.brand_name : "General");
        tvPrice.setText("₹ " + product.product_rate);
        tvStock.setText(product.quantity + " Units");
        tvWeight.setText(product.unit_size + " " + product.unit_type);
        tvExpiry.setText(product.product_expiry != null ? product.product_expiry : "Not Fixed");
        tvCategory.setText(getCategoryName(product.category_id));

        if (product.images != null && !product.images.isEmpty()) {
            String imagePath = product.images.get(0);
            String fullUrl = "https://uatamplchat.agromarket.co.in/" + imagePath;

            com.bumptech.glide.Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_product_placeholder)
                    .error(R.drawable.ic_product_placeholder)
                    .into(ivProductImage);
        }
    }

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
        return "General";
    }

}
