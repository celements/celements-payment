package com.celements.payment.classes;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.Component;

import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.BooleanField;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.LargeStringField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.classes.fields.list.EnumListField;
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
      "merchantId").build();

  public static final ClassField<String> FIELD_X_ID = new StringField.Builder(CLASS_DEF_HINT,
      "xId").build();

  public static final ClassField<String> FIELD_TRANS_ID = new StringField.Builder(CLASS_DEF_HINT,
      "transId").build();

  public static final ClassField<String> FIELD_DESCRIPTION = new LargeStringField.Builder(
      CLASS_DEF_HINT, "description").build();

  public static final ClassField<List<Status>> FIELD_STATUS = new EnumListField.Builder<>(
      CLASS_DEF_HINT, "status", Status.class).multiSelect(false).build();

  public static final ClassField<Integer> FIELD_ERROR_CODE = new IntField.Builder(CLASS_DEF_HINT,
      "errorCode").build();

  public static final ClassField<Boolean> FIELD_VERIFIED = new BooleanField.Builder(CLASS_DEF_HINT,
      "verified").build();

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

  public enum Status {
    UNKNOWN,
    OK,
    FAILED;

    public static @NotNull Status resolve(String str) {
      try {
        return Status.valueOf(str);
      } catch (IllegalArgumentException iae) {
        return Status.UNKNOWN;
      }
    }
  }

}
