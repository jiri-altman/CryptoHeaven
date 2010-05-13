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

package com.CH_gui.gui;

import com.CH_co.gui.FileChooserI;

import java.awt.Component;
import javax.swing.JFileChooser;

/**
 * <b>Copyright</b> &copy; 2001-2010
 * <a href="http://www.CryptoHeaven.com/DevelopmentTeam/">
 * CryptoHeaven Development Team.
 * </a><br>All rights reserved.<p>
 *
 *
 * @author  Marcin Kurzawa
 * @version
 */
public class SingleFileChooser extends JFileChooser implements FileChooserI {

  public boolean isApproved(int retVal) {
    return retVal == javax.swing.JFileChooser.APPROVE_OPTION;
  }

  public int showOpenDialog(Object parentComponent) {
    return super.showOpenDialog((Component) parentComponent);
  }

}