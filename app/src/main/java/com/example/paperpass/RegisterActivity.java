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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    //Declaring global variables
    String name, email, password;
    Button reg_button;
    EditText reg_name, reg_email, reg_password;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
    }

    public void openLogin(View v){
        Intent temp = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(temp);
        finish();
    }

    public void beginRegister(View v){
        reg_button = (Button) findViewById(R.id.btn_register);

        reg_name = (EditText) findViewById(R.id.reg_name);
        reg_email = (EditText) findViewById(R.id.reg_email);
        reg_password = (EditText) findViewById(R.id.reg_password);

        name = reg_name.getText().toString().trim();
        email = reg_email.getText().toString().trim();
        password = reg_password.getText().toString().trim();

        if(check_validity()){
            reg_button.setEnabled(false);

            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Registering user...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            final DatabaseReference myRootRef = FirebaseDatabase.getInstance().getReference("user_profile");

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            // If sign in fails, display a message to the user. If sign in succeeds
                            // the auth state listener will be notified and logic to handle the
                            // signed in user can be handled in the listener.
                            if (!task.isSuccessful()) {
                                progressDialog.dismiss();

                                Toast.makeText(getApplicationContext(), "Registration failed!", Toast.LENGTH_SHORT).show();
                                reg_button.setEnabled(true);
                            }

                            else{
                                DatabaseReference user_id_ref =  myRootRef.child(mAuth.getCurrentUser().getUid());

                                user_profile UProfile = new user_profile();
                                UProfile.setName(name);
                                user_id_ref.setValue(UProfile);

                                progressDialog.dismiss();

                                Intent temp = new Intent(RegisterActivity.this, LoginActivity.class);
                                temp.putExtra("username", email);
                                startActivity(temp);
                                Toast.makeText(getApplicationContext(), "Registration successful!", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }
                    });
        }

        else{
            //Do nothing
        }
    }

    public boolean check_validity(){

        boolean valid = true;

        if (name.isEmpty()) {
            Log.i("custom","Name is empty!");
            reg_name.setError("The field cannot be empty!");
            valid = false;
        } else {
            reg_name.setError(null);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Log.i("custom","Email is empty!");
            reg_email.setError("Enter a valid email address!");
            valid = false;
        } else {
            reg_email.setError(null);
        }

        if (password.isEmpty()) {
            Log.i("custom","Pass is empty!");
            reg_password.setError("The field cannot be empty!");
            valid = false;
        } else {
            reg_password.setError(null);
        }

        return valid;
    }
}
