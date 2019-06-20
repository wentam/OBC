package us.egeler.matt.obc.page;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import us.egeler.matt.obc.R;

// This is a static page. In the future, all pages should be generated from a config file.
public class MainPage extends Page {
    View view;

    @Override
    public void onCreate(Bundle savedinstancestate) {
        super.onCreate(savedinstancestate);
        //((TextView) view.findViewById(R.id.textView)).getPaint().setAntiAlias(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_mainpage,null);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected boolean onKeyAction(String action) {
        TextView helloworld = view.findViewById(R.id.helloworld);
        helloworld.setText(action);

        return false;
    }
}
