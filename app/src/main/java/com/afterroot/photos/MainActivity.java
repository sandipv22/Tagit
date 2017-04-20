package com.afterroot.photos;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afterroot.photos.adapter.ImagesRecyclerViewAdapter;
import com.afterroot.photos.async.AddToDatabaseTask;
import com.afterroot.photos.async.DeleteFromDatabaseTask;
import com.afterroot.photos.fragment.TagsListFragment;
import com.bumptech.glide.Glide;
import com.nineoldandroids.animation.ObjectAnimator;
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller;
import com.transitionseverywhere.Fade;
import com.transitionseverywhere.TransitionManager;

import net.yazeed44.imagepicker.model.ImageEntry;
import net.yazeed44.imagepicker.util.Picker;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.afterroot.photos.Helper.Static.EXTRA_GOTO_RECYCLER_POS;
import static com.afterroot.photos.Helper.Static.EXTRA_VIEWPAGER_POS;
import static com.afterroot.photos.Helper.Static.REQUEST_CODE_IMAGE_VIEWER;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ImagesRecyclerViewAdapter.OnItemInteractionListener{

    private Helper mHelper;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private boolean isInSelectionMode;
    private DrawerArrowDrawable mArrowDrawable;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.drawer_layout) DrawerLayout drawer;
    @BindView(R.id.toolbar_layout) CollapsingToolbarLayout toolbarLayout;
    @BindView(R.id.nav_view) NavigationView navigationView;
    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.images_list) RecyclerView mImagesList;
    @BindView(R.id.scrim_image) ImageView scrim_image;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        mHelper = new Helper(this);
        mSharedPreferences = mHelper.getSharedPreferences();
        mEditor = mSharedPreferences.edit();


        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mArrowDrawable = toggle.getDrawerArrowDrawable();

        navigationView.setNavigationItemSelectedListener(this);

        init();
    }

    AppBarLayout mAppBarLayout;
    public void init(){
        File file = new File(mHelper.getTagitPath());
        if (!file.exists()){
            file.mkdirs();
        }

        setFabAsAdd();
        setSwipeRefresh();

        mAppBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        if (mHelper.getFilterTag().equals("")){
            showTagsWithTitle("Select Tag");
        } else {
            loadToGrid();
        }
    }

    public void setFabAsAdd(){
        fab.setImageResource(R.drawable.ic_action_add);
        fab.setOnClickListener(view -> new Picker.Builder(
                MainActivity.this, new ImagePickListener(), R.style.Picker)
                .setBackBtnInMainActivity(true)
                .disableCaptureImageFromCamera()
                .build()
                .startActivity());
    }

    public void setFabAsRemove(){
        fab.setImageResource(R.drawable.ic_action_remove);
        fab.setOnClickListener(v -> {
            DeleteFromDatabaseTask deleteFromDatabaseTask =
                    new DeleteFromDatabaseTask(MainActivity.this, mAdapter, this::loadToGrid);
            deleteFromDatabaseTask.execute(mAdapter.getSelectedItems());
            finishSelectionMode();
        });
    }
    public int getSpanCount(){
        return mSharedPreferences.getInt(getString(R.string.key_span_count), 3);
    }

    @BindView(R.id.swipeRefresh) SwipeRefreshLayout mRefreshLayout;
    public void setSwipeRefresh(){
        mRefreshLayout.setOnRefreshListener(this::loadToGrid);
    }

    ImagesRecyclerViewAdapter mAdapter;
    Handler mHandler;
    private void loadToGrid(){
        mHandler = new Handler();
        try{
            stopScrimTask();
            mRefreshLayout.setRefreshing(true);
            TransitionManager.beginDelayedTransition(mRefreshLayout, new Fade(Fade.OUT));
            mImagesList.setVisibility(View.INVISIBLE);
            mAdapter = new ImagesRecyclerViewAdapter(
                    this, mHelper.getImagePaths(mHelper.getFilterTag(), mHelper.getSortOrder()), this);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, getSpanCount(),
                    LinearLayoutManager.VERTICAL, false);
            mImagesList.setLayoutManager(gridLayoutManager);
            mImagesList.setAdapter(mAdapter);
            mImagesList.setItemAnimator(new DefaultItemAnimator());
            mImagesList.setHasFixedSize(true);
            RecyclerFastScroller recyclerFastScroller = (RecyclerFastScroller) findViewById(R.id.fast_scroller);
            recyclerFastScroller.attachRecyclerView(mImagesList);

            toolbarLayout.setTitle(mHelper.getFilterTag());
            toolbarLayout.setStatusBarScrimColor(getResources().getColor(android.R.color.transparent));
            toolbarLayout.setContentScrimColor(getResources().getColor(R.color.scrim_color));

            if (mSharedPreferences.getBoolean(getString(R.string.key_change_images_automatically), true)){
                startScrimTask();
            } else {
                setRandomScrimImage();
            }

            mImagesList.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (dy > 0){
                        if (fab.isShown()){
                            fab.hide();
                        }
                    } else if (dy < 0){
                        if (!fab.isShown()){
                            fab.show();
                        }
                    }
                    super.onScrolled(recyclerView, dx, dy);
                }
            });

        } catch (Exception e){
            e.printStackTrace();
            mHelper.showToast("Opps! Some Error Occurred");
        } finally {
            mHandler.postDelayed(() -> {
                mRefreshLayout.setRefreshing(false);
                TransitionManager.beginDelayedTransition(mRefreshLayout, new Fade(Fade.IN));
                TextView noImages = (TextView) findViewById(R.id.text_no_images);
                try {
                    if (mAdapter.getValues().isEmpty()){
                        noImages.setVisibility(View.VISIBLE);
                        noImages.setText(String.format("No Images in %s ", mHelper.getFilterTag()));
                        mImagesList.setVisibility(View.INVISIBLE);
                        fab.show();
                    } else {
                        noImages.setVisibility(View.INVISIBLE);
                        mImagesList.setVisibility(View.VISIBLE);
                    }
                } catch (Exception ignored){

                }
            }, 1000);
        }

    }

    Handler handler = new Handler();
    private void setRandomScrimImage(){
        if (mAdapter.getItemCount() > 0){
            final int randomId = new Random().nextInt(mAdapter.getItemCount());
            final String randomPath = mAdapter.getPath(randomId);

            scrim_image.setOnClickListener(v -> startImageViewerAt(randomId));

            Bitmap bitmap = BitmapFactory.decodeFile(randomPath);
            Palette.from(bitmap).generate(palette -> {
                try {
                    Palette.Swatch swatch = checkVibrantSwatch(palette);
                    int paletteColor;
                    if (swatch != null){
                        paletteColor = swatch.getRgb();
                        toolbarLayout.setBackgroundColor(paletteColor);
                        int color = palette.getVibrantColor(paletteColor);
                        LinearLayout header = (LinearLayout) drawer.findViewById(R.id.header_root);
                        header.setBackgroundColor(color);
                    }
                } catch (NullPointerException n){
                    n.printStackTrace();
                }
            });
            handler.post(() -> {
                scrim_image.setVisibility(View.INVISIBLE);
                TransitionManager.beginDelayedTransition(toolbarLayout, new Fade(Fade.OUT));
                scrim_image.setVisibility(View.VISIBLE);
                try {
                    Glide.with(MainActivity.this)
                            .load(new File(randomPath))
                            .centerCrop()
                            .crossFade()
                            .into(scrim_image);
                } catch (Exception e){
                    e.printStackTrace();
                }
            });
        }
    }

    private Timer mTimer;
    public void startScrimTask(){
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                setRandomScrimImage();
            }
        }, 0, 10000);
    }

    public void stopScrimTask(){
        scrim_image.setImageDrawable(null);
        if (mTimer != null){
            mTimer.cancel();
        }
    }

    private Palette.Swatch checkVibrantSwatch(Palette palette){
        Palette.Swatch swatch = palette.getVibrantSwatch();
        if (swatch != null){
            return swatch;
        } else {
            return checkMutedSwatch(palette);
        }
    }

    private Palette.Swatch checkMutedSwatch(Palette palette){
        Palette.Swatch swatch = palette.getMutedSwatch();
        if (swatch != null){
            return swatch;
        } else {
            return new Palette.Swatch(getResources().getColor(R.color.colorPrimary), 100);
        }
    }

    public void startImageViewerAt(int viewPagerPos){
        if (isInSelectionMode){
            toggleSelection(viewPagerPos);
        } else {
            Intent intent = new Intent(MainActivity.this, ImageViewerActivity.class);
            intent.putExtra(EXTRA_VIEWPAGER_POS, viewPagerPos);
            startActivityForResult(intent, REQUEST_CODE_IMAGE_VIEWER, null);
        }
    }

    @Override
    public void onItemClick(int pos) {
        startImageViewerAt(pos);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_IMAGE_VIEWER && resultCode == RESULT_OK){
            mImagesList.scrollToPosition(data.getExtras().getInt(EXTRA_GOTO_RECYCLER_POS, 0));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onItemLongClick(int pos) {
        startSelectionMode();
        toggleSelection(pos);
        return true;
    }

    public void startSelectionMode(){
        if (!isInSelectionMode){
            isInSelectionMode = true;
        }
        mRefreshLayout.setEnabled(false);
        setFabAsRemove();

        ObjectAnimator.ofFloat(mArrowDrawable, "progress", 1).start();

        toolbar.setNavigationIcon(mArrowDrawable);
        toolbar.setNavigationOnClickListener(view -> finishSelectionMode());
        toolbar.setSubtitle(mAdapter.getSelectedItemsCount() + "selected");

        mOptionsMenu.setGroupVisible(R.id.one, false);
        selectAll.setVisible(true);
        gridSize.setVisible(false);
    }

    public void finishSelectionMode(){
        if (isInSelectionMode){
            isInSelectionMode = false;
        }
        mRefreshLayout.setEnabled(true);
        setFabAsAdd();
        ObjectAnimator.ofFloat(mArrowDrawable, "progress", 0).start();

        toolbarLayout.setTitle(mHelper.getFilterTag());

        mAdapter.clearSelection();
        toolbar.setNavigationOnClickListener(view12 -> drawer.openDrawer(GravityCompat.START));
        mOptionsMenu.setGroupVisible(R.id.one, true);
        selectAll.setVisible(false);
        gridSize.setVisible(true);
    }

    private void toggleSelection(int position){
        mAdapter.toggleSelection(position);
        int count = mAdapter.getSelectedItemsCount();

        if (count == 0){
            finishSelectionMode();
        } else {
            toolbarLayout.setTitle(count + " selected");
        }
        mAdapter.notifyItemChanged(position);
    }

    private void showTagsWithTitle(String title){
        TagsListFragment fragment1 = TagsListFragment.newInstance(title);
        fragment1.show(getSupportFragmentManager(), "tag");
        fragment1.setOnClickEventListener(new TagsListFragment.OnClickEventListener() {
            @Override
            public void onTagClicked(String string) {
                Log.d("tag", "Tag Set: " + string);
                loadToGrid();
            }

            @Override
            public void onTagDeleted() {
                loadToGrid();
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (isInSelectionMode) {
            finishSelectionMode();
        } else if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    MenuItem selectAll, gridSize;
    Menu mOptionsMenu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        selectAll = menu.findItem(R.id.selectAll);
        gridSize = menu.findItem(R.id.action_span_count);
        mOptionsMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_span_count:
                new MaterialDialog.Builder(this)
                        .title("Choose Grid Size")
                        .items("1", "2", "3", "4")
                        .negativeText("Cancel")
                        .itemsCallbackSingleChoice(getSpanCount()-1, (dialog, itemView, which, text) -> {
                            switch (which){
                                case 0:
                                    mEditor.putInt(getString(R.string.key_span_count), 1).apply();
                                    break;
                                case 1:
                                    mEditor.putInt(getString(R.string.key_span_count), 2).apply();
                                    break;
                                case 2:
                                    mEditor.putInt(getString(R.string.key_span_count), 3).apply();
                                    break;
                                case 3:
                                    mEditor.putInt(getString(R.string.key_span_count), 4).apply();
                                    break;
                            }
                            loadToGrid();
                            return false;
                        }).show();
                break;
            case R.id.selectAll:
                mAdapter.selectAll();
                break;

            case R.id.action_tags:
                showTagsWithTitle("Choose Tag");
                break;

            case R.id.action_sortBy:
                new MaterialDialog.Builder(this)
                        .title("Sort By")
                        .items("Sort By Name", "Sort By Date")
                        .itemsCallbackSingleChoice(mHelper.getSortOrder()-1, (dialog, itemView, which, text) -> {
                            switch (which){
                                case Helper.SORT_BY_NAME-1:
                                    mHelper.setSortOrder(Helper.SORT_BY_NAME);
                                    break;
                                case Helper.SORT_BY_DATE-1:
                                    mHelper.setSortOrder(Helper.SORT_BY_DATE);
                                    break;
                            }
                            loadToGrid();
                            return false;
                        }).show();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.manage_tags:
                showTagsWithTitle("Choose Tag");
                break;
            case R.id.settings:
                Intent intent2 = new Intent(this, CustomFragmentActivity.class);
                intent2.putExtra(getString(R.string.key_fragment_title), "Settings");
                intent2.putExtra(getString(R.string.key_fragment_id), CustomFragmentActivity.SETTINGS_FRAGMENT);
                startActivity(intent2);
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private class ImagePickListener implements Picker.PickListener {

        @Override
        public void onPickedSuccessfully(final ArrayList<ImageEntry> images) {
            final AddToDatabaseTask addToVaultTask = new AddToDatabaseTask(MainActivity.this, MainActivity.this::loadToGrid);
            final MaterialDialog.Builder newTagDialog = new MaterialDialog.Builder(MainActivity.this)
                    .title("Add New Tag")
                    .icon(getResources().getDrawable(R.drawable.ic_tag))
                    .input("Please Enter Tag", null, false, (dialog, input) -> {
                        addToVaultTask.setTag(input.toString());
                        addToVaultTask.execute(images);
                    });
            MaterialDialog.Builder tagsDialog = new MaterialDialog.Builder(MainActivity.this)
                    .title("Assign Tag")
                    .items(mHelper.getTags())
                    .icon(getResources().getDrawable(R.drawable.ic_tag))
                    .itemsCallback((dialog, itemView, position, text) -> {
                        addToVaultTask.setTag(text.toString());
                        addToVaultTask.execute(images);
                    })
                    .positiveText("Add Tag")
                    .onPositive((dialog, which) -> newTagDialog.show())
                    .negativeText("Cancel");
            if (mHelper.getTags().isEmpty()){
                newTagDialog.show();
            } else {
                tagsDialog.show();
            }
        }

        @Override
        public void onCancel() {

        }
    }
}
