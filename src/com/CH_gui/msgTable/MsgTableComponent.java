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

package com.CH_gui.msgTable;

import com.CH_co.trace.Trace;

import com.CH_gui.gui.*;
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
 * <b>$Revision: 1.16 $</b>
 * @author  Marcin Kurzawa
 * @version 
 */
public class MsgTableComponent extends RecordTableComponent {

  /** Creates new MsgTableComponent */
  public MsgTableComponent() {
    this(Template.get(Template.EMPTY_MAIL), false);
  }
  public MsgTableComponent(String emptyTemplateName, boolean msgPreviewMode) {
    this(Template.get(Template.EMPTY_MAIL), Template.get(Template.NONE), Template.get(Template.CATEGORY_MAIL), msgPreviewMode);
  }
  public MsgTableComponent(String emptyTemplateName, String backTemplateName, String categoryTemplateName, boolean msgPreviewMode) {
    super(new MsgActionTable(msgPreviewMode), emptyTemplateName, backTemplateName, categoryTemplateName, msgPreviewMode);
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgTableComponent.class, "MsgTableComponent()");
    if (trace != null) trace.exit(MsgTableComponent.class);
  }

  public void initDataModel(Long folderId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgTableComponent.class, "initDataModel(Long folderId)");
    if (trace != null) trace.args(folderId);
    ((MsgTableModel) getActionTable().getTableModel()).initData(folderId);
    if (trace != null) trace.exit(MsgTableComponent.class);
  }

  /*******************************************************
  *** V i s u a l s S a v a b l e    interface methods ***
  *******************************************************/
  public static final String visualsClassKeyName = "MsgTableComponent";
  public String getVisualsClassKeyName() {
    return visualsClassKeyName;
  }
}