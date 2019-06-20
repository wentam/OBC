package us.egeler.matt.obc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BoltKeyPressListener {
    BroadcastReceiver receiver;
    boolean ignoreInput = false;
    Context context;

    BoltKeyPressListener(Context c) {
        context = c;
    }

    protected void onKeyAction(String action) {
        // Override me!
    }

    public void start() {
        IntentFilter filter = new IntentFilter("com.wahoofitness.bolt.buttons.center_button.pressed");
        filter.addAction("com.wahoofitness.bolt.buttons.center_button.down");
        filter.addAction("com.wahoofitness.bolt.buttons.center_button.up");
        filter.addAction("com.wahoofitness.bolt.buttons.center_button.long_prossed");
        filter.addAction("com.wahoofitness.bolt.buttons.left_button.down");
        filter.addAction("com.wahoofitness.bolt.buttons.left_button.up");
        filter.addAction("com.wahoofitness.bolt.buttons.left_button.pressed");
        filter.addAction("com.wahoofitness.bolt.buttons.left_button.long_pressed");
        filter.addAction("com.wahoofitness.bolt.buttons.right_button.down");
        filter.addAction("com.wahoofitness.bolt.buttons.right_button.up");
        filter.addAction("com.wahoofitness.bolt.buttons.right_button.pressed");
        filter.addAction("com.wahoofitness.bolt.buttons.right_button.long_pressed");
        filter.addAction("com.wahoofitness.bolt.buttons.up_button.down");
        filter.addAction("com.wahoofitness.bolt.buttons.up_button.up");
        filter.addAction("com.wahoofitness.bolt.buttons.up_button.pressed");
        filter.addAction("com.wahoofitness.bolt.buttons.up_button.long_pressed");
        filter.addAction("com.wahoofitness.bolt.buttons.down_button.down");
        filter.addAction("com.wahoofitness.bolt.buttons.down_button.up");
        filter.addAction("com.wahoofitness.bolt.buttons.down_button.pressed");
        filter.addAction("com.wahoofitness.bolt.buttons.down_button.long_pressed");
        filter.addAction("com.wahoofitness.bolt.buttons.power_button.down");
        filter.addAction("com.wahoofitness.bolt.buttons.power_button.up");
        filter.addAction("com.wahoofitness.bolt.buttons.power_button.pressed");
        filter.addAction("com.wahoofitness.bolt.buttons.power_button.long_pressed");

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ignoreInput == false) {
                    onKeyAction(intent.getAction().split("\\.")[4] + "." + intent.getAction().split("\\.")[5]);
                }
            }
        };
        context.registerReceiver(receiver, filter);
    }

    public void pause(){
        ignoreInput = true;
    }

    public void resume(){
        ignoreInput = false;
    }

    public void stop() {
        context.unregisterReceiver(receiver);
    }
}
