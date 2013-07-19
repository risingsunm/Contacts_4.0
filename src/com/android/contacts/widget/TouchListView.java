
package com.android.contacts.widget;

/**Copyright(c) 2012 深圳凯虹移动通信有限公司 All rights reserved
 * xiepengfei 2012.4.5
 * khong 0868
 * touch listview Widget
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.android.contacts.R;

import java.lang.reflect.Field;

/*Begin: Modified by xiepengfei for add touch listview 2012/06/07*/
public class TouchListView extends ListView implements OnGestureListener {
    private final static String TAG = "TouchListView";
    private final static boolean DBG = false;
    private GestureDetector mGestureDetector;
    private Paint mPaint;
    private Paint textPaint;
//  private ViewGroup itemView; // comment by bxinchun 2012-08-03
    private final static int FLING_MIN_DISTANCE = 50;// X数轴上移动的距离，100px
    private final static int FLING_MIN_VELOCITY = 200;// X数轴上移动速度，200px/s
    private boolean longClickable = true;
    private static Bitmap mCallBmpbg;// 向右滑动时的背景
    private static Bitmap mMsgBmpbg;// 向左滑动时的背景

    private static Bitmap mCallIcon;
    private static Bitmap mMsgIcon;

    /**
     * 被拖拽项的影像
     */
    private Bitmap mDragBitmap;

    private String mCallStr;
    private String mMsgStr;

    private int norItemHeight = 96;
    private int mModifyHeight = 0;// 有时候列表item高度超过96，这个是超过的像素数

    private boolean isDraging = false;
    /**
     * right is false,left is true.default right;
     */
    //private boolean isMoveingToRight = false;

    private int mStartX = 0;
    private int mNowX = 0;

    private int mStartY = 0;
    private int mNowY = 0;

    private int mItemX = 0;

    private boolean isRealyMove = true;

    private boolean isAlreadyShowPress = false;

    private Move mMove;

    private TriggerListener mTriggerListener;

    /**
     * if false, then the list view is a normal list view.
     */
    protected boolean isNeedTouchMode = false;
    private int top = 0, width = 0, height = 0;

    /**
     * 手指拖动项原始在列表中的位置
     */
    private int dragSrcPosition;

    /**
     * The method is used to speed up the scroll velocity.
     */
    private void adjustFieldDefaultVal() {
        try {
            Field f = null;
            Class c = getClass().getSuperclass();
            while (c != null) {
                try {
                    c = c.getSuperclass();
                    f = c.getDeclaredField("mVelocityScale");
                    if (f != null) {
                        break;
                    }
                } catch (NoSuchFieldException e) {
                    System.out.println("NoSuchFieldException");
                }
            };
            if (f != null) {
                f.setAccessible(true);
                f.setFloat(this, 3f);
            } else {
                log("change field mVelocityScale fail !!");
            }

            Field f1 = null;
            c = getClass().getSuperclass();
            while (c != null) {
                try {
                    c = c.getSuperclass();
                    f1 = c.getDeclaredField("mMaximumVelocity");
                    if (f1 != null) {
                        break;
                    }
                } catch (NoSuchFieldException e) {
                    System.out.println("NoSuchFieldException");
                }
            };
            if (f1 != null) {
                f1.setAccessible(true);
                f1.setInt(this, 1500);
            } else {
                log("change field mMaximumVelocity fail !!");
            }
        }  catch (IllegalArgumentException e) {
            System.out.println("IllegalArgumentException");
        } catch (IllegalAccessException e) {
            System.out.println("IllegalAccessException");
        }
    }

    public TouchListView(Context context) {
        this(context, null);
    }

    public TouchListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TouchListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        adjustFieldDefaultVal();

        mGestureDetector = new GestureDetector(context, this);
        init();
    }

    private void init() {
        Resources res = getResources();

        mCallStr = res.getString(R.string.speed_call);
        mMsgStr = res.getString(R.string.speed_sms);
        if(mCallBmpbg==null)
        mCallBmpbg = BitmapFactory.decodeResource(res,
                R.drawable.ic_tl_sweeplist_call_tab);
        if(mMsgBmpbg==null)
        mMsgBmpbg = BitmapFactory.decodeResource(res,
                R.drawable.ic_tl_sweeplist_message_tab);
        if(mCallIcon==null)
        mCallIcon = BitmapFactory.decodeResource(res,
                R.drawable.ic_tl_sweeplist_call_icon);
        if(mMsgIcon==null)
        mMsgIcon = BitmapFactory.decodeResource(res,
                R.drawable.ic_tl_sweeplist_message_icon);

        // 初始化彩带画笔
        mPaint = new Paint();
        mPaint.setAlpha(80);

        // 初始化文字画笔
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(38);
        textPaint.setTextAlign(Align.CENTER);
        textPaint.setAntiAlias(true);

        mMove = new Move();
    }

    public void setTouchMode(boolean mode) {
        this.isNeedTouchMode = mode;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        log("onTouchEvent action :" + ev.getAction() + ", isDraging :" + isDraging
                + ", isNeedTouchMode =" + isNeedTouchMode);
        if (!isNeedTouchMode) {
            return super.onTouchEvent(ev);
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_UP:
                isAlreadyShowPress = false;
                log("onTouchEvent  ACTION_UP, ismove :" + mMove.isMove());
                if (!mMove.isMove() && isDraging) {
                    isDraging = false;
                    boolean moveRight = (mNowX - mStartX) >= 0;
                    int x = Math.abs(mNowX - mStartX);
                    log("scrolled x distance :" + x + ", start :" + mStartX + ", end :" + mNowX);

                    if (x <= 240) {// 进行复位操作
                        log("resume isRight :" + !moveRight);
                        int speed = x / 15;
                        if (speed < 4) {
                            speed = 4;
                        }
                        mMove = new Move(Move.ACTION_TYPE_RESUME, !moveRight, false);
                        mMove.setSpeedLow(speed);
                        mMove.start();
                    } else {
                        // action,左右滑动操作
                        if (moveRight) {
                            mMove = new Move(Move.ACTION_TYPE_MOVE_RIGHT, true, false);
                            log("action move to right .");
                        } else {
                            mMove = new Move(Move.ACTION_TYPE_MOVE_LEFT, false, false);
                            log("action move to left .");
                        }
                        mMove.start();
                    }

                    log("return mGestureDetector.onTouchEvent(ev)");
                    setPressed(false);
                    for (int i = 0; i < getChildCount(); i++) {
                        if (getChildAt(i).isPressed())
                            getChildAt(i).setPressed(false);
                    }
                    return mGestureDetector.onTouchEvent(ev);
                } else {
                    log("return super.onTouchEvent(ev)");
                    /*Begin: Modified by bxinchun 2012-08-03*/
                    setPressed(false);
                    for (int i = 0; i < getChildCount(); i++) {
                        if (getChildAt(i).isPressed())
                            getChildAt(i).setPressed(false);
                    }
                    /*End: Modified by bxinchun 2012-08-03*/
                    return super.onTouchEvent(ev);
                }
                //break;

            case MotionEvent.ACTION_MOVE:
                log("onTouchEvent  ACTION_MOVE xstart :" + mStartX + ", nowx :" + mNowX
                        + ", ystart :" + mStartY + ", nowy :" + mNowY);

                mNowX = (int) ev.getX();
                mNowY = (int) ev.getY();

                int x = Math.abs(mNowX - mStartX);
                int y = Math.abs(mNowY - mStartY);

                log("onTouchEvent action *******MOVE*********** x :" + x + ", y :" + y);
                /*Begin: Modified by bxinchun, make the angle between start-end line and the x axis is less than 45'  2012-08-03*/
                //if(y>=0&& y<15 && x>4){//滑动角度调整
                if (x >= y && (x >= 10)) {
                /*End: Modified by bxinchun 2012-08-03*/
                    log("action is already move");
                    isRealyMove = true;
                } else {
                    log("action is unknow, not handled.");
                    isRealyMove = false;
                }

                if (isRealyMove || isDraging) {
                    log("return mGestureDetector.onTouchEvent(ev)");
                    longClickable = false;
                    return mGestureDetector.onTouchEvent(ev);
                } else {
                    log("return super.onTouchEvent(ev)");
                    return super.onTouchEvent(ev);
                }

            case MotionEvent.ACTION_DOWN:
                longClickable = true;
                super.onTouchEvent(ev);
                return mGestureDetector.onTouchEvent(ev);
                // break;
            default:
                return mGestureDetector.onTouchEvent(ev);
                // break;
        }

    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {

        // 捕获down事件
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            log("onInterceptTouchEvent ACTION_DOWN");
            if(mMove!= null && mMove.isMove()){
                mMove.setMove(false);
            }

            int x = (int) ev.getX();
            int y = (int) ev.getY();

            // 选中的数据项位置，使用ListView自带的pointToPosition(x, y)方法
            dragSrcPosition = pointToPosition(x, y);
            if (mDragBitmap != null) {
                mDragBitmap.recycle();
                mDragBitmap = null;
            }
            // 如果是无效位置(超出边界，分割线等位置)，返回
            if (dragSrcPosition == AdapterView.INVALID_POSITION) {
                log("onInterceptTouchEvent INVALID_POSITION");
                return super.onInterceptTouchEvent(ev);
            }

            /*Begin: Added by bxinchun, limit the trigglable positions 2012-07-31*/
            final int headerCount = getHeaderViewsCount();
            if (headerCount > 0 && dragSrcPosition < headerCount) {
                return super.onInterceptTouchEvent(ev);
            }

            if (mTriggerListener != null && !mTriggerListener.isTriggable(dragSrcPosition)) {
                return super.onInterceptTouchEvent(ev);
            }
            /*End: Added by bxinchun, limit the trigglable positions 2012-07-31*/

            // 获取选中项View
            // getChildAt(int position)显示display在界面的position位置的View
            // getFirstVisiblePosition()返回第一个display在界面的view在adapter的位置position，可能是0，也可能是4

            ViewGroup itemView = (ViewGroup) getChildAt(dragSrcPosition - getFirstVisiblePosition());

            /*Begin: Modified by bxinchun 2012-08-03*/
            itemView.setDrawingCacheEnabled(true);
            /*itemView.buildDrawingCache();
            mDragBitmap = Bitmap.createBitmap(itemView.getDrawingCache());
            int h = mDragBitmap.getHeight();
            if (h > 96) {
                mModifyHeight = h - 96;
                mDragBitmap = Bitmap.createBitmap(mDragBitmap, 0, mModifyHeight, mDragBitmap.getWidth(), 96);
            }*/

            Bitmap srcBmp = itemView.getDrawingCache();
            int h = srcBmp.getHeight();
            if (h > norItemHeight) {
                mModifyHeight = h - norItemHeight;
                mDragBitmap = Bitmap.createBitmap(srcBmp, 0, mModifyHeight, srcBmp.getWidth(), norItemHeight);
            } else {
                mDragBitmap = Bitmap.createBitmap(srcBmp); // drag bitmap may be null.
            }
            srcBmp = null;

            itemView.setDrawingCacheEnabled(false);
            // if(!mMove.isMove())
            top = itemView.getTop();
            width = itemView.getWidth();
            height = itemView.getHeight();

            mStartX = (int) ev.getX();
            mNowX = mStartX;

            mStartY = (int) ev.getY();
            mNowY = mStartY;

            log("mStartX:" + mStartX + ", StartY:" + mStartY);
            /*End: Modified by bxinchun 2012-08-03*/
        }

        return super.onInterceptTouchEvent(ev);
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean b = super.drawChild(canvas, child, drawingTime);

        if (mMove != null && mMove.isMove()) {
            // fling right

            canvas.save();
            // 将画布移动到要画的条目处
            canvas.translate(0, top);

            if (mMove.getActionType() == Move.ACTION_TYPE_RESUME) {
                /*Begin: Modified by bxinchun 2012-08-03*/
                //if (mStartX < 240) {
                if (mMove.isMovingRight) {
                /*End: Modified by bxinchun 2012-08-03*/
                    canvas.drawBitmap(mCallBmpbg, -580 + mItemX, height - norItemHeight,
                            null);
                } else {
                    canvas.drawBitmap(mMsgBmpbg, -140 + mItemX,
                            height - norItemHeight, null);
                }
            } else {
                String text;
                if (mMove.isMovingRight()) {
                    canvas.drawBitmap(mCallBmpbg, -580 + mItemX, height - norItemHeight, null);
                    canvas.drawBitmap(mCallIcon, 370, height - norItemHeight + 7, null);
                    text = mCallStr;
                } else {
                    canvas.drawBitmap(mMsgBmpbg, -140 + mItemX, height - norItemHeight, null);
                    /*Begin: Modified by bxinchun for drawing the msg icon on the left side 2012/07/16*/
                    //canvas.drawBitmap(mMessageIcon, 370, height - 96 + 7, null);
                    canvas.drawBitmap(mMsgIcon, 0, height - norItemHeight + 7, null);
                    /*End: Modified by bxinchun 2012/07/16*/
                    text = mMsgStr;
                }
                int x = Math.abs(mNowX - mStartX);
                if (x >= 95) {
                    canvas.drawText(text, width / 2f, height - 48 + 15, textPaint);
                }

            }

            if (mDragBitmap != null) {
                canvas.drawBitmap(mDragBitmap, mItemX, height - norItemHeight, mPaint);
            }

            // 还原画布
            canvas.restore();
            // 刷新View
            this.postInvalidate();
        } else if (isDraging) {
            canvas.save();
            // 将画布移动到要画的条目处
            canvas.translate(0, top);
            // 画文字
            int x = Math.abs(mNowX - mStartX);
            int p = mNowX - mStartX;
            if(p>0){
                canvas.drawBitmap(mCallBmpbg, -580 + mNowX, height - norItemHeight, null);
                canvas.drawBitmap(mCallIcon, 370, height - norItemHeight + 7, null);
                if (x >= 95) {
                    canvas.drawText(mCallStr, width / 2f, height - 48 + 15, textPaint);
                }
            }else{
                canvas.drawBitmap(mMsgBmpbg, -580 + mNowX, height - norItemHeight, null);
                /*Begin: Modified by bxinchun for drawing the msg icon on the left side 2012/07/16*/
                //canvas.drawBitmap(mMessageIcon, 370, height - 96 + 7, null);
                canvas.drawBitmap(mMsgIcon, 0, height - norItemHeight + 7, null);
                /*End: Modified by bxinchun 2012/07/16*/
                if (x >= 95) {
                    canvas.drawText(mMsgStr, width / 2f, height - 48 + 15, textPaint);
                }
            }

            if (mDragBitmap != null) {
                canvas.drawBitmap(mDragBitmap, mNowX - mStartX, height - norItemHeight, mPaint);
            }

            // 还原画布
            canvas.restore();
            // 刷新View
            this.postInvalidate();
        }

        return b;
    }

    private void log(String s) {
        if (DBG)
            Log.v(TAG, s);
    }

    public boolean onDown(MotionEvent e) {
        log("onDown");
        return true;
    }

    public void onShowPress(MotionEvent e) {
        log("onShowPress isRealyMove :"+isRealyMove+", isDraging :"+isDraging);
        log("onShowPress action :"+e.getAction());
       int y = Math.abs(mNowY - mStartY);
        if((y==0||y==1 ||y==2) && !isRealyMove){
            log("super.onTouchEvent(e);");
            isAlreadyShowPress = true;
            super.onTouchEvent(e);
        }else{
            isAlreadyShowPress = false;
        }

    }

    public void onLongPress(MotionEvent e) {
        log("onLongPress");
    }

    public boolean onSingleTapUp(MotionEvent e) {
        log("onSingleTapUp");
        return super.onTouchEvent(e);
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) {
        log("onScroll");
        if (isAlreadyShowPress) return true;
        if (e2.getAction() == MotionEvent.ACTION_MOVE) {

            log("onScroll action1:" + e2.getAction() + ", action2:"
                    + e2.getAction() + ", distanceX:" + distanceX);
            isDraging = true;
            mNowX = (int) e2.getX();
            log("onScroll mNowX:" + mNowX);

            this.postInvalidate();
            return false;
        }
        return true;
    }

    public boolean isLongClickable(){
        return longClickable;
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        log("onFling");
        // log("onFling velocityX:" + velocityX + "action1:" + e1.getAction() +
        // ", action2:"
        // + e2.getAction());
        // log("e1-e2:" + (e1.getX() - e2.getX()));
        // 参数解释：
        // e1：第1个ACTION_DOWN MotionEvent
        // e2：最后一个ACTION_MOVE MotionEvent
        // velocityX：X轴上的移动速度，像素/秒
        // velocityY：Y轴上的移动速度，像素/秒

        // 触发条件 ：
        // X轴的坐标位移大于FLING_MIN_DISTANCE，且移动速度大于FLING_MIN_VELOCITY个像素/秒
        if (e1 == null || e2 == null) {
            return true;
        }
        if (e1.getX() - e2.getX() > FLING_MIN_DISTANCE
                && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
            // Fling left
            log("Fling left" + mMove.isMove());
            if (mMove.isMove()) {
                mMove.setMove(false);
            }
            log("Fling left strat");
            mMove = new Move(Move.ACTION_TYPE_MOVE_LEFT, false, true);
            mMove.start();
            return true;
        } else if (e2.getX() - e1.getX() > FLING_MIN_DISTANCE
                && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
            // Fling right
            log("Fling right" + mMove.isMove());
            if (mMove.isMove()) {
                mMove.setMove(false);
            }
            log("Fling right start");
            mMove = new Move(Move.ACTION_TYPE_MOVE_RIGHT, true, true);
            mMove.start();
            return true;
        }
        return true;
    }

    private class Move extends Thread {
        public final static int ACTION_TYPE_RESUME = 0;
        public final static int ACTION_TYPE_MOVE_RIGHT = 1;
        public final static int ACTION_TYPE_MOVE_LEFT = 2;
        private boolean isMove = false;
        private int actionType;
        private boolean isMovingRight;

        private boolean isFling;

        private final int SPEED_HIGH = 16;
        private final int SPEED_MIDDING = 16;
        private int SPEED_LOW = 16;

        private int moveSpeed;

        public Move() {

        }

        public void setSpeedLow(int s) {
            this.SPEED_LOW = s;
        }

        public Move(int action, boolean isRight, boolean isfling) {
            this.isMove = true;
            this.actionType = action;
            this.isMovingRight = isRight;
            this.isFling = isfling;
            this.moveSpeed = SPEED_LOW;
        }

        public boolean isMove() {
            return this.isMove;
        }

        public void setMove(boolean isMove) {
            this.isMove = isMove;
        }

        public int getActionType() {
            return this.actionType;
        }

        public boolean isMovingRight() {
            return this.isMovingRight;
        }

        public void run() {
            super.run();
            log("1 start:" + mStartX + ", now:" + mNowX);

            mItemX = mNowX - mStartX;
            log("2 mItemX:" + mItemX);
            if (isFling) {
                this.moveSpeed = this.SPEED_HIGH;
            } else if (actionType == ACTION_TYPE_RESUME) {
                this.moveSpeed = this.SPEED_LOW;
            } else {
                this.moveSpeed = this.SPEED_MIDDING;
            }

            while (isMove()) {
                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (actionType == ACTION_TYPE_RESUME) {
                    if ((mItemX >= 0 && isMovingRight) || (mItemX <= 0 && !isMovingRight)) {
                        setMove(false);
                        log("1action type:" + getActionType() + ", isMovingRight :"
                                + isMovingRight);
                        /*Begin: Modified by bxinchun, when action type is resume, don't trigger 2012/08/03*/
                        /*if (mTriggerListener != null)
                            mTriggerListener.onTrigger(dragSrcPosition, actionType);*/
                        /*End: Modified by bxinchun, when action type is resume, don't trigger 2012/08/03*/
                        break;
                    }
                } else {
                    if ((mItemX <= -480 && !isMovingRight) || (mItemX >= 480 && isMovingRight)) {
                        setMove(false);
                        log("2action type :" + getActionType());
                        if (mTriggerListener != null)
                            mTriggerListener.onTrigger(dragSrcPosition, actionType);
                        break;
                    }
                }

                if (isMovingRight) {
                    mItemX += this.moveSpeed;
                } else {
                    mItemX -= this.moveSpeed;
                }
                // log("3 mItemX:"+mItemX);
                postInvalidate();

            }
        }
    }

    /**
     * 绑定滑动监听器到 ListView
     *
     * @param tl 滑动监听器
     */
    public void setTriggerListener(TriggerListener tl) {
        this.mTriggerListener = tl;
    }

    /**
     * 滑动操作的监听器
     *
     * @author xpengfei
     */
    public static abstract class TriggerListener {
        public static final int NONE = 0;
        public static final int LEFT = 2;
        public static final int RIGHT = 1;

        /**
         * @param actionType 滑动操作的方向, 左, 右, 无
         * @param Position 该条目在List中的位置
         */
        public abstract void onTrigger(int position, int actionType);

        /**
         *  add by bxinchun 2012-07-31
         * @param position
         * @return
         */
        public boolean isTriggable(int position) {
            if (position <= AdapterView.INVALID_POSITION) {
                return false;
            }
            return true;
        }
    }
}
/*End: Modified by xiepengfei for add touch listview 2012/06/07*/
