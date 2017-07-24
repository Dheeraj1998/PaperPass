package com.example.paperpass;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static android.R.attr.bitmap;
import static android.R.attr.onClick;

public class Dashboard extends AppCompatActivity {

    private static final int GALLERY_INTENT = 2;
    private static final int CAMERA_REQUEST_CODE = 3;
    private static StorageReference mStorage;
    final Context thisContext = this;
    FirebaseAuth mAuth;
    String Course_Code, user_name, image_url, course_code;
    private RecyclerView qp_list;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

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
        qp_list.setLayoutManager(new LinearLayoutManager(this));

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

        // Creating the Firebase Recycler Adapter
        FirebaseRecyclerAdapter<QP_Post, QP_Post_Holder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<QP_Post, QP_Post_Holder>(
                QP_Post.class,
                R.layout.qp_row,
                QP_Post_Holder.class,
                mDatabase) {

            @Override
            protected void populateViewHolder(QP_Post_Holder viewHolder, QP_Post model, final int position) {
                viewHolder.setCourse_Code(model.getCourse_Code());
                viewHolder.setUser_Id(model.getUser_Id());
                viewHolder.setImage(getApplicationContext(), model.getImage_Url());

                viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String post_key = getRef(position).getKey();

                        mDatabase.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                image_url = dataSnapshot.child(post_key).child("Image_Url").getValue().toString();
                                course_code = dataSnapshot.child(post_key).child("Course_Code").getValue().toString();
                                open_downloadPage();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
//                        Do nothing!
                            }
                        });
                    }
                });
            }
        };

        qp_list.setAdapter(firebaseRecyclerAdapter);
    }

    public void open_downloadPage(){
        try {
            if (!image_url.isEmpty()) {
                Intent temp = new Intent(Dashboard.this, DownloadActivity.class);
                temp.putExtra("image_url", image_url);
                temp.putExtra("course_code", course_code);

                image_url = "";
                course_code = "";

                startActivity(temp);
            } else {
                Toast.makeText(getApplicationContext(), "Please try after some time!", Toast.LENGTH_SHORT).show();
            }
        }

        catch (Exception error){
            Toast.makeText(getApplicationContext(), "Please wait!", Toast.LENGTH_SHORT).show();
        }
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
            Uri uri = data.getData();

            InputStream imageStream = null;
            try {
                imageStream = getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            Bitmap bmp = BitmapFactory.decodeStream(imageStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
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

                                    Course_Code = userInput.getText().toString();
                                    if (subjects_list.contains(Course_Code)) {
                                        final ProgressDialog progressDialog = new ProgressDialog(Dashboard.this);
                                        progressDialog.setIndeterminate(true);
                                        progressDialog.setMessage("Uploading image, please wait!");
                                        progressDialog.setCancelable(false);
                                        progressDialog.show();

                                        final String unique_name = UUID.randomUUID().toString();
                                        final StorageReference file_path = mStorage.child("question_papers").child(unique_name);
                                        UploadTask upload_image = file_path.putBytes(dataBAOS);

                                        upload_image.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                DatabaseReference user_profile_reg = FirebaseDatabase.getInstance().getReference("user_profile").child(mAuth.getCurrentUser().getUid());
                                                user_profile_reg.child("uploaded_papers").push().setValue(unique_name);

                                                InputMethodManager in = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                                                in.hideSoftInputFromWindow(userInput.getWindowToken(), 0);

                                                @SuppressWarnings("VisibleForTests") Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                                DatabaseReference question_paper_reg = FirebaseDatabase.getInstance().getReference("all_uploaded_papers");
                                                String push_key = question_paper_reg.push().getKey();
                                                question_paper_reg.child(push_key).child("User_Id").setValue(user_name);
                                                question_paper_reg.child(push_key).child("Image_Id").setValue(unique_name);
                                                question_paper_reg.child(push_key).child("Image_Url").setValue(downloadUrl.toString());
                                                question_paper_reg.child(push_key).child("Course_Code").setValue(Course_Code);

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

                                    Course_Code = userInput.getText().toString();
                                    if (subjects_list.contains(Course_Code)) {
                                        final ProgressDialog progressDialog = new ProgressDialog(Dashboard.this);
                                        progressDialog.setIndeterminate(true);
                                        progressDialog.setMessage("Uploading image, please wait!");
                                        progressDialog.setCancelable(false);
                                        progressDialog.show();

                                        final String unique_name = UUID.randomUUID().toString();
                                        final StorageReference file_path = mStorage.child("question_papers").child(unique_name);
                                        UploadTask upload_image = file_path.putBytes(dataBAOS);

                                        upload_image.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                DatabaseReference user_profile_reg = FirebaseDatabase.getInstance().getReference("user_profile").child(mAuth.getCurrentUser().getUid());
                                                user_profile_reg.child("uploaded_papers").push().setValue(unique_name);

                                                InputMethodManager in = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                                                in.hideSoftInputFromWindow(userInput.getWindowToken(), 0);

                                                @SuppressWarnings("VisibleForTests") Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                                DatabaseReference question_paper_reg = FirebaseDatabase.getInstance().getReference("all_uploaded_papers");
                                                String push_key = question_paper_reg.push().getKey();
                                                question_paper_reg.child(push_key).child("User_Id").setValue(user_name);
                                                question_paper_reg.child(push_key).child("Image_Id").setValue(unique_name);
                                                question_paper_reg.child(push_key).child("Image_Url").setValue(downloadUrl.toString());
                                                question_paper_reg.child(push_key).child("Course_Code").setValue(Course_Code);

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
            Picasso.with(ctx).load(image).into(qp_image);
        }
    }
}
