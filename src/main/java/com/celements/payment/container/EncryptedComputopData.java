package com.celements.payment.container;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import com.google.common.base.Optional;

@Immutable
public class EncryptedComputopData {

  private final String cipherText;
  private final int plainDataLength;

  public EncryptedComputopData(@Nullable String cipherText, int plainDataLength) {
    this.cipherText = cipherText;
    this.plainDataLength = plainDataLength;
  }

  public @NotNull Optional<String> getCipherText() {
    return Optional.fromNullable(cipherText);
  }

  public int getPlainDataLength() {
    return plainDataLength;
  }

  public @NotNull String getData() {
    return getCipherText().or("");
  }

  public int getLen() {
    return getPlainDataLength();
  }
}
