package com.uit.ce.smart_home.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.uit.ce.smart_home.R;
import com.uit.ce.smart_home.activities.LoginActivity;
import com.uit.ce.smart_home.services.ConnectServerService;


public class Profile extends Fragment {
    public TextView textView_name;
    public Button button_logout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile, container, false);
        textView_name = view.findViewById(R.id.txtv_name);
        button_logout = view.findViewById(R.id.btn_logout);
        textView_name.setText(ConnectServerService.user.getName());
        button_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    ConnectServerService.client.send("logout/"+ConnectServerService.user.getId().toString());

                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(intent);
                }
                catch (Exception e){
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                }
            }
        });

        return view;
    }
}