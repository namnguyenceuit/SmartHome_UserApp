package com.uit.ce.smart_home.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.ToxicBakery.viewpager.transforms.DefaultTransformer;
import com.uit.ce.smart_home.R;
import com.uit.ce.smart_home.fragments.Profile;
import com.uit.ce.smart_home.fragments.HomeController;
import com.uit.ce.smart_home.fragments.Fragment3;
import com.uit.ce.smart_home.services.ConnectServerService;

import static com.uit.ce.smart_home.services.ConnectServerService.ACTION_LOGIN;
import static com.uit.ce.smart_home.services.ConnectServerService.APP_PASSWORD;
import static com.uit.ce.smart_home.services.ConnectServerService.APP_USERNAME;
import static com.uit.ce.smart_home.services.ConnectServerService.CONNECTED;
import static com.uit.ce.smart_home.services.ConnectServerService.IS_NETWORK_CONNECTED;
import static com.uit.ce.smart_home.services.ConnectServerService.LOGIN_ALREADY;
import static com.uit.ce.smart_home.services.ConnectServerService.LOGIN_FAIL;
import static com.uit.ce.smart_home.services.ConnectServerService.LOGIN_SUCCESS;
import static com.uit.ce.smart_home.services.ConnectServerService.MESSAGE_RESPOND;

public class MainActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {

    private BottomNavigationView navigation;
    private ViewPager viewPager;
    public Toolbar toolbar;

    private Profile profile = new Profile();
    private HomeController homeController = new HomeController();
    private Fragment3 fragment3 = new Fragment3();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.addOnPageChangeListener(this);
        viewPager.setPageTransformer(true, new DefaultTransformer());

        navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        // create notification channel
        createNotificationChannel();
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return profile;
                    case 1:
                        return homeController;
                    case 2:
                        return fragment3;
                }
                return null;
            }

            @Override
            public int getCount() {
                return 3;
            }
        });
    }

    private final BroadcastReceiver UpdateResponseService = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (MESSAGE_RESPOND.equals(action)) {
                Toast.makeText(MainActivity.this, intent.getStringExtra("MESSAGE_RES"), Toast.LENGTH_SHORT).show();
            }

            if (CONNECTED.equals(action)) {
                toolbar.setSubtitle("Tap to re-login");
            }

            if (LOGIN_SUCCESS.equals(action)) {
                toolbar.setSubtitle("Connected");
            }

            if (LOGIN_ALREADY.equals(action)) {
                Toast.makeText(getApplicationContext(), "Cannot login this moment, please try again", Toast.LENGTH_LONG);
                try{
                    LoginActivity.isLoggedOut = true;
                    Intent intentAlready = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intentAlready);
                }
                catch (Exception e){
                    Intent intentAlready = new Intent(MainActivity.this, LoginActivity.class);
                    intentAlready.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intentAlready);
                }
            }

            if (LOGIN_FAIL.equals(action)) {
                try{
                    LoginActivity.isLoggedOut = true;
                    Intent intentFail = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intentFail);
                }
                catch (Exception e){
                    Intent intentFail = new Intent(MainActivity.this, LoginActivity.class);
                    intentFail.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intentFail);
                }
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
                if (!toolbar.getSubtitle().equals("Connected")) {
                    toolbar.setSubtitle("Tap to reconnect");
                    toolbar.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            if (toolbar.getSubtitle().equals("Tap to reconnect")) {
                                ReconnectServer();
                            }

                            if (toolbar.getSubtitle().equals("Tap to re-login")){
                                ReLoginServer();
                            }
                        }
                    });
                }
            } else {
                // if not online
                toolbar.setSubtitle("Connection went wrong");
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
        registerReceiver(UpdateResponseService, UpdateResponseServiceFilter());
        registerReceiver(UpdateResponseService, ReLogin());
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(UpdateResponseService);
        unregisterReceiver(networkStateReceiver);
    }

    private static IntentFilter UpdateResponseServiceFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CONNECTED);
        intentFilter.addAction(MESSAGE_RESPOND);
        return intentFilter;
    }

    private static IntentFilter ReLogin() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LOGIN_SUCCESS);
        intentFilter.addAction(LOGIN_FAIL);
        intentFilter.addAction(LOGIN_ALREADY);
        return intentFilter;
    }

    private void ReconnectServer() {
        Intent reconnect = new Intent(getApplicationContext(), ConnectServerService.class);
        reconnect.setAction(IS_NETWORK_CONNECTED);
        toolbar.setSubtitle("Trying to reconnect...");
        startService(reconnect);
    }

    private void ReLoginServer() {
        //String username = editText_username.getText().toString();
        //String password = editText_password.getText().toString();
        Intent reLoginIntent = new Intent(getApplicationContext(), ConnectServerService.class);
        reLoginIntent.setAction(ACTION_LOGIN);

        // TODO: those values are just for testing, need to get it form SharedPreferences
        reLoginIntent.putExtra(APP_USERNAME, "user");
        reLoginIntent.putExtra(APP_PASSWORD, "user123");
        startService(reLoginIntent);
    }


    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Alert notification";
            String description = "Alert if the house is in a danger situation";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("1", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            viewPager.setCurrentItem(item.getOrder());
            return true;
        }

    };

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        navigation.getMenu().getItem(position).setChecked(true);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }
}
