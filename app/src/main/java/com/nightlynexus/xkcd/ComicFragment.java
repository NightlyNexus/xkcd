package com.nightlynexus.xkcd;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.ShareActionProvider;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ComicFragment extends Fragment {

    private static final String ENDPOINT = "http://xkcd.com";
    private static final String KEY_MAX = "KEY_MAX";
    private static final String KEY_POSITION = "KEY_POSITION";

    private ComicService mRestService;
    private ViewPager mViewPager;
    private PagerAdapter mAdapter;
    private ShareActionProvider mShareActionProvider = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        final RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(ENDPOINT)
                .build();
        mRestService = restAdapter.create(ComicService.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_comic, container, false);
        mViewPager = (ViewPager) rootView.findViewById(R.id.view_pager);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.getInt(KEY_MAX) >= 0
                && savedInstanceState.getInt(KEY_POSITION) >= 0) {
            setupAdapter(savedInstanceState.getInt(KEY_MAX),
                    savedInstanceState.getInt(KEY_POSITION));
        } else {
            mRestService.getComicLatest(new Callback<Comic>() {

                @Override
                public void success(Comic comic, Response response) {
                    if (isDetached()) return;
                    setupAdapter(comic.getNum(), comic.getNum() - 1);
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    if (isDetached()) return;
                }
            });
        }
    }

    private void setupAdapter(final int num, final int position) {
        mAdapter = new PagerAdapter() {

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                final RelativeLayout rl = new RelativeLayout(getActivity());
                final RelativeLayout.LayoutParams params0
                        = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                params0.addRule(RelativeLayout.CENTER_VERTICAL);
                final ScrollView sv = new ScrollView(getActivity());
                sv.setLayoutParams(params0);
                final LinearLayout ll = new LinearLayout(getActivity());
                final int hp = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
                final int vp = (int) getResources().getDimension(R.dimen.activity_vertical_margin);
                ll.setPadding(hp, vp, hp, vp);
                final TextView titleView = new TextView(getActivity());
                final ProgressBar progress = new ProgressBar(getActivity());
                final TextView tv = new TextView(getActivity());
                final ScrollView.LayoutParams params1 = new ScrollView.LayoutParams(
                        ScrollView.LayoutParams.MATCH_PARENT,
                        ScrollView.LayoutParams.WRAP_CONTENT);
                ll.setLayoutParams(params1);
                ll.setGravity(Gravity.CENTER);
                ll.setOrientation(LinearLayout.VERTICAL);
                final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ScrollView.LayoutParams.MATCH_PARENT,
                        ScrollView.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                titleView.setLayoutParams(params);
                titleView.setGravity(Gravity.CENTER);
                titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
                progress.setVisibility(View.VISIBLE);
                tv.setLayoutParams(params);
                tv.setGravity(Gravity.CENTER);
                mRestService.getComic(position + 1, new Callback<Comic>() {

                    @Override
                    public void success(Comic comic, Response response) {
                        if (isDetached()) return;
                        titleView.setText(comic.getTitle());
                        tv.setText(comic.getAlt());
                        final Target target = new Target() {

                            @Override
                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                if (isDetached()) return;
                                progress.setVisibility(View.GONE);
                                tv.setCompoundDrawablesWithIntrinsicBounds(null,
                                        new BitmapDrawable(getResources(), bitmap), null, null);
                            }

                            @Override
                            public void onBitmapFailed(Drawable drawable) {
                                if (isDetached()) return;
                            }

                            @Override
                            public void onPrepareLoad(Drawable drawable) {
                                if (isDetached()) return;
                            }
                        };
                        tv.setTag(target);
                        Picasso.with(getActivity()).load(comic.getImg()).into(target);
                    }

                    @Override
                    public void failure(RetrofitError retrofitError) {
                        if (isDetached()) return;
                    }
                });
                ll.addView(titleView);
                ll.addView(progress);
                ll.addView(tv);
                sv.addView(ll);
                rl.addView(sv);
                container.addView(rl);
                return rl;
            }

            @Override public void destroyItem(ViewGroup container, int position,
                                              Object view) {
                container.removeView((View) view);
            }

            @Override
            public int getCount() {
                return num;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }
        };
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(int i) {
                if (mShareActionProvider != null)
                    mShareActionProvider.setShareIntent(getShareIntent());
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
        mViewPager.setCurrentItem(position);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.comic, menu);
        MenuItem menuItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) menuItem.getActionProvider();
        mShareActionProvider.setShareIntent(getShareIntent());
    }

    private Intent getShareIntent() {
        // hacky to get link
        final String link = mAdapter == null
                ? ENDPOINT : ENDPOINT + "/" + (mViewPager.getCurrentItem() + 1);
        final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(Intent.EXTRA_SUBJECT, ENDPOINT);
        intent.putExtra(Intent.EXTRA_TEXT, link);
        return intent;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_MAX, mAdapter == null ? -1 : mAdapter.getCount());
        outState.putInt(KEY_POSITION, mAdapter == null ? -1 : mViewPager.getCurrentItem());
        super.onSaveInstanceState(outState);
    }
}
