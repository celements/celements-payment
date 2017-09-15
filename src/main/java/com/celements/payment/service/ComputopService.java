package com.celements.payment.service;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;

import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;

@Component
public class ComputopService implements ComputopServiceRole {

  private static Logger LOGGER = LoggerFactory.getLogger(ComputopService.class);

  private static final String BLOWFISH = "Blowfish";
  private static final String BLOWFISH_ECB = BLOWFISH + "/ECB/PKCS5Padding";

  public static final String FORM_INPUT_NAME_LENGTH = "Len";
  public static final String FORM_INPUT_NAME_DATA = "Data";

  @Override
  public Map<String, String> encryptPaymentData(String paymentData) {
    Preconditions.checkNotNull(paymentData);
    Map<String, String> encryptedData = new HashMap<>();
    encryptedData.put(FORM_INPUT_NAME_LENGTH, Integer.toString(paymentData.length()));
    String encyrptedData = encryptString(paymentData.getBytes(), getKey());
    if (encyrptedData != null) {
      encryptedData.put(FORM_INPUT_NAME_DATA, encyrptedData);
    }
    return encryptedData;
  }

  @Override
  public Map<String, String> decryptCallbackData(String encryptedCallback, int plainDataLength) {
    Preconditions.checkNotNull(encryptedCallback);
    byte[] decryptedData = decryptString(encryptedCallback, plainDataLength, getKey());
    Map<String, String> callbackData = new HashMap<>();
    if (decryptedData != null) {
      for (String parameter : (new String(decryptedData)).split("&")) {
        String[] keyValue = parameter.split("=", 2);
        String key = keyValue[0].toLowerCase();
        String value = "";
        if (keyValue.length > 1) {
          value = keyValue[1];
        }
        callbackData.put(key, value);
      }
    }
    return callbackData;
  }

  String encryptString(byte[] plainText, SecretKey key) {
    try {
      return BaseEncoding.base16().encode(getCipher(Cipher.ENCRYPT_MODE, key).doFinal(plainText));
    } catch (IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException excp) {
      LOGGER.error("Problem while encrypting payment data", excp);
    }
    return null;
  }

  byte[] decryptString(String encryptedBase16, int plainTextLength, SecretKey key) {
    try {
      byte[] decodedCipher = BaseEncoding.base16().decode(encryptedBase16.toUpperCase());
      System.out.println("cipher [" + encryptedBase16 + "] length: [" + decodedCipher.length
          + "] plain [" + plainTextLength + "]");
      Cipher cipher = getCipher(Cipher.DECRYPT_MODE, key);
      return cipher.doFinal(decodedCipher);
    } catch (IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException excp) {
      LOGGER.error("Problem while decrypting Computop callback", excp);
    }
    return null;
  }

  Cipher getCipher(int cipherMode, SecretKey key) throws NoSuchPaddingException {
    try {
      Cipher cipher = Cipher.getInstance(BLOWFISH_ECB);
      cipher.init(cipherMode, key);
      return cipher;
    } catch (NoSuchAlgorithmException e) {
      LOGGER.error("{} algorithm not availabe", BLOWFISH_ECB);
    } catch (InvalidKeyException e) {
      LOGGER.error("SecretKey invalid for {} encryption", BLOWFISH_ECB);
    }
    return null;
  }

  SecretKey getKey() {
    return new SecretKeySpec("1234567890123456".getBytes(), BLOWFISH);
  }

}
