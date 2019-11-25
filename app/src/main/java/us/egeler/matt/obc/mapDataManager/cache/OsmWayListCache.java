package us.egeler.matt.obc.mapDataManager.cache;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import us.egeler.matt.obc.mapDataManager.osmDataModel.Way;

// Caches osmElement Ways as an array for easy iteration later
//
// -- Disk file format --
//
// The file is a simple list of ways, with each way being the following:
// * 4-byte integer defining length of way in bytes
// * 8-byte way ID
// * 2-byte integer defining number of nodes that follow
// * a list of node IDs as 8-byte ints
// * a list of tags:
// * * key as UTF-8 encoded string
// * * UTF-8 encoded '=' character
// * * value as UTF-8 encoded string
// * * 0xFF as a separator between key-value pairs (but not after the last pair)
//
// TODO: store Way ID
public class OsmWayListCache {
    private static int instance = 0;

    private File diskCacheFile;
    private RandomAccessFile diskCacheRaf;
    private MappedByteBuffer mmapCache;
    private int currentDataLength = 0;
    private int currentFileLength = 0;

    public OsmWayListCache(File dir) throws IOException {
        // -- create file, open stream --
        if (!dir.exists()) {
            dir.mkdir();
        }

        diskCacheFile = new File(dir, "way_cache_"+instance);

        if (diskCacheFile.exists()) { // delete any old caches
            diskCacheFile.delete();
        }

        diskCacheFile.createNewFile();

        diskCacheRaf = new RandomAccessFile(diskCacheFile,"rw");
        diskCacheRaf.setLength(1024*1024); // 1MB
        mmapCache = diskCacheRaf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, diskCacheRaf.length());
        currentFileLength = (int) diskCacheRaf.length();

        instance++;
    }

    public void destroyCache() throws IOException {
        diskCacheRaf.close();
        if (diskCacheFile.exists()) {
            diskCacheFile.delete();
        }
    }

    public void cacheWay(Way n) throws IOException {

        // -- convert this way into our format in memory (excluding the length header) --

        ArrayList<Byte> bytes = new ArrayList<>();

        // add 8-byte way id
        byte[] wayIdBytes = Longs.toByteArray(n.id);
        for (int i = 0; i < wayIdBytes.length; i++) {
            bytes.add(wayIdBytes[i]);
        }

        // add 2-byte number of nodes
        byte[] nodeCountBytes = Shorts.toByteArray((short) n.nodes.length);
        bytes.add(nodeCountBytes[0]);
        bytes.add(nodeCountBytes[1]);

        // add nodes
        for (long node : n.nodes) {
            byte[] nodeBytes = Longs.toByteArray(node);
            for (int i = 0; i < nodeBytes.length; i++) {
                bytes.add(nodeBytes[i]);
            }
        }

        // add tags separated by 0xFF
        boolean isFirst = true;
        for (Map.Entry k : n.tags.entrySet()) {
            if (!isFirst) {
                bytes.add((byte) 0xFF);
            }

            String key = (String) k.getKey();
            String value = n.tags.get(key);

            byte[] pairByteArray = (key+"="+value).getBytes();

            for (int i = 0; i < pairByteArray.length; i++) {
                bytes.add(pairByteArray[i]);
            }

            isFirst = false;
        }


        byte[] result = new byte[bytes.size()];
        for(int i = 0; i < bytes.size(); i++) {
            result[i] = bytes.get(i);
        }

        growMemoryMappedCacheIfNeeded();
        mmapCache.position(currentDataLength);
        mmapCache.put(Ints.toByteArray(result.length));
        mmapCache.put(result);

        currentDataLength+=result.length+4;
    }

    private int readingPosition = 0;

    public void restartReading() {
        readingPosition = 0;
    }

    public Way getNextWay() {
        if (readingPosition >= currentDataLength)  {
            return null;
        }

        // get the length of this way
        byte[] lengthBytes = new byte[4];
        mmapCache.position(readingPosition);
        mmapCache.get(lengthBytes);

        int length = Ints.fromByteArray(lengthBytes);

        // read the rest of the way into memory
        byte[] wayBytes = new byte[length];
        mmapCache.get(wayBytes);

        // create long with ID
        long id = Longs.fromByteArray(new byte[] {wayBytes[0], wayBytes[1], wayBytes[2], wayBytes[3],
                                                  wayBytes[4], wayBytes[5], wayBytes[6], wayBytes[7]});

        // create array of nodes
        short nodeCount = Shorts.fromByteArray(new byte[] {wayBytes[8],wayBytes[9]});
        long nodes[] = new long[nodeCount];

        for (int i = 0; i < nodeCount; i++) {
            byte[] node = new byte[8];
            System.arraycopy(wayBytes, (i*8)+2+8, node, 0, 8);
            nodes[i] = Longs.fromByteArray(node);
        }

        // create hashmap of tags
        HashMap<String, String> tags = new HashMap<>();

        boolean currentlyReadingKey = true;
        boolean currentlyReadingVal = false;
        StringBuffer keyBuffer = new StringBuffer();
        StringBuffer valBuffer = new StringBuffer();
        for (int i = (nodeCount*8)+2+8; i < wayBytes.length; i++) { // iterate over all remaining bytes
            byte b = wayBytes[i];

            if ((char)b == '=' && currentlyReadingKey) {
                currentlyReadingKey = false;
                currentlyReadingVal = true;
                continue;
            }

            if (b == (byte) 0xFF) {
                currentlyReadingKey = true;
                currentlyReadingVal = false;
                tags.put(keyBuffer.toString(),valBuffer.toString());
                keyBuffer = new StringBuffer();
                valBuffer = new StringBuffer();
                continue;
            }

            if (currentlyReadingKey) {
                keyBuffer.append((char) b);
            } else if (currentlyReadingVal) {
                valBuffer.append((char) b);
            }
        }
        tags.put(keyBuffer.toString(),valBuffer.toString());

        // create our Way object
        Way w = new Way();
        w.id = id;
        w.nodes = nodes;
        w.tags = tags;

        // set the reading position to the end of this way so the next call reads the next way
        readingPosition += length+4;

        return w;
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
}
