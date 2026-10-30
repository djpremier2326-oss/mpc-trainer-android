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
    private final java.util.concurrent.ExecutorService workoutQueue=java.util.concurrent.Executors.newSingleThreadExecutor();
    private android.webkit.ValueCallback<Uri[]> photoCallback;
    private String pendingBackup;
    private boolean pageReady;
    private boolean activityResumed;
    private long launchStarted;
    private boolean startupRequested;
    private final RendererRecoveryPolicy rendererRecovery=new RendererRecoveryPolicy();
    private boolean pendingRendererRecovery;
    private boolean rendererWasBackgrounded;
    private View recoveryButton;
    private boolean nativeThemeInitialized,nativeDark;
    private int nativeThemeColor;
    private String lastNotificationStatus;
    private android.widget.FrameLayout root;
    private String pendingOpenDate;
    private boolean pendingWorkout;
    private String pendingReminderId;
    private static final int SAVE=101, OPEN=102, MAX_BYTES=50_000_000;
    @Override public void onCreate(Bundle saved) {
        launchStarted=android.os.SystemClock.elapsedRealtime();
        android.content.SharedPreferences appearance=getSharedPreferences("appearance",MODE_PRIVATE);
        boolean dark=appearance.getBoolean("dark",!BuildConfig.FRIEND_SHIFT);
        setTheme(dark?R.style.PlannerIceLaunchTheme:R.style.PlannerGraphiteLaunchTheme);
        super.onCreate(saved);
        // Keep the platform launch transition; no app-owned splash or extra animation.
        if(Build.VERSION.SDK_INT>=31)getSplashScreen().setOnExitAnimationListener(screen->screen.remove());
        pendingWorkout=getIntent().getBooleanExtra("openWorkout",false);
        pendingOpenDate=getIntent().getStringExtra("openDate");pendingReminderId=getIntent().getStringExtra("openReminderId");
        int background=appearance.getInt("background",BuildConfig.FRIEND_SHIFT?0xff8d8d8d:0xff03192e);
        applyNativeTheme(background,appearance.getBoolean("dark",!BuildConfig.FRIEND_SHIFT));
        root=new android.widget.FrameLayout(this);
        root.setBackgroundColor(background);
        setContentView(root);
        // Let Android reveal the real first page rather than an empty WebView frame.
        // This listener removes itself permanently: resumes never wait or animate again.
        root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){
            @Override public boolean onPreDraw(){
                if(!pageReady&&recoveryButton==null)return false;
                root.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        });
        // Do not touch android.webkit before the asynchronous startup callback.
        java.util.concurrent.ExecutorService startupExecutor=java.util.concurrent.Executors.newSingleThreadExecutor();
        androidx.webkit.WebViewCompat.startUpWebView(getApplicationContext(),
            new androidx.webkit.WebViewStartUpConfig.Builder(startupExecutor).build(),
            new androidx.webkit.WebViewOutcomeReceiver<androidx.webkit.WebViewStartUpResult,androidx.webkit.WebViewStartupException>(){
                @Override public void onResult(androidx.webkit.WebViewStartUpResult result){
                    startupExecutor.shutdown();
                    android.util.Log.i("PlannerStartup","engine ready: "+(android.os.SystemClock.elapsedRealtime()-launchStarted)+"ms; UI work="+result.getTotalTimeInUiThreadMillis());
                    initializeWeb(nativeThemeColor);
                }
                @Override public void onError(androidx.webkit.WebViewStartupException error){
                    startupExecutor.shutdown();
                    android.util.Log.e("PlannerStartup","Async startup failed; using standard initialization",error);
                    initializeWeb(nativeThemeColor);
                }
            });
    }
    private void recoverRenderer(){
        if(isFinishing()||isDestroyed()||web!=null)return;
        pendingRendererRecovery=true;
        if(!activityResumed)return;
        pendingRendererRecovery=false;
        if(recoveryButton!=null){root.removeView(recoveryButton);recoveryButton=null;}
        initializeWeb(nativeThemeColor);
    }
    private void initializeWeb(int background){
        if(isFinishing()||isDestroyed()||web!=null)return;
        if(!activityResumed){pendingRendererRecovery=true;return;}
        pendingRendererRecovery=false;
        rendererWasBackgrounded=false;
        web=new WebView(this);
        web.setBackgroundColor(background);
        root.addView(web,0,new android.widget.FrameLayout.LayoutParams(-1,-1));
        WebSettings settings=web.getSettings();
        settings.setJavaScriptEnabled(true);
        web.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT,false);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean onRenderProcessGone(WebView view,android.webkit.RenderProcessGoneDetail detail){
                boolean current=view==web;
                android.util.Log.e("PlannerRenderer","Renderer exited; crashed="+detail.didCrash()+"; foreground="+activityResumed+"; current="+current);
                getSharedPreferences("diagnostics",MODE_PRIVATE).edit()
                    .putLong("rendererExitAt",System.currentTimeMillis()).putBoolean("rendererCrashed",detail.didCrash()).apply();
                if(current){
                    if(photoCallback!=null){photoCallback.onReceiveValue(null);photoCallback=null;}
                    web=null;pageReady=false;startupRequested=false;lastNotificationStatus=null;
                }
                root.removeView(view);view.removeJavascriptInterface("AndroidFiles");view.destroy();
                if(current&&!isFinishing()&&!isDestroyed()){
                    boolean automatic=rendererRecovery.onExit(detail.didCrash(),activityResumed&&!rendererWasBackgrounded,android.os.SystemClock.elapsedRealtime());
                    if(automatic){pendingRendererRecovery=true;root.post(()->recoverRenderer());}
                    else {
                        pendingRendererRecovery=false;
                        android.widget.Button retry=new android.widget.Button(PlannerActivity.this);
                        recoveryButton=retry;retry.setText("Восстановить экран");
                        android.widget.FrameLayout.LayoutParams params=new android.widget.FrameLayout.LayoutParams(-2,-2,android.view.Gravity.CENTER);
                        root.addView(retry,params);
                        retry.setOnClickListener(v->{rendererRecovery.reset();recoverRenderer();});
                    }
                }
                return true;
            }
            @Override public void onPageFinished(WebView view,String url){
                if(url.equals("file:///android_asset/index.html"))view.post(()->{
                    if(view!=web||isFinishing()||isDestroyed())return;
                    // page commit can precede the planner's bottom-of-document render.
                    // Wait for its pixels, not a timer or an empty document commit.
                    view.postVisualStateCallback(0,new WebView.VisualStateCallback(){
                        @Override public void onComplete(long id){if(view==web)finishStartup();}
                    });
                });
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){String url=request.getUrl().toString();if(request.isForMainFrame()&&(url.startsWith("https://")||url.startsWith("http://")))openExternalLink(url);return !url.startsWith("file:///android_asset/");}
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onShowFileChooser(WebView view,android.webkit.ValueCallback<Uri[]> callback,WebChromeClient.FileChooserParams params){
                if(photoCallback!=null)photoCallback.onReceiveValue(null);
                photoCallback=callback;
                boolean withVideo=false;
                for(String type:params.getAcceptTypes())if(type!=null&&type.contains("video"))withVideo=true;
                String mediaType=withVideo?"*/*":"image/*";
                Intent picker=new Intent(android.provider.MediaStore.ACTION_PICK_IMAGES);
                if(!withVideo)picker.setType("image/*");
                if(Build.VERSION.SDK_INT>=35)picker.putExtra(android.provider.MediaStore.EXTRA_PICK_IMAGES_LAUNCH_TAB,android.provider.MediaStore.PICK_IMAGES_TAB_ALBUMS);
                Intent gallery=new Intent(Intent.ACTION_PICK).setDataAndType(withVideo?android.provider.MediaStore.Files.getContentUri("external"):android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,mediaType);
                Intent fallback=new Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mediaType);
                if(withVideo){String[] types={"image/*","video/*"};gallery.putExtra(Intent.EXTRA_MIME_TYPES,types);fallback.putExtra(Intent.EXTRA_MIME_TYPES,types);}
                for(Intent pick:new Intent[]{gallery,picker,fallback}){
                    try{startActivityForResult(pick,103);return true;}
                    catch(android.content.ActivityNotFoundException unavailable){}
                    catch(SecurityException unavailable){}
                }
                photoCallback=null;callback.onReceiveValue(null);message("Не удалось открыть галерею");
                return true;
            }
            @Override public boolean onJsConfirm(WebView v,String url,String message,JsResult result){new AlertDialog.Builder(PlannerActivity.this).setMessage(message).setPositiveButton("Да",(d,w)->result.confirm()).setNegativeButton("Отмена",(d,w)->result.cancel()).setOnCancelListener(d->result.cancel()).show();return true;}
            @Override public boolean onJsAlert(WebView v,String url,String message,JsResult result){new AlertDialog.Builder(PlannerActivity.this).setMessage(message).setPositiveButton("ОК",(d,w)->result.confirm()).setOnCancelListener(d->result.confirm()).show();return true;}
        });
        web.addJavascriptInterface(new BackupBridge(),"AndroidFiles");
        // Async creation can finish after Activity.onResume or after it went to background.
        // Newly attached WebView must receive the current lifecycle state in both cases.
        if(activityResumed)web.onResume();else web.onPause();
        web.loadUrl("file:///android_asset/index.html");
    }
    private void applyNativeTheme(int color,boolean dark){
        if(nativeThemeInitialized&&nativeThemeColor==color&&nativeDark==dark)return;
        nativeThemeInitialized=true;nativeThemeColor=color;nativeDark=dark;
        // Persist the selected launch theme in Android itself, before the next process starts.
        if(Build.VERSION.SDK_INT>=31)getSplashScreen().setSplashScreenTheme(
            dark?R.style.PlannerIceLaunchTheme:R.style.PlannerGraphiteLaunchTheme);
        int chrome=color;
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(chrome));
        getWindow().setStatusBarColor(chrome);getWindow().setNavigationBarColor(chrome);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if(root!=null)root.setBackgroundColor(color);
        if(web!=null)web.setBackgroundColor(color);
    }
    private void finishStartup(){
        if(web==null||startupRequested||isFinishing()||isDestroyed())return;
        startupRequested=true;pageReady=true;
        rendererRecovery.onReady(android.os.SystemClock.elapsedRealtime());
        android.util.Log.i("PlannerStartup","web frame: "+(android.os.SystemClock.elapsedRealtime()-launchStarted)+"ms");
        web.evaluateJavascript("window.onAndroidReady&&window.onAndroidReady()",null);
        root.invalidate();
        openPendingDate();
        workoutQueue.execute(()->ReminderScheduler.restore(getApplicationContext()));
        web.postOnAnimation(()->{if(!isFinishing()&&!isDestroyed())reportFullyDrawn();});
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
    @Override protected void onResume(){
        super.onResume();
        activityResumed=true;
        WorkoutTimer.foreground=true;
        if(pendingRendererRecovery&&web==null)recoverRenderer();
        if(web!=null){
            final WebView resumed=web;
            resumed.onResume();
            // A dead renderer may report its background exit only after resume.
            // Clear the marker only after the same live renderer answers.
            resumed.evaluateJavascript("true",value->{if(web==resumed)rendererWasBackgrounded=false;});
        }
        if(web!=null&&pageReady){
            updateNotificationStatus();
            web.evaluateJavascript("window.onPlannerResume&&window.onPlannerResume()",null);
        }
    }
    @Override protected void onPause(){
        activityResumed=false;
        rendererWasBackgrounded=true;
        WorkoutTimer.foreground=false;
        if(web!=null)web.onPause();
        super.onPause();
    }
    private void openExternalLink(String value){runOnUiThread(()->{try{if(value==null||value.length()>10000)return;Uri uri=Uri.parse(value);if(!("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))||uri.getHost()==null){message("Нужна ссылка http или https");return;}Intent intent=new Intent(Intent.ACTION_VIEW,uri);intent.addCategory(Intent.CATEGORY_BROWSABLE);startActivity(intent);}catch(Exception e){message("Не удалось открыть ссылку");}});}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);pendingWorkout=intent.getBooleanExtra("openWorkout",false);pendingOpenDate=intent.getStringExtra("openDate");pendingReminderId=intent.getStringExtra("openReminderId");openPendingDate();}
    private void openPendingDate(){if(pageReady&&pendingWorkout){pendingWorkout=false;web.evaluateJavascript("window.openWorkout&&window.openWorkout()",null);}if(pageReady&&pendingOpenDate!=null){String date=pendingOpenDate;pendingOpenDate=null;web.evaluateJavascript("window.openReminderDate&&window.openReminderDate("+JSONObject.quote(date)+","+JSONObject.quote(pendingReminderId)+")",null);}}
    public class BackupBridge {
        @JavascriptInterface public void startupMetrics(String metrics){if(metrics!=null&&metrics.length()<1500)android.util.Log.i("PlannerStartup","DOM: "+metrics);}
        @JavascriptInterface public boolean friendSchedule(){return BuildConfig.FRIEND_SHIFT;}
        @JavascriptInterface public void workoutTimer(String token,long end,String title){if(token==null||token.length()>100||title==null||title.length()>100)return;workoutQueue.execute(()->WorkoutTimer.schedule(getApplicationContext(),token,end,title));}

        @JavascriptInterface public boolean copyText(String text){try{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(clipboard==null)return false;clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Текст заметки",text==null?"":text));return true;}catch(Exception e){return false;}}

        @JavascriptInterface public void hideKeyboard(){runOnUiThread(()->{if(web==null)return;android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(web.getWindowToken(),0);web.clearFocus();});}
        @JavascriptInterface public String notificationStatus(){return PlannerActivity.this.notificationStatus();}
        @JavascriptInterface public void requestNotifications(){runOnUiThread(()->configureNotifications());}
        @JavascriptInterface public boolean syncReminders(String json){return ReminderScheduler.sync(PlannerActivity.this,json);}
        @JavascriptInterface public void testNotification(){runOnUiThread(()->{if(!ReminderScheduler.notificationsAllowed(PlannerActivity.this)){configureNotifications();return;}ReminderScheduler.notify(PlannerActivity.this,"test","Уведомления работают","Напоминания о делах появятся здесь",java.time.LocalDate.now().toString());message("Проверочное уведомление отправлено");});}
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
        if(request==103){android.webkit.ValueCallback<Uri[]> callback=photoCallback;photoCallback=null;if(callback!=null)callback.onReceiveValue(result==RESULT_OK&&data!=null&&data.getData()!=null?new Uri[]{data.getData()}:null);return;}
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
    @Override public void onBackPressed(){if(web==null||!pageReady){moveTaskToBack(true);return;}web.evaluateJavascript("(function(){const d=document.querySelector('dialog[open]');if(d){d.close();return true;}if(window.handlePlannerBack&&window.handlePlannerBack())return true;return false;})()",closed->{if(!"true".equals(closed))moveTaskToBack(true);});}
    @Override protected void onDestroy(){if(photoCallback!=null){photoCallback.onReceiveValue(null);photoCallback=null;}if(web!=null){web.removeJavascriptInterface("AndroidFiles");web.destroy();web=null;}workoutQueue.shutdown();super.onDestroy();}
}
