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
    private DataVault vault;
    private java.io.File pendingVaultExport;
    private static final int VAULT_EXPORT=105, VAULT_IMPORT=106;
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
    private Integer statusBarOverride;
    private long statusBarUpdate;
    private boolean statusBarDark;
    private String lastNotificationStatus;
    private android.widget.FrameLayout root;
    private String pendingOpenDate;
    private boolean pendingWorkout;
    private String pendingReminderId;
    private static final int STARTUP_BACKGROUND=0xff121214;
    private static final int SAVE=101, OPEN=102, MAX_BYTES=50_000_000;
    @Override public void onCreate(Bundle saved) {
        launchStarted=android.os.SystemClock.elapsedRealtime();
        android.content.SharedPreferences appearance=getSharedPreferences("appearance",MODE_PRIVATE);
        setTheme(R.style.PlannerLaunchTheme);
        super.onCreate(saved);
        vault=new DataVault(getApplicationContext());
        // Keep the OS launch surface neutral until the first complete planner frame.
        // Never persist a light/different appearance as the next system splash.
        if(Build.VERSION.SDK_INT>=31){
            getSplashScreen().setSplashScreenTheme(R.style.PlannerLaunchTheme);
            getSplashScreen().setOnExitAnimationListener(splash->{
                if(pageReady)applyWindowTheme();
                if(!android.animation.ValueAnimator.areAnimatorsEnabled()||!activityResumed){splash.remove();return;}
                splash.animate().alpha(0f).setDuration(180)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(splash::remove).start();
            });
        }
        pendingWorkout=getIntent().getBooleanExtra("openWorkout",false);
        pendingOpenDate=getIntent().getStringExtra("openDate");pendingReminderId=getIntent().getStringExtra("openReminderId");
        int background=appearance.getInt("background",STARTUP_BACKGROUND);
        applyNativeTheme(background,appearance.getBoolean("dark",!BuildConfig.FRIEND_SHIFT));
        root=new android.widget.FrameLayout(this);
        root.setBackgroundColor(STARTUP_BACKGROUND);
        setContentView(root);
        // Let Android reveal the real first page rather than an empty WebView frame.
        // This listener removes itself permanently: resumes never wait or animate again.
        root.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){
            @Override public boolean onPreDraw(){
                if(!pageReady&&recoveryButton==null&&android.os.SystemClock.elapsedRealtime()-launchStarted<10000)return false;
                root.getViewTreeObserver().removeOnPreDrawListener(this);
                if(Build.VERSION.SDK_INT<31&&pageReady&&android.animation.ValueAnimator.areAnimatorsEnabled()){
                    root.setAlpha(0f);
                    root.animate().alpha(1f).setDuration(180)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .withEndAction(()->applyWindowTheme()).start();
                }else if(pageReady)applyWindowTheme();
                return true;
            }
        });
        root.postDelayed(()->{if(!pageReady&&!isFinishing()&&!isDestroyed())showStartupError();},10000);
        startWebEngine();
    }
    protected void startWebEngine(){
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
    private void showStartupError(){
        if(recoveryButton!=null||isFinishing()||isDestroyed())return;
        android.widget.Button retry=new android.widget.Button(this);
        retry.setText("Не удалось открыть ежедневник. Повторить");
        recoveryButton=retry;
        root.addView(retry,new android.widget.FrameLayout.LayoutParams(-2,-2,android.view.Gravity.CENTER));
        retry.setOnClickListener(v->{root.removeView(retry);recoveryButton=null;launchStarted=android.os.SystemClock.elapsedRealtime();if(web!=null)web.reload();else recoverRenderer();root.postDelayed(()->{if(!pageReady&&!isFinishing()&&!isDestroyed())showStartupError();},10000);});
        root.invalidate();
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
        settings.setBlockNetworkLoads(true);
        settings.setBlockNetworkImage(false);
        settings.setLoadsImagesAutomatically(true);
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
                        @Override public void onComplete(long id){if(view!=web||isFinishing()||isDestroyed())return;
                            view.evaluateJavascript("document.documentElement.dataset.appReady==='true'",ready->{
                                if(view!=web||isFinishing()||isDestroyed())return;
                                if("true".equals(ready))finishStartup();else showStartupError();
                            });}
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
                boolean multiple=params.getMode()==WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE;
                try{startActivityForResult(new Intent(PlannerActivity.this,AlbumPickerActivity.class).putExtra("video",withVideo).putExtra("multiple",multiple),103);}
                catch(Exception error){photoCallback=null;callback.onReceiveValue(null);message("Не удалось открыть альбомы");}
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
        // During startup, only update the offscreen planner surface. Changing the
        // window/system bars now exposes a bright frame beneath the launch splash.
        if(pageReady&&root!=null)root.setBackgroundColor(color);
        if(web!=null)web.setBackgroundColor(color);
        if(pageReady)applyWindowTheme();
    }
    private void applyWindowTheme(){
        if(isFinishing()||isDestroyed())return;
        if(root!=null)root.setBackgroundColor(nativeThemeColor);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(nativeThemeColor));
        getWindow().setStatusBarColor(nativeThemeColor);getWindow().setNavigationBarColor(nativeThemeColor);
        getWindow().getDecorView().setSystemUiVisibility(nativeDark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        applyStatusBarColor();
    }
    private void applyStatusBarColor(){
        if(!pageReady||isFinishing()||isDestroyed())return;
        getWindow().setStatusBarColor(statusBarOverride==null?nativeThemeColor:statusBarOverride);
        int flags=getWindow().getDecorView().getSystemUiVisibility();
        boolean dark=statusBarOverride==null?nativeDark:statusBarDark;
        getWindow().getDecorView().setSystemUiVisibility(dark?flags&~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR:flags|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }
    private void finishStartup(){
        if(web==null||startupRequested||isFinishing()||isDestroyed())return;
        startupRequested=true;pageReady=true;
        if(recoveryButton!=null){root.removeView(recoveryButton);recoveryButton=null;}
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
    private void openExternalLink(String value){runOnUiThread(()->message("Приложение работает офлайн. Интернет-ссылки отключены"));}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);pendingWorkout=intent.getBooleanExtra("openWorkout",false);pendingOpenDate=intent.getStringExtra("openDate");pendingReminderId=intent.getStringExtra("openReminderId");openPendingDate();}
    private void openPendingDate(){if(pageReady&&pendingWorkout){pendingWorkout=false;web.evaluateJavascript("window.openWorkout&&window.openWorkout()",null);}if(pageReady&&pendingOpenDate!=null){String date=pendingOpenDate;pendingOpenDate=null;web.evaluateJavascript("window.openReminderDate&&window.openReminderDate("+JSONObject.quote(date)+","+JSONObject.quote(pendingReminderId)+")",null);}}
    public class BackupBridge {
        @JavascriptInterface public String headerArtwork(String path){
            if(path==null||!path.matches("(?:header-images|aurora-headers|indigo-headers|plum-headers)/header_[0-9]{2}\\.png"))return "";
            try(InputStream in=getAssets().open(path);ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>10000000)return "";out.write(buffer,0,n);}
                return "data:image/png;base64,"+android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP);
            }catch(Exception e){return "";}
        }

        @JavascriptInterface public String vaultStates(){return vault.candidates();}
        @JavascriptInterface public boolean vaultSaveState(String json){return vault.save(json);}
        @JavascriptInterface public String vaultStatus(){return vault.status();}
        @JavascriptInterface public boolean vaultBegin(){return vault.begin();}
        @JavascriptInterface public boolean vaultEntry(String name){return vault.entry(name);}
        @JavascriptInterface public boolean vaultChunk(String data){return vault.chunk(data);}
        @JavascriptInterface public boolean vaultEndEntry(){return vault.endEntry();}
        @JavascriptInterface public void vaultFinish(){new Thread(()->{String result=vault.finish();runOnUiThread(()->{if(web!=null)web.evaluateJavascript("window.vaultSnapshotDone&&window.vaultSnapshotDone("+JSONObject.quote(result)+")",null);});}).start();}
        @JavascriptInterface public void vaultAbort(){vault.abort();}
        @JavascriptInterface public String vaultRead(String name,long offset,int length){return vault.restoreChunk(name,offset,length);}
        @JavascriptInterface public void vaultCloseRestore(){vault.closeRestore();}
        @JavascriptInterface public void vaultExport(){runOnUiThread(()->{
            java.io.File[] snapshots=vault.snapshots();if(snapshots.length==0){message("Сначала создай защищённую копию");return;}
            android.widget.LinearLayout box=new android.widget.LinearLayout(PlannerActivity.this);box.setOrientation(android.widget.LinearLayout.VERTICAL);box.setPadding(36,12,36,12);
            android.widget.EditText first=new android.widget.EditText(PlannerActivity.this),second=new android.widget.EditText(PlannerActivity.this);first.setHint("Пароль — минимум 10 символов");second.setHint("Повтори пароль");first.setInputType(129);second.setInputType(129);box.addView(first);box.addView(second);
            AlertDialog dialog=new AlertDialog.Builder(PlannerActivity.this).setTitle("Пароль переносной копии").setMessage("Копия включает скрытые папки и все записи. Для открытия на новом телефоне нужен этот пароль. Сохрани его отдельно: без пароля восстановить копию нельзя. Приложение не отправляет файл в Интернет.").setView(box).setPositiveButton("Создать файл",null).setNegativeButton("Отмена",null).create();
            dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String value=first.getText().toString();if(value.length()<10||!value.equals(second.getText().toString())){second.setError("Пароли должны совпадать, минимум 10 символов");return;}char[] password=value.toCharArray();first.setText("");second.setText("");dialog.dismiss();new Thread(()->{try{java.io.File ready=vault.prepareExport(snapshots[0],password);runOnUiThread(()->{if(isFinishing()||isDestroyed()){ready.delete();return;}if(pendingVaultExport!=null)pendingVaultExport.delete();pendingVaultExport=ready;try{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/octet-stream");i.putExtra(Intent.EXTRA_LOCAL_ONLY,true);i.putExtra(Intent.EXTRA_TITLE,"black-deeds-backup-"+System.currentTimeMillis()+".bdvault");startActivityForResult(i,VAULT_EXPORT);}catch(Exception error){ready.delete();pendingVaultExport=null;message("Не удалось выбрать место сохранения");}});}catch(Exception error){runOnUiThread(()->message("Копия не создана: "+error.getMessage()));}}).start();}));dialog.show();dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        });}
        @JavascriptInterface public void vaultOpen(){runOnUiThread(()->{try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_LOCAL_ONLY,true);startActivityForResult(i,VAULT_IMPORT);}catch(Exception e){message("Не удалось открыть файл копии");}});}
        @JavascriptInterface public void vaultLocal(){runOnUiThread(()->{java.io.File[] files=vault.snapshots();if(files.length==0){message("Полных локальных копий пока нет");return;}String[] labels=new String[files.length];for(int n=0;n<files.length;n++)labels[n]=new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss",java.util.Locale.getDefault()).format(new java.util.Date(files[n].lastModified()));new AlertDialog.Builder(PlannerActivity.this).setTitle("Восстановить локальную копию").setItems(labels,(d,n)->new Thread(()->{try{stageVaultFile(files[n],null);}catch(Exception e){runOnUiThread(()->message("Копия не открыта: "+e.getMessage()));}}).start()).setNegativeButton("Отмена",null).show();});}

        @JavascriptInterface public void startupMetrics(String metrics){if(metrics!=null&&metrics.length()<1500)android.util.Log.i("PlannerStartup","DOM: "+metrics);}
        @JavascriptInterface public boolean friendSchedule(){return BuildConfig.FRIEND_SHIFT;}
        @JavascriptInterface public void workoutTimer(String token,long end,String title){if(token==null||token.length()>100||title==null||title.length()>100)return;workoutQueue.execute(()->WorkoutTimer.schedule(getApplicationContext(),token,end,title));}

        @JavascriptInterface public boolean copyText(String text){try{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(clipboard==null)return false;clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Текст заметки",text==null?"":text));return true;}catch(Exception e){return false;}}

        @JavascriptInterface public void hideKeyboard(){runOnUiThread(()->{if(web==null)return;android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(web.getWindowToken(),0);web.clearFocus();});}
        @JavascriptInterface public String notificationStatus(){return PlannerActivity.this.notificationStatus();}
        @JavascriptInterface public void requestNotifications(){runOnUiThread(()->configureNotifications());}
        @JavascriptInterface public boolean syncReminders(String json){return ReminderScheduler.sync(PlannerActivity.this,json);}
        @JavascriptInterface public void testNotification(){runOnUiThread(()->{if(!ReminderScheduler.notificationsAllowed(PlannerActivity.this)){configureNotifications();return;}ReminderScheduler.notify(PlannerActivity.this,"test","Уведомления работают","Напоминания о делах появятся здесь",java.time.LocalDate.now().toString());message("Проверочное уведомление отправлено");});}
        @JavascriptInterface public void statusBarColor(String color,boolean dark){runOnUiThread(()->{
            try{
                final Integer next=color==null||color.isEmpty()?null:Color.parseColor(color);
                final long update=++statusBarUpdate;
                if(pageReady&&web!=null){
                    final WebView current=web;
                    // Change system chrome alongside the committed destination page,
                    // rather than showing light chrome over the previous dark home frame.
                    current.postVisualStateCallback(update,new WebView.VisualStateCallback(){
                        @Override public void onComplete(long id){
                            if(update!=statusBarUpdate||web!=current||isFinishing()||isDestroyed())return;
                            statusBarOverride=next;statusBarDark=dark;applyStatusBarColor();
                        }
                    });
                }else{statusBarOverride=next;statusBarDark=dark;applyStatusBarColor();}
            }catch(IllegalArgumentException ignored){}
        });}
        @JavascriptInterface public void themeColors(String bg,boolean dark){runOnUiThread(()->{try{int color=Color.parseColor(bg);getSharedPreferences("appearance",MODE_PRIVATE).edit().putInt("background",color).putBoolean("dark",dark).apply();applyNativeTheme(color,dark);}catch(Exception e){}});}


        @JavascriptInterface public void saveBackup(String name,String json){
            if(json==null||json.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)return;
            runOnUiThread(()->{pendingBackup=json;try{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,name);startActivityForResult(i,SAVE);}catch(Exception e){pendingBackup=null;message("Не удалось открыть выбор файла");}});
        }
        @JavascriptInterface public void openLink(String url){openExternalLink(url);}
        @JavascriptInterface public void loadBackup(){runOnUiThread(()->{try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_LOCAL_ONLY,true);startActivityForResult(i,OPEN);}catch(Exception e){message("Не удалось открыть выбор файла");}});}
    }
    static Uri[] photoUris(int result,Intent data){
        if(result!=RESULT_OK||data==null)return null;
        java.util.LinkedHashSet<Uri> uris=new java.util.LinkedHashSet<>();
        android.content.ClipData clips=data.getClipData();
        if(clips!=null)for(int i=0;i<clips.getItemCount();i++){Uri uri=clips.getItemAt(i).getUri();if(uri!=null)uris.add(uri);}
        if(data.getData()!=null)uris.add(data.getData());
        return uris.isEmpty()?null:uris.toArray(new Uri[0]);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==103){android.webkit.ValueCallback<Uri[]> callback=photoCallback;photoCallback=null;if(callback!=null)callback.onReceiveValue(photoUris(result,data));return;}
        if(result!=RESULT_OK||data==null||data.getData()==null){if(request==SAVE)pendingBackup=null;if(request==VAULT_EXPORT&&pendingVaultExport!=null){pendingVaultExport.delete();pendingVaultExport=null;}return;}
        Uri uri=data.getData();
        if(request==VAULT_EXPORT){java.io.File ready=pendingVaultExport;pendingVaultExport=null;if(ready==null){message("Повтори создание переносной копии");return;}new Thread(()->{try{
            try(InputStream in=new java.io.FileInputStream(ready);OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new java.io.IOException("Файл недоступен");DataVault.copy(in,out);}
            String before=vaultDigest(new java.io.FileInputStream(ready)),after=vaultDigest(getContentResolver().openInputStream(uri));if(!before.equals(after))throw new java.io.IOException("Проверка копии не пройдена");runOnUiThread(()->message("Зашифрованная копия сохранена и проверена. Перенеси файл на новый телефон"));
        }catch(Exception e){runOnUiThread(()->message("Копия не сохранена полностью: "+e.getMessage()));}finally{ready.delete();}}).start();return;}
        if(request==VAULT_IMPORT){new Thread(()->{try{stageVault(()->getContentResolver().openInputStream(uri),null);}catch(Exception e){runOnUiThread(()->message("Копия не открыта: "+e.getMessage()));}}).start();return;}

        if(request==SAVE){String json=pendingBackup;pendingBackup=null;if(json==null){message("Повтори сохранение копии");return;}new Thread(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new Exception();out.write(json.getBytes(StandardCharsets.UTF_8));runOnUiThread(()->message("Копия сохранена"));}catch(Exception e){runOnUiThread(()->message("Не удалось сохранить копию"));}}).start();}
        if(request==OPEN)new Thread(()->{try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            if(in==null)throw new Exception();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>MAX_BYTES)throw new Exception();out.write(buffer,0,n);}String json=out.toString("UTF-8");String name="backup.json";
            try(android.database.Cursor cursor=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())name=cursor.getString(0);}
            String script="window.importAndroidBackup("+JSONObject.quote(name)+","+JSONObject.quote(json)+");";
            runOnUiThread(()->web.evaluateJavascript(script,null));
        }catch(Exception e){runOnUiThread(()->message("Не удалось прочитать копию"));}}).start();
    }
    private static String vaultDigest(InputStream source)throws Exception{if(source==null)throw new java.io.IOException("Файл недоступен");try(InputStream in=source){java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");byte[] bytes=new byte[65536];int n;while((n=in.read(bytes))!=-1)hash.update(bytes,0,n);return android.util.Base64.encodeToString(hash.digest(),android.util.Base64.NO_WRAP);}}
    private interface VaultInput { InputStream open()throws Exception; }
    private void stageVaultFile(java.io.File file,char[] password)throws Exception{stageVault(()->new java.io.FileInputStream(file),password);}
    private void stageVault(VaultInput source,char[] password)throws Exception{
        try{String manifest=vault.stage(source.open(),password);runOnUiThread(()->{if(web!=null)web.evaluateJavascript("window.restoreVaultArchive&&window.restoreVaultArchive("+JSONObject.quote(manifest)+")",null);});}
        catch(VaultCrypto.PasswordRequired e){runOnUiThread(()->{android.widget.EditText field=new android.widget.EditText(PlannerActivity.this);field.setInputType(129);field.setHint("Пароль этой копии");AlertDialog dialog=new AlertDialog.Builder(PlannerActivity.this).setTitle("Расшифровать копию").setView(field).setPositiveButton("Открыть",(d,w)->{char[] entered=field.getText().toString().toCharArray();field.setText("");new Thread(()->{try{stageVault(source,entered);}catch(Exception error){runOnUiThread(()->message("Копия не открыта: неверный пароль или повреждённый файл"));}}).start();}).setNegativeButton("Отмена",null).create();dialog.show();dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);});}
    }
    @Override public void onBackPressed(){if(web==null||!pageReady){moveTaskToBack(true);return;}web.evaluateJavascript("(function(){const d=document.querySelector('dialog[open]');if(d){d.close();return true;}if(window.handlePlannerBack&&window.handlePlannerBack())return true;return false;})()",closed->{if(!"true".equals(closed))moveTaskToBack(true);});}
    @Override protected void onDestroy(){if(photoCallback!=null){photoCallback.onReceiveValue(null);photoCallback=null;}if(web!=null){web.removeJavascriptInterface("AndroidFiles");web.destroy();web=null;}workoutQueue.shutdown();super.onDestroy();}
}
