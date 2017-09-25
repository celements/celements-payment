package com.celements.payment.service;

import static com.celements.payment.service.ComputopServiceRole.*;

import java.math.BigDecimal;

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
import org.xwiki.script.service.ScriptService;

import com.celements.model.context.ModelContext;
import com.celements.payment.container.EncryptedComputopData;
import com.google.common.base.Optional;
import com.xpn.xwiki.web.XWikiRequest;

@Component("computop")
public class ComputopScriptService implements ScriptService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComputopScriptService.class);

  @Requirement
  private ComputopServiceRole computopService;

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
      @Nullable String orderDescription, double amount, @Nullable String currency) {
    return computopService.encryptPaymentData(transactionId, orderDescription, new BigDecimal(
        amount), currency);
  }
}
