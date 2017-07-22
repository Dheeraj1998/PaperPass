package com.example.paperpass;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.IntentCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Settings extends AppCompatActivity {

    // Initializing global variables
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.settings_actionbar);

        View view =getSupportActionBar().getCustomView();

        ImageButton imageButton= (ImageButton)view.findViewById(R.id.settings_goback);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent temp = new Intent(Settings.this, Dashboard.class);
                startActivity(temp);
                finish();
            }
        });
    }

    public void beginLogout(View v){
        try {
            mAuth.signOut();

            Intent temp = new Intent(Settings.this, SplashScreen.class);
            finishAffinity();
            startActivity(temp);
            Toast.makeText(getApplicationContext(), "Logout successful!", Toast.LENGTH_SHORT).show();
        }

        catch (Exception e){
            Toast.makeText(getApplicationContext(), "Logout failed. Try again later!", Toast.LENGTH_SHORT).show();
        }
    }
}
