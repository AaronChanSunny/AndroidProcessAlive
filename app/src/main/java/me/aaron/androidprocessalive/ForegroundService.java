package me.aaron.androidprocessalive;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class ForegroundService extends Service {

    private static final int FOREGROUND_SERVICE_ID = -2222;

    public static void actionStart(Context context) {
        context.startService(new Intent(context, ForegroundService.class));
    }

    public ForegroundService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Notification notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_sms_white_24dp)
                    .build();
            startForeground(FOREGROUND_SERVICE_ID, notification);
        } else {
            startForeground(FOREGROUND_SERVICE_ID, new Notification());
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
