package us.egeler.matt.obc.mapDataManager.osmDataReader.osmElement;

public abstract class OsmElement {
    public long id;
    public int version;
    public String timestamp;
    public long changeset;
    public long uid;
    public String user;
}
