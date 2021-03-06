/**
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Corp. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Corp.
 */
package com.CH_cl.service.actions.sys;

import com.CH_cl.service.actions.*;

import com.CH_co.monitor.Stats;
import com.CH_co.trace.Trace;

import com.CH_co.service.msg.MessageAction;
import com.CH_co.service.msg.dataSets.PingPong_Cm;

/** 
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * <b>$Revision: 1.7 $</b>
 *
 * @author  Marcin Kurzawa
 */
public class SysAPong extends ClientMessageAction {

  /** Creates new SysAPong */
  public SysAPong() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(SysAPong.class, "SysAPong()");
    if (trace != null) trace.exit(SysAPong.class);
  }

  /** 
   * The action handler performs all actions related to the received message (reply),
   * and optionally returns a request Message.  If there is no request, null is returned.
   */
  public MessageAction runAction() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(SysAPong.class, "runAction(Connection)");

    PingPong_Cm reply = (PingPong_Cm) getMsgDataSet();
    long timeDiff = System.currentTimeMillis() - reply.date.getTime();
    long pingTime = timeDiff / 2;
    Stats.setPing(pingTime);
    if (trace != null) trace.data(10, "Ping-Pong (ms)", pingTime);

    if (trace != null) trace.exit(SysAPong.class, null);
    return null;
  }

}