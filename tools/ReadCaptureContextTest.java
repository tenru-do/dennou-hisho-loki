package com.example.rokidkeyboardbridge;
import org.json.*;
public final class ReadCaptureContextTest {
 static int checks;static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static JSONObject fix(long at)throws Exception{return new JSONObject().put("available",true).put("source","phone").put("latitude",35.123456).put("longitude",139.654321).put("accuracyMeters",25.1).put("locationAt",at);}
 public static void main(String[] args)throws Exception {
  long now=java.time.Instant.parse("2026-10-15T00:10:00Z").toEpochMilli();
  JSONObject meta=ReadCaptureContext.safe(fix(now-7000),now);String[] lines=ReadCaptureContext.lines(meta);
  check(meta.getBoolean("available"),"fresh phone fix accepted");
  check(lines[0].contains("2026-10-15 09:10:00 JST"),"capture clock rendered in Japan");
  check(lines[1].contains("35.12346")&&lines[1].contains("139.65432"),"coordinates preserved with bounded precision");
  check(lines[1].contains("スマホ位置"),"phone position not asserted as glasses position");
  check(lines[2].contains("26m")&&lines[2].contains("7秒"),"accuracy and sample age explicit");
  check(ReadCaptureContext.prompt(meta).contains("予約年として補わず"),"capture year is not inferred as appointment year");
  check(!ReadCaptureContext.safe(fix(now-90001),now).getBoolean("available"),"stale fix omitted");
  check(!ReadCaptureContext.safe(fix(now+1),now).getBoolean("available"),"future fix omitted");
  check(!ReadCaptureContext.safe(fix(now).put("accuracyMeters",501),now).getBoolean("available"),"inaccurate fix omitted");
  check(!ReadCaptureContext.safe(fix(now).put("accuracyMeters","NaN"),now).getBoolean("available"),"nonfinite fix omitted");
  check(!ReadCaptureContext.safe(fix(now).put("latitude",91),now).getBoolean("available"),"invalid coordinate omitted");
  check(!ReadCaptureContext.safe(fix(now).put("source","unknown"),now).getBoolean("available"),"unknown origin omitted");
  JSONObject missing=ReadCaptureContext.safe(null,now);check(ReadCaptureContext.lines(missing)[1].contains("未取得"),"no location is visible");
  check(!missing.has("latitude")&&!missing.has("longitude"),"missing fix does not leak stale coordinates");
  check(ReadCaptureContext.safe(fix(now-90000).put("accuracyMeters",500),now).getBoolean("available"),"freshness and accuracy bounds inclusive");
  System.out.println("ReadCaptureContext PASS checks="+checks);
 }
}
