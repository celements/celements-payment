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

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.context.Execution;

import com.celements.common.test.AbstractBridgedComponentTestCase;
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
  }

  @Test
  public void testStorePayPalCallback() throws Exception {
    expect(mockRequest.get(eq("txn_id"))).andReturn("1GM85317KV049841K").atLeastOnce();
    Capture<PayPal> payPalObjCapture = new Capture<PayPal>();
    mockPayPalService.storePayPalObject(capture(payPalObjCapture), eq(true));
    expectLastCall().once();
    replayAll();
    paypalScriptService.storePayPalCallback();
    PayPal payPalObj = payPalObjCapture.getValue();
    assertNotNull(payPalObj);
    verifyAll();
  }

  @Test
  public void testCreatePayPalObjFromRequest() {
    //TODO
    replayAll();
    assertNotNull(paypalScriptService.createPayPalObjFromRequest());
    verifyAll();
  }


  private void replayAll(Object ... mocks) {
    replay(xwiki, mockRequest, mockPayPalService);
    replay(mocks);
  }

  private void verifyAll(Object ... mocks) {
    verify(xwiki, mockRequest, mockPayPalService);
    verify(mocks);
  }

}
