/**
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Corp. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Corp.
 */
package com.CH_gui.traceTable;

import com.CH_cl.service.cache.*;
import com.CH_cl.service.engine.*;

import com.CH_co.service.records.*;
import com.CH_co.util.*;
import com.CH_co.trace.Trace;

import com.CH_gui.action.*;
import com.CH_gui.dialog.*;
import com.CH_gui.frame.*;
import com.CH_gui.list.*;
import com.CH_gui.table.*;
import com.CH_gui.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.dnd.*;
import java.util.*;

/** 
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * <b>$Revision: 1.24 $</b>
 *
 * @author  Marcin Kurzawa
 */
public class TraceActionTable extends RecordActionTable implements ActionProducerI {

  private Action[] actions;

  private static final int REFRESH_ACTION = 0;
  private static final int COPY_TO_CLIPBOARD_ACTION = 1;
  private static final int ADD_TO_CONTACTS_ACTION = 2;
  private static final int MESSAGE_ACTION = 3;

  private int leadingActionId = Actions.LEADING_ACTION_ID_STAT_ACTION_TABLE;
  private ServerInterfaceLayer SIL;

  /** Creates new TraceActionTable
   */
  public TraceActionTable(Record[] parentObjLinks) {
    super(new TraceTableModel(parentObjLinks));
    initialize();
  }
  private void initialize() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(TraceActionTable.class, "initialize()");
    SIL = MainFrame.getServerInterfaceLayer();
    initActions();
    if (trace != null) trace.exit(TraceActionTable.class);
  }


  public DragGestureListener createDragGestureListener() {
    return null;
  }
  public DropTargetListener createDropTargetListener() {
    return null;
  }


  private void initActions() {
    actions = new Action[4];
    actions[REFRESH_ACTION] = new RefreshAction(leadingActionId + REFRESH_ACTION);
    actions[COPY_TO_CLIPBOARD_ACTION] = new CopyToClipboardAction(leadingActionId + COPY_TO_CLIPBOARD_ACTION);
    actions[ADD_TO_CONTACTS_ACTION] = new InitiateContactAction(leadingActionId + ADD_TO_CONTACTS_ACTION);
    actions[MESSAGE_ACTION] = new SendMessageAction(leadingActionId + MESSAGE_ACTION);
    setEnabledActions();
  }
  public Action getRefreshAction() {
    return actions[REFRESH_ACTION];
  }
  public Action getCopyToClipboardAction() {
    return actions[COPY_TO_CLIPBOARD_ACTION];
  }
  public Action getInitiateAction() {
    return actions[ADD_TO_CONTACTS_ACTION];
  }
  public Action getMessageAction() {
    return actions[MESSAGE_ACTION];
  }


  // =====================================================================
  // LISTENERS FOR THE MENU ITEMS        
  // =====================================================================

  /**
   * Refresh Trace List.
   */
  private class RefreshAction extends AbstractActionTraced {
    public RefreshAction(int actionId) {
      super(com.CH_cl.lang.Lang.rb.getString("action_Refresh_Traces"), Images.get(ImageNums.REFRESH16));
      putValue(Actions.ACTION_ID, new Integer(actionId));
      putValue(Actions.TOOL_TIP, com.CH_cl.lang.Lang.rb.getString("actionTip_Refresh_Trace_List_from_the_server."));
      putValue(Actions.TOOL_ICON, Images.get(ImageNums.REFRESH24));
      putValue(Actions.IN_MENU, Boolean.FALSE);
      putValue(Actions.IN_POPUP, Boolean.FALSE);
      putValue(Actions.IN_TOOLBAR, Boolean.FALSE);
    }
    public void actionPerformedTraced(ActionEvent event) {
      TraceTableModel tableModel = (TraceTableModel) getTableModel();
      tableModel.refreshData();
    }
  }

  /** 
   * Copy trace details to clipboard.
   */
  private class CopyToClipboardAction extends AbstractActionTraced {
    public CopyToClipboardAction(int actionId) {
      super(com.CH_cl.lang.Lang.rb.getString("action_Copy_To_Clipboard"), Images.get(ImageNums.COPY16));
      putValue(Actions.ACTION_ID, new Integer(actionId));
      putValue(Actions.TOOL_ICON, Images.get(ImageNums.COPY24));
    }
    public void actionPerformedTraced(ActionEvent event) {
      FetchedDataCache cache = FetchedDataCache.getSingleInstance();
      ArrayList traceRecs = getTableModel().getRowListForViewOnly();
      MultiHashMap traceRecsHM = new MultiHashMap(true);
      if (traceRecs != null) {
        // Group them by OBJECT
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<traceRecs.size(); i++) {
          TraceRecord rec = (TraceRecord) traceRecs.get(i);
          traceRecsHM.put(rec.objId, rec);
        }
        // FOR EACH OBJECT WRITE OUT TRACE DATA
        Set objIDs = traceRecsHM.keys();
        Iterator iter = objIDs.iterator();
        while (iter.hasNext()) {
          Long objId = (Long) iter.next();
          Collection traces = traceRecsHM.getAll(objId);
          Iterator iterRec = traces.iterator();
          while (iterRec.hasNext()) {
            try {
              TraceRecord trace = (TraceRecord) iterRec.next();
              sb.append("Access Record for ");
              Record obj = null;
              if (trace.objType.byteValue() == TraceRecord.STAT_TYPE_MESSAGE) {
                sb.append("message id ");
                obj = cache.getMsgDataRecord(trace.objId);
              } else if (trace.objType.byteValue() == TraceRecord.STAT_TYPE_FILE) {
                sb.append("file id ");
                obj = cache.getFileLinkRecordsForFile(trace.objId)[0]; // link will render into filename
              } else if (trace.objType.byteValue() == TraceRecord.STAT_TYPE_FOLDER) {
                sb.append("folder id ");
                obj = cache.getFolderRecord(trace.objId);
              }
              sb.append(trace.objId);
              if (obj != null) {
                sb.append(" : ");
                sb.append(ListRenderer.getRenderedText(obj));
              }
              sb.append("\n");
              UserRecord uRec = cache.getUserRecord(trace.ownerUserId);
              if (uRec != null)
                sb.append("user: "+uRec.shortInfo()+"\n");
              else
                sb.append("user: "+java.text.MessageFormat.format(com.CH_cl.lang.Lang.rb.getString("User_(USER-ID)"), new Object[] {trace.ownerUserId})+"\n");
              if (trace.hasHistoryRecord) {
                sb.append("first seen: "+Misc.getFormattedTimestamp(trace.firstSeen)+"\n");
                sb.append("retrieved: "+Misc.getFormattedTimestamp(trace.firstDelivered)+"\n");
              } else {
                sb.append("No access history found.\n");
              }
              if (trace.hasReadAccess)
                sb.append("Object is currently accessible by the user.\n");
              else
                sb.append("Object is no longer accessible by the user.\n");
              sb.append("\r\n");
            } catch (Throwable t) {
              t.printStackTrace();
            }
          }
        }
        String traceStr = sb.toString();
        Transferable selection = new HtmlSelection(Misc.encodePlainIntoHtml(traceStr), traceStr);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
        NotificationCenter.show(NotificationCenter.INFORMATION_MESSAGE, "Copied", "Trace records have been copied to clipboard.");
      }
    }
  }

  /** 
   * Initiate a new contact.
   */
  private class InitiateContactAction extends AbstractActionTraced {
    public InitiateContactAction(int actionId) {
      super(com.CH_cl.lang.Lang.rb.getString("action_Add_to_Contact_List_..."), Images.get(ImageNums.CONTACT_ADD16));
      putValue(Actions.ACTION_ID, new Integer(actionId));
      putValue(Actions.TOOL_TIP, com.CH_cl.lang.Lang.rb.getString("actionTip_Add_User_to_your_Contact_List."));
      putValue(Actions.TOOL_ICON, Images.get(ImageNums.CONTACT_ADD24));
    }
    public void actionPerformedTraced(ActionEvent event) {
      TraceRecord tRec = (TraceRecord) getSelectedRecord();
      FetchedDataCache cache = FetchedDataCache.getSingleInstance();
      UserRecord uRec = cache.getUserRecord(tRec.ownerUserId);
      if (uRec != null) {
        Window w = SwingUtilities.windowForComponent(TraceActionTable.this);
        if (w instanceof Frame)
          new InitiateContactDialog((Frame) w, new Long[] { uRec.userId });
        else if (w instanceof Dialog)
          new InitiateContactDialog((Dialog) w, new Long[] { uRec.userId });
      }
    }
  }

  /** 
   * Message a user.
   */
  private class SendMessageAction extends AbstractActionTraced {
    public SendMessageAction(int actionId) {
      super(com.CH_cl.lang.Lang.rb.getString("action_Send_Message_..."), Images.get(ImageNums.MAIL_COMPOSE16));
      putValue(Actions.ACTION_ID, new Integer(actionId));
      putValue(Actions.TOOL_TIP, com.CH_cl.lang.Lang.rb.getString("actionTip_New_Message_to_the_selected_user(s)."));
      putValue(Actions.TOOL_ICON, Images.get(ImageNums.MAIL_COMPOSE24));
    }
    public void actionPerformedTraced(ActionEvent event) {
      TraceRecord[] traceRecs = (TraceRecord[]) getSelectedRecords();
      UserRecord[] uRecs = getUserRecords(traceRecs);
      if (uRecs != null && uRecs.length > 0) {
        String subject = null;
        Hashtable usedRecsHT = new Hashtable();
        if (traceRecs.length == 1) {
          usedRecsHT.put(traceRecs[0].objId, ((TraceTableModel) TraceActionTable.this.getTableModel()).getTracedObjRecord(traceRecs[0].objId));
        } else {
          for (int i=0; i<traceRecs.length; i++) {
            Long objId = traceRecs[i].objId;
            Record rec = ((TraceTableModel) TraceActionTable.this.getTableModel()).getTracedObjRecord(objId);
            if (!usedRecsHT.containsKey(objId))
              usedRecsHT.put(objId, rec);
          }
        }
        if (usedRecsHT.size() > 1) {
          StringBuffer sb = new StringBuffer();
          Enumeration enm = usedRecsHT.keys();
          while (enm.hasMoreElements()) {
            Long objId = (Long) enm.nextElement();
            Record rec = (Record) usedRecsHT.get(objId);
            sb.append('"');
            sb.append(ListRenderer.getRenderedText(rec));
            sb.append("\" (");
            sb.append(objId);
            sb.append(')');
            if (enm.hasMoreElements())
              sb.append(", ");
          }
          subject = java.text.MessageFormat.format(com.CH_cl.lang.Lang.rb.getString("msgSubject_Access_Trace_for"), new Object[] {sb.toString()});
        } else {
          Long objId = traceRecs[0].objId;
          Record rec = (Record) usedRecsHT.get(objId);
          boolean isFile = rec instanceof FileLinkRecord;
          boolean isMsg = rec instanceof MsgLinkRecord;
          boolean isFolder = rec instanceof FolderRecord;
          String objType = "";
          if (isFile) {
            objType = com.CH_cl.lang.Lang.rb.getString("File");
          } else if (isMsg) {
            FetchedDataCache cache = FetchedDataCache.getSingleInstance();
            MsgDataRecord msgData = cache.getMsgDataRecord(((MsgLinkRecord) rec).msgId);
            if (msgData != null && msgData.isTypeAddress())
              objType = com.CH_cl.lang.Lang.rb.getString("Address");
            else 
              objType = com.CH_cl.lang.Lang.rb.getString("Message");
          } else if (isFolder) {
            FolderRecord fldRec = (FolderRecord) rec;
            if (!fldRec.isGroupType())
              objType = com.CH_cl.lang.Lang.rb.getString("Folder");
            else
              objType = com.CH_cl.lang.Lang.rb.getString("Group");
          }
          subject = java.text.MessageFormat.format(com.CH_cl.lang.Lang.rb.getString("msgSubject_Access_Trace_for_OBJECT-TYPE_-_OBJECT-NAME___(id_OBJECT-ID)"), new Object[] {objType, '"'+ListRenderer.getRenderedText(rec)+'"', objId});
        }
        new MessageFrame(uRecs, subject);
      }
    }
  }

  private UserRecord[] getUserRecords(TraceRecord[] traceRecords) {
    UserRecord[] userRecords = null;
    Vector userRecsV = new Vector();
    if (traceRecords != null && traceRecords.length > 0) {
      FetchedDataCache cache = FetchedDataCache.getSingleInstance();
      for (int i=0; i<traceRecords.length; i++) {
        UserRecord uRec = cache.getUserRecord(traceRecords[i].ownerUserId);
        if (uRec != null && !userRecsV.contains(uRec))
          userRecsV.addElement(uRec);
      }
      if (userRecsV.size() > 0) {
        userRecords = new UserRecord[userRecsV.size()];
        userRecsV.toArray(userRecords);
      }
    }
    return userRecords;
  }

  /****************************************************************************/
  /*        A c t i o n P r o d u c e r I                                  
  /****************************************************************************/

  /** @return all the acitons that this objects produces.
   */
  public Action[] getActions() {
    return actions;
  }

  /**
   * Enables or Disables actions based on the current state of the Action Producing component.
   */
  public void setEnabledActions() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(TraceActionTable.class, "setEnabledActions()");
    actions[REFRESH_ACTION].setEnabled(true);
    actions[COPY_TO_CLIPBOARD_ACTION].setEnabled(getTableModel().getRowCount() > 0);

    int count = 0;
    boolean messageOk = true;
    boolean initiateOk = true;

    TraceRecord[] selectedTraceRecords = (TraceRecord[]) getSelectedRecords();
    UserRecord[] selectedUserRecords = getUserRecords(selectedTraceRecords);
    if (selectedUserRecords != null) {

      count = selectedUserRecords.length;

      FetchedDataCache cache = SIL.getFetchedDataCache();
      Long userId = cache.getMyUserId();

      for (int i=0; i<selectedUserRecords.length; i++) {
        UserRecord uRec = selectedUserRecords[i];
        ContactRecord cRec = cache.getContactRecordOwnerWith(userId, uRec.userId);

        if (cRec != null || uRec.userId.equals(userId)) {
          initiateOk = false;
        }

        // If user does not want spam...
        if (((uRec.acceptingSpam.shortValue() & UserRecord.ACC_SPAM_YES_INTER)) == 0) {
          // If we don't have an active contact, then we can't message
          if (cRec == null || !cRec.isOfActiveType())
            messageOk = false;
        }
      }
    }

    try {
      if (count == 0) {
        actions[ADD_TO_CONTACTS_ACTION].setEnabled(false); // This list sometimes throws NullPointerException -- weird!!! why??
        actions[MESSAGE_ACTION].setEnabled(false);
      } else if (count == 1) {
        actions[ADD_TO_CONTACTS_ACTION].setEnabled(initiateOk);
        actions[MESSAGE_ACTION].setEnabled(messageOk);
      } else {
        actions[ADD_TO_CONTACTS_ACTION].setEnabled(false);
        actions[MESSAGE_ACTION].setEnabled(messageOk);
      }
    } catch (NullPointerException e) {
    }
    if (trace != null) trace.exit(TraceActionTable.class);
  }

  /*******************************************************
  *** V i s u a l s S a v a b l e    interface methods ***
  *******************************************************/
  public static final String visualsClassKeyName = "TraceActionTable";
  public String getVisualsClassKeyName() {
    return visualsClassKeyName;
  }
}