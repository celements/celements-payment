package com.celements.payment.service;

import static com.celements.payment.service.ComputopServiceRole.*;
import static com.google.common.base.Strings.*;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

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

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.script.service.ScriptService;

import com.celements.model.context.ModelContext;
import com.celements.payment.container.EncryptedComputopData;
import com.celements.payment.exception.ComputopCryptoException;
import com.celements.payment.exception.PaymentException;
import com.celements.rights.access.EAccessLevel;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.google.common.base.Optional;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;

@Component("computop")
public class ComputopScriptService implements ScriptService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComputopScriptService.class);

  public static final String COMPUTOP_PAYFORM_ACTION_URL = "computop_paymform_action_url";
  public static final String CELEMENTS_PAYFORM_ACTION_URL = "celements_paymform_action_url";

  @Requirement
  private ComputopServiceRole computopService;

  @Requirement
  private ConfigurationSource configSrc;

  @Requirement
  private IRightsAccessFacadeRole rightsAccess;

  @Requirement
  private ModelContext modelContext;

  /**
   * Checks from request parameters if the callback HMAC is valid.
   *
   * @return true if the request contains a HMAC which is verifiable
   */
  public boolean isCallbackHashValid() {
    Optional<XWikiRequest> request = modelContext.getRequest();
    if (request.isPresent()) {
      String hash = request.get().get(PAYER_RETURN_REQUEST_NAME_HMAC);
      String payId = request.get().get(PAYER_RETURN_REQUEST_NAME_PAY_ID);
      String transId = request.get().get(PAYER_RETURN_REQUEST_NAME_TRANS_ID);
      String merchantId = request.get().get(PAYER_RETURN_REQUEST_NAME_MERCHANT_ID);
      String status = request.get().get(PAYER_RETURN_REQUEST_NAME_STATUS);
      String code = request.get().get(PAYER_RETURN_REQUEST_NAME_CODE);
      if ((hash != null) && (payId != null) && (transId != null) && (merchantId != null)
          && (status != null) && (code != null)) {
        return isCallbackHashValid(hash, payId, transId, merchantId, status, code);
      } else {
        LOGGER.warn("isCallbackHashValid: missing parameter(s) to verify HMAC! {}=[{}], {}=[{}], "
            + "{}=[{}], {}=[{}], {}=[{}], {}=[{}]", PAYER_RETURN_REQUEST_NAME_HMAC, hash,
            PAYER_RETURN_REQUEST_NAME_PAY_ID, payId, PAYER_RETURN_REQUEST_NAME_TRANS_ID, transId,
            PAYER_RETURN_REQUEST_NAME_MERCHANT_ID, merchantId, PAYER_RETURN_REQUEST_NAME_STATUS,
            status, PAYER_RETURN_REQUEST_NAME_CODE, code);
      }
    }
    return false;
  }

  public boolean isCallbackHashValid(@NotNull String hash, @NotNull String payId,
      @NotNull String transId, @NotNull String merchantId, @NotNull String status,
      @NotNull String code) {
    return computopService.isCallbackHashValid(hash, payId, transId, merchantId, status, code);
  }

  public @NotNull EncryptedComputopData encryptPaymentData(@NotNull String transactionId,
      @Nullable String orderDescription, int amount, @Nullable String currency) {
    try {
      if (transactionId != null) {
        return computopService.encryptPaymentData(transactionId, orderDescription, amount,
            currency);
      } else {
        LOGGER.warn("encryptPaymentData called with transaction ID 'null'");
      }
    } catch (ComputopCryptoException cce) {
      LOGGER.error("Exception encrypting computop data transId [{}], orderDesc [{}], amount [{}], "
          + "currency [{}]", transactionId, orderDescription, amount, currency, cce);
    }
    return new EncryptedComputopData("", -1);
  }

  /* Double is used here, since cart item saves the amount as floating point number */
  public @NotNull String getPayFormAction(double amount) {
    LOGGER.debug("checking amount[{}] > 0 to determin form action", amount);
    if (amount > 0) {
      return configSrc.getProperty(COMPUTOP_PAYFORM_ACTION_URL, "");
    }
    return configSrc.getProperty(CELEMENTS_PAYFORM_ACTION_URL, "");
  }

  /**
   * This method is meant for testing purposes only. In a production environment it is discouraged
   * to use Velocity to handle payment callbacks. A Java implementation should be used instead.
   *
   * @return A map containing the decrypted callback data (keys are lower case, number and name of
   *         keys depends on Computop and may vary)
   * @throws ComputopCryptoException
   *           thrown if the decryption fails
   */
  public @NotNull Map<String, String> decryptPaymentData() throws ComputopCryptoException {
    Optional<XWikiRequest> request = modelContext.getRequest();
    if (request.isPresent()) {
      String cipherText = nullToEmpty(request.get().get(FORM_INPUT_NAME_DATA));
      int plainDataLength = -1;
      try {
        plainDataLength = Integer.parseInt(nullToEmpty(request.get().get(FORM_INPUT_NAME_LENGTH)));
      } catch (NumberFormatException nfe) {
        LOGGER.debug("Exception parsing number from param [{}]=[{}]", FORM_INPUT_NAME_LENGTH,
            request.get().get(FORM_INPUT_NAME_LENGTH), nfe);
      }
      EncryptedComputopData encryptedData = new EncryptedComputopData(cipherText, plainDataLength);
      return computopService.decryptCallbackData(encryptedData);
    }
    return Collections.emptyMap();
  }

  public boolean storeCallback() {
    try {
      computopService.storeCallback();
      LOGGER.info("storeCallback called");
      return true;
    } catch (PaymentException exc) {
      LOGGER.error("storeCallback failed", exc);
      return false;
    }
  }

  /**
   * Store a callback for the current document (no check for request data from third parties)
   *
   * @return true if the callback was saved correctly
   */
  public boolean storeOfflineCallback() {
    XWikiDocument doc = modelContext.getDoc();
    if (rightsAccess.hasAccessLevel(doc.getDocumentReference(), EAccessLevel.EDIT)) {
      try {
        LOGGER.info("storeOfflineCallback called");
        computopService.storeOfflineCallback(doc);
        return true;
      } catch (PaymentException exc) {
        LOGGER.error("storeOfflineCallback failed", exc);
      }
    } else {
      LOGGER.warn("storeOfflineCallback [{}] has no edit rights on [{}]",
          modelContext.getUserName(), doc);
    }
    return false;
  }

  public SpaceReference getOrderSpaceRef() {
    return computopService.getOrderSpaceRef();
  }

  public DocumentReference getOrderDocRef(String transactionId) {
    return computopService.getOrderDocRef(transactionId);
  }

  public SpaceReference getPaymentSpaceRef() {
    return computopService.getOrderSpaceRef();
  }

  public DocumentReference getPaymentDocRef(String transactionId) {
    return computopService.getPaymentDocRef(transactionId);
  }

}
