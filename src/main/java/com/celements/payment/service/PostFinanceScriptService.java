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
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryManager;
import org.xwiki.script.service.ScriptService;

import com.celements.payment.IPaymentService;
import com.celements.payment.PaymentService;
import com.celements.payment.raw.EProcessStatus;
import com.celements.payment.raw.PostFinance;
import com.celements.web.service.IWebUtilsService;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiRequest;

@Component("postFinance")
@Singleton
public class PostFinanceScriptService implements ScriptService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PostFinanceScriptService.class);

  @Requirement
  IPostFinanceService postFinanceService;

  @Requirement
  IPaymentService paymentService;

  @Requirement
  IWebUtilsService webUtils;

  @Requirement
  Execution execution;

  @Requirement
  QueryManager queryManager;

  private XWikiContext getContext() {
    return (XWikiContext) execution.getContext().getProperty("xwikicontext");
  }

  private SimpleDateFormat paymentDateFormat = new SimpleDateFormat("MM/dd/yy");

  public void storePostFinanceCallback() {
    String txnId = getNewTxnId();
    LOGGER.info("received PostFinance callback with txn_id [" + txnId + "].");
    if ((txnId != null) && (!"".equals(txnId))) {
      PostFinance PostFinanceObj = createPostFinanceObjFromRequest(txnId);
      try {
        postFinanceService.storePostFinanceObject(PostFinanceObj, true);
        // FIXME move execution of callbackAction to general async processing of callback
        executeCallbackAction(getContext().getRequest().getParameterMap());
      } catch (XWikiException exp) {
        LOGGER.error("Failed to store PostFinance object for txn_id [" + txnId + "].", exp);
      }
    }
  }

  private String getNewTxnId() {
    // XXX What to use for txnId? Postfinance does not provide any...
    return getRequestParam("orderID") + "-" + (new Date().getTime() / 1000 / 60);
  }

  public boolean reExecuteCallbackActionForTxn(String txnId) {
    try {
      PostFinance PostFinanceObj = postFinanceService.loadPostFinanceObject(txnId);
      executeCallbackAction(convertToMap(PostFinanceObj.getOrigMessage()));
      return true;
    } catch (XWikiException exp) {
      LOGGER.error("Failed to load/create PostFinance object.", exp);
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  public void executeCallbackAction(Map parameterMap) {
    Map<String, String[]> data = new HashMap<>();
    data.putAll(parameterMap);
    if (parameterMap.containsKey("custom")) {
      String customValue = data.get("custom")[0];
      if ((customValue != null) && (!"".equals(customValue))) {
        // shoppingCartDoc.fullName;$user
        String[] customValueSplit = customValue.split(";");
        if (customValueSplit.length > 1) {
          String cartDocFN = customValueSplit[0];
          String user = customValueSplit[1];
          data.put("cartUser", new String[] { user });
          getContext().setUser(user);
          try {
            XWikiDocument userDoc = getContext().getWiki().getDocument(
                webUtils.resolveDocumentReference(user), getContext());
            BaseObject userObj = userDoc.getXObject(new DocumentReference(
                getContext().getDatabase(), "XWiki", "XWikiUsers"));
            data.put("userEmail", new String[] { userObj.getStringValue("email") });
          } catch (XWikiException exp) {
            LOGGER.error("Failed to get userdoc for [" + user + "]. Possibly failing to"
                + " send any callback emails.", exp);
          }
          data.put("cartDocFN", new String[] { cartDocFN });
        } else {
          LOGGER.warn("illegal custom value [" + customValue + "] found."
              + " Failed to reconstruct cart payed.");
        }
      } else {
        LOGGER.warn("no custom value found to reconstruct cart payed.");
      }
    }
    paymentService.executePaymentAction(data);
  }

  PostFinance createPostFinanceObjFromRequest(String txnId) {
    PostFinance postFinanceObj = new PostFinance();
    postFinanceObj.setTxn_id(txnId);
    postFinanceObj.setPaymentMethod(getRequestParam("PM"));
    String payment_date = getRequestParam("TRXDATE");
    if (payment_date != null) {
      try {
        postFinanceObj.setTrxDate(paymentDateFormat.parse(payment_date));
      } catch (ParseException exp) {
        LOGGER.error("Failed to parse payment date [" + getRequestParam("payment_date")
            + "] for txn_id [" + txnId + "].", exp);
      }
    }
    postFinanceObj.setOrigHeader(getOrigHeader(getContext().getRequest()));
    postFinanceObj.setOrigMessage(getOrigMessage(getContext().getRequest()));
    postFinanceObj.setPayId(getRequestParam("PAYID"));
    postFinanceObj.setCardNumber(getRequestParam("CARDNO"));
    postFinanceObj.setExpDate(getRequestParam("ED"));
    postFinanceObj.setClientIP(getRequestParam("IP"));
    postFinanceObj.setCardholder(getRequestParam("CN"));
    postFinanceObj.setCardbrand(getRequestParam("BRAND"));
    postFinanceObj.setAcceptance(getRequestParam("ACCEPTANCE"));
    postFinanceObj.setPaymentStatus(getRequestParam("STATUS"));
    postFinanceObj.setNcError(getRequestParam("NCERROR"));
    postFinanceObj.setCurrency(getRequestParam("currency"));
    postFinanceObj.setAmount(getRequestParam("amount"));
    postFinanceObj.setShasign(getRequestParam("SHASIGN"));
    postFinanceObj.setOrderId(getRequestParam("orderID"));
    postFinanceObj.setProcessStatus(EProcessStatus.New);
    return postFinanceObj;
  }

  private String getRequestParam(String key) {
    return getContext().getRequest().get(key);
  }

  /**
   * @deprecated instead use {@link PaymentService#serializeHeaderFromRequest()}
   */
  @Deprecated
  @SuppressWarnings("unchecked")
  String getOrigHeader(XWikiRequest request) {
    StringBuffer origHeaderBuffer = new StringBuffer();
    Enumeration headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String key = (String) headerNames.nextElement();
      List<String> valueList = Collections.list(request.getHeaders(key));
      String[] values = valueList.toArray(new String[0]);
      addParamToStringBuffer(origHeaderBuffer, key, values);
    }
    return origHeaderBuffer.toString();
  }

  /**
   * @deprecated instead use {@link PaymentService#serializeParameterMapFromRequest()}
   */
  @Deprecated
  @SuppressWarnings("unchecked")
  String getOrigMessage(XWikiRequest request) {
    StringBuffer origMessageBuffer = new StringBuffer();
    Map<String, String[]> parameterMap = request.getParameterMap();
    if (parameterMap != null) {
      for (String key : parameterMap.keySet()) {
        String[] values = parameterMap.get(key);
        addParamToStringBuffer(origMessageBuffer, key, values);
      }
    }
    return origMessageBuffer.toString();
  }

  private void addParamToStringBuffer(StringBuffer stringBuffer, String key, String[] values) {
    String valueStr = "";
    if (values.length == 1) {
      valueStr = values[0];
    } else {
      valueStr = Arrays.deepToString(values);
    }
    stringBuffer.append(key + "=" + valueStr + "\n");
  }

  private Map<String, String[]> convertToMap(String origMessage) {
    Map<String, String[]> paramMap = new HashMap<>();
    for (String line : origMessage.split("\n")) {
      String[] pair = line.split("=");
      String value = "";
      if (pair.length > 1) {
        value = pair[1];
      }
      String[] params = new String[] { value };
      paramMap.put(pair[0], params);
    }
    return paramMap;
  }

}
