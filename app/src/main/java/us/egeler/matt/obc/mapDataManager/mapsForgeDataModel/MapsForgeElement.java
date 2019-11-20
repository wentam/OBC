package us.egeler.matt.obc.mapDataManager.mapsForgeDataModel;

import java.io.Serializable;

// TODO mapsforge elements are not specific to mapsForgeDataWriter, we should move them up one level
// example: if we wanted to create a mapsForgeDataReader, we would want these same classes available

public abstract class MapsForgeElement implements Serializable {
    static long maxId = -1; // The highest ID taken. Used for ID generation
    public long id;

    MapsForgeElement() {
        maxId++;
        this.id = maxId;
    }
}
