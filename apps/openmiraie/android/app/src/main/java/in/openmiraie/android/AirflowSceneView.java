package in.openmiraie.android;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.*;

/** Illustrative airflow, not telemetry. Model capability validation lives in Swing. */
@android.annotation.SuppressLint("ViewConstructor") // Created with model state; never inflated from XML.
final class AirflowSceneView extends View {
    interface Commit{void selected(Integer vertical,Integer horizontal);}
    static final int LEFT=0x01040001,RIGHT=0x01040002,UP=0x01040003,DOWN=0x01040004;
    Integer pendingV,pendingH;final DashPathEffect pendingDash=new DashPathEffect(new float[]{4,5},0);
    final Integer vertical,horizontal;final List<Integer> vs,hs;final boolean vAllowed,hAllowed;final Commit commit;
    final RectF photoBounds=new RectF();
    final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final Path path=new Path();final Bitmap photo;
    final int[] hOrder={3,2,1,4,5};Integer dv,dh;boolean dragging;int pointer=-1;float startX,startY;final Runnable settle=this::finish;
    AirflowSceneView(Context c,Integer v,Integer h,List<Integer> vs,List<Integer> hs,boolean vAllowed,boolean hAllowed,Commit commit){super(c);vertical=v;horizontal=h;this.vs=vs;this.hs=hs;this.vAllowed=vAllowed;this.hAllowed=hAllowed;this.commit=commit;photo=BitmapFactory.decodeResource(getResources(),R.drawable.ac_scene);setFocusable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);describe();}
    float d(float x){return x*getResources().getDisplayMetrics().density;}int color(int id){return getContext().getColor(id);}boolean canV(){return vAllowed&&vertical!=null&&vertical!=0&&!vs.isEmpty();}boolean canH(){return hAllowed&&horizontal!=null&&horizontal!=0&&!hs.isEmpty();}
    @Override protected void onMeasure(int w,int h){setMeasuredDimension(MeasureSpec.getSize(w),Math.round(d(300)));}
    float xFor(int code){for(int i=0;i<hOrder.length;i++)if(hOrder[i]==code)return 48+i*51;return 150;}
    float yFor(int code){return 130+(code-1)*31;}
    boolean motion(){return android.animation.ValueAnimator.areAnimatorsEnabled()&&hasWindowFocus();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float sx=getWidth()/300f,sy=getHeight()/300f;c.save();c.scale(sx,sy);float t=SystemClock.uptimeMillis();float x=horizontal==null?150:horizontal==0?(motion()?150+102*(float)Math.sin(t/2200):150):xFor(horizontal),y=vertical==null?192:vertical==0?(motion()?192+62*(float)Math.sin(t/1750):192):yFor(vertical);if(dh!=null&&canH())x=xFor(dh);if(dv!=null&&canV())y=yFor(dv);
        if(vertical!=null||horizontal!=null){paint.setColor(color(R.color.violet));paint.setStyle(Paint.Style.FILL);path.reset();path.moveTo(106,80);path.quadTo(106+(x-150)*.34f,y*.7f,x-44,y);path.lineTo(x+44,y);path.quadTo(194+(x-150)*.34f,y*.7f,194,80);path.close();c.drawPath(path,paint);paint.setColor(color(R.color.primary));paint.setAlpha(95);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.2f);paint.setStrokeCap(Paint.Cap.ROUND);float phase=motion()?(t%1800)/1800f:0;for(int i=0;i<5;i++){float start=114+i*18,control=start+(x-150)*.34f,end=x+(i-2)*18;for(int j=0;j<13;j++){float q=(j/13f+phase)%1,u=1-q;float px=u*u*start+2*u*q*control+q*q*end,py=u*u*83+2*u*q*y*.7f+q*q*y;c.drawCircle(px,py,.8f,paint);}}paint.setPathEffect(null);paint.setAlpha(255);paint.setStrokeWidth(3);if(canV()||canH())c.drawLine(x-44,y,x+44,y,paint);paint.setStyle(Paint.Style.FILL);}
        if(pendingV!=null||pendingH!=null){float gx=pendingH!=null&&horizontal!=null&&horizontal!=0?xFor(pendingH):x,gy=pendingV!=null&&vertical!=null&&vertical!=0?yFor(pendingV):y;paint.setColor(color(R.color.primary));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.2f);paint.setPathEffect(pendingDash);path.reset();path.moveTo(106,80);path.quadTo(106+(gx-150)*.34f,gy*.7f,gx-44,gy);path.lineTo(gx+44,gy);path.quadTo(194+(gx-150)*.34f,gy*.7f,194,80);c.drawPath(path,paint);paint.setPathEffect(null);paint.setStyle(Paint.Style.FILL);}
        if(photo!=null){paint.setColor(Color.WHITE);paint.setAlpha(255);float scale=Math.min(270f/photo.getWidth(),90f/photo.getHeight()),w=photo.getWidth()*scale,h=photo.getHeight()*scale;photoBounds.set(150-w/2,8,150+w/2,8+h);c.drawBitmap(photo,null,photoBounds,paint);}c.restore();
        if(motion()&&isShown()&&(vAllowed||hAllowed||pendingV!=null||pendingH!=null))postInvalidateDelayed(32);
    }
    void describe(){setContentDescription("Illustrative airflow. Vertical "+axis(vertical)+", horizontal "+axis(horizontal)+". Horizontal preset spacing is uncalibrated. "+(canV()||canH()?"Drag the stream or use direction actions.":"Use position options for supported presets."));}
    String axis(Integer n){return n==null?"unknown":n==0?"swinging":"position "+n;}
    int nearest(float value,boolean verticalAxis){List<Integer> choices=verticalAxis?vs:hs;float best=Float.MAX_VALUE;int result=verticalAxis?vertical:horizontal;for(int code:choices)if(code!=0){float delta=Math.abs(value-(verticalAxis?yFor(code):xFor(code)));if(delta<best){best=delta;result=code;}}return result;}
    void preview(float x,float y){Integer oldV=dv,oldH=dh;if(canV())dv=nearest(y/getHeight()*300,true);if(canH())dh=nearest(x/getWidth()*300,false);if(!Objects.equals(oldV,dv)||!Objects.equals(oldH,dh))performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);invalidate();}
    @Override public boolean onTouchEvent(MotionEvent e){if(!canV()&&!canH())return false;switch(e.getActionMasked()){
        case MotionEvent.ACTION_DOWN:if(e.getY()<d(100))return false;removeCallbacks(settle);pointer=e.getPointerId(0);startX=e.getX();startY=e.getY();dragging=true;getParent().requestDisallowInterceptTouchEvent(true);preview(e.getX(),e.getY());return true;
        case MotionEvent.ACTION_POINTER_DOWN:cancelInteraction();return true;
        case MotionEvent.ACTION_MOVE:if(dragging&&e.getPointerId(0)==pointer)preview(e.getX(),e.getY());return true;
        case MotionEvent.ACTION_UP:if(dragging){preview(e.getX(),e.getY());dragging=false;pointer=-1;finish();performClick();}return true;
        case MotionEvent.ACTION_CANCEL:cancelInteraction();return true;
        default:return true;}}
    @Override public boolean performClick(){super.performClick();return true;}
    void finish(){removeCallbacks(settle);Integer v=dv,h=dh;dv=dh=null;if(v!=null&&!v.equals(vertical)||h!=null&&!h.equals(horizontal))commit.selected(v,h);invalidate();}
    void cancelInteraction(){removeCallbacks(settle);dv=dh=null;dragging=false;pointer=-1;invalidate();}
    boolean adjust(boolean vAxis,int delta){if(vAxis?!canV():!canH())return false;List<Integer> choices=new ArrayList<>();if(vAxis){for(int n:vs)if(n!=0)choices.add(n);}else for(int n:hOrder)if(hs.contains(n))choices.add(n);Integer current=vAxis?(dv==null?vertical:dv):(dh==null?horizontal:dh);int i=choices.indexOf(current);if(i<0)return false;int next=choices.get(Math.max(0,Math.min(choices.size()-1,i+delta)));if(vAxis)dv=next;else dh=next;invalidate();removeCallbacks(settle);postDelayed(settle,450);return true;}
    @Override public boolean onKeyDown(int key,KeyEvent e){switch(key){case KeyEvent.KEYCODE_DPAD_LEFT:return adjust(false,-1);case KeyEvent.KEYCODE_DPAD_RIGHT:return adjust(false,1);case KeyEvent.KEYCODE_DPAD_UP:return adjust(true,-1);case KeyEvent.KEYCODE_DPAD_DOWN:return adjust(true,1);case KeyEvent.KEYCODE_ESCAPE:cancelInteraction();return true;default:return super.onKeyDown(key,e);}}
    @Override protected void onFocusChanged(boolean gain,int direction,Rect r){super.onFocusChanged(gain,direction,r);if(!gain)cancelInteraction();}
    @Override protected void onDetachedFromWindow(){cancelInteraction();super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility!=VISIBLE)cancelInteraction();else invalidate();}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);if(canH()){info.addAction(new AccessibilityNodeInfo.AccessibilityAction(LEFT,"Aim left"));info.addAction(new AccessibilityNodeInfo.AccessibilityAction(RIGHT,"Aim right"));}if(canV()){info.addAction(new AccessibilityNodeInfo.AccessibilityAction(UP,"Aim up"));info.addAction(new AccessibilityNodeInfo.AccessibilityAction(DOWN,"Aim down"));}}
    @Override public boolean performAccessibilityAction(int action,Bundle args){if(action==LEFT)return adjust(false,-1);if(action==RIGHT)return adjust(false,1);if(action==UP)return adjust(true,-1);if(action==DOWN)return adjust(true,1);return super.performAccessibilityAction(action,args);}
}
