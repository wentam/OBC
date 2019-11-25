package us.egeler.matt.obc.mapDataManager.osmXmlReader;

import us.egeler.matt.obc.mapDataManager.osmDataModel.OsmElement;

// Not to be confused with InputStream/OutputStream/ObjectOutputStream --
// This guy works directly with objects, no serialization or anything
//
// Extend me and listen for your elements!
public abstract class OsmElementOutputStream {
    public abstract void writeElement(OsmElement elem);
}
