package us.egeler.matt.obc.field;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import us.egeler.matt.obc.R;

public class Speed extends Field {
    private LocationManager locationManager;
    private GPSspeed GPSspeed;
    TextView speedText;

    @Override
    public void onCreate(Bundle savedinstancestate) {
       super.onCreate(savedinstancestate);
       locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
       GPSspeed = new GPSspeed();
       locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, GPSspeed);
    }

    class GPSspeed implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            speedText.setText((Math.round((loc.getSpeed()*60*60*0.000621371192) * 10)/10.0)+" mph");
        }
        @Override
        public void onProviderDisabled(String arg0) {}
        @Override
        public void onProviderEnabled(String arg0) {}
        @Override
        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.field_speed,null);
        ((TextView) v.findViewById(R.id.textView)).getPaint().setAntiAlias(false);
        ((TextView) v.findViewById(R.id.textView2)).getPaint().setAntiAlias(false);
        speedText = v.findViewById(R.id.textView);

        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
