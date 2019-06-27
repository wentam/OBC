package us.egeler.matt.obc.field;

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

public class Map extends Field {
    private static final String MAP_FILE = "michigan.map";

    @Override
    public void onCreate(Bundle savedinstancestate) {
        super.onCreate(savedinstancestate);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.field_map,null);

        AndroidGraphicFactory.createInstance(getActivity().getApplication());

        MapView mapView = v.findViewById(R.id.map);

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

        mapView.setCenter(new LatLong(42.8664,-84.8984));
        mapView.setZoomLevel((byte) 15);
        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
