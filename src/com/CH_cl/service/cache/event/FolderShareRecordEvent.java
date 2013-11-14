/**
 * Copyright 2001-2013 CryptoHeaven Corp. All Rights Reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Corp. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Corp.
 */
package com.CH_cl.service.cache.event;

import com.CH_cl.service.cache.*;

import com.CH_co.trace.Trace;
import com.CH_co.service.records.FolderShareRecord;

/** 
 * Copyright 2001-2013 CryptoHeaven Corp. All Rights Reserved.
 *
 * <b>$Revision: 1.8 $</b>
 *
 * @author  Marcin Kurzawa
 */
public class FolderShareRecordEvent extends RecordEvent {

  /** Creates new FolderShareRecordEvent */
  public FolderShareRecordEvent(FetchedDataCache source, FolderShareRecord[] folderShareRecords, int eventType) {
    super(source, folderShareRecords, eventType);
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FolderShareRecordEvent.class, "(FetchedDataCach, FolderShareRecord[], int eventType)");
    if (trace != null) trace.exit(FolderShareRecordEvent.class);
  }
  
  public FolderShareRecord[] getFolderShareRecords() {
    return (FolderShareRecord[]) records;
  }

}