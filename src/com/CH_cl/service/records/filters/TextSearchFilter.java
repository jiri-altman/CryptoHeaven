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

import com.CH_cl.util.SearchTextProviderI;
import com.CH_cl.service.cache.FetchedDataCache;
import com.CH_co.trace.Trace;
import com.CH_co.service.records.*;
import com.CH_co.service.records.filters.*;
import com.CH_co.util.*;

import java.util.*;

/**
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * <b>$Revision: 1.1 $</b>
 *
 * @author  Marcin Kurzawa
 */
public class TextSearchFilter extends AbstractRecordFilter implements RecordFilter {

  private FetchedDataCache cache;
  private String searchStr;
  private String[] searchTokens;
  private boolean includeMsgBodies;
  private SearchTextProviderI searchTextProvider;

  /** Creates new TextSearchFilter */
  public TextSearchFilter(FetchedDataCache cache, String searchStr, boolean includeMsgBodies, SearchTextProviderI searchTextProvider) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(TextSearchFilter.class, "TextSearchFilter(String searchStr, boolean includeMsgBodies, SearchTextProviderI searchTextProvider)");
    if (trace != null) trace.args(searchStr);
    if (trace != null) trace.args(includeMsgBodies);
    if (trace != null) trace.args(searchTextProvider);
    this.cache = cache;
    this.includeMsgBodies = includeMsgBodies;
    this.searchTextProvider = searchTextProvider;
    setSearchStr(searchStr);
    if (trace != null) trace.exit(TextSearchFilter.class);
  }

  public final void setSearchStr(String searchStr) {
    this.searchStr = searchStr;
    this.searchTokens = searchStr.split("[ ]+");
  }

  public boolean keep(Record record) {
    boolean keep = false;

    if (record instanceof FileLinkRecord) {
      FileLinkRecord fLink = (FileLinkRecord) record;
      if (isMatch(searchTextProvider.getSearchableCharSequencesFor(cache, fLink)))
        keep = true;
    } else if (record instanceof MsgLinkRecord) {
      MsgLinkRecord mLink = (MsgLinkRecord) record;
      if (isMatch(searchTextProvider.getSearchableCharSequencesFor(cache, mLink, includeMsgBodies)))
        keep = true;
    } else {
      if (isMatch(searchTextProvider.getSearchableCharSequencesFor(cache, record)))
        keep = true;
    }

    return keep;
  }

  private boolean isMatch(Collection charSequences) {
    boolean isMatch = true;
    if (charSequences == null) {
      isMatch = false;
    } else if (charSequences.size() > 0 && searchTokens.length > 0) {
      for (int i=0; i<searchTokens.length; i++) {
        boolean anyFound = false;
        Iterator iter = charSequences.iterator();
        while (iter.hasNext()) {
          CharSequence charSeq = (CharSequence) iter.next();
          // charSeq maybe null in rare cases so check for it...
          if (charSeq != null) {
            if (contains(charSeq, searchTokens[i], true)) {
              anyFound = true;
              break;
            }
          }
        }
        if (!anyFound) {
          isMatch = false;
          break;
        }
      }
    }
    return isMatch;
  }

  private static boolean contains(CharSequence string, CharSequence phrase, boolean ignoreCase) {
    int stringLen = string.length();
    int subStringLen = phrase.length();
    if (subStringLen == 0)
      return true;
    else {
      for (int i = 0; i <= stringLen - subStringLen; i++) {
        if (regionMatches(ignoreCase, i, string, 0, phrase, subStringLen)) {
          return true;
        }
      }
    }
    return false;
  }
  private static boolean regionMatches(boolean ignoreCase, int strOffset, CharSequence string, int phraseOffset, CharSequence phrase, int len) {
    boolean match = true;
    for (int i=0; i<len; i++) {
      char c1 = string.charAt(i+strOffset);
      char c2 = phrase.charAt(i+phraseOffset);
      boolean same = c1 == c2 || (ignoreCase && Character.toLowerCase(c1) == Character.toLowerCase(c2));
      if (!same) {
        match = false;
        break;
      }
    }
    return match;
  }

  public String getSearchStr() {
    return searchStr;
  }

  public boolean isIncludingMsgBodies() {
    return includeMsgBodies;
  }

  public void setIncludingMsgBodies(boolean includeMsgBodies) {
    this.includeMsgBodies = includeMsgBodies;
  }

  public void setSearchTextProvider(SearchTextProviderI searchTextProvider) {
    this.searchTextProvider = searchTextProvider;
  }

  public String toString() {
    return "[TextSearchFilter"
      + ": searchStr=" + searchStr
      + ", includeMsgBodies=" + includeMsgBodies
      + ", searchTextProvider=" + searchTextProvider
      + "]";
  }
}