package us.egeler.matt.obc.mapDataManager.mapsForgeDataModel;

import java.util.ArrayList;

public class Tile extends MapsForgeElement {
    public boolean isAllWater = false;

    public ArrayList<Long> wayIds = null;
    public ArrayList<Long> poiIds = null;

    public Tile() {
        wayIds = new ArrayList<>();
        poiIds = new ArrayList<>();
    }
}
