/**
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
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
import com.CH_co.service.records.MsgDataRecord;

/** 
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * <b>$Revision: 1.8 $</b>
 *
 * @author  Marcin Kurzawa
 */
public class MsgDataRecordEvent extends RecordEvent {

  /** Creates new MsgDataRecordEvent */
  public MsgDataRecordEvent(FetchedDataCache source, MsgDataRecord[] dataRecords, int eventType) {
    super(source, dataRecords, eventType);
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgDataRecordEvent.class, "MsgDataRecordEvent()");
    if (trace != null) trace.exit(MsgDataRecordEvent.class);
  }


  public MsgDataRecord[] getMsgDataRecords() {
    return (MsgDataRecord[]) records;
  }

}