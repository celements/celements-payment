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
import static java.lang.Math.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;

import com.celements.model.context.ModelContext;
import com.celements.payment.IPaymentService;
import com.celements.payment.container.EncryptedComputopData;
import com.celements.payment.exception.ComputopCryptoException;
import com.celements.payment.exception.PaymentException;
import com.celements.payment.raw.Computop;
import com.celements.payment.raw.EProcessStatus;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.io.BaseEncoding;

@Component
public class ComputopService implements ComputopServiceRole {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComputopService.class);

  static final String MERCHANT_ID_PROP = "computop_merchant_id";

  static final String BLOWFISH = "Blowfish";
  static final String BLOWFISH_ECB_PADDED = BLOWFISH + "/ECB/PKCS5Padding";
  static final String BLOWFISH_ECB_UNPADDED = BLOWFISH + "/ECB/NoPadding";
  static final String BLOWFISH_SECRET_KEY_PROP = "computop_blowfish_secret_key";

  static final String HMAC_SHA256 = "HmacSHA256";
  static final String HMAC_SECRET_KEY_PROP = "computop_hmac_secret_key";

  enum ReturnUrl {
    SUCCESS("computop_return_url_success", "URLSuccess"),
    FAILURE("computop_return_url_failure", "URLFailure"),
    CALLBACK("computop_return_url_callback", "URLNotify");

    private final String value;
    private final String param;

    private ReturnUrl(String value, String param) {
      this.value = value;
      this.param = param;
    }

    public @NotNull String getValue() {
      return value;
    }

    public @NotNull String getParamName() {
      return param;
    }
  }

  @Requirement
  IPaymentService paymentService;

  @Requirement
  ConfigurationSource configSrc;

  @Requirement
  ModelContext context;

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
        merchantId) + "*" + getFormatedAmountString(amount) + "*" + nullToEmpty(currency));
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
  public EncryptedComputopData encryptPaymentData(String transactionId, String orderDescription,
      BigDecimal amount, String currency) throws ComputopCryptoException {
    String dataPlainText = getPaymentDataPlainString(transactionId, orderDescription, amount,
        currency);
    String cipherData = encryptString(dataPlainText.getBytes(), getBlowfishKey());
    return new EncryptedComputopData(cipherData, dataPlainText.length());
  }

  @Override
  public Map<String, String> decryptCallbackData(EncryptedComputopData encryptedCallback)
      throws ComputopCryptoException {
    checkNotNull(encryptedCallback);
    byte[] decryptedData = decryptString(encryptedCallback, getBlowfishKey());
    Map<String, String> callbackData = new HashMap<>();
    for (String parameter : Splitter.on("&").omitEmptyStrings().split(new String(decryptedData))) {
      List<String> keyValue = Splitter.on("=").limit(2).splitToList(parameter);
      String key = keyValue.get(0).toLowerCase();
      if (keyValue.size() > 1) {
        callbackData.put(key, keyValue.get(1));
      } else {
        callbackData.put(key, "");
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
    appendQueryParameter(sb, FORM_INPUT_NAME_MERCHANT_ID, merchantId);
    appendQueryParameter(sb, FORM_INPUT_NAME_TRANS_ID, transactionId);
    appendQueryParameter(sb, FORM_INPUT_NAME_AMOUNT, getFormatedAmountString(amount));
    appendQueryParameter(sb, FORM_INPUT_NAME_CURRENCY, currency);
    appendQueryParameter(sb, FORM_INPUT_NAME_DESCRIPTION, orderDescription);
    appendQueryParameter(sb, FORM_INPUT_NAME_HMAC, getPaymentDataHmac(null, transactionId,
        merchantId, amount, currency));
    appendQueryParameter(sb, ReturnUrl.SUCCESS.getParamName(), getReturnUrl(ReturnUrl.SUCCESS));
    appendQueryParameter(sb, ReturnUrl.FAILURE.getParamName(), getReturnUrl(ReturnUrl.FAILURE));
    appendQueryParameter(sb, ReturnUrl.CALLBACK.getParamName(), getReturnUrl(ReturnUrl.CALLBACK));
    return sb.toString();
  }

  void appendQueryParameter(StringBuilder sb, String key, String value) {
    sb.append(key).append("=").append(value).append("&");
  }

  @NotNull
  String encryptString(byte[] plainText, final SecretKey key) throws ComputopCryptoException {
    LOGGER.debug("encrypting plain [{}]", new String(plainText));
    try {
      String cipherText = BaseEncoding.base16().encode(getCipher(Cipher.ENCRYPT_MODE,
          BLOWFISH_ECB_PADDED, key).doFinal(plainText));
      LOGGER.debug("encrypted cipher [{}]", cipherText);
      return cipherText;
    } catch (IllegalBlockSizeException | BadPaddingException excp) {
      throw new ComputopCryptoException("Exception encrypting message", excp);
    }
  }

  byte[] decryptString(EncryptedComputopData encryptedCallback, final SecretKey key)
      throws ComputopCryptoException {
    if (encryptedCallback.getCipherText().isPresent()) {
      CharSequence cs = encryptedCallback.getCipherText().get().toUpperCase();
      LOGGER.debug("decrypting cipher [{}]", cs);
      byte[] decodedCipher = BaseEncoding.base16().decode(cs);
      try {
        Cipher cipher = getCipher(Cipher.DECRYPT_MODE, BLOWFISH_ECB_UNPADDED, key);
        int len = encryptedCallback.getPlainDataLength();
        byte[] unpadded = cipher.doFinal(decodedCipher);
        byte[] deciphered = Arrays.copyOfRange(unpadded, 0, min(max(len, 0), unpadded.length));
        LOGGER.debug("decryped plain [{}]", new String(deciphered));
        return deciphered;
      } catch (IllegalBlockSizeException | BadPaddingException excp) {
        throw new ComputopCryptoException("Exception decrypting message", excp);
      }
    } else {
      throw new ComputopCryptoException("Ciphertext to decrypted is absent");
    }
  }

  Cipher getCipher(int cipherMode, final String algorithm, final SecretKey key)
      throws ComputopCryptoException {
    try {
      Cipher cipher = Cipher.getInstance(algorithm);
      cipher.init(cipherMode, key);
      return cipher;
    } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException excp) {
      throw new ComputopCryptoException("Exception creating encryption cipher", excp);
    }
  }

  String getFormatedAmountString(BigDecimal amount) {
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

  @Override
  public void storeCallback() throws ComputopCryptoException, PaymentException {
    LOGGER.info("received computop callback");
    Computop computopObj = createComputopObjectFromRequest();
    if (!computopObj.getData().isEmpty()) {
      paymentService.storePaymentObject(computopObj);
      // FIXME move callback processing to general async thread
      executeCallbackAction(computopObj);
    } else {
      throw new PaymentException("empty callback data");
    }
  }

  private Computop createComputopObjectFromRequest() {
    Computop computopObj = new Computop();
    computopObj.setOrigHeader(paymentService.serializeHeaderFromRequest());
    computopObj.setOrigMessage(paymentService.serializeParameterMapFromRequest());
    computopObj.setLength(NumberUtils.toInt(getRequestParam(FORM_INPUT_NAME_LENGTH), 0));
    computopObj.setData(getRequestParam(FORM_INPUT_NAME_DATA));
    computopObj.setProcessStatus(EProcessStatus.New);
    return computopObj;
  }

  private String getRequestParam(String key) {
    String value = "";
    if (context.getRequest().isPresent()) {
      value = context.getRequest().get().get(key);
    }
    return value;
  }

  @Override
  public void executeCallbackAction(Computop computopObj) throws ComputopCryptoException,
      PaymentException {
    EncryptedComputopData encryptedData = new EncryptedComputopData(computopObj.getData(),
        computopObj.getLength());
    Map<String, String> decryptedData = decryptCallbackData(encryptedData);
    // TODO SYNCEL-26 verify callback
    computopObj.setTxnId(decryptedData.get(FORM_INPUT_NAME_TRANS_ID));
    paymentService.storePaymentObject(computopObj);
    // TODO SYNCEL-26 save to BaseObject
  }

}
