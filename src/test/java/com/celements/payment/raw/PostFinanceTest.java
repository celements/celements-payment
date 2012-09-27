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
package com.celements.payment.raw;

import java.util.Date;

import static org.junit.Assert.*;

import static org.easymock.EasyMock.*;

import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractBridgedComponentTestCase;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

public class PostFinanceTest extends AbstractBridgedComponentTestCase {

  private XWikiContext context;
  private XWiki xwiki;
  private PostFinance postfinanceObj;

  @Before
  public void setUp_PayPalTest() throws Exception {
    context = getContext();
    xwiki = createMock(XWiki.class);
    context.setWiki(xwiki);
    postfinanceObj = new PostFinance();
  }

  @Test
  public void testGetSetTxn_id() {
    replayAll();
    assertNull(postfinanceObj.getTxn_id());
    postfinanceObj.setTxn_id(null);
    assertNull(postfinanceObj.getTxn_id());
    postfinanceObj.setTxn_id("test1");
    assertEquals("test1", postfinanceObj.getTxn_id());
    verifyAll();
  }

  @Test
  public void testGetSetPaymentMethod() {
    replayAll();
    assertEquals("", postfinanceObj.getPaymentMethod());
    postfinanceObj.setPaymentMethod(null);
    assertEquals("", postfinanceObj.getPaymentMethod());
    postfinanceObj.setPaymentMethod("test1");
    assertEquals("test1", postfinanceObj.getPaymentMethod());
    verifyAll();
  }

  @Test
  public void testGetSetTrxDate() {
    replayAll();
    assertNull(postfinanceObj.getTrxDate());
    postfinanceObj.setTrxDate(null);
    assertNull(postfinanceObj.getTrxDate());
    Date myDate = new Date();
    postfinanceObj.setTrxDate(myDate);
    assertEquals(myDate, postfinanceObj.getTrxDate());
    verifyAll();
  }

  @Test
  public void testGetSetOrigHeader() {
    replayAll();
    assertEquals("", postfinanceObj.getOrigHeader());
    postfinanceObj.setOrigHeader(null);
    assertEquals("", postfinanceObj.getOrigHeader());
    postfinanceObj.setOrigHeader("test1");
    assertEquals("test1", postfinanceObj.getOrigHeader());
    verifyAll();
  }

  @Test
  public void testGetSetOrigMessage() {
    replayAll();
    assertEquals("", postfinanceObj.getOrigMessage());
    postfinanceObj.setOrigMessage(null);
    assertEquals("", postfinanceObj.getOrigMessage());
    postfinanceObj.setOrigMessage("test1");
    assertEquals("test1", postfinanceObj.getOrigMessage());
    verifyAll();
  }

  @Test
  public void testGetSetPayId() {
    replayAll();
    assertEquals("", postfinanceObj.getPayId());
    postfinanceObj.setPayId(null);
    assertEquals("", postfinanceObj.getPayId());
    postfinanceObj.setPayId("test1");
    assertEquals("test1", postfinanceObj.getPayId());
    verifyAll();
  }

  @Test
  public void testGetSetCardNumber() {
    replayAll();
    assertEquals("", postfinanceObj.getCardNumber());
    postfinanceObj.setCardNumber(null);
    assertEquals("", postfinanceObj.getCardNumber());
    postfinanceObj.setCardNumber("test1");
    assertEquals("test1", postfinanceObj.getCardNumber());
    verifyAll();
  }

  @Test
  public void testGetSetExpDate() {
    replayAll();
    assertEquals("", postfinanceObj.getExpDate());
    postfinanceObj.setExpDate(null);
    assertEquals("", postfinanceObj.getExpDate());
    postfinanceObj.setExpDate("test1");
    assertEquals("test1", postfinanceObj.getExpDate());
    verifyAll();
  }

  @Test
  public void testGetSetClientIP() {
    replayAll();
    assertEquals("", postfinanceObj.getClientIP());
    postfinanceObj.setClientIP(null);
    assertEquals("", postfinanceObj.getClientIP());
    postfinanceObj.setClientIP("test1");
    assertEquals("test1", postfinanceObj.getClientIP());
    verifyAll();
  }

  @Test
  public void testGetSetCardholder() {
    replayAll();
    assertEquals("", postfinanceObj.getCardholder());
    postfinanceObj.setCardholder(null);
    assertEquals("", postfinanceObj.getCardholder());
    postfinanceObj.setCardholder("test1");
    assertEquals("test1", postfinanceObj.getCardholder());
    verifyAll();
  }

  @Test
  public void testGetSetCardbrand() {
    replayAll();
    assertEquals("", postfinanceObj.getCardbrand());
    postfinanceObj.setCardbrand(null);
    assertEquals("", postfinanceObj.getCardbrand());
    postfinanceObj.setCardbrand("test1");
    assertEquals("test1", postfinanceObj.getCardbrand());
    verifyAll();
  }

  @Test
  public void testGetSetAcceptance() {
    replayAll();
    assertEquals("", postfinanceObj.getAcceptance());
    postfinanceObj.setAcceptance(null);
    assertEquals("", postfinanceObj.getAcceptance());
    postfinanceObj.setAcceptance("test1");
    assertEquals("test1", postfinanceObj.getAcceptance());
    verifyAll();
  }

  @Test
  public void testGetSetPaymentStatus() {
    replayAll();
    assertEquals("", postfinanceObj.getPaymentStatus());
    postfinanceObj.setPaymentStatus(null);
    assertEquals("", postfinanceObj.getPaymentStatus());
    postfinanceObj.setPaymentStatus("test1");
    assertEquals("test1", postfinanceObj.getPaymentStatus());
    verifyAll();
  }

  @Test
  public void testGetSetNcError() {
    replayAll();
    assertEquals("", postfinanceObj.getNcError());
    postfinanceObj.setNcError(null);
    assertEquals("", postfinanceObj.getNcError());
    postfinanceObj.setNcError("test1");
    assertEquals("test1", postfinanceObj.getNcError());
    verifyAll();
  }

  @Test
  public void testGetSetCurrency() {
    replayAll();
    assertEquals("", postfinanceObj.getCurrency());
    postfinanceObj.setCurrency(null);
    assertEquals("", postfinanceObj.getCurrency());
    postfinanceObj.setCurrency("test1");
    assertEquals("test1", postfinanceObj.getCurrency());
    verifyAll();
  }

  @Test
  public void testGetSetAmount() {
    replayAll();
    assertEquals("", postfinanceObj.getAmount());
    postfinanceObj.setAmount(null);
    assertEquals("", postfinanceObj.getAmount());
    postfinanceObj.setAmount("test1");
    assertEquals("test1", postfinanceObj.getAmount());
    verifyAll();
  }

  @Test
  public void testGetSetShasign() {
    replayAll();
    assertEquals("", postfinanceObj.getShasign());
    postfinanceObj.setShasign(null);
    assertEquals("", postfinanceObj.getShasign());
    postfinanceObj.setShasign("test1");
    assertEquals("test1", postfinanceObj.getShasign());
    verifyAll();
  }

  @Test
  public void testGetSetOrderId() {
    replayAll();
    assertEquals("", postfinanceObj.getOrderId());
    postfinanceObj.setOrderId(null);
    assertEquals("", postfinanceObj.getOrderId());
    postfinanceObj.setOrderId("test1");
    assertEquals("test1", postfinanceObj.getOrderId());
    verifyAll();
  }

  @Test
  public void testGetSetProcessStatus() {
    replayAll();
    assertEquals(EProcessStatus.Unknown, postfinanceObj.getProcessStatus());
    postfinanceObj.setProcessStatus(null);
    assertEquals(EProcessStatus.Unknown, postfinanceObj.getProcessStatus());
    postfinanceObj.setProcessStatus(EProcessStatus.New);
    assertEquals(EProcessStatus.New, postfinanceObj.getProcessStatus());
    verifyAll();
  }


  private void replayAll(Object ... mocks) {
    replay(xwiki);
    replay(mocks);
  }

  private void verifyAll(Object ... mocks) {
    verify(xwiki);
    verify(mocks);
  }

}
