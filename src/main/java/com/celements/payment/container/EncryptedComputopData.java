package com.celements.payment.container;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import com.google.common.base.Preconditions;

@Immutable
public class EncryptedComputopData {

  private final String cipherText;
  private final int plainDataLength;

  public EncryptedComputopData(@NotNull String cipherText, int plainDataLength) {
    Preconditions.checkNotNull(cipherText);
    this.cipherText = cipherText;
    this.plainDataLength = plainDataLength;
  }

  public @NotNull String getCipherText() {
    return cipherText;
  }

  public int getPlainDataLength() {
    return plainDataLength;
  }

  public @NotNull String getData() {
    return getCipherText();
  }

  public int getLen() {
    return getPlainDataLength();
  }
}
