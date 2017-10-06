package com.celements.payment.observation;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.Event;

import com.celements.common.observation.listener.AbstractLocalEventListener;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentAccessException;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.payment.classes.ComputopPaymentClass;
import com.celements.payment.service.ComputopServiceRole;
import com.google.common.base.Optional;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component(ComputopPaymentListener.NAME)
public class ComputopPaymentListener extends AbstractLocalEventListener<XWikiDocument, Object> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComputopPaymentListener.class);

  public static final String NAME = "computopPayment";

  @Requirement
  private IModelAccessFacade modelAccess;

  @Requirement
  private ComputopServiceRole computopService;

  @Requirement(ComputopPaymentClass.CLASS_DEF_HINT)
  private ClassDefinition computopPaymentClass;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public List<Event> getEvents() {
    return Arrays.<Event>asList(new DocumentCreatedEvent(), new DocumentUpdatedEvent());
  }

  @Override
  protected void onEventInternal(Event event, XWikiDocument doc, Object data) {
    Optional<BaseObject> paymentObj = XWikiObjectFetcher.on(doc).filter(
        computopPaymentClass).first();
    if (paymentObj.isPresent()) {
      String transId = modelAccess.getFieldValue(paymentObj.get(),
          ComputopPaymentClass.FIELD_TRANS_ID).or("");
      setOrderStatus(computopService.getOrderDocRef(transId), computopService.isAuthorizedPayment(
          doc));
    } else {
      LOGGER.debug("onEvent: no computop payment doc");
    }
  }

  private void setOrderStatus(DocumentReference orderDocRef, boolean isAuthorizedPayment) {
    try {
      XWikiDocument orderDoc = modelAccess.getDocument(orderDocRef);
      // TODO [CELDEV-561] create ClassDefinitions for Classes.CXMLShoppingCart
      ClassReference classRef = new ClassReference("Classes", "CXMLShoppingCartOrder");
      Optional<BaseObject> orderObj = XWikiObjectEditor.on(orderDoc).filter(
          classRef).fetch().first();
      if (orderObj.isPresent()) {
        String status = "CartStati.Payment" + (isAuthorizedPayment ? "Success" : "Failure");
        if (modelAccess.setProperty(orderObj.get(), "status", status)) {
          modelAccess.saveDocument(orderDoc, "set order status from payment update");
        }
        LOGGER.info("setOrderStatus: for '{}' to '{}', isAuthorizedPayment '{}'", orderDocRef,
            status, isAuthorizedPayment);
      } else {
        LOGGER.warn("setOrderStatus: missing order object '{}'", orderDocRef);
      }
    } catch (DocumentAccessException dae) {
      LOGGER.error("setOrderStatus failed", dae);
    }
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

}
