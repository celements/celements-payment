package com.celements.payment.service;

import java.math.BigDecimal;
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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.script.service.ScriptService;

@Component("computop")
public class ComputopScriptService implements ScriptService {

  @Requirement
  private ComputopServiceRole computopService;

  public boolean isCallbackHashValid(@NotNull String hash, @NotNull String payId,
      @NotNull String transId, @NotNull String merchantId, @NotNull String status,
      @NotNull String code) {
    return computopService.isCallbackHashValid(hash, payId, transId, merchantId, status, code);
  }

  public @NotNull Map<String, String> encryptPaymentData(@NotNull String transactionId,
      @Nullable String orderDescription, double amount, @Nullable String currency) {
    return computopService.encryptPaymentData(transactionId, orderDescription, new BigDecimal(
        amount), currency);
  }
}
