package in.openmiraie.android;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ScrollView;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;

/** Restores position after layout and recognizes a downward refresh gesture only at the top. */
final class RefreshScrollView extends ScrollView {
    static final int REFRESH_ACTION=0x01040010;
    private Integer pendingScroll;
    private float downX,downY,distance;
    private boolean eligible,pulling;
    private final int slop;
    private final float threshold;
    BooleanSupplier refreshAllowed=()->false;
    Runnable refresh=()->{};
    DoubleConsumer pullProgress=value->{};

    RefreshScrollView(Context context){super(context);slop=ViewConfiguration.get(context).getScaledTouchSlop();threshold=72*getResources().getDisplayMetrics().density;}
    int savedPosition(){return pendingScroll==null?getScrollY():pendingScroll;}
    void restorePosition(int y){pendingScroll=Math.max(0,y);requestLayout();}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        super.onLayout(changed,l,t,r,b);
        if(pendingScroll!=null){int y=pendingScroll;pendingScroll=null;scrollTo(0,y);}
    }
    @Override public boolean requestChildRectangleOnScreen(View child,Rect rectangle,boolean immediate){return pendingScroll==null&&super.requestChildRectangleOnScreen(child,rectangle,immediate);}
    private void begin(MotionEvent event){cancelPull();downX=event.getX();downY=event.getY();eligible=getScrollY()==0&&refreshAllowed.getAsBoolean();}
    private boolean startPull(MotionEvent event){
        if(!eligible||!refreshAllowed.getAsBoolean()||getScrollY()!=0)return false;
        float dy=event.getY()-downY,dx=Math.abs(event.getX()-downX);
        if(dy < -slop || dx>slop&&dx>Math.abs(dy)){eligible=false;return false;}
        if(dy>slop&&dy>dx){pulling=true;return true;}return false;
    }
    private void track(MotionEvent event){distance=Math.max(0,event.getY()-downY-slop);pullProgress.accept(Math.min(1,distance/threshold));}
    @Override public boolean onInterceptTouchEvent(MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:begin(event);break;
            case MotionEvent.ACTION_MOVE:if(pulling||startPull(event)){track(event);return true;}break;
            case MotionEvent.ACTION_POINTER_DOWN:case MotionEvent.ACTION_CANCEL:cancelPull();break;
        }
        return super.onInterceptTouchEvent(event);
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN)begin(event);
        if(event.getActionMasked()==MotionEvent.ACTION_POINTER_DOWN||event.getActionMasked()==MotionEvent.ACTION_CANCEL){cancelPull();return super.onTouchEvent(event);}
        if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&(pulling||startPull(event))){track(event);return true;}
        if(event.getActionMasked()==MotionEvent.ACTION_UP&&pulling){
            boolean apply=distance>=threshold&&refreshAllowed.getAsBoolean();cancelPull();
            MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);super.onTouchEvent(cancel);cancel.recycle();
            performClick();if(apply)refresh.run();return true;
        }
        return super.onTouchEvent(event);
    }
    @Override public boolean performClick(){return super.performClick();}
    void cancelPull(){eligible=false;pulling=false;distance=0;pullProgress.accept(0);}
    @Override protected void onDetachedFromWindow(){cancelPull();super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility!=VISIBLE)cancelPull();}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);if(refreshAllowed.getAsBoolean())info.addAction(new AccessibilityNodeInfo.AccessibilityAction(REFRESH_ACTION,"Refresh"));}
    @Override public boolean performAccessibilityAction(int action,Bundle args){if(action==REFRESH_ACTION){if(!refreshAllowed.getAsBoolean())return false;refresh.run();return true;}return super.performAccessibilityAction(action,args);}
}
