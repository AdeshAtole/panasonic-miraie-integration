package in.openmiraie.android;
import android.content.Context;
import android.graphics.Rect;
import android.view.*;
import android.widget.SeekBar;
import java.util.*;
/** Standard slider with release/coalesced keyboard commit; interruption cancels. */
@android.annotation.SuppressLint("ViewConstructor") // Programmatic native slider only.
final class ReleaseSeekBar extends SeekBar {
    interface Commit{void selected(String value);}final List<String> values;final DiscreteSelection selection;final Commit commit;boolean touching,setting;final Runnable settle=this::finish;
    ReleaseSeekBar(Context c,List<String> values,String current,Commit commit){super(c);this.values=values;this.commit=commit;selection=new DiscreteSelection(values.size(),values.indexOf(current));setMax(Math.max(0,values.size()-1));setProgress(Math.max(0,selection.current()));setMinimumHeight(Math.round(48*getResources().getDisplayMetrics().density));setContentDescription("Fan speed, "+current);setOnSeekBarChangeListener(new OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){touching=true;selection.select(getProgress());removeCallbacks(settle);}public void onProgressChanged(SeekBar b,int n,boolean user){if(user&&!setting){selection.select(n);setContentDescription("Fan speed, "+values.get(n));if(!touching){removeCallbacks(settle);postDelayed(settle,450);}}}public void onStopTrackingTouch(SeekBar b){touching=false;finish();}});}
    void finish(){removeCallbacks(settle);Integer n=selection.consume();if(n!=null)commit.selected(values.get(n));}
    void cancelInteraction(){removeCallbacks(settle);touching=false;selection.cancel();setting=true;setProgress(Math.max(0,selection.observed));setting=false;}
    @Override public boolean performClick(){return super.performClick();}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_POINTER_DOWN||e.getActionMasked()==MotionEvent.ACTION_CANCEL){cancelInteraction();MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);super.onTouchEvent(cancel);cancel.recycle();return true;}boolean handled=super.onTouchEvent(e);if(e.getActionMasked()==MotionEvent.ACTION_UP)performClick();return handled;}
    @Override protected void onDetachedFromWindow(){cancelInteraction();super.onDetachedFromWindow();}
    @Override protected void onFocusChanged(boolean gain,int direction,Rect old){super.onFocusChanged(gain,direction,old);if(!gain&&selection!=null)cancelInteraction();}
}
