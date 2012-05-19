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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;

import com.celements.payment.raw.PayPal;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiRequest;

@Component("payPal")
public class PayPalScriptService implements ScriptService {

  private static Log LOGGER = LogFactory.getFactory().getInstance(
      PayPalScriptService.class);

  @Requirement
  IPayPalService payPalService;

  @Requirement
  Execution execution;

  private SimpleDateFormat paymentDateFormat = new SimpleDateFormat(
      "HH:mm:ss MMM dd, yyyy zz");

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
    XWikiRequest request = getContext().getRequest();
    String txnId = request.get("txn_id");
    payPalObj.setTxn_id(txnId);
    payPalObj.setInvoice(request.get("invoice"));
    payPalObj.setPayerId(request.get("payer_id"));
    String payment_date = request.get("payment_date");
    if (payment_date != null) {
      try {
        payPalObj.setPayment_date(paymentDateFormat.parse(payment_date));
      } catch (ParseException exp) {
        LOGGER.error("Failed to parse payment date [" + request.get("payment_date")
            + "] for txn_id [" + txnId + "].", exp);
      }
    }
    payPalObj.setOrigMessage(getOrigMessage(request));
    // TODO fill date from Request into PayPal object
    return payPalObj;
  }

  @SuppressWarnings("unchecked")
  String getOrigMessage(XWikiRequest request) {
    StringBuffer origMessageBuffer = new StringBuffer();
    Map<String, String[]> parameterMap = request.getParameterMap();
    if (parameterMap != null) {
      for (String key : parameterMap.keySet()) {
        String[] values = parameterMap.get(key);
        String valueStr = "";
        if (values.length == 1) {
          valueStr = values[0];
        } else {
          valueStr = Arrays.deepToString(values);
        }
        origMessageBuffer.append(key + "=" + valueStr + "\n");
      }
    }
    return origMessageBuffer.toString();
  }

}
