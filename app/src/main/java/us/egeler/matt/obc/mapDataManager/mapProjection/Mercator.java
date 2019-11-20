package us.egeler.matt.obc.mapDataManager.mapProjection;

import android.util.Log;

public class Mercator implements MapProjection {
    long mapSizeX;
    long mapSizeY;

    public Mercator(long mapSizeX, long mapSizeY){
        this.mapSizeX = mapSizeX;
        this.mapSizeY = mapSizeY;
    }

    public double lonToX(double lon) {
        double remap = (lon+180)/360; // map lon with it's range of -180 to 180 so that it has a range of 0 to 1
        return remap*mapSizeX; // remap 0-1 to our map size
    }

    public double latToY(double lat) {
        double latRad = Math.toRadians(lat);
        double yUnmapped = MapProjection.asinh(Math.tan(latRad));

        return (1-yUnmapped/Math.PI)/2*mapSizeY;
    }

    public double xToLon(double x) {
        double remap = (x/mapSizeX); // map X with it's range of 0-mapSizeX to 0-1
        return (remap*360)-180; // remap 0 to 1 to -180 to 180
    }

    public double yToLat(double y) {
        double latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * y / mapSizeY)));
        return Math.toDegrees(latRad);
    }
}
