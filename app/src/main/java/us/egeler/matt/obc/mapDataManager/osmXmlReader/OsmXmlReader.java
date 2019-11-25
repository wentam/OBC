package us.egeler.matt.obc.mapDataManager.osmXmlReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;

import us.egeler.matt.obc.mapDataManager.osmDataModel.Bounds;
import us.egeler.matt.obc.mapDataManager.osmDataModel.Member;
import us.egeler.matt.obc.mapDataManager.osmDataModel.Node;
import us.egeler.matt.obc.mapDataManager.osmDataModel.OsmElement;
import us.egeler.matt.obc.mapDataManager.osmDataModel.Relation;
import us.egeler.matt.obc.mapDataManager.osmDataModel.Way;

// OsmXmlReader reads in OSM XML and spits out objects containing the map data into a stream as the file is read
//
// Operating in streams for this allows for nice efficient code, as the OSM file only needs to be read once,
// and not all of the data necessarily needs to be stored in memory depending on what you're doing (OSM data is big).
public class OsmXmlReader {
    private InputStream is;
    private OsmElementOutputStream os;
    private XmlPullParser xp;

    // parse state information
    private boolean currentlyProcessingWay = false;
    private Way wayBeingProcessed;

    private boolean currentlyProcessingRelation = false;
    private Relation relationBeingProcessed;

    // constructor
    public OsmXmlReader(InputStream is, OsmElementOutputStream os) {
        this.is = is;
        this.os = os;
    }

    // reads from the InputStream specific in the constructor,
    // writes OsmElements to the OsmElementOutputStream specified in the constructor
    public void read() throws XmlPullParserException, IOException {
        xp = XmlPullParserFactory.newInstance().newPullParser();
        xp.setInput(is, null);

        int eventType = xp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    break;

                case XmlPullParser.END_DOCUMENT:
                    break;

                case XmlPullParser.START_TAG:
                    xmlTagStarted(xp.getName());
                    break;

                case XmlPullParser.END_TAG:
                    xmlTagEnded(xp.getName());
                    break;

                case XmlPullParser.TEXT:
                    break;
            }

            eventType = xp.next();
        }
    }

    private void xmlTagStarted(String tagname) throws IOException {
        switch (tagname) {
            // -- some tags will never have children, so we can go ahead and set them up and output them here. --
            case "bounds":
                Bounds b = new Bounds();
                addOsmElementAttrsToOsmElement(b);
                addBoundsAttrsToBounds(b);
                os.writeElement(b);
                break;

            case "node":
                Node n = new Node();
                addOsmElementAttrsToOsmElement(n);
                addNodeAttrsToNode(n);
                os.writeElement(n);
                break;

            // -- some tags require more data to be completed. Get them started and keep track of state. --
            case "way":
                Way w = new Way();
                this.currentlyProcessingWay = true;
                this.wayBeingProcessed = w;

                addOsmElementAttrsToOsmElement(w);
                addWayAttrsToWay(w);

                break;

            case "relation":
                Relation r = new Relation();
                this.currentlyProcessingRelation = true;
                this.relationBeingProcessed = r;

                addOsmElementAttrsToOsmElement(r);
                addRelationAttrsToRelation(r);

                break;

            // -- some tags only make sense in the context of others, but we can set them up right away as they have no children --

            // "nd" occurs as a child of the way tag and has a single attribute: ref.
            // These refs should be placed in Way.nodes[]
            case "nd":
                if (currentlyProcessingWay) {
                    addNdRefToWay(wayBeingProcessed, Long.parseLong(getAttributeValue("ref")));
                }
                break;

            // "tag" occurs as a child of the way tag and the relation tag
            // It has k="" and v=""
            // These key-value pairs should be added to Way.tags or Relation.tags
            case "tag":
                if (currentlyProcessingWay) {
                   addTagAttrsToWay(wayBeingProcessed);
                }
                if (currentlyProcessingRelation) {
                    addTagAttrsToRelation(relationBeingProcessed);
                }
                break;

            // "member" occurs as a child of the relation tag and has type, ref, and role attributes
            // Create Member OsmElement and add to whatever relation is being currently processed
            case "member":
                Member m = new Member();

                addOsmElementAttrsToOsmElement(m);
                addMemberAttrsToMember(m);

                if (currentlyProcessingRelation) {
                   addMemberToRelation(m,relationBeingProcessed);
                }
                break;
        }
    }

    private void xmlTagEnded(String tagname) throws IOException {
        // finish up with tags that had children
        switch (tagname)  {
            case "way":
                this.currentlyProcessingWay = false;
                os.writeElement(wayBeingProcessed);
                break;

            case "relation":
                this.currentlyProcessingRelation = false;
                os.writeElement(relationBeingProcessed);
                break;
        }
    }

    private void addTagAttrsToRelation(Relation r) {
        String key = null;
        String value = null;

        for (int i = 0; i < xp.getAttributeCount(); i++) {
            String attrName = xp.getAttributeName(i);
            String attrVal = xp.getAttributeValue(i);

            switch (attrName) {
                case "k":
                    key = attrVal;
                    break;
                case "v":
                    value = attrVal;
                    break;
            }
        }

        if (key != null && value != null) {
            r.tags.put(key, value);
        }
    }

    private void addTagAttrsToWay(Way w) {
        String key = null;
        String value = null;

        for (int i = 0; i < xp.getAttributeCount(); i++) {
            String attrName = xp.getAttributeName(i);
            String attrVal = xp.getAttributeValue(i);

            switch (attrName) {
                case "k":
                    key = attrVal;
                    break;
                case "v":
                    value = attrVal;
                    break;
            }
        }

        if (key != null && value != null) {
            w.tags.put(key, value);
        }
    }

    private void addMemberToRelation(Member m, Relation r) {
        r.members.add(m);
    }

    private void addNdRefToWay(Way w, long ref) {
        // make nodes array one element longer (or create it if it doesn't exist)
        if (w.nodes == null) {
           w.nodes = new long[1] ;
        } else {
            long[] longerNodes = new long[w.nodes.length + 1];

            for (int i = 0; i < w.nodes.length; i++) {
                longerNodes[i] = w.nodes[i];
            }

            w.nodes = longerNodes;
        }

        // write our ref
        w.nodes[w.nodes.length-1] = ref;
    }

    private void addOsmElementAttrsToOsmElement(OsmElement e) {
        for (int i = 0; i < xp.getAttributeCount(); i++) {
            String attrName = xp.getAttributeName(i);
            String attrVal = xp.getAttributeValue(i);

            switch (attrName) {
                case "id":
                    e.id = Long.parseLong(attrVal);
                    break;
                case "version":
                    e.version = Integer.parseInt(attrVal);
                    break;
                case "timestamp":
                    e.timestamp = attrVal;
                    break;
                case "changeset":
                    e.changeset = Long.parseLong(attrVal);
                    break;
                case "uid":
                    e.uid = Long.parseLong(attrVal);
                    break;
                case "user":
                    e.user = attrVal;
                    break;
                case "layer":
                    e.layer = Byte.parseByte(attrVal);
                    break;
            }
        }
    }

    private void addMemberAttrsToMember(Member m){
        for (int i = 0; i < xp.getAttributeCount(); i++) {
            String attrName = xp.getAttributeName(i);
            String attrVal = xp.getAttributeValue(i);

            switch (attrName) {
                case "type":
                    m.type = attrVal;
                    break;
                case "ref":
                    m.ref = Long.parseLong(attrVal);
                    break;
                case "role":
                    m.role = attrVal;
                    break;
            }
        }

    }

    private void addBoundsAttrsToBounds(Bounds b) {
        for (int i = 0; i < xp.getAttributeCount(); i++) {
            String attrName = xp.getAttributeName(i);
            String attrVal = xp.getAttributeValue(i);

            switch (attrName) {
                case "minlat":
                    b.minlat = Double.parseDouble(attrVal);
                    break;
                case "minlon":
                    b.minlon = Double.parseDouble(attrVal);
                    break;
                case "maxlat":
                    b.maxlat = Double.parseDouble(attrVal);
                    break;
                case "maxlon":
                    b.maxlon = Double.parseDouble(attrVal);
                    break;
            }
        }
    }

    private void addNodeAttrsToNode(Node n) {
        for (int i = 0; i < xp.getAttributeCount(); i++) {
            String attrName = xp.getAttributeName(i);
            String attrVal = xp.getAttributeValue(i);

            switch (attrName) {
                case "lat":
                    n.lat = Double.parseDouble(attrVal);
                    break;
                case "lon":
                    n.lon = Double.parseDouble(attrVal);
                    break;
            }
        }

    }

    private void addWayAttrsToWay(Way w) {
        // currently no way-specific attributes that I'm aware of. do nothing.
    }

    private void addRelationAttrsToRelation(Relation r) {
        // currently no relation-specific attributes that I'm aware of. do nothing.
    }

    private String getAttributeValue(String name) {
        for (int i = 0; i < xp.getAttributeCount(); i++) {
            if (xp.getAttributeName(i).equals(name)) {
                return xp.getAttributeValue(i);
            }
        }

        return null;
    }
}
