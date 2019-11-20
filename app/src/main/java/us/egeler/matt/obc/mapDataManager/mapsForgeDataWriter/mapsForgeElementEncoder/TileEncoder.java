package us.egeler.matt.obc.mapDataManager.mapsForgeDataWriter.mapsForgeElementEncoder;

import java.io.OutputStream;

import us.egeler.matt.obc.mapDataManager.mapsForgeDataModel.Tile;
import us.egeler.matt.obc.mapDataManager.serializableObjectCache.MapsForgeElementCache;

public class TileEncoder {
    MapsForgeElementCache c;

    public TileEncoder(MapsForgeElementCache c) {
        this.c = c;
    }

    // returns number of bytes written
    public int encodeTileToStream(Tile t, OutputStream os) {
        // TODO
        return 0;
    }
}
