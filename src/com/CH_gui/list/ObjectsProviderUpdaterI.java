/**
 * Copyright 2001-2013 CryptoHeaven Corp. All Rights Reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Corp. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Corp.
 */
package com.CH_gui.list;

import com.CH_co.util.DisposableObj;
import com.CH_co.util.ObjectsProviderI;

/**
* Copyright 2001-2013 CryptoHeaven Corp. All Rights Reserved.
*
* <b>$Revision: 1.4 $</b>
*
* @author  Marcin Kurzawa
*/
public interface ObjectsProviderUpdaterI extends ObjectsProviderI, DisposableObj {

  public Object[] provide(Object args, ListUpdatableI updatable);
  public void registerForUpdates(ListUpdatableI updatable);

}