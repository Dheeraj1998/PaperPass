package com.example.paperpass;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    //Initializing global variables
    String username, password;
    Button lgn_button;
    EditText log_email, log_password;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        log_email = (EditText) findViewById(R.id.log_email);
        log_password = (EditText) findViewById(R.id.log_password);
        lgn_button = (Button) findViewById(R.id.btn_login);

        try {
            username = getIntent().getStringExtra("username");
            log_email.setText(username);

            if (!username.equals("")) {
                log_password.requestFocus();
            }
        } catch (Exception e) {
            // Do nothing.
            // Proceed with normal execution
        }

        mAuth = FirebaseAuth.getInstance();
    }

    public void openRegister(View v) {
        Intent temp = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(temp);
        finish();
    }

    public void beginLogin(View v) {
        username = log_email.getText().toString();
        password = log_password.getText().toString();

        if (check_validity()) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Authenticating user...");
            progressDialog.setCanceledOnTouchOutside(false);

            progressDialog.show();

            mAuth.signInWithEmailAndPassword(username, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            // If sign in fails, display a message to the user. If sign in succeeds
                            // the auth state listener will be notified and logic to handle the
                            // signed in user can be handled in the listener.
                            if (!task.isSuccessful()) {
                                progressDialog.dismiss();

                                Toast.makeText(getApplicationContext(), "Login failed!", Toast.LENGTH_SHORT).show();
                                lgn_button.setEnabled(true);
                            } else {
                                progressDialog.dismiss();

                                Intent temp = new Intent(LoginActivity.this, Dashboard.class);
                                startActivity(temp);
                                Toast.makeText(getApplicationContext(), "Login successful!", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }
                    });
        } else {
            // Do nothing!
        }
    }

    public boolean check_validity() {
        boolean valid = true;

        if (username.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            Log.i("custom", "Email is empty!");
            log_email.setError("Enter a valid email address!");
            valid = false;
        } else {
            log_email.setError(null);
        }

        if (password.isEmpty()) {
            Log.i("custom", "Pass is empty!");
            log_password.setError("The field cannot be empty!");
            valid = false;
        } else {
            log_password.setError(null);
        }

        return valid;
    }
}
