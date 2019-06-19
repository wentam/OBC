package us.egeler.matt.obc;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {
    MyBoltKeyPressListener boltKeyPressListener;

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

    @Override
    protected void onPause() {
        boltKeyPressListener.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        boltKeyPressListener.resume();
        setScreenBrightness(5);
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((TextView) findViewById(R.id.textView)).getPaint().setAntiAlias(false);



        // set the screen brightness
        setScreenBrightness(5);

        //create key press listener and start listening for key presses
        boltKeyPressListener = new MyBoltKeyPressListener(this);
        boltKeyPressListener.start();
    }

    protected class MyBoltKeyPressListener extends BoltKeyPressListener {
        private Context context;

        MyBoltKeyPressListener(Context c) {
            super(c);
            context = c;
        }

        @Override
        public void onKeyAction(String action) {
            if (action.equals("power_button.long_pressed")) {
                Log.d("OBC","Power button long press, trying to shut down...");
                Intent intent=new Intent("com.wahoofitness.bolt.system.shutdown");
                context.sendBroadcast(intent);
            } else if (action.equals("power_button.pressed")) {
                Intent intent=new Intent(context, us.egeler.matt.obc.SettingsActivity.class);
                context.startActivity(intent);
            }

            TextView helloworld = (TextView) ((Activity) context).findViewById(R.id.helloworld);
            helloworld.setText(action);
        }
    }
}
