package com.afterroot.photos.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afterroot.photos.R;

import java.util.ArrayList;

/**
 * Created by Sandip on 22-03-2017.
 */

public class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.ViewHolder> {

    private ArrayList<String> mTagList;
    private OnClickListener mDeleteListener;

    public TagsAdapter(ArrayList<String> tagList){
        mTagList = tagList;
    }

    public void setOnClickListener(OnClickListener listener){
        mDeleteListener = listener;
    }

    public void clear(){
        mTagList.clear();
    }

    public String getTagAtPos(int pos){
        return mTagList.get(pos);
    }

    public ArrayList<String> getTagList(){
        return mTagList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_tag, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        holder.mTextTag.setText(getTagAtPos(holder.getAdapterPosition()));
        holder.mDeleteButton.setOnClickListener(v -> mDeleteListener.onDeletePresssed(holder, holder.getAdapterPosition()));
        holder.mItem.setOnClickListener(v -> mDeleteListener.onTagClicked(holder, holder.getAdapterPosition()));
        holder.mItem.setOnLongClickListener(v -> {
            mDeleteListener.onTagLongClick(holder, holder.getAdapterPosition());
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return mTagList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView mTextTag;
        ImageButton mDeleteButton;
        FrameLayout mItem;
        ViewHolder(View itemView) {
            super(itemView);
            mItem = (FrameLayout) itemView.findViewById(R.id.item_tag);
            mTextTag = (TextView) itemView.findViewById(R.id.text_tag);
            mDeleteButton = (ImageButton) itemView.findViewById(R.id.button_delete_tag);
        }
    }

    public interface OnClickListener {
        void onTagClicked(ViewHolder holder, int position);
        void onTagLongClick(ViewHolder holder, int position);
        void onDeletePresssed(ViewHolder holder, int position);
    }
}
