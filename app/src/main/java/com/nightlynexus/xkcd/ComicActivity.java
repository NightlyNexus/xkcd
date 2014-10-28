package com.nightlynexus.xkcd;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class ComicActivity extends ActionBarActivity {

    private static final String TAG_COMIC_FRAGMENT = "TAG_COMIC_FRAGMENT";

    private ComicFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic);
        if (savedInstanceState == null) {
            final Bundle bundle = new Bundle();
            bundle.putInt(ComicFragment.KEY_COMIC_NUMBER_REQUESTED, getComicNumberRequested());
            mFragment = new ComicFragment();
            mFragment.setArguments(bundle);
            getFragmentManager().beginTransaction()
                    .add(R.id.container, mFragment, TAG_COMIC_FRAGMENT)
                    .commit();
        } else {
            mFragment = (ComicFragment) getFragmentManager().findFragmentByTag(TAG_COMIC_FRAGMENT);
        }
    }

    private int getComicNumberRequested() {
        final Intent intent = getIntent();
        if (!Intent.ACTION_VIEW.equals(intent.getAction())
                || (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            // Activity launched not from url or from history
            return -1;
        }
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
