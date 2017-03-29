package com.afterroot.photos;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afterroot.photos.adapter.ImagesRecyclerViewAdapter;
import com.afterroot.photos.async.AddToDatabaseTask;
import com.nineoldandroids.animation.ObjectAnimator;

import net.yazeed44.imagepicker.model.ImageEntry;
import net.yazeed44.imagepicker.util.Picker;

import java.io.File;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ImagesRecyclerViewAdapter.OnItemInteractionListener {

    Helper mHelper;
    SharedPreferences mSharedPreferences;
    SharedPreferences.Editor mEditor;
    private boolean isInSelectionMode;
    DrawerArrowDrawable mArrowDrawable;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.drawer_layout) DrawerLayout drawer;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        mArrowDrawable = new DrawerArrowDrawable(this);
        mArrowDrawable.setColor(getResources().getColor(R.color.arrowDrawableColor));
        toolbar.setNavigationIcon(mArrowDrawable);

        mHelper = new Helper(this);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mSharedPreferences.edit();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //setUpTags();
        init();
    }

    public void init(){
        File file = new File(mHelper.getTagitPath());
        if (!file.exists()){
            file.mkdirs();
        }

        setFab();
        setSwipeRefresh();

        loadToGrid(getSpanCount(), false);
    }

    @BindView(R.id.fab) FloatingActionButton fab;
    public void setFab(){
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Picker.Builder(MainActivity.this, new ImagePickListener(), R.style.AppTheme_NoActionBar)
                        .setBackBtnInMainActivity(true)
                        .disableCaptureImageFromCamera()
                        .build()
                        .startActivity();
            }
        });
    }

    public int getSpanCount(){
        return mSharedPreferences.getInt(getString(R.string.key_span_count), 3);
    }

    @BindView(R.id.swipeRefresh) SwipeRefreshLayout mRefreshLayout;
    public void setSwipeRefresh(){
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadToGrid(getSpanCount(), false);
            }
        });
    }

    @BindView(R.id.tags_list) RecyclerView mTagList;
    /*TagsAdapter mTagsAdapter;
    public void setUpTags(){
        mTagList.setHasFixedSize(true);

        RecyclerView.LayoutManager manager = new LinearLayoutManager(this);
        mTagList.setLayoutManager(manager);

        mTagsAdapter = new TagsAdapter(mHelper.getTags());
        mTagsAdapter.setOnClickListener(new TagsAdapter.OnClickListener() {
            @Override
            public void onDeletePresssed(int position) {
                try {
                    deleteTag(mTagsAdapter.getTagAtPos(position));
                    mTagsAdapter.notifyItemRemoved(position);
                    mTagsAdapter.getTagList().remove(position);
                } catch (ArrayIndexOutOfBoundsException e){
                    mHelper.showSnackbar(mTagList, "You are too fast!! 😀");
                }
            }
        });
        mTagList.setAdapter(mTagsAdapter);
    }*/


    ImagesRecyclerViewAdapter mAdapter;
    private void loadToGrid(int spanCount, boolean isScrollToLastPos){
        try{
            mRefreshLayout.setRefreshing(true);
            mTagList = (RecyclerView) findViewById(R.id.tags_list);
            mAdapter = new ImagesRecyclerViewAdapter(this, mHelper.getImagePaths(), this);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this,
                    spanCount, LinearLayoutManager.VERTICAL, false);
            mTagList.setLayoutManager(gridLayoutManager);
            mTagList.setAdapter(mAdapter);
            mTagList.setItemAnimator(new DefaultItemAnimator());
            mTagList.setHasFixedSize(false);
            mTagList.invalidate();
            //RecyclerFastScroller recyclerFastScroller = (RecyclerFastScroller) findViewById(R.id.fast_scroller);
            //recyclerFastScroller.attachRecyclerView(mTagList);

            if (isScrollToLastPos){
                //mTagList.scrollToPosition(viewPagerPos);
                //viewPagerPos = 0;
            }

        } catch (Exception e){
            e.printStackTrace();
        } finally {
            mRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onItemClick(int pos) {
        if (isInSelectionMode){
            toggleSelection(pos);
        } else {
            //TODO Add ImageViewerACtivity
            //Intent intent = new Intent(MainActivity.this, com.afterroot.images.ImageViewerActivity.class);
            //intent.putExtra(com.afterroot.images.Utils.EXTRA_VIEWPAGER_POS, position);
            //startActivityForResult(intent, REQUEST_CODE_IMAGE_VIEWER, null);
            mHelper.showToast("Soon");
        }
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

        ObjectAnimator.ofFloat(mArrowDrawable, "progress", 1).start();

        toolbar.setNavigationIcon(mArrowDrawable);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishSelectionMode();
            }
        });
    }

    public void finishSelectionMode(){
        if (isInSelectionMode){
            isInSelectionMode = false;
        }
        mRefreshLayout.setEnabled(true);

        ObjectAnimator.ofFloat(mArrowDrawable, "progress", 0).start();

        toolbar.setTitle(getString(R.string.app_name));
        mAdapter.clearSelection();
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view12) {
                drawer.openDrawer(GravityCompat.START);
            }
        });
    }

    private void toggleSelection(int position){
        mAdapter.toggleSelection(position);
        int count = mAdapter.getSelectedItemsCount();

        if (count == 0){
            finishSelectionMode();
        } else if (count == 1) {
            toolbar.setTitle(count + " item selected");
        } else {
            toolbar.setTitle(count + " items selected");
        }
    }

    private void deleteTag(String tag){
        try {
            mHelper.getDatabase().delete(DatabaseHelper.TableColumns.TABLE_NAME_TAGS,
                    DatabaseHelper.TableColumns.COLUMN_NAME_TAG + "=?", new String[]{tag});
        } finally {
            mHelper.getDatabase().close();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_span_count:
                new MaterialDialog.Builder(this)
                        .title("Choose Grid Size")
                        .items("1", "2", "3", "4")
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                                switch (position){
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
                                loadToGrid(getSpanCount(), false);
                            }
                        }).show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void showAddTagDialog(){
        MaterialDialog.Builder newTagDialog = new MaterialDialog.Builder(MainActivity.this)
                .title("Add Tag")
                .icon(getResources().getDrawable(R.drawable.ic_tag))
                .input("Please Enter Tag", null, true, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        String tag = input.toString();
                        if (tag != null && !tag.equals("") && !mHelper.getTags().contains(tag)){
                            ContentValues tagsValues = new ContentValues();
                            tagsValues.put(DatabaseHelper.TableColumns.COLUMN_NAME_TAG, tag);
                            mHelper.getDatabase().insert(DatabaseHelper.TableColumns.TABLE_NAME_TAGS, null, tagsValues);
                            //setUpTags();
                        } else {
                            mHelper.showToast(tag + " already exists.");
                        }
                    }
                });
        newTagDialog.show();
    }

    private class ImagePickListener implements Picker.PickListener {

        @Override
        public void onPickedSuccessfully(final ArrayList<ImageEntry> images) {
            final AddToDatabaseTask addToVaultTask = new AddToDatabaseTask(MainActivity.this, new AddToDatabaseTask.OnPreExcuteListener() {
                @Override
                public void onTaskFinished() {
                    loadToGrid(getSpanCount(),false);
                }
            });
            final MaterialDialog.Builder newTagDialog = new MaterialDialog.Builder(MainActivity.this)
                    .title("Add New Tag")
                    .icon(getResources().getDrawable(R.drawable.ic_tag))
                    .input("Please Enter Tag", null, true, new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                            addToVaultTask.setTag(input.toString());
                            addToVaultTask.execute(images);
                        }
                    });
            MaterialDialog.Builder tagsDialog = new MaterialDialog.Builder(MainActivity.this)
                    .title("Assign Tag")
                    .items(mHelper.getTags())
                    .icon(getResources().getDrawable(R.drawable.ic_tag))
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                            addToVaultTask.setTag(text.toString());
                            addToVaultTask.execute(images);
                        }
                    })
                    .neutralText("Add Tag")
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            newTagDialog.show();
                        }
                    })
                    .negativeText("Cancel")
                    .positiveText("Add W/O Tagging")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            addToVaultTask.execute(images);
                        }
                    });
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
