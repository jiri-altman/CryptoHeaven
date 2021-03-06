/**
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Corp. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Corp.
 */
package com.CH_gui.frame;

import java.io.*;
import java.awt.*;

import com.CH_co.trace.Trace;

import com.CH_gui.actionGui.*;
import com.CH_gui.localFileTable.*;

/** 
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * <b>$Revision: 1.13 $</b>
 *
 * @author  Marcin Kurzawa
 */
public class LocalFileTableFrame extends JActionFrameClosable {

  /** Creates new FileTableFrame */
  public LocalFileTableFrame(String propertyName) {
    super(com.CH_cl.lang.Lang.rb.getString("title_Local_File_System_View"), true, true);
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FileTableFrame.class, "FileTableFrame()");

    FileChooserComponent mainComponent = new FileChooserComponent(propertyName);
    this.getContentPane().add(mainComponent, BorderLayout.CENTER);

    // all JActionFrames already size themself
    setVisible(true);

    if (trace != null) trace.exit(FileTableFrame.class);
  }

  /*******************************************************
  *** V i s u a l s S a v a b l e    interface methods ***
  *******************************************************/
  public static final String visualsClassKeyName = "LocalFileTableFrame";
  public String getVisualsClassKeyName() {
    return visualsClassKeyName;
  }
}