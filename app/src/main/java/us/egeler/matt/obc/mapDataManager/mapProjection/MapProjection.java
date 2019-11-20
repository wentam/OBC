package us.egeler.matt.obc.mapDataManager.mapProjection;

public interface MapProjection {
    double lonToX(double lon);
    double latToY(double lat);
    double xToLon(double x);
    double yToLat(double y);

    static double asinh(double x) {
        return Math.log(x + Math.sqrt(x*x + 1.0));
    }
}
