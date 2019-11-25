package us.egeler.matt.obc.mapDataManager.mapsForgeDataModel;

import java.io.Serializable;

public abstract class MapsForgeElement implements Serializable {
    static long maxId = -1; // The highest ID taken. Used for ID generation
    public long id;

    MapsForgeElement() {
        maxId++;
        this.id = maxId;
    }
}
