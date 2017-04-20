package com.afterroot.photos.adapter;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.afterroot.photos.Helper;
import com.afterroot.photos.R;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

/**
 * Created by Sandip on 03-04-2017.
 */

public class ImagePagerAdapter extends PagerAdapter {

    Context mContext;
    Helper mHelper;
    LayoutInflater inflater;
    private final ImagesRecyclerViewAdapter.OnItemInteractionListener mListener;
    private final ArrayList<String> mItems;

    public ImagePagerAdapter(Context context, ArrayList<String> items, ImagesRecyclerViewAdapter.OnItemInteractionListener listener){
        mContext = context;
        inflater = LayoutInflater.from(context);
        mListener = listener;
        mItems = items;
        mHelper= new Helper(mContext);

    }

    public String getPath(int index){
        return mItems.get(index);
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        finishUpdate(container);
        container.removeView((View) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        View imageLayout = inflater.inflate(R.layout.item_image_pager, container, false);
        assert imageLayout != null;
        ImageView imageView = (ImageView) imageLayout.findViewById(R.id.image);
        imageLayout.setOnClickListener(v -> mListener.onItemClick(position));

        Glide.with(container.getContext())
                .load(getPath(position))
                .into(imageView);
        container.addView(imageLayout, 0);
        return imageLayout;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }
}
