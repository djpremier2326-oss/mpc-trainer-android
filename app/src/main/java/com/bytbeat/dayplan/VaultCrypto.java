package com.bytbeat.dayplan;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.io.*;
import java.security.*;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.*;

/** AES-GCM authenticates all backup bytes; portable keys use PBKDF2-HMAC-SHA256. */
final class VaultCrypto {
    private static final byte[] MAGIC={'B','D','V','1'};
    private static final int ITERATIONS=310000;
    private final Context context;
    private final SecretKey testKey;
    VaultCrypto(Context context){this(context,null);}
    VaultCrypto(Context context,SecretKey testKey){this.context=context;this.testKey=testKey;}
    private SecretKey deviceKey()throws Exception{
        if(testKey!=null)return testKey;
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        String alias="black-deeds-private-vault-v1";
        if(store.containsAlias(alias))return (SecretKey)store.getKey(alias,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
        return generator.generateKey();
    }
    boolean configured(){return context.getSharedPreferences("vault-crypto",0).contains("wrapped");}
    private static SecretKey derive(char[] password,byte[] salt)throws Exception{
        PBEKeySpec spec=new PBEKeySpec(password,salt,ITERATIONS,256);
        try{return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(),"AES");}finally{spec.clearPassword();}
    }
    synchronized void configure(char[] password)throws Exception{
        try{
            byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);SecretKey key=derive(password,salt);
            ByteArrayOutputStream wrapped=new ByteArrayOutputStream();seal(new ByteArrayInputStream(key.getEncoded()),wrapped,false);
            if(!context.getSharedPreferences("vault-crypto",0).edit().putString("salt",Base64.encodeToString(salt,Base64.NO_WRAP)).putString("wrapped",Base64.encodeToString(wrapped.toByteArray(),Base64.NO_WRAP)).commit())throw new IOException("Ключ не сохранён");
        }finally{Arrays.fill(password,'\0');}
    }
    private byte[] salt(){return Base64.decode(context.getSharedPreferences("vault-crypto",0).getString("salt",""),Base64.NO_WRAP);}
    private SecretKey portableKey()throws Exception{
        byte[] bytes=Base64.decode(context.getSharedPreferences("vault-crypto",0).getString("wrapped",""),Base64.NO_WRAP);
        ByteArrayOutputStream raw=new ByteArrayOutputStream();open(new ByteArrayInputStream(bytes),raw,null);
        return new SecretKeySpec(raw.toByteArray(),"AES");
    }
    synchronized void seal(InputStream in,OutputStream out,boolean portable)throws Exception{
        byte[] salt=portable?salt():new byte[16];if(salt.length!=16)throw new IOException("Неверный ключ копий");
        SecretKey key=portable?portableKey():deviceKey();byte[] header=new byte[21];System.arraycopy(MAGIC,0,header,0,4);header[4]=(byte)(portable?2:1);System.arraycopy(salt,0,header,5,16);out.write(header);
        DataOutputStream target=new DataOutputStream(out);byte[] block=new byte[196608];long index=0;
        while(true){int n=0;while(n<block.length){int got=in.read(block,n,block.length-n);if(got<0)break;n+=got;}
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key);byte[] iv=cipher.getIV();if(iv.length!=12)throw new IOException("Неверный вектор шифрования");
            authenticate(cipher,header,index++,n);target.writeInt(n);target.write(iv);target.write(cipher.doFinal(block,0,n));if(n==0)break;
        }
    }
    synchronized void open(InputStream in,OutputStream out,char[] password)throws Exception{
        try{
            DataInputStream source=new DataInputStream(in);byte[] header=new byte[21];source.readFully(header);
            if(!Arrays.equals(Arrays.copyOf(header,4),MAGIC))throw new IOException("Нужна зашифрованная копия «Чёрных дел»");
            int mode=header[4];byte[] salt=Arrays.copyOfRange(header,5,21);SecretKey key;
            if(mode==1)key=deviceKey();else if(mode==2){
                if(password!=null)key=derive(password,salt);
                else if(configured()&&Arrays.equals(salt,salt()))key=portableKey();
                else throw new PasswordRequired();
            }else throw new IOException("Неподдерживаемая версия шифрования");
            long index=0;while(true){int n=source.readInt();if(n<0||n>196608)throw new IOException("Повреждён блок копии");byte[] iv=new byte[12],encrypted=new byte[n+16];source.readFully(iv);source.readFully(encrypted);
                Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,iv));authenticate(cipher,header,index++,n);out.write(cipher.doFinal(encrypted));if(n==0){if(source.read()!=-1)throw new IOException("Лишние данные в копии");break;}
            }
        }finally{if(password!=null)Arrays.fill(password,'\0');}
    }
    private static void authenticate(Cipher cipher,byte[] header,long index,int length){cipher.updateAAD(header);cipher.updateAAD(java.nio.ByteBuffer.allocate(12).putLong(index).putInt(length).array());}
    static final class PasswordRequired extends Exception {}
}
