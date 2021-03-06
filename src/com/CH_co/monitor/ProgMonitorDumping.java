/**
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Corp. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Corp.
 */
package com.CH_co.monitor;

/** 
* Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
*
* <b>$Revision: 1.13 $</b>
*
* @author  Marcin Kurzawa
*/
public class ProgMonitorDumping extends Object implements ProgMonitorI {

  /** Creates new ProgMonitorDumping */
  public ProgMonitorDumping() {
  }

  public void enqueue(int actionCode, long stamp) {
  }
  public void dequeue(int actionCode, long stamp) {
  }
  public void startSend(int actionCode, long stamp) {
  }
  public void startSendAction(String actionName) {
  }
  public void startSendData(String dataName) {
  }
  public void doneSend(int actionCode, long stamp) {
  }
  public void doneSendAction(String actionName) {
  }
  public void doneSendData(String dataName) {
  }
  public void startReceive(int actionCode, long stamp) {
  }
  public void startReceiveAction(String actionName) {
  }
  public void startReceiveData(String dataName) {
  }
  public void doneReceive(int actionCode, long stamp) {
  }
  public void doneReceiveAction(String actionName) {
  }
  public void doneReceiveData(String dataName) {
  }
  public void startExecution(int actionCode) {
  }
  public void doneExecution(int actionCode) {
  }
  public String getLastStatusInfo() {
    return null;
  }
  public String getLastStatusTitle() {
    return null;
  }
  public void setCurrentStatus(String currentStatus) {
  }
  public void setFileNameSource(String fileName) {
  }
  public void setFileNameDestination(String fileName) {
  }
  public void setFilePathDestination(String filePath) {
  }
  public long getTransferred() {
    return -1;
  }
  public long getTransferSize() {
    return -1;
  }
  public void setTransferSize(long size) {
  }
  public void updateTransferSize(long size) {
  }
  public void addBytes(long bytes) {
  }
  public void doneTransfer() {
  }
  public void nextTask() {
  }
  public void nextTask(String task) {
  }
  public void allDone() {
    ProgMonitorPool.removeProgMonitor(this);
  }
  public void setInterrupt(Interruptible interruptible) {
  }
  public void jobKilled() {
  }
  public void jobForRetry() {
  }
  public boolean isAllDone() {
    return false;
  }
  public boolean isCancelled() {
    return false;
  }
  public void setCancellable(Cancellable cancellable) {
  }
  public void interruptAndCancel() {
  }
  public boolean isJobKilled() {
    return false;
  }
  public void appendLine(String info) {
  }
  public boolean isLoginMonitor() {
    return false;
  }
}