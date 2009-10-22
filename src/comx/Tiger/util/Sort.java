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
// Source File Name:   Sort.java

package comx.Tiger.util;


// Referenced classes of package com.wintertree.util:
//      Comparable

public class Sort {

  public Sort() {
  }

  public static void ascending(String as[]) {
    for (int i = as.length / 2; i > 0; i /= 2) {
      for (int j = i; j < as.length; j++) {
        for (int k = j - i; k >= 0 && as[k].compareTo(as[k + i]) > 0; k -= i) {
          String s = as[k];
          as[k] = as[k + i];
          as[k + i] = s;
        }
      }
    }
  }

  public static void ascending(Comparable acomparable[]) {
    for (int i = acomparable.length / 2; i > 0; i /= 2) {
      for (int j = i; j < acomparable.length; j++) {
        for (int k = j - i; k >= 0 && acomparable[k].compareTo(acomparable[k + i]) > 0; k -= i) {
          Comparable comparable = acomparable[k];
          acomparable[k] = acomparable[k + i];
          acomparable[k + i] = comparable;
        }
      }
    }
  }

  public static void descending(Comparable acomparable[]) {
    for (int i = acomparable.length / 2; i > 0; i /= 2) {
      for (int j = i; j < acomparable.length; j++) {
        for (int k = j - i; k >= 0 && acomparable[k].compareTo(acomparable[k + i]) <= 0; k -= i) {
          Comparable comparable = acomparable[k];
          acomparable[k] = acomparable[k + i];
          acomparable[k + i] = comparable;
        }
      }
    }
  }
}