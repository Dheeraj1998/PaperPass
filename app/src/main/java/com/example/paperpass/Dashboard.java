package com.example.paperpass;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Dashboard extends AppCompatActivity {

    private static final int GALLERY_INTENT = 2;
    private static final int CAMERA_REQUEST_CODE = 3;
    final Context thisContext = this;
    FirebaseAuth mAuth;
    String course_code;
    private StorageReference mStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mStorage = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Customizing the action bar
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.dashboard_actionbar);

        View view = getSupportActionBar().getCustomView();

        // Actions related to the different buttons on the action bar
        ImageButton imageButton = (ImageButton) view.findViewById(R.id.settings);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent temp = new Intent(Dashboard.this, Settings.class);
                startActivity(temp);
            }
        });
    }

    public void uploadPicture(View v) {
        Intent temp = new Intent(Intent.ACTION_PICK);
        temp.setType("image/*");
        startActivityForResult(temp, GALLERY_INTENT);
    }

    public void takePicture(View v) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_INTENT && resultCode == RESULT_OK) {
            final Uri uri = data.getData();

            // Setting up the custom input dialog box
            LayoutInflater li = LayoutInflater.from(thisContext);
            View dialogView = li.inflate(R.layout.user_input_dialog, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(thisContext);
            alertDialogBuilder.setTitle("Paper Details");
            alertDialogBuilder.setView(dialogView);
            final AutoCompleteTextView userInput = (AutoCompleteTextView) dialogView.findViewById(R.id.coursecode);

            // Setting up the autocomplete in the dialog box
            String[] subjects = getResources().getStringArray(R.array.subjects_array);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, subjects);
            final List<String> subjects_list = Arrays.asList(subjects);
            userInput.setAdapter(adapter);

            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton("Confirm",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    course_code = userInput.getText().toString();
                                    if (subjects_list.contains(course_code)) {

                                        final ProgressDialog progressDialog = new ProgressDialog(Dashboard.this);
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

                                                DatabaseReference question_paper_reg = FirebaseDatabase.getInstance().getReference("all_uploaded_papers");
                                                question_paper_reg.child(unique_name).child("Course Code").setValue(course_code);

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
                                    } else {
                                        Toast.makeText(Dashboard.this, "Select appropriate option!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            })

                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    Toast.makeText(getApplicationContext(), "Upload has been cancelled.", Toast.LENGTH_SHORT).show();
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

        } else if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {

            // Getting the bitmap instead of URI
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            final byte[] dataBAOS = baos.toByteArray();

            // Setting up the custom input dialog box
            LayoutInflater li = LayoutInflater.from(thisContext);
            View dialogView = li.inflate(R.layout.user_input_dialog, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(thisContext);
            alertDialogBuilder.setTitle("Paper Details");
            alertDialogBuilder.setView(dialogView);
            final AutoCompleteTextView userInput = (AutoCompleteTextView) dialogView.findViewById(R.id.coursecode);

            // Setting up the autocomplete in the dialog box
            final String[] subjects = getResources().getStringArray(R.array.subjects_array);
            final List<String> subjects_list = Arrays.asList(subjects);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, subjects);
            userInput.setAdapter(adapter);

            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton("Confirm",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    course_code = userInput.getText().toString();
                                    if (subjects_list.contains(course_code)) {
                                        final ProgressDialog progressDialog = new ProgressDialog(Dashboard.this);
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

                                                DatabaseReference question_paper_reg = FirebaseDatabase.getInstance().getReference("all_uploaded_papers");
                                                question_paper_reg.child(unique_name).child("Course Code").setValue(course_code);

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
                                    } else {
                                        Toast.makeText(Dashboard.this, "Select appropriate option!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }).setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            Toast.makeText(getApplicationContext(), "Upload has been cancelled.", Toast.LENGTH_SHORT).show();
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
}
