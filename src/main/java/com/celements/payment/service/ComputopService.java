package com.celements.payment.service;

/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;

import com.google.common.base.Optional;
import com.google.common.io.BaseEncoding;

@Component
public class ComputopService implements ComputopServiceRole {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComputopService.class);

  public static final String FORM_INPUT_NAME_LENGTH = "Len";
  public static final String FORM_INPUT_NAME_DATA = "Data";
  public static final String FORM_INPUT_NAME_MERCHANT_ID = "MerchantID";
  public static final String FORM_INPUT_NAME_TRANS_ID = "TransID";
  public static final String FORM_INPUT_NAME_AMOUNT = "Amount";
  public static final String FORM_INPUT_NAME_CURRENCY = "Currency";
  public static final String FORM_INPUT_NAME_DESCRIPTION = "OrderDesc";
  public static final String FORM_INPUT_NAME_HMAC = "MAC";
  public static final String DEFAULT_CURRENCY = "CHF";

  static final String MERCHANT_ID_PROP = "computop_merchant_id";

  static final String BLOWFISH = "Blowfish";
  static final String BLOWFISH_ECB_PADDED = BLOWFISH + "/ECB/PKCS5Padding";
  static final String BLOWFISH_ECB_UNPADDED = BLOWFISH + "/ECB/NoPadding";
  static final String BLOWFISH_SECRET_KEY_PROP = "computop_blowfish_secret_key";

  static final String HMAC_SHA256 = "HmacSHA256";
  static final String HMAC_SECRET_KEY_PROP = "computop_hmac_secret_key";

  enum ReturnUrl {
    SUCCESS("computop_return_url_success", "URLSuccess"), FAILURE("computop_return_url_failure",
        "URLFailure"), CALLBACK("computop_return_url_callback", "URLNotify");

    private final String value;
    private final String param;

    private ReturnUrl(String value, String param) {
      this.value = value;
      this.param = param;
    }

    public String getValue() {
      return value;
    }

    public String getParamName() {
      return param;
    }
  }

  @Requirement
  ConfigurationSource configSrc;

  @Override
  public boolean isCallbackHashValid(String hash, String payId, String transId, String merchantId,
      String status, String code) {
    checkNotNull(hash);
    if (!hash.toLowerCase().equals(hashPaymentData(nullToEmpty(payId) + "*" + nullToEmpty(transId)
        + "*" + nullToEmpty(merchantId) + "*" + nullToEmpty(status) + "*" + nullToEmpty(code)))) {
      LOGGER.warn("Verifying hash [{}] failed with data payId [{}], transId [{}], merchantId [{}], "
          + "status [{}], code [{}]", hash, payId, transId, merchantId, status, code);
      return false;
    }
    return true;
  }

  @Override
  public String getPaymentDataHmac(String payId, String transId, String merchantId,
      BigDecimal amount, String currency) {
    return hashPaymentData(nullToEmpty(payId) + "*" + nullToEmpty(transId) + "*" + nullToEmpty(
        merchantId) + "*" + getAmount(amount) + "*" + nullToEmpty(currency));
  }

  String hashPaymentData(@NotNull String paymentData) {
    SecretKeySpec key = new javax.crypto.spec.SecretKeySpec(getHmacKey(), HMAC_SHA256);
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      mac.init(key);
      return BaseEncoding.base16().encode(mac.doFinal(paymentData.getBytes())).toLowerCase();
    } catch (NoSuchAlgorithmException e) {
      LOGGER.error("Computop requires [{}] HMAC", HMAC_SHA256);
    } catch (InvalidKeyException e) {
      LOGGER.warn("Key [{}] is not valid for generating HMAC", key);
    }
    return "";
  }

  @Override
  public Map<String, String> encryptPaymentData(String transactionId, String orderDescription,
      BigDecimal amount, String currency) {
    String dataPlainText = getPaymentDataPlainString(transactionId, orderDescription, amount,
        currency);
    Map<String, String> encryptedData = new HashMap<>();
    encryptedData.put(FORM_INPUT_NAME_LENGTH, Integer.toString(dataPlainText.length()));
    String encyrptedData = encryptString(dataPlainText.getBytes(), getBlowfishKey());
    if (encyrptedData != null) {
      encryptedData.put(FORM_INPUT_NAME_DATA, encyrptedData);
    }
    return encryptedData;
  }

  @Override
  public Map<String, String> decryptCallbackData(String encryptedCallback, int plainDataLength) {
    checkNotNull(encryptedCallback);
    byte[] decryptedData = decryptString(encryptedCallback, plainDataLength, getBlowfishKey());
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

  String getPaymentDataPlainString(String transactionId, String orderDescription, BigDecimal amount,
      String currency) {
    checkNotNull(transactionId);
    checkNotNull(amount);
    currency = Optional.fromNullable(currency).or(DEFAULT_CURRENCY);
    String merchantId = getMerchantId();
    StringBuilder sb = new StringBuilder();
    sb.append(FORM_INPUT_NAME_MERCHANT_ID).append("=").append(merchantId);
    sb.append("&").append(FORM_INPUT_NAME_TRANS_ID).append("=").append(transactionId);
    sb.append("&").append(FORM_INPUT_NAME_AMOUNT).append("=").append(getAmount(amount));
    sb.append("&").append(FORM_INPUT_NAME_CURRENCY).append("=").append(currency);
    sb.append("&").append(FORM_INPUT_NAME_DESCRIPTION).append("=").append(orderDescription);
    sb.append("&").append(FORM_INPUT_NAME_HMAC);
    sb.append("=").append(getPaymentDataHmac(null, transactionId, merchantId, amount, currency));
    sb.append("&").append(ReturnUrl.SUCCESS.getParamName());
    sb.append("=").append(getReturnUrl(ReturnUrl.SUCCESS));
    sb.append("&").append(ReturnUrl.FAILURE.getParamName());
    sb.append("=").append(getReturnUrl(ReturnUrl.FAILURE));
    sb.append("&").append(ReturnUrl.CALLBACK.getParamName());
    sb.append("=").append(getReturnUrl(ReturnUrl.CALLBACK));
    return sb.toString();
  }

  String encryptString(byte[] plainText, final SecretKey key) {
    try {
      return BaseEncoding.base16().encode(getCipher(Cipher.ENCRYPT_MODE, BLOWFISH_ECB_PADDED,
          key).doFinal(plainText));
    } catch (IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException excp) {
      LOGGER.error("Problem while encrypting payment data", excp);
    }
    return null;
  }

  byte[] decryptString(String encryptedBase16, int plainTextLength, final SecretKey key) {
    CharSequence cs = encryptedBase16.toUpperCase();
    byte[] decodedCipher = BaseEncoding.base16().decode(cs);
    try {
      Cipher cipher = getCipher(Cipher.DECRYPT_MODE, BLOWFISH_ECB_UNPADDED, key);
      byte[] deciphered = cipher.doFinal(decodedCipher);
      return Arrays.copyOfRange(deciphered, 0, plainTextLength);
    } catch (IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException excp) {
      LOGGER.error("Problem while decrypting Computop callback", excp);
    }
    return null;
  }

  Cipher getCipher(int cipherMode, final String algorithm, final SecretKey key)
      throws NoSuchPaddingException {
    try {
      Cipher cipher = Cipher.getInstance(algorithm);
      cipher.init(cipherMode, key);
      return cipher;
    } catch (NoSuchAlgorithmException e) {
      LOGGER.error("{} algorithm not availabe", algorithm);
    } catch (InvalidKeyException e) {
      LOGGER.error("SecretKey invalid for {} encryption", algorithm);
    }
    return null;
  }

  String getAmount(BigDecimal amount) {
    if (amount != null) {
      return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
    return "";
  }

  byte[] getHmacKey() {
    return configSrc.getProperty(HMAC_SECRET_KEY_PROP, "").getBytes();
  }

  SecretKey getBlowfishKey() {
    String secretKey = configSrc.getProperty(BLOWFISH_SECRET_KEY_PROP, "");
    return new SecretKeySpec(secretKey.getBytes(), BLOWFISH);
  }

  String getMerchantId() {
    return configSrc.getProperty(MERCHANT_ID_PROP, "");
  }

  String getReturnUrl(ReturnUrl urlType) {
    return configSrc.getProperty(urlType.getValue(), "");
  }

}
