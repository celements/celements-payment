package com.celements.payment.classes;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

import com.celements.model.classes.AbstractClassPackage;
import com.celements.model.classes.ClassDefinition;

@Component(PaymentClassPackage.NAME)
public class PaymentClassPackage extends AbstractClassPackage {

  public static final String NAME = "payment";

  @Requirement
  private List<PaymentClassDefinition> classDef;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public List<? extends ClassDefinition> getClassDefinitions() {
    return new ArrayList<>(classDef);
  }
}
