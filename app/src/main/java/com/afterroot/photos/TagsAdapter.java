package com.afterroot.photos;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

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
        holder.mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDeleteListener.onDeletePresssed(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mTagList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView mTextTag;
        ImageButton mDeleteButton;
        ViewHolder(View itemView) {
            super(itemView);
            mTextTag = (TextView) itemView.findViewById(R.id.text_tag);
            mDeleteButton = (ImageButton) itemView.findViewById(R.id.button_delete_tag);
        }
    }

    public interface OnClickListener {
        void onDeletePresssed(int position);
    }
}
