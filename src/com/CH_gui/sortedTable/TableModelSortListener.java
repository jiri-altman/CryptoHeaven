/**
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Corp. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Corp.
 */
package com.CH_gui.sortedTable;

import java.util.EventListener;

/** 
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * @author  Marcin Kurzawa
 */
public interface TableModelSortListener extends EventListener {

  public void deleteNotify(TableModelSortEvent event);
  public void preSortNotify(TableModelSortEvent event);
  public void postSortNotify(TableModelSortEvent event);
  
}
