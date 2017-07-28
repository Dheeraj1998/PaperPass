package com.example.paperpass;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by dheeraj on 27/07/17.
 */

public class FCMService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder nc_builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.app_logo_white)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setContentTitle((getResources().getString(R.string.app_name)))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(remoteMessage.getNotification().getBody()))
                .setAutoCancel(true)
                .setSound(soundUri);

        NotificationManager n_manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        try {
            // Code for opening browser for the updated version download
            if (remoteMessage.getData().get("action").equals("download")) {
                String download_url = remoteMessage.getData().get("download_url");

                Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(download_url));
                PendingIntent pending_intent = PendingIntent.getActivity(this, 0, notificationIntent, Intent.FILL_IN_ACTION);
                nc_builder.setContentIntent(pending_intent);
            }
        } catch (Exception error) {
            // Do nothing!
        }

        n_manager.notify(0, nc_builder.build());
    }

}
