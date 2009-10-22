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

package com.CH_co.cryptx;

import com.CH_co.trace.Trace;

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
 * <b>$Revision: 1.11 $</b>
 * @author  Marcin Kurzawa
 * @version 
 */
public class BADigestBlock extends BA {

  /** Creates new BADigestBlock */
  public BADigestBlock(byte[] digestContent) {
    super(digestContent);
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(BADigestBlock.class, "BADigestBlock()");
    if (trace != null) trace.exit(BADigestBlock.class);
  }
  
  /** Creates new BADigestBlock */
  public BADigestBlock(BA digestContent) {
    super(digestContent);
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(BADigestBlock.class, "BADigestBlock()");
    if (trace != null) trace.exit(BADigestBlock.class);
  }
}