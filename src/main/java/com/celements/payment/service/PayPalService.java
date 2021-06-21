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

import org.hibernate.FlushMode;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;

import com.celements.payment.raw.PayPal;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;

@Component
public class PayPalService implements IPayPalService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PayPalService.class);

  @Requirement
  Execution execution;

  private XWikiContext getContext() {
    return (XWikiContext) execution.getContext().getProperty("xwikicontext");
  }

  @Override
  @Deprecated
  public void storePayPalObject(final PayPal payPalObj, boolean bTransaction)
      throws XWikiException {
    getStore().executeWrite(getContext(), bTransaction,
        session -> {
          LOGGER.debug("in doInHibernate with session: " + session);
          session.saveOrUpdate(payPalObj);
          LOGGER.debug("after saveOrUpdate in doInHibernate with session: " + session);
          return null;
        });
  }

  @Override
  public PayPal loadPayPalObject(final String txnId) throws XWikiException {
    boolean bTransaction = true;

    PayPal payPalObj = new PayPal();
    payPalObj.setTxn_id(txnId);

    getStore().checkHibernate(getContext());

    SessionFactory sfactory = getStore().injectCustomMappingsInSessionFactory(getContext());
    bTransaction = bTransaction && getStore().beginTransaction(sfactory, false, getContext());
    Session session = getStore().getSession(getContext());
    session.setFlushMode(FlushMode.MANUAL);

    try {
      session.load(payPalObj, payPalObj.getTxn_id());
    } catch (ObjectNotFoundException exp) {
      LOGGER.debug("no paypal object for txn_id [" + payPalObj.getTxn_id() + "] in" + " database ["
          + getContext().getDatabase() + "] found.");
      // No paypall object in store
    }
    LOGGER.trace("successfully loaded paypal object for txn_id [" + payPalObj.getTxn_id()
        + "] in database [" + getContext().getDatabase() + "] :" + payPalObj.getOrigMessage());
    return payPalObj;
  }

  XWikiHibernateStore getStore() {
    return getContext().getWiki().getHibernateStore();
  }

}
