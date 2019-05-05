package us.egeler.matt.obc;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private void setScreenBrightness(int brightness) {
        //Get the content resolver
        ContentResolver cResolver = getContentResolver();

        //Get the current window
        Window window = getWindow();


        Settings.System.putInt(cResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);

        //Get the current window attributes
        WindowManager.LayoutParams layoutpars = window.getAttributes();
        //Set the brightness of this window
        layoutpars.screenBrightness = brightness;
        //Apply attribute changes to this window
        window.setAttributes(layoutpars);
    }

    private void handleKeyPress(String action) {
        if (action.equals("power_button.long_pressed")) {
            Log.d("OBC","Power button long press, trying to shut down...");
            try {
                Process process = Runtime.getRuntime().exec("/system/xbin/su -c am start -a android.intent.action.ACTION_REQUEST_SHUTDOWN");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            } catch (Exception e) {
                Log.e("OBC", "Unable to shut down: ", e);
            }
        }

        TextView helloworld = (TextView) findViewById(R.id.helloworld);
        helloworld.setText(action);

    }

    private void startKeyPressListener() {
        // create broadcastReceiver to listen for button presses
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
                handleKeyPress(intent.getAction().split("\\.")[4] + "." + intent.getAction().split("\\.")[5]);
            }
        };
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        startKeyPressListener();

        // set the screen brightness
        setScreenBrightness(5);


    }
}