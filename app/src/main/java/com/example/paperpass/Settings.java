package com.example.paperpass;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class Settings extends AppCompatActivity {

    // Initializing global variables
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        FirebaseDatabase.getInstance().goOnline();

        final TextView hello_text = (TextView) findViewById(R.id.name_holder);
        mAuth = FirebaseAuth.getInstance();

        try {
            hello_text.setText(getIntent().getStringExtra("user_name"));

            if (getIntent().getStringExtra("user_name").equals("")) {
                hello_text.setText(getIntent().getStringExtra("Hello!"));
            }
        } catch (Exception error) {
            hello_text.setText("Hello!");
        }

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.settings_actionbar);

        View view = getSupportActionBar().getCustomView();

        TextView title = (TextView) view.findViewById(R.id.ac_bar_title);
        title.setTextSize(20);
        title.setText("Settings");

        ImageButton imageButton = (ImageButton) view.findViewById(R.id.settings_goback);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent temp = new Intent(Settings.this, Dashboard.class);
                startActivity(temp);
                finish();
            }
        });
    }

    public void beginLogout(View v) {
        try {
            mAuth.signOut();

            Intent temp = new Intent(Settings.this, SplashScreen.class);
            finishAffinity();
            FirebaseDatabase.getInstance().goOffline();
            startActivity(temp);
            Toast.makeText(getApplicationContext(), "Logout successful!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Logout failed. Try again later!", Toast.LENGTH_SHORT).show();
        }
    }

    public void openUserPosts(View v) {
        Intent temp = new Intent(Settings.this, UserPosts.class);
        startActivity(temp);
    }
}
