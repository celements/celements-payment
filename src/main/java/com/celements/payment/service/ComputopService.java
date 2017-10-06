package com.celements.payment.service;

import static com.celements.payment.classes.ComputopPaymentClass.*;

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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
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
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.context.ModelContext;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.util.References;
import com.celements.payment.IPaymentService;
import com.celements.payment.container.EncryptedComputopData;
import com.celements.payment.exception.ComputopCryptoException;
import com.celements.payment.exception.PaymentException;
import com.celements.payment.raw.Computop;
import com.celements.payment.raw.EProcessStatus;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.io.BaseEncoding;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component
public class ComputopService implements ComputopServiceRole {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComputopService.class);

  static final String BLOWFISH = "Blowfish";
  static final String BLOWFISH_ECB_PADDED = BLOWFISH + "/ECB/PKCS5Padding";
  static final String BLOWFISH_ECB_UNPADDED = BLOWFISH + "/ECB/NoPadding";
  static final String HMAC_SHA256 = "HmacSHA256";

  static final String CFG_PROP_MERCHANT_ID = "computop_merchant_id";
  static final String CFG_PROP_ORDER_SPACE = "computop_order_space";
  static final String CFG_PROP_PAYMENT_SPACE = "computop_payment_space";
  static final String CFG_PROP_BLOWFISH_SECRET_KEY = "computop_blowfish_secret_key";
  static final String CFG_PROP_HMAC_SECRET_KEY = "computop_hmac_secret_key";

  static final String DATA_KEY_MID = "mid";
  static final String DATA_KEY_PAYID = "payid";
  static final String DATA_KEY_XID = "xid";
  static final String DATA_KEY_TRANSID = "transid";
  static final String DATA_KEY_MAC = "mac";
  static final String DATA_KEY_TYPE = "type";
  static final String DATA_KEY_DESCR = "description";
  static final String DATA_KEY_STATUS = "status";
  static final String DATA_KEY_CODE = "code";

  enum ComputopPaymentStatus {
    SUCCESSFUL("AUTHORIZED"),
    FAILED("FAILED");

    private final String value;

    private ComputopPaymentStatus(String value) {
      this.value = value;
    }

    public @NotNull String getValue() {
      return value;
    }

  }

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
  private IPaymentService paymentService;

  @Requirement
  private IModelAccessFacade modelAccess;

  @Requirement
  private ConfigurationSource configSrc;

  @Requirement
  private ModelContext context;

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

  private boolean isCallbackHashValid(Map<String, String> data) {
    return isCallbackHashValid(getDataValue(data, DATA_KEY_MAC), getDataValue(data, DATA_KEY_PAYID),
        getDataValue(data, DATA_KEY_TRANSID), getDataValue(data, DATA_KEY_MID), getDataValue(data,
            DATA_KEY_STATUS), getDataValue(data, DATA_KEY_CODE));
  }

  @Override
  public String getPaymentDataHmac(String payId, String transId, String merchantId, int amount,
      String currency) {
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

  private byte[] getHmacKey() {
    return getCfgPropertyNonEmpty(CFG_PROP_HMAC_SECRET_KEY).getBytes();
  }

  @Override
  public EncryptedComputopData encryptPaymentData(String transactionId, String orderDescription,
      int amount, String currency) throws ComputopCryptoException {
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

  private SecretKey getBlowfishKey() {
    return new SecretKeySpec(getCfgPropertyNonEmpty(CFG_PROP_BLOWFISH_SECRET_KEY).getBytes(),
        BLOWFISH);
  }

  String getPaymentDataPlainString(String transactionId, String orderDescription, int amount,
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

  String getFormatedAmountString(int amount) {
    return Integer.toString(amount);
  }

  String getReturnUrl(ReturnUrl urlType) {
    return getCfgPropertyNonEmpty(urlType.getValue());
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

  @Override
  public String getMerchantId() {
    return getCfgPropertyNonEmpty(CFG_PROP_MERCHANT_ID);
  }

  @Override
  public SpaceReference getOrderSpaceRef() {
    return References.create(SpaceReference.class, getCfgPropertyNonEmpty(CFG_PROP_ORDER_SPACE),
        context.getWikiRef());
  }

  @Override
  public DocumentReference getOrderDocRef(String transactionId) {
    return References.create(DocumentReference.class, transactionId, getOrderSpaceRef());
  }

  @Override
  public SpaceReference getPaymentSpaceRef() {
    return References.create(SpaceReference.class, getCfgPropertyNonEmpty(CFG_PROP_PAYMENT_SPACE),
        context.getWikiRef());
  }

  @Override
  public DocumentReference getPaymentDocRef(String transactionId) {
    return References.create(DocumentReference.class, transactionId, getPaymentSpaceRef());
  }

  @Override
  public void storeCallback() throws ComputopCryptoException, PaymentException {
    Computop computopObj = createComputopObjectFromRequest();
    if (!computopObj.getData().isEmpty()) {
      LOGGER.info("storeCallback: '{}'", computopObj);
      paymentService.storePaymentObject(computopObj);
      // TODO [CELDEV-559] async callback processing
      executeCallbackAction(computopObj);
    } else {
      throw new PaymentException("empty callback data");
    }
  }

  @Override
  public void storeOfflineCallback(XWikiDocument doc) throws PaymentException {
    // TODO [CELDEV-561] create ClassDefinitions for Classes.CXMLShoppingCart
    ClassReference classRef = new ClassReference("Classes", "CXMLShoppingCartItem");
    Optional<BaseObject> cartObj = XWikiObjectFetcher.on(doc).filter(classRef).first();
    if (cartObj.isPresent()) {
      Map<String, String> data = new HashMap<>();
      data.put(DATA_KEY_MID, getMerchantId());
      Float price = cartObj.get().getFloatValue("einzPreis");
      String articleNr = cartObj.get().getStringValue("artikelnr");
      data.put(DATA_KEY_DESCR, articleNr + ":" + price);
      if ((price == null) || (price > 0)) {
        data.put(DATA_KEY_STATUS, ComputopPaymentStatus.FAILED.getValue());
      } else {
        data.put(DATA_KEY_STATUS, ComputopPaymentStatus.SUCCESSFUL.getValue());
      }
      String transId = doc.getDocumentReference().getName();
      executeOfflineCallbackAction(transId, true, data);
    } else {
      throw new PaymentException("no order doc: " + doc.getDocumentReference());
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
    LOGGER.info("executeCallbackAction: '{}'", computopObj);
    Map<String, String> data = decryptCallbackData(new EncryptedComputopData(computopObj.getData(),
        computopObj.getLength()));
    LOGGER.trace("executeCallbackAction: with data '{}'", data);
    String transId = getDataValue(data, DATA_KEY_TRANSID);
    if (!transId.isEmpty()) {
      boolean verified = isCallbackHashValid(data);
      computopObj.setTxnId(transId);
      computopObj.setProcessStatus(verified ? EProcessStatus.Verified : EProcessStatus.Unverified);
      paymentService.storePaymentObject(computopObj);
      try {
        storePaymentData(transId, verified, data);
      } catch (DocumentSaveException dse) {
        throw new PaymentException(dse);
      }
    } else {
      computopObj.setProcessStatus(EProcessStatus.Processed);
      paymentService.storePaymentObject(computopObj);
      throw new PaymentException("No transId for: " + computopObj);
    }
  }

  @Override
  public void executeOfflineCallbackAction(String transId, boolean verified,
      Map<String, String> data) throws PaymentException {
    data.put(DATA_KEY_XID, "Manual Callback Action");
    try {
      storePaymentData(transId, verified, data);
    } catch (DocumentSaveException dse) {
      throw new PaymentException(dse);
    }
  }

  @Override
  public boolean isAuthorizedPayment(XWikiDocument doc) {
    return XWikiObjectFetcher.on(doc).filter(FIELD_VERIFIED, true).filter(FIELD_STATUS,
        ComputopPaymentStatus.SUCCESSFUL.getValue()).exists();
  }

  private void storePaymentData(String transId, boolean verified, Map<String, String> data)
      throws DocumentSaveException {
    XWikiDocument paymentDoc = modelAccess.getOrCreateDocument(getPaymentDocRef(transId));
    BaseObject paymentObj = XWikiObjectEditor.on(paymentDoc).filter(FIELD_TRANS_ID,
        transId).createFirstIfNotExists();
    modelAccess.setProperty(paymentObj, FIELD_MERCHANT_ID, getDataValue(data, DATA_KEY_MID));
    modelAccess.setProperty(paymentObj, FIELD_PAY_ID, getDataValue(data, DATA_KEY_PAYID));
    modelAccess.setProperty(paymentObj, FIELD_X_ID, getDataValue(data, DATA_KEY_XID));
    modelAccess.setProperty(paymentObj, FIELD_DESCRIPTION, getDataValue(data, DATA_KEY_DESCR));
    modelAccess.setProperty(paymentObj, FIELD_STATUS, getDataValue(data, DATA_KEY_STATUS));
    modelAccess.setProperty(paymentObj, FIELD_STATUS_CODE, NumberUtils.toInt(getDataValue(data,
        DATA_KEY_CODE), -1));
    modelAccess.setProperty(paymentObj, FIELD_VERIFIED, verified);
    modelAccess.setProperty(paymentObj, FIELD_DATA, data.toString());
    modelAccess.setProperty(paymentDoc, FIELD_CALLBACK_DATE, new Date());
    modelAccess.saveDocument(paymentDoc, "add computop payment object");
  }

  private String getDataValue(Map<String, String> data, String key) {
    String dataMapKey = MoreObjects.firstNonNull(emptyToNull(configSrc.getProperty(
        "computop_data_key_" + key, key)), key);
    return nullToEmpty(data.get(dataMapKey));
  }

  private String getCfgPropertyNonEmpty(String key) {
    String value = nullToEmpty(configSrc.getProperty(key, ""));
    checkArgument(!value.isEmpty(), key + " not configured");
    return value;
  }

}
