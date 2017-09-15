package com.celements.payment.service;

import java.util.Map;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;

@ComponentRole
public interface ComputopServiceRole {

  public @NotNull Map<String, String> encryptPaymentData(@NotNull String paymentData);

  public @NotNull Map<String, String> decryptCallbackData(@NotNull String encryptedCallback,
      int plainDataLength);
}
