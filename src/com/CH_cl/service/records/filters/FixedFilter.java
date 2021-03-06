/**
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Corp. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Corp.
 */
package com.CH_cl.service.records.filters;

import com.CH_co.trace.Trace;
import com.CH_co.service.records.*;
import com.CH_co.service.records.filters.*;

/** 
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * <b>$Revision: 1.7 $</b>
 *
 * @author  Marcin Kurzawa
 */
public class FixedFilter extends AbstractRecordFilter implements RecordFilter {

  private boolean keep;

  /** Creates new FixedFilter */
  public FixedFilter(boolean keep) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FixedFilter.class, "FixedFilter(boolean keep)");
    if (trace != null) trace.args(keep);
    this.keep = keep;
    if (trace != null) trace.exit(FixedFilter.class);
  }

  public boolean keep(Record record) {
    return keep;
  }

}