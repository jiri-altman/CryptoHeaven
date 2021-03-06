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

import com.CH_co.service.engine.*;
import com.CH_co.service.msg.MessageAction;
import com.CH_co.service.msg.dataSets.obj.*;
import com.CH_co.trace.Trace;

/**
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * <b>$Revision: 1.1 $</b>
 *
 * @author  Marcin Kurzawa
 */
public class SysAVersion extends ClientMessageAction {

  /** Creates new SysAVersion */
  public SysAVersion() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(SysAVersion.class, "SysAVersion()");
    if (trace != null) trace.exit(SysAVersion.class);
  }

  /**
   * The action handler performs all actions related to the received message (reply),
   * and optionally returns a request Message.  If there is no request, null is returned.
   */
  public MessageAction runAction() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(SysAVersion.class, "runAction(Connection)");

    // reply syntax:
    // <serverVersion> <serverRelease> <serverBuild>

    Obj_List_Co reply = (Obj_List_Co) getMsgDataSet();
    CommonSessionContext sessionContext = getCommonContext();
    sessionContext.serverBuild = ((Short) reply.objs[2]).shortValue();

    // Unlock the Reader and Writer because synchronized request-reply is done.
    getClientContext().releaseLoginStreamers();

    if (trace != null) trace.exit(SysAVersion.class, null);
    return null;
  }

}