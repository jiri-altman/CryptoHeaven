/* The following code was generated by JFlex 1.3.5 on 10/9/03 8:07 PM */

/* CSVLexer.java is a generated file.  You probably want to
 * edit CSVLexer.lex to make changes.  Use JFlex to generate it.
 * JFlex may be obtained from
 * <a href="http://jflex.de">the JFlex website</a>.
 * Once JFlex is in your classpath run<br>
 * java --skel csv.jflex.skel JFlex.Main CSVLexer.lex<br>
 * You will then have a file called CSVLexer.java
 */

/*
 * Read files in comma separated value format.
 * Copyright (C) 2001-2003 Stephen Ostermiller
 * http://ostermiller.org/contact.pl?regarding=Java+Utilities
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * See COPYING.TXT for details.
 */

package com.CH_gui.csv;
import java.io.*;

/**
 * Read files in comma separated value format.
 *
 * The use of this class is no longer recommended.	It is now recommended that you use
 * com.Ostermiller.util.CSVParser instead.	That class, has a cleaner API, and methods
 * for returning all the values on a line in a String array.
 *
 * CSV is a file format used as a portable representation of a database.
 * Each line is one entry or record and the fields in a record are separated by commas.
 * Commas may be preceded or followed by arbitrary space and/or tab characters which are
 * ignored.
 * <P>
 * If field includes a comma or a new line, the whole field must be surrounded with double quotes.
 * When the field is in quotes, any quote literals must be escaped by \" Backslash
 * literals must be escaped by \\.	Otherwise a backslash an the character following it
 * will be treated as the following character, ie."\n" is equivelent to "n".  Other escape
 * sequences may be set using the setEscapes() method.	Text that comes after quotes that have
 * been closed but come before the next comma will be ignored.
 * <P>
 * Empty fields are returned as as String of length zero: "".  The following line has four empty
 * fields and two non-empty fields in it.  There is an empty field on each end, and two in the
 * middle.<br>
 * <pre>,second,, ,fifth,</pre>
 * <P>
 * Blank lines are always ignored.	Other lines will be ignored if they start with a
 * comment character as set by the setCommentStart() method.
 * <P>
 * An example of how CVSLexer might be used:
 * <pre>
 * CSVLexer shredder = new CSVLexer(System.in);
 * shredder.setCommentStart("#;!");
 * shredder.setEscapes("nrtf", "\n\r\t\f");
 * String t;
 * while ((t = shredder.getNextToken()) != null) {
 *	   System.out.println("" + shredder.getLineNumber() + " " + t);
 * }
 * </pre>
 *
 * @author Stephen Ostermiller http://ostermiller.org/contact.pl?regarding=Java+Utilities
 * @since ostermillerutils 1.00.00
 */


/**
 * This class is a scanner generated by
 * <a href="http://www.jflex.de/">JFlex</a> 1.3.5
 * on 10/9/03 8:07 PM from the specification file
 * <tt>file:/C:/a/ostermillerutils_1_02_18/com/Ostermiller/util/CSVLexer.lex</tt>
 */
public class CSVLexer {

  /** This character denotes the end of file */
  private static final int YYEOF = -1;

  /** initial size of the lookahead buffer */
  final private static int YY_BUFFERSIZE = 16384;

  /** lexical states */
  final public static int BEFORE = 1;
  final public static int YYINITIAL = 0;
  final public static int COMMENT = 3;
  final public static int AFTER = 2;

  /**
   * Translates characters to character classes
   */
  final private static String yycmap_packed =
  "\11\0\1\1\1\3\1\0\1\1\1\2\22\0\1\1\1\0\1\5"+
  "\11\0\1\4\57\0\1\6\uffa3\0";

  /**
   * Translates characters to character classes
   */
  final private static char [] yycmap = yy_unpack_cmap(yycmap_packed);

  /**
   * Translates a state to a row index in the transition table
   */
  final private static int yy_rowMap [] = {
    0,     7,    14,    21,    28,    35,    42,    49,    49,    56,
    63,    70,    77,    49,    49,    84,    91,    98,    49,   105,
    28,    35,    49,   112,    63,    49,   119,    56,    84
  };

  /**
   * The packed transition table of the DFA (part 0)
   */
  final private static String yy_packed0 =
  "\1\5\1\6\1\7\1\10\1\11\1\12\1\5\1\13"+
  "\1\14\1\15\1\16\1\17\1\20\1\13\1\21\1\22"+
  "\1\7\1\10\1\23\2\21\1\24\1\4\1\7\1\10"+
  "\3\24\1\5\1\25\3\0\2\5\1\0\1\26\1\7"+
  "\1\10\6\0\1\10\12\0\5\12\1\27\1\30\1\13"+
  "\1\31\3\0\2\13\1\0\1\14\10\0\1\16\3\0"+
  "\5\20\1\32\1\33\2\21\3\0\3\21\1\22\1\7"+
  "\1\10\1\0\2\21\2\24\2\0\3\24\5\12\1\34"+
  "\1\30\5\20\1\35\1\33";

  /**
   * The transition table of the DFA
   */
  final private static int yytrans [] = yy_unpack();

  /**
   * Per instance reference to the character map.
   * This can be cloned and modified per instance if needed.
   * It is initally set to the static value.
   */
  private char [] yycmap_instance = yycmap;

  /* error codes */
  private static final int YY_UNKNOWN_ERROR = 0;
  private static final int YY_ILLEGAL_STATE = 1;
  private static final int YY_NO_MATCH = 2;
  private static final int YY_PUSHBACK_2BIG = 3;

  /* error messages for the codes above */
  private static final String YY_ERROR_MSG[] = {
    "Unkown internal scanner error",
    "Internal error: unknown state",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * YY_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>
   */
  private final static byte YY_ATTRIBUTE[] = {
    0,  1,  1,  1,  1,  1,  1,  9,  9,  1,  1,  1,  1,  9,  9,  1,
    1,  1,  9,  1,  0,  0,  9,  1,  0,  9,  1,  1,  1
  };

  /** the input device */
  private java.io.Reader yy_reader;

  /** the current state of the DFA */
  private int yy_state;

  /** the current lexical state */
  private int yy_lexical_state = YYINITIAL;

  /** this buffer contains the current text to be matched and is
   * the source of the yytext() string */
  private char yy_buffer[] = new char[YY_BUFFERSIZE];

  /** the textposition at the last accepting state */
  private int yy_markedPos;

  /** the textposition at the last state to be included in yytext */
  private int yy_pushbackPos;

  /** the current text position in the buffer */
  private int yy_currentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int yy_startRead;

  /** endRead marks the last character in the buffer, that has been read
   * from input */
  private int yy_endRead;

  /** yy_atEOF == true <=> the scanner is at the EOF */
  private boolean yy_atEOF;

  /* user code: */
  /**
   * Prints out tokens and line numbers from a file or System.in.
   * If no arguments are given, System.in will be used for input.
   * If more arguments are given, the first argument will be used as
   * the name of the file to use as input
   *
   * @param args program arguments, of which the first is a filename
   *
   * @since ostermillerutils 1.00.00
   */
  private static void main(String[] args){
    InputStream in;
    try {
      if (args.length > 0){
        File f = new File(args[0]);
        if (f.exists()){
          if (f.canRead()){
            in = new BufferedInputStream(new FileInputStream(f), 32*1024);
          } else {
            throw new IOException("Could not open " + args[0]);
          }
        } else {
          throw new IOException("Could not find " + args[0]);
        }
      } else {
        in = System.in;
      }
      CSVLexer shredder = new CSVLexer(in);
      shredder.setCommentStart("#;!");
      shredder.setEscapes("nrtf", "\n\r\t\f");
      String t;
      while ((t = shredder.getNextToken()) != null) {
        System.out.println("" + shredder.getLineNumber() + " " + t);
      }
      in.close();
    } catch (IOException e){
      System.out.println(e.getMessage());
    }
  }

  private char delimiter = ',';
  private char quote = '\"';

  /**
   * Checks that yycmap_instance is an instance variable (not just
   * a pointer to a static variable).  If it is a pointer to a static
   * variable, it will be cloned.
   *
   * @since ostermillerutils 1.00.00
   */
  private void ensureCharacterMapIsInstance(){
    if (yycmap == yycmap_instance){
      yycmap_instance = new char[yycmap.length];
      System.arraycopy(yycmap, 0, yycmap_instance, 0, yycmap.length);
    }
  }

  /**
   * Ensures that the given character is not used for some special purpose
   * in parsing.  This method should be called before setting some character
   * to be a delimiter so that the parsing doesn't break.  Examples of bad
   * characters are quotes, commas, and whitespace.
   *
   * @since ostermillerutils 1.00.00
   */
  private boolean charIsSafe(char c){
    // There are two character classes that one could use as a delimiter.
    // The first would be the class that most characters are in.  These
    // are normally data.  The second is the class that the tab is usually in.
    return (yycmap_instance[c] == yycmap['a'] || yycmap_instance[c] == yycmap['\t']);
  }

  /**
   * Change the character classes of the two given characters.  This
   * will make the state machine behave as if the characters were switched
   * when they are encountered in the input.
   *
   * @param old the old character, its value will be returned to initial
   * @param two second character
   *
   * @since ostermillerutils 1.00.00
   */
  private void updateCharacterClasses(char oldChar, char newChar){
    // before modifying the character map, make sure it isn't static.
    ensureCharacterMapIsInstance();
    // make the newChar behave like the oldChar
    yycmap_instance[newChar] = yycmap_instance[oldChar];
    // make the oldChar behave like it isn't special.
    switch (oldChar){
      case ',':
      case '"': {
        // These should act as normal character,
        // not delimiters or quotes right now.
        yycmap_instance[oldChar] = yycmap['a'];
      } break;
      default: {
        // Set the it back to the way it would act
        // if not used as a delimiter or quote.
        yycmap_instance[oldChar] = yycmap[oldChar];
      } break;
    }
  }

  /**
   * Change this Lexer so that it uses a new delimiter.
   * <p>
   * The initial character is a comma, the delimiter cannot be changed
   * to a quote or other character that has special meaning in CSV.
   *
   * @param newDelim delimiter to which to switch.
   * @throws BadDelimeterException if the character cannot be used as a delimiter.
   *
   * @since ostermillerutils 1.00.00
   */
  public void changeDelimiter(char newDelim) throws BadDelimeterException {
    if (newDelim == delimiter) return; // no need to do anything.
    if (!charIsSafe(newDelim)){
      throw new BadDelimeterException(newDelim + " is not a safe delimiter.");
    }
    updateCharacterClasses(delimiter, newDelim);
    // keep a record of the current delimiter.
    delimiter = newDelim;
  }

  /**
   * Change this Lexer so that it uses a new character for quoting.
   * <p>
   * The initial character is a double quote ("), the delimiter cannot be changed
   * to a comma or other character that has special meaning in CSV.
   *
   * @param newQuote character to use for quoting.
   * @throws BadQuoteException if the character cannot be used as a quote.
   *
   * @since ostermillerutils 1.00.00
   */
  public void changeQuote(char newQuote) throws BadQuoteException {
    if (newQuote == quote) return; // no need to do anything.
    if (!charIsSafe(newQuote)){
      throw new BadQuoteException(newQuote + " is not a safe quote.");
    }
    updateCharacterClasses(quote, newQuote);
    // keep a record of the current quote.
    quote = newQuote;
  }

  private String escapes = "";
  private String replacements = "";

  /**
   * Specify escape sequences and their replacements.
   * Escape sequences set here are in addition to \\ and \".
   * \\ and \" are always valid escape sequences.  This method
   * allows standard escape sequenced to be used.  For example
   * "\n" can be set to be a newline rather than an 'n'.
   * A common way to call this method might be:<br>
   * <code>setEscapes("nrtf", "\n\r\t\f");</code><br>
   * which would set the escape sequences to be the Java escape
   * sequences.  Characters that follow a \ that are not escape
   * sequences will still be interpreted as that character.<br>
   * The two arguemnts to this method must be the same length.  If
   * they are not, the longer of the two will be truncated.
   *
   * @param escapes a list of characters that will represent escape sequences.
   * @param replacements the list of repacement characters for those escape sequences.
   *
   * @since ostermillerutils 1.00.00
   */
  public void setEscapes(String escapes, String replacements){
    int length = escapes.length();
    if (replacements.length() < length){
      length = replacements.length();
    }
    this.escapes = escapes.substring(0, length);
    this.replacements = replacements.substring(0, length);
  }

  private String unescape(String s){
    if (s.indexOf('\\') == -1){
      return s.substring(1, s.length()-1);
    }
    StringBuffer sb = new StringBuffer(s.length());
    for (int i=1; i<s.length()-1; i++){
      char c = s.charAt(i);
      if (c == '\\'){
        char c1 = s.charAt(++i);
        int index;
        if (c1 == '\\' || c1 == '\"'){
          sb.append(c1);
        } else if ((index = escapes.indexOf(c1)) != -1){
          sb.append(replacements.charAt(index));
        } else {
          sb.append(c1);
        }
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private String commentDelims = "";

  /**
   * Set the characters that indicate a comment at the beginning of the line.
   * For example if the string "#;!" were passed in, all of the following lines
   * would be comments:<br>
   * <pre> # Comment
   * ; Another Comment
   * ! Yet another comment</pre>
   * By default there are no comments in CVS files.  Commas and quotes may not be
   * used to indicate comment lines.
   *
   * @param commentDelims list of characters a comment line may start with.
   *
   * @since ostermillerutils 1.00.00
   */
  public void setCommentStart(String commentDelims){
    this.commentDelims = commentDelims;
  }

  private boolean addLine = true;
  private int lines = 0;

  /**
   * Get the line number that the last token came from.
   * <p>
   * New line breaks that occur in the middle of a token are not
   * counted in the line number count.
   * <p>
   * If no tokens have been returned, the line number is undefined.
   *
   * @return line number of the last token.
   *
   * @since ostermillerutils 1.00.00
   */
  public int getLineNumber(){
    return lines;
  }


  /**
   * Creates a new scanner
   * There is also a java.io.InputStream version of this constructor.
   *
   * @param   in  the java.io.Reader to read input from.
   */
  public CSVLexer(java.io.Reader in) {
    this.yy_reader = in;
  }

  /**
   * Creates a new scanner.
   * There is also java.io.Reader version of this constructor.
   *
   * @param   in  the java.io.Inputstream to read input from.
   */
  public CSVLexer(java.io.InputStream in) {
    this(new java.io.InputStreamReader(in));
  }

  /**
   * Unpacks the split, compressed DFA transition table.
   *
   * @return the unpacked transition table
   */
  private static int [] yy_unpack() {
    int [] trans = new int[126];
    int offset = 0;
    offset = yy_unpack(yy_packed0, offset, trans);
    return trans;
  }

  /**
   * Unpacks the compressed DFA transition table.
   *
   * @param packed   the packed transition table
   * @return         the index of the last entry
   */
  private static int yy_unpack(String packed, int offset, int [] trans) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do trans[j++] = value; while (--count > 0);
    }
    return j;
  }

  /**
   * Unpacks the compressed character translation table.
   *
   * @param packed   the packed character translation table
   * @return         the unpacked character translation table
   */
  private static char [] yy_unpack_cmap(String packed) {
    char [] map = new char[0x10000];
    int i = 0;  /* index in packed string  */
    int j = 0;  /* index in unpacked array */
    while (i < 30) {
      int  count = packed.charAt(i++);
      char value = packed.charAt(i++);
      do map[j++] = value; while (--count > 0);
    }
    return map;
  }


  /**
   * Refills the input buffer.
   *
   * @return      <code>false</code>, iff there was new input.
   *
   * @exception   IOException  if any I/O-Error occurs
   */
  private boolean yy_refill() throws java.io.IOException {

    /* first: make room (if you can) */
    if (yy_startRead > 0) {
      System.arraycopy(yy_buffer, yy_startRead,
      yy_buffer, 0,
      yy_endRead-yy_startRead);

      /* translate stored positions */
      yy_endRead-= yy_startRead;
      yy_currentPos-= yy_startRead;
      yy_markedPos-= yy_startRead;
      yy_pushbackPos-= yy_startRead;
      yy_startRead = 0;
    }

    /* is the buffer big enough? */
    if (yy_currentPos >= yy_buffer.length) {
      /* if not: blow it up */
      char newBuffer[] = new char[yy_currentPos*2];
      System.arraycopy(yy_buffer, 0, newBuffer, 0, yy_buffer.length);
      yy_buffer = newBuffer;
    }

    /* finally: fill the buffer with new input */
    int numRead = yy_reader.read(yy_buffer, yy_endRead,
    yy_buffer.length-yy_endRead);

    if (numRead < 0) {
      return true;
    }
    else {
      yy_endRead+= numRead;
      return false;
    }
  }

  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  private final void yybegin(int newState) {
    yy_lexical_state = newState;
  }

  /**
   * Returns the text matched by the current regular expression.
   */
  private final String yytext() {
    return new String( yy_buffer, yy_startRead, yy_markedPos-yy_startRead );
  }

  /**
   * Returns the length of the matched text region.
   */
  private final int yylength() {
    return yy_markedPos-yy_startRead;
  }


  /**
   * Reports an error that occured while scanning.
   *
   * In a wellformed scanner (no or only correct usage of
   * yypushback(int) and a match-all fallback rule) this method
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void yy_ScanError(int errorCode) {
    String message;
    try {
      message = YY_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = YY_ERROR_MSG[YY_UNKNOWN_ERROR];
    }

    throw new Error(message);
  }


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  private void yypushback(int number)  {
    if ( number > yylength() )
      yy_ScanError(YY_PUSHBACK_2BIG);

    yy_markedPos -= number;
  }

  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   IOException  if any I/O-Error occurs
   */
  public String getNextToken() throws java.io.IOException {
    int yy_input;
    int yy_action;

    // cached fields:
    int yy_currentPos_l;
    int yy_startRead_l;
    int yy_markedPos_l;
    int yy_endRead_l = yy_endRead;
    char [] yy_buffer_l = yy_buffer;
    char [] yycmap_l = yycmap_instance;

    int [] yytrans_l = yytrans;
    int [] yy_rowMap_l = yy_rowMap;
    byte [] yy_attr_l = YY_ATTRIBUTE;

    while (true) {
      yy_markedPos_l = yy_markedPos;

      yy_action = -1;

      yy_startRead_l = yy_currentPos_l = yy_currentPos =
      yy_startRead = yy_markedPos_l;

      yy_state = yy_lexical_state;


      yy_forAction: {
        while (true) {

          if (yy_currentPos_l < yy_endRead_l)
            yy_input = yy_buffer_l[yy_currentPos_l++];
          else if (yy_atEOF) {
            yy_input = YYEOF;
            break yy_forAction;
          }
          else {
            // store back cached positions
            yy_currentPos  = yy_currentPos_l;
            yy_markedPos   = yy_markedPos_l;
            boolean eof = yy_refill();
            // get translated positions and possibly new buffer
            yy_currentPos_l  = yy_currentPos;
            yy_markedPos_l   = yy_markedPos;
            yy_buffer_l      = yy_buffer;
            yy_endRead_l     = yy_endRead;
            if (eof) {
              yy_input = YYEOF;
              break yy_forAction;
            }
            else {
              yy_input = yy_buffer_l[yy_currentPos_l++];
            }
          }
          int yy_next = yytrans_l[ yy_rowMap_l[yy_state] + yycmap_l[yy_input] ];
          if (yy_next == -1) break yy_forAction;
          yy_state = yy_next;

          int yy_attributes = yy_attr_l[yy_state];
          if ( (yy_attributes & 1) == 1 ) {
            yy_action = yy_state;
            yy_markedPos_l = yy_currentPos_l;
            if ( (yy_attributes & 8) == 8 ) break yy_forAction;
          }

        }
      }

      // store back cached position
      yy_markedPos = yy_markedPos_l;

      switch (yy_action) {

        case 22:
        case 27: {
          if (addLine) {
            lines++;
            addLine = false;
          }
          yybegin(AFTER);
          return(unescape(yytext()));
        }
        case 30: break;
        case 15:
        case 26: {
          yybegin(YYINITIAL);
          return(yytext());
        }
        case 31: break;
        case 8: {
          if (addLine) {
            lines++;
            addLine = false;
          }
          yybegin(BEFORE);
          return("");
        }
        case 32: break;
        case 1:
        case 11: {
        }
        case 33: break;
        case 2:
        case 16:
        case 17: {
        }
        case 34: break;
        case 3:
        case 19: {
        }
        case 35: break;
        case 10: {
          yybegin(AFTER);
          return(yytext());
        }
        case 36: break;
        case 4: {
          if (addLine) {
            lines++;
            addLine = false;
          }
          String text = yytext();
          if (commentDelims.indexOf(text.charAt(0)) == -1){
            yybegin(AFTER);
            return(text);
          } else {
            yybegin(COMMENT);
          }
        }
        case 37: break;
        case 6:
        case 7: {
          addLine = true;
          yybegin(YYINITIAL);
        }
        case 38: break;
        case 25:
        case 28: {
          yybegin(AFTER);
          return(unescape(yytext()));
        }
        case 39: break;
        case 14: {
          yybegin(BEFORE);
          return("");
        }
        case 40: break;
        case 18: {
          yybegin(BEFORE);
        }
        case 41: break;
        case 12:
        case 13: {
          addLine = true;
          yybegin(YYINITIAL);
          return("");
        }
        case 42: break;
        case 9:
        case 23: {
          if (addLine) {
            lines++;
            addLine = false;
          }
          yybegin(YYINITIAL);
          return(yytext());
        }
        case 43: break;
        case 5: {
          if (addLine) {
            lines++;
            addLine = false;
          }
          yybegin(BEFORE);
        }
        case 44: break;
        default:
          if (yy_input == YYEOF && yy_startRead == yy_currentPos) {
            yy_atEOF = true;
            switch (yy_lexical_state) {
              case BEFORE: {
                yybegin(YYINITIAL);
                addLine = true;
                return("");
              }
              case 30: break;
              default:
                return null;
            }
          }
          else {
            yy_ScanError(YY_NO_MATCH);
          }
      }
    }
  }


}