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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;

import com.celements.payment.raw.PayPal;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

@Component("payPal")
public class PayPalScriptService implements ScriptService {

  private static Log LOGGER = LogFactory.getFactory().getInstance(
      PayPalScriptService.class);

  @Requirement
  IPayPalService payPalService;

  @Requirement
  Execution execution;

  private XWikiContext getContext() {
    return (XWikiContext)execution.getContext().getProperty("xwikicontext");
  }

  public void storePayPalCallback() {
    String txnId = getContext().getRequest().get("txn_id");
    LOGGER.info("received paypal callback with txn_id [" + txnId + "].");
    PayPal payPalObj = createPayPalObjFromRequest();
    try {
      payPalService.storePayPalObject(payPalObj, true);
    } catch (XWikiException exp) {
      LOGGER.error("Failed to store paypal object for txn_id [" + txnId + "].", exp);
    }
  }

  PayPal createPayPalObjFromRequest() {
    PayPal payPalObj = new PayPal();
    // TODO fill date from Request into PayPal object
    return payPalObj;
  }

}
