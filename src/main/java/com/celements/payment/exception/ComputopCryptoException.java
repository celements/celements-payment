package com.celements.payment.exception;

public class ComputopCryptoException extends PaymentException {

  private static final long serialVersionUID = 1L;

  public ComputopCryptoException(String msg) {
    super(msg);
  }

  public ComputopCryptoException(String msg, Throwable cause) {
    super(msg, cause);

  }

}
