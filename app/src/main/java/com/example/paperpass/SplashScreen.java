package com.example.paperpass;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreen extends AppCompatActivity {

    //Initializing global variables
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        // Handling FCM notification
        Bundle notification_data = getIntent().getExtras();
        if (notification_data != null) {
            try {
                if (notification_data.getString("action").equals("download")) {
                    String download_url = notification_data.getString("download_url");
                    Intent temp = new Intent(Intent.ACTION_VIEW);
                    temp.setData(Uri.parse(download_url));
                    startActivity(temp);
                    finishAffinity();
                }
            } catch (Exception error) {
                // Do nothing
            }
        }

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    if (user.isEmailVerified()) {
                        Intent temp = new Intent(SplashScreen.this, Dashboard.class);
                        startActivity(temp);
                        finish();
                    } else {
                        // User is not email-verified
                        Intent temp = new Intent(SplashScreen.this, LoginActivity.class);
                        startActivity(temp);
                        finish();
                    }
                } else {
                    // User is signed out
                    Intent temp = new Intent(SplashScreen.this, LoginActivity.class);
                    startActivity(temp);
                    finish();
                }
                }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
