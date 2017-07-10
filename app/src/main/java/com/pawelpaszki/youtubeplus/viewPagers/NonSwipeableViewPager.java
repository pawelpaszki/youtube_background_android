package com.pawelpaszki.youtubeplus.viewPagers;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by PawelPaszki on 06/07/2017.
 */

public class NonSwipeableViewPager extends ViewPager {
    public NonSwipeableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NonSwipeableViewPager(Context context) {
        super(context);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            this.getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        } else {
            return super.onTouchEvent(event);
        }
    }
}
