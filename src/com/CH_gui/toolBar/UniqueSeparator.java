/**
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Corp. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Corp.
 */
package com.CH_gui.toolBar;

import javax.swing.*;

import com.CH_gui.list.List_Viewable;
import com.CH_gui.menuing.MenuActionItem;

/** 
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * <b>$Revision: 1.11 $</b>
 *
 * @author  Marcin Kurzawa
 */
public class UniqueSeparator extends JSeparator implements List_Viewable {

  private static int count;
  private int id;

  public UniqueSeparator() {
    id = count;
    count++;
    if (count == Integer.MAX_VALUE)
      count = 0;
  }

  public boolean equals(Object o) {
    if (o instanceof UniqueSeparator)
      return id == ((UniqueSeparator) o).id;
    else
      return super.equals(o);
  }
  public int hashCode() {
    return id;
  }


  /** List_Viewable interface methods. **/

  public String getLabel() {
    return MenuActionItem.STR_SEPARATOR;
  }

  public Icon getIcon() {
    return null;
  }

}