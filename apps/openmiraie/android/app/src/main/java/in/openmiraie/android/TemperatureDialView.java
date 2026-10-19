package in.openmiraie.android;

import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.*;
import java.util.function.DoubleConsumer;

/** Native adjustable range: relative drag, end stops, no inertia or reconnect queue. */
// Constructed in code with validated capabilities; it is never inflated from XML.
@android.annotation.SuppressLint("ViewConstructor")
final class TemperatureDialView extends View {
    private static final Typeface FONT=Typeface.create("sans-serif",Typeface.NORMAL);
    private final RectF bounds=new RectF();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TemperatureSelection selection;
    private final DoubleConsumer commit;
    private final String pending;
    private final double reported;
    private boolean dragging, editing;
    private int pointer=-1;
    private float cx,cy,radius;
    private final Runnable apply=()->finish();

    TemperatureDialView(Context context,double[] range,double observed,Double requested,
                        boolean enabled,DoubleConsumer commit) {
        super(context); selection=new TemperatureSelection(range[0],range[1],range[2],observed);
        this.commit=commit; reported=observed;
        pending=requested==null?null:MainActivity.number(requested);
        if(requested!=null)selection.adjust(requested);
        setEnabled(enabled); setFocusable(true); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription(requested==null?"Target temperature":"Applying "+MainActivity.number(requested)+" degrees Celsius. AC reports "+MainActivity.number(observed));
        if(requested!=null)setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
    }
    private float dp(float value){return value*getResources().getDisplayMetrics().density;}
    private int color(int id){return getContext().getColor(id);}
    @Override protected void onMeasure(int width,int height){
        int w=resolveSize((int)dp(340),width);
        setMeasuredDimension(w,resolveSize(Math.min(w,(int)dp(340)),height));
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);cx=getWidth()/2f;cy=getHeight()/2f;
        radius=Math.min(cx,cy)-dp(24);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(3));paint.setStrokeCap(Paint.Cap.ROUND);
        bounds.set(cx-radius,cy-radius,cx+radius,cy+radius);
        paint.setColor(color(R.color.line));canvas.drawArc(bounds,135,270,false,paint);
        paint.setColor(color(isEnabled()||pending!=null?R.color.primary:R.color.muted));
        canvas.drawArc(bounds,135,(float)(270*selection.fraction()),false,paint);
        for(int i=0;i<=14;i++){
            double a=Math.toRadians(135+i*270d/14);paint.setStrokeWidth(dp(1));paint.setColor(color(R.color.line));
            canvas.drawLine(cx+(float)Math.cos(a)*(radius-dp(12)),cy+(float)Math.sin(a)*(radius-dp(12)),cx+(float)Math.cos(a)*(radius-dp(15)),cy+(float)Math.sin(a)*(radius-dp(15)),paint);
        }
        double angle=Math.toRadians(135+270*selection.fraction());
        float x=cx+(float)Math.cos(angle)*radius,y=cy+(float)Math.sin(angle)*radius;
        paint.setStyle(Paint.Style.FILL);paint.setColor(color(R.color.violet));canvas.drawCircle(x,y,dp(16),paint);
        paint.setColor(color(isEnabled()||pending!=null?R.color.primary:R.color.muted));canvas.drawCircle(x,y,dp(7),paint);
        if(pending!=null)drawText(canvas,"Applying",cx,cy-radius*.48f,13,R.color.muted);
        drawValue(canvas);
        if(pending!=null)drawText(canvas,"AC reports "+MainActivity.number(reported)+"°",cx,cy+radius*.55f,12,R.color.muted);
        drawText(canvas,MainActivity.number(selection.min),cx-radius*.71f,cy+radius*.96f,12,R.color.muted);
        drawText(canvas,MainActivity.number(selection.max),cx+radius*.71f,cy+radius*.96f,12,R.color.muted);
        if(isFocused()){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setColor(color(R.color.primary));canvas.drawRoundRect(2,2,getWidth()-2,getHeight()-2,dp(24),dp(24),paint);}
    }
    private void drawValue(Canvas canvas){
        paint.setStyle(Paint.Style.FILL);paint.setColor(color(R.color.ink));paint.setTypeface(FONT);paint.setTextAlign(Paint.Align.LEFT);
        String value=MainActivity.number(selection.value());
        float size=Math.min(96*getResources().getDisplayMetrics().scaledDensity,radius*.8f);
        paint.setTextSize(size);float width=paint.measureText(value);paint.setTextSize(size*.36f);float unitWidth=paint.measureText("°");
        float x=cx-(width+unitWidth+dp(3))/2,baseline=cy+radius*.22f;
        paint.setTextSize(size);canvas.drawText(value,x,baseline,paint);
        paint.setTextSize(size*.36f);canvas.drawText("°",x+width+dp(3),baseline-size*.48f,paint);
    }
    private void drawText(Canvas canvas,String text,float x,float y,float sp,int color){
        paint.setStyle(Paint.Style.FILL);paint.setColor(color(color));paint.setTypeface(FONT);
        paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(sp*getResources().getDisplayMetrics().scaledDensity);
        // Fit the ring interior while preserving the larger layout's accessible system text.
        float limit=sp==96?radius*1.45f:radius*1.8f;
        if(paint.measureText(text)>limit)paint.setTextSize(paint.getTextSize()*limit/paint.measureText(text));
        if(sp==96)paint.setTextSize(Math.min(paint.getTextSize(),radius*.8f));
        canvas.drawText(text,x,y,paint);
    }
    private double angle(float x,float y){return Math.toDegrees(Math.atan2(y-cy,x-cx));}
    @Override public boolean onTouchEvent(android.view.MotionEvent event){
        if(!isEnabled())return false;
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                // Only the visible ring begins a drag; the center remains available for scrolling.
                if(Math.abs(Math.hypot(event.getX()-cx,event.getY()-cy)-radius)>dp(28))return false;
                removeCallbacks(apply);selection.cancel();requestFocus();dragging=editing=true;pointer=event.getPointerId(0);
                selection.begin(angle(event.getX(),event.getY()));getParent().requestDisallowInterceptTouchEvent(true);invalidate();return true;
            case MotionEvent.ACTION_MOVE:
                int index=event.findPointerIndex(pointer);if(!dragging||index<0){cancelInteraction();return true;}
                double previous=selection.value();selection.drag(angle(event.getX(index),event.getY(index)));
                if(previous!=selection.value())performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                invalidate();return true;
            case MotionEvent.ACTION_UP:
                if(dragging){dragging=false;getParent().requestDisallowInterceptTouchEvent(false);performClick();finish();}return true;
            case MotionEvent.ACTION_CANCEL:case MotionEvent.ACTION_POINTER_DOWN:case MotionEvent.ACTION_POINTER_UP:
                cancelInteraction();return true;
            default:return dragging;
        }
    }
    @Override public boolean performClick(){super.performClick();return true;}
    private void finish(){
        removeCallbacks(apply);editing=false;Double value=selection.finish();invalidate();
        if(value!=null&&isEnabled()&&isAttachedToWindow()&&hasWindowFocus())commit.accept(value);
    }
    void cancelInteraction(){removeCallbacks(apply);dragging=editing=false;pointer=-1;if(pending==null)selection.cancel();if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);invalidate();}
    private boolean adjust(double value){
        if(!isEnabled()||!Double.isFinite(value))return false;
        selection.adjust(value);editing=true;invalidate();removeCallbacks(apply);postDelayed(apply,450);
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);return true;
    }
    @Override public boolean onKeyDown(int key,android.view.KeyEvent event){
        if(key==KeyEvent.KEYCODE_DPAD_RIGHT||key==KeyEvent.KEYCODE_DPAD_UP)return adjust(selection.value()+selection.step);
        if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_DOWN)return adjust(selection.value()-selection.step);
        return super.onKeyDown(key,event);
    }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){
        super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.SeekBar");
        info.setContentDescription("Target temperature, "+MainActivity.number(selection.value())+" degrees Celsius"+(pending==null?"":", applying; AC reports "+MainActivity.number(reported)));
        info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,(float)selection.min,(float)selection.max,(float)selection.value()));
        if(isEnabled()){info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);if(selection.value()<selection.max)info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);if(selection.value()>selection.min)info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);}
    }
    @Override public boolean performAccessibilityAction(int action,Bundle args){
        if(action==AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS)cancelInteraction();
        if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)return adjust(selection.value()+selection.step);
        if(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)return adjust(selection.value()-selection.step);
        if(action==AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId()&&args!=null&&args.containsKey(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE))return adjust(args.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE));
        return super.performAccessibilityAction(action,args);
    }
    @Override protected void onFocusChanged(boolean gain,int direction,Rect previous){super.onFocusChanged(gain,direction,previous);if(!gain)cancelInteraction();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(!focused)cancelInteraction();}
    @Override protected void onDetachedFromWindow(){cancelInteraction();super.onDetachedFromWindow();}
}
