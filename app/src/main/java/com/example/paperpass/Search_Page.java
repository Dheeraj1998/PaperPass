package com.example.paperpass;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.squareup.picasso.Picasso;

public class Search_Page extends AppCompatActivity {

    private RecyclerView qp_list;
    private Query mQuery;
    String selected_course = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search__page);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.search_actionbar);

        View view = getSupportActionBar().getCustomView();
        final AutoCompleteTextView userInput = (AutoCompleteTextView) view.findViewById(R.id.search_papers);

        ImageButton imageButton = (ImageButton) view.findViewById(R.id.search_back);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!userInput.getText().toString().equals("")){
                    userInput.setText("");
                }

                else {
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
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                searchResults();
            }
        });

        qp_list = (RecyclerView) findViewById(R.id.search_list);
        qp_list.setHasFixedSize(true);
        qp_list.setLayoutManager(new LinearLayoutManager(this));
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
            protected void populateViewHolder(QP_Post_Holder viewHolder, QP_Post model, int position) {
                viewHolder.setCourse_Code(model.getCourse_Code());
                viewHolder.setUser_Id(model.getUser_Id());
                viewHolder.setImage(getApplicationContext(), model.getImage_Url());
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
            qp_user.setText(user);
        }

        public void setImage(Context ctx, String image) {
            ImageView qp_image = (ImageView) mView.findViewById(R.id.qp_image);
            Picasso.with(ctx).load(image).into(qp_image);
        }
    }
}
