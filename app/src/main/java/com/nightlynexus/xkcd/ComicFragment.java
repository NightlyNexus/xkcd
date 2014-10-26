package com.nightlynexus.xkcd;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.ShareActionProvider;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ComicFragment extends Fragment {

    public static final String KEY_COMIC_NUMBER_REQUESTED = "KEY_COMIC_NUMBER_REQUESTED";

    private static final String ENDPOINT = "http://xkcd.com";
    private static final String DIRECTORY_NAME = "xkcd";
    private static final String KEY_MAX = "KEY_MAX";
    private static final String KEY_POSITION = "KEY_POSITION";

    private ComicService mRestService;
    private View mRetryFrame;
    private View mNavBarComic;
    private EditText mNumberPicker;
    private View mPreviousView;
    private View mNextView;
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
        mNavBarComic = rootView.findViewById(R.id.nav_bar_comic);
        mNumberPicker = (EditText) rootView.findViewById(R.id.number_picker);
        mPreviousView = rootView.findViewById(R.id.previous);
        mNextView = rootView.findViewById(R.id.next);
        mViewPager = (ViewPager) rootView.findViewById(R.id.view_pager);
        mRetryFrame = rootView.findViewById(R.id.retry_frame);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setNavBarComicAndChildrenEnabled(false);
        if (savedInstanceState != null && savedInstanceState.getInt(KEY_MAX) >= 0
                && savedInstanceState.getInt(KEY_POSITION) >= 0) {
            setupNumberPicker(savedInstanceState.getInt(KEY_MAX));
            setupAdapter(savedInstanceState.getInt(KEY_MAX),
                    savedInstanceState.getInt(KEY_POSITION));
        } else {
            mRestService.getComicLatest(new Callback<Comic>() {

                private final Callback<Comic> mCallback = this;

                @Override
                public void success(Comic comic, Response response) {
                    if (isDetached()) return;
                    mRetryFrame.setVisibility(View.GONE);
                    mViewPager.setVisibility(View.VISIBLE);
                    setupNumberPicker(comic.getNum());
                    final int comicNumRequested
                            = getArguments().getInt(KEY_COMIC_NUMBER_REQUESTED);
                    setupAdapter(comic.getNum(),
                            getSafeComicIndex(comicNumRequested, comic.getNum()));
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    if (isDetached()) return;
                    mRetryFrame.findViewById(R.id.retry_button).setOnClickListener(
                            new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            mRestService.getComicLatest(mCallback);
                        }
                    });
                    mViewPager.setVisibility(View.GONE);
                    mRetryFrame.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void setupNumberPicker(final int numMax) {
        final InputFilter filters[] = new InputFilter[] { new InputFilter() {

            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                final String str = dest.toString().substring(0, dstart)
                        + source.toString().substring(start, end)
                        + dest.toString().substring(dend);
                if (str.length() == 0) return source.subSequence(start, end);
                try {
                    final int number = Integer.parseInt(str);
                    if (number < 1 || number > numMax) {
                        return dest.subSequence(dstart, dend);
                    }
                    return source.subSequence(start, end);
                } catch (NumberFormatException nfe) {
                    return dest.subSequence(dstart, dend);
                }
            }
        } };
        mNumberPicker.setFilters(filters);
        mNumberPicker.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    mViewPager.setCurrentItem(Integer.parseInt(s.toString()) - 1);
                }
            }
        });
        mPreviousView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
            }
        });
        mNextView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
            }
        });
        setNavBarComicAndChildrenEnabled(true);
    }

    private void setupAdapter(final int num, final int position) {
        mAdapter = new PagerAdapter() {

            @Override
            public Object instantiateItem(ViewGroup container, final int position) {
                final SwipeRefreshLayout swipeRefreshLayout
                        = new SwipeRefreshLayout(getActivity());
                final ScrollView sv = new ScrollView(getActivity());
                sv.setFillViewport(true);
                final LinearLayout ll = new LinearLayout(getActivity());
                final int hp = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
                final int vp = (int) getResources().getDimension(R.dimen.activity_vertical_margin);
                ll.setPadding(hp, vp, hp, vp);
                final TextView titleView = new TextView(getActivity());
                final ProgressBar progress = new ProgressBar(getActivity());
                final Button failedButton = new Button(getActivity());
                final String[] interblag = getResources().getStringArray(R.array.internet_names);
                failedButton.setText(getString(R.string.retry_button_text_comic,
                        interblag[new Random().nextInt(interblag.length)]));
                failedButton.setVisibility(View.GONE);
                final TextView tv = new TextView(getActivity());
                final ScrollView.LayoutParams params1 = new ScrollView.LayoutParams(
                        ScrollView.LayoutParams.MATCH_PARENT,
                        ScrollView.LayoutParams.WRAP_CONTENT);
                ll.setLayoutParams(params1);
                ll.setGravity(Gravity.CENTER);
                ll.setOrientation(LinearLayout.VERTICAL);
                final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                progress.setLayoutParams(params);
                failedButton.setLayoutParams(params);
                titleView.setLayoutParams(params);
                titleView.setPadding(0, 0, 0,
                        (int) getResources().getDimension(R.dimen.padding_bottom_title));
                titleView.setGravity(Gravity.CENTER);
                titleView.setTextAppearance(getActivity(), R.style.TitleText);
                progress.setVisibility(View.VISIBLE);
                final ImageView iv = getImageView();
                iv.setVisibility(View.GONE);
                final TextView dateView = getDateView();
                tv.setLayoutParams(params);
                tv.setPadding(0,
                        (int) getResources().getDimension(R.dimen.padding_top_alt_text), 0, 0);
                tv.setGravity(Gravity.CENTER);
                mRestService.getComic(position + 1, new Callback<Comic>() {

                    private final Callback<Comic> mCallback = this;

                    @Override
                    public void success(final Comic comic, Response response) {
                        if (isDetached()) return;
                        titleView.setText(comic.getTitle());
                        tv.setText(comic.getAlt());
                        dateView.setText(comic.getDate());
                        final Target target = new Target() {

                            @Override
                            public void onBitmapLoaded(final Bitmap bitmap,
                                                       Picasso.LoadedFrom from) {
                                if (isDetached()) return;
                                progress.setVisibility(View.GONE);
                                iv.setImageBitmap(bitmap);
                                iv.setTag(false);
                                iv.setOnClickListener(new View.OnClickListener() {

                                    @Override
                                    public void onClick(View v) {
                                        if ((Boolean) iv.getTag()) {
                                            iv.setImageBitmap(bitmap);
                                            iv.setTag(false);
                                        } else {
                                            iv.setImageBitmap(invert(bitmap));
                                            iv.setTag(true);
                                        }
                                    }
                                });
                                iv.setOnLongClickListener(new View.OnLongClickListener() {

                                    @Override
                                    public boolean onLongClick(View v) {
                                        final Bitmap bitmapSave
                                                = (Boolean) iv.getTag() ? invert(bitmap) : bitmap;
                                        final String comicName
                                                = position + 1 + " -- " + comic.getTitle();
                                        if (saveBitmap(bitmapSave, comicName)) {
                                            Toast.makeText(getActivity(),
                                                    getString(R.string.saved_comic_confirmation,
                                                            comicName),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                        return true;
                                    }
                                });
                                iv.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onBitmapFailed(Drawable drawable) {
                                if (isDetached()) return;
                                onFailure();
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
                        onFailure();
                    }

                    private void onFailure() {
                        failedButton.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                failedButton.setVisibility(View.GONE);
                                progress.setVisibility(View.VISIBLE);
                                mRestService.getComic(position + 1, mCallback);
                            }
                        });
                        progress.setVisibility(View.GONE);
                        failedButton.setVisibility(View.VISIBLE);
                    }
                });
                ll.addView(titleView);
                ll.addView(progress);
                ll.addView(failedButton);
                ll.addView(iv);
                ll.addView(tv);
                ll.addView(dateView);
                sv.addView(ll);
                swipeRefreshLayout.addView(sv);
                swipeRefreshLayout.setColorSchemeResources(android.R.color.black);
                swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

                    @Override
                    public void onRefresh() {
                        final Random random = new Random();
                        final int position = random.nextInt(num);
                        mViewPager.setCurrentItem(position, false);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
                container.addView(swipeRefreshLayout);
                return swipeRefreshLayout;
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
                mNumberPicker.setText(String.valueOf(i + 1));
                mNumberPicker.setSelection(mNumberPicker.length());
                mPreviousView.setEnabled(i > 0);
                mNextView.setEnabled(i < num - 1);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
        mViewPager.setCurrentItem(position);
    }

    private void setNavBarComicAndChildrenEnabled(final boolean enabled) {
        mNumberPicker.setEnabled(enabled);
        mPreviousView.setEnabled(enabled);
        mNextView.setEnabled(enabled);
        mNavBarComic.setEnabled(enabled);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.comic, menu);
        MenuItem menuItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        mShareActionProvider.setShareIntent(getShareIntent());
    }

    private Intent getShareIntent() {
        // hacky to get link
        final String link = mAdapter == null
                ? ENDPOINT : ENDPOINT + "/" + (mViewPager.getCurrentItem() + 1);
        final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        }
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

    private static int getSafeComicIndex(final int comicNumber, final int comicNumberMax) {
        return ((comicNumber > 0 && comicNumber <= comicNumberMax)
                ? comicNumber : comicNumberMax) - 1;
    }

    public void requestComicNumber(final int comicNumber) {
        if (isDetached())
            throw new RuntimeException(
                    "This fragment has been detached and cannot update the current comic.");
        mViewPager.setCurrentItem(getSafeComicIndex(comicNumber, mAdapter.getCount()), false);
    }

    private ImageView getImageView() {
        final LinearLayout.LayoutParams paramsImage = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        final ImageView iv = new ImageView(getActivity());
        iv.setLayoutParams(paramsImage);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setAdjustViewBounds(true);
        iv.setContentDescription(getString(
                R.string.comic_content_description));
        return iv;
    }

    private TextView getDateView() {
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.END;
        final TextView dateView = new TextView(getActivity());
        dateView.setLayoutParams(params);
        dateView.setPadding(0,
                (int) getResources().getDimension(R.dimen.padding_top_date), 0, 0);
        dateView.setGravity(Gravity.CENTER);
        dateView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        return dateView;
    }

    private static Bitmap invert(Bitmap original) {
        // Create mutable Bitmap to invert, argument true makes it mutable
        Bitmap inversion = original.copy(Bitmap.Config.ARGB_4444, true);

        // Get info about Bitmap
        int width = inversion.getWidth();
        int height = inversion.getHeight();
        int pixels = width * height;

        // Get original pixels
        int[] pixel = new int[pixels];
        inversion.getPixels(pixel, 0, width, 0, 0, width, height);

        // Modify pixels
        final int RGB_MASK = 0x00FFFFFF;
        for (int i = 0; i < pixels; i++)
            pixel[i] ^= RGB_MASK;
        inversion.setPixels(pixel, 0, width, 0, 0, width, height);

        // Return inverted Bitmap
        return inversion;
    }

    private boolean saveBitmap(final Bitmap bmp, final String comicName) {
        final String filename = Environment.getExternalStorageDirectory().getPath()
                + "/" + DIRECTORY_NAME + "/" + comicName + ".png";
        boolean success = false;
        FileOutputStream out = null;
        try {
            new File(filename).getParentFile().mkdirs();
            out = new FileOutputStream(filename);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return success;
    }
}
