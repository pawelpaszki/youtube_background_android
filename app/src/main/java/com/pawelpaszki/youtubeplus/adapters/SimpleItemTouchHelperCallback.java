package com.pawelpaszki.youtubeplus.adapters;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import com.pawelpaszki.youtubeplus.interfaces.ItemTouchHelperAdapter;

/**
 * Created by PawelPaszki on 31/07/2017.
 */

public class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private final ItemTouchHelperAdapter mAdapter;

    public static boolean isLongPressEnabled() {
        return isLongPressEnabled;
    }

    public static void setIsLongPressEnabled(boolean isLongPressEnabled) {
        SimpleItemTouchHelperCallback.isLongPressEnabled = isLongPressEnabled;
    }

    private static boolean isLongPressEnabled = true;

    public SimpleItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
        mAdapter = adapter;
    }
    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        return makeMovementFlags(dragFlags, 0);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        mAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        // not used
    }
}
