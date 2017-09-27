/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.celements.payment.raw;

import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Strings.*;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

public class Computop implements PaymentRawObject {

  private int id = 0;

  private String origHeader = "";

  private String origMessage = "";

  private String txnId = "";

  private String merchantId = "";

  private int length = 0;

  private String data = "";

  @Enumerated(EnumType.STRING)
  private EProcessStatus processStatus = EProcessStatus.Unknown;

  public Computop() {
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getOrigHeader() {
    return origHeader;
  }

  public void setOrigHeader(String origHeader) {
    this.origHeader = nullToEmpty(origHeader);
  }

  public String getOrigMessage() {
    return origMessage;
  }

  public void setOrigMessage(String origMessage) {
    this.origMessage = nullToEmpty(origMessage);
  }

  public String getTxnId() {
    return txnId;
  }

  public void setTxnId(String txnId) {
    this.txnId = nullToEmpty(txnId);
  }

  public String getMerchantId() {
    return merchantId;
  }

  public void setMerchantId(String merchantId) {
    this.merchantId = nullToEmpty(merchantId);
  }

  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = nullToEmpty(data);
  }

  public EProcessStatus getProcessStatus() {
    return processStatus;
  }

  public void setProcessStatus(EProcessStatus processStatus) {
    this.processStatus = firstNonNull(processStatus, EProcessStatus.Unknown);
  }

}
