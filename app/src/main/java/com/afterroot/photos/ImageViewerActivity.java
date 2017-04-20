package com.afterroot.photos;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.afterroot.photos.adapter.ImagePagerAdapter;
import com.afterroot.photos.adapter.ImagesRecyclerViewAdapter;
import com.transitionseverywhere.Explode;
import com.transitionseverywhere.Slide;
import com.transitionseverywhere.TransitionManager;

import java.io.File;

import static com.afterroot.photos.Helper.Static.EXTRA_GOTO_RECYCLER_POS;
import static com.afterroot.photos.Helper.Static.EXTRA_VIEWPAGER_POS;

public class ImageViewerActivity extends AppCompatActivity {
    
    private static final int UI_ANIMATION_DELAY = 0;
    private final Handler mHideHandler = new Handler();
    private ViewPager mImagesPager;
    private final Runnable mHideSystemUi = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mImagesPager.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    };

    private boolean mVisible;
    private final Runnable mHideRunnable = this::hide;
    private ActionBar mToolbar;
    private LinearLayout bottomBarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_viewer);
        mToolbar = getSupportActionBar();
        if (mToolbar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mVisible = true;
        mImagesPager = (ViewPager) findViewById(R.id.images_pager);

        load();
    }

    ImagePagerAdapter mPagerAdapter;
    Helper mHelper;
    public void load(){
        mHelper = new Helper(this);
        mPagerAdapter = new ImagePagerAdapter(this, mHelper.getImagePaths(mHelper.getFilterTag(), mHelper.getSortOrder()),
                new ImagesRecyclerViewAdapter.OnItemInteractionListener() {
            @Override
            public void onItemClick(int pos) {
                toggle();
            }

            @Override
            public boolean onItemLongClick(int pos) {
                return false;
            }
        });

        bottomBarLayout = (LinearLayout) findViewById(R.id.bottom_bar_layout);
        ImageButton shareButton = (ImageButton) findViewById(R.id.share_button);
        shareButton.setOnClickListener(v -> {
            File file = new File(mPagerAdapter.getPath(mImagesPager.getCurrentItem()));
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            Intent chooser = Intent.createChooser(shareIntent, "Share Image");
            startActivity(chooser);
        });

        assert mImagesPager != null;
        mImagesPager.setAdapter(mPagerAdapter);
        //mImagesPager.setPageTransformer(true, new ZoomOutPageTransformer());
        File file = new File(mPagerAdapter.getPath(mImagesPager.getCurrentItem()));
        try {
            getSupportActionBar().setTitle(mHelper.getFilterTag());
        } catch (Exception e){
            e.printStackTrace();
        }
        mImagesPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Intent i = new Intent();
                i.putExtra(EXTRA_GOTO_RECYCLER_POS, mImagesPager.getCurrentItem());
                File file = new File(mPagerAdapter.getPath(mImagesPager.getCurrentItem()));
                setResult(RESULT_OK, i);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mImagesPager.setCurrentItem(getIntent().getExtras().getInt(EXTRA_VIEWPAGER_POS, 0));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide(0);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        if (mToolbar != null) {
            mToolbar.hide();
        }
        if (bottomBarLayout != null){
            TransitionManager.beginDelayedTransition((ViewGroup) findViewById(R.id.image_viewer_root), new Explode());
            bottomBarLayout.setVisibility(View.INVISIBLE);
        }
        mVisible = false;
        mHideHandler.postDelayed(mHideSystemUi, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        mImagesPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;
        if (mToolbar != null) {
            mToolbar.show();
        }
        if (bottomBarLayout != null){
            TransitionManager.beginDelayedTransition((ViewGroup) findViewById(R.id.image_viewer_root), new Slide(Gravity.BOTTOM));
            bottomBarLayout.setVisibility(View.VISIBLE);
        }
        
        mHideHandler.removeCallbacks(mHideSystemUi);
    }
    
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private class ZoomOutPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 1) { // [-1,1]
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

                // Fade the page relative to its size.
                view.setAlpha(MIN_ALPHA +
                        (scaleFactor - MIN_SCALE) /
                                (1 - MIN_SCALE) * (1 - MIN_ALPHA));

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }
}
