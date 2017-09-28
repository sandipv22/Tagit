package com.afterroot.tagit.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView

import com.afterroot.tagit.R

import java.util.ArrayList

/**
 * Created by Sandip on 22-03-2017.
 */

class TagsAdapter(val tagList: ArrayList<String>) : RecyclerView.Adapter<TagsAdapter.ViewHolder>() {
    private var mDeleteListener: OnClickListener? = null

    fun setOnClickListener(listener: OnClickListener) {
        mDeleteListener = listener
    }

    fun clear() {
        tagList.clear()
    }

    fun getTagAtPos(pos: Int): String {
        return tagList[pos]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_tag, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.mTextTag.text = getTagAtPos(holder.adapterPosition)
        holder.mDeleteButton.setOnClickListener { v -> mDeleteListener!!.onDeletePresssed(holder, holder.adapterPosition) }
        holder.mItem.setOnClickListener { v -> mDeleteListener!!.onTagClicked(holder, holder.adapterPosition) }
        holder.mItem.setOnLongClickListener { v ->
            mDeleteListener!!.onTagLongClick(holder, holder.adapterPosition)
            false
        }
    }

    override fun getItemCount(): Int {
        return tagList.size
    }

    class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var mTextTag: TextView
        internal var mDeleteButton: ImageButton
        internal var mItem: FrameLayout

        init {
            mItem = itemView.findViewById<View>(R.id.item_tag) as FrameLayout
            mTextTag = itemView.findViewById<View>(R.id.text_tag) as TextView
            mDeleteButton = itemView.findViewById<View>(R.id.button_delete_tag) as ImageButton
        }
    }

    interface OnClickListener {
        fun onTagClicked(holder: ViewHolder, position: Int)
        fun onTagLongClick(holder: ViewHolder, position: Int)
        fun onDeletePresssed(holder: ViewHolder, position: Int)
    }
}
