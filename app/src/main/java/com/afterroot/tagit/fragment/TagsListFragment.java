package com.afterroot.tagit.fragment;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afterroot.tagit.DatabaseHelper;
import com.afterroot.tagit.Helper;
import com.afterroot.tagit.R;
import com.afterroot.tagit.adapter.TagsAdapter;
import com.transitionseverywhere.AutoTransition;
import com.transitionseverywhere.TransitionManager;

/**
 * A placeholder fragment containing a simple view.
 */
public class TagsListFragment extends BottomSheetDialogFragment {

    Helper mHelper;

    private OnClickEventListener mOnClickEventListener;
    public TagsListFragment() {

    }

    public static TagsListFragment newInstance(String title){
        TagsListFragment fragment = new TagsListFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnClickEventListener(OnClickEventListener listener){
        mOnClickEventListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mHelper = new Helper(getActivity());
        return inflater.inflate(R.layout.fragment_tags, container, false);
    }

    RecyclerView mTagList;
    TagsAdapter mTagsAdapter;
    AppCompatButton mAddTagButton;
    @Override
    public void onStart() {
        init();
        super.onStart();
    }

    public void init(){
        Handler handler = new Handler();
        mAddTagButton = (AppCompatButton) getView().findViewById(R.id.button_add_new_tag);
        TextView title = (TextView) getView().findViewById(R.id.tags_fragment_title);
        title.setText(getArguments().getString("title", "Tags"));
        mAddTagButton.setVisibility(View.INVISIBLE);
        handler.postDelayed(() -> {
            setUpTags();
            mAddTagButton.setOnClickListener(view -> showAddTagDialog());
            getView().findViewById(R.id.tags_progress).setVisibility(View.GONE);
        }, 500);
    }

    private void setUpTags(){

        mTagList = (RecyclerView) getView().findViewById(R.id.tags_list);
        RecyclerView.LayoutManager manager = new LinearLayoutManager(getActivity());
        mTagList.setLayoutManager(manager);

        mTagsAdapter = new TagsAdapter(mHelper.getTags());
        mTagsAdapter.setOnClickListener(new TagsAdapter.OnClickListener() {
            @Override
            public void onTagClicked(TagsAdapter.ViewHolder holder, int position) {
                mHelper.setFilterTag(mTagsAdapter.getTagAtPos(position));
                mOnClickEventListener.onTagClicked(mTagsAdapter.getTagAtPos(position));
                dismiss();
            }

            @Override
            public void onTagLongClick(TagsAdapter.ViewHolder holder, int position) {
                String oldTag = mTagsAdapter.getTagAtPos(position);
                new MaterialDialog.Builder(getContext())
                        .title("Edit Tag")
                        .input("New tag", oldTag, false, (dialog, newTag) -> {
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(DatabaseHelper.TableColumns.Companion.getCOLUMN_NAME_TAG(), newTag.toString());
                            try {
                                mHelper.getDatabase().update(DatabaseHelper.TableColumns.Companion.getTABLE_NAME_TAGS(), contentValues,
                                        DatabaseHelper.TableColumns.Companion.getCOLUMN_NAME_TAG() + "=?",
                                        new String[]{mTagsAdapter.getTagAtPos(position)});
                                mHelper.getDatabase().update(DatabaseHelper.TableColumns.Companion.getTABLE_NAME_IMAGES(), contentValues,
                                        DatabaseHelper.TableColumns.Companion.getCOLUMN_NAME_TAG() + "=?",
                                        new String[]{mTagsAdapter.getTagAtPos(position)});
                            } finally {
                                mHelper.getDatabase().close();
                                if (mHelper.getFilterTag().equals(oldTag)){
                                    mHelper.setFilterTag(newTag.toString());
                                }
                                init();
                            }
                        })
                        .show();
            }

            @Override
            public void onDeletePresssed(TagsAdapter.ViewHolder holder, int position) {
                try {
                    deleteTag(mTagsAdapter.getTagAtPos(position), position);
                } catch (ArrayIndexOutOfBoundsException e){
                    mHelper.showSnackbar(mTagList, "You are too fast!! 😀");
                }
            }
        });

        mTagList.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                return false;
            }
        });

        mTagList.setAdapter(mTagsAdapter);
        TransitionManager.beginDelayedTransition((ViewGroup) getView(), new AutoTransition());
        mTagList.setVisibility(View.VISIBLE);
        mAddTagButton.setVisibility(View.VISIBLE);
    }

    private void deleteTag(String tag, int position){
        new MaterialDialog.Builder(getContext())
                .title("Delete")
                .content(String.format("Are you sure you want to delete %s", tag))
                .positiveText("Yes")
                .onPositive((dialog, which) -> {
                    try {
                        mHelper.getDatabase().delete(DatabaseHelper.TableColumns.Companion.getTABLE_NAME_TAGS(),
                                DatabaseHelper.TableColumns.Companion.getCOLUMN_NAME_TAG() + "=?", new String[]{tag});
                        mHelper.getDatabase().delete(DatabaseHelper.TableColumns.Companion.getTABLE_NAME_IMAGES(),
                                DatabaseHelper.TableColumns.Companion.getCOLUMN_NAME_TAG() + "=?", new String[]{tag});
                        mTagsAdapter.notifyItemRemoved(position);
                        mTagsAdapter.getTagList().remove(position);
                        mOnClickEventListener.onTagDeleted();
                    } catch (Exception ignored) {

                    } finally {
                        mHelper.getDatabase().close();
                    }
                })
                .negativeText("No")
                .show();

    }

    public void showAddTagDialog(){
        MaterialDialog.Builder newTagDialog = new MaterialDialog.Builder(getActivity())
                .title("Add Tag")
                .icon(getResources().getDrawable(R.drawable.ic_tag))
                .input("Please Enter Tag", null, true, (dialog, input) -> {
                    String tag = input.toString();
                    if (tag != null && !tag.equals("") && !mHelper.getTags().contains(tag)){
                        ContentValues tagsValues = new ContentValues();
                        tagsValues.put(DatabaseHelper.TableColumns.Companion.getCOLUMN_NAME_TAG(), tag);
                        mHelper.getDatabase().insert(DatabaseHelper.TableColumns.Companion.getTABLE_NAME_TAGS(), null, tagsValues);
                    } else {
                        mHelper.showToast(tag + " already exists.");
                    }

                    setUpTags();
                });
        newTagDialog.show();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    public interface OnClickEventListener {
    void onTagClicked(String string);
        void onTagDeleted();
    }
}
