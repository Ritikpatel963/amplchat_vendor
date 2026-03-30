package com.agromarket.ampl_chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agromarket.ampl_chat.adapters.VendorProductAdapter;
import com.agromarket.ampl_chat.models.VendorProduct;
import com.agromarket.ampl_chat.models.api.VendorProductListResponse;
import com.agromarket.ampl_chat.models.api.VendorProductMetricsResponse;
import com.agromarket.ampl_chat.models.api.VendorProductResponse;
import com.agromarket.ampl_chat.utils.ApiClient;
import com.agromarket.ampl_chat.utils.ApiService;
import com.agromarket.ampl_chat.utils.SessionManager;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*
TODO:
1. Same as Vendor Activity
2. Edit Product System
 */

public class VendorProductListActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private RecyclerView productRecycler;
    private Button btnAddProduct;// ad change
    private boolean isNearExpiryFilter = false; // ad

    private VendorProductAdapter productAdapter;
    private List<VendorProduct> productList = new ArrayList<>();

    private SessionManager session;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_product_list);
        // ad start
        isNearExpiryFilter = getIntent().getBooleanExtra("FILTER_NEAR_EXPIRY", false);
        if (isNearExpiryFilter && getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Near Expiry Products");
        }
        // ad close
        session = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        initViews();
        btnAddProduct.setOnClickListener(v -> {
            startActivity(new Intent(this, VendorAddProductActivity.class));
        });// ad chnage

        setupWindowInsets();
        setupToolbar();
        setupNavigationDrawer();
        setupRecyclerView();

        loadProducts();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        productRecycler = findViewById(R.id.productRecycler);
        btnAddProduct = findViewById(R.id.btnAddProduct);// ad chnage

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
            getSupportActionBar().setTitle("My Products");
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
            if (item.getItemId() == R.id.nav_dashboard) {
                finish();
            } else if (item.getItemId() == R.id.nav_logout) {
                logout();
            }
            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void setupRecyclerView() {
        productAdapter = new VendorProductAdapter(productList, this::showProductMenu);
        productRecycler.setLayoutManager(new LinearLayoutManager(this));
        productRecycler.setAdapter(productAdapter);
    }

    /**
     * 🔥 API CALL – Load vendor products
     */
    private void loadProducts() {
        String token = session.getToken();
        if (token == null) {
            goToLogin();
            return;
        }
        Integer nearExpiryQuery = isNearExpiryFilter ? 1 : null;// ad code
        apiService.getVendorProducts("Bearer " + token, nearExpiryQuery)
                .enqueue(new Callback<VendorProductListResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<VendorProductListResponse> call,
                            @NonNull Response<VendorProductListResponse> response) {

                        if (response.isSuccessful() && response.body() != null && response.body().status) {
                            productList.clear();

                            for (VendorProductListResponse.VendorProduct product : response.body().products) {
                                VendorProduct vendorProduct = new VendorProduct(
                                        product.product_name,
                                        product.brand_name != null ? "Brand: " + product.brand_name : "N/A",
                                        product.product_expiry_formatted != null
                                                ? "Expiry Date: " + product.product_expiry_formatted
                                                : "N/A");
                                vendorProduct.setId(product.id);
                                vendorProduct.setProductRate(product.product_rate);
                                vendorProduct.setQuantity(product.quantity);
                                vendorProduct.setImages(product.images);

                                productList.add(vendorProduct);
                            }

                            productAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(VendorProductListActivity.this,
                                    "Failed to load products",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<VendorProductListResponse> call,
                            @NonNull Throwable t) {
                        Toast.makeText(VendorProductListActivity.this,
                                "Network error: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showProductMenu(View view, int position) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.inflate(R.menu.product_menu);

        popupMenu.setOnMenuItemClickListener(item -> {
            VendorProduct product = productList.get(position);

            if (item.getItemId() == R.id.menu_edit) {
                Intent intent = new Intent(this, VendorAddProductActivity.class);
                intent.putExtra("PRODUCT_ID", product.getId());
                intent.putExtra("EDIT_MODE", true);
                startActivity(intent);
                return true;
            } else if (item.getItemId() == R.id.menu_delete) {
                deleteProduct(product.getId(), position);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    /**
     * ❌ Delete product
     */
    private void deleteProduct(int productId, int position) {
        String token = session.getToken();
        if (token == null) {
            goToLogin();
            return;
        }

        apiService.deleteVendorProduct("Bearer " + token, productId)
                .enqueue(new Callback<VendorProductResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<VendorProductResponse> call,
                            @NonNull Response<VendorProductResponse> response) {
                        if (response.isSuccessful()) {
                            productList.remove(position);
                            productAdapter.notifyItemRemoved(position);
                            Toast.makeText(VendorProductListActivity.this,
                                    "Product deleted",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(VendorProductListActivity.this,
                                    "Delete failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<VendorProductResponse> call,
                            @NonNull Throwable t) {
                        Toast.makeText(VendorProductListActivity.this,
                                "Error: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logout() {
        session.clear();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts(); // Refresh when returning from add/edit
    }
}
