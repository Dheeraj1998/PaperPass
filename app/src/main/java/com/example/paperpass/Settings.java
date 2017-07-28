package com.example.paperpass;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

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

    public void sendFeedback(View v) {
        LayoutInflater li = LayoutInflater.from(getApplicationContext());
        final View dialogView = li.inflate(R.layout.feedback_form, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Settings.this);
        alertDialogBuilder.setTitle("Feedback");
        alertDialogBuilder.setView(dialogView);

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Send",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                final TextView feedback_textview = (TextView) dialogView.findViewById(R.id.feedback_data);
                                String feedback = feedback_textview.getText().toString();

                                if (feedback.isEmpty()) {
                                    Toast.makeText(getApplicationContext(), "The feedback is empty!", Toast.LENGTH_SHORT).show();
                                } else {
                                    DatabaseReference feedback_reg = FirebaseDatabase.getInstance().getReference("user_feedback");
                                    String push_key = feedback_reg.push().getKey();

                                    feedback_reg.child(push_key).child("User_UUID").setValue(mAuth.getCurrentUser().getUid());
                                    feedback_reg.child(push_key).child("User_FCM").setValue(FirebaseInstanceId.getInstance().getToken());
                                    feedback_reg.child(push_key).child("Feedback").setValue(feedback);

                                    Toast.makeText(getApplicationContext(), "Thank you for your suggestion!", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                }
                            }
                        }).setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        final AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#f34235"));
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#43A047"));
            }
        });

        alertDialog.show();

    }
}
