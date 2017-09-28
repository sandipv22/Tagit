package com.afterroot.tagit.fragment

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.graphics.drawable.DrawerArrowDrawable
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.tagit.Helper
import com.afterroot.tagit.R
import com.afterroot.tagit.TaskCallback
import com.afterroot.tagit.adapter.ImagesRecyclerViewAdapter
import com.afterroot.tagit.adapter.ImagesRecyclerViewAdapter.OnItemInteractionListener
import com.afterroot.tagit.async.AddToDatabaseTask
import com.afterroot.tagit.async.DeleteFromDatabaseTask
import com.nineoldandroids.animation.ObjectAnimator
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller
import com.transitionseverywhere.Fade
import com.transitionseverywhere.TransitionManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.view.*
import net.yazeed44.imagepicker.model.ImageEntry
import net.yazeed44.imagepicker.util.Picker
import java.util.*

class MainFragment : Fragment(), OnItemInteractionListener {

    private var mHelper: Helper? = null
    private var mSharedPreferences: SharedPreferences? = null
    private lateinit var mEditor: SharedPreferences.Editor
    var isInSelectionMode: Boolean = false
    private lateinit var mContext: Context
    private lateinit var mFragmentView: View
    private var mArrowDrawable: DrawerArrowDrawable? = null
    private lateinit var mCallbacks: MainFragmentCallbacks

    fun setCallbacks(callbacks: MainFragmentCallbacks) {
        mCallbacks = callbacks
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater!!.inflate(R.layout.fragment_main, container, false)
        mFragmentView = view
        mHelper = Helper(activity)
        mContext = activity
        Log.d("TEST", "TEST")
        setUpThings()
        return view
    }

    @SuppressLint("CommitPrefEdits")
    private fun setUpThings() {
        mSharedPreferences = mHelper?.sharedPreferences
        mEditor = mSharedPreferences!!.edit()

        mFragmentView.swipeRefresh.setOnRefreshListener({ this.loadToGrid() })

        val toggle = ActionBarDrawerToggle(
                activity, activity.drawer_layout, activity.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        activity.drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        mArrowDrawable = toggle.drawerArrowDrawable

        setFabAsAdd()
        loadToGrid()
    }

    private fun setFabAsAdd() {
        activity.fab.setImageResource(R.drawable.ic_action_add)
        activity.fab.setOnClickListener {
            Picker.Builder(
                    mContext, ImagePickListener(), R.style.Picker)
                    .setBackBtnInMainActivity(true)
                    .disableCaptureImageFromCamera()
                    .build()
                    .startActivity()
        }
    }

    private fun setFabAsRemove() {
        activity.fab.setImageResource(R.drawable.ic_action_remove)
        activity.fab.setOnClickListener {
            val deleteFromDatabaseTask = DeleteFromDatabaseTask(mContext, adapter, object: TaskCallback{
                override fun onTaskFinished() {
                    loadToGrid()
                }

            })
            deleteFromDatabaseTask.execute(mAdapter!!.selectedItems)
            finishSelectionMode()
        }
    }

    private var mAdapter: ImagesRecyclerViewAdapter? = null
    private lateinit var mHandler: Handler
    fun loadToGrid() {
        mHandler = Handler()
        swipeRefresh.isRefreshing = true
        try {
            mAdapter = ImagesRecyclerViewAdapter(
                    mContext, mHelper!!.getImagePaths(mHelper?.filterTag, mHelper!!.sortOrder), this)
            mCallbacks.onLoadToGrid()
            TransitionManager.beginDelayedTransition(swipeRefresh, Fade(Fade.OUT))
            images_list.visibility = View.INVISIBLE
            val gridLayoutManager = GridLayoutManager(mContext, spanCount,
                    LinearLayoutManager.VERTICAL, false)
            images_list.layoutManager = gridLayoutManager
            images_list.adapter = mAdapter
            images_list.itemAnimator = DefaultItemAnimator()
            images_list.setHasFixedSize(true)
            val recyclerFastScroller = mFragmentView.findViewById<View>(R.id.fast_scroller) as RecyclerFastScroller
            recyclerFastScroller.attachRecyclerView(images_list)

            activity.toolbar_layout.title = mHelper?.filterTag
            activity.toolbar_layout.setStatusBarScrimColor(resources.getColor(android.R.color.transparent))
            activity.toolbar_layout.setContentScrimColor(resources.getColor(R.color.scrim_color))

            images_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                    if (dy > 0) {
                        if (fab!!.isShown) {
                            fab.hide()
                        }
                    } else if (dy < 0) {
                        if (!fab!!.isShown) {
                            fab.show()
                        }
                    }
                    super.onScrolled(recyclerView, dx, dy)
                }
            })

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("loadToGrid", e.toString())
            Helper(mContext).showToast("Opps! Some Error Occurred")
        } finally {
            mHandler.postDelayed({
                swipeRefresh.isRefreshing = false
                TransitionManager.beginDelayedTransition(swipeRefresh, Fade(Fade.IN))
                val noImages = mFragmentView.findViewById<View>(R.id.text_no_images) as TextView
                try {
                    if (mAdapter!!.values.isEmpty()) {
                        noImages.visibility = View.VISIBLE
                        noImages.text = String.format("No Images in %s ", mHelper?.filterTag)
                        images_list.visibility = View.INVISIBLE
                        fab.show()
                    } else {
                        noImages.visibility = View.INVISIBLE
                        images_list.visibility = View.VISIBLE
                    }
                } catch (ignored: Exception) {

                }
            }, 1000)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == Helper.Companion.REQUEST_CODE_IMAGE_VIEWER && resultCode == RESULT_OK) {
            images_list.scrollToPosition(data.extras!!.getInt(Helper.Companion.EXTRA_GOTO_RECYCLER_POS, 0))
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onItemClick(pos: Int) {
        mCallbacks.onImageItemClick(pos)
    }

    override fun onItemLongClick(pos: Int): Boolean {
        startSelectionMode()
        toggleSelection(pos)
        mCallbacks.onImageItemLongClick(pos)
        return true
    }

    private fun startSelectionMode() {
        if (!isInSelectionMode) {
            isInSelectionMode = true
        }
        swipeRefresh.isEnabled = false
        setFabAsRemove()

        ObjectAnimator.ofFloat(mArrowDrawable, "progress", 1.toFloat()).start()

        toolbar.navigationIcon = mArrowDrawable
        toolbar.setNavigationOnClickListener { finishSelectionMode() }
        toolbar.subtitle = mAdapter!!.selectedItemsCount.toString() + "selected"
        mCallbacks.onSelectionModeStart()
    }

    fun finishSelectionMode() {
        if (isInSelectionMode) {
            isInSelectionMode = false
        }
        swipeRefresh.isEnabled = true
        setFabAsAdd()
        ObjectAnimator.ofFloat(mArrowDrawable, "progress", 0.toFloat()).start()

        activity.toolbar_layout.title = mHelper?.filterTag

        mAdapter!!.clearSelection()
        toolbar.setNavigationOnClickListener { drawer_layout.openDrawer(GravityCompat.START) }
        mCallbacks.onSelectionModeFinish()

    }

    fun toggleSelection(position: Int) {
        mAdapter!!.toggleSelection(position)
        val count = mAdapter!!.selectedItemsCount

        if (count == 0) {
            finishSelectionMode()
        } else {
            activity.toolbar_layout.title = count.toString() + " selected"
        }
        mAdapter!!.notifyItemChanged(position)
    }


    val spanCount: Int
        get() = mSharedPreferences!!.getInt(getString(R.string.key_span_count), 3)

    val adapter: ImagesRecyclerViewAdapter
        get() {
            if (mAdapter == null) {
                mAdapter = ImagesRecyclerViewAdapter(
                        mContext, mHelper!!.getImagePaths(mHelper?.filterTag, mHelper!!.sortOrder), this)
            }
            return mAdapter as ImagesRecyclerViewAdapter
        }

    private inner class ImagePickListener : Picker.PickListener {

        override fun onPickedSuccessfully(images: ArrayList<ImageEntry>) {
            val addToVaultTask = AddToDatabaseTask(mContext, object: TaskCallback {
                override fun onTaskFinished() {
                    loadToGrid()
                }

            })
            val newTagDialog = MaterialDialog.Builder(mContext)
                    .title("Add New Tag")
                    .icon(resources.getDrawable(R.drawable.ic_tag))
                    .input("Please Enter Tag", null, false) { _, input ->
                        addToVaultTask.setTag(input.toString())
                        addToVaultTask.execute(images)
                    }
            val tagsDialog = MaterialDialog.Builder(mContext)
                    .title("Assign Tag")
                    .items(mHelper!!.tags)
                    .icon(resources.getDrawable(R.drawable.ic_tag))
                    .itemsCallback { _, _, _, text ->
                        addToVaultTask.setTag(text.toString())
                        addToVaultTask.execute(images)
                    }
                    .positiveText("Add Tag")
                    .onPositive { _, _ -> newTagDialog.show() }
                    .negativeText("Cancel")
            if (mHelper!!.tags.isEmpty()) {
                newTagDialog.show()
            } else {
                tagsDialog.show()
            }
        }

        override fun onCancel() {

        }
    }

    interface MainFragmentCallbacks {
        fun onImageItemClick(pos: Int)
        fun onImageItemLongClick(pos: Int)
        fun onSelectionModeStart()
        fun onSelectionModeFinish()
        fun onLoadToGrid()
    }
}
