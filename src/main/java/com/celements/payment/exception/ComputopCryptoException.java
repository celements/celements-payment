package com.celements.payment.exception;

public class ComputopCryptoException extends Exception {

  private static final long serialVersionUID = -5534681246083544419L;

  public ComputopCryptoException(String msg, Exception excp) {
    super(msg, excp);
  }

}
