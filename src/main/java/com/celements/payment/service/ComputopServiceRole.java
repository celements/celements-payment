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

import java.math.BigDecimal;
import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;

@ComponentRole
public interface ComputopServiceRole {

  /**
   * Hash Message Authentication Codes (HMAC) used in the MAC parameter when submitting a payment
   * form to Coputop
   *
   * @param payId
   *          Computop ID to connect multiple authorisations / payments. Empty for first payment
   * @param transId
   *          A transaction ID for identifying the cart
   * @param merchantId
   *          The ID of the merchent receiving the payment
   * @param amount
   *          Amount to be payed with 2 decimal
   * @param currency
   *          Currency of the transaction
   * @return HMAC of the given data
   */
  public @NotNull String getPaymentDataHmac(@Nullable String payId, @Nullable String transId,
      @Nullable String merchantId, @Nullable BigDecimal amount, @Nullable String currency);

  /**
   * To verify the data returned to the success or failed URLs
   *
   * @param hash
   *          Hashed value to verify the given callback data
   * @param payId
   *          Computop ID used when making e.g. when collecting payment for a prior authorised
   *          amount
   * @param transId
   *          A transaction ID for identifying the cart
   * @param merchantId
   *          The ID of the merchant receiving the payment
   * @param status
   *          Status of the payment
   * @param code
   *          Code specifying the status
   * @return
   */
  public boolean isCallbackHashValid(@NotNull String hash, @Nullable String payId,
      @Nullable String transId, @Nullable String merchantId, @Nullable String status,
      @Nullable String code);

  /**
   * Returns a map with the form parameters 'Len' and 'Data' to transmit to computop when checking
   * out. The 'data' map entry contains the encrypted version of the following fields:
   * - MerchantID
   * - TransID
   * - Amount
   * - Currency
   * - URLSuccess
   * - URLFailure
   * - URLNotify
   *
   * @param transactionId
   *          Identification of the payment / cart
   * @param amount
   *          Amount to be payed
   * @param currency
   *          Currency of the amount (default 'CHF')
   * @return Map containing 'Len' and 'Data' parameters to be transmitted with checkout form
   */
  public @NotNull Map<String, String> encryptPaymentData(@NotNull String transactionId,
      @NotNull BigDecimal amount, @Nullable String currency);

  /**
   * Deciphers callback data and returns all contained data in a map.
   * Attention:
   * - All the map key names are lower case. (Computop asks in their documentation to ignore case)
   * - The content of the map can change if Computop adds or removes parameters from their callback.
   *
   * @param encryptedCallback
   *          Encrypted data string received as callback for a Computop payment
   * @param plainDataLength
   *          The length of the unencrypted data
   * @return Map containing all parameters received in a Computop payment callback. Keys in lower
   *         case
   */
  public @NotNull Map<String, String> decryptCallbackData(@NotNull String encryptedCallback,
      int plainDataLength);
}
