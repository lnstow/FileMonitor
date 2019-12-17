package com.lnstow.filemonitor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

public class RvHorizontalScrollView extends HorizontalScrollView {
    //    private int mLastX = 0;
//    private int mLastY = 0;
    private int mLastXIntercept = 0;
    private int mLastYIntercept = 0;

    public RvHorizontalScrollView(Context context) {
        super(context);
    }

    public RvHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RvHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        return super.onInterceptTouchEvent(ev);
        boolean intercepted = false;
        int x = (int) ev.getX();
        int y = (int) ev.getY();

        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                intercepted = false;
                super.onInterceptTouchEvent(ev);
                break;

            case MotionEvent.ACTION_MOVE:
                int deltaX = x - mLastXIntercept;
                int deltaY = y - mLastYIntercept;
                if (Math.abs(deltaX) > Math.abs(deltaY) && (deltaX > 0 || deltaY > 0)) {
                    intercepted = true;
                } else {
                    intercepted = false;
                }
                break;

            case MotionEvent.ACTION_UP:
                intercepted = false;
                break;

            default:
                break;
        }
//        mLastX = x;
//        mLastY = y;
        mLastXIntercept = x;
        mLastYIntercept = y;
        return intercepted;
    }
}
