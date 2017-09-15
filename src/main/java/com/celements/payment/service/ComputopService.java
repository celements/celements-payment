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

@Component
public class ComputopService implements ComputopServiceRole {

  private static Logger LOGGER = LoggerFactory.getLogger(ComputopService.class);

  private static final char hexTab[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B',
      'C', 'D', 'E', 'F' };
  private static final String BLOWFISH = "Blowfish";

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
      return hexEncode(getCipher(Cipher.ENCRYPT_MODE, key).doFinal(plainText));
    } catch (IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException excp) {
      LOGGER.error("Problem while encrypting payment data", excp);
    }
    return null;
  }

  String hexEncode(byte[] cipherText) {
    int textLength = cipherText.length;
    StringBuffer encodedText = new StringBuffer(textLength * 2);
    for (byte c : cipherText) {
      encodedText.append(hexTab[(c >> 4) & 0x0F]);
      encodedText.append(hexTab[c & 0x0F]);
    }
    return encodedText.toString();
  }

  byte[] decryptString(String encryptedText, int plainTextLength, SecretKey key) {
    try {
      byte[] decodedCipher = hexDecode(encryptedText);
      System.out.println("cipher length: [" + decodedCipher.length + "] plain [" + plainTextLength
          + "]");
      byte[] deciphered = getCipher(Cipher.DECRYPT_MODE, key).doFinal(decodedCipher);
      return deciphered;
    } catch (IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException excp) {
      LOGGER.error("Problem while decrypting Computop callback", excp);
    }
    return null;
  }

  byte[] hexDecode(String callbackText) throws IllegalArgumentException {
    int length = callbackText.length();
    Preconditions.checkArgument((length % 2) == 0);
    byte[] cypherText = new byte[length / 2];
    byte byte1;
    byte byte2;
    int decodedByteIndex = 0;
    for (int i = 0; i < length; i += 2) {
      byte1 = hexDecodeByte((byte) callbackText.charAt(i));
      byte2 = hexDecodeByte((byte) callbackText.charAt(i + 1));
      cypherText[decodedByteIndex] = ((byte) (((byte1 * 16) + byte2) & 0x00ff));
      decodedByteIndex++;
    }
    System.out.println(cypherText.length);
    return cypherText;
  }

  byte hexDecodeByte(byte b) {
    b -= 48;
    if (b > 9) {
      b -= 7;
      if (b > 15) {
        b -= 32;
      }
    }
    return b;
  }

  Cipher getCipher(int cipherMode, SecretKey key) throws NoSuchPaddingException {
    try {
      Cipher cipher = Cipher.getInstance(BLOWFISH);
      cipher.init(cipherMode, key);
      return cipher;
    } catch (NoSuchAlgorithmException e) {
      LOGGER.error("{} algorithm not availabe", BLOWFISH);
    } catch (InvalidKeyException e) {
      LOGGER.error("SecretKey invalid for {} encryption", BLOWFISH);
    }
    return null;
  }

  SecretKey getKey() {
    return new SecretKeySpec("1234567890123456".getBytes(), BLOWFISH);
  }

}
