package com.uit.ce.smart_home.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.uit.ce.smart_home.R;
import com.uit.ce.smart_home.activities.LoginActivity;
import com.uit.ce.smart_home.models.User;
import com.uit.ce.smart_home.utils.UserConverter;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

import static android.support.v4.app.NotificationCompat.PRIORITY_MIN;

public class ConnectServerService extends Service {
    static final int NOTIFICATION_ID = 543;
    public final static String ACTION_START_SERVICE = "START_SERVICE";
    public final static String ACTION_LOGIN = "LOGIN";
    public final static String IS_NETWORK_CONNECTED = "IS_NETWORK_CONNECTED";
    public final static String APP_USERNAME = "USERNAME";
    public final static String APP_PASSWORD = "PASSWORD";
    public final static String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public final static String LOGIN_ALREADY = "LOGIN_ALREADY";
    public final static String LOGIN_FAIL = "LOGIN_FAIL";
    public final static String MESSAGE_RESPOND = "MESSAGE_RESPOND";
    public final static String MESSAGE_UPDATE = "MESSAGE_UPDATE";
    public final static String CLOSE_SOCKET = "CLOSE_SOCKET";
    public static final String TAG = "Tag";
    public static final String CONNECTED = "CONNECTED";
    public static final String DISCONNECTED = "DISCONNECTED";

    public static WebSocketClient client;
    public static User user;
    public static String[] response;
    public static boolean isServiceRunning = false;

    public void onCreate() {
        super.onCreate();
        startServiceWithNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_START_SERVICE)) {

        } else if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_LOGIN)) {
            String name = intent.getStringExtra(APP_USERNAME);
            String pass = intent.getStringExtra(APP_PASSWORD);
            doLogin(name, pass);
        } else if (intent != null && intent.getAction() != null && intent.getAction().equals(CLOSE_SOCKET)) {
            client.close();
            stopMyService();
        } else if (intent != null && intent.getAction() != null &&
                intent.getAction().equals(IS_NETWORK_CONNECTED)) {
            client.reconnect();
        } else stopMyService();

        return START_STICKY;
    }


    public void doLogin(String name, String pass) {
        final String username = name;
        final String password = pass;
        try {
            client.send("login/" + username + "@" + password);
        } catch (Exception e) {
            Log.d(TAG, "Error: " + e.toString());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        client.close();
    }

    void stopMyService() {
        stopForeground(true);
        stopSelf();
        isServiceRunning = false;
    }

    void startServiceWithNotification() {
        if (isServiceRunning) return;
        isServiceRunning = true;
        String chanID = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            chanID = createNotificationChannel("7437", "SmartHome service");
        } else {
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, chanID);
        Notification noti = builder
                .setSmallIcon(R.drawable.ic_home_black_24dp)
                .setOngoing(true)
                .setPriority(PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentTitle("SmartHome is running in background")
                .setContentText("The service is connecting to the internet")
                .setSound(null)
                .setTicker("TICKER")
                .build();

        startForeground(NOTIFICATION_ID, noti);
        connectServer();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelID, String channelName) {
        NotificationChannel chan = new NotificationChannel(channelID,
                channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelID;
    }


    public void connectServer() {
        client = new WebSocketClient(URI.create("http://ubuntu.localhost.run:80")) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.d("TAG", "Connected");
                broadcastUpdateExtra(CONNECTED);
                LoginActivity.connectView.post(new Runnable() {
                    @Override
                    public void run() {
                        LoginActivity.toolbarSubTitle.setText(R.string.toolbar_status_connected);
                        //button_connect.setText(R.string.text_button_disconnect);
                        //loginView.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                super.onMessage(bytes);
            }

            @Override
            public void onMessage(String message) {
                Log.d(TAG, "server:" + message);
                response = message.split("/");
                switch (response[0]) {
                    // login
                    case "login":
                        if (response[1].equals("1")) {

                        } else if (response[1].equals("2")) {
                            broadcastUpdateExtra(LOGIN_ALREADY);
                        } else {
                            broadcastUpdateExtra(LOGIN_FAIL);
                        }
                        break;

                    // get User after login
                    case "getUser":
                        DBObject userDBObject = (DBObject) JSON.parse(response[1]);
                        user = new User();
                        user = UserConverter.toUser(userDBObject);
                        broadcastUpdateExtra(LOGIN_SUCCESS);
                        break;

                    case "messageRes":
                        broadcastUpdateMessage(MESSAGE_RESPOND, response[1]);
                        break;

                    case "update":
                        DBObject userUpdate = (DBObject) JSON.parse(response[1]);
                        user = UserConverter.toUser(userUpdate);
                        broadcastUpdateExtra(MESSAGE_UPDATE);
                        break;
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.d(TAG, "Disconnected");
                broadcastUpdateExtra(DISCONNECTED);
                LoginActivity.connectView.post(new Runnable() {
                    @Override
                    public void run() {
                        LoginActivity.toolbarSubTitle.setText(R.string.toolbar_status_disconnect);
                        //button_connect.setText(R.string.text_button_connect);
                        //loginView.setVisibility(View.INVISIBLE);
                    }
                });
            }

            @Override
            public void onError(Exception ex) {
                Log.e(TAG, "onError: " + ex);
                LoginActivity.connectView.post(new Runnable() {
                    @Override
                    public void run() {
                        LoginActivity.toolbarSubTitle.setText(R.string.toolbar_status_error);
                        //loginView.setVisibility(View.INVISIBLE);

                    }
                });
            }
        };
        client.connect();
    }

    private void broadcastUpdateExtra(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdateMessage(final String action, String msg) {
        final Intent intent = new Intent(action);
        intent.putExtra("MESSAGE_RES", msg);
        sendBroadcast(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
