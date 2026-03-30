package com.agromarket.ampl_chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agromarket.ampl_chat.adapters.ChatMessageAdapter;
import com.agromarket.ampl_chat.adapters.ProductAdapter;
import com.agromarket.ampl_chat.models.MessageItem;
import com.agromarket.ampl_chat.models.ProductItem;
import com.agromarket.ampl_chat.models.api.CallResponse;
import com.agromarket.ampl_chat.models.api.ChatMessage;
import com.agromarket.ampl_chat.models.api.MessageListResponse;
import com.agromarket.ampl_chat.models.api.Product;
import com.agromarket.ampl_chat.models.api.ProductListResponse;
import com.agromarket.ampl_chat.models.api.SendMessageRequest;
import com.agromarket.ampl_chat.models.api.SendMessageResponse;
import com.agromarket.ampl_chat.models.api.SendProductRequest;
import com.agromarket.ampl_chat.utils.ApiClient;
import com.agromarket.ampl_chat.utils.ApiService;
import com.agromarket.ampl_chat.utils.RealtimeSocketManager;
import com.agromarket.ampl_chat.utils.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*
TODO:
1. Add Search
2. Attachments Upload
3. Realtime Seen Show
4. Show Chat history Dates
5. Show Call History
6. Product Order System
7. Image and Video View
 */

public class ChatScreenActivity extends AppCompatActivity
        implements RealtimeSocketManager.SocketListener {

    private static final String TAG = "ChatScreen";
    private static final String BASE_URL = "https://uatamplchat.agromarket.co.in/";
    private static final long DUPLICATE_BLOCK_WINDOW = 800;

    /* ================= STATE ================= */
    private int pendingCallId;
    private String pendingChannel;
    private String pendingToken;
    private int pendingUid;

    private SessionManager session;
    private int receiverId;
    private boolean isCustomer;

    private String lastSentText;
    private long lastSentTime;
    private int lastSentProductId = -1;
    private long lastProductSentTime;

    /* ================= UI ================= */

    private RecyclerView chatRecycler;
    private ChatMessageAdapter chatAdapter;
    private final ArrayList<MessageItem> messageList = new ArrayList<>();

    private EditText messageBox;
    private TextView chatTitle;
    private ImageView sendBtn, cartBtn, backBtn, callBtn;

    /* ================= PRODUCTS ================= */

    private int productCurrentPage = 1;
    private boolean productIsLoading = false;
    private boolean productHasMore = true;
    private final ArrayList<ProductItem> productList = new ArrayList<>();

    /* ================= ACTIVITY ================= */

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_screen);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), bars.top, v.getPaddingRight(), bars.bottom);
            return insets;
        });

        session = new SessionManager(this);
        isCustomer = "customer".equals(session.getUserRole());

        int customerId = getIntent().getIntExtra("customer_id", 0);
        int agentId = getIntent().getIntExtra("agent_id", 0);
        receiverId = isCustomer ? agentId : customerId;

        boolean isCustomer = session.getUserRole() != null && session.getUserRole().equals("customer");

        initViews();

        String chatName = getIntent().getStringExtra("name");
        if (isCustomer)
            chatTitle.setVisibility(View.GONE);
        else
            chatTitle.setText(chatName);

        setupChat();
        loadMessages();
        markMessagesSeen();

        RealtimeSocketManager.connect(session, session.getUserId(), this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RealtimeSocketManager.disconnect();
    }

    /* ================= INIT ================= */

    private void initViews() {
        chatRecycler = findViewById(R.id.chatRecycler);
        messageBox = findViewById(R.id.messageBox);
        sendBtn = findViewById(R.id.sendBtn);
        cartBtn = findViewById(R.id.cartBtn);
        callBtn = findViewById(R.id.callBtn);
        backBtn = findViewById(R.id.backBtn);
        chatTitle = findViewById(R.id.chatName);

        chatRecycler.setLayoutManager(new LinearLayoutManager(this));

        sendBtn.setOnClickListener(v -> sendTextMessage());
        cartBtn.setOnClickListener(v -> openProductPopup());
        callBtn.setOnClickListener(v -> startCall());
        backBtn.setOnClickListener(v -> onBackPressed());
    }

    private void setupChat() {
        chatAdapter = new ChatMessageAdapter(this, messageList, this::retrySend);
        chatRecycler.setAdapter(chatAdapter);
    }

    /* ================= CALLING ================= */

    private void startCall() {
        if (VoiceCallActivity.isActive())
            return;

        ApiService api = ApiClient.getClient().create(ApiService.class);
        Map<String, Integer> body = new HashMap<>();
        body.put("receiver_id", receiverId);

        api.startCall("Bearer " + session.getToken(), body)
                .enqueue(new Callback<CallResponse>() {
                    @Override
                    public void onResponse(Call<CallResponse> call,
                            Response<CallResponse> response) {

                        if (!response.isSuccessful() || response.body() == null)
                            return;

                        CallResponse data = response.body();

                        // STORE credentials
                        pendingCallId = data.call_id;
                        pendingChannel = data.channel;
                        pendingToken = data.token;
                        pendingUid = data.uid;

                        // Start calling UI ONLY (no Agora yet)
                        VoiceCallActivity.startCalling(
                                ChatScreenActivity.this,
                                pendingCallId,
                                true);
                    }

                    @Override
                    public void onFailure(Call<CallResponse> call, Throwable t) {
                        Toast.makeText(ChatScreenActivity.this,
                                "Call failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /* ================= SOCKET EVENTS ================= */

    @Override
    public void onIncomingCall(String data) {
        try {
            JSONObject json = new JSONObject(data);
            Intent i = new Intent(this, IncomingCallActivity.class);
            i.putExtra("call_id", json.getInt("callId"));
            i.putExtra("caller_name", json.getString("callerName"));
            startActivity(i);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onCallAccepted(String data) {
        try {
            JSONObject json = new JSONObject(data);
            int callId = json.getInt("callId");

            if (callId != pendingCallId)
                return;
            if (!VoiceCallActivity.isActive())
                return;

            // DON'T start new activity - just trigger join in existing one
            VoiceCallActivity.joinChannel(
                    pendingChannel,
                    pendingToken,
                    pendingUid);

            // Clear pending state
            pendingCallId = 0;
            pendingChannel = null;
            pendingToken = null;
            pendingUid = 0;

        } catch (Exception e) {
            Log.e(TAG, "call_accepted error", e);
        }
    }

    @Override
    public void onCallRejected(String data) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Call rejected", Toast.LENGTH_SHORT).show();
            if (VoiceCallActivity.isActive()) {
                VoiceCallActivity.finishCall();
            }
        });
    }

    @Override
    public void onCallEnded(String data) {
        runOnUiThread(() -> {
            if (VoiceCallActivity.isActive()) {
                VoiceCallActivity.finishCall();
            }
        });
    }

    /* ================= MESSAGES ================= */

    @Override
    public void onMessageReceived(String data) {
        runOnUiThread(() -> {
            ChatMessage m = new Gson().fromJson(data, ChatMessage.class);
            if (m.sender_id == session.getUserId())
                return;

            MessageItem item = MessageItem.fromChatMessage(m, BASE_URL);
            messageList.add(item);
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            chatRecycler.scrollToPosition(messageList.size() - 1);
            markMessagesSeen();
        });
    }

    /* ================= API ================= */

    private void sendTextMessage() {
        String msg = messageBox.getText().toString().trim();
        if (msg.isEmpty())
            return;

        long now = System.currentTimeMillis();
        if (msg.equals(lastSentText) && now - lastSentTime < DUPLICATE_BLOCK_WINDOW)
            return;

        lastSentText = msg;
        lastSentTime = now;

        MessageItem item = MessageItem.createLocalText(msg, now);
        messageList.add(item);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecycler.scrollToPosition(messageList.size() - 1);
        messageBox.setText("");

        ApiClient.getClient().create(ApiService.class)
                .sendTextMessage("Bearer " + session.getToken(),
                        new SendMessageRequest(receiverId, msg))
                .enqueue(MessageItem.sendCallback(item, chatAdapter));
    }

    private void retrySend(MessageItem item, int pos) {
        item.retrySend(receiverId, session.getToken(), chatAdapter);
    }

    private void markMessagesSeen() {
        ApiClient.getClient().create(ApiService.class)
                .markSeen("Bearer " + session.getToken(), receiverId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> c, Response<Void> r) {
                    }

                    @Override
                    public void onFailure(Call<Void> c, Throwable t) {
                    }
                });
    }

    private void loadMessages() {
        ApiClient.getClient().create(ApiService.class)
                .getMessages("Bearer " + session.getToken(), receiverId)
                .enqueue(new Callback<MessageListResponse>() {
                    @Override
                    public void onResponse(Call<MessageListResponse> call,
                            Response<MessageListResponse> response) {

                        if (!response.isSuccessful() || response.body() == null)
                            return;

                        messageList.clear();
                        for (ChatMessage m : response.body().messages) {
                            messageList.add(
                                    MessageItem.fromChatMessage(m, BASE_URL));
                        }

                        chatAdapter.notifyDataSetChanged();
                        if (!messageList.isEmpty()) {
                            chatRecycler.scrollToPosition(messageList.size() - 1);
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageListResponse> c, Throwable t) {
                    }
                });
    }

    /* ================= PRODUCTS ================= */

    private void openProductPopup() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.product_popup, null);

        RecyclerView grid = view.findViewById(R.id.productGrid);
        GridLayoutManager lm = new GridLayoutManager(this, 3);
        grid.setLayoutManager(lm);

        ProductAdapter adapter = new ProductAdapter(productList, null);
        grid.setAdapter(adapter);

        loadProducts(adapter);

        view.findViewById(R.id.sendSelected).setOnClickListener(v -> {
            for (ProductItem p : productList) {
                if (p.isSelected && !p.isSkeleton)
                    sendProduct(p);
            }
            dialog.dismiss();
        });

        view.findViewById(R.id.closePopup).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(view);
        dialog.show();
    }

    private void sendProduct(ProductItem product) {
        MessageItem item = MessageItem.createLocalProduct(product);
        messageList.add(item);
        chatAdapter.notifyItemInserted(messageList.size() - 1);

        ApiClient.getClient().create(ApiService.class)
                .sendProductMessage("Bearer " + session.getToken(),
                        new SendProductRequest(receiverId, product.id))
                .enqueue(MessageItem.sendCallback(item, chatAdapter));
    }

    private void loadProducts(ProductAdapter adapter) {
        if (productIsLoading || !productHasMore)
            return;

        productIsLoading = true;
        ApiClient.getClient().create(ApiService.class)
                .getProducts("Bearer " + session.getToken(), productCurrentPage)
                .enqueue(new Callback<ProductListResponse>() {
                    @Override
                    public void onResponse(Call<ProductListResponse> c,
                            Response<ProductListResponse> r) {
                        productIsLoading = false;
                        if (!r.isSuccessful() || r.body() == null)
                            return;

                        for (Product p : r.body().products) {
                            productList.add(ProductItem.fromApi(p, BASE_URL));
                        }

                        productHasMore = r.body().has_more;
                        productCurrentPage++;
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(Call<ProductListResponse> c, Throwable t) {
                        productIsLoading = false;
                    }
                });
    }
}