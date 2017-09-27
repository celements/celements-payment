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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.celements.payment.container.EncryptedComputopData;
import com.celements.payment.exception.ComputopCryptoException;
import com.celements.payment.raw.Computop;
import com.xpn.xwiki.XWikiException;

@ComponentRole
public interface ComputopServiceRole {

  public static final String FORM_INPUT_NAME_LENGTH = "Len";
  public static final String FORM_INPUT_NAME_DATA = "Data";
  public static final String FORM_INPUT_NAME_MERCHANT_ID = "MerchantID";
  public static final String FORM_INPUT_NAME_TRANS_ID = "TransID";
  public static final String FORM_INPUT_NAME_AMOUNT = "Amount";
  public static final String FORM_INPUT_NAME_CURRENCY = "Currency";
  public static final String FORM_INPUT_NAME_DESCRIPTION = "OrderDesc";
  public static final String FORM_INPUT_NAME_HMAC = "MAC";

  public static final String PAYER_RETURN_REQUEST_NAME_HMAC = "MAC";
  public static final String PAYER_RETURN_REQUEST_NAME_PAY_ID = "PayID";
  public static final String PAYER_RETURN_REQUEST_NAME_MERCHANT_ID = "mid";
  public static final String PAYER_RETURN_REQUEST_NAME_TRANS_ID = "TransID";
  public static final String PAYER_RETURN_REQUEST_NAME_STATUS = "Status";
  public static final String PAYER_RETURN_REQUEST_NAME_CODE = "Code";
  public static final String PAYER_RETURN_REQUEST_NAME_DESCRIPTION = "Description";
  public static final String PAYER_RETURN_REQUEST_NAME_TYPE = "Type";
  public static final String PAYER_RETURN_REQUEST_NAME_XID = "XID";

  public static final String DEFAULT_CURRENCY = "CHF";

  public static final String MERCHANT_ID_PROP = "computop_merchant_id";
  public static final String COMPUTOP_PAYFORM_ACTION_URL = "computop_paymform_action_url";
  public static final String CELEMENTS_PAYFORM_ACTION_URL = "celements_paymform_action_url";

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
   * Returns an EncryptedComputopData object with the form parameters 'Len' and 'Data' to transmit
   * to computop when checking out. The 'data' map entry contains the encrypted version of the
   * following fields:
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
   * @param orderDescription
   *          A text description of the order the payment belongs to
   * @param amount
   *          Amount to be payed
   * @param currency
   *          Currency of the amount (default 'CHF')
   * @return EncryptedComputopData containing plain text length and encrypted text parameters to be
   *         transmitted with checkout form
   * @throws ComputopCryptoException
   *           thrown if encryption fails with an Exception
   */
  public @NotNull EncryptedComputopData encryptPaymentData(@NotNull String transactionId,
      @Nullable String orderDescription, @NotNull BigDecimal amount, @Nullable String currency)
      throws ComputopCryptoException;

  /**
   * Deciphers callback data and returns all contained data in a map.
   * Attention:
   * - All the map key names are lower case. (Computop asks in their documentation to ignore case)
   * - The content of the map can change if Computop adds or removes parameters from their callback.
   *
   * @param encryptedCallback
   *          Encrypted data received as callback for a Computop payment
   * @return Map containing all parameters received in a Computop payment callback. Keys in lower
   *         case
   * @throws ComputopCryptoException
   *           thrown if decryption fails with an Exception
   */
  public @NotNull Map<String, String> decryptCallbackData(
      @NotNull EncryptedComputopData encryptedData) throws ComputopCryptoException;

  String getMerchantId();

  SpaceReference getOrderSpaceRef();

  DocumentReference getOrderDocRef(String transactionId);

  void storeCallback() throws ComputopCryptoException, XWikiException;

  void executeCallbackAction(@NotNull Computop computopObj) throws ComputopCryptoException,
      XWikiException;

}
