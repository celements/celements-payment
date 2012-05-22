package com.celements.payment;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;

import com.celements.web.plugin.api.CelementsWebPluginApi;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
public class PaymentService implements IPaymentService {

  private static Log LOGGER = LogFactory.getFactory().getInstance(PaymentService.class);

  @Requirement
  Execution execution;

  private XWikiContext getContext() {
    return (XWikiContext)execution.getContext().getProperty("xwikicontext");
  }

  public void executePaymentAction(Map<String, String[]> data) {
    try {
      DocumentReference callbackDocRef = new DocumentReference(
          getContext().getDatabase(), "Payment", "CallbackAction");
      if (getContext().getWiki().exists(callbackDocRef, getContext())) {
        XWikiDocument actionXdoc = getContext().getWiki().getDocument(callbackDocRef, 
            getContext());
        Document actionDoc = actionXdoc.newDocument(getContext());
        CelementsWebPluginApi celementsweb = (CelementsWebPluginApi)getContext().getWiki(
            ).getPluginApi("celementsweb", getContext());
        celementsweb.getPlugin().executeAction(actionDoc, data, actionXdoc, getContext());
      } else {
        LOGGER.warn("Failed to execute payment action because Payment.CallbackAction does"
            + " not exist in [" + getContext().getDatabase() + "].");
      }
    } catch (XWikiException e) {
      LOGGER.error("Exeption while creating callback object", e);
    }
  }

}
