/**
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Corp. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Corp.
 */
package com.CH_cl.lang;

import java.io.Serializable;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** 
* Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
*
* <b>$Revision: 1.10 $</b>
*
* @author  Marcin Kurzawa
*/
public class Lang extends Object implements Serializable { // use Serializable as a trick to skip obfuscation

  public static ResourceBundle rb;

  static {
    String className = Lang.class.getName();
    String resourceName = className.substring(0, className.lastIndexOf('.')+1) + "CHLang";
    try {
      rb = java.util.ResourceBundle.getBundle(resourceName);
    } catch (MissingResourceException e) {
    }
  }
}