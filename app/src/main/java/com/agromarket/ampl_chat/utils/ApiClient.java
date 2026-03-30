package com.agromarket.ampl_chat.utils;

import java.util.concurrent.TimeUnit;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "https://uatamplchat.agromarket.co.in/api/";
    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getUnsafeOkHttpClient()) // ✅ bypasses SSL for dev
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    /**
     * ⚠️ FOR DEVELOPMENT ONLY — bypasses SSL certificate validation.
     * Before releasing to production, replace this with a proper SSL certificate
     * on your server and remove this method.
     */
    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Trust all certificates
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[] {};
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(),
                            (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true) // bypass hostname check
                    .connectTimeout(120, TimeUnit.SECONDS) // Max time to connect to server
                    .writeTimeout(120, TimeUnit.SECONDS) // Max time to upload files
                    .readTimeout(120, TimeUnit.SECONDS) // Max time to wait for response
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("SSL bypass failed", e);
        }
    }
}