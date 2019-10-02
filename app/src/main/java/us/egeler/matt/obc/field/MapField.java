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
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import java.io.File;
import java.io.IOException;

import us.egeler.matt.obc.R;

public class MapField extends Field {
    private static final String MAP_FILE = "michigan.map";
    private MapView mapView;
    private LocationManager locationManager;
    private int zoomLevel = 15;

    static MapField newInstance() {
        return new MapField();
    }

    @Override
    public void onCreate(Bundle savedinstancestate) {
        super.onCreate(savedinstancestate);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.field_map,null);

        org.mapsforge.core.util.Parameters.USE_ANTI_ALIASING = false;
        AndroidGraphicFactory.createInstance(getActivity().getApplication());

        mapView = v.findViewById(R.id.map);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TileCache tileCache = AndroidUtil.createTileCache(getActivity(), "mapcache",
                mapView.getModel().displayModel.getTileSize(), 1f,
                mapView.getModel().frameBufferModel.getOverdrawFactor());

        File mapFile = new File(getActivity().getExternalFilesDir(null), MAP_FILE);

        MapDataStore mapDataStore = new MapFile(mapFile);
        TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
                mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE);
        try {
            tileRendererLayer.setXmlRenderTheme(new AssetsRenderTheme(getActivity(),"","maptheme.xml"));
        } catch (IOException e) {
            Log.e("OBC","",e);
        }

        mapView.getLayerManager().getLayers().add(tileRendererLayer);

        mapView.setCenter(new LatLong(42.9412,-85.6427));
        mapView.setZoomLevel((byte) 15);



        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        MapGPS mapGPS = new MapGPS();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mapGPS);
    }


    class MapGPS implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            mapView.setCenter(new LatLong(loc.getLatitude(),loc.getLongitude()));
        }
        @Override
        public void onProviderDisabled(String arg0) {}
        @Override
        public void onProviderEnabled(String arg0) {}
        @Override
        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}

    }

    public boolean onKeyAction(String action) {
        if (action.equals("down_button.pressed")) {
            mapView.setZoomLevel((byte) --zoomLevel);
            return true;
        } else if (action.equals("up_button.pressed")) {
            mapView.setZoomLevel((byte) ++zoomLevel);
            return true;
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
