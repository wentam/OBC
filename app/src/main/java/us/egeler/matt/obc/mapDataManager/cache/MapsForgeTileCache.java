package us.egeler.matt.obc.mapDataManager.cache;


// -- Disk file format --
// The first thing to appear in the file will be tile definitions for all tiles that will be within this file.
// Tiles are stored as a table, written as a row-wise 1-dimensional list, based on tile X and tile Y, so:
// [0,0][1,0][2,0][3,0][4,0]
// [0,1][2,1][3,1][4,1][5,1]
//
// So, to find a tile based on it's x y coords:
// * row_index=Y*table_width
// * tile_byte_offset=(row_index+x)*tile_byte_size
//
// A tile is fixed in size and contains the following:
// * 4-byte tile X
// * 4-byte tile Y
// * 4-byte offset to first way in linked list of ways
// * -not implemented- 4-byte offset to first poi in linked list of pois
//
// A way is a fully encoded mapsforge-format Way followed by a 4-byte pointer to the next way.
// -not implemented- a poi is a fully encoded mapsforge-format poi followed by a 4-byte pointer to the next poi

import com.google.common.primitives.Ints;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MapsForgeTileCache {
    private static int instance = 0;

    private File diskCacheFile;
    private RandomAccessFile diskCacheRaf;
    private MappedByteBuffer mmapCache;
    private int currentDataLength = 0;
    private int currentFileLength = 0;

    public MapsForgeTileCache(File dir, int tileMinX, int tileMinY, int tileMaxX, int tileMaxY) throws IOException {
        // -- create file, open stream --
        if (!dir.exists()) {
            dir.mkdir();
        }

        diskCacheFile = new File(dir, "tile_cache_"+instance);

        if (diskCacheFile.exists()) { // delete any old caches
            diskCacheFile.delete();
        }

        diskCacheFile.createNewFile();

        diskCacheRaf = new RandomAccessFile(diskCacheFile,"rw");
        diskCacheRaf.setLength(1024*1024); // 1MB
        mmapCache = diskCacheRaf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, diskCacheRaf.length());
        currentFileLength = (int) diskCacheRaf.length();

        writeTiles(tileMinX, tileMinY, tileMaxX, tileMaxY);

        instance++;
    }

    private void writeTiles(int tileMinX, int tileMinY, int tileMaxX, int tileMaxY) {
        // iterate over rows
        for (int i = tileMinY; i <= tileMaxY; i++) {
            // iterate over columns
            for (int i2 = tileMinX; i2 <= tileMaxX; i2++) {
                // write tile X
                byte[] x = Ints.toByteArray(i2);
                mmapCache.put(x);

                // write tile Y
                byte[] y = Ints.toByteArray(i);
                mmapCache.put(y);

                // write null 0xFF pointer
                byte[] p = new byte[] {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
                mmapCache.put(p);
            }
        }
    }

}
