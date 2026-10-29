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
    private android.widget.ImageView resumeCover;
    private android.graphics.Bitmap resumeBitmap;
    private long resumeFrameId;
    private final java.util.concurrent.ExecutorService workoutQueue=java.util.concurrent.Executors.newSingleThreadExecutor();
    private String pendingBackup;
    private boolean pageReady;
    private boolean firstFrameReady;
    private boolean startupRequested;
    private boolean nativeThemeInitialized,nativeDark;
    private int nativeThemeColor;
    private String lastNotificationStatus;
    private android.widget.FrameLayout root;
    private String pendingOpenDate;
    private boolean pendingWorkout;
    private String pendingReminderId;
    private static final int SAVE=101, OPEN=102, MAX_BYTES=20_000_000;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        pendingWorkout=getIntent().getBooleanExtra("openWorkout",false);
        pendingOpenDate=getIntent().getStringExtra("openDate");pendingReminderId=getIntent().getStringExtra("openReminderId");
        android.content.SharedPreferences appearance=getSharedPreferences("appearance",MODE_PRIVATE);
        int background=appearance.getInt("background",BuildConfig.FRIEND_SHIFT?0xff8d8d8d:0xff03192e);
        applyNativeTheme(background,appearance.getBoolean("dark",!BuildConfig.FRIEND_SHIFT));
        web=new WebView(this);
        web.setBackgroundColor(background);
        root=new android.widget.FrameLayout(this);root.setBackgroundColor(background);
        root.addView(web,new android.widget.FrameLayout.LayoutParams(-1,-1));setContentView(root);
        // Keep the launch transition on the previous surface until WebView has a real frame.
        // Do not draw the empty native/WebView background as an intermediate screen.
        root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){
            @Override public boolean onPreDraw(){
                if(!firstFrameReady)return false;
                root.getViewTreeObserver().removeOnPreDrawListener(this);return true;
            }
        });
        WebSettings settings=web.getSettings();
        settings.setJavaScriptEnabled(true);
        // Retain rasterized tiles while the Activity is behind another app.
        settings.setOffscreenPreRaster(true);
        web.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT,false);
        settings.setDomStorageEnabled(true);
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
    @Override protected void onStart(){
        super.onStart();
        if(Build.VERSION.SDK_INT>=34)overrideActivityTransition(OVERRIDE_TRANSITION_OPEN,R.anim.planner_open,R.anim.planner_hold);
        else overridePendingTransition(R.anim.planner_open,R.anim.planner_hold);
    }
    private void applyNativeTheme(int color,boolean dark){
        if(nativeThemeInitialized&&nativeThemeColor==color&&nativeDark==dark)return;
        nativeThemeInitialized=true;nativeThemeColor=color;nativeDark=dark;
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(color));
        getWindow().setStatusBarColor(color);getWindow().setNavigationBarColor(color);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if(root!=null)root.setBackgroundColor(color);
        if(web!=null)web.setBackgroundColor(color);
    }
    private void finishStartup(){
        if(web==null||startupRequested)return;startupRequested=true;
        web.postVisualStateCallback(1,new WebView.VisualStateCallback(){@Override public void onComplete(long requestId){
            if(web==null||isFinishing()||isDestroyed())return;
            firstFrameReady=true;root.invalidate();
            pageReady=true;
            web.evaluateJavascript("window.onAndroidReady&&window.onAndroidReady()",null);
            openPendingDate();
            new Thread(()->ReminderScheduler.restore(PlannerActivity.this)).start();
        }});
    }
    private void message(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void updateNotificationStatus(){if(web==null||!pageReady)return;String status=notificationStatus();if(status.equals(lastNotificationStatus))return;lastNotificationStatus=status;web.evaluateJavascript("window.receiveNotificationStatus&&window.receiveNotificationStatus("+status+")",null);}
    private String notificationStatus(){return "{\"supported\":true,\"notifications\":"+ReminderScheduler.notificationsAllowed(this)+",\"exact\":"+ReminderScheduler.exactAllowed(this)+"}";}
    private void configureNotifications(){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},201);return;}
        if(!ReminderScheduler.notificationsAllowed(this)){Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());startActivity(i);return;}
        if(Build.VERSION.SDK_INT>=31&&!ReminderScheduler.exactAllowed(this)){try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(Exception e){message("Открой настройки приложения и разреши точные напоминания");}return;}
        ReminderScheduler.restore(this);updateNotificationStatus();message("Уведомления включены");
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==201){updateNotificationStatus();if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)configureNotifications();}}
    private void preserveResumeFrame(){
        if(web==null||root==null||!pageReady||resumeCover!=null||web.getWidth()<1||web.getHeight()<1)return;
        try{
            android.graphics.Bitmap frame=android.graphics.Bitmap.createBitmap(web.getWidth(),web.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
            web.draw(new android.graphics.Canvas(frame));
            resumeBitmap=frame;
            resumeCover=new android.widget.ImageView(this);
            resumeCover.setScaleType(android.widget.ImageView.ScaleType.FIT_XY);
            resumeCover.setImageBitmap(frame);
            resumeCover.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            root.addView(resumeCover,new android.widget.FrameLayout.LayoutParams(-1,-1));
        }catch(RuntimeException|OutOfMemoryError ignored){clearResumeFrame();}
    }
    private void clearResumeFrame(){
        if(resumeCover!=null){resumeCover.setImageDrawable(null);root.removeView(resumeCover);resumeCover=null;}
        // Let the render thread release the old display list before GC reclaims the bitmap.
        resumeBitmap=null;
    }
    private void revealResumeFrame(){
        if(web==null||resumeCover==null||!hasWindowFocus())return;
        final long token=++resumeFrameId;
        web.postVisualStateCallback(token,new WebView.VisualStateCallback(){@Override public void onComplete(long id){
            if(web==null||isFinishing()||isDestroyed()||token!=resumeFrameId||!hasWindowFocus())return;
            web.invalidate();
            web.postOnAnimation(()->{if(token==resumeFrameId&&hasWindowFocus())clearResumeFrame();});
        }});
    }
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)revealResumeFrame();}
    @Override protected void onResume(){super.onResume();WorkoutTimer.foreground=true;if(web!=null&&pageReady){updateNotificationStatus();web.evaluateJavascript("window.onPlannerResume&&window.onPlannerResume()",value->revealResumeFrame());}}
    @Override protected void onPause(){++resumeFrameId;preserveResumeFrame();WorkoutTimer.foreground=false;super.onPause();}
    private void openExternalLink(String value){runOnUiThread(()->{try{if(value==null||value.length()>10000)return;Uri uri=Uri.parse(value);if(!("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))||uri.getHost()==null){message("Нужна ссылка http или https");return;}Intent intent=new Intent(Intent.ACTION_VIEW,uri);intent.addCategory(Intent.CATEGORY_BROWSABLE);startActivity(intent);}catch(Exception e){message("Не удалось открыть ссылку");}});}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);pendingWorkout=intent.getBooleanExtra("openWorkout",false);pendingOpenDate=intent.getStringExtra("openDate");pendingReminderId=intent.getStringExtra("openReminderId");openPendingDate();}
    private void openPendingDate(){if(pageReady&&pendingWorkout){pendingWorkout=false;web.evaluateJavascript("window.openWorkout&&window.openWorkout()",null);}if(pageReady&&pendingOpenDate!=null){String date=pendingOpenDate;pendingOpenDate=null;web.evaluateJavascript("window.openReminderDate&&window.openReminderDate("+JSONObject.quote(date)+","+JSONObject.quote(pendingReminderId)+")",null);}}
    public class BackupBridge {
        @JavascriptInterface public boolean friendSchedule(){return BuildConfig.FRIEND_SHIFT;}
        @JavascriptInterface public void workoutTimer(String token,long end,String title){if(token==null||token.length()>100||title==null||title.length()>100)return;workoutQueue.execute(()->WorkoutTimer.schedule(getApplicationContext(),token,end,title));}

        @JavascriptInterface public boolean copyText(String text){try{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(clipboard==null)return false;clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Текст заметки",text==null?"":text));return true;}catch(Exception e){return false;}}

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
    @Override public void onBackPressed(){web.evaluateJavascript("(function(){const d=document.querySelector('dialog[open]');if(d){d.close();return true;}if(window.handlePlannerBack&&window.handlePlannerBack())return true;return false;})()",closed->{if(!"true".equals(closed))moveTaskToBack(true);});}
    @Override protected void onDestroy(){if(web!=null){web.removeJavascriptInterface("AndroidFiles");web.destroy();web=null;}workoutQueue.shutdown();super.onDestroy();}
}
