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

public class PayPalTest extends AbstractBridgedComponentTestCase {

  private XWikiContext context;
  private XWiki xwiki;
  private PayPal payPalObj;

  @Before
  public void setUp_PayPalTest() throws Exception {
    context = getContext();
    xwiki = createMock(XWiki.class);
    context.setWiki(xwiki);
    payPalObj = new PayPal();
  }

  @Test
  public void testGetSetTxn_id() {
    replayAll();
    assertNull(payPalObj.getTxn_id());
    payPalObj.setTxn_id(null);
    assertNull(payPalObj.getTxn_id());
    payPalObj.setTxn_id("test1");
    assertEquals("test1", payPalObj.getTxn_id());
    verifyAll();
  }

  @Test
  public void testGetSetTxn_type() {
    replayAll();
    assertEquals("", payPalObj.getTxn_type());
    payPalObj.setTxn_type(null);
    assertEquals("", payPalObj.getTxn_type());
    payPalObj.setTxn_type("test1");
    assertEquals("test1", payPalObj.getTxn_type());
    verifyAll();
  }

  @Test
  public void testGetSetPending_reason() {
    replayAll();
    assertEquals("", payPalObj.getPending_reason());
    payPalObj.setPending_reason(null);
    assertEquals("", payPalObj.getPending_reason());
    payPalObj.setPending_reason("test1");
    assertEquals("test1", payPalObj.getPending_reason());
    verifyAll();
  }

  @Test
  public void testGetSetReason_code() {
    replayAll();
    assertEquals("", payPalObj.getReason_code());
    payPalObj.setReason_code(null);
    assertEquals("", payPalObj.getReason_code());
    payPalObj.setReason_code("test1");
    assertEquals("test1", payPalObj.getReason_code());
    verifyAll();
  }

  @Test
  public void testGetSetPayment_date() {
    replayAll();
    assertNull(payPalObj.getPayment_date());
    payPalObj.setPayment_date(null);
    assertNull(payPalObj.getPayment_date());
    Date myDate = new Date();
    payPalObj.setPayment_date(myDate);
    assertEquals(myDate, payPalObj.getPayment_date());
    verifyAll();
  }

  @Test
  public void testGetSetOrigHeader() {
    replayAll();
    assertEquals("", payPalObj.getOrigHeader());
    payPalObj.setOrigHeader(null);
    assertEquals("", payPalObj.getOrigHeader());
    payPalObj.setOrigHeader("test1");
    assertEquals("test1", payPalObj.getOrigHeader());
    verifyAll();
  }

  @Test
  public void testGetSetOrigMessage() {
    replayAll();
    assertEquals("", payPalObj.getOrigMessage());
    payPalObj.setOrigMessage(null);
    assertEquals("", payPalObj.getOrigMessage());
    payPalObj.setOrigMessage("test1");
    assertEquals("test1", payPalObj.getOrigMessage());
    verifyAll();
  }

  @Test
  public void testGetSetPayerId() {
    replayAll();
    assertEquals("", payPalObj.getPayerId());
    payPalObj.setPayerId(null);
    assertEquals("", payPalObj.getPayerId());
    payPalObj.setPayerId("test1");
    assertEquals("test1", payPalObj.getPayerId());
    verifyAll();
  }

  @Test
  public void testGetSetReceiverId() {
    replayAll();
    assertEquals("", payPalObj.getReceiverId());
    payPalObj.setReceiverId(null);
    assertEquals("", payPalObj.getReceiverId());
    payPalObj.setReceiverId("test1");
    assertEquals("test1", payPalObj.getReceiverId());
    verifyAll();
  }

  @Test
  public void testGetSetPaymentStatus() {
    replayAll();
    assertEquals("", payPalObj.getPaymentStatus());
    payPalObj.setPaymentStatus(null);
    assertEquals("", payPalObj.getPaymentStatus());
    payPalObj.setPaymentStatus("test1");
    assertEquals("test1", payPalObj.getPaymentStatus());
    verifyAll();
  }

  @Test
  public void testGetSetVerify_sign() {
    replayAll();
    assertEquals("", payPalObj.getVerify_sign());
    payPalObj.setVerify_sign(null);
    assertEquals("", payPalObj.getVerify_sign());
    payPalObj.setVerify_sign("test1");
    assertEquals("test1", payPalObj.getVerify_sign());
    verifyAll();
  }

  @Test
  public void testGetSetInvoice() {
    replayAll();
    assertEquals("", payPalObj.getInvoice());
    payPalObj.setInvoice(null);
    assertEquals("", payPalObj.getInvoice());
    payPalObj.setInvoice("test1");
    assertEquals("test1", payPalObj.getInvoice());
    verifyAll();
  }

  @Test
  public void testGetSetProcessStatus() {
    replayAll();
    assertEquals(EProcessStatus.Unknown, payPalObj.getProcessStatus());
    payPalObj.setProcessStatus(null);
    assertEquals(EProcessStatus.Unknown, payPalObj.getProcessStatus());
    payPalObj.setProcessStatus(EProcessStatus.New);
    assertEquals(EProcessStatus.New, payPalObj.getProcessStatus());
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
