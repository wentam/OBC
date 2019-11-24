package us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement;

import java.io.Serializable;

// TODO OSM elements are not specific to the osmXmlReader, we should move them up one level
// example: if we wanted to create an osmXmlWriter, we would want these same classes available

public abstract class OsmElement implements Serializable {
    public long id;
    public int version;
    public String timestamp;
    public long changeset;
    public long uid;
    public String user;
    public byte layer;
}
