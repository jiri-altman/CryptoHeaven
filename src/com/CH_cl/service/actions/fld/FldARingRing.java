/*
 * Copyright 2001-2010 by CryptoHeaven Development Team,
 * Mississauga, Ontario, Canada.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Development Team ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Development Team.
 */

package com.CH_cl.service.actions.fld;

import com.CH_co.trace.Trace;

import com.CH_cl.service.actions.*;

import com.CH_co.service.msg.*;
import com.CH_co.service.msg.dataSets.obj.*;

/** 
 * <b>Copyright</b> &copy; 2001-2010
 * <a href="http://www.CryptoHeaven.com/DevelopmentTeam/">
 * CryptoHeaven Development Team.
 * </a><br>All rights reserved.<p>
 *
 * Class Description:
 *
 *
 * Class Details:
 *
 *
 * <b>$Revision: 1.8 $</b>
 * @author  Marcin Kurzawa
 * @version
 */
public class FldARingRing extends ClientMessageAction {

  /** Creates new FldARingRing */
  public FldARingRing() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FldARingRing.class, "FldARingRing()");
    if (trace != null) trace.exit(FldARingRing.class);
  }

  /**
   * The action handler performs all actions related to the received message (reply),
   * and optionally returns a request Message.  If there is no request, null is returned.
   */
  public MessageAction runAction() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FldARingRing.class, "runAction(Connection)");

    getServerInterfaceLayer().getFetchedDataCache().fireFolderRingEvent((Obj_List_Co) getMsgDataSet());

    if (trace != null) trace.exit(FldARingRing.class, null);
    return null;
  }
}