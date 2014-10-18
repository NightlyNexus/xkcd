package com.nightlynexus.xkcd;

import android.app.Activity;
import android.os.Bundle;

public class ComicActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new ComicFragment())
                    .commit();
        }
    }
}
