package com.celements.payment.raw;

import java.util.Date;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

public class PostFinance implements PaymentRawObject {

  /**
   * transaction id is required
   */
  private String txn_id;

  private String paymentMethod = "";

  private Date trxDate;

  private String origHeader = "";

  private String origMessage = "";

  private String payId = "";

  private String cardNumber = "";

  private String expDate = "";

  private String clientIP = "";

  private String cardholder = "";

  private String cardbrand = "";

  private String acceptance = "";

  private String paymentStatus = "";

  private String ncError = "";

  private String currency = "";

  private String amount = "";

  private String shasign = "";

  private String orderId = "";

  @Enumerated(EnumType.STRING)
  private EProcessStatus processStatus = EProcessStatus.Unknown;

  public PostFinance() {
  }

  public String getTxn_id() {
    return txn_id;
  }

  public void setTxn_id(String txnId) {
    txn_id = txnId;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(String payment_method) {
    if (payment_method == null) {
      payment_method = "";
    }
    paymentMethod = payment_method;
  }

  public Date getTrxDate() {
    return trxDate;
  }

  public void setTrxDate(Date trx_date) {
    trxDate = trx_date;
  }

  public String getOrigHeader() {
    return origHeader;
  }

  public void setOrigHeader(String origHeader) {
    if (origHeader == null) {
      origHeader = "";
    }
    this.origHeader = origHeader;
  }

  public String getOrigMessage() {
    return origMessage;
  }

  public void setOrigMessage(String origMessage) {
    if (origMessage == null) {
      origMessage = "";
    }
    this.origMessage = origMessage;
  }

  public String getPayId() {
    return payId;
  }

  public void setPayId(String payId) {
    if (payId == null) {
      payId = "";
    }
    this.payId = payId;
  }

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    if (cardNumber == null) {
      cardNumber = "";
    }
    this.cardNumber = cardNumber;
  }

  public String getExpDate() {
    return expDate;
  }

  public void setExpDate(String expDate) {
    if (expDate == null) {
      expDate = "";
    }
    this.expDate = expDate;
  }

  public String getClientIP() {
    return clientIP;
  }

  public void setClientIP(String clientIP) {
    if (clientIP == null) {
      clientIP = "";
    }
    this.clientIP = clientIP;
  }

  public String getCardholder() {
    return cardholder;
  }

  public void setCardholder(String cardholder) {
    if (cardholder == null) {
      cardholder = "";
    }
    this.cardholder = cardholder;
  }

  public String getCardbrand() {
    return cardbrand;
  }

  public void setCardbrand(String cardbrand) {
    if (cardbrand == null) {
      cardbrand = "";
    }
    this.cardbrand = cardbrand;
  }

  public String getAcceptance() {
    return acceptance;
  }

  public void setAcceptance(String acceptance) {
    if (acceptance == null) {
      acceptance = "";
    }
    this.acceptance = acceptance;
  }

  public String getPaymentStatus() {
    return paymentStatus;
  }

  public void setPaymentStatus(String paymentStatus) {
    if (paymentStatus == null) {
      paymentStatus = "";
    }
    this.paymentStatus = paymentStatus;
  }

  public String getNcError() {
    return ncError;
  }

  public void setNcError(String ncError) {
    if (ncError == null) {
      ncError = "";
    }
    this.ncError = ncError;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    if (currency == null) {
      currency = "";
    }
    this.currency = currency;
  }

  public String getAmount() {
    return amount;
  }

  public void setAmount(String amount) {
    if (amount == null) {
      amount = "";
    }
    this.amount = amount;
  }

  public String getShasign() {
    return shasign;
  }

  public void setShasign(String shasign) {
    if (shasign == null) {
      shasign = "";
    }
    this.shasign = shasign;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    if (orderId == null) {
      orderId = "";
    }
    this.orderId = orderId;
  }

  public EProcessStatus getProcessStatus() {
    return processStatus;
  }

  public void setProcessStatus(EProcessStatus processStatus) {
    if (processStatus == null) {
      processStatus = EProcessStatus.Unknown;
    }
    this.processStatus = processStatus;
  }

}
