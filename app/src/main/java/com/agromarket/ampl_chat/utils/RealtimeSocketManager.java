package com.agromarket.ampl_chat.utils;

import android.util.Log;

import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.PrivateChannel;
import com.pusher.client.channel.PrivateChannelEventListener;
import com.pusher.client.channel.PusherEvent;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import com.pusher.client.util.HttpAuthorizer;

import java.util.HashMap;
import java.util.Map;

public class RealtimeSocketManager {

    private static final String TAG = "SOCKET";

    private static Pusher pusher;
    private static PrivateChannel chatChannel;
    private static PrivateChannel callChannel;

    private static int currentUserId = -1;

    /* ================= CONNECT ================= */
    public static synchronized void connect(
            SessionManager session,
            int userId,
            SocketListener listener) {
        if (pusher != null && currentUserId == userId) {
            Log.d(TAG, "Already connected");
            return;
        }

        disconnect(); // ensure clean state

        currentUserId = userId;

        String authUrl = "https://uatamplchat.agromarket.co.in/api/broadcasting/auth";

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + session.getToken());
        headers.put("Accept", "application/json");

        HttpAuthorizer authorizer = new HttpAuthorizer(authUrl);
        authorizer.setHeaders(headers);

        PusherOptions options = new PusherOptions()
                .setAuthorizer(authorizer)
                .setUseTLS(true)
                .setHost("uatamplchat.agromarket.co.in")
                .setWsPort(443)
                .setWssPort(443)
                .setEncrypted(true)
                .setActivityTimeout(120_000)
                .setPongTimeout(30_000);

        pusher = new Pusher(
                "lcwlcvigxtbjksfcedjh", // REVERB_APP_KEY
                options);

        /* ================= CONNECTION LISTENER ================= */
        pusher.getConnection().bind(ConnectionState.ALL, new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange change) {
                Log.d(TAG, "State: " + change.getPreviousState() + " → " + change.getCurrentState());
            }

            @Override
            public void onError(String message, String code, Exception e) {
                Log.e(TAG, "Connection error: " + message + " (" + code + ")", e);
            }
        });

        subscribeChatChannel(userId, listener);
        subscribeCallChannel(userId, listener);

        pusher.connect();
    }

    /* ================= CHAT CHANNEL ================= */
    private static void subscribeChatChannel(int userId, SocketListener listener) {
        chatChannel = pusher.subscribePrivate(
                "private-chat-channel." + userId,
                baseChannelListener());

        chatChannel.bind("message.sent", baseEventListener(listener));
    }

    /* ================= CALL CHANNEL ================= */
    private static void subscribeCallChannel(int userId, SocketListener listener) {
        callChannel = pusher.subscribePrivate(
                "private-call-channel." + userId,
                baseChannelListener());

        callChannel.bind("incoming_call", baseEventListener(listener));

        callChannel.bind("call_accepted", baseEventListener(listener));

        callChannel.bind("call_rejected", baseEventListener(listener));

        callChannel.bind("call_ended", baseEventListener(listener));
    }

    /* ================= BASE CHANNEL LISTENER ================= */
    private static PrivateChannelEventListener baseChannelListener() {
        return new PrivateChannelEventListener() {
            @Override
            public void onSubscriptionSucceeded(String channelName) {
                Log.d(TAG, "Subscribed: " + channelName);
            }

            @Override
            public void onAuthenticationFailure(String message, Exception e) {
                Log.e(TAG, "Auth failed: " + message, e);
            }

            @Override
            public void onEvent(com.pusher.client.channel.PusherEvent event) {
                // not used
            }
        };
    }

    private static PrivateChannelEventListener baseEventListener(SocketListener listener) {
        return new PrivateChannelEventListener() {
            public void onEvent(PusherEvent event) {
                String eventName = event.getEventName();
                String data = event.getData();

                Log.d(TAG, "Event received: " + eventName);
                Log.d(TAG, "Data: " + data);

                switch (eventName) {
                    case "message.sent":
                        listener.onMessageReceived(data);
                        break;

                    case "incoming_call":
                        listener.onIncomingCall(data);
                        break;

                    case "call_accepted":
                        listener.onCallAccepted(data);
                        break;

                    case "call_rejected":
                        listener.onCallRejected(data);
                        break;

                    case "call_ended":
                        listener.onCallEnded(data);
                        break;

                    default:
                        Log.w(TAG, "Unhandled event: " + eventName);
                        break;
                }
            }

            @Override
            public void onSubscriptionSucceeded(String channelName) {
            }

            @Override
            public void onAuthenticationFailure(String message, Exception e) {
            }
        };
    }

    /* ================= DISCONNECT ================= */
    public static synchronized void disconnect() {
        if (pusher != null) {
            try {
                if (chatChannel != null) {
                    pusher.unsubscribe(chatChannel.getName());
                }
                if (callChannel != null) {
                    pusher.unsubscribe(callChannel.getName());
                }

                pusher.disconnect();
            } catch (Exception ignored) {
            }

            pusher = null;
            chatChannel = null;
            callChannel = null;
            currentUserId = -1;

            Log.d(TAG, "Disconnected");
        }
    }

    /* ================= LISTENER ================= */
    public interface SocketListener {
        void onMessageReceived(String data);

        void onIncomingCall(String data);

        void onCallAccepted(String data);

        void onCallRejected(String data);

        void onCallEnded(String data);
    }

}