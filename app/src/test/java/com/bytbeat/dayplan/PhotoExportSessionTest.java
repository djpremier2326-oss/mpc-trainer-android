package com.bytbeat.dayplan;
import android.app.Application;
import android.util.Base64;
import java.nio.file.Files;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=Application.class)
public class PhotoExportSessionTest {
 @Test public void streamedExportPreservesBytesAndCancellationRemovesOnlyStagingFile()throws Exception{
  PhotoExportSession session=new PhotoExportSession(RuntimeEnvironment.getApplication().getCacheDir());
  assertTrue(session.begin("photo.png","image/png"));assertFalse(session.begin("second.png","image/png"));
  byte[] one=new byte[196608],two=new byte[23];new java.util.Random(12).nextBytes(one);new java.util.Random(13).nextBytes(two);
  assertTrue(session.chunk(Base64.encodeToString(one,Base64.NO_WRAP)));assertTrue(session.chunk(Base64.encodeToString(two,Base64.NO_WRAP)));assertTrue(session.finish());
  java.io.File staged=session.file();byte[] expected=new byte[one.length+two.length];System.arraycopy(one,0,expected,0,one.length);System.arraycopy(two,0,expected,one.length,two.length);assertArrayEquals(expected,Files.readAllBytes(staged.toPath()));
  session.clear();assertFalse(staged.exists());assertTrue(session.begin("next.jpg","image/jpeg"));session.clear();
 }
 @Test public void nonImagesEmptyAndOversizedChunksAreRejected(){
  PhotoExportSession s=new PhotoExportSession(RuntimeEnvironment.getApplication().getCacheDir());assertFalse(s.begin("bad.html","text/html"));assertTrue(s.begin("safe.png","image/png"));assertFalse(s.finish());assertFalse(s.chunk("A".repeat(262145)));s.clear();assertNull(s.file());
 }
}
