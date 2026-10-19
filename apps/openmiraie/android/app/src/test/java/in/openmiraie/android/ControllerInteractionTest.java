package in.openmiraie.android;
import org.junit.Test;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;
public class ControllerInteractionTest {
    @Test public void releaseConsumesOnce(){DiscreteSelection s=new DiscreteSelection(9,0);s.select(5);assertEquals(5,s.current());assertEquals(Integer.valueOf(5),s.consume());assertNull(s.consume());}
    @Test public void interruptedDraftDoesNotSend(){DiscreteSelection s=new DiscreteSelection(4,1);s.select(3);s.cancel();assertEquals(1,s.current());assertNull(s.consume());}
    @Test public void unchangedDoesNotSend(){DiscreteSelection s=new DiscreteSelection(4,1);s.select(1);assertNull(s.consume());}
    @Test public void boundsClamp(){DiscreteSelection s=new DiscreteSelection(4,1);s.select(20);assertEquals(3,s.current());s.select(-5);assertEquals(0,s.current());}
    @Test public void manualChoiceFromAutoAppliesFirstStop(){DiscreteSelection s=new DiscreteSelection(4,-1);s.select(0);assertEquals(Integer.valueOf(0),s.consume());}
    @Test public void swingAxisCannotBeStoppedByDrag()throws Exception{var steps=AirflowPlan.plan(SwingTest.profile(),SwingTest.state().put("acvs",0),5,4,0,1);assertEquals(1,steps.size());assertEquals(Swing.HORIZONTAL,steps.get(0).getKey());assertEquals(4,steps.get(0).getValue());}
    @Test public void horizontalSwingAllowsVerticalOnly()throws Exception{var steps=AirflowPlan.plan(SwingTest.profile(),SwingTest.state().put("achs",0),5,4,1,0);assertEquals(1,steps.size());assertEquals(Swing.VERTICAL,steps.get(0).getKey());}
    @Test public void bothSwingingHaveNoManualSteps()throws Exception{assertTrue(AirflowPlan.plan(SwingTest.profile(),SwingTest.state().put("acvs",0).put("achs",0),5,5,0,0).isEmpty());}
    @Test public void fixedPairIsOrderedAndIndependent()throws Exception{var steps=AirflowPlan.plan(SwingTest.profile(),SwingTest.state(),5,4,1,1);assertEquals(2,steps.size());assertEquals(Swing.VERTICAL,steps.get(0).getKey());assertEquals(Swing.HORIZONTAL,steps.get(1).getKey());}
    @Test public void unchangedAxisNotSent()throws Exception{var steps=AirflowPlan.plan(SwingTest.profile(),SwingTest.state(),1,4,1,1);assertEquals(1,steps.size());assertEquals(Swing.HORIZONTAL,steps.get(0).getKey());}
    @Test public void staleGestureRejectedBeforeSending()throws Exception{try{AirflowPlan.plan(SwingTest.profile(),SwingTest.state().put("acvs",2),5,4,1,1);fail();}catch(IllegalArgumentException expected){}}
    @Test public void manualGestureCannotEnableSwing()throws Exception{try{AirflowPlan.plan(SwingTest.profile(),SwingTest.state(),0,1,1,1);fail();}catch(IllegalArgumentException expected){}}
    @Test public void validatesBothTargetsBeforeFirstStep()throws Exception{try{AirflowPlan.plan(SwingTest.profile(),SwingTest.state(),5,6,1,1);fail();}catch(IllegalArgumentException expected){}}
    @Test public void offlineGestureRejected()throws Exception{try{AirflowPlan.plan(SwingTest.profile(),SwingTest.state().put("onlineStatus",false),5,4,1,1);fail();}catch(IllegalArgumentException expected){}}
}
