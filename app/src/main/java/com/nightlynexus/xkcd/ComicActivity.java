package com.nightlynexus.xkcd;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class ComicActivity extends ActionBarActivity {

    public static final String ACTION_UPDATE_COMIC
            = ComicActivity.class + "." + "ACTION_UPDATE_COMIC";
    public static final String EXTRA_COMIC_NUMBER = "EXTRA_COMIC_NUMBER";

    private static final String TAG_COMIC_FRAGMENT = "TAG_FRAGMENT_COMIC";

    private ComicFragment mFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic);
        mFragment = (ComicFragment) getFragmentManager().findFragmentByTag(TAG_COMIC_FRAGMENT);
        if (mFragment == null) {
            mFragment = new ComicFragment();
            mFragment.requestComicNumber(getComicNumberRequested());
            getFragmentManager().beginTransaction()
                    .add(R.id.container, mFragment, TAG_COMIC_FRAGMENT)
                    .commit();
        }
    }

    private int getComicNumberRequested() {
        final Intent intent = getIntent();
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            return -1;
        }
        final String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            String url = intent.getDataString();
            if (url.lastIndexOf("/") == url.length() - 1) {
                url = url.substring(0, url.length() - 1);
            }
            url = url.substring(url.lastIndexOf("/") + 1);
            try {
                return Integer.parseInt(url);
            } catch (NumberFormatException nfe) {
                return -1;
            }
        }
        if (ACTION_UPDATE_COMIC.equals(action)) {
            return intent.getIntExtra(EXTRA_COMIC_NUMBER, -1);
        }
        return -1;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        final int comicNumber = getComicNumberRequested();
        if (comicNumber > 0) {
            mFragment.requestComicNumber(comicNumber);
        }
    }
}
