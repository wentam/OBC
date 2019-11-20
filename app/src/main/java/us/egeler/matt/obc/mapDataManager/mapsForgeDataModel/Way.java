package us.egeler.matt.obc.mapDataManager.mapsForgeDataModel;

import java.util.ArrayList;

public class Way extends MapsForgeElement {
    /*
    A tile on zoom level z is made up of exactly 16 sub tiles on zoom level z+2
    for each sub tile (row-wise, left to right):

    1 bit that represents a flag whether the way is relevant for the sub tile

    Special case: coastline ways must always have all 16 bits set.
     */
    public short subTileBitmap;

    // layer (OSM-Tag: layer=...) + 5 (to avoid negative values)
    //
    // I don't see any layer= in any of the OSM data I've got -- I'm assuming it defaults to 0
    public byte layer = 0;

    // In MapsForge format, the header of the file contains all unique tags in an array instead of inside each way.
    // These are array indices for the tags inside this way for that array in this way's parent header
    public ArrayList<Integer> tagIndexes;

    public String name;
    public String houseNumber;
    public String reference;

    // geo coordinate difference to the first way node
    // stored as microdegrees
    int labelPositionLat;
    int labelPositionLon;

    // geo coordinate difference to the top-left corner of the current tile
    // stored as microdegrees
    // [[lat,lon],
    //  [lat,lon],
    //  [lat,lon]]
    public ArrayList<int[]> nodes = new ArrayList<>();

    // optional, just available to make calc easier in certain situations
    public int[] parentTileCoords;

}
