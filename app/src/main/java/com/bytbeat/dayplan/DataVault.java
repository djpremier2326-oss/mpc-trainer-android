package com.bytbeat.dayplan;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.AtomicFile;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Independent of WebView storage. Failed writes never replace a completed backup. */
final class DataVault {
    private final Context context;
    private final File dir;
    final VaultCrypto crypto;
    private ZipOutputStream zip;
    private FileOutputStream zipFile;
    private File pending;
    private boolean entryOpen;
    private boolean finishing;
    private File restoreDir;
    private String error="";
    DataVault(Context context){this(context,new VaultCrypto(context));}
    DataVault(Context context,VaultCrypto crypto){this.context=context;this.crypto=crypto;dir=new File(context.getFilesDir(),"data-vault-v1");dir.mkdirs();}
    static boolean valid(String text){try{JSONObject j=new JSONObject(text);return j.optInt("version")==1&&j.opt("days") instanceof JSONObject;}catch(Exception e){return false;}}
    private String read(File file)throws IOException{try{ByteArrayOutputStream out=new ByteArrayOutputStream();crypto.open(new ByteArrayInputStream(new AtomicFile(file).readFully()),out,null);return out.toString("UTF-8");}catch(Exception e){throw new IOException("Не прочитана защищённая запись",e);}}
    private void atomic(File file,String value)throws IOException{
        AtomicFile atomic=new AtomicFile(file);FileOutputStream out=null;
        try{out=atomic.startWrite();crypto.seal(new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)),out,false);atomic.finishWrite(out);if(!value.equals(read(file)))throw new IOException("Запись не прошла проверку");}catch(Exception e){atomic.failWrite(out);throw new IOException("Не сохранена защищённая запись",e);}
    }
    synchronized String candidates(){JSONArray a=new JSONArray();for(String name:new String[]{"state.json","previous.json"})try{String s=read(new File(dir,name));a.put(valid(s)?s:JSONObject.NULL);}catch(Exception ignored){a.put(JSONObject.NULL);}return a.toString();}
    synchronized boolean save(String value){
        if(!valid(value)){error="Запись не прошла проверку";return false;}
        try{
            File current=new File(dir,"state.json");String old=null;try{old=read(current);}catch(IOException ignored){}
            if(value.equals(old))return true;
            if(old!=null&&valid(old)){
                atomic(new File(dir,"previous.json"),old);
                // Keep an immutable checkpoint of the first state in each hour.
                File history=new File(dir,"history");history.mkdirs();File checkpoint=new File(history,(System.currentTimeMillis()/3600000)+".json");
                if(!checkpoint.exists())atomic(checkpoint,old);
            }
            atomic(current,value);error="";return true;
        }catch(Exception e){error="Не удалось сохранить независимую копию: "+e.getMessage();return false;}
    }
    synchronized boolean begin(){
        if(zip!=null||finishing)return false;
        try{pending=new File(dir,"pending-"+UUID.randomUUID()+".zip");zipFile=new FileOutputStream(pending);zip=new ZipOutputStream(zipFile);entryOpen=false;return true;}catch(Exception e){abort();error=e.getMessage();return false;}
    }
    private static boolean entryName(String name){return "manifest.json".equals(name)||name.matches("blobs/[0-9]+\\.bin");}
    synchronized boolean entry(String name){try{if(zip==null||entryOpen||!entryName(name))return false;zip.putNextEntry(new ZipEntry(name));entryOpen=true;return true;}catch(Exception e){error=e.getMessage();return false;}}
    synchronized boolean chunk(String base64){try{if(zip==null||!entryOpen||base64==null||base64.length()>400000)return false;zip.write(Base64.decode(base64,Base64.NO_WRAP));return true;}catch(Exception e){error=e.getMessage();return false;}}
    synchronized boolean endEntry(){try{if(zip==null||!entryOpen)return false;zip.closeEntry();entryOpen=false;return true;}catch(Exception e){error=e.getMessage();return false;}}
    synchronized void abort(){try{if(zip!=null)zip.close();}catch(Exception ignored){}zip=null;zipFile=null;if(pending!=null)pending.delete();pending=null;entryOpen=false;}
    String finish(){
        File complete;String folder,cloudA,cloudB;int nextCloud;
        try{
          synchronized(this){
            finishing=true;
            if(zip==null||entryOpen)throw new IOException("Незавершённая копия");
            zip.finish();zip.flush();zipFile.getFD().sync();zip.close();zip=null;zipFile=null;
            verify(pending);
            complete=new File(dir,"black-deeds-"+System.currentTimeMillis()+"-"+UUID.randomUUID()+".bdvault");
            File sealedFile=new File(complete.getPath()+".partial");
            try{
                try(InputStream input=new FileInputStream(pending);FileOutputStream sealed=new FileOutputStream(sealedFile)){crypto.seal(input,sealed,crypto.configured());sealed.getFD().sync();}
                try(InputStream check=new FileInputStream(sealedFile)){crypto.open(check,new OutputStream(){@Override public void write(int b){}@Override public void write(byte[] b,int start,int count){}},null);}
                if(!sealedFile.renameTo(complete))throw new IOException("Не удалось завершить защищённую копию");
            }catch(Exception e){sealedFile.delete();throw e;}
            pending.delete();pending=null;pruneLocalSnapshots();
            context.getSharedPreferences("vault",0).edit().putLong("localAt",System.currentTimeMillis()).commit();
            folder=context.getSharedPreferences("vault",0).getString("folder",null);cloudA=context.getSharedPreferences("vault",0).getString("cloudA",null);cloudB=context.getSharedPreferences("vault",0).getString("cloudB",null);nextCloud=context.getSharedPreferences("vault",0).getInt("nextCloud",0);
          }
            if(folder!=null||(cloudA!=null&&cloudB!=null)){if(!crypto.configured())throw new IOException("Задай пароль облачной копии");try(InputStream header=new FileInputStream(complete)){byte[] prefix=new byte[5];if(header.read(prefix)!=5||prefix[4]!=2)throw new IOException("Облачная копия ожидает шифрования паролем");}if(folder!=null)publish(complete,Uri.parse(folder));else{publishToFile(complete,Uri.parse(nextCloud==0?cloudA:cloudB));context.getSharedPreferences("vault",0).edit().putInt("nextCloud",1-nextCloud).putLong("externalAt",System.currentTimeMillis()).commit();}}
            synchronized(this){error="";return status();}
        }catch(Exception e){synchronized(this){abort();error="Копия не завершена: "+e.getMessage();return status();}}finally{synchronized(this){finishing=false;}}
    }
    private void verify(File file)throws IOException{
        boolean manifest=false;try(ZipFile archive=new ZipFile(file)){
            Set<String> names=new HashSet<>();Enumeration<? extends ZipEntry> entries=archive.entries();byte[] bytes=new byte[65536];
            while(entries.hasMoreElements()){
                ZipEntry e=entries.nextElement();if(!entryName(e.getName())||!names.add(e.getName()))throw new IOException("Неверная структура копии");
                CRC32 crc=new CRC32();long size=0;try(InputStream in=archive.getInputStream(e)){int n;while((n=in.read(bytes))!=-1){crc.update(bytes,0,n);size+=n;}}
                if(size!=e.getSize()||crc.getValue()!=e.getCrc())throw new IOException("Повреждён файл копии");
                if(e.getName().equals("manifest.json"))manifest=true;
            }
        }if(!manifest)throw new IOException("В копии нет описания");
    }
    private void publish(File file,Uri tree)throws IOException{
        Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));Uri created=null;
        try{
            created=DocumentsContract.createDocument(context.getContentResolver(),parent,"application/octet-stream",file.getName()+".partial");if(created==null)throw new IOException("Папка недоступна");
            try(InputStream in=new FileInputStream(file);OutputStream out=context.getContentResolver().openOutputStream(created,"wt")){if(out==null)throw new IOException("Папка недоступна");copy(in,out);}
            // Re-read the provider's bytes before reporting an external backup as saved.
            String before=digest(new FileInputStream(file)),after=digest(context.getContentResolver().openInputStream(created));
            if(!before.equals(after))throw new IOException("Проверка внешней копии не пройдена");
            if(DocumentsContract.renameDocument(context.getContentResolver(),created,file.getName())==null)throw new IOException("Не удалось завершить облачную копию");
            context.getSharedPreferences("vault",0).edit().putLong("externalAt",System.currentTimeMillis()).commit();
        }catch(Exception e){if(created!=null)try{DocumentsContract.deleteDocument(context.getContentResolver(),created);}catch(Exception ignored){}throw new IOException("Внешняя копия не сохранена. Локальная сохранена. "+e.getMessage(),e);}
    }
    private void publishToFile(File file,Uri destination)throws Exception{
        try(InputStream in=new FileInputStream(file);OutputStream out=context.getContentResolver().openOutputStream(destination,"wt")){if(out==null)throw new IOException("Облачный файл недоступен");copy(in,out);}
        if(!digest(new FileInputStream(file)).equals(digest(context.getContentResolver().openInputStream(destination))))throw new IOException("Проверка облачного файла не пройдена; другая копия сохранена");
    }
    private static String digest(InputStream stream)throws Exception{if(stream==null)throw new IOException("Файл недоступен");try(InputStream in=stream){java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)hash.update(b,0,n);return Base64.encodeToString(hash.digest(),Base64.NO_WRAP);}}
    static void copy(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
    synchronized String status(){try{return new JSONObject().put("folder",context.getSharedPreferences("vault",0).contains("folder")||(context.getSharedPreferences("vault",0).contains("cloudA")&&context.getSharedPreferences("vault",0).contains("cloudB"))).put("files",context.getSharedPreferences("vault",0).contains("cloudA")&&context.getSharedPreferences("vault",0).contains("cloudB")).put("localAt",context.getSharedPreferences("vault",0).getLong("localAt",0)).put("externalAt",context.getSharedPreferences("vault",0).getLong("externalAt",0)).put("password",crypto.configured()).put("error",error).toString();}catch(Exception e){return "{}";}}
    private void pruneLocalSnapshots(){File[] files=snapshots();Set<String> days=new HashSet<>();long earliest=System.currentTimeMillis()-7L*86400000L;for(int i=0;i<files.length;i++){String day=new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.ROOT).format(new Date(files[i].lastModified()));boolean daily=files[i].lastModified()>=earliest&&days.add(day);if(i>=3&&!daily)files[i].delete();}}
    synchronized File[] snapshots(){File[] files=dir.listFiles((d,n)->n.startsWith("black-deeds-")&&n.endsWith(".bdvault"));if(files==null)return new File[0];Arrays.sort(files,(a,b)->Long.compare(b.lastModified(),a.lastModified()));return files;}
    synchronized String stage(InputStream source)throws Exception{return stage(source,null);}
    synchronized String stage(InputStream source,char[] password)throws Exception{
        File archive=new File(dir,"import-"+UUID.randomUUID()+".zip");
        try{
            try(InputStream in=source;OutputStream out=new FileOutputStream(archive)){if(in==null)throw new IOException("Нет файла");crypto.open(in,out,password);}verify(archive);
            File next=new File(dir,"restore-"+UUID.randomUUID());if(!next.mkdirs())throw new IOException("Нет места для восстановления");
            try(ZipFile z=new ZipFile(archive)){
                Enumeration<? extends ZipEntry> entries=z.entries();while(entries.hasMoreElements()){
                    ZipEntry e=entries.nextElement();File target=new File(next,e.getName());target.getParentFile().mkdirs();try(InputStream in=z.getInputStream(e);OutputStream out=new FileOutputStream(target)){copy(in,out);}
                }
            }catch(Exception e){deleteTree(next);throw e;}
            if(new File(next,"manifest.json").length()>50000000){deleteTree(next);throw new IOException("Описание копии слишком велико");}
            String manifest=new String(java.nio.file.Files.readAllBytes(new File(next,"manifest.json").toPath()),StandardCharsets.UTF_8);
            try{JSONObject j=new JSONObject(manifest);if(!"black-deeds-vault-1".equals(j.optString("format"))||!valid(j.getJSONObject("state").toString()))throw new IOException("Копия другого приложения");}catch(Exception e){deleteTree(next);throw e;}
            if(restoreDir!=null)deleteTree(restoreDir);restoreDir=next;return manifest;
        }finally{archive.delete();}
    }
    synchronized String restoreChunk(String name,long offset,int length){try{if(restoreDir==null||!entryName(name)||name.equals("manifest.json")||offset<0||length<1||length>196608)return "!";try(RandomAccessFile in=new RandomAccessFile(new File(restoreDir,name),"r")){in.seek(offset);byte[] bytes=new byte[length];int n=in.read(bytes);return n<0?"":Base64.encodeToString(Arrays.copyOf(bytes,n),Base64.NO_WRAP);}}catch(Exception e){return "!";}}
    synchronized void closeRestore(){if(restoreDir!=null)deleteTree(restoreDir);restoreDir=null;}
    private static void deleteTree(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)deleteTree(child);file.delete();}
}
