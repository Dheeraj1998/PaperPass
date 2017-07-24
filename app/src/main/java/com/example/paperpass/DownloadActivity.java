package com.example.paperpass;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import android.support.v7.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import static android.R.attr.id;

public class DownloadActivity extends AppCompatActivity {

    private String image_url, course_code;
    int count = 1;
    boolean picture_check = false;
    Button download_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        final ProgressBar mProgressSpin = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressSpin.getIndeterminateDrawable()
                .setColorFilter(ContextCompat.getColor(this, R.color.progressBar), PorterDuff.Mode.SRC_IN );

        download_button = (Button) findViewById(R.id.download_button);
        image_url = getIntent().getStringExtra("image_url");
        course_code = getIntent().getStringExtra("course_code");

        getIntent().removeExtra("image_url");
        getIntent().removeExtra("course_code");

//        Setting up the image in ImageView
        ImageView download_image = (ImageView) findViewById(R.id.download_image);
        Picasso.with(getApplicationContext()).load(image_url).into(download_image, new Callback() {
            @Override
            public void onSuccess() {
                mProgressSpin.setVisibility(View.INVISIBLE);
                picture_check = true;
                download_button.setText("DOWNLOAD");
                download_button.setBackgroundColor(Color.parseColor("#ff669900"));
            }

            @Override
            public void onError() {
                Toast.makeText(getApplicationContext(), "There was some error. Try later!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void downloadPicture(View v){
        download_button.setText("DOWNLOADING...");
        download_button.setEnabled(false);
        download_button.setBackgroundColor(Color.parseColor("#ff0099cc"));

        if(picture_check) {
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
                    Toast.makeText(getApplicationContext(), "Download successful!", Toast.LENGTH_SHORT).show();
                    download_button.setText("DOWNLOAD");
                    download_button.setBackgroundColor(Color.parseColor("#ff669900"));
                    download_button.setEnabled(true);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(getApplicationContext(), "Download failed!", Toast.LENGTH_SHORT).show();
                    download_button.setText("DOWNLOAD");
                    download_button.setBackgroundColor(Color.parseColor("#ff669900"));
                    download_button.setEnabled(true);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

}
