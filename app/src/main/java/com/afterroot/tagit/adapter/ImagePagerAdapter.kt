package com.afterroot.tagit.adapter

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.afterroot.tagit.Helper
import com.afterroot.tagit.R
import com.bumptech.glide.Glide
import java.util.*

/**
 * Created by Sandip on 03-04-2017.
 */

class ImagePagerAdapter(context: Context, private val mItems: ArrayList<String>, private val mListener: ImagesRecyclerViewAdapter.OnItemInteractionListener) : PagerAdapter() {
    var mContext = context
    private var mHelper: Helper = Helper(mContext)
    private var inflater: LayoutInflater = LayoutInflater.from(mContext)

    fun getPath(index: Int): String {
        return mItems[index]
    }

    override fun getCount(): Int {
        return mItems.size
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        finishUpdate(container)
        container.removeView(`object` as View)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val imageLayout = inflater.inflate(R.layout.item_image_pager, container, false)!!
        val imageView = imageLayout.findViewById<View>(R.id.image) as ImageView
        imageLayout.setOnClickListener { v -> mListener.onItemClick(position) }

        Glide.with(container.context)
                .load(getPath(position))
                .into(imageView)
        container.addView(imageLayout, 0)
        return imageLayout
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }
}
