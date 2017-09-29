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

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.payment.service.ComputopService.*;
import static com.celements.payment.service.ComputopServiceRole.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.configuration.ConfigurationSource;

import com.celements.common.test.AbstractComponentTest;
import com.celements.payment.container.EncryptedComputopData;
import com.celements.payment.exception.ComputopCryptoException;
import com.celements.payment.service.ComputopService.ReturnUrl;
import com.xpn.xwiki.web.Utils;

public class ComputopServiceTest extends AbstractComponentTest {

  private static final String DEFAULT_HMAC_TEST_KEY = "ComputopSecretHmacKey!";
  private static final String DEFAULT_HMAC_EXAMPLE_PLAIN_TEXT = "She sells sea shells by the sea shore.";
  private static final String DEFAULT_HMAC_EXAMPLE_HASHED = "5d286cc2c516e86470098ef7da266294fec3897035ba74da1ddcc60bd5732701";

  private static final SecretKey DEFAULT_BLOWFISH_KEY = new SecretKeySpec(
      "16CharKeyLength!".getBytes(), BLOWFISH);
  private static final String DEFAULT_BLOWFISH_PLAIN_TEXT = "Unencrypted plain text!";
  private static final int DEFAULT_BLOWFISH_PLAIN_TEXT_LENGTH = DEFAULT_BLOWFISH_PLAIN_TEXT.length();
  private static final String DEFAULT_BLOWFISH_MATCHING = "4105AAFDC6445BF2EB8242371136F488";
  private static final String DEFAULT_BLOWFISH_COMPUTOP_ENCODED = "4105AAFDC6445BF2EB8242371136F4886413347EEB8731B9";
  private static final String DEFAULT_BLOWFISH_INTERNET_ENCODED = "4105aafdc6445bf2eb8242371136f488a4812b38a821e753";

  private ComputopService service;
  private ConfigurationSource configSrcMock;

  @Before
  public void prepareTest() throws ComponentRepositoryException {
    configSrcMock = registerComponentMock(ConfigurationSource.class);
    service = (ComputopService) Utils.getComponent(ComputopServiceRole.class);
  }

  @Test
  public void testGetAmount() {
    assertEquals("200", service.getFormatedAmountString(200));
    assertEquals("355", service.getFormatedAmountString(355));
    assertEquals("2130", service.getFormatedAmountString(2130));
    assertEquals("51", service.getFormatedAmountString(51));
  }

  @Test
  public void testHashPaymentData() {
    expect(configSrcMock.getProperty(eq(CFG_PROP_HMAC_SECRET_KEY), eq(""))).andReturn(
        DEFAULT_HMAC_TEST_KEY);
    replayDefault();
    assertEquals(DEFAULT_HMAC_EXAMPLE_HASHED, service.hashPaymentData(
        DEFAULT_HMAC_EXAMPLE_PLAIN_TEXT));
    verifyDefault();
  }

  @Test
  public void testGetPaymentDataHmac() {

    String paymentHmac = "927c9bcc5eaee1ce9e904237c1c78ec746e99e14016d8fe1155e508e4b2ae2f8";
    expect(configSrcMock.getProperty(eq(CFG_PROP_HMAC_SECRET_KEY), eq(""))).andReturn(
        DEFAULT_HMAC_TEST_KEY);
    replayDefault();
    assertEquals(paymentHmac, service.getPaymentDataHmac("payId", "transId", "merchantId", 2330,
        "CHF"));
    verifyDefault();
  }

  @Test
  public void testGetPaymentDataHmac_nullField() {

    String paymentHmac = "4c4b41b2f0f5de8c1a30006bfbadb0c1ebd2a320301feb1eed785de5bf6d506b";
    expect(configSrcMock.getProperty(eq(CFG_PROP_HMAC_SECRET_KEY), eq(""))).andReturn(
        DEFAULT_HMAC_TEST_KEY);
    replayDefault();
    assertEquals(paymentHmac, service.getPaymentDataHmac(null, "transId", "merchantId", 2330,
        "CHF"));
    verifyDefault();
  }

  @Test
  public void testIsCallbackHashValid_true() {
    String callbackHmac = "04b7e62d9b0fc4e024edf416317861707261351a71b5fb1f464e11ec2e5d161a";
    expect(configSrcMock.getProperty(eq(CFG_PROP_HMAC_SECRET_KEY), eq(""))).andReturn(
        DEFAULT_HMAC_TEST_KEY);
    replayDefault();
    assertTrue(service.isCallbackHashValid(callbackHmac, "payId", "transId", "merchantId", "payed",
        "123"));
    verifyDefault();
  }

  @Test
  public void testIsCallbackHashValid_false() {
    String callbackHmac = "ffffe62d9b0fc4e024edf416317861707261351a71b5fb1f464e11ec2e5d161a";
    expect(configSrcMock.getProperty(eq(CFG_PROP_HMAC_SECRET_KEY), eq(""))).andReturn(
        DEFAULT_HMAC_TEST_KEY);
    replayDefault();
    assertFalse(service.isCallbackHashValid(callbackHmac, "payId", "transId", "merchantId", "payed",
        "123"));
    verifyDefault();
  }

  @Test
  public void testBlowfishEncrypt() throws ComputopCryptoException {
    assertTrue(service.encryptString(DEFAULT_BLOWFISH_PLAIN_TEXT.getBytes(),
        DEFAULT_BLOWFISH_KEY).startsWith(DEFAULT_BLOWFISH_MATCHING.toUpperCase()));
  }

  @Test
  public void testBlowfishDecrypt_computopExampleImplementationEncrypted()
      throws ComputopCryptoException {
    assertEquals(DEFAULT_BLOWFISH_PLAIN_TEXT, new String(service.decryptString(
        new EncryptedComputopData(DEFAULT_BLOWFISH_COMPUTOP_ENCODED,
            DEFAULT_BLOWFISH_PLAIN_TEXT_LENGTH), DEFAULT_BLOWFISH_KEY)));
  }

  @Test
  public void testBlowfishDecrypt_onlineToolEncrypted() throws ComputopCryptoException {
    assertEquals(DEFAULT_BLOWFISH_PLAIN_TEXT, new String(service.decryptString(
        new EncryptedComputopData(DEFAULT_BLOWFISH_INTERNET_ENCODED,
            DEFAULT_BLOWFISH_PLAIN_TEXT_LENGTH), DEFAULT_BLOWFISH_KEY)));
  }

  @Test
  public void testBlowfishEncryptDecryptCycle() throws ComputopCryptoException {
    String encrypted = service.encryptString(DEFAULT_BLOWFISH_PLAIN_TEXT.getBytes(),
        DEFAULT_BLOWFISH_KEY);
    assertEquals(DEFAULT_BLOWFISH_PLAIN_TEXT, new String(service.decryptString(
        new EncryptedComputopData(encrypted, DEFAULT_BLOWFISH_PLAIN_TEXT_LENGTH),
        DEFAULT_BLOWFISH_KEY)));
  }

  @Test
  public void testGetPaymentDataPlainString() throws Exception {
    String merchantId = "merchant";
    String transactionId = "tid";
    String orderDescription = "1.35 meter of kilogram per hour";
    int amount = 3250;
    String currency = "EUR";
    String successUrl = "https://server/success?test=x";
    String failureUrl = "https://server/failure";
    String notifyUrl = "https://server/notify?callback=1";
    expect(configSrcMock.getProperty(eq(CFG_PROP_HMAC_SECRET_KEY), eq(""))).andReturn(
        DEFAULT_HMAC_TEST_KEY).atLeastOnce();
    expect(configSrcMock.getProperty(eq(CFG_PROP_MERCHANT_ID), eq(""))).andReturn(merchantId);
    expect(configSrcMock.getProperty(eq(ReturnUrl.SUCCESS.getValue()), eq(""))).andReturn(
        successUrl);
    expect(configSrcMock.getProperty(eq(ReturnUrl.FAILURE.getValue()), eq(""))).andReturn(
        failureUrl);
    expect(configSrcMock.getProperty(eq(ReturnUrl.CALLBACK.getValue()), eq(""))).andReturn(
        notifyUrl);
    replayDefault();
    String hmac = service.getPaymentDataHmac(null, transactionId, merchantId, amount, currency);
    assertEquals(FORM_INPUT_NAME_MERCHANT_ID + "=" + merchantId + "&" + FORM_INPUT_NAME_TRANS_ID
        + "=" + transactionId + "&" + FORM_INPUT_NAME_AMOUNT + "=" + amount + "&"
        + FORM_INPUT_NAME_CURRENCY + "=" + currency + "&" + FORM_INPUT_NAME_DESCRIPTION + "="
        + orderDescription + "&" + FORM_INPUT_NAME_HMAC + "=" + hmac + "&"
        + ReturnUrl.SUCCESS.getParamName() + "=" + successUrl + "&"
        + ReturnUrl.FAILURE.getParamName() + "=" + failureUrl + "&"
        + ReturnUrl.CALLBACK.getParamName() + "=" + notifyUrl + "&",
        service.getPaymentDataPlainString(transactionId, orderDescription, amount, currency));
    verifyDefault();
  }

}
