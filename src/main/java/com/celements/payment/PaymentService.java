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
package com.celements.payment;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.context.ModelContext;
import com.celements.payment.exception.PaymentException;
import com.celements.payment.raw.PaymentRawObject;
import com.celements.web.plugin.api.CelementsWebPluginApi;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.web.XWikiRequest;

@Component
public class PaymentService implements IPaymentService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);

  @Requirement
  private ModelContext context;

  @Deprecated
  private XWikiContext getXWikiContext() {
    return context.getXWikiContext();
  }

  @Override
  public void executePaymentAction(Map<String, String[]> data) {
    try {
      DocumentReference callbackDocRef = new DocumentReference(getXWikiContext().getDatabase(),
          "Payment", "CallbackActions");
      if (getXWikiContext().getWiki().exists(callbackDocRef, getXWikiContext())) {
        XWikiDocument actionXdoc = getXWikiContext().getWiki().getDocument(callbackDocRef,
            getXWikiContext());
        Document actionDoc = actionXdoc.newDocument(getXWikiContext());
        CelementsWebPluginApi celementsweb = (CelementsWebPluginApi) getXWikiContext().getWiki().getPluginApi(
            "celementsweb", getXWikiContext());
        celementsweb.getPlugin().executeAction(actionDoc, data, actionXdoc, getXWikiContext());
      } else {
        LOGGER.warn("Failed to execute payment action because Payment.CallbackActions"
            + " does not exist in [" + getXWikiContext().getDatabase() + "].");
      }
    } catch (XWikiException e) {
      LOGGER.error("Exeption while creating callback object", e);
    }
  }

  @Override
  public void storePaymentObject(final PaymentRawObject paymentObj) throws PaymentException {
    try {
      getHibStore().executeWrite(context.getXWikiContext(), true, new HibernateCallback<Void>() {

        @Override
        public Void doInHibernate(Session session) throws HibernateException {
          session.saveOrUpdate(paymentObj);
          return null;
        }
      });
      LOGGER.info("storePaymentObject: '{}'", paymentObj);
    } catch (XWikiException xwe) {
      throw new PaymentException(xwe);
    }
  }

  @Override
  public String serializeHeaderFromRequest() {
    StringBuilder sb = new StringBuilder();
    if (context.getRequest().isPresent()) {
      XWikiRequest request = context.getRequest().get();
      Enumeration<?> headerNames = request.getHeaderNames();
      while (headerNames.hasMoreElements()) {
        String key = headerNames.nextElement().toString();
        Enumeration<?> headers = request.getHeaders(key);
        List<String> values = Lists.transform(Collections.list(headers),
            Functions.toStringFunction());
        addParamToStringBuilder(sb, key, values);
      }
    }
    return sb.toString();
  }

  @Override
  @SuppressWarnings("unchecked")
  public String serializeParameterMapFromRequest() {
    StringBuilder sb = new StringBuilder();
    if (context.getRequest().isPresent()) {
      Map<String, String[]> parameterMap = context.getRequest().get().getParameterMap();
      if (parameterMap != null) {
        for (String key : parameterMap.keySet()) {
          addParamToStringBuilder(sb, key, Arrays.asList(parameterMap.get(key)));
        }
      }
    }
    return sb.toString();
  }

  private void addParamToStringBuilder(StringBuilder sb, String key, List<String> values) {
    sb.append(key).append("=");
    if (values.size() == 1) {
      sb.append(values.get(0));
    } else if (values.size() > 1) {
      sb.append(values);
    }
    sb.append("\n");
  }

  XWikiHibernateStore getHibStore() {
    return context.getXWikiContext().getWiki().getHibernateStore();
  }

}
