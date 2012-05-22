package com.celements.payment;

import java.util.Map;

import org.xwiki.component.annotation.ComponentRole;

@ComponentRole
public interface IPaymentService {

  public void executePaymentAction(Map<String, String[]> data);

}
