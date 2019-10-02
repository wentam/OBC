package us.egeler.matt.obc.page;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import us.egeler.matt.obc.R;
import us.egeler.matt.obc.field.MapField;

public class MapPage extends Page {
    View view;
    MapField mapField = null;

    @Override
    public void onCreate(Bundle savedinstancestate) {
        super.onCreate(savedinstancestate);
    }

    public static void writeBytesToFile(InputStream is, File file) throws IOException {
        FileOutputStream fos = null;
        try {
            byte[] data = new byte[2048];
            int nbread = 0;
            fos = new FileOutputStream(file);
            while((nbread=is.read(data))>-1){
                fos.write(data,0,nbread);
            }
        }
        catch (Exception ex) {
            // lel
        }
        finally{
            if (fos!=null){
                fos.close();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_mappage, null);


        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        FragmentManager fragMan = getChildFragmentManager();

        mapField = new MapField();
        fragMan.beginTransaction().add(R.id.maplayout, mapField, "mapfield0").commit();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected boolean onKeyAction(String action) {
        if (mapField != null) {
            mapField.onKeyAction(action);
        }

        return false;
    }
}
