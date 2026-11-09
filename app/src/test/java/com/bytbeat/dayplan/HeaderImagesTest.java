package com.bytbeat.dayplan;
import android.app.Application;
import android.util.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
public class HeaderImagesTest {
 public static class WithoutWeb extends PlannerActivity {@Override protected void startWebEngine(){}}
 @Test public void packagedHeadersAreAvailableAsOfflineImageData()throws Exception{
  var controller=Robolectric.buildActivity(WithoutWeb.class).create();WithoutWeb activity=controller.get();var bridge=activity.new BackupBridge();
  for(int i=1;i<=10;i++){String name=String.format(java.util.Locale.ROOT,"header-images/header_%02d.png",i),image=bridge.headerArtwork(name);assertTrue(name,image.startsWith("data:image/png;base64,"));byte[] actual=Base64.decode(image.substring(image.indexOf(',')+1),Base64.NO_WRAP);try(var in=activity.getAssets().open(name)){assertArrayEquals(in.readAllBytes(),actual);}assertEquals(0x89,actual[0]&255);assertEquals('P',actual[1]);}
  assertEquals("",bridge.headerArtwork("../state.json"));controller.destroy();
 }
}
