package com.celements.payment.service;

import static com.google.common.base.Preconditions.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
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

  static final String HMAC_SHA256 = "HmacSHA256";
  static final String HMAC_SECRET_KEY_PROP = "computop_hmac_secret_key";

  @Requirement
  ConfigurationSource configSrc;

  @Override
  public boolean isCallbackHashValid(String hash, String payId, String transId, String merchantId,
      String status, String code) {
    checkNotNull(hash);
    if (!hash.toLowerCase().equals(hashPaymentData(nullStringToEmpty(payId) + "*"
        + nullStringToEmpty(transId) + "*" + nullStringToEmpty(merchantId) + "*"
        + nullStringToEmpty(status) + "*" + nullStringToEmpty(code)))) {
      LOGGER.warn("Verifying hash [{}] failed with data payId [{}], transId [{}], merchantId [{}], "
          + "status [{}], code [{}]", hash, payId, transId, merchantId, status, code);
      return false;
    }
    return true;
  }

  @Override
  public String getPaymentDataHmac(String payId, String transId, String merchantId,
      BigDecimal amount, String currency) {
    return hashPaymentData(nullStringToEmpty(payId) + "*" + nullStringToEmpty(transId) + "*"
        + nullStringToEmpty(merchantId) + "*" + getAmount(amount) + "*" + nullStringToEmpty(
            currency));
  }

  private String nullStringToEmpty(String payId) {
    return Optional.fromNullable(payId).or("");
  }

  String getAmount(BigDecimal amount) {
    if (amount != null) {
      return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
    return "";
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

  byte[] getHmacKey() {
    return configSrc.getProperty(HMAC_SECRET_KEY_PROP, "").getBytes();
  }

}
