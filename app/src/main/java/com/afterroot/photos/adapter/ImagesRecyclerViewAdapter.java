package com.afterroot.photos.adapter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.afterroot.photos.Helper;
import com.afterroot.photos.R;
import com.bumptech.glide.Glide;
import com.transitionseverywhere.TransitionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ImagesRecyclerViewAdapter extends RecyclerView.Adapter<ImagesRecyclerViewAdapter.ViewHolder> {

    private final ArrayList<String> mValues;
    private final OnItemInteractionListener mListener;
    private final Context mContext;
    private SparseBooleanArray mSelectedItems;
    private Helper mHelper;

    public ImagesRecyclerViewAdapter(Context context, ArrayList<String> items, OnItemInteractionListener listener) {
        mContext = context;
        mValues = items;
        mListener = listener;
        mSelectedItems = new SparseBooleanArray();
        mHelper = new Helper(mContext);
    }

    public void clear(){
        mValues.clear();
    }

    public String getPath(int index){
        return mValues.get(index);
    }

    public ArrayList<String> getValues() {
        return mValues;
    }

    public boolean isSelected(int pos){
        return getSelectedItems().contains(pos);
    }

    public int getSelectedItemsCount() {
        return mSelectedItems.size();
    }

    public List<Integer> getSelectedItems(){
        List<Integer> items = new ArrayList<>(mSelectedItems.size());
        for (int i = 0; i < mSelectedItems.size(); ++i){
            items.add(mSelectedItems.keyAt(i));
        }
        return items;
    }

    private void removeItem(int pos){
        notifyItemRemoved(pos);
        mValues.remove(pos);
    }

    public void removeItems(List<Integer> positions){
        Collections.sort(positions, new Comparator<Integer>() {
            @Override
            public int compare(Integer lhs, Integer rhs) {
                return rhs - lhs;
            }
        });

        // Split the list in ranges
        while (!positions.isEmpty()) {
            if (positions.size() == 1) {
                removeItem(positions.get(0));
                positions.remove(0);
            } else {
                int count = 1;
                while (positions.size() > count && positions.get(count).equals(positions.get(count - 1) - 1)) {
                    ++count;
                }

                if (count == 1) {
                    removeItem(positions.get(0));
                } else {
                    removeRange(positions.get(count - 1), count);
                }

                for (int i = 0; i < count; ++i) {
                    positions.remove(0);
                }
            }
        }

    }

    private void removeRange(int positionStart, int itemCount) {
        for (int i = 0; i < itemCount; ++i) {
            mValues.remove(positionStart);
        }
        notifyItemRangeRemoved(positionStart, itemCount);
    }

    public void toggleSelection(int pos){
        if (mSelectedItems.get(pos, false)){
            mSelectedItems.delete(pos);
        } else {
            mSelectedItems.put(pos, true);
        }
    }

    public void selectAll(){
        for (int i = 0; i < getItemCount(); i++){
            mSelectedItems.put(i, true);
            notifyItemChanged(i);
        }
    }

    public void clearSelection(){
        List<Integer> selection = getSelectedItems();
        mSelectedItems.clear();
        for (Integer i : selection){
            notifyItemChanged(i);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        Glide.with(mContext)
                .load("file:///"+mValues.get(position))
                .centerCrop()
                .crossFade()
                .placeholder(new ColorDrawable(mContext.getResources().getColor(R.color.alter_album_background)))
                .into(holder.mContentView);

        holder.mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.onItemClick(holder.getAdapterPosition());
                }
            }
        });

        holder.mContentView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return mListener != null && mListener.onItemLongClick(holder.getAdapterPosition());
            }
        });

        TransitionManager.beginDelayedTransition((ViewGroup) holder.container);
        int padding = 16;
        if (isSelected(position)){
            holder.mContentView.setPadding(padding, padding, padding, padding);
        } else {
            holder.mContentView.setPadding(0, 0, 0, 0);
        }
        holder.mSelectedOverlay.setVisibility(isSelected(position) ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView mContentView;
        public final ImageView mSelectedOverlay;
        public final View container;
        public final AppCompatCheckBox mCheckBox;

        public ViewHolder(View view) {
            super(view);
            container = view.findViewById(R.id.image_container);
            mContentView = (ImageView) view.findViewById(R.id.content);
            mSelectedOverlay = (ImageView) view.findViewById(R.id.selected_overlay);
            mCheckBox = (AppCompatCheckBox) view.findViewById(R.id.checkbox);
        }
    }

    public interface OnItemInteractionListener {
        void onItemClick(int pos);
        boolean onItemLongClick(int pos);
    }
}
