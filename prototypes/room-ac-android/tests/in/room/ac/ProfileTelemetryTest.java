package in.room.ac;

import org.json.*;
import java.util.*;
import java.io.*;

/** Pure-JVM checks; does not create Android components, access accounts or send commands. */
public final class ProfileTelemetryTest {
 static int passed, failed;
 interface Case { void run() throws Exception; }
 static void test(String name, Case body) {
  try { body.run(); passed++; System.out.println("PASS " + name); }
  catch (Throwable e) { failed++; System.out.println("FAIL " + name + ": " + e); }
 }
 static void equal(Object actual,Object expected) {
  if (!Objects.equals(actual,expected)) throw new AssertionError("expected " + expected + "; got " + actual);
 }
 static void yes(boolean value,String message) { if(!value)throw new AssertionError(message); }
 static DeviceProfile profile(String text) throws Exception { return new DeviceProfile(new JSONObject(text)); }
 static void range(double[] actual,double... expected) { if(!Arrays.equals(actual,expected))throw new AssertionError("expected "+Arrays.toString(expected)+"; got "+Arrays.toString(actual)); }
 public static void main(String[] args) throws Exception {
  final String actualText=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(args[0])),java.nio.charset.StandardCharsets.UTF_8);
  test("actual AC advertises four modes and no heat",()->equal(profile(actualText).modes(),Arrays.asList("auto","cool","dry","fan")));
  test("actual AC fan choices follow metadata",()->equal(profile(actualText).fanSpeeds(),Arrays.asList("auto","quiet","low","medium","high")));
  test("actual AC capacity choices preserve advertised integers only",()->equal(profile(actualText).intValues("Convertible"),Arrays.asList(110,100,90,80,70,60,50,40,0)));
  test("actual AC power and display use separate keys",()-> {
   DeviceProfile p=profile(actualText);equal(p.key("PowerStatus","fallback"),"ps");equal(p.key("DisplayControl","fallback"),"acdc");
  });
  test("actual AC primary temperature capability supplies bounds and key",()-> {
   DeviceProfile p=profile(actualText);equal(p.key("ACTemperature","fallback"),"actmp");range(p.temperatureRange("cool"),16,30,1);
  });
  test("backend celcius spelling is honored rather than falling back",()-> {
   JSONObject model=new JSONObject(actualText);DeviceProfile p=new DeviceProfile(model);
   JSONObject r=p.attribute("ACTemperature").getJSONObject("range");
   r.getJSONArray("min").getJSONObject(0).put("value",18);
   r.getJSONArray("max").getJSONObject(0).put("value",28);
   r.getJSONArray("increment").getJSONObject(0).put("value",0.5);
   range(p.temperatureRange("cool"),18,28,0.5);
  });
  test("only advertised recognized modes are offered",()-> {
   DeviceProfile p=profile("{\"controlCapabilities\":[{\"capability\":\"ACMode\",\"attributes\":[{\"values\":[\"cool\",\"dry\",\"fan\",\"future-mode\"]}]}]}");
   equal(p.modes(),Arrays.asList("cool","dry","fan"));
  });
  test("mode metadata normalizes case",()->equal(profile("{\"controlCapabilities\":[{\"capability\":\"acmode\",\"attributes\":[{\"values\":[\"Cool\",\"HEAT\"]}]}]}").modes(),Arrays.asList("cool","heat")));
  test("duplicate advertised modes produce one option",()->equal(profile("{\"controlCapabilities\":[{\"capability\":\"ACMode\",\"attributes\":[{\"values\":[\"Cool\",\"cool\",\"COOL\"]}]}]}").modes(),Arrays.asList("cool")));
  test("fan choices follow device metadata with exact duplicates removed",()->equal(profile("{\"controlCapabilities\":[{\"capability\":\"ACFanSpeed\",\"attributes\":[{\"values\":[\"auto\",\"quiet\",\"low\",\"low\"]}]}]}").fanSpeeds(),Arrays.asList("auto","quiet","low")));
  test("missing model metadata does not invent modes or fan choices",()-> {
   DeviceProfile p=new DeviceProfile(null); equal(p.modes(),Collections.emptyList());equal(p.fanSpeeds(),Collections.emptyList());equal(p.key("RoomTemperature","rmtmp"),"rmtmp");range(p.temperatureRange("cool"),16,30,1);
  });
  test("status capabilities supply sensor keys",()->equal(profile("{\"statusCapabilitiesV2\":[{\"capability\":\"RoomTemperature\",\"attributes\":[{\"key\":\"indoor_sensor\"}]}]}").key("RoomTemperature","rmtmp"),"indoor_sensor"));
  test("malformed optional capabilities are ignored",()-> {
   DeviceProfile p=profile("{\"controlCapabilities\":[null,3,{\"capability\":\"ACMode\",\"attributes\":[]}],\"statusCapabilitiesV2\":{}}");
   equal(p.modes(),Collections.emptyList());equal(p.fanSpeeds(),Collections.emptyList());
  });
  final DeviceProfile modes=profile("{\"controlCapabilities\":[{\"capability\":\"ACModeTemperature\",\"attributes\":[{\"range\":[{\"mode\":\"cool\",\"min\":[{\"unit\":\"Fahrenheit\",\"value\":60.8},{\"unit\":\"Celsius\",\"value\":16}],\"max\":[{\"unit\":\"Fahrenheit\",\"value\":86},{\"unit\":\"Celsius\",\"value\":30}],\"increment\":[{\"unit\":\"Celsius\",\"value\":0.5}]},{\"mode\":\"heat\",\"min\":[{\"unit\":\"Celsius\",\"value\":18}],\"max\":[{\"unit\":\"Celsius\",\"value\":28}],\"increment\":[{\"unit\":\"Celsius\",\"value\":1}]}]}]}]}");
  test("cool range chooses Celsius rather than first Fahrenheit entry",()->range(modes.temperatureRange("cool"),16,30,0.5));
  test("heat uses its own bounds",()->range(modes.temperatureRange("HEAT"),18,28,1));
  test("missing per-mode range uses fallback",()->range(modes.temperatureRange("dry"),16,30,1));
  test("invalid temperature range cannot reach controls",()->range(profile("{\"controlCapabilities\":[{\"capability\":\"ACModeTemperature\",\"attributes\":[{\"range\":{\"mode\":\"cool\",\"min\":[30],\"max\":[16],\"increment\":[0]}}]}]}").temperatureRange("cool"),16,30,1));
  test("CSV preserves comma quotes and line breaks",()->equal(MetricStore.csv("a,\"b\"\nc"),"\"a,\"\"b\"\"\nc\""));
  test("CSV keeps numeric negative readings",()->equal(MetricStore.csv("-1.5"),"\"-1.5\""));
  test("CSV neutralizes formula-shaped text",()-> {
   for(String text:Arrays.asList("=1+1","+SUM(A1)","@SUM(A1)","-cmd"))equal(MetricStore.csv(text),"\"'"+text+"\"");
  });
  test("CSV row uses CRLF and handles empty cells",()-> {
   StringWriter writer=new StringWriter();MetricStore.writeRow(writer,Arrays.asList("","23.5","a,b"));equal(writer.toString(),"\"\",\"23.5\",\"a,b\"\r\n");
  });
  test("flatten preserves nested readings arrays null and precision",()-> {
   Map<String,String> out=new LinkedHashMap<>();MetricStore.flatten(new JSONObject("{\"z\":null,\"sensor\":{\"room\":23.75,\"faults\":[\"F1\",\"F2\"]},\"online\":true}"),"",out);
   equal(new ArrayList<>(out.keySet()),Arrays.asList("online","sensor.faults","sensor.room","z"));equal(out.get("sensor.room"),"23.75");equal(out.get("sensor.faults"),"[\"F1\",\"F2\"]");equal(out.get("z"),"");equal(out.get("online"),"true");
  });
  test("room temperature missing null invalid and sentinel are not readings",()-> {
   for(String json:Arrays.asList("{}","{\"rmtmp\":null}","{\"rmtmp\":-1}","{\"rmtmp\":\"-1.0\"}","{\"rmtmp\":\"NaN\"}","{\"rmtmp\":\"Infinity\"}","{\"rmtmp\":\"oops\"}"))
    yes(Telemetry.panel(new DeviceProfile(null),new JSONObject(json)).startsWith("Room temperature: Not reported"),json);
  });
  test("room sensor uses advertised key and preserves fractional or zero reading",()-> {
   DeviceProfile p=profile("{\"statusCapabilitiesV2\":[{\"capability\":\"RoomTemperature\",\"attributes\":[{\"key\":\"indoor\"}]}]}");
   yes(Telemetry.panel(p,new JSONObject("{\"indoor\":23.75,\"rmtmp\":99}")).startsWith("Room temperature: 23.75°C"),"must prefer advertised key");
   yes(Telemetry.panel(p,new JSONObject("{\"indoor\":0}")).startsWith("Room temperature: 0°C"),"zero is a real numeric reading");
  });
  test("fan setting is not represented as measured RPM",()-> {
   String panel=Telemetry.panel(new DeviceProfile(null),new JSONObject("{\"acfs\":\"high\"}"));
   yes(panel.contains("Fan setting: High"),panel);
   String rpm=Arrays.stream(panel.split("\\n")).filter(line->line.startsWith("Measured fan RPM:")).findFirst().orElse("");
   yes(!rpm.isEmpty()&&!rpm.matches(".*\\d.*")&&rpm.toLowerCase(Locale.ROOT).contains("not "),panel);
  });
  test("daily energy CSV dates are ISO and kWh precision is preserved",()-> {
   ByteArrayOutputStream output=new ByteArrayOutputStream();
   MetricStore.exportEnergy(new JSONObject("{\"data\":[{\"day\":\"13102026\",\"power\":15.644910538737001},{\"day\":\"19102026\",\"power\":0}]}"),output);
   String csv=output.toString("UTF-8");
   equal(csv,"\"date\",\"energy_kwh\",\"source\"\r\n\"2026-10-13\",\"15.644910538737001\",\"Panasonic Daily energy history\"\r\n\"2026-10-19\",\"0\",\"Panasonic Daily energy history\"\r\n");
   yes(!csv.toLowerCase(Locale.ROOT).contains("watt"),"energy export must not relabel energy as watts");
  });
  test("energy CSV accepts wrapped API data and skips malformed rows",()-> {
   ByteArrayOutputStream output=new ByteArrayOutputStream();
   MetricStore.exportEnergy(new JSONObject("{\"data\":{\"data\":[null,{\"day\":\"18102026\",\"power\":\"12.34567\"}]}}"),output);
   yes(output.toString("UTF-8").contains("\"2026-10-18\",\"12.34567\""),"wrapped daily row preserved");
  });
  test("missing energy history cannot silently export empty results",()-> {
   try{MetricStore.exportEnergy(new JSONObject(),new ByteArrayOutputStream());throw new AssertionError("missing history should fail");}
   catch(IOException expected){}
  });
  System.out.println("RESULT " + passed + " passed, " + failed + " failed");
  if(failed>0)System.exit(1);
 }
}
