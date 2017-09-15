package com.celements.payment.service;

import static org.junit.Assert.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;
import com.xpn.xwiki.web.Utils;

public class ComputopServiceTest extends AbstractComponentTest {

  // Default key, plain- and encrypted-text. Encrypted using the Computop example implementation.
  private static SecretKey DEFAULT_KEY = new SecretKeySpec("16CharKeyLength!".getBytes(),
      "Blowfish");
  private static String DEFAULT_PLAIN_TEXT = "Unencrypted plain text!";
  private static int DEFAULT_PLAIN_TEXT_LENGTH = DEFAULT_PLAIN_TEXT.length();
  private static String DEFAULT_ENCODED = "4105aafdc6445bf2eb8242371136f488a4812b38a821e753";

  private ComputopService computopSrv;

  @Before
  public void prepareTest() {
    computopSrv = (ComputopService) Utils.getComponent(ComputopServiceRole.class);
  }

  @Test
  public void testBlowfishEncrypt() {
    assertEquals(DEFAULT_ENCODED.toUpperCase(), computopSrv.encryptString(
        DEFAULT_PLAIN_TEXT.getBytes(), DEFAULT_KEY));
  }

  @Test
  public void testBlowfishDecrypt() {
    assertEquals(DEFAULT_PLAIN_TEXT, new String(computopSrv.decryptString(DEFAULT_ENCODED,
        DEFAULT_PLAIN_TEXT_LENGTH, DEFAULT_KEY)));
  }

  @Test
  public void testBlowfishEncryptDecryptCycle() {
    String encrypted = computopSrv.encryptString(DEFAULT_PLAIN_TEXT.getBytes(), DEFAULT_KEY);
    assertEquals(DEFAULT_PLAIN_TEXT, new String(computopSrv.decryptString(encrypted,
        DEFAULT_PLAIN_TEXT_LENGTH, DEFAULT_KEY)));
  }

}
