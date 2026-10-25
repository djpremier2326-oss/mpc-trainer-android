package com.bytbeat.dayplan;

import android.app.Activity;
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
    private static final int SAVE=101, OPEN=102, MAX_BYTES=20_000_000;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        web=new WebView(this);
        web.setBackgroundColor(0xffdde5f6);
        setContentView(web);
        WebSettings settings=web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){return !request.getUrl().toString().startsWith("file:///android_asset/");}
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onJsConfirm(WebView v,String url,String message,JsResult result){new AlertDialog.Builder(PlannerActivity.this).setMessage(message).setPositiveButton("Да",(d,w)->result.confirm()).setNegativeButton("Отмена",(d,w)->result.cancel()).setOnCancelListener(d->result.cancel()).show();return true;}
            @Override public boolean onJsAlert(WebView v,String url,String message,JsResult result){new AlertDialog.Builder(PlannerActivity.this).setMessage(message).setPositiveButton("ОК",(d,w)->result.confirm()).setOnCancelListener(d->result.confirm()).show();return true;}
        });
        web.addJavascriptInterface(new BackupBridge(),"AndroidFiles");
        web.loadUrl("file:///android_asset/index.html");
    }
    private void message(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    public class BackupBridge {
        @JavascriptInterface public void saveBackup(String name,String json){
            if(json==null||json.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)return;
            runOnUiThread(()->{pendingBackup=json;try{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,name);startActivityForResult(i,SAVE);}catch(Exception e){pendingBackup=null;message("Не удалось открыть выбор файла");}});
        }
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
    @Override public void onBackPressed(){web.evaluateJavascript("(function(){const d=document.querySelector('dialog[open]');if(d){d.close();return true;}return false;})()",closed->{if(!"true".equals(closed))super.onBackPressed();});}
    @Override protected void onDestroy(){if(web!=null){web.removeJavascriptInterface("AndroidFiles");web.destroy();}super.onDestroy();}
}
