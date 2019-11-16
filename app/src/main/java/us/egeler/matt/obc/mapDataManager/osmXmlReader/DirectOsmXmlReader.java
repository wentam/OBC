package us.egeler.matt.obc.mapDataManager.osmXmlReader;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement.Member;
import us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement.Way;
import us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement.Bounds;
import us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement.Node;
import us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement.OsmElement;
import us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement.Relation;

public class DirectOsmXmlReader {
    InputStream is;
    OsmElementOutputStream os;
    BufferedReader br;

    private boolean stateInTag;
    private boolean stateInQuotes;
    private boolean stateReadingTagName;
    private boolean stateReadingAttributeKey;
    private boolean stateReadingAttributeValue;

    public void read(InputStream is, OsmElementOutputStream os) throws IOException {
        this.is = is;
        this.os = os;

        stateInTag = false;
        stateInQuotes = false;
        stateReadingTagName = false;
        stateReadingAttributeKey = false;
        stateReadingAttributeValue = false;

        br = new BufferedReader(new InputStreamReader(is));


        StringBuffer currentTagName = new StringBuffer();
        StringBuffer currentKey = new StringBuffer();
        StringBuffer currentVal = new StringBuffer();
        boolean currentTagIsEndTag = false;
        boolean currentCharIsFirstCharOfTagName = false;

        HashMap<String, String> currentAttributes = new HashMap<>();

        // ...this loop hurts my head
        while (true) {
            int b = br.read();

            if (b == -1) {
                break;
            }

            if ((char)b == '\n') {
                continue;
            }

            if ((char)b == '"' && !stateInQuotes) {
                stateInQuotes = true;
                continue;
            } else if ((char)b == '"' && stateInQuotes) {
                stateInQuotes = false;
                continue;
            }

            if ((char)b == '<' && !stateInTag && !stateInQuotes) {
                stateInTag = true;
                stateReadingTagName = true;
                currentCharIsFirstCharOfTagName = true;
                continue;
            }


            if (stateReadingAttributeValue && !stateInQuotes && ((char) b == ' ' || (char) b == '/' || (char) b == '>')) {
                stateReadingAttributeValue = false;
                currentAttributes.put(currentKey.toString(), currentVal.toString());
                currentKey.delete(0, currentKey.length());
                currentVal.delete(0, currentVal.length());

                if ((char) b != '>') {
                    continue;
                }
            }

            if ((char)b == '>' && stateInTag && !stateInQuotes) {
                stateInTag = false;
                stateReadingTagName = false;
                if (currentTagIsEndTag) {
                    xmlEndTag(currentTagName.toString());
                } else {
                    xmlStartTag(currentTagName.toString(), currentAttributes);
                }
                currentTagName.delete(0,currentTagName.length());
                currentAttributes = new HashMap<>();
                currentTagIsEndTag = false;
                continue;
            }

            if (stateReadingTagName)  {
                if ((char)b == ' ') {
                    stateReadingTagName = false;
                } else {
                    if (currentCharIsFirstCharOfTagName && (char) b == '/') {
                        currentTagIsEndTag = true;
                    }
                    currentTagName.append((char)b);
                    currentCharIsFirstCharOfTagName = false;
                }
                continue;
            }

            // attribute value
            if (stateReadingAttributeValue) {
                currentVal.append((char)b);
                continue;
            }

            // attribute key name
            if (stateInTag && (char)b != ' ' && !stateReadingAttributeKey && !stateReadingAttributeValue) {
               stateReadingAttributeKey = true;
               currentKey.append((char)b);
               continue;
            }

            if (stateInTag && (char)b != ' ' && stateReadingAttributeKey && (char)b != '=') {
                currentKey.append((char)b);
                continue;
            }

            // key->value state switch
            // must remain below attribute value to prevent '=' from being in value string
            if ((char) b == '=' && stateReadingAttributeKey) {
                stateReadingAttributeKey = false;
                stateReadingAttributeValue = true;
                continue;
            }

        }
    }

    // parse state information
    private boolean currentlyProcessingWay = false;
    private Way wayBeingProcessed;

    private boolean currentlyProcessingRelation = false;
    private Relation relationBeingProcessed;

    private void xmlStartTag(String tagname, HashMap<String, String> attributes) {
        /*Log.i("OBCL","got tag: "+tagname);
        for (String key : attributes.keySet()) {
            Log.i("OBCL", "with attr "+key+"="+attributes.get(key));
        }*/

        switch (tagname) {
            // -- some tags will never have children, so we can go ahead and set them up and output them here. --
            case "bounds":
                Bounds b = new Bounds();
                addOsmElementAttrsToOsmElement(b, attributes);
                addBoundsAttrsToBounds(b, attributes);
                os.writeElement(b);
                break;

            case "node":
                Node n = new Node();
                addOsmElementAttrsToOsmElement(n, attributes);
                addNodeAttrsToNode(n, attributes);
                os.writeElement(n);
                break;

            // -- some tags require more data to be completed. Get them started and keep track of state. --
            case "way":
                Way w = new Way();
                this.currentlyProcessingWay = true;
                this.wayBeingProcessed = w;

                addOsmElementAttrsToOsmElement(w, attributes);

                break;

            case "relation":
                Relation r = new Relation();
                this.currentlyProcessingRelation = true;
                this.relationBeingProcessed = r;

                addOsmElementAttrsToOsmElement(r, attributes);
                break;

            // -- some tags only make sense in the context of others, but we can set them up right away as they have no children --

            // "nd" occurs as a child of the way tag and has a single attribute: ref.
            // These refs should be placed in Way.nodes[]
            case "nd":
                if (currentlyProcessingWay) {
                    addNdRefToWay(wayBeingProcessed, Long.parseLong(attributes.get("ref")));
                }
                break;

            // "tag" occurs as a child of the way tag and the relation tag
            // It has k="" and v=""
            // These key-value pairs should be added to Way.tags or Relation.tags
            case "tag":
                if (currentlyProcessingWay) {
                    addTagAttrsToWay(wayBeingProcessed, attributes);
                }
                if (currentlyProcessingRelation) {
                    addTagAttrsToRelation(relationBeingProcessed, attributes);
                }
                break;

            // "member" occurs as a child of the relation tag and has type, ref, and role attributes
            // Create Member OsmElement and add to whatever relation is being currently processed
            case "member":
                Member m = new Member();

                addOsmElementAttrsToOsmElement(m, attributes);
                addMemberAttrsToMember(m, attributes);

                if (currentlyProcessingRelation) {
                    addMemberToRelation(m,relationBeingProcessed);
                }
                break;
        }

    }


    private void addOsmElementAttrsToOsmElement(OsmElement e, HashMap<String, String> attributes) {
        String buf;

        buf = attributes.get("id");
        if (buf != null) {
            e.id = Long.parseLong(buf);
        }

        buf = attributes.get("version");
        if (buf != null) {
            e.version = Integer.parseInt(buf);
        }

        e.timestamp = attributes.get("timestamp");

        buf = attributes.get("changeset");
        if (buf != null) {
            e.changeset = Long.parseLong(buf);
        }

        buf = attributes.get("uid");
        if (buf != null) {
            e.uid = Long.parseLong(buf);
        }

        e.user = attributes.get("user");
    }

    private void addBoundsAttrsToBounds(Bounds b, HashMap<String, String> attributes) {
        String buf;

        buf = attributes.get("minlat");
        if (buf != null) {
            b.minlat = Double.parseDouble(buf);
        }

        buf = attributes.get("minlon");
        if (buf != null) {
            b.minlon = Double.parseDouble(buf);
        }

        buf = attributes.get("maxlat");
        if (buf != null) {
            b.maxlat = Double.parseDouble(buf);
        }

        buf = attributes.get("maxlon");
        if (buf != null) {
            b.maxlon = Double.parseDouble(buf);
        }
    }

    private void addNodeAttrsToNode(Node n, HashMap<String, String> attributes) {
        String buf;

        buf = attributes.get("lat");
        if (buf != null) {
            n.lat = Double.parseDouble(buf);
        }

        buf = attributes.get("lon");
        if (buf != null) {
            n.lon = Double.parseDouble(buf);
        }
    }

    private void addMemberAttrsToMember(Member m, HashMap<String, String> attributes){
        String buf;

        m.type = attributes.get("type");

        buf = attributes.get("ref");
        if (buf != null) {
            m.ref = Long.parseLong(buf);
        }

        m.role = attributes.get("role");
    }

    private void addTagAttrsToWay(Way w, HashMap<String, String> attributes) {
        String key = attributes.get("k");
        String value = attributes.get("v");

        if (key != null && value != null) {
            w.tags.put(key, value);
        }
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

    private void addTagAttrsToRelation(Relation r, HashMap<String, String> attributes) {
        String key = attributes.get("k");
        String value = attributes.get("v");

        if (key != null && value != null) {
            r.tags.put(key, value);
        }
    }

    private void addMemberToRelation(Member m, Relation r) {
        r.members.add(m);
    }

    private void xmlEndTag(String tagname) {
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
}
