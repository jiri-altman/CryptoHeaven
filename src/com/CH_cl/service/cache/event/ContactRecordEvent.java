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

package com.CH_cl.service.cache.event;

import com.CH_cl.service.cache.*;

import com.CH_co.trace.Trace;
import com.CH_co.service.records.ContactRecord;

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
 * <b>$Revision: 1.8 $</b>
 * @author  Marcin Kurzawa
 * @version 
 */
public class ContactRecordEvent extends RecordEvent {

  /** Creates new ContactRecordEvent */
  public ContactRecordEvent(FetchedDataCache source, ContactRecord[] contactRecords, int eventType) {
    super(source, contactRecords, eventType);
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(ContactRecordEvent.class, "ContactRecordEvent()");
    if (trace != null) trace.exit(ContactRecordEvent.class);
  }

  public ContactRecord[] getContactRecords() {
    return (ContactRecord[]) records;
  }

}