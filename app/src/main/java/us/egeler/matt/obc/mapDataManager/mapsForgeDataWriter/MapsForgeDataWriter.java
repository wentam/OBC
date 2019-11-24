package us.egeler.matt.obc.mapDataManager.mapsForgeDataWriter;

import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;

import us.egeler.matt.obc.mapDataManager.cache.MapsForgeTileCache;
import us.egeler.matt.obc.mapDataManager.cache.OsmNodeKeyValueCache;
import us.egeler.matt.obc.mapDataManager.cache.OsmWayListCache;
import us.egeler.matt.obc.mapDataManager.mapProjection.MapProjection;
import us.egeler.matt.obc.mapDataManager.mapProjection.Mercator;
import us.egeler.matt.obc.mapDataManager.osmXmlReader.OsmElementOutputStream;
import us.egeler.matt.obc.mapDataManager.osmXmlReader.OsmXmlReader;
import us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement.Bounds;
import us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement.Node;
import us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement.OsmElement;
import us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement.Way;

// TODO: generally needs cleanup/refactor
// TODO: define nodeCache bucket count based on file size
public class MapsForgeDataWriter {
    static final int FORMAT_VERSION = 5;
    private OutputStream os;
    OsmNodeKeyValueCache nodeCache;
    OsmWayListCache wayCache;
    private File cacheDir;

    public MapsForgeDataWriter(OutputStream os, File cacheDir) throws IOException {
        this.os = os;
        this.cacheDir = cacheDir;

        // -- initialize caches --
        nodeCache = new OsmNodeKeyValueCache(cacheDir, 150000);
        wayCache = new OsmWayListCache(cacheDir);
    }

    // MUST be called when you're done
    public void close() {
        try {
         //   nodeCache.destroyCaches(OsmNodeKeyValueCache.CACHE_TARGET_DISK);
        } catch(Exception e){
            // do nothing, if it doesn't work that's okay
        }
    }

    // TODO: accept OsmElement stream of some kind, not a file. This would mean the same function
    // could deal with OSM pbf or other formats we can turn into an OsmElement stream
    public void writeFromOSMXML(File file) throws XmlPullParserException, IOException {
        // Set up object stream
        class MyOsmElementOutputStream extends OsmElementOutputStream {
            Bounds bounds;

            @Override
            public void writeElement(OsmElement e) {
                if (e instanceof us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement.Node) {
                    // This is the first read-through of the file, let's cache the node
                    try {
                        nodeCache.cacheNode((Node) e);
                   //     Log.i("OBCL", "wrote lat:"+OsmNodeKeyValueCache.degreesToMicroDegrees(((Node) e).lat)+" lon:"+OsmNodeKeyValueCache.degreesToMicroDegrees(((Node) e).lon));
                      //  int[] r = nodeCache.getNode(e.id);
                    //    Log.i("OBCL", "got lat:"+r[0]+" lon:"+r[1]);
                    } catch (IOException ex) {
                       Log.e("OBCL", "bad", ex);
                    }
                } else if (e instanceof us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement.Way) {
                    try {
                        wayCache.cacheWay((Way) e);
                    } catch (IOException ex) {
                        Log.e("OBCL", "bad", ex);
                    }
                } else if (e instanceof us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement.Bounds) {
                    bounds = (Bounds) e;
                }

            }
        }

        // set up OsmXmlReader and read in data to create a cache of nodes and ways
        MyOsmElementOutputStream myOsmElementOutputStream = new MyOsmElementOutputStream();

        BufferedInputStream is;
        OsmXmlReader xmlReader;

        is = new BufferedInputStream(new FileInputStream(file), 16*1024);
        xmlReader = new OsmXmlReader(is, myOsmElementOutputStream);
        xmlReader.read();
        is.close();

        Log.i("OBCL","done filling way and node caches");

        // obtain our bounds, error if null
        Bounds bounds = myOsmElementOutputStream.bounds;

        if (bounds == null) {
            throw new UnsupportedOperationException("No bounds found in OSM XML. Bounds element is required.");
        }


        // -------------------------------------------------------------------------------------------
        // create mapsforge Ways from our cached OSM ways and cached nodes, and place those ways
        // into a tile cache
        // -------------------------------------------------------------------------------------------

        // we create a mercator projection with a resolution that equals the number of tiles we are generating,
        // with each "pixel" representing a tile. This way our output coordinates are actually tile coordinates.
        int baseZoomLevel = 14;
        MapProjection tileMercator = new Mercator((long)Math.pow(2,baseZoomLevel), (long)Math.pow(2,baseZoomLevel));

        // convert bounds into tiles
        int tileMinX = (int)Math.floor(tileMercator.lonToX(bounds.minlon));
        int tileMaxX = (int)Math.floor(tileMercator.lonToX(bounds.maxlon));
        int tileMinY = (int)Math.floor(tileMercator.latToY(bounds.minlat));
        int tileMaxY = (int)Math.floor(tileMercator.latToY(bounds.maxlat));

        // create a tile cache
        Log.i("OBCL", "creating tile cache with bounds minX:"+tileMinX+" minY:"+tileMinY+" maxX:"+tileMaxX+" maxY:"+tileMaxY);
        MapsForgeTileCache tileCache = new MapsForgeTileCache(cacheDir, tileMinX, tileMaxY, tileMaxX, tileMinY);

        Way w;
        while((w = wayCache.getNextWay()) != null) {

            // tile questions
            // 1 which tile do I define the subtile bitmap for? the first node in the way?
            // 2 in the subtile bitmap, what counts as being "relevant"? A node? The way having a path through the subtile without any nodes?
            // 3 in a way, the nodes are an offset from the NW corner of a tile. Which tile? the tile the first node exists in? (this would result in negative numbers)
            // 4 which tiles do I put the way in if it crosses multiple tiles? Just the tile it starts in? All of the tiles it crosses, each time with a different subtile bitmap?
            // TODO 5 if a way has a node to the left of a tile and a node to the right of a tile, but zero nodes inside the tile, does the way go into the tile ?
            // TODO My guess is that the answer is yes here. If so, this code needs to get reworked to understand line intersections
            // TODO Assuming this is the case, we could use getTileIntersectionsForLine for each node pair to see which ways we need to stick each node in
            //
            // we can create an OSM file with a manually created way that crosses 4 tiles in a loop, and convert it to a mapsforge file with the official tool to answer these questions
            //
            // answers derived from test
            // 4 if a way crosses multiple tiles, it gets placed into each tile individually. only the nodes relevant to that tile are included in the way for each tile.
            // This includes nodes outside of the tile that lead into the tile (the nodes immediately before or after the tile in the chain).
            // This also answer question 1, because we need to define a subtile bitmap for each way, each in it's own tile
            // This also answers question 3, because we're self contained in each tile we simply offset from our own specific tile
            //
            // 2 the subtile bitmap needs to have bits flipped to 1 if any part of the way is inside the subtile. This includes non-node section of the way.
            // so, a bitmap for a way going straight up from a node near the center, with only one node inside the tile, would look like:
            // 0010
            // 0010
            // 0010
            // 0000


            // add nodes to our mapsforge way. in OSM format, nodes are stored as ID references.
            // for our mapsforge way, we write the node lat/lon directly into the way

            ArrayList<us.egeler.matt.obc.mapDataManager.mapsForgeDataModel.Way> mapsForgeWays = new ArrayList<>();
            us.egeler.matt.obc.mapDataManager.mapsForgeDataModel.Way mapsForgeWay = new us.egeler.matt.obc.mapDataManager.mapsForgeDataModel.Way();
            mapsForgeWays.add(mapsForgeWay);

            // create all mapsforge ways for this OSM way while adding nodes
            // we need one mapsforge way for each tile that this OSM way exists in.
            // see explanation above
            us.egeler.matt.obc.mapDataManager.mapsForgeDataModel.Way currentWay = mapsForgeWays.get(0);
            int previousNode[] = null;
            int[] previousTile = null;
            for (int i = 0; i < w.nodes.length; i++) {
                long nodeId = w.nodes[i];

                int latlon[] = nodeCache.getNode(nodeId);
                int[] thisTile = TileUtils.getTileCoordsForNode(tileMercator, latlon);
                int[] tileLatLon = TileUtils.getLatLonForTile(tileMercator, thisTile);
                int[] latlonDiff = new int[]{(latlon[0]-tileLatLon[0]),(latlon[1]-tileLatLon[1])};

                if (currentWay.parentTileCoords == null) {
                    currentWay.parentTileCoords = thisTile;
                    mapsForgeWay.nodes.add(latlonDiff);
                    // assign new tile to new way
                    currentWay.parentTileCoords = thisTile;
                    currentWay.parentTileLatLonMicroDegrees = tileLatLon;
                } else if (thisTile[0] != currentWay.parentTileCoords[0] || thisTile[1] != currentWay.parentTileCoords[1]) {
                    // this node is in a new tile. we will still add this node to this way to complete it for it's tile
                    int[] prevTileLatLon = TileUtils.getLatLonForTile(tileMercator, previousTile);
                    int[] prevLatlonDiff = new int[]{(latlon[0]-prevTileLatLon[0]),(latlon[1]-prevTileLatLon[1])};
                    currentWay.nodes.add(prevLatlonDiff);

                    // we then create a new Way for the new tile.
                    currentWay = new us.egeler.matt.obc.mapDataManager.mapsForgeDataModel.Way();
                    mapsForgeWays.add(currentWay);

                    // The new way needs to include this node and the previous node.
                    int[] prevNodeLatlonDiff = new int[]{(previousNode[0]-tileLatLon[0]),(previousNode[1]-tileLatLon[1])};
                    currentWay.nodes.add(prevNodeLatlonDiff);
                    currentWay.nodes.add(latlonDiff);

                    // assign new tile to new way
                    currentWay.parentTileCoords = thisTile;
                    currentWay.parentTileLatLonMicroDegrees = tileLatLon;

                } else {
                    mapsForgeWay.nodes.add(latlonDiff);
                }

                previousNode = latlon;
                previousTile = thisTile;
            }

            // add everything else into each mapsforge way
            MapProjection subTileMercator = new Mercator((long)Math.pow(2,(baseZoomLevel+2)), (long)Math.pow(2,(baseZoomLevel+2)));
            for (us.egeler.matt.obc.mapDataManager.mapsForgeDataModel.Way mw : mapsForgeWays) {
                // define subtile bitmap

                // NOTE: sometimes our subtile bitmap differs slightly from mapsforge. Here are a couple examples.
                // I believe this occurs because mapsforge is setting a neighbor bit if the line is extremely close to the edge to handle imprecision and/or road width.
                //
                // Personally I don't feel this is necessary, as you're only going to see a couple pixels of a road in that situation.
                // Nevertheless, documenting it here in case it does become an issue.

                // way ID 17809940 zoom level 14
                // mapsforge
                // 0000
                // 1100
                // 1110
                // 0000

                // ours
                // 0000
                // 1000
                // 1110
                // 0000


                // way ID 603189491 zoom level 14
                // mapsforge
                // 0000
                // 1000
                // 1000
                // 1000

                // ours
                // 0000
                // 1000
                // 1000
                // 0000

                double[] previousNodeCoords = null;
                for (int[] nodeLatLon : mw.nodes)  {
                    // figure out which sub tile we need to mark for this node
                    double[] subTileCoords = TileUtils.getPreciseTileCoordsForNode(subTileMercator, new int[]{mw.parentTileLatLonMicroDegrees[0]+nodeLatLon[0], mw.parentTileLatLonMicroDegrees[1]+nodeLatLon[1]});
                    double[] localSubTileCoords = new double[] {subTileCoords[0]-(mw.parentTileCoords[0]*4), subTileCoords[1]-(mw.parentTileCoords[1]*4)};

                    if (previousNodeCoords != null) {
                        // there was a previous node in the chain. we need to flip bits for anything between our current node and the previous node
                        ArrayList<long[]> intersections = TileUtils.getTileIntersectionsForLine(previousNodeCoords, localSubTileCoords, 4, 4);

                        for (long[] intersection : intersections) {
                            if (!(intersection[0] >= 4 || intersection[0] < 0 || intersection[1] >= 4 ||intersection[1] < 0)) {
                                // set the bit in bitmap for this sub tile
                                mw.subTileBitmap = (short)(mw.subTileBitmap | (0b1000000000000000>>>((4*intersection[1])+intersection[0])));
                            }
                        }

                    }

                    previousNodeCoords = localSubTileCoords;
                }

                mw.layer = w.layer;
                // TODO define tagIndexes and write tags to header
                // TODO define name, house number, reference
                // TODO define label position

            }

        }
        Log.i("OBCL","done creating tiles");
    }
}
