package us.egeler.matt.obc.mapDataManager.mapsForgeDataWriter;

import java.util.ArrayList;

import us.egeler.matt.obc.mapDataManager.mapProjection.MapProjection;

public class TileUtils {
    public static int[] getTileCoordsForNode(MapProjection p, int latlon_microdegrees[]){
        int tileX = (int) Math.floor(p.lonToX((latlon_microdegrees[1]/1000000D)));
        int tileY = (int) Math.floor(p.latToY((latlon_microdegrees[0]/1000000D)));
        return new int[] {tileX, tileY};
    }

    public static double[] getPreciseTileCoordsForNode(MapProjection p, int latlon_microdegrees[]){
        double tileX = p.lonToX((latlon_microdegrees[1]/1000000D));
        double tileY = p.latToY((latlon_microdegrees[0]/1000000D));
        return new double[] {tileX, tileY};
    }

    public static int[] getLatLonForTile(MapProjection p, int tileCoords[]){
        double lon = p.xToLon(tileCoords[0]);
        double lat = p.yToLat(tileCoords[1]);
        return new int[] {(int)(lat*1000000D), (int)(lon*1000000D)};
    }

    // point A and B are tile coordinates with double precision (so you can define a position within each tile), so if tileCountX/Y=50, pointA/B are 0.0-50.0
    // this function is for operating in projected space: so X/Y, not lat/lon.
    public static ArrayList<long[]> getTileIntersectionsForLine(double[] pointA, double[] pointB, long tileCountX, long tileCountY) {
        ArrayList<long[]> intersections = new ArrayList<>();

        // the first and last point's tile obviously intersect, so let's output that right away
        // (it's possible for the following algorithm to miss the first and last tiles)
        intersections.add(new long[]{
                (long)Math.floor(pointA[0]),
                (long)Math.floor(pointA[1])
        });

        intersections.add(new long[]{
                (long)Math.floor(pointB[0]),
                (long)Math.floor(pointB[1])
        });

        // figure out the length of adjacent and opposite sides of a triangle based on our line, and the angle of our line
        double adjacent = pointB[0]-pointA[0];
        double opposite = pointB[1]-pointA[1];
        double angle = Math.atan(opposite/adjacent);


        // figure out if we process vertically or horizontally.
//        byte processDirection = 1; // 0 == horizontal 1 == vertical

        if (Math.abs(adjacent) > Math.abs(opposite)) {
            // horizontal processing

            double[] leftMostPoint = (pointA[0]<pointB[0]) ? pointA : pointB;
            double[] rightMostPoint = (pointB[0]>pointA[0]) ? pointB : pointA;

            // test every vertical edge between the two points
            for (long i = (long)Math.ceil(leftMostPoint[0]); i < (long)Math.ceil(rightMostPoint[0]); i++) {
                double differenceToFirstPoint = i-leftMostPoint[0];
                double distanceAlongEdge = ((Math.tan(angle) * differenceToFirstPoint)) + leftMostPoint[1];

                // mark the tiles left and right of this edge at this Y value as intersected
                intersections.add(new long[] {
                        i-1,
                        (long)Math.floor(distanceAlongEdge)
                });

                intersections.add(new long[] {
                        i,
                        (long)Math.floor(distanceAlongEdge)
                });
            }
        } else {
            // vertical processing

            double[] smallestCoordPoint = (pointA[1]<pointB[1]) ? pointA : pointB;
            double[] largestCoordPoint = (pointA[1]>pointB[1]) ? pointA : pointB;

            // test every horizontal edge between the two points
            for (long i = (long)Math.ceil(smallestCoordPoint[1]); i < (long)Math.ceil(largestCoordPoint[1]); i++) {
                double differenceToFirstPoint = i-smallestCoordPoint[1];
                double distanceAlongEdge = (differenceToFirstPoint / Math.tan(angle)) + smallestCoordPoint[0];

                // mark the tiles above and below this edge at this X value as intersected
                intersections.add(new long[] {
                        (long)Math.floor(distanceAlongEdge),
                        i-1
                });

                intersections.add(new long[] {
                        (long)Math.floor(distanceAlongEdge),
                        i
                });
            }

        }


        // remove duplicates and intersections that are outside of the provided coordinate system
        ArrayList<long[]> newIntersections = new ArrayList();
        for (long[] intersection : intersections)  {
            boolean existsAlready = false;
            for (long [] newIntersection : newIntersections) {
                if (newIntersection[0] == intersection[0] && newIntersection[1] == intersection[1])  {
                    existsAlready = true;
                }
            }


            if (!existsAlready) {
                if (!(intersection[0] < 0 || intersection[0] > tileCountX)) {
                    if (!(intersection[1] < 0 || intersection[1] > tileCountY)) {
                        newIntersections.add(intersection);
                    }
                }
            }
        }
        intersections = newIntersections;

        return intersections;
    }
}
