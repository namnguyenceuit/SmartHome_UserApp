package com.uit.ce.smart_home.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.uit.ce.smart_home.R;
import com.uit.ce.smart_home.models.User;
import com.uit.ce.smart_home.services.ConnectServerService;

import org.java_websocket.client.WebSocketClient;

import static com.uit.ce.smart_home.services.ConnectServerService.ACTION_LOGIN;
import static com.uit.ce.smart_home.services.ConnectServerService.ACTION_START_SERVICE;
import static com.uit.ce.smart_home.services.ConnectServerService.APP_PASSWORD;
import static com.uit.ce.smart_home.services.ConnectServerService.APP_USERNAME;
import static com.uit.ce.smart_home.services.ConnectServerService.IS_NETWORK_CONNECTED;
import static com.uit.ce.smart_home.services.ConnectServerService.LOGIN_ALREADY;
import static com.uit.ce.smart_home.services.ConnectServerService.LOGIN_FAIL;
import static com.uit.ce.smart_home.services.ConnectServerService.LOGIN_SUCCESS;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "Tag";
    public static int STATUS = 0;
    public static final int CONNECTED = 1;

    public static boolean isLoggedOut = false;
    public static WebSocketClient client;
    public static User user;

    Intent loginIntent;
    Intent connectServer;

    LinearLayout loginView;
    public static LinearLayout connectView;
    Toolbar toolbar;
    public static TextView toolbarSubTitle;
    EditText editText_username;
    EditText editText_password;
    CheckBox checkBox_remember;
    Button button_login;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        InitialView();
        connectServer = new Intent(getApplicationContext(), ConnectServerService.class);
        connectServer.setAction(ACTION_START_SERVICE);
        startService(connectServer);
    }

    private final BroadcastReceiver UpdateInternetService = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (LOGIN_SUCCESS.equals(action)) {
                STATUS = CONNECTED;
                Intent intentReceived = new Intent(LoginActivity.this, MainActivity.class);
                finish();
                LoginActivity.this.startActivity(intentReceived);
            }

            if (LOGIN_ALREADY.equals(action)) {
                Toast.makeText(LoginActivity.this, "Account is already logged in", Toast.LENGTH_SHORT).show();
            }

            if (LOGIN_FAIL.equals(action)) {
                Toast.makeText(LoginActivity.this, "Wrong username/password", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager manager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
                // if online
                if (!toolbarSubTitle.getText().equals("Connected")) {
                    toolbarSubTitle.setText("Tab to reconnect");
                    toolbarSubTitle.setOnClickListener(new View.OnClickListener() {
                        private boolean clickStateChanged;

                        @Override
                        public void onClick(View view) {
                            ReconnectServer();
                            if (clickStateChanged) {
                                // reset background to default;
                            } else {
                                toolbarSubTitle.setText("Trying to reconnect...");
                            }
                            clickStateChanged = !clickStateChanged;
                        }
                    });
                }
            } else {
                // if not online
                toolbarSubTitle.setText("No Internet connection");
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
        registerReceiver(UpdateInternetService, LoginIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(UpdateInternetService);
        unregisterReceiver(networkStateReceiver);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                LoginServer();
                if (!toolbarSubTitle.getText().equals("Connected")) {
                    Toast.makeText(LoginActivity.this, "Please check your internet connection", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void LoginServer() {
        String username = editText_username.getText().toString();
        String password = editText_password.getText().toString();
        loginIntent = new Intent(getApplicationContext(), ConnectServerService.class);
        loginIntent.setAction(ACTION_LOGIN);
        loginIntent.putExtra(APP_USERNAME, username);
        loginIntent.putExtra(APP_PASSWORD, password);
        startService(loginIntent);
    }

    private static IntentFilter LoginIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LOGIN_SUCCESS);
        intentFilter.addAction(LOGIN_FAIL);
        intentFilter.addAction(LOGIN_ALREADY);
        return intentFilter;
    }

    private void ReconnectServer() {
        Intent reconnect = new Intent(getApplicationContext(), ConnectServerService.class);
        reconnect.setAction(IS_NETWORK_CONNECTED);
        startService(reconnect);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void InitialView() {

        loginView = findViewById(R.id.loginview);
        connectView = findViewById(R.id.connect_view);
        toolbar = findViewById(R.id.toolbar_login);
        toolbarSubTitle = findViewById(R.id.toolbar_subtitle);
        editText_username = findViewById(R.id.edt_username);
        editText_password = findViewById(R.id.edt_password);
        checkBox_remember = findViewById(R.id.chb_rem);
        button_login = findViewById(R.id.btn_login);

        //set Click listener view
        button_login.setOnClickListener(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (isLoggedOut) {
            toolbarSubTitle.setText("Connected");
            isLoggedOut = false;
        }
    }

    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }
}
