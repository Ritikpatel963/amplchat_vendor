package com.agromarket.ampl_chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.agromarket.ampl_chat.adapters.VendorProductAdapter;
import com.agromarket.ampl_chat.models.VendorProduct;
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
1. Implement Share Functionality
2. Show product image
3. Implement profile system
 */

public class VendorActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private Button btnAddProduct, btnProductList;
    private RecyclerView productRecycler;
    private VendorProductAdapter productAdapter;
    private List<VendorProduct> productList;
    private SwipeRefreshLayout swipeRefresh;

    // Metric TextViews
    private TextView tvTotalProductValue, tvActiveProductValue, tvNearExpiryValue;

    // Services
    private SessionManager session;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor);

        // Initialize services
        session = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        initViews();
        setupWindowInsets();
        setupToolbar();
        setupNavigationDrawer();
        setupRecyclerView();
        setupSwipeRefresh();

        // Load data from API
        loadMetricsAndProducts();

        btnAddProduct.setOnClickListener(v -> {
            startActivity(new Intent(this, VendorAddProductActivity.class));
        });

        btnProductList.setOnClickListener(v -> {
            startActivity(new Intent(this, VendorProductListActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to activity
        loadMetricsAndProducts();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.mainContent);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        productRecycler = findViewById(R.id.productRecycler);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        btnAddProduct = findViewById(R.id.btnAddProduct);
        btnProductList = findViewById(R.id.btnProductList);

        // Initialize metric cards
        View cardTotalProduct = findViewById(R.id.cardTotalProduct);
        View cardActiveProduct = findViewById(R.id.cardActiveProduct);
        View cardNearExpiry = findViewById(R.id.cardNearExpiry);

        // ad start
        cardNearExpiry.setOnClickListener(v -> {
            Intent intent = new Intent(this, VendorProductListActivity.class);
            intent.putExtra("FILTER_NEAR_EXPIRY", true); // Send the filter flag
            startActivity(intent);
        });
        // ad close
        // adarsh chnage
        View.OnClickListener openProductList = v -> {
            startActivity(new Intent(this, VendorProductListActivity.class));
        };
        cardTotalProduct.setOnClickListener(openProductList);
        cardActiveProduct.setOnClickListener(openProductList);

        //
        tvTotalProductValue = cardTotalProduct.findViewById(R.id.tvMetricValue);
        tvActiveProductValue = cardActiveProduct.findViewById(R.id.tvMetricValue);
        tvNearExpiryValue = cardNearExpiry.findViewById(R.id.tvMetricValue);

        // Set labels for each card
        TextView tvTotalLabel = cardTotalProduct.findViewById(R.id.tvMetricLabel);
        TextView tvActiveLabel = cardActiveProduct.findViewById(R.id.tvMetricLabel);
        TextView tvNearExpiryLabel = cardNearExpiry.findViewById(R.id.tvMetricLabel);

        tvTotalLabel.setText("Total\nProducts");
        tvActiveLabel.setText("Active\nProducts");
        tvNearExpiryLabel.setText("Near\nExpiry");
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContent), (v, insets) -> {
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
            getSupportActionBar().setTitle("Dashboard");
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
                Toast.makeText(this, "Dashboard", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_products) {
                startActivity(new Intent(this, VendorProductListActivity.class));
            } else if (id == R.id.nav_logout) {
                logout();
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void setupRecyclerView() {
        productList = new ArrayList<>();
        productAdapter = new VendorProductAdapter(productList, this::showProductMenu);
        productRecycler.setLayoutManager(new LinearLayoutManager(this));
        productRecycler.setAdapter(productAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadMetricsAndProducts);
        swipeRefresh.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    private void loadMetricsAndProducts() {
        String token = session.getToken();
        if (token == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            goToLogin();
            return;
        }

        apiService.getVendorProductMetrics("Bearer " + token)
                .enqueue(new Callback<VendorProductMetricsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<VendorProductMetricsResponse> call,
                            @NonNull Response<VendorProductMetricsResponse> response) {
                        if (isFinishing())
                            return;

                        swipeRefresh.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            VendorProductMetricsResponse data = response.body();

                            if (data.status) {
                                // Update metrics
                                updateMetrics(data.metrics);

                                // Update recent products
                                updateRecentProducts(data.recent_products);
                            } else {
                                Toast.makeText(VendorActivity.this,
                                        "Failed to load metrics",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(VendorActivity.this,
                                    "Error: " + response.code(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<VendorProductMetricsResponse> call,
                            @NonNull Throwable t) {
                        if (!isFinishing()) {
                            swipeRefresh.setRefreshing(false);
                            Toast.makeText(VendorActivity.this,
                                    "Network error: " + t.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateMetrics(VendorProductMetricsResponse.Metrics metrics) {
        tvTotalProductValue.setText(String.valueOf(metrics.total_products));
        tvActiveProductValue.setText(String.valueOf(metrics.active_products));
        tvNearExpiryValue.setText(String.valueOf(metrics.near_expiry_products));
    }

    private void updateRecentProducts(List<VendorProductMetricsResponse.RecentProduct> recentProducts) {
        productList.clear();

        if (recentProducts != null && !recentProducts.isEmpty()) {
            for (VendorProductMetricsResponse.RecentProduct product : recentProducts) {
                VendorProduct vendorProduct = new VendorProduct(
                        product.product_name,
                        product.brand_name != null ? "Brand: " + product.brand_name : "N/A",
                        product.product_expiry_formatted != null ? "Expiry Date: " + product.product_expiry_formatted
                                : "N/A");
                vendorProduct.setId(product.id);
                vendorProduct.setProductRate(product.product_rate);
                vendorProduct.setQuantity(product.quantity);
                vendorProduct.setImages(product.images);

                productList.add(vendorProduct);
            }
        }

        productAdapter.notifyDataSetChanged();
    }

    private void showProductMenu(View view, int position) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.inflate(R.menu.product_menu);

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_edit) {
                // Navigate to edit activity
                VendorProduct product = productList.get(position);
                Intent intent = new Intent(this, VendorAddProductActivity.class);
                intent.putExtra("PRODUCT_ID", product.getId());
                intent.putExtra("EDIT_MODE", true);
                startActivity(intent);
                return true;
            } else if (id == R.id.menu_delete) {
                deleteProduct(position);
                return true;
            } else if (id == R.id.menu_share) {
                shareProduct(position);
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    private void deleteProduct(int position) {
        VendorProduct product = productList.get(position);
        String token = session.getToken();

        if (token == null) {
            goToLogin();
            return;
        }

        apiService.deleteVendorProduct("Bearer " + token, product.getId())
                .enqueue(new Callback<VendorProductResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<VendorProductResponse> call,
                            @NonNull Response<VendorProductResponse> response) {
                        if (response.isSuccessful()) {
                            productList.remove(position);
                            productAdapter.notifyItemRemoved(position);
                            Toast.makeText(VendorActivity.this,
                                    "Product deleted successfully",
                                    Toast.LENGTH_SHORT).show();
                            loadMetricsAndProducts(); // Refresh metrics
                        } else {
                            Toast.makeText(VendorActivity.this,
                                    "Failed to delete product",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<VendorProductResponse> call,
                            @NonNull Throwable t) {
                        Toast.makeText(VendorActivity.this,
                                "Error: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void shareProduct(int position) {
        VendorProduct product = productList.get(position);
        String shareText = "Check out this product:\n" +
                "Name: " + product.getTitle() + "\n" +
                "Brand: " + product.getBrandName() + "\n" +
                "Price: ₹" + product.getProductRate() + "\n" +
                "Quantity: " + product.getQuantity();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Product"));
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
}