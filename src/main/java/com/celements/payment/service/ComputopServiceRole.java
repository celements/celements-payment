package com.celements.payment.service;

import java.math.BigDecimal;

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

}
