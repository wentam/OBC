package us.egeler.matt.obc.mapDataManager;

import android.icu.util.Output;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

// OsmDataProcessor modifies OSM data to be suitable for use with OBC (such as by removing the word "street")
// TODO: this class should probably use OsmXmlReader
public class OSMDataReducer {
    public OSMDataReducer() {

    }

    // Feed me XML and I'll make it good for OBC use
    public void process(InputStream inputStream, OutputStream outputStream) throws XmlPullParserException, IOException {
        XmlPullParser xp = Xml.newPullParser();
        xp.setInput(inputStream, null);
        XmlSerializer xw = Xml.newSerializer();
        xw.setOutput(outputStream, null);

        int eventType = xp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    xw.startDocument("UTF-8", true);
                    break;

                case XmlPullParser.END_DOCUMENT:
                    xw.endDocument();
                    break;

                case XmlPullParser.START_TAG:
                    String v = null;
                    if (xp.getName().equals("tag")) { // tag name is tag, just to be confusing
                        // if k="name", remove instances of "Street" "Road" etc. from v attribute
                        // also remove trailing spaces and double spaces

                        if (getAttributeValue(xp,"k").equals("name")) {
                            v = getAttributeValue(xp,"v");
                            v = v.replaceAll("(?i)street","");
                            v = v.replaceAll("(?i)road","");
                            v = v.replaceAll("(?i)avenue","");
                            v = v.replaceAll("(?i)ave","");
                            v = v.replaceAll("(?i)northeast","");
                            v = v.replaceAll("(?i)southeast","");
                            v = v.replaceAll("(?i)northwest","");
                            v = v.replaceAll("(?i)southwest","");
                            v = v.replaceAll("(?i)north","");
                            v = v.replaceAll("(?i)south","");
                            v = v.replaceAll("(?i)east","");
                            v = v.replaceAll("(?i)west","");
                            v = v.replaceAll("  "," ");
                            v = v.replaceAll(" $","");
                        }
                    }

                    xw.startTag(xp.getNamespace(), xp.getName());
                    for (int i = 0; i < xp.getAttributeCount(); i++) {
                        if (xp.getAttributeName(i).equals("v") && v != null) {
                            xw.attribute(xp.getNamespace(), xp.getAttributeName(i), v);
                        } else {
                            xw.attribute(xp.getNamespace(), xp.getAttributeName(i), xp.getAttributeValue(i));
                        }
                    }
                    break;

                case XmlPullParser.END_TAG:
                    xw.endTag(xp.getNamespace(), xp.getName());
                    break;

                case XmlPullParser.TEXT:
                    xw.text(xp.getText());
                    break;
            }

            eventType = xp.next();
        }

        xw.flush();
    }

    private String getAttributeValue(XmlPullParser xp, String name) {
        for (int i = 0; i < xp.getAttributeCount(); i++) {
            if (xp.getAttributeName(i).equals(name)) {
                return xp.getAttributeValue(i);
            }
        }

        return null;
    }
}
