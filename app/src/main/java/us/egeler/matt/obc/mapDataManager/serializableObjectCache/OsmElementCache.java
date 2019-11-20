package us.egeler.matt.obc.mapDataManager.serializableObjectCache;

import us.egeler.matt.obc.mapDataManager.osmXmlReader.osmElement.OsmElement;

public class OsmElementCache extends SerializableObjectCache<Long, OsmElement> {
    @Override
    protected Long getUniqueIdentifierForElement(OsmElement e) {
        return e.id;
    }
}
