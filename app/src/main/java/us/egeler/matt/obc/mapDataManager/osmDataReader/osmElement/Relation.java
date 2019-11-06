package us.egeler.matt.obc.mapDataManager.osmDataReader.osmElement;

import java.util.ArrayList;
import java.util.HashMap;

public class Relation extends OsmElement {
    public ArrayList<Member> members = new ArrayList<>();
    public HashMap<String, String> tags = new HashMap<>();
}
