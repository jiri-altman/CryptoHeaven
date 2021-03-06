/**
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Corp. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Corp.
 */
package comx.Tiger.ssce;

class ContainsWordCatcher implements WordCatcher {

  protected boolean caseSensitive;
  protected boolean found;
  protected String keyWord;

  public ContainsWordCatcher() {
    found = false;
    keyWord = null;
    caseSensitive = true;
    found = false;
  }

  public ContainsWordCatcher(String s, boolean flag) {
    found = false;
    keyWord = s;
    caseSensitive = flag;
    found = false;
  }

  public boolean catchWord(String s) {
    if (caseSensitive) {
      if (keyWord.equals(s)) {
        found = true;
        return false;
      } else {
        return keyWord.compareTo(s) > 0;
      }
    }
    if (keyWord.equalsIgnoreCase(s)) {
      found = true;
    }
    return true;
  }

  public void setCaseSensitive(boolean flag) {
    caseSensitive = flag;
  }

  public void setFound(boolean flag) {
    found = flag;
  }

  public void setKeyWord(String s) {
    keyWord = s;
  }

  public boolean IsFound() {
    return found;
  }
}