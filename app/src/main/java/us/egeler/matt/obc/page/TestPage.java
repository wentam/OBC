package us.egeler.matt.obc.page;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import us.egeler.matt.obc.R;

public class TestPage extends Page {
    View view;

    @Override
    public void onCreate(Bundle savedinstancestate) {
        super.onCreate(savedinstancestate);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_testpage,null);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
