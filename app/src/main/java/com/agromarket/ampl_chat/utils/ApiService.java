package com.agromarket.ampl_chat.utils;

import com.agromarket.ampl_chat.models.api.AgentResponse;
import com.agromarket.ampl_chat.models.api.CallResponse;
import com.agromarket.ampl_chat.models.api.CustomerListResponse;
import com.agromarket.ampl_chat.models.api.LoginRequest;
import com.agromarket.ampl_chat.models.api.LoginResponse;
import com.agromarket.ampl_chat.models.api.MessageListResponse;
import com.agromarket.ampl_chat.models.api.ProductListResponse;
import com.agromarket.ampl_chat.models.api.SendMessageRequest;
import com.agromarket.ampl_chat.models.api.SendMessageResponse;
import com.agromarket.ampl_chat.models.api.SendProductRequest;
import com.agromarket.ampl_chat.models.api.VendorProductCreateResponse;
import com.agromarket.ampl_chat.models.api.VendorProductListResponse;
import com.agromarket.ampl_chat.models.api.VendorProductMetricsResponse;
import com.agromarket.ampl_chat.models.api.VendorProductResponse;
import com.agromarket.ampl_chat.models.api.VendorRegisterResponse;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
        // ad start
        @FormUrlEncoded
        @POST("login/send-otp")
        Call<LoginResponse> sendLoginOtp(@Field("phone_number") String phone);

        @FormUrlEncoded
        @POST("login/verify")
        Call<LoginResponse> verifyLoginOtp(@Field("phone_number") String phone, @Field("otp") String otp);
        // ad close
        // @Multipart
        // @POST("vendor/register")
        // Call<VendorRegisterResponse> registerVendor(

        // -------- Account --------
        // @Part("name") RequestBody name,
        // @Part("email") RequestBody email,
        // @Part("password") RequestBody password,
        // @Part("password_confirmation") RequestBody passwordConfirmation,

        // // -------- Firm --------
        // @Part("firm_name") RequestBody firmName,
        // @Part("gst_number") RequestBody gstNumber,
        // @Part("license_type") RequestBody licenseType,
        // @Part("fertilizer_license_no") RequestBody fertilizerLicense,
        // @Part("seeds_license_no") RequestBody seedsLicense,
        // @Part("pesticides_license_no") RequestBody pesticideLicense,

        // // -------- Contact --------
        // @Part("address") RequestBody address,
        // @Part("phone_number") RequestBody phone,
        // @Part("alternate_number") RequestBody alternatePhone,

        // // -------- Files --------
        // @Part MultipartBody.Part gst_doc,
        // @Part MultipartBody.Part license_doc,
        // @Part MultipartBody.Part aadhar_front_path,
        // @Part MultipartBody.Part aadhar_back_path);
        @Multipart
        @POST("vendor/register")
        Call<VendorRegisterResponse> registerVendor(
                        @Part("otp") RequestBody otp, // 🔥 Added OTP field
                        @Part("name") RequestBody name,
                        @Part("email") RequestBody email,
                        @Part("password") RequestBody password,
                        @Part("password_confirmation") RequestBody passwordConfirmation,
                        @Part("firm_name") RequestBody firmName,
                        @Part("gst_number") RequestBody gstNumber,
                        @Part("license_type") RequestBody licenseType,
                        @Part("fertilizer_license_no") RequestBody fertilizerLicense,
                        @Part("seeds_license_no") RequestBody seedsLicense,
                        @Part("pesticides_license_no") RequestBody pesticideLicense,
                        @Part("address") RequestBody address,
                        @Part("phone_number") RequestBody phone,
                        @Part("alternate_number") RequestBody alternatePhone,
                        @Part MultipartBody.Part gst_doc,
                        @Part MultipartBody.Part license_doc,
                        @Part MultipartBody.Part aadhar_front_path,
                        @Part MultipartBody.Part aadhar_back_path);

        @POST("logout")
        Call<Void> logout(@Header("Authorization") String token);

        @Headers("Accept: application/json")
        @GET("agent/customers")
        Call<CustomerListResponse> getAssignedCustomers(@Header("Authorization") String token);

        @GET("products")
        Call<ProductListResponse> getProducts(
                        @Header("Authorization") String token,
                        @Query("page") int page);

        @GET("messages/{user_id}")
        Call<MessageListResponse> getMessages(
                        @Header("Authorization") String token,
                        @Path("user_id") int customerId);

        @POST("messages/send")
        Call<SendMessageResponse> sendTextMessage(
                        @Header("Authorization") String token,
                        @Body SendMessageRequest body);

        @POST("messages/send-product")
        Call<SendMessageResponse> sendProductMessage(
                        @Header("Authorization") String token,
                        @Body SendProductRequest body);

        @Headers("Accept: application/json")
        @GET("customer/agent")
        Call<AgentResponse> getAssignedAgent(@Header("Authorization") String token);

        @POST("messages/seen/{user_id}")
        Call<Void> markSeen(
                        @Header("Authorization") String token,
                        @Path("user_id") int userId);

        @POST("calls/start")
        Call<CallResponse> startCall(
                        @Header("Authorization") String token,
                        @Body Map<String, Integer> body);

        @POST("calls/{id}/accept")
        Call<CallResponse> acceptCall(
                        @Header("Authorization") String token,
                        @Path("id") int callId);

        @POST("calls/{id}/reject")
        Call<Void> rejectCall(
                        @Header("Authorization") String token,
                        @Path("id") int callId);

        @POST("calls/{id}/end")
        Call<Void> endCall(
                        @Header("Authorization") String token,
                        @Path("id") int callId);

        // ========== VENDOR PRODUCT APIS ==========

        /**
         * Get vendor product metrics (total, active, near expiry) + recent products
         */
        @Headers("Accept: application/json")
        @POST("vendor/metrics")
        Call<VendorProductMetricsResponse> getVendorProductMetrics(
                        @Header("Authorization") String token);

        /**
         * Get all vendor products
         */
        @Headers("Accept: application/json")
        @GET("vendor/products")
        Call<VendorProductListResponse> getVendorProducts(
                        @Header("Authorization") String token,
                        @Query("near_expiry") Integer nearExpiry // ad code
        );

        /**
         * Get single vendor product by ID
         */
        @Headers("Accept: application/json")
        @GET("vendor/products/{id}")
        Call<VendorProductResponse> getVendorProduct(
                        @Header("Authorization") String token,
                        @Path("id") int productId);

        /**
         * Create new vendor product (with images)
         */
        @Multipart
        @POST("vendor/products")
        Call<VendorProductCreateResponse> createVendorProduct(
                        @Header("Authorization") String token,
                        @Part("product_name") RequestBody productName,
                        @Part("category_id") RequestBody categoryId,
                        @Part("brand_name") RequestBody brandName,
                        @Part("unit_type") RequestBody unitType,
                        @Part("unit_size") RequestBody unitSize,
                        @Part("product_rate") RequestBody productRate,
                        @Part("product_expiry") RequestBody productExpiry,
                        @Part("quantity") RequestBody quantity,
                        @Part MultipartBody.Part[] images);

        /**
         * Update vendor product
         */
        // ad change start
        @Multipart
        @POST("vendor/products/{id}")
        Call<VendorProductResponse> updateVendorProduct(
                        @Header("Authorization") String token,
                        @Path("id") int productId,
                        @Part("_method") RequestBody method, // Pass "PUT" here
                        @Part("product_name") RequestBody productName,
                        @Part("category_id") RequestBody categoryId,
                        @Part("brand_name") RequestBody brandName,
                        @Part("unit_type") RequestBody unitType,
                        @Part("unit_size") RequestBody unitSize,
                        @Part("product_rate") RequestBody productRate,
                        @Part("product_expiry") RequestBody productExpiry,
                        @Part("quantity") RequestBody quantity,
                        @Part MultipartBody.Part[] images);
        // ad change close

        /**
         * this mthod
         * Delete vendor product
         */
        @DELETE("vendor/products/{id}")
        Call<VendorProductResponse> deleteVendorProduct(
                        @Header("Authorization") String token,
                        @Path("id") int productId);

        // otp send
        @Multipart
        @POST("vendor/send-otp")
        Call<VendorRegisterResponse> sendOtp(
                        @Part("name") RequestBody name,
                        @Part("email") RequestBody email,
                        @Part("password") RequestBody password,
                        @Part("password_confirmation") RequestBody passwordConfirmation,
                        @Part("firm_name") RequestBody firmName,
                        @Part("gst_number") RequestBody gstNumber,
                        @Part("license_type") RequestBody licenseType,
                        @Part("fertilizer_license_no") RequestBody fertilizerLicense,
                        @Part("seeds_license_no") RequestBody seedsLicense,
                        @Part("pesticides_license_no") RequestBody pesticideLicense,
                        @Part("address") RequestBody address,
                        @Part("phone_number") RequestBody phone,
                        @Part("alternate_number") RequestBody alternatePhone,
                        @Part MultipartBody.Part gst_doc,
                        @Part MultipartBody.Part license_doc,
                        @Part MultipartBody.Part aadhar_front_path,
                        @Part MultipartBody.Part aadhar_back_path);
}