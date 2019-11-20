package us.egeler.matt.obc.mapDataManager.cache;

import android.os.Environment;
import android.util.Log;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import java.io.File;
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

public class OsmNodeKeyValueCache {
    private static int instance = 0;
    //private static final long bucketCount = 32;
    private long bucketCount = 262144;
    //private long bucketCount = 2000000;
    private static final short listItemSize = 20;

    private HashMap<Long, Double[]> memoryCache;

    private File diskCacheFile;
    private RandomAccessFile diskCacheRaf;
    private MappedByteBuffer mmapCache;
    private int currentDataLength = 0;
    private int currentFileLength = 0;

    // TODO: handle duplicate nodes

    public OsmNodeKeyValueCache(File dir, int expectedNodeCount) throws IOException {
        // -- decide how many buckets we need --
        if (expectedNodeCount != 0) {
            bucketCount = expectedNodeCount;
        }

        // -- create file, open stream --

        if (!dir.exists()) {
            dir.mkdir();
        }

        diskCacheFile = new File(dir, "node_cache_"+instance);

        if (diskCacheFile.exists()) { // delete any old caches
            diskCacheFile.delete();
        }

        diskCacheFile.createNewFile();

        diskCacheRaf = new RandomAccessFile(diskCacheFile,"rw");
        diskCacheRaf.setLength(1024*1024); // 1MB
        mmapCache = diskCacheRaf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, diskCacheRaf.length());
        currentFileLength = (int) diskCacheRaf.length();

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
            mmapCache.put(blankListItem);
            currentDataLength += 20;
        }

        instance++;
    }

    private void growMemoryMappedCacheIfNeeded() throws IOException {
        // if our current cache has less than 128kb of space free
        if (currentDataLength > currentFileLength -(128*1024)) {
            // double the size
            int targetSize = currentFileLength *2;

            // but let's not increase more than 10mb at a time
            if (currentFileLength > 10*1024*1024) {
                targetSize = currentFileLength +(10*1024*1024);
            }

            int cachePos = mmapCache.position();
            mmapCache.force();
            diskCacheRaf.setLength(targetSize);
            mmapCache = diskCacheRaf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, diskCacheRaf.length());
            currentFileLength = (int) diskCacheRaf.length();
            mmapCache.position(cachePos);
        }
    }

    public void destroyCache() throws IOException {
        diskCacheRaf.close();

      /*  // TODO: relies on Environment.getExternalStorageDirectory(), which is android-specific
        // would prefer to not make this library android-specific, so probably have the root passed
        // to us
        File Root = Environment.getExternalStorageDirectory();
        File Dir = new File(Root.getAbsolutePath() + "/MapsForgeWriterCache");

        if (!Dir.exists()) {
            Dir.mkdir();
        }

        diskCacheFile = new File(Dir, "OsmNodeKeyValueCache");*/

        if (diskCacheFile.exists()) {
            diskCacheFile.delete();
        }
    }

    // returns lat, lon in microdegrees
    public int[] getNode(long nodeId) throws IOException {
        // Find byte offset of first list item for the correct bucket (offset from beginning of file)
        long firstListItemByteOffset = getBucketIndex(nodeId)*listItemSize;

        // Read the first list item's node ID of this bucket to see if it's a placeholder
        byte nodeIdBytes[] = new byte[8];
        mmapCache.position((int)firstListItemByteOffset);
        mmapCache.get(nodeIdBytes);

        // while the nodeID does not match our target
        long nodeIdInt = Longs.fromByteArray(nodeIdBytes);
        while (nodeIdInt != nodeId) {
            // -- seek to the next node --

            // seek to the pointer and read it
            mmapCache.position((int) (mmapCache.position()+8));
            byte pointer[] = new byte[4];
            mmapCache.get(pointer);

            // if the pointer is null, we're done. The node doesn't exist
            if (    pointer[0] == (byte) 0xFFFF && pointer[1] == (byte) 0xFFFF &&
                    pointer[2] == (byte) 0xFFFF && pointer[3] == (byte) 0xFFFF) {
                return null;
            }

            // follow the pointer
            mmapCache.position((int) (mmapCache.position()+ Ints.fromByteArray(pointer)));

            // read our next node ID
            mmapCache.get(nodeIdBytes);
            nodeIdInt = Longs.fromByteArray(nodeIdBytes);
        }

        // Cursor is just after the node ID. Read the rest of the node and return
        byte lat[] = new byte[4];
        mmapCache.get(lat);

        byte lon[] = new byte[4];
        mmapCache.get(lon);

        int out[] = new int[2];
        out[0] = Ints.fromByteArray(lat);
        out[1] = Ints.fromByteArray(lon);

        return out;
    }

    public void cacheNode(Node n) throws IOException {
        // -------------------------------------------------------
        // --- seek and write necessary pointers along the way ---

        // Find byte offset of first list item for the correct bucket (offset from beginning of file)
        long firstListItemByteOffset = getBucketIndex(n.id)*listItemSize;

        // Read the first list item's node ID of this bucket to see if it's a placeholder
        byte nodeid[] = new byte[8];
        mmapCache.position((int)firstListItemByteOffset);
        mmapCache.get(nodeid);

        boolean isPlaceHolder = true;

        // Check if the first item is a placeholder
        for (int i = 0; i < nodeid.length; i++)  {
            if (nodeid[i] != (byte) 0xFF) {
                isPlaceHolder = false;
                break;
            }
        }

        if (isPlaceHolder) {
            // Yup, it's a placeholder. Seek to the start of the placeholder item in the file.
            mmapCache.position((int)firstListItemByteOffset);
            isPlaceHolder = true;
        } else {
            // Starting from the first item, follow the pointers to the end of the last item of the list
            seekToEndOfList(firstListItemByteOffset);

            // Let's modify this item to make it point to the end of the file
            mmapCache.position((mmapCache.position() - 4)); // seek back 4 bytes
            mmapCache.put(Ints.toByteArray(currentDataLength - mmapCache.position()-4)); // write our pointer

            // seek to the end of the file to write our item
            mmapCache.position(currentDataLength);
        }

        // ----------------------
        // --- write our item ---
        if (!isPlaceHolder) {
            currentDataLength +=listItemSize;
            growMemoryMappedCacheIfNeeded();
        }

        writeListItem(n);
    }

    private void writeListItem(Node n) {
        // 8-byte Node ID
        mmapCache.put(Longs.toByteArray(n.id));

        // 4-byte lat microdegrees
        byte lat[] = Ints.toByteArray(degreesToMicroDegrees(n.lat));
        mmapCache.put(lat);

        // 4-byte lon microdegrees
        byte lon[] = Ints.toByteArray(degreesToMicroDegrees(n.lon));
        mmapCache.put(lon);

        // No pointer yet
        byte offset[] = new byte[4];
        Arrays.fill(offset, (byte) 0xFFFF);
        mmapCache.put(offset);
    }

    // give me an offset from start of file to the first item in a list, and I'll seek to the end of
    // the last item of the list
    private void seekToEndOfList(long firstListItemByteOffset) {
        byte pointer[] = new byte[4];
        mmapCache.position((int)(firstListItemByteOffset + (listItemSize-4))); // seek to first pointer
        mmapCache.get(pointer); // read the pointer

        while (!(pointer[0] == (byte) 0xFFFF && pointer[1] == (byte) 0xFFFF &&
                pointer[2] == (byte) 0xFFFF && pointer[3] == (byte) 0xFFFF)) {
            // current cursor is at the end of the pointer, so we want our
            // current position+pointer value+offset to find next pointer
            mmapCache.position((mmapCache.position() + Ints.fromByteArray(pointer) + (listItemSize-4)));

            // grab our next pointer
            mmapCache.get(pointer);
        }
    }

    public static int degreesToMicroDegrees(double degrees){
        return (int) Math.round(degrees*1000000);
    }

    private long getBucketIndex(long node_id) {
        return node_id % (bucketCount); // TODO: require bucketCount to be power of two, and use & instead of %?
    }
}