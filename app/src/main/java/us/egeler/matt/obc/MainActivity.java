package us.egeler.matt.obc;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import us.egeler.matt.obc.mapDataManager.mapProjection.MapProjection;
import us.egeler.matt.obc.mapDataManager.mapProjection.Mercator;
import us.egeler.matt.obc.mapDataManager.mapsForgeDataWriter.MapsForgeDataWriter;
import us.egeler.matt.obc.mapDataManager.osmXmlReader.DirectOsmXmlReader;
import us.egeler.matt.obc.page.MainPage;
import us.egeler.matt.obc.page.MapPage;
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
        pager.setOffscreenPageLimit(5);

        // projection test
        MapProjection p = new Mercator(100, 100);

       /* double x = p.lonToX(10);
        Log.i("OBCL","lon to x = "+x);
        double lon = p.xToLon(x);
        Log.i("OBCL","x to lon = "+lon);*/
       /* double y = p.latToY(43);
        Log.i("OBCL","lat 43 to y = "+y);
        double lat = p.yToLat(y);
        Log.i("OBCL","y "+y+" to lat  = "+lat);*/

        // OsmXmlReader test
        class MyOutputStream extends OutputStream {
            public String out = "";

            @Override
            public void write(int b) throws IOException {
                out = out + ((char) b);
            }
        }

        MyOutputStream mos = new MyOutputStream();

        MapsForgeDataWriter writer;

        try {
            File Root = Environment.getExternalStorageDirectory();
            File Dir = new File(Root.getAbsolutePath() + "/MapsForgeWriterCache");

            File infile = new File(Dir, "bigmap.osm");


            writer = new MapsForgeDataWriter(mos, Dir);
            writer.writeFromOSMXML(infile);
            writer.close();
        } catch (Exception e) {
            Log.e("OBC","Bad:",e);
        }
/*
        try {
            File Root = Environment.getExternalStorageDirectory();
            File Dir = new File(Root.getAbsolutePath() + "/MapsForgeWriterCache");

            File infile = new File(Dir, "smallmap.osm");


            DirectOsmXmlReader r = new DirectOsmXmlReader();
            r.read(new FileInputStream(infile));
        } catch (Exception e) {
            Log.e("OBC","Bad:",e);
        }*/


    }

    public static class MyFragmentPagerAdapter extends FragmentPagerAdapter {
        private Page page0;
        private Page page1;
        private Page page2;

        public MyFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
            page0 = new MainPage();
            page1 = new TestPage();
            page2 = new MapPage();
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return page0;
            } else if (position == 1) {
                return page1;
            } else {
                return page2;
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
