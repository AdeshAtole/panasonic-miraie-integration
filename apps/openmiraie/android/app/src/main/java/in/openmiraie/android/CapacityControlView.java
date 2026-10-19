package in.openmiraie.android;

import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.*;

/** The whole surface is the slider. Off is a collapsed, accessible disclosure. */
@android.annotation.SuppressLint("ViewConstructor") // Created with capability choices; never inflated from XML.
final class CapacityControlView extends View {
    interface Listener{void commit(String value);void expanded(boolean expanded);}
    final List<String> values;final DiscreteSelection selection;final Listener listener;
    final Path drawPath=new Path();final Typeface medium=Typeface.create("sans-serif-medium",Typeface.NORMAL);
    final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final RectF rect=new RectF();
    String pendingValue="";final DashPathEffect pendingDash=new DashPathEffect(new float[]{4,5},0);boolean expanded,dragging;int pointer=-1;final Runnable keyCommit=this::commit;
    CapacityControlView(Context c,List<String> values,String observed,boolean expanded,boolean enabled,Listener listener){super(c);this.values=new ArrayList<>(values);selection=new DiscreteSelection(values.size(),values.indexOf(observed));this.expanded=expanded||!"0".equals(observed);this.listener=listener;setEnabled(enabled&&selection.observed>=0&&!values.isEmpty());setFocusable(true);setClickable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);describe();}
    float d(float v){return v*getResources().getDisplayMetrics().density;}float sp(float v){return v*getResources().getDisplayMetrics().scaledDensity;}int color(int id){return getContext().getColor(id);}
    boolean collapsed(){return !expanded&&pendingValue.isEmpty()&&"0".equals(value());}String value(){int i=selection.current();return i<0||i>=values.size()?"":values.get(i);}String label(){return value().isEmpty()?"Unknown":"0".equals(value())?"Off":value()+"%";}
    boolean stackedStatus(){return !pendingValue.isEmpty()&&getResources().getConfiguration().fontScale>=1.5f;}
    @Override protected void onMeasure(int w,int h){setMeasuredDimension(MeasureSpec.getSize(w),(int)Math.ceil(collapsed()?Math.max(d(48),sp(17)+d(24)):Math.max(d(80),sp(17)+sp(12)+d(38))+(stackedStatus()?sp(13)+d(10):0)));}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();rect.set(0,0,w,h);paint.setColor(color(R.color.panel));c.drawRoundRect(rect,d(16),d(16),paint);c.save();drawPath.reset();drawPath.addRoundRect(rect,d(16),d(16),Path.Direction.CW);c.clipPath(drawPath);
        int index=selection.current();float progress=index<=0?0:index/(float)Math.max(1,values.size()-1);float end=progress==0?0:progress==1?w:d(16)+(w-d(32))*progress;
        paint.setColor(color(R.color.violet));c.drawRect(0,0,end,h,paint);if(end>0&&end<w){paint.setColor(color(R.color.primary));c.drawRect(end-d(1),0,end+d(1),h,paint);}c.restore();
        paint.setTypeface(medium);paint.setTextSize(sp(15));paint.setColor(color(R.color.ink));float baseline=collapsed()?h/2-(paint.ascent()+paint.descent())/2:d(15)-paint.ascent();c.drawText("Converti8",d(14),baseline,paint);
        paint.setTextSize(sp(13));paint.setColor(color(index>0?R.color.primary:R.color.muted));String label=pendingValue.isEmpty()?label():"Applying…";float right=w-d(collapsed()?32:14);c.drawText(label,stackedStatus()?d(14):right-paint.measureText(label),baseline+(stackedStatus()?sp(13)+d(10):0),paint);
        if(collapsed()){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d(1.5f));drawPath.reset();drawPath.moveTo(w-d(24),h/2-d(2));drawPath.lineTo(w-d(20),h/2+d(2));drawPath.lineTo(w-d(16),h/2-d(2));c.drawPath(drawPath,paint);paint.setStyle(Paint.Style.FILL);}
        else{paint.setTextSize(sp(10));paint.setTypeface(Typeface.DEFAULT);for(int i=0;i<values.size();i++){float x=d(16)+(w-d(32))*i/Math.max(1,values.size()-1);String tick="0".equals(values.get(i))?"Off":values.get(i);paint.setColor(color(i==index?R.color.primary:R.color.muted));if(getResources().getConfiguration().fontScale<1.5f||i==0||i==values.size()-1)c.drawText(tick,x-paint.measureText(tick)/2,h-d(14),paint);else c.drawCircle(x,h-d(16),d(1.5f),paint);}}
        if(!pendingValue.isEmpty()){int requested=values.indexOf(pendingValue);if(requested>=0){float boundary=d(16)+(w-d(32))*requested/Math.max(1,values.size()-1);paint.setColor(color(R.color.primary));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d(1.5f));paint.setPathEffect(pendingDash);c.drawLine(boundary,0,boundary,h,paint);paint.setPathEffect(null);paint.setStyle(Paint.Style.FILL);}}
        if(!isEnabled()&&pendingValue.isEmpty()){paint.setColor(color(R.color.surface)&0x66ffffff);c.drawRoundRect(rect,d(16),d(16),paint);}if(isFocused()){paint.setColor(color(R.color.primary));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d(2));rect.inset(d(2),d(2));c.drawRoundRect(rect,d(14),d(14),paint);paint.setStyle(Paint.Style.FILL);}
    }
    void describe(){setContentDescription("Converti8, "+label()+(collapsed()?". Choose capacity":". Capacity preset, not measured power"));}
    void select(float x){int n=Math.round((x-d(16))/Math.max(1,getWidth()-d(32))*(values.size()-1));int old=selection.current();selection.select(n);if(old!=selection.current())performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);describe();invalidate();}
    @Override public boolean onTouchEvent(android.view.MotionEvent e){if(!isEnabled())return false;if(collapsed())return super.onTouchEvent(e);switch(e.getActionMasked()){
        case MotionEvent.ACTION_DOWN:removeCallbacks(keyCommit);pointer=e.getPointerId(0);dragging=true;getParent().requestDisallowInterceptTouchEvent(true);select(e.getX());return true;
        case MotionEvent.ACTION_POINTER_DOWN:cancelInteraction();return true;
        case MotionEvent.ACTION_MOVE:if(dragging&&e.getPointerId(0)==pointer)select(e.getX());return true;
        case MotionEvent.ACTION_UP:if(dragging){select(e.getX());dragging=false;pointer=-1;getParent().requestDisallowInterceptTouchEvent(true);performClick();commit();}return true;
        case MotionEvent.ACTION_CANCEL:cancelInteraction();return true;
        default:return true;}}
    @Override public boolean performClick(){super.performClick();if(isEnabled()&&collapsed()){expanded=true;listener.expanded(true);requestLayout();describe();invalidate();}return true;}
    void commit(){removeCallbacks(keyCommit);Integer i=selection.consume();if(i!=null)listener.commit(values.get(i));else if("0".equals(value())){expanded=false;listener.expanded(false);requestLayout();}describe();invalidate();}
    void cancelInteraction(){removeCallbacks(keyCommit);selection.cancel();dragging=false;pointer=-1;describe();invalidate();}
    boolean adjust(int delta){if(!isEnabled())return false;expanded=true;selection.select(selection.current()+delta);listener.expanded(true);requestLayout();describe();invalidate();removeCallbacks(keyCommit);postDelayed(keyCommit,450);return true;}
    @Override public boolean onKeyDown(int key,android.view.KeyEvent e){if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_DOWN)return adjust(-1);if(key==KeyEvent.KEYCODE_DPAD_RIGHT||key==KeyEvent.KEYCODE_DPAD_UP)return adjust(1);if(key==KeyEvent.KEYCODE_ESCAPE){cancelInteraction();if("0".equals(value())){expanded=false;listener.expanded(false);requestLayout();}return true;}return super.onKeyDown(key,e);}
    @Override protected void onFocusChanged(boolean gain,int direction,Rect old){super.onFocusChanged(gain,direction,old);if(!gain)cancelInteraction();invalidate();}
    @Override protected void onDetachedFromWindow(){cancelInteraction();super.onDetachedFromWindow();}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName(collapsed()?"android.widget.Button":"android.widget.SeekBar");if(!collapsed()&&selection.observed>=0){info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT,0,values.size()-1,selection.current()));if(isEnabled()){info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);}}}
    @Override public boolean performAccessibilityAction(int action,Bundle args){if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)return adjust(1);if(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)return adjust(-1);if(action==AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId()&&args!=null&&isEnabled()){int target=Math.round(args.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE));return adjust(target-selection.current());}return super.performAccessibilityAction(action,args);}
}
