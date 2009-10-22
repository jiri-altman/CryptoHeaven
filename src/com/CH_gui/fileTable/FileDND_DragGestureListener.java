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

package com.CH_gui.fileTable;

import java.awt.dnd.*;
import java.awt.datatransfer.*;

import com.CH_co.trace.Trace;
import com.CH_co.service.records.*;

/** 
 * <b>Copyright</b> &copy; 2001-2009
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
 * <b>$Revision: 1.10 $</b>
 * @author  Marcin Kurzawa
 * @version 
 */
public class FileDND_DragGestureListener extends Object implements DragGestureListener {

  private FileActionTable fileActionTable;

  /** Creates new FileDND_DragGestureListener */
  protected FileDND_DragGestureListener(FileActionTable fileActionTable) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FileDND_DragGestureListener.class, "FileDND_DragGestureListener(FileActionTable fileActionTable)");
    if (trace != null) trace.args(fileActionTable);
    this.fileActionTable = fileActionTable;
    if (trace != null) trace.exit(FileDND_DragGestureListener.class);
  }

  public void dragGestureRecognized(DragGestureEvent event) {
    FolderPair[] fPairs = (FolderPair[]) fileActionTable.getSelectedInstancesOf(FolderPair.class);
    FileLinkRecord[] fLinks = (FileLinkRecord[]) fileActionTable.getSelectedInstancesOf(FileLinkRecord.class);
    if ((fPairs != null && fPairs.length > 0) ||
        (fLinks != null && fLinks.length > 0))
    {
      FileDND_Transferable transferable = new FileDND_Transferable(fPairs, fLinks);
      // as the name suggests, starts the dragging
      event.getDragSource().startDrag(event, null, transferable, new FileDND_DragSourceListener());
    } else {
      //System.out.println( "nothing was selected");   
    }
  }
} // end class FileDND_DragGestureListener