package in.openmiraie.android;

import org.junit.Test;
import static org.junit.Assert.*;

public class TemperatureSelectionTest {
    TemperatureSelection dial(){return new TemperatureSelection(16,30,1,24);}
    @Test public void touchingAnotherAngleDoesNotJump(){TemperatureSelection d=dial();d.begin(-170);assertEquals(24,d.value(),0);assertNull(d.finish());}
    @Test public void dragCommitsOnceOnlyOnRelease(){TemperatureSelection d=dial();d.begin(0);d.drag(45);assertEquals(26,d.value(),0);assertEquals(26,d.finish(),0);assertNull(d.finish());}
    @Test public void cancelledGestureNeverCommits(){TemperatureSelection d=dial();d.begin(0);d.drag(90);d.cancel();assertEquals(24,d.value(),0);assertNull(d.finish());}
    @Test public void crossingAngleBoundaryIsContinuous(){TemperatureSelection d=dial();d.begin(179);d.drag(-170);assertEquals(25,d.value(),0);}
    @Test public void maxAndMinAreStopsWithoutWrap(){TemperatureSelection d=dial();d.begin(0);d.drag(150);d.drag(-60);assertEquals(30,d.value(),0);d.drag(-100);assertEquals(28,d.value(),0);d.drag(100);d.drag(-60);assertEquals(16,d.value(),0);}
    @Test public void halfDegreeModelUsesItsOwnIncrement(){TemperatureSelection d=new TemperatureSelection(18,28,.5,22);d.adjust(23.3);assertEquals(23.5,d.finish(),0);}
    @Test public void unreachableUpperBoundStaysOnStep(){TemperatureSelection d=new TemperatureSelection(16,30,.6,22);d.adjust(30);assertEquals(29.8,d.finish(),1e-7);}
    @Test public void repeatedAccessibleChangesCoalesceToFinalValue(){TemperatureSelection d=dial();d.adjust(25);d.adjust(26);d.adjust(27);assertEquals(27,d.finish(),0);assertNull(d.finish());}
    @Test public void returningToReportedValueDoesNotWrite(){TemperatureSelection d=dial();d.adjust(25);d.adjust(24);assertNull(d.finish());}
    @Test public void cancellingAccessibleEditDoesNotReplay(){TemperatureSelection d=dial();d.adjust(25);d.cancel();assertNull(d.finish());d.begin(30);assertNull(d.finish());}
    @Test public void invalidAccessibilityValueDoesNotCreateCommand(){TemperatureSelection d=dial();d.adjust(Double.NaN);assertNull(d.finish());}
    @Test(expected=IllegalArgumentException.class) public void invalidRangeFailsClosed(){new TemperatureSelection(30,16,1,24);}
}
