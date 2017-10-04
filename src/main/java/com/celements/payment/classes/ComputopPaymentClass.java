package com.celements.payment.classes;

import java.util.Date;

import org.xwiki.component.annotation.Component;

import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.BooleanField;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.DateField;
import com.celements.model.classes.fields.LargeStringField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.classes.fields.number.IntField;

@Component(ComputopPaymentClass.CLASS_DEF_HINT)
public class ComputopPaymentClass extends AbstractClassDefinition implements
    PaymentClassDefinition {

  public static final String SPACE_NAME = "PaymentClasses";
  public static final String DOC_NAME = "ComputopPaymentClass";
  public static final String CLASS_DEF_HINT = SPACE_NAME + "." + DOC_NAME;

  public static final ClassField<String> FIELD_MERCHANT_ID = new StringField.Builder(CLASS_DEF_HINT,
      "merchantId").build();

  public static final ClassField<String> FIELD_PAY_ID = new StringField.Builder(CLASS_DEF_HINT,
      "payId").build();

  public static final ClassField<String> FIELD_X_ID = new StringField.Builder(CLASS_DEF_HINT,
      "xId").build();

  public static final ClassField<String> FIELD_TRANS_ID = new StringField.Builder(CLASS_DEF_HINT,
      "transId").build();

  public static final ClassField<String> FIELD_DESCRIPTION = new LargeStringField.Builder(
      CLASS_DEF_HINT, "description").build();

  public static final ClassField<String> FIELD_STATUS = new StringField.Builder(CLASS_DEF_HINT,
      "status").build();

  public static final ClassField<Integer> FIELD_STATUS_CODE = new IntField.Builder(CLASS_DEF_HINT,
      "statusCode").build();

  public static final ClassField<Boolean> FIELD_VERIFIED = new BooleanField.Builder(CLASS_DEF_HINT,
      "verified").build();

  public static final ClassField<String> FIELD_DATA = new LargeStringField.Builder(CLASS_DEF_HINT,
      "data").build();

  public static final ClassField<Date> FIELD_CALLBACK_DATE = new DateField.Builder(CLASS_DEF_HINT,
      "callbackDate").build();

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
