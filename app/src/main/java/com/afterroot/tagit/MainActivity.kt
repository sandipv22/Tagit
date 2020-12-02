package com.afterroot.tagit

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.palette.graphics.Palette
import com.afollestad.materialdialogs.MaterialDialog
import com.afterroot.tagit.adapter.ImagesRecyclerViewAdapter
import com.afterroot.tagit.databinding.ActivityMainBinding
import com.afterroot.tagit.fragment.MainFragment
import com.afterroot.tagit.fragment.TagsListFragment
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.pascalwelsch.extensions.launchActivity
import com.transitionseverywhere.Fade
import com.transitionseverywhere.TransitionManager
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, MainFragment.MainFragmentCallbacks {

    private lateinit var mHelper: Helper
    private lateinit var mSharedPreferences: SharedPreferences
    private lateinit var mEditor: SharedPreferences.Editor
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.includeAppBar.toolbar)

        mHelper = Helper(this)
        mSharedPreferences = mHelper.sharedPreferences
        mEditor = mSharedPreferences.edit()

        binding.navView.setNavigationItemSelectedListener(this)

        init()
    }

    private lateinit var mainFragment: MainFragment

    private fun init() {
        val file = File(mHelper.tagItPath)
        if (!file.exists()) {
            file.mkdirs()
        }

        mainFragment = MainFragment()
        Log.d("Testing", "main fragment load")
        if (mHelper.filterTag == "") {
            setFragment(mainFragment)
            showTagsWithTitle("Select Tag")
        } else {
            setFragment(mainFragment)
            mainFragment.setCallbacks(this)
        }
    }

    private var mFragmentManager: FragmentManager? = null
    private fun setFragment(fragment: Fragment) {
        if (mFragmentManager == null){
            mFragmentManager = supportFragmentManager
        }
        mFragmentManager!!.beginTransaction().replace(R.id.main_fragment, fragment).commit()
    }

    private var handler = Handler()
    private lateinit var mAdapter: ImagesRecyclerViewAdapter
    private fun setRandomScrimImage() {
        mAdapter = mainFragment.adapter
        try {
            if (mAdapter.itemCount > 0) {
                val randomId = Random().nextInt(mAdapter.itemCount)
                val randomPath = mAdapter.getPath(randomId)

                binding.includeAppBar.scrimImage.setOnClickListener { startImageViewerAt(randomId) }

                val bitmap = BitmapFactory.decodeFile(randomPath)!!
                Palette.from(bitmap).generate { palette ->
                    try {
                        val swatch = palette?.let { checkVibrantSwatch(it) }
                        val paletteColor: Int
                        if (swatch != null) {
                            paletteColor = swatch.rgb
                            binding.includeAppBar.toolbarLayout.setBackgroundColor(paletteColor)
                            val color = palette.getVibrantColor(paletteColor)
                            binding.navView.getHeaderView(0).setBackgroundColor(color)
                        }
                    } catch (n: NullPointerException) {
                        n.printStackTrace()
                    }
                }
                handler.post {
                    binding.includeAppBar.scrimImage.visibility = View.INVISIBLE
                    TransitionManager.beginDelayedTransition(binding.includeAppBar.toolbarLayout, Fade(Fade.OUT))
                    binding.includeAppBar.scrimImage.visibility = View.VISIBLE
                    try {
                        Glide.with(this)
                                .load(File(randomPath))
                                .into(binding.includeAppBar.scrimImage)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @SuppressLint("RestrictedApi")
    private fun startImageViewerAt(viewPagerPos: Int) {
        if (mainFragment.isInSelectionMode) {
            mainFragment.toggleSelection(viewPagerPos)
        } else {
            launchActivity<ImageViewerActivity>(Helper.REQUEST_CODE_IMAGE_VIEWER) {
                putExtra(Helper.EXTRA_VIEWPAGER_POS, viewPagerPos)
            }
        }
    }

    private var mTimer: Timer? = null
    private fun startScrimTask() {
        mTimer = Timer()
        mTimer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                try {
                    setRandomScrimImage()
                } catch (ignored: Exception) {

                }

            }
        }, 0, 10000)
    }

    private fun stopScrimTask() {
        binding.includeAppBar.scrimImage.setImageDrawable(null)
        if (mTimer != null) {
            mTimer!!.cancel()
        }
    }

    private fun checkVibrantSwatch(palette: Palette): Palette.Swatch? {
        val swatch = palette.vibrantSwatch
        return swatch ?: checkMutedSwatch(palette)
    }

    private fun checkMutedSwatch(palette: Palette): Palette.Swatch {
        val swatch = palette.mutedSwatch
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            swatch ?: Palette.Swatch(resources.getColor(R.color.colorPrimary, theme), 100)
        } else {
            swatch ?: Palette.Swatch(resources.getColor(R.color.colorPrimary), 100)
        }
    }

    private fun showTagsWithTitle(title: String) {
        val fragment1 = TagsListFragment.newInstance(title)
        fragment1.show(supportFragmentManager, "tag")
        fragment1.setOnClickEventListener(object : TagsListFragment.OnClickEventListener {
            override fun onTagClicked(string: String) {
                Log.d("tag", "Tag Set: $string")
                mainFragment.loadToGrid()
            }

            override fun onTagDeleted() {
                mainFragment.loadToGrid()
            }
        })
    }

    override fun onBackPressed() {
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        when {
            mainFragment.isInSelectionMode -> mainFragment.finishSelectionMode()
            drawer.isDrawerOpen(GravityCompat.START) -> drawer.closeDrawer(GravityCompat.START)
            else -> super.onBackPressed()
        }
    }

    private lateinit var selectAll: MenuItem
    private lateinit var gridSize: MenuItem
    private lateinit var mOptionsMenu: Menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        selectAll = menu.findItem(R.id.selectAll)
        gridSize = menu.findItem(R.id.action_span_count)
        mOptionsMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_span_count -> MaterialDialog.Builder(this)
                    .title("Choose Grid Size")
                    .items("1", "2", "3", "4")
                    .negativeText("Cancel")
                    .itemsCallbackSingleChoice(mainFragment.spanCount - 1) { _, _, which, _ ->
                        when (which) {
                            0 -> mEditor.putInt(getString(R.string.key_span_count), 1).apply()
                            1 -> mEditor.putInt(getString(R.string.key_span_count), 2).apply()
                            2 -> mEditor.putInt(getString(R.string.key_span_count), 3).apply()
                            3 -> mEditor.putInt(getString(R.string.key_span_count), 4).apply()
                        }
                        mainFragment.loadToGrid()
                        false
                    }.show()
            R.id.selectAll -> mAdapter.selectAll()

            R.id.action_tags -> showTagsWithTitle("Choose Tag")

            R.id.action_sortBy -> MaterialDialog.Builder(this)
                    .title("Sort By")
                    .items("Sort By Name", "Sort By Date")
                    .itemsCallbackSingleChoice(mHelper.sortOrder) { _, _, which, _ ->
                        when (which) {
                            0 -> mHelper.sortOrder = Helper.SORT_BY_NAME
                            1 -> mHelper.sortOrder = Helper.SORT_BY_DATE
                        }
                        mainFragment.loadToGrid()
                        false
                    }.show()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                launchActivity<CustomFragmentActivity> {
                    putExtra(getString(R.string.key_fragment_title), getString(R.string.action_settings))
                    putExtra(getString(R.string.key_fragment_id), CustomFragmentActivity.SETTINGS_FRAGMENT)

                }
            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    //Callbacks
    override fun onImageItemClick(pos: Int) {
        startImageViewerAt(pos)
    }

    override fun onImageItemLongClick(pos: Int) {

    }

    override fun onLoadToGrid() {
        stopScrimTask()
        if (mSharedPreferences.getBoolean(getString(R.string.key_change_images_automatically), true)) {
            startScrimTask()
        } else {
            setRandomScrimImage()
        }
    }

    override fun onSelectionModeStart() {
        mOptionsMenu.setGroupVisible(R.id.one, false)
        selectAll.isVisible = true
        gridSize.isVisible = false
    }

    override fun onSelectionModeFinish() {
        mOptionsMenu.setGroupVisible(R.id.one, true)
        selectAll.isVisible = false
        gridSize.isVisible = true
    }
}
