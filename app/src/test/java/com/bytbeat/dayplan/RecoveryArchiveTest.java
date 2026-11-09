package com.bytbeat.dayplan;
import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.nio.file.*;
import java.io.*;
import java.util.zip.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
public class RecoveryArchiveTest {
 @Test public void archiveIncludesOriginalBytesWithoutChangingOrDeletingFiles()throws Exception{
  Path base=Files.createTempDirectory("planner-recovery").toRealPath(),dir=Files.createDirectories(base.resolve("app_webview/Default/Local Storage/leveldb")),data=dir.resolve("000003.log");byte[] original="old note and schedule bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);Files.write(data,original);
  PlannerActivity activity=org.robolectric.Robolectric.buildActivity(PlannerActivity.class).get();var method=PlannerActivity.class.getDeclaredMethod("archiveRecoveryTree",ZipOutputStream.class,File.class,File.class);method.setAccessible(true);ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(out)){method.invoke(activity,zip,base.toFile(),base.resolve("app_webview").toFile());}
  assertArrayEquals(original,Files.readAllBytes(data));try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))){assertEquals("app_webview/Default/Local Storage/leveldb/000003.log",zip.getNextEntry().getName());assertArrayEquals(original,zip.readAllBytes());assertNull(zip.getNextEntry());}
  Files.delete(data);Files.delete(dir);Files.delete(dir.getParent());Files.delete(dir.getParent().getParent());Files.delete(base.resolve("app_webview"));Files.delete(base);
 }
}
