package us.egeler.matt.obc.field;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import us.egeler.matt.obc.R;

public class Map extends Field {
    @Override
    public void onCreate(Bundle savedinstancestate) {
        super.onCreate(savedinstancestate);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.field_map,null);
        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
