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

package comx.Jaguar.gui;

import java.awt.*;
import javax.swing.*;

import com.CH_co.gui.MyInsets;

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
 * <b>$Revision: 1.4 $</b>
 * @author  Marcin Kurzawa
 * @version 
 */
/** Class for providing a JToggleButton that does not obtain focus
    */
public class JToggleButtonNoFocus extends JToggleButton {
  public JToggleButtonNoFocus()                       { super();           this.setRequestFocusEnabled(false); this.setMargin(new MyInsets(1,1,1,1));}
  public JToggleButtonNoFocus(Action a)               { super(a);          this.setRequestFocusEnabled(false); this.setMargin(new MyInsets(1,1,1,1));}
  public JToggleButtonNoFocus(Icon icon)              { super(icon);       this.setRequestFocusEnabled(false); this.setMargin(new MyInsets(1,1,1,1));}
  public JToggleButtonNoFocus(String text)            { super(text);       this.setRequestFocusEnabled(false); this.setMargin(new MyInsets(1,1,1,1));}
  public JToggleButtonNoFocus(String text, Icon icon) { super(text, icon); this.setRequestFocusEnabled(false); this.setMargin(new MyInsets(1,1,1,1));}

  public boolean isFocusTraversable() {
    return false;
  }
}