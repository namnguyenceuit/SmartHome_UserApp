package com.uit.ce.smart_home.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.uit.ce.smart_home.fragments.HomeController;

import java.io.IOException;

import static com.uit.ce.smart_home.fragments.HomeController.mMediaPlayer;


public class ClearFireAlarmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceStat) {
        super.onCreate(savedInstanceStat);
        if(mMediaPlayer.isPlaying())
        {
            mMediaPlayer.stop();
            try {
                mMediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Intent intent = new Intent(this, AlertFireActivity.class);
        startActivity(intent);
        HomeController.notificationManager.cancelAll();
        finish();
    }
}
