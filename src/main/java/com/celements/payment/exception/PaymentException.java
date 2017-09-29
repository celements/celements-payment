package com.celements.payment.exception;

public class PaymentException extends Exception {

  private static final long serialVersionUID = 1L;

  public PaymentException(String msg) {
    super(msg);
  }

  public PaymentException(Throwable cause) {
    super(cause);
  }

  public PaymentException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
