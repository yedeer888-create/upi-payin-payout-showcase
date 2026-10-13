package com.tripartite.core.net.phonepe;


import com.tripartite.core.net.jdh5.HashingExample;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class AESKeyGenerator {

    public static void main(String[] args) throws Exception {

        String str = deviceFingerprint("8pLlW8Xp4XgsVRlA5VDfgoodt7MeyroFYE2Opf4mY", PhonePeHttpHelper.DEFAULT_AES_PUBLICKEY);
        System.out.println(str);
    }


    public static String deviceFingerprint(String deviceUnifierId,String publicKey) throws Exception {
        // 生成256位AES密钥
        byte[] deviceUnifierIdByte = deviceUnifierId.getBytes();
        SecretKey secretKey = generateAESKey(256);
       // byte[] sd=Base64.decode("PAdDUPJ9YemaYoH3kVxxAl73KOukjNSA5/PEVLrP4nY=",2);
        //secretKey=new SecretKeySpec(sd,"AES");
        //System.out.println(Base64.encodeToString(secretKey.getEncoded(), 2));
        String b11 = b(secretKey, Base64.encodeToString(deviceUnifierIdByte, 2).getBytes());
        String k3 = k(Base64.encodeToString(secretKey.getEncoded(), 2), publicKey);

        String num = Integer.toString(k3.length());
        return ("0000000000".substring(num.length()) + num) + k3 + b11;

    }

    public static String k(String str,String publicKey) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        try {
            StringBuilder sb2 = new StringBuilder();
            sb2.append(str);

            sb2.append(new String(Base64.decode(publicKey.getBytes(), 2)));
            String sb3 = sb2.toString();
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
            cipher.init(1, e(publicKey));
            String encodeToString = Base64.encodeToString(cipher.doFinal(str.getBytes()), 2);
            return encodeToString;
        } catch (Exception e11) {
            throw e11;
        }
    }

    public static PublicKey e(String publicKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
        try {

            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(publicKey.getBytes(), 2)));
        } catch (Exception e11) {
            throw e11;
        }
    }



    public static String b(SecretKey secretKey, byte[] bArr) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, NoSuchProviderException {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(Arrays.copyOf(secretKey.getEncoded(), 16));

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(1, secretKeySpec, ivParameterSpec);
            return Base64.encodeToString(cipher.doFinal(bArr), 2);
        } catch (Exception e11) {
           throw e11;
        }
    }


    public static SecretKey generateAESKey(int keySize)
            throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(keySize); // 支持128/192/256位
        return keyGen.generateKey();
    }

}
