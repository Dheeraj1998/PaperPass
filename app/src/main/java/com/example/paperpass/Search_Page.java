package com.example.paperpass;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;

public class Search_Page extends AppCompatActivity {

    String selected_course = "", image_url, course_code;
    private RecyclerView qp_list;
    private Query mQuery;
    private DatabaseReference mDatabase;
    int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search__page);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("all_uploaded_papers");

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.search_actionbar);

        View view = getSupportActionBar().getCustomView();
        final AutoCompleteTextView userInput = (AutoCompleteTextView) view.findViewById(R.id.search_papers);

        ImageButton imageButton = (ImageButton) view.findViewById(R.id.search_back);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!userInput.getText().toString().equals("")) {
                    userInput.setText("");
                } else {
                    Intent temp = new Intent(Search_Page.this, Dashboard.class);
                    startActivity(temp);
                    finish();
                }
            }
        });

        userInput.setDropDownBackgroundResource(R.color.cardview_light_background);

        // Setting up the autocomplete in the dialog box
        String[] subjects = getResources().getStringArray(R.array.subjects_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, subjects);
        userInput.setAdapter(adapter);

        userInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selected_course = parent.getItemAtPosition(position).toString();
                searchResults();
            }
        });

        qp_list = (RecyclerView) findViewById(R.id.search_list);
        qp_list.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);

        qp_list.setLayoutManager(layoutManager);
    }

    public void searchResults() {
        mQuery = FirebaseDatabase.getInstance().getReference().child("all_uploaded_papers").orderByChild("Course_Code").equalTo(selected_course);

        // Creating the Firebase Recycler Adapter
        FirebaseRecyclerAdapter<QP_Post, QP_Post_Holder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<QP_Post, QP_Post_Holder>(
                QP_Post.class,
                R.layout.qp_row,
                QP_Post_Holder.class,
                mQuery) {

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

                                final ProgressDialog progressDialog = new ProgressDialog(Search_Page.this);
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
                                        Toast.makeText(getApplicationContext(), "Download successful!", Toast.LENGTH_SHORT).show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        progressDialog.dismiss();
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
            }
        };

        qp_list.setAdapter(firebaseRecyclerAdapter);
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

    public static class QP_Post_Holder extends RecyclerView.ViewHolder {
        View mView;

        public QP_Post_Holder(View itemView) {
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
