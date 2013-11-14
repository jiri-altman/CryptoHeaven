/**
 * Copyright 2001-2013 CryptoHeaven Corp. All Rights Reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Corp. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Corp.
 */
package com.CH_cl.service.actions.usr;

import com.CH_cl.service.actions.*;

import com.CH_co.service.msg.MessageAction;
import com.CH_co.trace.Trace;

/**
 * Copyright 2001-2013 CryptoHeaven Corp. All Rights Reserved.
 *
 * <b>$Revision: 1.6 $</b>
 *
 * @author  Marcin Kurzawa
 */
public class UsrAGetCumulativeUsage extends ClientMessageAction {

  /** Creates new UsrAGetCumulativeUsage */
  public UsrAGetCumulativeUsage() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(UsrAGetCumulativeUsage.class, "UsrAGetCumulativeUsage()");
    if (trace != null) trace.exit(UsrAGetCumulativeUsage.class);
  }

  /**
   * The action handler performs all actions related to the received message (reply),
   * and optionally returns a request Message.  If there is no request, null is returned.
   */
  public MessageAction runAction() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(UsrAGetCumulativeUsage.class, "runAction(Connection)");

    // no action

    if (trace != null) trace.exit(UsrAGetCumulativeUsage.class, null);
    return null;
  }

}