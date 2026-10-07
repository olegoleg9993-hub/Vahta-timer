package ru.oilab.flightweather;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.net.Uri;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

public class MainActivity extends Activity {
 private WebView web;
 private final ExecutorService worker = Executors.newFixedThreadPool(3);
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  getWindow().setStatusBarColor(Color.rgb(11,23,34));
  getWindow().setNavigationBarColor(Color.rgb(11,23,34));
  FrameLayout frame = new FrameLayout(this);
  frame.setBackgroundColor(Color.rgb(11,23,34));
  web = new WebView(this);
  frame.addView(web, new FrameLayout.LayoutParams(-1,-1));
  frame.setOnApplyWindowInsetsListener((v,insets)->{
   v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
   return insets.consumeSystemWindowInsets();
  });
  setContentView(frame);
  web.setBackgroundColor(Color.rgb(11,23,34));
  web.getSettings().setJavaScriptEnabled(true);
  web.getSettings().setDomStorageEnabled(true);
  web.getSettings().setAllowFileAccess(false);
  web.getSettings().setAllowContentAccess(false);
  web.getSettings().setAllowFileAccessFromFileURLs(false);
  web.getSettings().setAllowUniversalAccessFromFileURLs(false);
  web.setWebViewClient(new WebViewClient() {
   @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) { return true; }
  });
  web.setWebChromeClient(new WebChromeClient());
  web.addJavascriptInterface(new Bridge(), "Native");
  web.loadUrl("file:///android_asset/index.html");
 }
 private void reply(String id, String body, String error) {
  runOnUiThread(()->{ if(!isFinishing() && web!=null) web.evaluateJavascript("window.nativeResult("+JSONObject.quote(id)+","+(body==null?"null":JSONObject.quote(body))+","+(error==null?"null":JSONObject.quote(error))+")",null); });
 }
 private void request(String id, String url) { request(id,url,false); }
 private void request(String id, String url, boolean textResponse) {
  worker.execute(()->{
   HttpURLConnection c=null;
   try {
    c=(HttpURLConnection)new URL(url).openConnection();
    c.setConnectTimeout(18000); c.setReadTimeout(25000);
    c.setRequestProperty("User-Agent","PoletVahta/0.5 (Android personal weather prototype)");
    c.setRequestProperty("Accept","application/json");
    c.setInstanceFollowRedirects(false);
    int status=c.getResponseCode();
    if(status==204){reply(id,"[]",null);return;}
    if(status!=200) throw new Exception(status==429?"Источник временно ограничил запросы. Повторите позже.":"Источник погоды ответил с ошибкой "+status);
    try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
     byte[] b=new byte[8192]; int n;
     while((n=in.read(b))!=-1) {out.write(b,0,n); if(out.size()>4000000) throw new Exception("Ответ источника слишком большой");}
     String body=out.toString(StandardCharsets.UTF_8.name());
     reply(id,textResponse?new JSONObject().put("text",body).toString():body,null);
    }
   } catch(Exception e) {reply(id,null,e.getMessage()==null?"Не удалось загрузить погоду":e.getMessage());}
   finally {if(c!=null)c.disconnect();}
  });
 }
 public class Bridge {
  @JavascriptInterface public void cloudRun(String id) {
   request(id,"https://thredds.ucar.edu/thredds/catalog/grib/NCEP/GFS/Global_0p25deg/latest.xml",true);
  }
  @JavascriptInterface public void cloud(String id,String run,String lat,String lon,String start,String end) {
   try {
    double a=Double.parseDouble(lat),b=Double.parseDouble(lon);
    if(!Double.isFinite(a)||!Double.isFinite(b)||Math.abs(a)>90||Math.abs(b)>180||run==null||!run.matches("[0-9]{8}_[0-9]{4}")||start==null||end==null||!start.matches("[0-9T:Z-]{20}")||!end.matches("[0-9T:Z-]{20}"))throw new Exception();
    request(id,"https://thredds.ucar.edu/thredds/ncss/grid/grib/NCEP/GFS/Global_0p25deg/GFS_Global_0p25deg_"+run+".grib2?var=Geopotential_height_cloud_ceiling&var=Geopotential_height_surface&latitude="+Uri.encode(lat)+"&longitude="+Uri.encode(lon)+"&time_start="+Uri.encode(start)+"&time_end="+Uri.encode(end)+"&accept=csv",true);
   }catch(Exception e){reply(id,null,"Проверьте параметры запроса высоты облаков");}
  }

  @JavascriptInterface public void close() { runOnUiThread(()->finish()); }
  @JavascriptInterface public String load() {return getPreferences(0).getString("state","{}");}
  @JavascriptInterface public void save(String value) { if(value!=null && value.length()<2000000) getPreferences(0).edit().putString("state",value).apply(); }
  @JavascriptInterface public void weather(String id,String lats,String lons) {
   try {
    String[] a=lats.split(","), b=lons.split(",");
    if(a.length!=b.length || a.length<1 || a.length>5) throw new Exception();
    for(int i=0;i<a.length;i++) {
     double lat=Double.parseDouble(a[i]),lon=Double.parseDouble(b[i]);
     if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180) throw new Exception();
    }
    request(id,"https://api.open-meteo.com/v1/forecast?latitude="+Uri.encode(lats)+"&longitude="+Uri.encode(lons)+"&hourly=temperature_2m,visibility,wind_speed_10m,wind_direction_10m,wind_gusts_10m,cloud_cover_low,weather_code,precipitation,is_day&wind_speed_unit=ms&timezone=GMT&forecast_days=4");
   }catch(Exception e){reply(id,null,"Проверьте координаты маршрута");}
  }
  @JavascriptInterface public void search(String id,String query) {
   if(query==null || query.trim().length()<2 || query.length()>100) {reply(id,null,"Введите название населённого пункта");return;}
   request(id,"https://geocoding-api.open-meteo.com/v1/search?name="+Uri.encode(query.trim())+"&count=10&language=ru&format=json");
  }
  @JavascriptInterface public void taf(String id,String codes) {
   if(codes==null || !codes.matches("[A-Z]{4}(,[A-Z]{4})?")) {reply(id,null,"Укажите корректные коды ICAO");return;}
   request(id,"https://aviationweather.gov/api/data/taf?ids="+Uri.encode(codes)+"&format=json");
  }
  @JavascriptInterface public void metar(String id,String codes) {
   if(codes==null || !codes.matches("[A-Z]{4}(,[A-Z]{4})?")) {reply(id,null,"Код ICAO должен состоять из четырёх латинских букв");return;}
   request(id,"https://aviationweather.gov/api/data/metar?ids="+Uri.encode(codes)+"&format=json");
  }
 }
 @Override public void onBackPressed() {web.evaluateJavascript("window.handleBack && window.handleBack()",null);}
 @Override protected void onPause() { if(web!=null){web.onPause();web.pauseTimers();}super.onPause(); }
 @Override protected void onResume() { super.onResume();if(web!=null){web.onResume();web.resumeTimers();web.evaluateJavascript("window.onAppResume && window.onAppResume()",null);} }
 @Override protected void onDestroy() {worker.shutdownNow(); if(web!=null){web.removeJavascriptInterface("Native");web.destroy();web=null;}super.onDestroy();}
}
