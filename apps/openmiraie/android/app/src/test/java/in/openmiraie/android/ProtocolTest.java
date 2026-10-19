package in.openmiraie.android;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.util.*;

public class ProtocolTest {
    static Profile profile()throws Exception {
        JSONObject model=new JSONObject();JSONArray caps=new JSONArray();
        String[][] mappings={{"PowerStatus","ps"},{"DisplayControl","acdc"},{"ACMode","acmd"},{"ACTemperature","actmp"},{"ACFanSpeed","acfs"},{"ECOMode","acem"},{"PowerfulMode","acpm"},{"Convertible","cnv"},{"EvaporatorClean","acec"}};
        for(String[] map:mappings){JSONObject attr=new JSONObject().put("key",map[1]);
            if(map[0].equals("ACMode"))attr.put("values",new JSONArray("['auto','cool','dry','fan']"));
            if(map[0].equals("ACFanSpeed"))attr.put("values",new JSONArray("['auto','quiet','low','medium','high']"));
            if(map[0].equals("Convertible"))attr.put("intValues",new JSONArray("[0,40,50,60,70,80,90,100,110]"));
            if(map[0].equals("ACTemperature"))attr.put("range",new JSONObject("{min:[{unit:'fahrenheit',value:61},{unit:'celcius',value:16}],max:[{unit:'celcius',value:30}],increment:[{unit:'celcius',value:1}]}"));
            caps.put(new JSONObject().put("capability",map[0]).put("attributes",new JSONArray().put(attr)));}
        return new Profile(model.put("controlCapabilities",caps));
    }
    static JSONObject state()throws Exception{return new JSONObject("{ps:'on',acdc:'off',acmd:'cool',actmp:'23.0',acfs:'auto',acem:'off',acpm:'off',cnv:0,acec:'off',onlineStatus:true,rmtmp:22.61}");}
    @Test public void powerAndDisplayHaveIndependentKeysAndIndexes()throws Exception {Profile p=profile();assertEquals("ps",p.command("PowerStatus","off",state()).key());assertEquals(1,p.command("PowerStatus","off",state()).index());assertEquals("acdc",p.command("DisplayControl","on",state()).key());assertEquals(8,p.command("DisplayControl","on",state()).index());}
    @Test public void modelKeyOverridesShortKeyAssumptions()throws Exception {Profile p=profile();p.attribute("PowerStatus").put("key","power_custom");JSONObject s=state().put("power_custom","on");assertEquals("power_custom",p.command("PowerStatus","off",s).key());}
    @Test public void absentCapabilitiesNeverInventControls(){Profile p=new Profile(null);assertFalse(p.supports("PowerStatus"));assertNull(p.range("cool"));assertTrue(p.choices("ACMode").isEmpty());}
    @Test public void statusOnlyCapabilityIsNotAControl()throws Exception {Profile p=new Profile(new JSONObject("{statusCapabilitiesV2:[{capability:'PowerStatus',attributes:[{key:'ps'}]}]}"));assertFalse(p.supports("PowerStatus"));}
    @Test public void celsiusChosenInsteadOfFirstUnit()throws Exception{assertArrayEquals(new double[]{16,30,1},profile().range("cool"),0.0001);}
    @Test public void noHeatOrCapacity55()throws Exception{assertFalse(profile().choices("ACMode").contains("heat"));assertFalse(profile().choices("Convertible").contains("55"));}
    @Test public void fractionalRoomReadingPreserved()throws Exception{assertEquals("22.61°C",Data.room(state()));}
    @Test public void missingRoomIsNotZero()throws Exception{assertEquals("Not reported",Data.room(new JSONObject()));assertEquals("Not reported",Data.room(state().put("rmtmp",-1)));assertEquals("0°C",Data.room(state().put("rmtmp",0)));}
    @Test public void missingAppliedModeBlocksDependentSettings()throws Exception{JSONObject s=state();s.remove("acmd");assertFalse(profile().blocked(s,"ACTemperature").isEmpty());assertFalse(profile().blocked(s,"ACFanSpeed").isEmpty());}
    @Test public void dryDisablesManualFan()throws Exception{assertFalse(profile().blocked(state().put("acmd","dry"),"ACFanSpeed").isEmpty());}
    @Test public void dryPlanDoesNotForceLow()throws Exception{List<Map.Entry<String,Object>> steps=CommandRunner.plan(profile(),state(),"ACMode","dry");assertEquals(1,steps.size());assertEquals("ACMode",steps.get(0).getKey());}
    @Test public void fanOnlyDisablesTarget()throws Exception{assertFalse(profile().blocked(state().put("acmd","fan"),"ACTemperature").isEmpty());}
    @Test public void cleaningDisablesTargetFanAndPowerful()throws Exception{JSONObject s=state().put("acec","on");for(String cap:Arrays.asList("ACTemperature","ACFanSpeed","PowerfulMode"))assertFalse(profile().blocked(s,cap).isEmpty());}
    @Test public void cleaningDoesNotInventACommand()throws Exception{assertFalse(profile().supports("EvaporatorClean"));}
    @Test public void powerOffStillAllowsDisplay()throws Exception{JSONObject s=state().put("ps","off");assertTrue(profile().blocked(s,"DisplayControl").isEmpty());assertFalse(profile().blocked(s,"ACMode").isEmpty());}
    @Test public void missingDisplayDisablesToggle()throws Exception{JSONObject s=state();s.remove("acdc");assertFalse(profile().blocked(s,"DisplayControl").isEmpty());}
    @Test public void offlineOrUnknownOnlineBlocksWrites()throws Exception{Profile p=profile();JSONObject s=state().put("onlineStatus",false);assertFalse(p.blocked(s,"PowerStatus").isEmpty());s.remove("onlineStatus");assertFalse(p.blocked(s,"PowerStatus").isEmpty());}
    @Test public void ecoPlanIncludes26AfterEnable()throws Exception{List<Map.Entry<String,Object>> steps=CommandRunner.plan(profile(),state(),"ECOMode","on");assertEquals("ECOMode",steps.get(0).getKey());assertEquals("ACTemperature",steps.get(1).getKey());assertEquals("26",steps.get(1).getValue());}
    @Test public void disablingEcoDoesNotRestoreTarget()throws Exception{assertEquals(1,CommandRunner.plan(profile(),state().put("acem","on"),"ECOMode","off").size());}
    @Test public void powerfulResetsCapacityBeforeEnabling()throws Exception{List<Map.Entry<String,Object>> steps=CommandRunner.plan(profile(),state().put("cnv",80),"PowerfulMode","on");assertEquals("Convertible",steps.get(0).getKey());assertEquals(0,steps.get(0).getValue());assertEquals("PowerfulMode",steps.get(1).getKey());}
    @Test public void capacityDisablesPowerfulFirst()throws Exception{assertEquals("PowerfulMode",CommandRunner.plan(profile(),state().put("acpm","on"),"Convertible",80).get(0).getKey());}
    @Test(expected=IllegalArgumentException.class) public void ecoOutsideCoolRejected()throws Exception{CommandRunner.plan(profile(),state().put("acmd","dry"),"ECOMode","on");}
    @Test(expected=IllegalArgumentException.class) public void targetOutsideRangeRejected()throws Exception{profile().command("ACTemperature",31,state());}
    @Test(expected=IllegalArgumentException.class) public void targetOffStepRejected()throws Exception{profile().command("ACTemperature",23.5,state());}
    @Test(expected=IllegalArgumentException.class) public void unknownCapacityRejected()throws Exception{profile().command("Convertible",55,state());}
    static final class Fake implements CommandRunner.Port {
        JSONObject reported;int sent=0,reads=0,saved=0;boolean match=true,failSend=false,failRead=false;int matchThrough=99;
        Fake()throws Exception{reported=state();}
        public void send(Profile.Command c)throws Exception{sent++;if(failSend)throw new java.io.IOException();if(match&&sent<=matchThrough)reported.put(c.key(),c.value());}
        public JSONObject read()throws Exception{reads++;if(failRead)throw new java.io.IOException();return new JSONObject(reported.toString());}
        public void observed(JSONObject s){saved++;}
    }
    CommandRunner runner(Fake fake){return new CommandRunner(fake,ms->{},message->{});}
    @Test public void mutationSentOnceWhenUnconfirmed()throws Exception{Fake f=new Fake();f.match=false;CommandRunner.Result result=runner(f).execute(profile(),state(),"PowerStatus","off");assertFalse(result.confirmed());assertEquals(1,f.sent);assertEquals(5,f.reads);assertEquals("on",result.observed().getString("ps"));}
    @Test public void failedSendNeverReplayed()throws Exception{Fake f=new Fake();f.failSend=true;assertFalse(runner(f).execute(profile(),state(),"PowerStatus","off").confirmed());assertEquals(1,f.sent);assertEquals(0,f.reads);}
    @Test public void failedReadbackStopsDependentCommand()throws Exception{Fake f=new Fake();f.failRead=true;CommandRunner.Result r=runner(f).execute(profile(),state(),"ECOMode","on");assertFalse(r.confirmed());assertEquals(1,f.sent);}
    @Test public void unconfirmedFirstStepStopsSequence()throws Exception{Fake f=new Fake();f.match=false;CommandRunner.Result r=runner(f).execute(profile(),state(),"ECOMode","on");assertEquals(0,r.completed());assertEquals(1,f.sent);}
    @Test public void partialSequenceExposesConfirmedStep()throws Exception{Fake f=new Fake();f.matchThrough=1;CommandRunner.Result r=runner(f).execute(profile(),state(),"ECOMode","on");assertFalse(r.confirmed());assertEquals(1,r.completed());assertEquals("on",r.observed().getString("acem"));assertEquals("23.0",r.observed().getString("actmp"));}
    @Test public void eachReadbackIsRecorded()throws Exception{Fake f=new Fake();CommandRunner.Result r=runner(f).execute(profile(),state(),"ECOMode","on");assertTrue(r.confirmed());assertEquals(2,f.saved);assertEquals(2,f.sent);}
    @Test public void matchingHandlesNumericStrings(){assertTrue(Data.same("23.0",23));assertTrue(Data.same("ON","on"));assertFalse(Data.same("",0));}
    @Test public void csvQuotesAndNeutralizesTextFormula(){assertEquals("\"'=HYPERLINK(\"\"x\"\")\"",Data.csv("=HYPERLINK(\"x\")"));assertEquals("\"-23.5\"",Data.csv("-23.5"));assertEquals("\"'  +cmd\"",Data.csv("  +cmd"));}
    @Test public void energyKeepsDecimalPrecisionAndDates()throws Exception{String csv=Data.energyCsv(new JSONArray("[{day:'19102026',power:'1.2300'}]"));assertTrue(csv.contains("2026-10-19"));assertTrue(csv.contains("1.2300"));}
    @Test public void discoveryRetainsHierarchyAndUnsupportedDevices()throws Exception{List<JSONObject> d=Data.discovery(new JSONArray("[{homeId:'synthetic-home',homeName:'Sample',spaces:[{spaceName:'Room',devices:[{deviceId:'synthetic-ac',category:'AC',modelId:'140013'},{deviceId:'synthetic-other',category:'LIGHT'}]}]}]"));assertEquals(2,d.size());assertEquals("Room",d.get(0).getString("spaceName"));assertTrue(Data.supportedDevice(d.get(0)));assertFalse(Data.supportedDevice(d.get(1)));}
    @Test public void partnerAcIsNotAssumedRestCompatible()throws Exception{assertFalse(Data.supportedDevice(new JSONObject("{category:'AC',thirdPartyService:'example'}")));assertFalse(Data.supportedDevice(new JSONObject("{category:'ODMIRB-AC'}")));}
    @Test public void diagnosticsRemoveNestedSecrets()throws Exception{JSONObject clean=(JSONObject)Data.redact(new JSONObject("{deviceId:'synthetic',rmtmp:22.61,nested:{udpKey:'example',refreshToken:'example',ssid:'example',ps:'on'}}"));assertFalse(clean.has("deviceId"));assertEquals("on",clean.getJSONObject("nested").getString("ps"));assertEquals(1,clean.getJSONObject("nested").length());}
}
