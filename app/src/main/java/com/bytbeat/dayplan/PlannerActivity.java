package com.bytbeat.dayplan;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.graphics.Color;
import android.view.View;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class PlannerActivity extends Activity {
    private WebView web;
    private String pendingBackup;
    private boolean pageReady;
    private boolean startupRequested;
    private String pendingOpenDate;
    private String pendingReminderId;
    private static final int SAVE=101, OPEN=102, MAX_BYTES=20_000_000;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        pendingOpenDate=getIntent().getStringExtra("openDate");pendingReminderId=getIntent().getStringExtra("openReminderId");
        android.content.SharedPreferences appearance=getSharedPreferences("appearance",MODE_PRIVATE);
        int background=appearance.getInt("background",0xff03192e);
        applyNativeTheme(background,appearance.getBoolean("dark",true));
        web=new WebView(this);
        web.setBackgroundColor(background);
        web.setVisibility(View.INVISIBLE);
        setContentView(web);
        WebSettings settings=web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setOffscreenPreRaster(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){if(!startupRequested)view.evaluateJavascript("if(document.documentElement.dataset.appReady===\"true\"){window.AndroidFiles.uiReady();}",null);}
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){String url=request.getUrl().toString();if(request.isForMainFrame()&&(url.startsWith("https://")||url.startsWith("http://")))openExternalLink(url);return !url.startsWith("file:///android_asset/");}
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onJsConfirm(WebView v,String url,String message,JsResult result){new AlertDialog.Builder(PlannerActivity.this).setMessage(message).setPositiveButton("Да",(d,w)->result.confirm()).setNegativeButton("Отмена",(d,w)->result.cancel()).setOnCancelListener(d->result.cancel()).show();return true;}
            @Override public boolean onJsAlert(WebView v,String url,String message,JsResult result){new AlertDialog.Builder(PlannerActivity.this).setMessage(message).setPositiveButton("ОК",(d,w)->result.confirm()).setOnCancelListener(d->result.confirm()).show();return true;}
        });
        web.addJavascriptInterface(new BackupBridge(),"AndroidFiles");
        web.loadUrl("file:///android_asset/index.html");
    }
    private void applyNativeTheme(int color,boolean dark){
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(color));
        getWindow().setStatusBarColor(color);getWindow().setNavigationBarColor(color);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if(web!=null)web.setBackgroundColor(color);
    }
    private void finishStartup(){
        if(web==null||startupRequested)return;startupRequested=true;
        web.postVisualStateCallback(1,new WebView.VisualStateCallback(){@Override public void onComplete(long requestId){
            if(web==null||isFinishing()||isDestroyed())return;
            web.setVisibility(View.VISIBLE);pageReady=true;
            web.evaluateJavascript("window.onAndroidReady&&window.onAndroidReady()",null);
            openPendingDate();
            new Thread(()->ReminderScheduler.restore(PlannerActivity.this)).start();
        }});
    }
    private void message(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void updateNotificationStatus(){if(web!=null&&pageReady)web.evaluateJavascript("window.receiveNotificationStatus&&window.receiveNotificationStatus("+notificationStatus()+")",null);}
    private String notificationStatus(){return "{\"supported\":true,\"notifications\":"+ReminderScheduler.notificationsAllowed(this)+",\"exact\":"+ReminderScheduler.exactAllowed(this)+"}";}
    private void configureNotifications(){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},201);return;}
        if(!ReminderScheduler.notificationsAllowed(this)){Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(i);return;}
        if(Build.VERSION.SDK_INT>=31&&!ReminderScheduler.exactAllowed(this)){try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(Exception e){message("Открой настройки приложения и разреши точные напоминания");}return;}
        ReminderScheduler.restore(this);updateNotificationStatus();message("Уведомления включены");
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==201){updateNotificationStatus();if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)configureNotifications();}}
    @Override protected void onResume(){super.onResume();if(web!=null&&pageReady){new Thread(()->ReminderScheduler.restore(PlannerActivity.this)).start();updateNotificationStatus();}}
    private void openExternalLink(String value){runOnUiThread(()->{try{if(value==null||value.length()>10000)return;Uri uri=Uri.parse(value);if(!("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))||uri.getHost()==null){message("Нужна ссылка http или https");return;}Intent intent=new Intent(Intent.ACTION_VIEW,uri);intent.addCategory(Intent.CATEGORY_BROWSABLE);startActivity(intent);}catch(Exception e){message("Не удалось открыть ссылку");}});}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);pendingOpenDate=intent.getStringExtra("openDate");pendingReminderId=intent.getStringExtra("openReminderId");openPendingDate();}
    private void openPendingDate(){if(pageReady&&pendingOpenDate!=null){String date=pendingOpenDate;pendingOpenDate=null;web.evaluateJavascript("window.openReminderDate&&window.openReminderDate("+JSONObject.quote(date)+","+JSONObject.quote(pendingReminderId)+")",null);}}
    public class BackupBridge {
        @JavascriptInterface public void hideKeyboard(){runOnUiThread(()->{if(web==null)return;android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(web.getWindowToken(),0);web.clearFocus();});}
        @JavascriptInterface public String notificationStatus(){return PlannerActivity.this.notificationStatus();}
        @JavascriptInterface public void requestNotifications(){runOnUiThread(()->configureNotifications());}
        @JavascriptInterface public boolean syncReminders(String json){return ReminderScheduler.sync(PlannerActivity.this,json);}
        @JavascriptInterface public void testNotification(){runOnUiThread(()->{if(!ReminderScheduler.notificationsAllowed(PlannerActivity.this)){configureNotifications();return;}ReminderScheduler.notify(PlannerActivity.this,"test","Уведомления работают","Напоминания о делах появятся здесь",java.time.LocalDate.now().toString());message("Проверочное уведомление отправлено");});}
        @JavascriptInterface public void uiReady(){runOnUiThread(()->finishStartup());}
        @JavascriptInterface public void themeColors(String bg,boolean dark){runOnUiThread(()->{try{int color=Color.parseColor(bg);getSharedPreferences("appearance",MODE_PRIVATE).edit().putInt("background",color).putBoolean("dark",dark).apply();applyNativeTheme(color,dark);}catch(Exception e){}});}


        @JavascriptInterface public void saveBackup(String name,String json){
            if(json==null||json.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)return;
            runOnUiThread(()->{pendingBackup=json;try{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,name);startActivityForResult(i,SAVE);}catch(Exception e){pendingBackup=null;message("Не удалось открыть выбор файла");}});
        }
        @JavascriptInterface public void openLink(String url){openExternalLink(url);}
        @JavascriptInterface public void loadBackup(){runOnUiThread(()->{try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,OPEN);}catch(Exception e){message("Не удалось открыть выбор файла");}});}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null){if(request==SAVE)pendingBackup=null;return;}
        Uri uri=data.getData();
        if(request==SAVE){String json=pendingBackup;pendingBackup=null;if(json==null){message("Повтори сохранение копии");return;}new Thread(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new Exception();out.write(json.getBytes(StandardCharsets.UTF_8));runOnUiThread(()->message("Копия сохранена"));}catch(Exception e){runOnUiThread(()->message("Не удалось сохранить копию"));}}).start();}
        if(request==OPEN)new Thread(()->{try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            if(in==null)throw new Exception();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>MAX_BYTES)throw new Exception();out.write(buffer,0,n);}String json=out.toString("UTF-8");String name="backup.json";
            try(android.database.Cursor cursor=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}
            String script="window.importAndroidBackup("+JSONObject.quote(name)+","+JSONObject.quote(json)+");";
            runOnUiThread(()->web.evaluateJavascript(script,null));
        }catch(Exception e){runOnUiThread(()->message("Не удалось прочитать копию"));}}).start();
    }
    @Override public void onBackPressed(){web.evaluateJavascript("(function(){const d=document.querySelector('dialog[open]');if(d){d.close();return true;}if(window.handlePlannerBack&&window.handlePlannerBack())return true;return false;})()",closed->{if(!"true".equals(closed))super.onBackPressed();});}
    @Override protected void onDestroy(){if(web!=null){web.removeJavascriptInterface("AndroidFiles");web.destroy();web=null;}super.onDestroy();}
}
