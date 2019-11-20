package us.egeler.matt.obc.mapDataManager.serializableObjectCache;

import us.egeler.matt.obc.mapDataManager.mapsForgeDataModel.MapsForgeElement;

public class MapsForgeElementCache extends SerializableObjectCache<Long, MapsForgeElement> {
    @Override
    protected Long getUniqueIdentifierForElement(MapsForgeElement e) {
        return e.id;
    }
}
