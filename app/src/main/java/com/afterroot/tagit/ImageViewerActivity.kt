package com.afterroot.tagit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.view.ViewPager
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout

import com.afterroot.tagit.adapter.ImagePagerAdapter
import com.afterroot.tagit.adapter.ImagesRecyclerViewAdapter
import com.transitionseverywhere.Explode
import com.transitionseverywhere.Slide
import com.transitionseverywhere.TransitionManager

import java.io.File

class ImageViewerActivity : AppCompatActivity() {
    private val mHideHandler = Handler()
    private var mImagesPager: ViewPager? = null
    private val mHideSystemUi = Runnable {
        mImagesPager!!.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY }

    private var mVisible: Boolean = false
    private var mToolbar: ActionBar? = null
    private var bottomBarLayout: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_viewer)
        mToolbar = supportActionBar
        if (mToolbar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        mToolbar!!.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.transparent_black)))

        mVisible = true
        mImagesPager = findViewById<View>(R.id.images_pager) as ViewPager

        load()
    }

    private lateinit var mPagerAdapter: ImagePagerAdapter
    private lateinit var mHelper: Helper
    private fun load() {
        mHelper = Helper(this)
        mPagerAdapter = ImagePagerAdapter(this, mHelper.getImagePaths(mHelper.filterTag, mHelper.sortOrder),
                object : ImagesRecyclerViewAdapter.OnItemInteractionListener {
                    override fun onItemClick(pos: Int) {
                        toggle()
                    }

                    override fun onItemLongClick(pos: Int): Boolean {
                        return false
                    }
                })

        bottomBarLayout = findViewById<View>(R.id.bottom_bar_layout) as LinearLayout
        val shareButton = findViewById<View>(R.id.share_button) as ImageButton
        shareButton.setOnClickListener { v ->
            val file = File(mPagerAdapter.getPath(mImagesPager!!.currentItem))
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "image/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
            val chooser = Intent.createChooser(shareIntent, "Share Image")
            startActivity(chooser)
        }

        assert(mImagesPager != null)
        mImagesPager!!.adapter = mPagerAdapter
        try {
            supportActionBar!!.title = mHelper.filterTag
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mImagesPager!!.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                val i = Intent()
                i.putExtra(Helper.EXTRA_GOTO_RECYCLER_POS, mImagesPager!!.currentItem)
                setResult(Activity.RESULT_OK, i)
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })

        mImagesPager!!.currentItem = intent.extras!!.getInt(Helper.EXTRA_VIEWPAGER_POS, 0)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        hide()
    }

    private fun toggle() {
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        if (mToolbar != null) {
            mToolbar!!.hide()
        }
        if (bottomBarLayout != null) {
            TransitionManager.beginDelayedTransition(findViewById<View>(R.id.image_viewer_root) as ViewGroup, Explode())
            bottomBarLayout!!.visibility = View.INVISIBLE
        }
        mVisible = false
        mHideHandler.postDelayed(mHideSystemUi, UI_ANIMATION_DELAY.toLong())
    }

    @SuppressLint("InlinedApi")
    private fun show() {
        mImagesPager!!.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        mVisible = true
        if (mToolbar != null) {
            mToolbar!!.show()
        }
        if (bottomBarLayout != null) {
            TransitionManager.beginDelayedTransition(findViewById<View>(R.id.image_viewer_root) as ViewGroup, Slide(Gravity.BOTTOM))
            bottomBarLayout!!.visibility = View.VISIBLE
        }

        mHideHandler.removeCallbacks(mHideSystemUi)
    }

    companion object {

        private val UI_ANIMATION_DELAY = 0

        fun getSoftButtonsBarSizePort(activity: Activity): Int {
            // getRealMetrics is only available with API 17 and +
            val metrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(metrics)
            val usableHeight = metrics.heightPixels
            activity.windowManager.defaultDisplay.getRealMetrics(metrics)
            val realHeight = metrics.heightPixels
            return if (realHeight > usableHeight)
                realHeight - usableHeight
            else
                0
        }
    }
}
