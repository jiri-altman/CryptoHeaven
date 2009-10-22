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

package com.CH_gui.traceTable;

import java.util.*;
import javax.swing.table.*;

import com.CH_cl.service.actions.*;
import com.CH_cl.service.cache.*;
import com.CH_cl.service.engine.*;
import com.CH_cl.service.ops.*;
import com.CH_cl.service.records.filters.*;

import com.CH_co.service.msg.*;
import com.CH_co.service.msg.dataSets.stat.*;
import com.CH_co.service.records.*;
import com.CH_co.trace.Trace;
import com.CH_co.util.*;

import com.CH_gui.frame.*;
import com.CH_gui.list.*;
import com.CH_gui.table.*;

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
 * <b>$Revision: 1.27 $</b>
 * @author  Marcin Kurzawa
 * @version 
 */
public class TraceTableModel extends RecordTableModel {

  // Parent is either MsgLinkRecord or FileLinkRecord
  private Record[] parentObjLinks;
  // callback to update GUI if some trace records are hidden
  private CallbackI statsRecivedCallback = null;

  public static final int MODE_FILE = 0;
  public static final int MODE_MSG = 1;
  public static final int MODE_MULTI = 2;

  private static String STR_NAME = com.CH_gui.lang.Lang.rb.getString("column_Name");
  private static String STR_PRIVILEGE = com.CH_gui.lang.Lang.rb.getString("column_Privilege");
  private static String STR_HISTORY = com.CH_gui.lang.Lang.rb.getString("column_History");
  private static String STR_USER = com.CH_gui.lang.Lang.rb.getString("column_User");
  private static String STR_FIRST_SEEN = com.CH_gui.lang.Lang.rb.getString("column_First_Seen");
  private static String STR_RECEIVED = com.CH_gui.lang.Lang.rb.getString("column_Received");
  private static String STR_RETRIEVED = com.CH_gui.lang.Lang.rb.getString("column_Retrieved");
  private static String STR_READ = com.CH_gui.lang.Lang.rb.getString("column_Read");

  private static String STR_TIP__READ_ACCESS_PRIVILEGE = com.CH_gui.lang.Lang.rb.getString("columnTip_Read_Access_Privilege");
  private static String STR_TIP__READ_ACCESS_HISTORY = com.CH_gui.lang.Lang.rb.getString("columnTip_Read_Access_History");

  private static final ColumnHeaderData[] columnHeaderDatas = { 
    new ColumnHeaderData(new Object[][] 
        { { STR_NAME, null, null, STR_USER, STR_FIRST_SEEN, STR_RETRIEVED },
          { STR_NAME, STR_PRIVILEGE, STR_HISTORY, STR_USER, STR_FIRST_SEEN, STR_RETRIEVED },
          { null, STR_TIP__READ_ACCESS_PRIVILEGE, STR_TIP__READ_ACCESS_HISTORY, null, null, null },
          { null, new Integer(ImageNums.TRACE_PRIVILEGE12_13), new Integer(ImageNums.TRACE_HISTORY12_13), null, null, null },
          { new Integer(160), new Integer(16), new Integer(16), new Integer(160), new Integer(160), new Integer(160) },
          { new Integer(160), new Integer(16), new Integer(16), new Integer(160), new Integer(160), new Integer(160) },
          { new Integer(160), new Integer(16), new Integer(16), new Integer(160), new Integer(160), new Integer(160) },
          { new Integer(  0), new Integer(16), new Integer(16), new Integer(  0), new Integer(240), new Integer(240) },
          { new Integer( 90), new Integer(16), new Integer(16), new Integer( 90), new Integer(160), new Integer(160) },
          { new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5) },
          { new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5) },
          { new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5) },
          { new Integer(4), new Integer(3) }
        }),
      new ColumnHeaderData(new Object[][]
        { { STR_NAME, null, null, STR_USER, STR_RECEIVED, STR_READ },
          { STR_NAME, STR_PRIVILEGE, STR_HISTORY, STR_USER, STR_RECEIVED, STR_READ },
          { null, STR_TIP__READ_ACCESS_PRIVILEGE, STR_TIP__READ_ACCESS_HISTORY, null, null, null },
          { null, new Integer(ImageNums.TRACE_PRIVILEGE12_13), new Integer(ImageNums.TRACE_HISTORY12_13), null, null, null },
          { new Integer(160), new Integer(16), new Integer(16), new Integer(160), new Integer(160), new Integer(160) },
          { new Integer(160), new Integer(16), new Integer(16), new Integer(160), new Integer(160), new Integer(160) },
          { new Integer(160), new Integer(16), new Integer(16), new Integer(160), new Integer(160), new Integer(160) },
          { new Integer(  0), new Integer(16), new Integer(16), new Integer(  0), new Integer(240), new Integer(240) },
          { new Integer( 90), new Integer(16), new Integer(16), new Integer( 90), new Integer(160), new Integer(160) },
          { new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5) },
          { new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5) },
          { new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5) },
          { new Integer(4), new Integer(3) }
        }),
      new ColumnHeaderData(new Object[][]
        { { STR_NAME, null, null, STR_USER, STR_FIRST_SEEN, STR_RETRIEVED },
          { STR_NAME, STR_PRIVILEGE, STR_HISTORY, STR_USER, STR_FIRST_SEEN, STR_RETRIEVED },
          { null, STR_TIP__READ_ACCESS_PRIVILEGE, STR_TIP__READ_ACCESS_HISTORY, null, null, null },
          { null, new Integer(ImageNums.TRACE_PRIVILEGE12_13), new Integer(ImageNums.TRACE_HISTORY12_13), null, null, null },
          { new Integer(160), new Integer(16), new Integer(16), new Integer(160), new Integer(160), new Integer(160) },
          { new Integer(160), new Integer(16), new Integer(16), new Integer(160), new Integer(160), new Integer(160) },
          { new Integer(160), new Integer(16), new Integer(16), new Integer(160), new Integer(160), new Integer(160) },
          { new Integer(  0), new Integer(16), new Integer(16), new Integer(  0), new Integer(240), new Integer(240) },
          { new Integer( 90), new Integer(16), new Integer(16), new Integer( 90), new Integer(160), new Integer(160) },
          { new Integer(0), new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5) },
          { new Integer(0), new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5) },
          { new Integer(0), new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5) },
          { new Integer(0), new Integer(4), new Integer(3) }
        })
  };

  /** Creates new TraceTableModel */
  public TraceTableModel(Record[] parentObjLinks) {
    super(columnHeaderDatas[parentObjLinks.length == 1 ? (parentObjLinks[0] instanceof MsgLinkRecord ? (MODE_MSG):(MODE_FILE)) : MODE_MULTI]);
    initialize(parentObjLinks);
  }
  private void initialize(Record[] parentObjLinks) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(TraceTableModel.class, "initialize(Record[] parentObjLinks)");
    if (trace != null) trace.args(parentObjLinks);
    this.parentObjLinks = parentObjLinks;
    refreshData();
    if (trace != null) trace.exit(TraceTableModel.class);
  }

  /**
   * When folders are fetched, their IDs are cached so that we know if table fetch is required when
   * user switches focus to another folder...
   * This vector should also be cleared when users are switched...
   */
  public Vector getCachedFetchedFolderIDs() {
    return null;
  }

  /**
   * Sets auto update mode by listening on the cache stat updates.
   */
  public void setAutoUpdate(boolean flag) {
  }

  /**
   * Sets the interface to be notified when stats arrive...
   * @param callback
   */
  public void setStatCallback(CallbackI callback) {
    statsRecivedCallback = callback;
  }

  public Object getValueAtRawColumn(Record record, int column, boolean forSortOnly) {
    Object value = null;

    if (record instanceof TraceRecord) {
      TraceRecord traceRecord = (TraceRecord) record;

      FetchedDataCache cache = null;
      UserRecord uRec = null;

      if (column == 0 || column == 3)
        cache = FetchedDataCache.getSingleInstance();
      if (column == 3) {
        uRec = cache.getUserRecord(traceRecord.ownerUserId);
      }

      switch (column) {
        case 0: 
          Record rec = getTracedObjRecord(traceRecord.objId);
          value = ListRenderer.getRenderedText(rec);
          break;
        case 1: value = Boolean.valueOf(traceRecord.hasReadAccess);
          break;
        case 2: value = Boolean.valueOf(traceRecord.hasHistoryRecord);
          break;
        case 3: value = ListRenderer.getRenderedText(uRec);
          break;
        case 4: value = traceRecord.firstSeen;
          break;
        case 5: value = traceRecord.firstDelivered;
          break;
      }
    }

    return value;
  }

  public RecordTableCellRenderer createRenderer() {
    return new TraceTableCellRenderer();
  }
/*
  public synchronized void initData(Long parentObjLinkId) {
    FetchedDataCache cache = FetchedDataCache.getSingleInstance();
    parentObjLink = cache.getFileLinkRecord(parentObjLinkId);
    if (parentObjLink == null)
      parentObjLink = cache.getMsgLinkRecord(parentObjLinkId);
    refreshData();
  }
*/
  public synchronized void refreshData() {
    new Thread("Trace Refresher") {
      public void run() {
        Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(getClass(), "run()");

        removeData();

        MsgLinkRecord[] msgLinks = (MsgLinkRecord[]) ArrayUtils.gatherAllOfType(parentObjLinks, MsgLinkRecord.class);
        FileLinkRecord[] fileLinks = (FileLinkRecord[]) ArrayUtils.gatherAllOfType(parentObjLinks, FileLinkRecord.class);
        FolderPair[] folderPairs = (FolderPair[]) ArrayUtils.gatherAllOfType(parentObjLinks, FolderPair.class);

        if (msgLinks != null && msgLinks.length > 0) {
          doStatRequestMsgOrFile(msgLinks);
        } 
        if (fileLinks != null && fileLinks.length > 0) {
          doStatRequestMsgOrFile(fileLinks);
        }
        if (folderPairs != null && folderPairs.length > 0) {
          Stats_Get_Rq request = new Stats_Get_Rq();
          request.ownerObjType = new Short(Record.RECORD_TYPE_SHARE);
          request.ownerObjIDs = FolderPair.getShareIDs(folderPairs);
          request.statsForObjType = request.ownerObjType;
          request.objLinkIDs = new Long[0];
          doStatRequest(request);
        }

        if (trace != null) trace.data(300, Thread.currentThread().getName() + " done.");
        if (trace != null) trace.exit(getClass());
        if (trace != null) trace.clear();
      }

      private void doStatRequestMsgOrFile(LinkRecordI[] links) {
        Stats_Get_Rq request = new Stats_Get_Rq();
        request.statsForObjType = new Short(links[0] instanceof MsgLinkRecord ? Record.RECORD_TYPE_MSG_LINK : Record.RECORD_TYPE_FILE_LINK);
        request.objLinkIDs = RecordUtils.getIDs((Record[]) links);
        if (links[0] instanceof MsgLinkRecord && ((MsgLinkRecord) links[0]).ownerObjType.shortValue() == Record.RECORD_TYPE_MESSAGE) {
          request.ownerObjType = new Short(Record.RECORD_TYPE_MESSAGE);
          Long[] msgIDs = links[0].getOwnerObjIDs(links, Record.RECORD_TYPE_MESSAGE);
          request.ownerObjIDs = msgIDs;
        } else {
          request.ownerObjType = new Short(Record.RECORD_TYPE_SHARE);
          Long[] folderIDs = links[0].getOwnerObjIDs(links, Record.RECORD_TYPE_FOLDER);
          Long[] shareIDs = RecordUtils.getIDs(FetchedDataCache.getSingleInstance().getFolderSharesMyForFolders(folderIDs, true));
          request.ownerObjIDs = shareIDs;
        }
        doStatRequest(request);
      }

      private void doStatRequest(Stats_Get_Rq request) {
        Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(getClass(), "doStatRequest()");
        MessageAction msgAction = new MessageAction(CommandCodes.STAT_Q_FETCH_ALL_OBJ_STATS, request);
        ServerInterfaceLayer serverInterfaceLayer = MainFrame.getServerInterfaceLayer();
        ClientMessageAction reply = serverInterfaceLayer.submitAndFetchReply(msgAction, 60000);
        if (trace != null) trace.data(10, reply);
        if (reply != null && reply.getActionCode() == CommandCodes.STAT_A_GET) {
          Stats_Get_Rp statsReply = (Stats_Get_Rp) reply.getMsgDataSet();
          if (trace != null) trace.data(20, statsReply);
          UserOps.fetchUnknownUsers(serverInterfaceLayer, statsReply.stats);
          updateData(statsReply.stats);
          if (statsRecivedCallback != null)
            statsRecivedCallback.callback(statsReply);
          // Clear the data from reply, we don't want it going to the cache.
          statsReply.stats = new StatRecord[0];
        }
        DefaultReplyRunner.nonThreadedRun(serverInterfaceLayer, reply);
        if (trace != null) trace.exit(getClass());
      }
    }.start();
  }

  /**
   * Checks if folder share's content of a given ID was already retrieved.
   */
  public boolean isContentFetched(Long shareId) {
    return false;
  }

  /**
   * Return the Record being traced by link id.
   */
  public Record getTracedObjRecord(Long objId) {
    Record rec = null;
    if (objId != null) {
      for (int i=0; i<parentObjLinks.length; i++) {
        Record pRec = parentObjLinks[i];
        if (pRec.getId().equals(objId)) {
          rec = pRec;
          break;
        } else if (pRec instanceof MsgLinkRecord) {
          MsgLinkRecord mRec = (MsgLinkRecord) pRec;
          if (mRec.msgId.equals(objId)) {
            rec = pRec;
            break;
          }
        } else if (pRec instanceof FileLinkRecord) {
          FileLinkRecord fRec = (FileLinkRecord) pRec;
          if (fRec.fileId.equals(objId)) {
            rec = fRec;
            break;
          }
        } 
      }
    }
    return rec;
  }

}