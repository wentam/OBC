package us.egeler.matt.obc.page;

import android.support.v4.app.Fragment;

public class Page extends Fragment {
    public void triggerKeyAction(String action) {
        onKeyAction(action);
    }

    protected boolean onKeyAction(String action) {
        // Override me!
        // Return true if you have consumed the event
        return false;
    }
}
