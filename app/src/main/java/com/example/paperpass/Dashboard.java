package com.example.paperpass;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import java.text.*;
import java.util.*;

public class Dashboard extends AppCompatActivity {

    private StorageReference mStorage;
    private static final int GALLERY_INTENT = 2;
    private static final int CAMERA_REQUEST_CODE = 3;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mStorage = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.dashboard_actionbar);

        View view =getSupportActionBar().getCustomView();

        ImageButton imageButton= (ImageButton)view.findViewById(R.id.settings);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent temp = new Intent(Dashboard.this, Settings.class);
                startActivity(temp);
            }
        });
    }

    public void uploadPicture(View v){
        Intent temp = new Intent(Intent.ACTION_PICK);
        temp.setType("image/*");
        startActivityForResult(temp, GALLERY_INTENT);
    }

    public void takePicture(View v){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_INTENT && resultCode == RESULT_OK ) {
            Uri uri = data.getData();

            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Uploading image, please wait!");
            progressDialog.setCancelable(false);
            progressDialog.show();

            final String unique_name = UUID.randomUUID().toString();
            StorageReference file_path = mStorage.child("question_papers").child(unique_name);
            file_path.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    DatabaseReference user_profile_reg = FirebaseDatabase.getInstance().getReference("user_profile").child(mAuth.getCurrentUser().getUid());
                    user_profile_reg.child("uploaded_papers").push().setValue(unique_name);

                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "The image has been uploaded!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "The image could not be uploaded. Try again!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        else if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK){

            // Getting the bitmap instead of URI
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] dataBAOS = baos.toByteArray();

            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Uploading image, please wait!");
            progressDialog.setCancelable(false);
            progressDialog.show();

            final String unique_name = UUID.randomUUID().toString();
            StorageReference file_path = mStorage.child("question_papers").child(unique_name);
            UploadTask upload_image = file_path.putBytes(dataBAOS);

            upload_image.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    DatabaseReference user_profile_reg = FirebaseDatabase.getInstance().getReference("user_profile").child(mAuth.getCurrentUser().getUid());
                    user_profile_reg.child("uploaded_papers").push().setValue(unique_name);

                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "The image has been uploaded!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "The image could not be uploaded. Try again!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
