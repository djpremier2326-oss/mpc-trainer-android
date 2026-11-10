package com.bytbeat.dayplan;
import java.io.*;
import android.util.Base64;
/** A bounded, private staging file, used only after an explicit export gesture. */
final class PhotoExportSession {
 private final File cache;private File file;private FileOutputStream output;private long bytes;private String name,mime;
 PhotoExportSession(File cache){this.cache=cache;}
 synchronized boolean begin(String name,String mime){
  if(file!=null||name==null||mime==null||!mime.matches("image/(jpeg|png|webp|gif|heic|heif|avif|bmp)"))return false;
  try{this.name=name.replaceAll("[^\\p{L}\\p{N}._ -]","_");this.mime=mime;file=File.createTempFile("photo-export-",".tmp",cache);output=new FileOutputStream(file);bytes=0;return true;}catch(IOException e){clear();return false;}
 }
 synchronized boolean chunk(String data){
  if(output==null||data==null||data.length()>262144)return false;
  try{byte[] part=Base64.decode(data,Base64.NO_WRAP);if(part.length>196608||bytes+part.length>100_000_000)return false;output.write(part);bytes+=part.length;return true;}catch(Exception e){return false;}
 }
 synchronized boolean finish(){if(output==null||bytes==0)return false;try{output.getFD().sync();output.close();output=null;return true;}catch(IOException e){clear();return false;}}
 synchronized File file(){return output==null?file:null;}
 synchronized String name(){return name;}
 synchronized String mime(){return mime;}
 synchronized void clear(){try{if(output!=null)output.close();}catch(IOException ignored){}output=null;if(file!=null)file.delete();file=null;bytes=0;}
}
