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

import javax.crypto.Mac;
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
import com.celements.payment.raw.Computop;
import com.celements.payment.raw.EProcessStatus;
import com.google.common.io.BaseEncoding;
import com.xpn.xwiki.XWikiException;

@Component
public class ComputopService implements ComputopServiceRole {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComputopService.class);

  static final String HMAC_SHA256 = "HmacSHA256";
  static final String HMAC_SECRET_KEY_PROP = "computop_hmac_secret_key";

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
        merchantId) + "*" + getAmount(amount) + "*" + nullToEmpty(currency));
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

  public void storeCallback() {
    // TODO check for callback request
    if (context.getRequest().isPresent()) {
      LOGGER.info("received computop callback");
      Computop computopObj = createComputopObjectFromRequest();
      try {
        paymentService.storePaymentObject(computopObj);
        // FIXME move execution of callbackAction to general async processing of callback
        // TODO execute callback action
      } catch (XWikiException exp) {
        LOGGER.error("Failed to store computop object", exp);
      }
    }
  }

  private Computop createComputopObjectFromRequest() {
    Computop computopObj = new Computop();
    computopObj.setOrigHeader(paymentService.serializeHeaderFromRequest());
    computopObj.setOrigMessage(paymentService.serializeParameterMapFromRequest());
    computopObj.setMerchantId(paymentService.getRequestParam("MerchantID"));
    computopObj.setLength(NumberUtils.toInt(paymentService.getRequestParam("Length"), 0));
    computopObj.setData(paymentService.getRequestParam("Data"));
    computopObj.setProcessStatus(EProcessStatus.New);
    return computopObj;
  }

}
