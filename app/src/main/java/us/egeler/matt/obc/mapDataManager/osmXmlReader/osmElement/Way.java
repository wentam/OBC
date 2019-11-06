package us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement;

import java.util.HashMap;

public class Way extends OsmElement {
    public long nodes[];
    public HashMap<String, String> tags = new HashMap<>();
}
