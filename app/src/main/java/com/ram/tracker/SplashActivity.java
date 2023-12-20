package com.ram.tracker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.airbnb.lottie.LottieAnimationView;

public class SplashActivity extends AppCompatActivity {
    LottieAnimationView animationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        animationView = findViewById(R.id.animationView);
        animationView.setAnimation("anim.json");
        animationView.playAnimation();
        animationView.loop(true);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {


                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(SplashActivity.this);
                String name = sp.getString("Name", "");

                if (name != null && name.length() > 1) {

                    Intent intent = new Intent(SplashActivity.this, MyLocationActivity.class);
                    intent.putExtra("name", name);
                    startActivity(intent);

                } else {
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(intent);


                }


            }
        }, 3000);
    }
}
