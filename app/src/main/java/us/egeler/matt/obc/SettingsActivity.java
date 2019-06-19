package us.egeler.matt.obc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

public class SettingsActivity extends AppCompatActivity {
    MyBoltKeyPressListener boltKeyPressListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        boltKeyPressListener = new MyBoltKeyPressListener(this);
        boltKeyPressListener.start();
    }

    @Override
    protected void onPause() {
        boltKeyPressListener.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        boltKeyPressListener.resume();
        super.onResume();
    }

    protected class MyBoltKeyPressListener extends BoltKeyPressListener {
        private Context context;

        MyBoltKeyPressListener(Context c) {
            super(c);
            context = c;
        }

        @Override
        protected void onKeyAction(String action) {
            if (action.equals("power_button.pressed")) {
                ((Activity) context).finish();
            }
        }
    }

}
