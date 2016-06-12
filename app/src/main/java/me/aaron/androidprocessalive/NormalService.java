package me.aaron.androidprocessalive;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class NormalService extends Service {

    public static void actionStart(Context context) {
        context.startService(new Intent(context, NormalService.class));
    }

    public NormalService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
