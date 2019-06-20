package us.egeler.matt.obc;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Window;
import android.view.WindowManager;

import us.egeler.matt.obc.page.MainPage;
import us.egeler.matt.obc.page.Page;
import us.egeler.matt.obc.page.TestPage;


public class MainActivity extends FragmentActivity {
    MyBoltKeyPressListener boltKeyPressListener;
    MyFragmentPagerAdapter myFragmentPagerAdapter;
    ViewPager pager;

    private void setScreenBrightness(int brightness) {
        // get the content resolver
        ContentResolver cResolver = getContentResolver();

        // get the current window
        Window window = getWindow();

        Settings.System.putInt(cResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);

        // get the current window attributes
        WindowManager.LayoutParams layoutpars = window.getAttributes();
        // set the brightness of this window
        layoutpars.screenBrightness = brightness;
        // apply attribute changes to this window
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

        // set the screen brightness
        setScreenBrightness(5);

        // create key press listener and start listening for key presses
        boltKeyPressListener = new MyBoltKeyPressListener(this);
        boltKeyPressListener.start();

        // set up viewPager
        myFragmentPagerAdapter = new MyFragmentPagerAdapter(getSupportFragmentManager());
        pager = (ViewPager) findViewById(R.id.viewPager);
        pager.setAdapter(myFragmentPagerAdapter);
    }

    public static class MyFragmentPagerAdapter extends FragmentPagerAdapter {
        private Page page0;
        private Page page1;

        public MyFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
            page0 = new MainPage();
            page1 = new TestPage();
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return page0;
            } else {
                return page1;
            }
        }
    }

    protected class MyBoltKeyPressListener extends BoltKeyPressListener {
        private Context context;

        MyBoltKeyPressListener(Context c) {
            super(c);
            context = c;
        }

        @Override
        protected void onKeyAction(String action) {
            if (action.equals("power_button.long_pressed")) {
                // power button long-pressed. Shut down the device.
                Intent intent=new Intent("com.wahoofitness.bolt.system.shutdown");
                context.sendBroadcast(intent);
            } else if (action.equals("power_button.pressed")) {
                // power button pressed. Open settings menu.
                Intent intent = new Intent(context, us.egeler.matt.obc.SettingsActivity.class);
                context.startActivity(intent);
            } else if (action.equals("right_button.pressed")) {
                int currentItem = pager.getCurrentItem();
                int itemCount = myFragmentPagerAdapter.getCount();

                int targetItem = currentItem+1;
                if (targetItem >= itemCount) {
                    targetItem = 0;
                }

                pager.setCurrentItem(targetItem,true);
            } else {
                // we didn't use the event. pass event to our pages.
                for (int i = 0; i < myFragmentPagerAdapter.getCount(); i++) {
                    ((Page) myFragmentPagerAdapter.getItem(i)).triggerKeyAction(action);
                }
            }
        }
    }
}
