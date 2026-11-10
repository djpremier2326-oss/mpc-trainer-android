package com.bytbeat.dayplan;
import android.app.Application;
import android.content.Context;
import android.util.Base64;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import org.json.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
public class DataVaultTest {
 private Context context;private VaultCrypto crypto;private DataVault vault;
 private final String one="{\"version\":1,\"days\":{},\"secret\":\"PRIVATE-NOTE-DO-NOT-EXPOSE\"}";
 @Before public void setup(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences("vault",0).edit().clear().commit();context.getSharedPreferences("vault-crypto",0).edit().clear().commit();delete(new File(context.getFilesDir(),"data-vault-v1"));crypto=new VaultCrypto(context,new SecretKeySpec(new byte[32],"AES"));vault=new DataVault(context,crypto);}
 private static void delete(File f){File[] children=f.listFiles();if(children!=null)for(File x:children)delete(x);f.delete();}
 @Test public void staleScreenCannotOverwriteNewFavoriteCards()throws Exception{
  assertTrue(vault.compareSave(null,one));String newer=one.replace("PRIVATE-NOTE","NEW-PHOTO");
  assertTrue(vault.compareSave(one,newer));assertFalse(vault.compareSave(one,one));
  assertEquals(newer,new JSONArray(vault.candidates()).getString(0));
  assertTrue(vault.compareSave(newer,newer));
 }
 @Test public void appHasNoInternetPermission()throws Exception{String[] permissions=context.getPackageManager().getPackageInfo(context.getPackageName(),android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions;if(permissions!=null)assertFalse(Arrays.asList(permissions).contains("android.permission.INTERNET"));}
 @Test public void mirrorIsEncryptedAndSurvivesInvalidNextWrite()throws Exception{
  assertTrue(vault.save(one));assertFalse(new String(Files.readAllBytes(new File(context.getFilesDir(),"data-vault-v1/state.json").toPath()),StandardCharsets.ISO_8859_1).contains("PRIVATE-NOTE"));
  assertFalse(vault.save("{broken"));assertEquals(one,new JSONArray(vault.candidates()).getString(0));
 }
 @Test public void corruptNewestMirrorFallsBackWithoutDeletingCorruptBytes()throws Exception{
  assertTrue(vault.save(one));String two=one.replace("PRIVATE-NOTE","SECOND-NOTE");assertTrue(vault.save(two));File file=new File(context.getFilesDir(),"data-vault-v1/state.json");byte[] damaged={1,2,3};Files.write(file.toPath(),damaged);
  JSONArray candidates=new JSONArray(vault.candidates());assertTrue(candidates.isNull(0));assertEquals(one,candidates.getString(1));assertArrayEquals(damaged,Files.readAllBytes(file.toPath()));
 }
 @Test public void fullBackupIsEncryptedAndStreamsLargeMediaRoundTrip()throws Exception{
  byte[] bytes=new byte[600123];new java.util.Random(42).nextBytes(bytes);
  assertTrue(vault.begin());assertTrue(vault.entry("blobs/0.bin"));for(int at=0;at<bytes.length;at+=196608)assertTrue(vault.chunk(Base64.encodeToString(Arrays.copyOfRange(bytes,at,Math.min(bytes.length,at+196608)),Base64.NO_WRAP)));assertTrue(vault.endEntry());
  String manifest=new JSONObject().put("format","black-deeds-vault-1").put("state",new JSONObject(one)).put("media",new JSONArray()).put("wishes",new JSONArray()).toString();assertTrue(vault.entry("manifest.json"));assertTrue(vault.chunk(Base64.encodeToString(manifest.getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP)));assertTrue(vault.endEntry());assertEquals("",new JSONObject(vault.finish()).getString("error"));
  File complete=vault.snapshots()[0];assertFalse(new String(Files.readAllBytes(complete.toPath()),StandardCharsets.ISO_8859_1).contains("PRIVATE-NOTE"));assertEquals(manifest,vault.stage(new FileInputStream(complete)));ByteArrayOutputStream restored=new ByteArrayOutputStream();for(int at=0;at<bytes.length;at+=196608)restored.write(Base64.decode(vault.restoreChunk("blobs/0.bin",at,Math.min(bytes.length-at,196608)),Base64.NO_WRAP));assertArrayEquals(bytes,restored.toByteArray());vault.closeRestore();assertEquals("!",vault.restoreChunk("blobs/0.bin",0,20));
  File portable=vault.prepareExport(complete,"manual transfer password".toCharArray());byte[] otherKey=new byte[32];otherKey[0]=1;DataVault nextPhone=new DataVault(context,new VaultCrypto(context,new SecretKeySpec(otherKey,"AES")));assertEquals(manifest,nextPhone.stage(new FileInputStream(portable),"manual transfer password".toCharArray()));assertArrayEquals(Arrays.copyOfRange(bytes,0,196608),Base64.decode(nextPhone.restoreChunk("blobs/0.bin",0,196608),Base64.NO_WRAP));nextPhone.closeRestore();portable.delete();
 }
 @Test public void unfinishedOrUnsafeArchiveIsNeverPublished()throws Exception{assertTrue(vault.begin());assertFalse(vault.entry("../secret"));vault.abort();assertEquals(0,vault.snapshots().length);assertTrue(vault.begin());assertTrue(vault.entry("blobs/0.bin"));assertTrue(vault.endEntry());assertFalse(new JSONObject(vault.finish()).getString("error").isEmpty());assertEquals(0,vault.snapshots().length);}
 @Test public void manuallyExportedBackupOpensWithPasswordOnAnotherPhone()throws Exception{
  ByteArrayOutputStream sealed=new ByteArrayOutputStream();crypto.sealPortable(new ByteArrayInputStream(one.getBytes(StandardCharsets.UTF_8)),sealed,"correct horse battery".toCharArray());
  byte[] different=new byte[32];different[0]=1;VaultCrypto another=new VaultCrypto(context,new SecretKeySpec(different,"AES"));ByteArrayOutputStream opened=new ByteArrayOutputStream();another.open(new ByteArrayInputStream(sealed.toByteArray()),opened,"correct horse battery".toCharArray());assertEquals(one,opened.toString("UTF-8"));
  assertThrows(VaultCrypto.PasswordRequired.class,()->another.open(new ByteArrayInputStream(sealed.toByteArray()),new ByteArrayOutputStream(),null));assertThrows(Exception.class,()->another.open(new ByteArrayInputStream(sealed.toByteArray()),new ByteArrayOutputStream(),"wrong password".toCharArray()));
 }
 @Test public void modifiedOrTruncatedCiphertextCannotBecomeAValidBackup()throws Exception{
  ByteArrayOutputStream sealed=new ByteArrayOutputStream();crypto.seal(new ByteArrayInputStream(one.getBytes(StandardCharsets.UTF_8)),sealed,false);byte[] bytes=sealed.toByteArray();bytes[bytes.length-3]^=1;assertThrows(Exception.class,()->crypto.open(new ByteArrayInputStream(bytes),new ByteArrayOutputStream(),null));byte[] truncated=Arrays.copyOf(sealed.toByteArray(),sealed.size()-32);assertThrows(Exception.class,()->crypto.open(new ByteArrayInputStream(truncated),new ByteArrayOutputStream(),null));
 }

}
