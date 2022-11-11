package com.jjasan2.project_4;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

public class Player1Thread extends Thread{
    public Handler p1Handler;

    @Override
    public void run() {
        // Create looper if one not exists for the current thread
        if(Looper.myLooper() == null) {
            Looper.prepare();
        }

        p1Handler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Log.i("appDebug", "Player 1 Handle message");
            }
        };
        Looper.loop();

        Log.i("appDebug", "Player 1 run");
    }
}
