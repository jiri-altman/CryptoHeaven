/*
 * Copyright 2001-2009 by CryptoHeaven Development Team,
 * Mississauga, Ontario, Canada.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Development Team ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Development Team.
 */

package com.CH_cl.service.actions.error;

import java.awt.*;
import javax.swing.*;

import com.CH_cl.service.actions.*;

import com.CH_co.service.msg.MessageAction;
import com.CH_co.service.msg.dataSets.*;
import com.CH_co.util.*;

/** 
 * <b>Copyright</b> &copy; 2001-2009
 * <a href="http://www.CryptoHeaven.com/DevelopmentTeam/">
 * CryptoHeaven Development Team.
 * </a><br>All rights reserved.<p> 
 *
 * @author  Marcin Kurzawa
 * @version 
 */
public class ErrorMessageAction extends ClientMessageAction {

  /** Creates new ErrorMessageAction */
  public ErrorMessageAction() {
  }

  /** The action handler performs all actions related to the received error message (reply),
      and optionally returns a request Message.  If there is no request, null is returned.
  */
  public MessageAction runAction() {
    Str_Rp dataSet = (Str_Rp) getMsgDataSet();
    if (dataSet.message != null && dataSet.message.length() > 0) {
      if (dataSet.message.startsWith("System Notice: ")) {
        String msg = dataSet.message.substring("System Notice: ".length());
        MessageDialog.showWarningDialog(null, msg, "System Notice");
      }
      else {
        String msg = "<html> Operation did not complete successfully. <p>" + dataSet.message;
        MessageDialog.showErrorDialog(null, msg, "Error Dialog");
      }
    }
    return null;
  }

}