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
package com.celements.payment.service;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.context.Execution;

import com.celements.common.test.AbstractBridgedComponentTestCase;
import com.celements.payment.IPaymentService;
import com.celements.payment.raw.EProcessStatus;
import com.celements.payment.raw.PayPal;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

public class PayPalScriptServiceTest extends AbstractBridgedComponentTestCase {

  private PayPalScriptService paypalScriptService;
  private XWiki xwiki;
  private XWikiContext context;
  private XWikiRequest mockRequest;
  private IPayPalService mockPayPalService;
  private IPaymentService mockPaymentService;

  @Before
  public void setUp_PayPalScriptServiceTest() throws Exception {
    context = getContext();
    xwiki = createMock(XWiki.class);
    context.setWiki(xwiki);
    mockRequest = createMock(XWikiRequest.class);
    context.setRequest(mockRequest);
    paypalScriptService = new PayPalScriptService();
    paypalScriptService.execution = getComponentManager().lookup(Execution.class);
    mockPayPalService = createMock(IPayPalService.class);
    paypalScriptService.payPalService = mockPayPalService;
    mockPaymentService = createMock(IPaymentService.class);
    paypalScriptService.paymentService = mockPaymentService;
  }

  @Test
  public void testGetOrigMessage_singleString() {
    Map<String, String[]> paramMap = new HashMap<>();
    paramMap.put("a", new String[] { "b" });
    paramMap.put("c", new String[] { "d" });
    expect(mockRequest.getParameterMap()).andReturn(paramMap).anyTimes();
    replayAll();
    String origMessage = paypalScriptService.getOrigMessage(mockRequest);
    assertTrue(origMessage, origMessage.contains("a=b\n"));
    assertTrue(origMessage, origMessage.contains("c=d\n"));
    verifyAll();
  }

  @Test
  public void testGetOrigMessage_multipleString() {
    Map<String, String[]> paramMap = new HashMap<>();
    paramMap.put("a", new String[] { "b" });
    paramMap.put("c", new String[] { "d", "e" });
    paramMap.put("f", new String[] { "g" });
    expect(mockRequest.getParameterMap()).andReturn(paramMap).anyTimes();
    replayAll();
    String origMessage = paypalScriptService.getOrigMessage(mockRequest);
    assertTrue(origMessage, origMessage.contains("a=b\n"));
    assertTrue(origMessage, origMessage.contains("c=[d, e]\n"));
    assertTrue(origMessage, origMessage.contains("f=g\n"));
    verifyAll();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testStorePayPalCallback() throws Exception {
    String txnId = "1GM85317KV049841K";
    expect(mockRequest.get(eq("txn_id"))).andReturn(txnId).atLeastOnce();
    expect(mockRequest.get(isA(String.class))).andReturn(null).anyTimes();
    Map<String, String[]> paramMap = new HashMap<>();
    expect(mockRequest.getParameterMap()).andReturn(paramMap).anyTimes();
    Capture<PayPal> payPalObjCapture = newCapture();
    mockPayPalService.storePayPalObject(capture(payPalObjCapture), eq(true));
    expectLastCall().once();
    expect(mockRequest.getHeaderNames()).andReturn(Collections.enumeration(
        Collections.emptyList())).anyTimes();
    mockPaymentService.executePaymentAction(isA(Map.class));
    expectLastCall().once();
    replayAll();
    paypalScriptService.storePayPalCallback();
    PayPal payPalObj = payPalObjCapture.getValue();
    assertNotNull(payPalObj);
    assertEquals(txnId, payPalObj.getTxn_id());
    verifyAll();
  }

  @Test
  public void testStorePayPalCallback_no_txnId() throws Exception {
    expect(mockRequest.get(eq("txn_id"))).andReturn(null).atLeastOnce();
    replayAll();
    paypalScriptService.storePayPalCallback();
    verifyAll();
  }

  @Test
  public void testCreatePayPalObjFromRequest() throws Exception {
    String origMessage = "tax1=2.40\n"
        + "residence_country=DE\n"
        + "shipping_discount=0.00\n"
        + "num_cart_items=1\n"
        + "invoice=913774\n"
        + "address_city=Musterort\n"
        + "payer_id=SLMPXMKUWVF44\n"
        + "first_name=Philipp\n"
        + "txn_id=1GM85317KV049841K\n"
        + "receiver_email=dealer_1332258454_biz@synventis.com\n"
        + "custom=Weitere Infos die von Paypal nur weitergeleitet werden...\n"
        + "payment_date=01:46:06 Apr 24, 2012 PDT\n"
        + "charset=windows-1252\n"
        + "address_country_code=DE\n"
        + "payment_gross=\n"
        + "address_zip=1234\n"
        + "item_name1=Ihre Bestellung ABC123\n"
        + "ipn_track_id=665a591250dc1\n"
        + "discount=2.50\n"
        + "mc_handling=3.00\n"
        + "mc_handling1=0.00\n"
        + "tax=2.20\n"
        + "address_name=Peter Muster\n"
        + "last_name=Buser\n"
        + "receiver_id=WG2M8GUTB5HPL\n"
        + "shipping_method=Default\n"
        + "verify_sign=A0RW9Ox1hgP6eQ-urjHfAlccEDbcAizs6d3oEIDBHcTw5Hn27yDD0WIT\n"
        + "insurance_amount=0.00\n"
        + "address_country=Germany\n"
        + "address_status=unconfirmed\n"
        + "business=dealer_1332258454_biz@synventis.com\n"
        + "payment_status=Pending\n"
        + "transaction_subject=Weitere Infos die von Paypal nur weitergeleitet werden...\n"
        + "protection_eligibility=Eligible\n"
        + "payer_email=client_1332258558_pre@synventis.com\n"
        + "notify_version=3.4\n"
        + "txn_type=cart\n"
        + "mc_gross=33.70\n"
        + "payer_status=verified\n"
        + "mc_currency=CHF\n"
        + "mc_shipping=1.00\n"
        + "test_ipn=1\n"
        + "mc_gross_1=31.00\n"
        + "mc_shipping1=1.00\n"
        + "item_number1=ABC123\n"
        + "quantity1=1\n"
        + "address_state=\n"
        + "pending_reason=multi_currency\n"
        + "payment_type=instant\n"
        + "address_street=Musterstrasse 12";
    addToRequest(origMessage);
    expect(mockRequest.get(eq("reason_code"))).andReturn(null).anyTimes();
    expect(mockRequest.getHeaderNames()).andReturn(Collections.enumeration(
        Arrays.asList("Content-Type", "Character-Encoding"))).anyTimes();
    expect(mockRequest.getHeaders(eq("Content-Type"))).andReturn(Collections.enumeration(
        Arrays.asList("abcd"))).anyTimes();
    expect(mockRequest.getHeaders(eq("Character-Encoding"))).andReturn(
        Collections.enumeration(Arrays.asList("defg"))).anyTimes();
    Date paymentDate = new SimpleDateFormat("HH:mm:ss MMM dd, yyyy zz").parse(
        "01:46:06 Apr 24, 2012 PDT");
    replayAll();
    PayPal payPalObj = paypalScriptService.createPayPalObjFromRequest();
    assertNotNull(payPalObj);
    assertEquals("1GM85317KV049841K", payPalObj.getTxn_id());
    assertEquals("cart", payPalObj.getTxn_type());
    assertEquals(paymentDate, payPalObj.getPayment_date());
    String headerLine = "Content-Type=abcd\n";
    assertTrue("not found [" + headerLine + "] in [" + payPalObj.getOrigHeader() + "].",
        payPalObj.getOrigHeader().contains(headerLine));
    headerLine = "Character-Encoding=defg\n";
    assertTrue("not found [" + headerLine + "] in [" + payPalObj.getOrigHeader() + "].",
        payPalObj.getOrigHeader().contains(headerLine));
    for (String line : origMessage.split("\n")) {
      assertTrue("not found [" + line + "] in [" + payPalObj.getOrigMessage() + "].",
          payPalObj.getOrigMessage().contains(line));
    }
    assertEquals("SLMPXMKUWVF44", payPalObj.getPayerId());
    assertEquals("WG2M8GUTB5HPL", payPalObj.getReceiverId());
    assertEquals("Pending", payPalObj.getPaymentStatus());
    assertEquals("multi_currency", payPalObj.getPending_reason());
    assertEquals("", payPalObj.getReason_code());
    assertEquals("A0RW9Ox1hgP6eQ-urjHfAlccEDbcAizs6d3oEIDBHcTw5Hn27yDD0WIT",
        payPalObj.getVerify_sign());
    assertEquals("913774", payPalObj.getInvoice());
    assertEquals(EProcessStatus.New, payPalObj.getProcessStatus());
    verifyAll();
  }

  private void addToRequest(String origMessage) {
    Map<String, String[]> paramMap = new HashMap<>();
    for (String line : origMessage.split("\n")) {
      String[] pair = line.split("=");
      String value = "";
      if (pair.length > 1) {
        value = pair[1];
      }
      expect(mockRequest.get(eq(pair[0]))).andReturn(value).anyTimes();
      expect(mockRequest.getParameter(eq(pair[0]))).andReturn(value).anyTimes();
      String[] params = new String[] { value };
      paramMap.put(pair[0], params);
    }
    expect(mockRequest.getParameterMap()).andReturn(paramMap);
  }

  private void replayAll(Object... mocks) {
    replay(xwiki, mockRequest, mockPayPalService, mockPaymentService);
    replay(mocks);
  }

  private void verifyAll(Object... mocks) {
    verify(xwiki, mockRequest, mockPayPalService, mockPaymentService);
    verify(mocks);
  }

}
