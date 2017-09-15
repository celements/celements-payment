package com.celements.payment.service;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.script.service.ScriptService;

@Component
public class ComputopScriptService implements ScriptService {

  @Requirement
  private ComputopServiceRole computopService;

  public boolean isCallbackHashValid(@NotNull String hash, @NotNull String payId,
      @NotNull String transId, @NotNull String merchantId, @NotNull String status,
      @NotNull String code) {
    return computopService.isCallbackHashValid(hash, payId, transId, merchantId, status, code);
  }
}
