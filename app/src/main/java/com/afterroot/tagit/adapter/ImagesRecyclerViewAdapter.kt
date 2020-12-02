package com.afterroot.tagit.adapter

import android.content.Context
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.afterroot.tagit.R
import com.bumptech.glide.Glide
import org.jetbrains.anko.find
import java.util.*

class ImagesRecyclerViewAdapter(private val mContext: Context, val values: ArrayList<String>, private val mListener: OnItemInteractionListener?) : RecyclerView.Adapter<ImagesRecyclerViewAdapter.ViewHolder>() {
    private val mSelectedItems: SparseBooleanArray = SparseBooleanArray()

    fun clear() {
        values.clear()
    }

    fun getPath(index: Int): String {
        return values[index]
    }

    private fun isSelected(pos: Int): Boolean {
        return selectedItems.contains(pos)
    }

    val selectedItemsCount: Int
        get() = mSelectedItems.size()

    val selectedItems: List<Int>
        get() {
            val items = ArrayList<Int>(mSelectedItems.size())
            (0 until mSelectedItems.size()).mapTo(items) { mSelectedItems.keyAt(it) }
            return items
        }

    private fun removeItem(pos: Int) {
        notifyItemRemoved(pos)
        values.removeAt(pos)
    }

    fun removeItems(positions: MutableList<Int>) {
        positions.sortWith { lhs, rhs -> rhs!! - lhs!! }

        // Split the list in ranges
        while (positions.isNotEmpty()) {
            if (positions.size == 1) {
                removeItem(positions[0])
                positions.removeAt(0)
            } else {
                var count = 1
                while (positions.size > count && positions[count] == positions[count - 1] - 1) {
                    ++count
                }

                if (count == 1) {
                    removeItem(positions[0])
                } else {
                    removeRange(positions[count - 1], count)
                }

                for (i in 0 until count) {
                    positions.removeAt(0)
                }
            }
        }

    }

    private fun removeRange(positionStart: Int, itemCount: Int) {
        for (i in 0 until itemCount) {
            values.removeAt(positionStart)
        }
        notifyItemRangeRemoved(positionStart, itemCount)
    }

    fun toggleSelection(pos: Int) {
        if (mSelectedItems.get(pos, false)) {
            mSelectedItems.delete(pos)
        } else {
            mSelectedItems.put(pos, true)
        }
    }

    fun selectAll() {
        Thread {
            for (i in 0 until itemCount) {
                mSelectedItems.put(i, true)
                notifyDataSetChanged()
            }
        }.run()
    }

    fun clearSelection() {
        mSelectedItems.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.image_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.clearAnim()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(mContext)
                .load("file:///" + values[position])
                .into(holder.mContentView)
        //setAnim(holder.container, position);
        holder.mContentView.setOnClickListener { v ->
            mListener?.onItemClick(holder.adapterPosition)
        }

        holder.mContentView.setOnLongClickListener { v -> mListener != null && mListener.onItemLongClick(holder.adapterPosition) }

        val padding = 16
        if (isSelected(position)) {
            holder.mContentView.setPadding(padding, padding, padding, padding)
        } else {
            holder.mContentView.setPadding(0, 0, 0, 0)
        }

        holder.mChecked.visibility = if (isSelected(position)) View.VISIBLE else View.INVISIBLE
    }

    fun setAnim(view: View, pos: Int) {
        var lastPos = -1
        if (pos > lastPos) {
            val scaleAnimation = ScaleAnimation(0.5f, 1.0f, 0.5f, 1.0f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
            scaleAnimation.duration = 250
            view.startAnimation(scaleAnimation)
            lastPos = pos
        }
    }

    override fun getItemCount(): Int {
        return values.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mContentView: ImageView
        val mChecked: ImageView
        val container: View = view.findViewById(R.id.image_container)

        init {
            mChecked = view.findViewById<View>(R.id.image_checked) as ImageView
            mContentView = container.find(R.id.image)
        }

        fun clearAnim() {
            container.clearAnimation()
        }
    }

    interface OnItemInteractionListener {
        fun onItemClick(pos: Int)
        fun onItemLongClick(pos: Int): Boolean
    }
}
