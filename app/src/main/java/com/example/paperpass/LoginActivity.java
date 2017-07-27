package com.example.paperpass;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    //Initializing global variables
    String username, password, user_name;
    Button lgn_button;
    EditText log_email, log_password;
    FirebaseAuth mAuth;
    ProgressDialog progressDialog;

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

                                if (mAuth.getCurrentUser().isEmailVerified()) {
                                    Intent temp = new Intent(LoginActivity.this, Dashboard.class);
                                    startActivity(temp);
                                    Toast.makeText(getApplicationContext(), "Login successful!", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    // Getting the name of the user
                                    DatabaseReference myRootRef = FirebaseDatabase.getInstance().getReference("user_profile/" + mAuth.getCurrentUser().getUid());

                                    try {
                                        mAuth.getCurrentUser().reload();
                                        mAuth.getCurrentUser().getDisplayName();

                                        mAuth.getCurrentUser().sendEmailVerification();
                                        Toast.makeText(getApplicationContext(), "Verification email needs to be confirmed!", Toast.LENGTH_SHORT).show();
                                        mAuth.signOut();
                                    } catch (Exception error) {
                                        myRootRef.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                user_profile UProfile = dataSnapshot.getValue(user_profile.class);
                                                user_name = UProfile.getName();

                                                // Updating the name of the profile of user
                                                FirebaseUser user = mAuth.getCurrentUser();
                                                if (user != null) {
                                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(user_name).build();
                                                    user.updateProfile(profileUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            mAuth.getCurrentUser().reload();
                                                            mAuth.getCurrentUser().sendEmailVerification();
                                                            Toast.makeText(getApplicationContext(), "Verification email needs to be confirmed!", Toast.LENGTH_SHORT).show();
                                                            mAuth.signOut();
                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Toast.makeText(getApplicationContext(), "Error. Please try again later!", Toast.LENGTH_SHORT).show();
                                                            mAuth.signOut();
                                                        }
                                                    });
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                Toast.makeText(getApplicationContext(), "Error. Please try again later!", Toast.LENGTH_SHORT).show();
                                                mAuth.signOut();
                                            }
                                        });
                                    }
                                }
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
            log_email.setError("Enter a valid email address!");
            valid = false;
        } else {
            log_email.setError(null);
        }

        if (password.isEmpty()) {
            log_password.setError("The field cannot be empty!");
            valid = false;
        } else {
            log_password.setError(null);
        }

        return valid;
    }

    public void startPasswordRecovery(View v) {
        username = log_email.getText().toString();
        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        if (username.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            Toast.makeText(getApplicationContext(), "Enter a valid email!", Toast.LENGTH_SHORT).show();
        } else {
            progressDialog.show();
            log_email.setError(null);
            mAuth.sendPasswordResetEmail(username).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Password reset mail has been sent!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Error. Please try again later!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
