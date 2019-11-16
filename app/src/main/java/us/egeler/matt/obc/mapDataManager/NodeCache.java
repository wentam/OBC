package us.egeler.matt.obc.mapDataManager;

import android.os.Environment;
import android.util.Log;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;

import us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement.Node;

// Caches OsmElement nodes on disk or memory in a way that the data can be retrieved quickly.
// Backed by a HashMap for memory, and implements hashmap-like storage on disk.
//
// Cache is temporary. Any on-disk cache is deleted on destroyCache().
// destroyCache must be called when you're done!
//
//
// -- Disk file format --
// The file is just a bunch of linked lists, each list representing a bucket.
// If there are no nodes in the bucket yet, the list item will have a placeholder value, but still needs to exist.
//
// The first list item for each bucket appears sequentially at the beginning of the file, and this position represents the bucket index.
//
// Each list item is fixed in size (20 bytes):

// * The first value is the 64-bit (8-byte) integer node ID. If the value is #FFFFFFFF, this is a placeholder.
// * The second value is the latitude as a 32-bit (4-byte) integer in microdegrees
// * The third value is the longitude as a 32-bit (4-byte) integer in microdegrees
// * The fourth value is a 32-bit (4-byte) integer byte offset to the next element in the linked list starting from the end of this element (or null)
//
// So if we had 4 buckets total, the file could look like this:
// [list item 0 for bucket 0][list item 0 for bucket 1][list item 0 for bucket 2][list item 0 for bucket 3]
// [list item 1 for bucket 2][list item 1 for bucket 3][list item 2 for bucket 2][list item 3 for bucket 2]
// [list item 1 for bucket 0][list item 2 for bucket 3]...
//
// To add a node to the cache, we first need to decide what index it belongs in. We need to remap an
// arbitrary large set of IDS to a smaller set (the number of buckets)
//
// We use 'index = node_id % (bucketCount-1)' for this. If n is a power of two, we can use 'index = node_id & (bucketCount-1)' for performance.
//
// We then:
// * find the byte offset for our target bucket index by doing 20*index.
// * If that link item item is a placeholder, replace that placeholder with our node
// * If that item is not a placeholder, write the byte offset to the end of the file in the byte offset field of this item
// * Write our node as a new list item at the end of the file
//
// Make sure to check for duplicates before writing a list item.
//
//
// To read a node from the cache via node ID:
// * calculate the bucket index from node ID again.
// * find the byte offset by doing 20*index
// * If the item's node ID matches, read and return value
// * If the item's node ID does not match, use the byte offset of that item to check the next item in the linked list
// * repeat previous step until item found or end of list hit

public class NodeCache {
    public static final int CACHE_TARGET_DISK = 1;
    public static final int CACHE_TARGET_MEMORY = 2;

    //private static final long bucketCount = 32;
    private static final long bucketCount = 262144;
   // private static final long bucketCount = 2000000;
    private static final short listItemSize = 20;

    HashMap<Long, Double[]> memoryCache;

    File diskCache;
    RandomAccessFile diskCacheFile;
    MappedByteBuffer memoryMappedCache;
    int currentFileLength = 0;
    int currentCacheLength = 0;

    private int initializedCaches = 0;

    // TODO user-definable bucket counts
    // TODO user-definable cache dir/file

    // TODO: handle duplicate nodes
    // TODO: filename used should be unique to this instance of NodeCache

    public void initCaches(int cacheTargets) throws IOException {
        if ((cacheTargets & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
            if ((initializedCaches & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
                throw new IllegalStateException("Cannot initialize memory cache - it is already initialized.");
            } else {
                memoryCache = new HashMap<>();
                initializedCaches |= CACHE_TARGET_MEMORY;
            }
        }

        if ((cacheTargets & CACHE_TARGET_DISK) == CACHE_TARGET_DISK) {
            if ((initializedCaches & CACHE_TARGET_DISK) == CACHE_TARGET_DISK) {
                throw new IllegalStateException("Cannot initialize disk cache - it is already initialized.");
            } else {
                // -- create file, open stream --

                // TODO: relies on Environment.getExternalStorageDirectory(), which is android-specific
                // would prefer to not make this library android-specific, so probably have the root passed
                // to us. We also should be writing to internal storage.
                File Root = Environment.getExternalStorageDirectory();
                Log.i("OBCL", Environment.getExternalStorageDirectory().toString());
                File Dir = new File(Root.getAbsolutePath() + "/MapsForgeWriterCache");

                if (!Dir.exists()) {
                    Dir.mkdir();
                }

                diskCache = new File(Dir, "NodeCache");

                if (diskCache.exists()) { // delete any old caches
                    diskCache.delete();
                }

                diskCache.createNewFile();

                diskCacheFile = new RandomAccessFile(diskCache,"rw");
                diskCacheFile.setLength(1024*1024); // 1MB
                memoryMappedCache = diskCacheFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, diskCacheFile.length());
                currentCacheLength = (int) diskCacheFile.length();

                initializedCaches |= CACHE_TARGET_DISK;

                // -- create the first list item for each bucket --
                byte blankListItem[] = new byte[20];
                // node ID (placeholder)
                blankListItem[0]  = (byte) 0xFF;
                blankListItem[1]  = (byte) 0xFF;
                blankListItem[2]  = (byte) 0xFF;
                blankListItem[3]  = (byte) 0xFF;
                blankListItem[4]  = (byte) 0xFF;
                blankListItem[5]  = (byte) 0xFF;
                blankListItem[6]  = (byte) 0xFF;
                blankListItem[7]  = (byte) 0xFF;

                // latitude
                blankListItem[8]  = (byte) 0x00;
                blankListItem[9]  = (byte) 0x00;
                blankListItem[10]  = (byte) 0x00;
                blankListItem[11]  = (byte) 0x00;

                // longitude
                blankListItem[12]  = (byte) 0x00;
                blankListItem[13]  = (byte) 0x00;
                blankListItem[14]  = (byte) 0x00;
                blankListItem[15]  = (byte) 0x00;

                // pointer
                blankListItem[16]  = (byte) 0xFF;
                blankListItem[17]  = (byte) 0xFF;
                blankListItem[18]  = (byte) 0xFF;
                blankListItem[19]  = (byte) 0xFF;


                for (long i = 0; i < bucketCount; i++) {
                    growMemoryMappedCacheIfNeeded();
                    memoryMappedCache.put(blankListItem);
                    currentFileLength += 20;
                }
            }
        }
    }

    private void growMemoryMappedCacheIfNeeded() throws IOException {
        // if our current cache has less than 128kb of space free
        if (currentFileLength > currentCacheLength-(128*1024)) {
            // double the size
            int targetSize = currentCacheLength*2;

            // but let's not increase more than 10mb at a time
            if (currentCacheLength > 10*1024*1024) {
                targetSize = currentCacheLength+(10*1024*1024);
            }

            int cachePos = memoryMappedCache.position();
            memoryMappedCache.force();
            diskCacheFile.setLength(targetSize);
            memoryMappedCache = diskCacheFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, diskCacheFile.length());
            currentCacheLength = (int) diskCacheFile.length();
            memoryMappedCache.position(cachePos);
        }
    }

    public void destroyCaches(int cacheTargets) throws IOException {
        if ((cacheTargets & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
            if (memoryCache != null) {
                memoryCache.clear();
            }

            memoryCache = null;
        }

        if ((cacheTargets & CACHE_TARGET_DISK) == CACHE_TARGET_DISK) {
            if ((initializedCaches & CACHE_TARGET_DISK) != CACHE_TARGET_DISK) {
                throw new IllegalStateException("Cannot destroy disk cache - it's not initialized!");
            } else {
                diskCacheFile.close();

                // TODO: relies on Environment.getExternalStorageDirectory(), which is android-specific
                // would prefer to not make this library android-specific, so probably have the root passed
                // to us
                File Root = Environment.getExternalStorageDirectory();
                File Dir = new File(Root.getAbsolutePath() + "/MapsForgeWriterCache");

                if (!Dir.exists()) {
                    Dir.mkdir();
                }

                diskCache = new File(Dir, "NodeCache");

                if (diskCache.exists()) { // delete any old caches
                    diskCache.delete();
                }

                initializedCaches &= ~CACHE_TARGET_DISK;
            }
        }
    }

    // returns the ID of the element cached
    // -1 if nothing cached
    // TODO: check for duplicates
    public long cacheNode(Node n, int cacheTargets) throws IOException {
        if ((cacheTargets & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
            if ((initializedCaches & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
                cacheNodeMemory(n);
            } else {
                throw new IllegalStateException("Attempting to cache node on memory, but memory cache is not initialized.");
            }
        }

        if ((cacheTargets & CACHE_TARGET_DISK) == CACHE_TARGET_DISK) {
            if ((initializedCaches & CACHE_TARGET_DISK) == CACHE_TARGET_DISK) {
                cacheNodeDisk(n);
            } else {
                throw new IllegalStateException("Attempting to cache node on disk, but disk cache is not initialized.");
            }
        }

        return -1;
    }

    // returns null if nothing found. lat,lon in microdegrees if found
    public int[] getNode(long nodeId, int cacheTargets) throws IOException {
        if ((cacheTargets & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
            if ((initializedCaches & CACHE_TARGET_MEMORY) == CACHE_TARGET_MEMORY) {
                return getNodeMemory(nodeId);
            } else {
                throw new IllegalStateException("Attempting to cache node from memory, but memory cache is not initialized.");
            }
        }

        if ((cacheTargets & CACHE_TARGET_DISK) == CACHE_TARGET_DISK) {
            if ((initializedCaches & CACHE_TARGET_DISK) == CACHE_TARGET_DISK) {
                return getNodeDisk(nodeId);
            } else {
                throw new IllegalStateException("Attempting to get node from disk, but disk cache is not initialized.");
            }
        }


        return null;
    }

    // return lat, lon in microdegrees
    private int[] getNodeDisk(long nodeId) throws IOException {
        // Find byte offset of first list item for the correct bucket (offset from beginning of file)
        long firstListItemByteOffset = getBucketIndex(nodeId)*listItemSize;

        // Read the first list item's node ID of this bucket to see if it's a placeholder
        byte nodeIdBytes[] = new byte[8];
        memoryMappedCache.position((int)firstListItemByteOffset);
        memoryMappedCache.get(nodeIdBytes);

        // while the nodeID does not match our target
        long nodeIdInt = Longs.fromByteArray(nodeIdBytes);
        while (nodeIdInt != nodeId) {
            // -- seek to the next node --

            // seek to the pointer and read it
            memoryMappedCache.position((int) (diskCacheFile.getFilePointer()+8));
            byte pointer[] = new byte[4];
            memoryMappedCache.get(pointer);

            // if the pointer is null, we're done. The node doesn't exist
            if (    pointer[0] == (byte) 0xFFFF && pointer[1] == (byte) 0xFFFF &&
                    pointer[2] == (byte) 0xFFFF && pointer[3] == (byte) 0xFFFF) {
                return null;
            }

            // follow the pointer
            memoryMappedCache.position((int) (diskCacheFile.getFilePointer()+ Ints.fromByteArray(pointer)));

            // read our next node ID
            memoryMappedCache.get(nodeIdBytes);
            nodeIdInt = Longs.fromByteArray(nodeIdBytes);
        }

        // Cursor is just after the node ID. Read the rest of the node and return
        byte lat[] = new byte[4];
        memoryMappedCache.get(lat);

        byte lon[] = new byte[4];
        memoryMappedCache.get(lon);

        int out[] = new int[2];
        out[0] = Ints.fromByteArray(lat);
        out[1] = Ints.fromByteArray(lon);

        return out;
    }

    private int[] getNodeMemory(long nodeId) {
        Double latlon[] = memoryCache.get(nodeId);

        if (latlon != null)  {
            int out[] = new int[2];
            out[0] = degreesToMicroDegrees(latlon[0]);
            out[1] = degreesToMicroDegrees(latlon[1]);

            return out;
        }

        return null;
    }

    private void cacheNodeMemory(Node n) {
        memoryCache.put(n.id, new Double[] {n.lat, n.lon});
    }

    private void cacheNodeDisk(Node n) throws IOException {
        // -------------------------------------------------------
        // --- seek and write necessary pointers along the way ---

        // Find byte offset of first list item for the correct bucket (offset from beginning of file)
        long firstListItemByteOffset = getBucketIndex(n.id)*listItemSize;

        // Read the first list item's node ID of this bucket to see if it's a placeholder
        byte nodeid[] = new byte[8];
        memoryMappedCache.position((int)firstListItemByteOffset);
        memoryMappedCache.get(nodeid);

        boolean isPlaceHolder = false;

        // Check if the first item is a placeholder
        if (    nodeid[0] == (byte) 0xFFFF && nodeid[1] == (byte) 0xFFFF &&
                nodeid[2] == (byte) 0xFFFF && nodeid[3] == (byte) 0xFFFF &&
                nodeid[4] == (byte) 0xFFFF && nodeid[5] == (byte) 0xFFFF &&
                nodeid[6] == (byte) 0xFFFF && nodeid[7] == (byte) 0xFFFF) {
            // Yup, it's a placeholder. Seek to the start of the placeholder item in the file.
            memoryMappedCache.position((int)firstListItemByteOffset);
            isPlaceHolder = true;
        } else {
            // Starting from the first item, follow the pointers to the end of the last item of the list
            seekToEndOfList(firstListItemByteOffset);

            // Let's modify this item to make it point to the end of the file

            memoryMappedCache.position((memoryMappedCache.position() - 4)); // seek back 4 bytes
            memoryMappedCache.put(Ints.toByteArray((int) (currentFileLength-memoryMappedCache.position()-4))); // write our pointer

            // seek to the end of the file to write our item
            memoryMappedCache.position(currentFileLength);
        }

        // ----------------------
        // --- write our item ---
        if (!isPlaceHolder) {
            currentFileLength+=listItemSize;
            growMemoryMappedCacheIfNeeded();
        }

        writeListItem(n);
     //   memoryMappedCache.force();

    }

    private void writeListItem(Node n) throws IOException {
        // 8-byte Node ID
        memoryMappedCache.put(Longs.toByteArray(n.id));

        // 4-byte lat microdegrees
        byte lat[] = new byte[4];
        lat = Ints.toByteArray(degreesToMicroDegrees(n.lat));
        memoryMappedCache.put(lat);

        // 4-byte lon microdegrees
        byte lon[] = new byte[4];
        lon = Ints.toByteArray(degreesToMicroDegrees(n.lon));
        memoryMappedCache.put(lon);

        // No pointer yet
        byte offset[] = new byte[4];
        Arrays.fill(offset, (byte) 0xFFFF);
        memoryMappedCache.put(offset);
    }

    // give me an offset from start of file to the first item in a list, and I'll seek to the end of
    // the last item of the list
    private void seekToEndOfList(long firstListItemByteOffset) throws IOException {
        byte pointer[] = new byte[4];
         memoryMappedCache.position((int)(firstListItemByteOffset + (listItemSize-4))); // seek to first pointer
         memoryMappedCache.get(pointer); // read the pointer

        while (!(pointer[0] == (byte) 0xFFFF && pointer[1] == (byte) 0xFFFF &&
                pointer[2] == (byte) 0xFFFF && pointer[3] == (byte) 0xFFFF)) {
            // current cursor is at the end of the pointer, so we want our
            // current position+pointer value+offset to find next pointer
            memoryMappedCache.position((memoryMappedCache.position() + Ints.fromByteArray(pointer) + (listItemSize-4)));

            // grab our next pointer
            memoryMappedCache.get(pointer);
        }
    }

    public static int degreesToMicroDegrees(double degrees){
        return (int) Math.round(degrees*1000000);
    }

    private static long getBucketIndex(long node_id) {
        return node_id % (bucketCount); // TODO: require bucketCount to be power of two, and use & instead of %?
    }
}
