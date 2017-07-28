package com.example.paperpass;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Dashboard extends AppCompatActivity {

    private static final int GALLERY_INTENT = 2;
    private static final int CAMERA_REQUEST_CODE = 3;
    private static StorageReference mStorage;
    final Context thisContext = this;
    FirebaseAuth mAuth;
    String Course_Code, user_name, image_url, course_code, image_uuid, user_fcm;
    int count = 0;
    ProgressDialog progressDialog;
    private RecyclerView qp_list;
    private DatabaseReference mDatabase, rDatabase;
    private Uri camera_image_uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        count = 0;
        mStorage = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        DatabaseReference myRootRef = FirebaseDatabase.getInstance().getReference("user_profile/" + mAuth.getCurrentUser().getUid());

        myRootRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                user_profile UProfile = dataSnapshot.getValue(user_profile.class);
                user_name = UProfile.getName();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        qp_list = (RecyclerView) findViewById(R.id.qp_list);
        qp_list.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);

        qp_list.setLayoutManager(layoutManager);

//         Customizing the action bar
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.dashboard_actionbar);

        View view = getSupportActionBar().getCustomView();

//         Actions related to the different buttons on the action bar
        ImageButton imageButton = (ImageButton) view.findViewById(R.id.settings);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent temp = new Intent(Dashboard.this, Settings.class);
                temp.putExtra("user_name", user_name);
                startActivity(temp);
            }
        });

        ImageButton imageButton1 = (ImageButton) view.findViewById(R.id.search);
        imageButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent temp = new Intent(Dashboard.this, Search_Page.class);
                startActivity(temp);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        mDatabase = FirebaseDatabase.getInstance().getReference().child("all_uploaded_papers");
        rDatabase = FirebaseDatabase.getInstance().getReference().child("reported_posts");

        // Creating the Firebase Recycler Adapter
        FirebaseRecyclerAdapter<QP_Post, QP_Post_Holder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<QP_Post, QP_Post_Holder>(
                QP_Post.class,
                R.layout.qp_row,
                QP_Post_Holder.class,
                mDatabase) {

            @Override
            protected void populateViewHolder(final QP_Post_Holder viewHolder, final QP_Post model, final int position) {
                viewHolder.setCourse_Code(model.getCourse_Code());
                viewHolder.setUser_Id(model.getUser_Id());
                viewHolder.setImage(getApplicationContext(), model.getImage_Url());

                viewHolder.mView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        final String post_key = getRef(position).getKey();

                        // Setting up the custom input dialog box
                        LayoutInflater li = LayoutInflater.from(thisContext);
                        View dialogView = li.inflate(R.layout.post_longclick, null);
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(thisContext);
                        alertDialogBuilder.setTitle("Options");
                        alertDialogBuilder.setView(dialogView);
                        final AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();

                        Button download_button = (Button) dialogView.findViewById(R.id.post_download);
                        download_button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Code for downloading the picture
                                mDatabase.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        image_url = dataSnapshot.child(post_key).child("Image_Url").getValue().toString();
                                        course_code = dataSnapshot.child(post_key).child("Course_Code").getValue().toString();

                                        progressDialog = new ProgressDialog(Dashboard.this);
                                        progressDialog.setIndeterminate(true);
                                        progressDialog.setMessage("Downloading picture...");
                                        progressDialog.setCancelable(false);
                                        progressDialog.show();

                                        FirebaseStorage storage = FirebaseStorage.getInstance();
                                        StorageReference storageRef = storage.getReferenceFromUrl(image_url);

                                        File storagePath = new File(Environment.getExternalStorageDirectory(), "Paper Pass");

                                        if (!storagePath.exists()) {
                                            storagePath.mkdirs();
                                        }

                                        final File myFile = new File(storagePath, course_code + "_" + count + ".png");
                                        count++;

                                        storageRef.getFile(myFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                            @Override
                                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                                progressDialog.dismiss();
                                                alertDialog.dismiss();
                                                Toast.makeText(getApplicationContext(), "Download successful!", Toast.LENGTH_SHORT).show();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception exception) {
                                                progressDialog.dismiss();
                                                alertDialog.dismiss();
                                                Toast.makeText(getApplicationContext(), "Download failed!", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
//                        Do nothing!
                                    }
                                });
                            }
                        });

                        Button report_button = (Button) dialogView.findViewById(R.id.post_report);
                        report_button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Code for adding an entry in the reported database
                                mDatabase.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        image_url = dataSnapshot.child(post_key).child("Image_Url").getValue().toString();
                                        image_uuid = dataSnapshot.child(post_key).child("Image_Id").getValue().toString();
                                        user_fcm = FirebaseInstanceId.getInstance().getToken();

                                        rDatabase.child(post_key).child("Image_Url").setValue(image_url);
                                        rDatabase.child(post_key).child("Image_UUId").setValue(image_uuid);
                                        rDatabase.child(post_key).child("Image_Id").setValue(post_key);
                                        rDatabase.child(post_key).child("User_FCM").setValue(user_fcm);

                                        alertDialog.dismiss();
                                        Toast.makeText(getApplicationContext(), "The post has been reported!", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        alertDialog.dismiss();
                                        Toast.makeText(getApplicationContext(), "Error. Please try again later!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });

                        return false;
                    }
                });
            }
        };

        qp_list.setAdapter(firebaseRecyclerAdapter);
    }

    // Methods for the upload of picture from gallery and taking picture from camera
    public void uploadPicture(View v) {
        Intent temp = new Intent(Intent.ACTION_PICK);
        temp.setType("image/*");
        startActivityForResult(temp, GALLERY_INTENT);
    }

    public void takePicture(View v) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                File image_file = this.createTemporaryFile("test", ".png");
                camera_image_uri = Uri.fromFile(image_file);

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, camera_image_uri);

                startActivityForResult(intent, CAMERA_REQUEST_CODE);
            } catch (Exception error) {

            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (FirebaseDatabase.getInstance() != null) {
            FirebaseDatabase.getInstance().goOnline();
        }
    }

    @Override
    public void onPause() {

        super.onPause();

        if (FirebaseDatabase.getInstance() != null) {
            FirebaseDatabase.getInstance().goOffline();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_INTENT && resultCode == RESULT_OK) {
            // Getting high-res images
            final Uri uri = data.getData();

            // Setting up the custom input dialog box
            LayoutInflater li = LayoutInflater.from(thisContext);
            View dialogView = li.inflate(R.layout.upload_coursecode, null);
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

                                    Course_Code = userInput.getText().toString();
                                    if (subjects_list.contains(Course_Code)) {
                                        final ProgressDialog progressDialog = new ProgressDialog(Dashboard.this);
                                        progressDialog.setIndeterminate(true);
                                        progressDialog.setCancelable(false);
                                        progressDialog.show();

                                        final String unique_name = UUID.randomUUID().toString();
                                        final StorageReference file_path = mStorage.child("question_papers").child(unique_name);
                                        UploadTask upload_image = file_path.putFile(uri);

                                        upload_image.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                @SuppressWarnings("VisibleForTests") Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                                DatabaseReference question_paper_reg = FirebaseDatabase.getInstance().getReference("all_uploaded_papers");
                                                String push_key = question_paper_reg.push().getKey();
                                                question_paper_reg.child(push_key).child("User_Id").setValue(user_name);
                                                question_paper_reg.child(push_key).child("Image_Id").setValue(unique_name);
                                                question_paper_reg.child(push_key).child("Image_Url").setValue(downloadUrl.toString());
                                                question_paper_reg.child(push_key).child("Course_Code").setValue(Course_Code);
                                                question_paper_reg.child(push_key).child("User_UUID").setValue(mAuth.getCurrentUser().getUid());

                                                progressDialog.dismiss();
                                                Toast.makeText(getApplicationContext(), "Upload was successful!", Toast.LENGTH_SHORT).show();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.dismiss();
                                                Toast.makeText(getApplicationContext(), "Upload failed. Try again!", Toast.LENGTH_SHORT).show();
                                            }
                                        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                                @SuppressWarnings("VisibleForTests") double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                                progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
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


        } else if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {

            // Setting up the custom input dialog box
            LayoutInflater li = LayoutInflater.from(thisContext);
            View dialogView = li.inflate(R.layout.upload_coursecode, null);
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

                                    Course_Code = userInput.getText().toString();
                                    if (subjects_list.contains(Course_Code)) {
                                        final ProgressDialog progressDialog = new ProgressDialog(Dashboard.this);
                                        progressDialog.setIndeterminate(true);
                                        progressDialog.setMessage("Uploading image, please wait!");
                                        progressDialog.setCancelable(false);
                                        progressDialog.show();

                                        final String unique_name = UUID.randomUUID().toString();
                                        final StorageReference file_path = mStorage.child("question_papers").child(unique_name);
                                        UploadTask upload_image = file_path.putFile(camera_image_uri);

                                        upload_image.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                @SuppressWarnings("VisibleForTests") Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                                DatabaseReference question_paper_reg = FirebaseDatabase.getInstance().getReference("all_uploaded_papers");
                                                String push_key = question_paper_reg.push().getKey();
                                                question_paper_reg.child(push_key).child("User_Id").setValue(user_name);
                                                question_paper_reg.child(push_key).child("Image_Id").setValue(unique_name);
                                                question_paper_reg.child(push_key).child("Image_Url").setValue(downloadUrl.toString());
                                                question_paper_reg.child(push_key).child("Course_Code").setValue(Course_Code);
                                                question_paper_reg.child(push_key).child("User_UUID").setValue(mAuth.getCurrentUser().getUid());

                                                progressDialog.dismiss();
                                                Toast.makeText(getApplicationContext(), "Upload was successful!", Toast.LENGTH_SHORT).show();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.dismiss();
                                                Toast.makeText(getApplicationContext(), "Upload failed. Try again!", Toast.LENGTH_SHORT).show();
                                            }
                                        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                                @SuppressWarnings("VisibleForTests") double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                                progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
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

    // Method to create temporary file during the taking the picture from camera
    private File createTemporaryFile(String part, String ext) throws Exception {
        File tempDir = Environment.getExternalStorageDirectory();
        tempDir = new File(tempDir.getAbsolutePath() + "/.temp/");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        return File.createTempFile(part, ext, tempDir);
    }

    public static class QP_Post_Holder extends RecyclerView.ViewHolder {
        View mView;

        public QP_Post_Holder(final View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setCourse_Code(String courseCode) {
            TextView qp_coursecode = (TextView) mView.findViewById(R.id.qp_coursecode);
            qp_coursecode.setText(courseCode);
        }

        public void setUser_Id(String user) {
            TextView qp_user = (TextView) mView.findViewById(R.id.qp_username);
            qp_user.setText(user);
        }

        public void setImage(Context ctx, String image) {
            ImageView qp_image = (ImageView) mView.findViewById(R.id.qp_image);
            Picasso.with(ctx).load(image).resize(100, 100).into(qp_image);
        }
    }
}
