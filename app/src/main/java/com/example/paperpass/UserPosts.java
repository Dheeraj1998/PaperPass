package com.example.paperpass;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

public class UserPosts extends AppCompatActivity {

    FirebaseAuth mAuth;
    String image_url;
    // Initialising global variables
    private RecyclerView qp_list;
    private DatabaseReference mDatabase;
    private Query mQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_posts);
        mAuth = FirebaseAuth.getInstance();

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.settings_actionbar);

        View view = getSupportActionBar().getCustomView();

        TextView title = (TextView) view.findViewById(R.id.ac_bar_title);
        title.setTextSize(20);
        title.setText("Your Posts");

        ImageButton imageButton = (ImageButton) view.findViewById(R.id.settings_goback);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        qp_list = (RecyclerView) findViewById(R.id.user_posts);
        qp_list.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);

        qp_list.setLayoutManager(layoutManager);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mDatabase = FirebaseDatabase.getInstance().getReference().child("all_uploaded_papers");
        mQuery = FirebaseDatabase.getInstance().getReference().child("all_uploaded_papers").orderByChild("User_UUID").equalTo(mAuth.getCurrentUser().getUid());

        // Creating the Firebase Recycler Adapter
        FirebaseRecyclerAdapter<QP_Post, QP_Post_Holder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<QP_Post, QP_Post_Holder>(
                QP_Post.class,
                R.layout.qp_row,
                QP_Post_Holder.class,
                mQuery) {

            @Override
            protected void populateViewHolder(final QP_Post_Holder viewHolder, final QP_Post model, final int position) {
                viewHolder.setCourse_Code(model.getCourse_Code());
                viewHolder.setUser_Id(model.getUser_Id());
                viewHolder.setImage(getApplicationContext(), model.getImage_Url());

                viewHolder.mView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        final String post_key = getRef(position).getKey();
                        mDatabase = FirebaseDatabase.getInstance().getReference().child("all_uploaded_papers");

                        mDatabase.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                image_url = dataSnapshot.child(post_key).child("Image_Url").getValue().toString();

                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                final StorageReference storageRef = storage.getReferenceFromUrl(image_url);

                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(UserPosts.this);
                                alertDialogBuilder.setTitle("Delete item");
                                alertDialogBuilder.setMessage("Are you sure?");

                                alertDialogBuilder
                                        .setCancelable(false)
                                        .setPositiveButton("Confirm",
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        mDatabase.child(post_key).removeValue();
                                                        storageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Toast.makeText(UserPosts.this, "The post has been deleted!", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
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

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                // Do nothing!
                            }
                        });
                        return false;
                    }

//                            @Override
//                            public void onCancelled(DatabaseError databaseError) {
//                        Do nothing!
//                            }
//                        });
//                    }
                });
            }
        };

        qp_list.setAdapter(firebaseRecyclerAdapter);
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
//            qp_user.setText(user);

            // Setting the User ID as invisible.
            qp_user.setHeight(0);
            qp_user.setVisibility(View.INVISIBLE);
        }

        public void setImage(Context ctx, String image) {
            ImageView qp_image = (ImageView) mView.findViewById(R.id.qp_image);
            Picasso.with(ctx).load(image).resize(200, 200).into(qp_image);
        }
    }
}
