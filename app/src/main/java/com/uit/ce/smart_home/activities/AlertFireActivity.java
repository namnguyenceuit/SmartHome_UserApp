package com.uit.ce.smart_home.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.uit.ce.smart_home.R;
import com.uit.ce.smart_home.fragments.HomeController;
import com.uit.ce.smart_home.services.ConnectServerService;

public class AlertFireActivity extends AppCompatActivity {

    ImageButton button_reset;
    TextView textView_alert;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_fire);

        button_reset = findViewById(R.id.btn_resetButton);
        textView_alert = findViewById(R.id.txtv_alert_fire);
        String type = getIntent().getStringExtra(HomeController.RESET_STATUS);
        textView_alert.setText(getString(R.string.alert_fire));
//        if(type.equals("fire")){
//            textView_alert.setText(getString(R.string.alert_fire));
//        }
        button_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConnectServerService.client.send("resetFire/" + ConnectServerService.user.getHomes().get(0).getMacAddr());
                finish();
            }
        });
    }
}
