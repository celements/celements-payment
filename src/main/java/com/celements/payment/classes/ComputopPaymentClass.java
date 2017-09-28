package com.celements.payment.classes;

import org.xwiki.component.annotation.Component;

import com.celements.model.classes.AbstractClassDefinition;

@Component(ComputopPaymentClass.CLASS_DEF_HINT)
public class ComputopPaymentClass extends AbstractClassDefinition implements
    PaymentClassDefinition {

  public static final String SPACE_NAME = "PaymentClasses";
  public static final String DOC_NAME = "ComputopPaymentClass";
  public static final String CLASS_DEF_HINT = SPACE_NAME + "." + DOC_NAME;

  // TODO fields

  @Override
  public String getName() {
    return CLASS_DEF_HINT;
  }

  @Override
  public boolean isInternalMapping() {
    return true;
  }

  @Override
  protected String getClassSpaceName() {
    return SPACE_NAME;
  }

  @Override
  protected String getClassDocName() {
    return DOC_NAME;
  }
}
