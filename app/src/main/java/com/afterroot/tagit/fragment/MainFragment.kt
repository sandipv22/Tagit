package com.afterroot.tagit.fragment

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.tagit.Helper
import com.afterroot.tagit.R
import com.afterroot.tagit.TaskCallback
import com.afterroot.tagit.adapter.ImagesRecyclerViewAdapter
import com.afterroot.tagit.adapter.ImagesRecyclerViewAdapter.OnItemInteractionListener
import com.afterroot.tagit.async.AddToDatabaseTask
import com.afterroot.tagit.async.DeleteFromDatabaseTask
import com.afterroot.tagit.databinding.FragmentMainBinding
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nineoldandroids.animation.ObjectAnimator
import com.transitionseverywhere.Fade
import com.transitionseverywhere.TransitionManager
import net.yazeed44.imagepicker.model.ImageEntry
import net.yazeed44.imagepicker.util.Picker
import org.jetbrains.anko.find
import java.util.*

class MainFragment : androidx.fragment.app.Fragment(), OnItemInteractionListener {

    private var mHelper: Helper? = null
    private var mSharedPreferences: SharedPreferences? = null
    private lateinit var mEditor: SharedPreferences.Editor
    var isInSelectionMode: Boolean = false
    private lateinit var mContext: Context
    private var mArrowDrawable: DrawerArrowDrawable? = null
    private lateinit var mCallbacks: MainFragmentCallbacks
    private lateinit var binding: FragmentMainBinding
    private var fab: FloatingActionButton? = null

    fun setCallbacks(callbacks: MainFragmentCallbacks) {
        mCallbacks = callbacks
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        mHelper = Helper(requireContext())
        mContext = requireContext()
        Log.d("TEST", "TEST")
        setUpThings()
        return binding.root
    }

    @SuppressLint("CommitPrefEdits")
    private fun setUpThings() {
        mSharedPreferences = mHelper?.sharedPreferences
        mEditor = mSharedPreferences!!.edit()

        binding.swipeRefresh.setOnRefreshListener { this.loadToGrid() }

        val toggle = ActionBarDrawerToggle(
                activity, activity?.find(R.id.drawer_layout), activity?.find(R.id.toolbar), R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        activity?.find<DrawerLayout>(R.id.drawer_layout)?.addDrawerListener(toggle)
        toggle.syncState()

        mArrowDrawable = toggle.drawerArrowDrawable

        setFabAsAdd()
        loadToGrid()
    }

    private fun setFabAsAdd() {
        fab = activity?.find(R.id.fab)!!
        fab?.apply {
            setImageResource(R.drawable.ic_action_add)
            setOnClickListener {
                Picker.Builder(
                        mContext, ImagePickListener(), R.style.Picker)
                        .setBackBtnInMainActivity(true)
                        .disableCaptureImageFromCamera()
                        .build()
                        .startActivity()
            }
        }
    }

    private fun setFabAsRemove() {
        fab?.apply {
            setImageResource(R.drawable.ic_action_remove)
            setOnClickListener {
                val deleteFromDatabaseTask = DeleteFromDatabaseTask(mContext, adapter, object : TaskCallback {
                    override fun onTaskFinished() {
                        loadToGrid()
                    }

                })
                deleteFromDatabaseTask.execute(mAdapter!!.selectedItems)
                finishSelectionMode()
            }

        }
    }

    private var mAdapter: ImagesRecyclerViewAdapter? = null
    private lateinit var mHandler: Handler
    fun loadToGrid() {
        mHandler = Handler()
        binding.swipeRefresh.isRefreshing = true
        try {
            mAdapter = ImagesRecyclerViewAdapter(
                    mContext, mHelper!!.getImagePaths(mHelper?.filterTag, mHelper!!.sortOrder), this)
            mCallbacks.onLoadToGrid()
            TransitionManager.beginDelayedTransition(binding.swipeRefresh, Fade(Fade.OUT))
            binding.imagesList.apply {
                visibility = View.INVISIBLE
                val gridLayoutManager = GridLayoutManager(mContext, spanCount,
                        LinearLayoutManager.VERTICAL, false)
                layoutManager = gridLayoutManager
                adapter = mAdapter
                itemAnimator = DefaultItemAnimator()
                setHasFixedSize(true)
            }


            val recyclerFastScroller = binding.fastScroller
            recyclerFastScroller.attachRecyclerView(binding.imagesList)

            activity?.find<CollapsingToolbarLayout>(R.id.toolbar_layout)?.apply {
                title = mHelper?.filterTag
                setStatusBarScrimColor(resources.getColor(android.R.color.transparent))
                setContentScrimColor(resources.getColor(R.color.scrim_color))
            }

            binding.imagesList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        if (fab!!.isShown) {
                            fab?.hide()
                        }
                    } else if (dy < 0) {
                        if (!fab!!.isShown) {
                            fab?.show()
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
                binding.swipeRefresh.isRefreshing = false
                TransitionManager.beginDelayedTransition(binding.swipeRefresh, Fade(Fade.IN))
                val noImages = binding.textNoImages
                try {
                    if (mAdapter!!.values.isEmpty()) {
                        noImages.visibility = View.VISIBLE
                        noImages.text = String.format("No Images in %s ", mHelper?.filterTag)
                        binding.imagesList.visibility = View.INVISIBLE
                        fab?.show()
                    } else {
                        noImages.visibility = View.INVISIBLE
                        binding.imagesList.visibility = View.VISIBLE
                    }
                } catch (ignored: Exception) {

                }
            }, 1000)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Helper.REQUEST_CODE_IMAGE_VIEWER && resultCode == RESULT_OK) {
            binding.imagesList.scrollToPosition(data?.extras!!.getInt(Helper.EXTRA_GOTO_RECYCLER_POS, 0))
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
        binding.swipeRefresh.isEnabled = false
        setFabAsRemove()

        ObjectAnimator.ofFloat(mArrowDrawable, "progress", 1.toFloat()).start()

        val toolbar = activity?.find<Toolbar>(R.id.toolbar)
        toolbar?.apply {
            navigationIcon = mArrowDrawable
            setNavigationOnClickListener { finishSelectionMode() }
            subtitle = mAdapter!!.selectedItemsCount.toString() + "selected"

        }
        mCallbacks.onSelectionModeStart()
    }

    fun finishSelectionMode() {
        if (isInSelectionMode) {
            isInSelectionMode = false
        }
        binding.swipeRefresh.isEnabled = true
        setFabAsAdd()
        ObjectAnimator.ofFloat(mArrowDrawable, "progress", 0.toFloat()).start()

        activity?.find<CollapsingToolbarLayout>(R.id.toolbar_layout)?.title = mHelper?.filterTag

        mAdapter!!.clearSelection()
        activity?.find<Toolbar>(R.id.toolbar)?.setNavigationOnClickListener {
            activity?.find<DrawerLayout>(R.id.drawer_layout)?.openDrawer(GravityCompat.START)
        }
        mCallbacks.onSelectionModeFinish()

    }

    fun toggleSelection(position: Int) {
        mAdapter!!.toggleSelection(position)
        val count = mAdapter!!.selectedItemsCount

        if (count == 0) {
            finishSelectionMode()
        } else {
            activity?.find<Toolbar>(R.id.toolbar_layout)?.title = "$count selected"
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
            val addToVaultTask = AddToDatabaseTask(mContext, object : TaskCallback {
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
