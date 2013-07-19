package com.android.contacts.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
/**
 * test delay the viewpager drag
 * xiepengfei
 *  2012.5.24
 */
/*Begin: Modified by xiepengfei for stop viewpager drag 2012/05/24*/
public class NoDragViewPager extends ViewPager {
    public NoDragViewPager(Context context) {
        super(context);
    }

    public NoDragViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {

      return false;
  }
/*End: Modified by xiepengfei for stop viewpager drag 2012/05/24*/
}
