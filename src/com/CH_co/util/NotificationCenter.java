/**
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Corp. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Corp.
 */
package com.CH_co.util;

/**
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * @author  Marcin Kurzawa
 */
public class NotificationCenter {

  public static final int QUESTION_MESSAGE = 9001;
  public static final int INFORMATION_MESSAGE = 9002;
  public static final int WARNING_MESSAGE = 9003;
  public static final int ERROR_MESSAGE = 9004;
  public static final int ERROR_CONNECTION = 9005;
  public static final int RECYCLE_MESSAGE = 9006;
  public static final int DELETE_MESSAGE = 9007;
  public static final int EMPTY_RECYCLE_FOLDER = 9008;
  public static final int EMPTY_SPAM_FOLDER = 9009;

  private static Class implNotificationCenterI;
  private static SingleTokenArbiter msgDialogArbiter = null;

  public static void setImpl(Class notificationCenterImpl) {
    implNotificationCenterI = notificationCenterImpl;
  }

  public static void show(int type, String title, String msg) {
    if (implNotificationCenterI != null) {
      try {
        NotificationShowerI impl = (NotificationShowerI) implNotificationCenterI.newInstance();
        impl.show(type, title, msg);
      } catch (Throwable t) {
      }
    }
  }
  public static void show(final Object key, int type, String title, String msg) {
    if (implNotificationCenterI != null) {
      try {
        NotificationShowerI impl = (NotificationShowerI) implNotificationCenterI.newInstance();
        if (msgDialogArbiter == null) msgDialogArbiter = new SingleTokenArbiter();
        impl.show(msgDialogArbiter, key, type, title, msg);
      } catch (Throwable t) {
      }
    }
  }
  public static void showYesNo(int type, String title, String msg, boolean defaultYes, Runnable yes, Runnable no) {
    if (implNotificationCenterI != null) {
      try {
        NotificationShowerI impl = (NotificationShowerI) implNotificationCenterI.newInstance();
        impl.showYesNo(type, title, msg, defaultYes, yes, no);
      } catch (Throwable t) {
      }
    }
  }

}