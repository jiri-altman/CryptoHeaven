/*
 * Copyright 2001-2010 by CryptoHeaven Development Team,
 * Mississauga, Ontario, Canada.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Development Team ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Development Team.
 */

package com.CH_gui.msgs;

import com.CH_cl.service.cache.*;
import com.CH_cl.service.cache.event.*;
import com.CH_cl.service.engine.*;
import com.CH_cl.service.ops.*;
import com.CH_cl.service.records.filters.*;

import com.CH_co.queue.*;
import com.CH_co.service.msg.*;
import com.CH_co.service.msg.dataSets.obj.*;
import com.CH_co.service.records.filters.*;
import com.CH_co.service.records.*;
import com.CH_co.util.*;
import com.CH_co.trace.*;

import com.CH_gui.action.*;
import com.CH_gui.dialog.*;
import com.CH_gui.frame.*;
import com.CH_gui.gui.*;
import com.CH_gui.list.*;
import com.CH_gui.menuing.PopupMouseAdapter;
import com.CH_gui.msgTable.*;
import com.CH_gui.service.ops.DownloadUtilsGui;
import com.CH_gui.table.*;
import com.CH_gui.util.*;

import com.CH_guiLib.gui.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.DateFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;

/** 
 * <b>Copyright</b> &copy; 2001-2010
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
 * <b>$Revision: 1.59 $</b>
 * @author  Marcin Kurzawa
 * @version 
 */
public class MsgPreviewPanel extends JPanel implements ActionProducerI, RecordSelectionListener, MsgDataProviderI, VisualsSavable, DisposableObj {

  private Action[] actions;

  public static final int COPY_ACTION = 0;
  public static final int SELECT_ALL_ACTION = 1;

  private int leadingActionId = Actions.LEADING_ACTION_ID_PREVIEW_MESSAGE_PANEL;

  private static final short TO = MsgLinkRecord.RECIPIENT_TYPE_TO;
  private static final short CC = MsgLinkRecord.RECIPIENT_TYPE_CC;
  private static final short BCC = MsgLinkRecord.RECIPIENT_TYPE_BCC;
  private static final short[] RECIPIENT_TYPES = new short[] { TO, CC, BCC };

  public static final int LINK_RELATIVE_FONT_SIZE = -2;
  //private static final Color LINK_BG_FOCUS_COLOR = Color.decode("0x"+MsgDataRecord.WARNING_BACKGROUND_COLOR);
  private static final Color LINK_BG_FOCUS_COLOR = Color.yellow; // more saturated is better because the link is small
  private static final int MIN_DIVIDER_RESTORE = 25;

  private JPanel jLinePriority;
  private JLabel jPriorityLabel;
  private JLabel jPriority;
  private JPanel jLineFrom;
  private JLabel jFromLabel;
  private JLabel jFromName;
  private JMyLinkLikeLabel jFromNameAddContact;
  private JLabel jMsgDate;
  private JPanel jLineReplyTo;
  private JLabel jReplyToLabel;
  private JLabel jReplyTo;
  private JPanel jLineRecipients;
  private boolean lineRecipientsVisibilityAllowed;
  private JPanel jRecipients;
  private JLabel jSubjectLabel;
  private JLabel jSubject;
  private JLabel jLoadingLabel;
  private JMyLinkLikeLabel jNotSpam;
  private JMyLinkLikeLabel jFullView;
  private boolean isFullView = false;
  private int restoredViewLocation;
  private JPanel jLineAttachments;
  private JPanel jAttachments;
  private JPanel jLineExpiration;
  private JLabel jExpirationLabel;
  private JLabel jExpiration;
  private JPanel jLinePassword;
  private JLabel jPasswordHintText;
  private JTextField jPasswordField;
  private JComponent jMessage;
  private JMyLinkLikeLabel jHTML;
  private JButton jAttachment;
  private boolean isAttachmentButton;

  //private JEditorPane jHtmlMessage;
  //private JTextArea jTextMessage;
  private JComponent jHtmlMessage;
  private JComponent jTextMessage;
  private JPanel textPanel;
  private JScrollPane textScrollPane;
  private int textComponentPosY;

  private boolean isHTML;
  private short lastGUIcomponentsMode;
  private String lastGUIpriorityMode;
  private boolean lastGUIexpiryMode;
  private boolean lastGUIpasswordMode;
  private boolean lastGUIhasAttachmentsMode;
  private int lastSplitOrientationMode;

  // currently displayed message
  private MsgLinkRecord msgLinkRecord;
  private MsgDataRecord msgDataRecord;
  private RecordFilter ourMsgFilter;

  private MsgLinkListener linkListener;
  private MsgDataListener dataListener;

  private static final String STR_SELECT_A_SINGLE_MESSAGE = "<html><p align=\"left\">To view a message in this reading window, click on a message in the list.<br>To select more then one, hold Shift or Control key and click the desired messages.</p></html>";
  private static final String STR_SELECT_A_SINGLE_ADDRESS = "<html><p align=\"left\">To view address details in this reading window, click on an address in the list.<br>To select more then one, hold Shift or Control key and click the desired addresses.</p></html>";
  private static final String STR_MSG_BODY_UNAVAILABLE = "<html><p align=\"left\">Message content is currently unavailable.  If this is a shared Inbox, folder owner <br>must access these messages before they will become available to other participants.</p></html>";
  private boolean isWaitingForMsgBody;
  private String no_selected_msg_html;

  private QueueMM1 msgPreviewUpdateQueue;
  private Fifo msgPreviewUpdateFifo;

  private Vector componentsForPopupV = new Vector();

  static {
    HTML_ClickablePane.setRegisteredGlobalLauncher(HTML_ClickablePane.PROTOCOL_MAIL, new URLLauncherMAILTO());
  }


  /** Creates new MsgPreviewPanel */
  public MsgPreviewPanel(short objType) {
    this(objType, objType == MsgDataRecord.OBJ_TYPE_ADDR ? STR_SELECT_A_SINGLE_ADDRESS : STR_SELECT_A_SINGLE_MESSAGE);
  }

  /** Creates new MsgPreviewPanel */
  public MsgPreviewPanel(short objType, String no_selected_msg_html) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgPreviewPanel.class, "MsgPreviewPanel(short objType, String no_selected_msg_html)");
    if (trace != null) trace.args(objType);
    if (trace != null) trace.args(no_selected_msg_html);

    this.no_selected_msg_html = no_selected_msg_html;
    isHTML = true;

    initActions();
    initPanel();
    setGUIComponentsForObj(objType, null, false, false, false);
    restoreVisuals(GlobalProperties.getProperty(MiscGui.getVisualsKeyName(this)));

    // Listed on changes to the link/data so we can dynamically update the preview pane.
    linkListener = new MsgLinkListener();
    FetchedDataCache.getSingleInstance().addMsgLinkRecordListener(linkListener);
    dataListener = new MsgDataListener();
    FetchedDataCache.getSingleInstance().addMsgDataRecordListener(dataListener);

    addPopup(jTextMessage);
    addPopup(jHtmlMessage);

    if (trace != null) trace.exit(MsgPreviewPanel.class);
  }

  private void addPopup(Component c) {
    if (c != null && !componentsForPopupV.contains(c)) {
      c.addMouseListener(new PopupMouseAdapter(c, this));
      componentsForPopupV.addElement(c);
    }
  }

  private void initActions() {
    actions = new Action[2];

    actions[COPY_ACTION] = new CopyAction(leadingActionId+COPY_ACTION);
    actions[SELECT_ALL_ACTION] = new SelectAllAction(leadingActionId+SELECT_ALL_ACTION);

    setEnabledActions();
  }

  /**
   * MsgDataProviderI Interface method
   * @return
   */
  public MsgDataRecord provideMsgData() {
    return msgDataRecord;
  }


  /** 
   * Copy
   */
  private class CopyAction extends DefaultEditorKit.CopyAction {
    public CopyAction(int actionId) {
      putValue(Actions.NAME, com.CH_gui.lang.Lang.rb.getString("action_Copy"));
      putValue(Actions.MENU_ICON, Images.get(ImageNums.COPY16));
      putValue(Actions.ACTION_ID, new Integer(actionId));
      putValue(Actions.IN_MENU, Boolean.FALSE);
      putValue(Actions.IN_TOOLBAR, Boolean.FALSE);
    }
    public void actionPerformed(ActionEvent event) {
      super.actionPerformed(event);
    }
  }

  /** 
   * Select All
   */
  private class SelectAllAction extends AbstractActionTraced {
    public SelectAllAction(int actionId) {
      putValue(Actions.NAME, com.CH_gui.lang.Lang.rb.getString("action_Select_All"));
      putValue(Actions.MENU_ICON, Images.get(ImageNums.SELECT_ALL16));
      putValue(Actions.ACTION_ID, new Integer(actionId));
      putValue(Actions.IN_MENU, Boolean.FALSE);
      putValue(Actions.IN_TOOLBAR, Boolean.FALSE);
    }
    public void actionPerformedTraced(ActionEvent event) {
      jMessage.grabFocus();
      if (jMessage instanceof JTextComponent) {
        ((JTextComponent) jMessage).selectAll();
      } else {
        // TO-DO: cobra action...
      }
    }
  }


  private void initPanel() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgPreviewPanel.class, "initPanel()");

    // So the split panes are not limited in movement, but must have at least visible header.
    setMinimumSize(new Dimension(0, 24));
    setLayout(new GridBagLayout());
    setBorder(new EmptyBorder(0,0,0,0));

    jPriority = new JMyLabel();
    jPriority.setIconTextGap(2);
    jFromName = new JMyLabel();
    jFromName.setIconTextGap(2);
    {
      jFromNameAddContact = new JMyLinkLikeLabel("+Add to Address Book", LINK_RELATIVE_FONT_SIZE);
      jFromNameAddContact.setVisible(false);
    }
    jFromNameAddContact.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        e.consume();
        String[] contactInfo = (String[]) jFromNameAddContact.getDescription();
        if (contactInfo != null) {
          String nick = contactInfo[0];
          String email = contactInfo[1];
          Vector emailNicksV = new Vector();
          Vector emailStringRecordsV = new Vector();
          emailNicksV.addElement(nick);
          emailStringRecordsV.addElement(email);
          MsgComposePanel.checkEmailAddressesForAddressBookAdition_Threaded(MsgPreviewPanel.this, emailNicksV, emailStringRecordsV, true, new FolderFilter(FolderRecord.ADDRESS_FOLDER));
        }
      }
    });
    //jMsgDate = new JMyLinkLabel("", null);
    jMsgDate = new JMyLabel();
    Font labelFont = jMsgDate.getFont();
    jMsgDate.setFont(labelFont.deriveFont((float) (labelFont.getSize()-2)));

    jReplyTo = new JMyLabel();
    jReplyTo.setIconTextGap(2);

    jRecipients = new JPanel();
    jRecipients.setBorder(new EmptyBorder(0, 0, 0, 0));

    jAttachments = new JPanel();
    jAttachments.setBorder(new EmptyBorder(0, 0, 0, 0));

    if (isHTML) {
      jHTML = new JMyLinkLikeLabel("Plain Text", LINK_RELATIVE_FONT_SIZE);
    } else {
      jHTML = new JMyLinkLikeLabel("Rich Text", LINK_RELATIVE_FONT_SIZE);
    }
    jHTML.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        e.consume();
        pressedHTML();
      }
    });

    jAttachment = new JMyButtonNoFocus(Images.get(ImageNums.DETACH16));
    jAttachment.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    jAttachment.setBorder(new EmptyBorder(1, 1, 1, 1));
    jAttachment.setVisible(false);
    jAttachment.setToolTipText(com.CH_gui.lang.Lang.rb.getString("actionTip_Show_attachments..."));
    jAttachment.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        pressedAttachment();
      }
    });

    JButton jPrint = new JMyButtonNoFocus(Images.get(ImageNums.PRINT16));
    jPrint.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    jPrint.setBorder(new EmptyBorder(1, 1, 1, 1));
    jPrint.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        if (msgLinkRecord != null && msgDataRecord != null) {
          boolean isForceSimpleHTML = msgDataRecord.isHtmlMail() && !isHTML;
          Thread th = new ThreadTraced(new PrintRunnable(msgLinkRecord, msgDataRecord, isForceSimpleHTML, MsgPreviewPanel.this), "Print Runner");
          th.setDaemon(true);
          th.start();
        }
      }
    });

    jSubject = new JMyLabel();
    jLoadingLabel = new JMyLabel("Loading...");
    jLoadingLabel.setVisible(false);
    jExpiration = new JMyLabel();
    jPasswordField = new JMyTextField(10);
    jPasswordField.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        if (msgDataRecord != null) {
          Hasher.Set matchingSet = MsgPanelUtils.getMatchingPasswordHasher(msgDataRecord, jPasswordField.getText());
          if (matchingSet != null) {
            MsgPanelUtils.unlockPassProtectedMsg(msgDataRecord, matchingSet);
            // change our own display, but other listeners should be notified by the unlock code
            setCurrentMessageText();
            // refetch the attachments, maybe the subjects and filenames will show now
            setAttachmentsPanel(msgLinkRecord, msgDataRecord, jAttachments, jLineAttachments);
          }
        }
      }
    });

    jNotSpam = new JMyLinkLikeLabel("Not Spam", LINK_RELATIVE_FONT_SIZE);
    jNotSpam.setVisible(false);
    jNotSpam.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        e.consume();
        // get info before move because it might change after move...
        final String[] contactInfo = (String[]) jNotSpam.getDescription();
        jNotSpam.setVisible(false);
        jNotSpam.setDescription(null);
        // Move message to Inbox
        FetchedDataCache cache = FetchedDataCache.getSingleInstance();
        FolderPair inbox = CacheUtilities.convertRecordToPair(cache.getFolderRecord(cache.getUserRecord().msgFolderId));
        MsgActionTable.doMoveOrCopyOrSaveAttachmentsAction(true, inbox, new MsgLinkRecord[] { msgLinkRecord });
        // Add sender email address at once to Whitelist (free up AWT thread)
        Thread th = new ThreadTraced("Not Spam Mover") {
          public void runTraced() {
            FolderPair whiteList = FolderOps.getOrCreateWhiteList(MainFrame.getServerInterfaceLayer());
            if (contactInfo != null) {
              String nick = contactInfo[0];
              String email = contactInfo[1];
              Vector emailNicksV = new Vector();
              Vector emailStringRecordsV = new Vector();
              emailNicksV.addElement(nick);
              emailStringRecordsV.addElement(email);
              MsgComposePanel.checkEmailAddressesForAddressBookAdition_Threaded(MsgPreviewPanel.this, emailNicksV, emailStringRecordsV, false, new FolderFilter(FolderRecord.WHITELIST_FOLDER), true, whiteList, true);
            }
          }
        };
        th.setDaemon(true);
        th.start();
      }
    });

    jFullView = new JMyLinkLikeLabel("Full view", LINK_RELATIVE_FONT_SIZE);
    jFullView.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        e.consume();
        toggleExpandRestore();
      }
    });
    jFullView.addHierarchyListener(new InitialRunner(new Runnable() {
      public void run() {
        JSplitPane splitPane = MiscGui.getParentSplitPane(MsgPreviewPanel.this);
        jFullView.setVisible(splitPane != null);
        if (splitPane != null) {
          int dividerLocation = splitPane.getDividerLocation();
          if (dividerLocation >= 0 && dividerLocation <= MIN_DIVIDER_RESTORE) {
            isFullView = true;
            toggleExpandRestore();
          }
        }
      }
    }));

    this.jTextMessage = makeTextPane(false);
    this.jHtmlMessage = makeTextPane(true);

    if (isHTML) {
      jMessage = jHtmlMessage;
    } else {
      jMessage = jTextMessage;
    }

    jLinePriority = new JPanel(new GridBagLayout());
    jLineFrom = new JPanel(new GridBagLayout());
    jLineReplyTo = new JPanel(new GridBagLayout());
    jLineRecipients = new JPanel(new GridBagLayout());
    JPanel jLineSubject = new JPanel(new GridBagLayout());
    jLineAttachments = new JPanel(new GridBagLayout());
    jLineExpiration = new JPanel(new GridBagLayout());
    jLinePassword = new JPanel(new GridBagLayout());

    jLinePriority.setBorder(new EmptyBorder(0,0,0,0));
    jLineFrom.setBorder(new EmptyBorder(0,0,0,0));
    jLineReplyTo.setBorder(new EmptyBorder(0,0,0,0));
    jLineRecipients.setBorder(new EmptyBorder(0,0,0,0));
    jLineSubject.setBorder(new EmptyBorder(0,0,0,0));
    jLineAttachments.setBorder(new EmptyBorder(0,0,0,0));
    jLineExpiration.setBorder(new EmptyBorder(0,0,0,0));
    jLinePassword.setBorder(new EmptyBorder(0,0,0,0));

    int posY = 0;

    add(jLineSubject, new GridBagConstraints(0, posY, 1, 1, 10, 0,
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(0, 0, 0, 0), 0, 0));
    posY ++;

    add(jLinePriority, new GridBagConstraints(0, posY, 1, 1, 10, 0,
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(0, 0, 0, 0), 0, 0));
    posY ++;

    add(jLineFrom, new GridBagConstraints(0, posY, 1, 1, 10, 0, 
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(0, 0, 0, 0), 0, 0));
    posY ++;

    add(jLineReplyTo, new GridBagConstraints(0, posY, 1, 1, 10, 0, 
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(0, 0, 0, 0), 0, 0));
    posY ++;

    add(jLineRecipients, new GridBagConstraints(0, posY, 1, 1, 10, 0, 
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(0, 0, 0, 0), 0, 0));
    posY ++;

//    add(jLineSubject, new GridBagConstraints(0, posY, 1, 1, 10, 0,
//          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(0, 0, 0, 0), 0, 0));
//    posY ++;

    add(jLineAttachments, new GridBagConstraints(0, posY, 1, 1, 10, 0, 
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(0, 0, 0, 0), 0, 0));
    posY ++;

    add(jLineExpiration, new GridBagConstraints(0, posY, 1, 1, 10, 0, 
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(0, 0, 0, 0), 0, 0));
    posY ++;

    add(jLinePassword, new GridBagConstraints(0, posY, 1, 1, 10, 0, 
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(0, 0, 0, 0), 0, 0));
    posY ++;

    textScrollPane = new JScrollPane(jMessage);
    textScrollPane.setBorder(new EmptyBorder(0,0,0,0));
    textComponentPosY = posY;
    textPanel = new JPanel(new BorderLayout(0,0));
    if (jMessage instanceof JTextComponent) {
      textPanel.add(textScrollPane, BorderLayout.CENTER);
    } else {
      textPanel.add(jMessage, BorderLayout.CENTER);
    }
    add(textPanel, new GridBagConstraints(0, textComponentPosY, 4, 1, 10, 10,
          GridBagConstraints.WEST, GridBagConstraints.BOTH, new MyInsets(2, 1, 1, 1), 0, 0));


    JLabel jMinHeight1 = new JMyLabel(" ");
    jLinePriority.add(jMinHeight1, new GridBagConstraints(0, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(0, 0, 0, 0), 0, 0));
    jPriorityLabel = new JMyLabel(com.CH_gui.lang.Lang.rb.getString("label_Priority"));
    jLinePriority.add(jPriorityLabel, new GridBagConstraints(1, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
    jLinePriority.add(jPriority, new GridBagConstraints(2, 0, 1, 1, 10, 0, 
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(1, 3, 1, 3), 0, 0));


    JLabel jMinHeight2 = new JMyLabel(" ");
    jLineFrom.add(jMinHeight2, new GridBagConstraints(0, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(0, 0, 0, 0), 0, 0));
    jFromLabel = new JMyLabel(com.CH_gui.lang.Lang.rb.getString("label_From"));
    jLineFrom.add(jFromLabel, new GridBagConstraints(1, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
    jLineFrom.add(jFromName, new GridBagConstraints(2, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
    jLineFrom.add(jFromNameAddContact, new GridBagConstraints(3, 0, 1, 1, 1, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
    jLineFrom.add(new JLabel(), new GridBagConstraints(4, 0, 1, 1, 10, 0, 
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(0, 0, 0, 0), 0, 0));
//    jLineFrom.add(jNotSpam, new GridBagConstraints(5, 0, 1, 1, 0, 0,
//          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
//    jLineFrom.add(jFullView, new GridBagConstraints(6, 0, 1, 1, 0, 0,
//          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
//    jLineFrom.add(jHTML, new GridBagConstraints(7, 0, 1, 1, 0, 0,
//          GridBagConstraints.EAST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
//    jLineFrom.add(jAttachment, new GridBagConstraints(8, 0, 1, 1, 0, 0,
//          GridBagConstraints.EAST, GridBagConstraints.NONE, new MyInsets(1, 1, 1, 1), 0, 0));
//    jLineFrom.add(jPrint, new GridBagConstraints(9, 0, 1, 1, 0, 0,
//          GridBagConstraints.EAST, GridBagConstraints.NONE, new MyInsets(1, 1, 1, 1), 0, 0));
    jLineFrom.add(jMsgDate, new GridBagConstraints(5, 0, 1, 1, 0, 0,
          GridBagConstraints.EAST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));

    JLabel jMinHeight3 = new JMyLabel(" ");
    jLineReplyTo.add(jMinHeight3, new GridBagConstraints(0, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(0, 0, 0, 0), 0, 0));
    jReplyToLabel = new JMyLabel(com.CH_gui.lang.Lang.rb.getString("label_Reply_To"));
    jLineReplyTo.add(jReplyToLabel, new GridBagConstraints(1, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
    jLineReplyTo.add(jReplyTo, new GridBagConstraints(2, 0, 1, 1, 10, 0, 
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(1, 3, 1, 3), 0, 0));

    JLabel jMinHeight4 = new JMyLabel(" ");
    jLineRecipients.add(jMinHeight4, new GridBagConstraints(0, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(0, 0, 0, 0), 0, 0));
    jLineRecipients.add(jRecipients, new GridBagConstraints(1, 0, 1, 1, 10, 0, 
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(1, 3, 1, 3), 0, 0));
//    jLineRecipients.add(jFullView, new GridBagConstraints(2, 0, 1, 1, 0, 0, 
//          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
//    jLineRecipients.add(new JLabel(), new GridBagConstraints(2, 0, 1, 1, 10, 0, 
//          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(0, 0, 0, 0), 0, 0));


    JLabel jMinHeight5 = new JMyLabel(" ");
    jLineSubject.add(jMinHeight5, new GridBagConstraints(0, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(0, 0, 0, 0), 0, 0));
    jSubjectLabel = new JMyLabel(com.CH_gui.lang.Lang.rb.getString("label_Subject"));
//    jLineSubject.add(jSubjectLabel, new GridBagConstraints(1, 0, 1, 1, 0, 0,
//          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
    jSubject.setFont(jSubject.getFont().deriveFont(Font.BOLD));
    jLineSubject.add(jSubject, new GridBagConstraints(2, 0, 1, 1, 10, 0,
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
    // when "Loading" label is visible we want it to fill max space and be left justified
    jLineSubject.add(jLoadingLabel, new GridBagConstraints(3, 0, 1, 1, 1000000, 0,
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
//    jLineSubject.add(jMsgDate, new GridBagConstraints(3, 0, 1, 1, 0, 0,
//          GridBagConstraints.EAST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
    jLineSubject.add(jNotSpam, new GridBagConstraints(5, 0, 1, 1, 0, 0,
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
    jLineSubject.add(jFullView, new GridBagConstraints(6, 0, 1, 1, 0, 0,
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
    jLineSubject.add(jHTML, new GridBagConstraints(7, 0, 1, 1, 0, 0,
          GridBagConstraints.EAST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
    jLineSubject.add(jAttachment, new GridBagConstraints(8, 0, 1, 1, 0, 0,
          GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new MyInsets(0, 0, 0, 0), 0, 0));
    jLineSubject.add(jPrint, new GridBagConstraints(9, 0, 1, 1, 0, 0,
          GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new MyInsets(0, 0, 0, 0), 0, 0));


    JLabel jMinHeight_att = new JMyLabel(" ");
    jLineAttachments.add(jMinHeight_att, new GridBagConstraints(0, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(0, 0, 0, 0), 0, 0));
//    JLabel jAttachmentsLabel = new JMyLabel(com.CH_gui.lang.Lang.rb.getString("label_Attachments"));
//    jLineAttachments.add(jAttachmentsLabel, new GridBagConstraints(1, 0, 1, 1, 0, 0,
//          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
    jLineAttachments.add(jAttachments, new GridBagConstraints(2, 0, 1, 1, 10, 0,
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(1, 3, 1, 3), 0, 0));


    JLabel jMinHeight6 = new JMyLabel(" ");
    jLineExpiration.add(jMinHeight6, new GridBagConstraints(0, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(0, 0, 0, 0), 0, 0));
    jExpirationLabel = new JMyLabel(com.CH_gui.lang.Lang.rb.getString("label_Expiry_Date"));
    jExpirationLabel.setFont(jExpirationLabel.getFont().deriveFont(Font.BOLD));
    jLineExpiration.add(jExpirationLabel, new GridBagConstraints(1, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
    jLineExpiration.add(jExpiration, new GridBagConstraints(2, 0, 1, 1, 10, 0, 
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(1, 3, 1, 3), 0, 0));

    JLabel jMinHeight7 = new JMyLabel(" ");
    jLinePassword.add(jMinHeight7, new GridBagConstraints(0, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(0, 0, 0, 0), 0, 0));
    JLabel jPasswordHintLabel = new JMyLabel("Question:");
    jPasswordHintLabel.setFont(jPasswordHintLabel.getFont().deriveFont(Font.BOLD));
    jPasswordHintText = new JMyLabel("None");
    jPasswordHintText.setFont(jPasswordHintText.getFont().deriveFont(Font.BOLD));
    JLabel jPasswordAnswerLabel = new JMyLabel("Answer:");
    jPasswordAnswerLabel.setFont(jPasswordAnswerLabel.getFont().deriveFont(Font.BOLD));
    jLinePassword.add(jPasswordHintLabel, new GridBagConstraints(1, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
    jLinePassword.add(jPasswordHintText, new GridBagConstraints(2, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 10), 0, 0));
    jLinePassword.add(jPasswordAnswerLabel, new GridBagConstraints(3, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
    jLinePassword.add(jPasswordField, new GridBagConstraints(4, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(1, 3, 1, 3), 0, 0));
    jLinePassword.add(new JLabel(), new GridBagConstraints(5, 0, 1, 1, 10, 0, 
          GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(0, 0, 0, 0), 0, 0));

    JLabel sampleLabel = new JMyLabel("abcdefghijkl");
    Dimension minHeight = sampleLabel.getPreferredSize();
    minHeight = new Dimension(1, minHeight.height);
    jMinHeight1.setSize(minHeight);
    jMinHeight2.setSize(minHeight);
    jMinHeight3.setSize(minHeight);
    jMinHeight4.setSize(minHeight);
    jMinHeight5.setSize(minHeight);
    jMinHeight6.setSize(minHeight);
    jMinHeight7.setSize(minHeight);

    if (trace != null) trace.exit(MsgPreviewPanel.class);
  }

  private JComponent makeTextPane(boolean forHTML) {
    JComponent textComp = null;
    HTML_ClickablePane jHtmlMsg = null;
    JTextArea jTextMsg = null;
    if (forHTML) {
      jHtmlMsg = new HTML_ClickablePane(no_selected_msg_html);
      jHtmlMsg.setEditable(false);
    } else {
      jTextMsg = new JMyTextArea();
      jTextMsg.setWrapStyleWord(true);
      jTextMsg.setLineWrap(true);
      jTextMsg.setEditable(false);
      jTextMsg.setMargin(UIManager.getInsets("EditorPane.margin")); // matching to the corresponding HTML component

      Font font = UIManager.getFont("Label.font");
      font = font.deriveFont(Font.PLAIN, 14.0f);
      jTextMsg.setFont(font);
    }
    if (forHTML) {
      textComp = jHtmlMsg;
    } else {
      textComp = jTextMsg;
    }
    return textComp;
  }

  private void toggleExpandRestore() {
    // file split pane
    JSplitPane splitPane = MiscGui.getParentSplitPane(this);
    if (splitPane != null) {
      if (isFullView) {
        // restore
        int lastLocation = splitPane.getLastDividerLocation();
        if (lastLocation > MIN_DIVIDER_RESTORE && lastLocation == restoredViewLocation) { // starting up fresh and fully expanded gives lastLocation=1 not 0!
          splitPane.setDividerLocation(lastLocation);
        } else {
          splitPane.setDividerLocation(0.3);
        }
      } else {
        // expand
        restoredViewLocation = splitPane.getDividerLocation();
        splitPane.setDividerLocation(0);
      }
      isFullView = !isFullView;
    }
    updateExpandRestoreLabel();
  }

  private void updateExpandRestoreLabel() {
//    URL url = null;
//    try {
//      url = new URL("http", "localhost", 80, LINK_INTERNAL+"toggleExpandRestore()");
//    } catch (Throwable t) {
//    }
    if (isFullView) {
      jFullView.setText("Restore view");
    } else {
      jFullView.setText("Full view");
    }
  }

  private void pressedHTML() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgPreviewPanel.class, "pressedHTML()");

    boolean newHTMLstate = !isHTML;
    if (trace != null) trace.data(10, "newHTMLstate", newHTMLstate);

    // Chan the display mode now... and send request for change on the server.
    // When fetching mail folders it may take to long for view to respond, so switch right away.
    setHTMLMode(newHTMLstate);

    // Change the message status on the server, when it comes back, our gui will adjust
    // Run update to the server and so we don't hold up the GUI, update the local cached copy right away.  This is done to make the GUI very responsive
    if (msgLinkRecord != null && msgDataRecord != null) {
      msgLinkRecord.status = (Short) Misc.setBitObj(newHTMLstate == msgDataRecord.isHtmlMail(), msgLinkRecord.status, MsgLinkRecord.STATUS_FLAG__APPROVED_FOR_NATIVE_PREVIEW_MODE);
      // Make the setting persistant
      MainFrame.getServerInterfaceLayer().submitAndReturn(new MessageAction(CommandCodes.MSG_Q_UPDATE_STATUS, new Obj_List_Co(new Object[] { msgLinkRecord.msgLinkId, msgLinkRecord.status })), 30000, 3);
    }

    // make sure we update the GUI right away
    try {
      revalidate();
    } catch (Throwable t) {
    } try {
      doLayout();
    } catch (Throwable t) {
    } 
    if (trace != null) trace.exit(MsgPreviewPanel.class);
  }
  private void setHTMLMode(boolean isHtml) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgPreviewPanel.class, "setHTMLMode(boolean isHtml)");
    if (trace != null) trace.args(isHtml);

    boolean isChange = isHTML != isHtml;
    if (isChange) {
      isHTML = isHtml;
      try {
        updateHTMLModeIcon();
        if (isHTML) {
          jMessage = jHtmlMessage;
        } else {
          jMessage = jTextMessage;
        }
        if (jMessage instanceof JTextComponent) {
          ((JTextComponent) jMessage).setText("");
//        } else if (jMessage instanceof HtmlPanel) {
//          ((HtmlPanel) jMessage).clearDocument();
        }
        switchMessageArea(jMessage);
        setCurrentMessageText();
      } catch (Throwable t) {
        if (trace != null) trace.exception(MsgPreviewPanel.class, 100, t);
      }
      if (jMessage instanceof JTextComponent) {
        ((JTextComponent) jMessage).setCaretPosition(0);
      } else {
        // TO-DO: cobra component
      }
    }

    if (trace != null) trace.exit(MsgPreviewPanel.class);
  }

  private void updateHTMLModeIcon() {
    boolean isMsgMode = false;
    if (msgDataRecord != null)
      isMsgMode = msgDataRecord.objType.shortValue() == MsgDataRecord.OBJ_TYPE_MSG;
    if (!isMsgMode || msgDataRecord == null) {
      jHTML.setVisible(false);
    } else {
      if (isHTML) {
        if (!msgDataRecord.isHtmlMail()) {
          jHTML.setText("Plain Text", LINK_BG_FOCUS_COLOR);
          if (!jHTML.isVisible()) jHTML.setVisible(true);
        } else {
          jHTML.setText("Plain Text");
          boolean isPLAINpreferred = isDefaultToPLAINpreferred(msgDataRecord);
          if (jHTML.isVisible() != isPLAINpreferred) jHTML.setVisible(isPLAINpreferred);
        }
      } else {
        if (msgDataRecord.isHtmlMail()) {
          jHTML.setText("Rich Text", LINK_BG_FOCUS_COLOR);
          if (!jHTML.isVisible()) jHTML.setVisible(true);
        } else {
          jHTML.setText("Rich Text");
          if (jHTML.isVisible()) jHTML.setVisible(false);
        }
      }
    }
  }

  private void switchMessageArea(Component newViewComponent) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgPreviewPanel.class, "switchMessageArea(Component newViewComponent)");
    if (trace != null) trace.args(newViewComponent);
    boolean toValidate = false;
    if (newViewComponent instanceof JTextComponent) {
      if (textScrollPane.getViewport().getView() != newViewComponent) {
        textScrollPane.setViewportView(newViewComponent);
        toValidate = true;
      }
      if (textPanel.getComponentCount() != 1 || textPanel.getComponent(0) != textScrollPane) {
        textPanel.removeAll();
        textPanel.add(textScrollPane, BorderLayout.CENTER);
        toValidate = true;
      }
    } else {
      if (textPanel.getComponentCount() != 1 || textPanel.getComponent(0) != newViewComponent) {
        textPanel.removeAll();
        textPanel.add(newViewComponent, BorderLayout.CENTER);
        toValidate = true;
      }
    }
    if (toValidate) {
      textPanel.validate();
    }
    if (trace != null) trace.exit(MsgPreviewPanel.class);
  }


  private void pressedAttachment() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgPreviewPanel.class, "pressedAttachment()");
    if (msgLinkRecord != null) {
      new AttachmentFetcherPopup(jAttachment, msgLinkRecord).start();
    }
    if (trace != null) trace.exit(MsgPreviewPanel.class);
  }


  private void setRecipientsPanel(final MsgDataRecord dataRecord, final JPanel jRecipients, final JPanel jParentLineRecipients) {
    // Since there is a problem when a frame shows up with this panel, 
    // do this at the end of all AWT events after the frame is already shown.
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(getClass(), "setRecipientsPanel.run()");

        // Don't do a flowing-resizable panel, just put a label style boxes
        //MsgPanelUtils.drawMsgRecipientsPanel(dataRecord, jRecipients, maxSize);
        Record[][] recipientsAll = MsgPanelUtils.gatherAllMsgRecipients(dataRecord);
        jRecipients.removeAll();
        Record[][] objSets = recipientsAll;
        Record[] recipients = null;
        jRecipients.setBorder(new EmptyBorder(0, 0, 0, 0));

        Vector labelsV = new Vector();

        for (int setIndex=0; objSets!=null && setIndex<objSets.length; setIndex++) {
          Record[] objs = objSets[setIndex];
          recipients = objs;
          if (objs != null && objs.length > 0) {
            String setHeader = "";
            if (setIndex == TO)
              setHeader = com.CH_gui.lang.Lang.rb.getString("label_To");
            else if (setIndex == CC)
              setHeader = com.CH_gui.lang.Lang.rb.getString("label_Cc");
            else
              setHeader = com.CH_gui.lang.Lang.rb.getString("label_Bcc");
            JLabel headerLabel = new JMyLabel("<html>"+setHeader+"</html>");
            if (setIndex == 0) {
              headerLabel.setBorder(new EmptyBorder(0,0,0,3));
            } else {
              headerLabel.setBorder(new EmptyBorder(0,5,0,3));
            }
            labelsV.addElement(headerLabel);

            if (recipients != null) {
              for (int i=0; i<recipients.length; i++) {
                JLabel label = new JMyLabel();
                label.setText(ListRenderer.getRenderedText(recipients[i]));
                label.setIcon(ListRenderer.getRenderedIcon(recipients[i]));
                label.setIconTextGap(2);
                labelsV.addElement(label);
              }
            }
          }
        }

        // setup the panel with created components
        JPanel panel = jRecipients;
        panel.setLayout(new GridBagLayout());
        int posX = 0;
        for (posX=0; posX<labelsV.size(); posX++) {
          JLabel label = (JLabel) labelsV.elementAt(posX);
          panel.add(label, new GridBagConstraints(posX, 0, 1, 1, 0, 0, 
            GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new MyInsets(0, 0, 0, 5), 0, 0));
        }
        if (lineRecipientsVisibilityAllowed && jParentLineRecipients != null) {
          jParentLineRecipients.setVisible(labelsV.size() > 0);
        }
        // add horizontal filler
        panel.add(new JLabel(), new GridBagConstraints(posX, 0, 1, 1, 10, 0, 
          GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new MyInsets(0, 0, 0, 0), 0, 0));
        posX ++;

        jRecipients.revalidate();
        jRecipients.doLayout();

        // Runnable, not a custom Thread -- DO NOT clear the trace stack as it is run by the AWT-EventQueue Thread.
        if (trace != null) trace.exit(getClass());
      }
    });
  }


  private void setAttachmentsButton() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgPreviewPanel.class, "setAttachmentsButton()");
    if (msgDataRecord != null && msgDataRecord.attachedFiles != null && msgDataRecord.attachedMsgs != null && 
        msgDataRecord.attachedFiles.shortValue() + msgDataRecord.attachedMsgs.shortValue() > 0) {
      if (!isAttachmentButton) {
        jAttachment.setVisible(true);
        isAttachmentButton = true;
      }
    } else {
      if (isAttachmentButton) {
        jAttachment.setVisible(false);
        isAttachmentButton = false;
      }
    }
    if (trace != null) trace.exit(MsgPreviewPanel.class);
  }


  private void setAttachmentsPanel(final MsgLinkRecord _msgLink, final MsgDataRecord _dataRecord, final JPanel _jAttachments, final JPanel _jLineAttachments) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgPreviewPanel.class, "setAttachmentsPanel(final MsgLinkRecord _msgLink, final MsgDataRecord _dataRecord, final JPanel _jAttachments, final JPanel _jLineAttachments)");
    if (trace != null) trace.args(_msgLink, _dataRecord);
    Thread fetcher = new ThreadTraced("Attachments Preview Fetcher") {
      public void runTraced() {
        Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(getClass(), "MsgPreviewPanel.setAttachmentsPanel.fetcher.runTraced()");

        Record[] attachments = null;
        int numOfAttachments = _dataRecord != null && _dataRecord.attachedFiles != null && _dataRecord.attachedMsgs != null ? _dataRecord.attachedFiles.shortValue() + _dataRecord.attachedMsgs.shortValue() : 0;
        // if regular email, don't show serialized email as attachment ...
        if (_dataRecord != null && _dataRecord.isEmail()) numOfAttachments --;
        JLabel fetchingLabel = null;
        if (numOfAttachments > 0) {
          fetchingLabel = new JMyLabel("...");
          fetchingLabel.setIcon(Images.get(ImageNums.FILE_TYPE_OTHER));
        }
        try {
          final int _numOfAttachments = numOfAttachments;
          final JLabel _fetchingLabel = fetchingLabel;
          javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
              Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(getClass(), "MsgPreviewPanel.setAttachmentsPanel1.run()");
              String subject = _dataRecord != null ? _dataRecord.getSubject() : null;
              setAttachmentsPanel_updateGUI(null, _numOfAttachments > 0 ? new Object[] { _fetchingLabel } : null, subject, _jAttachments, _jLineAttachments);
              if (trace != null) trace.exit(getClass());
            }
          });
        } catch (Throwable t) {
          if (trace != null) trace.exception(getClass(), 100, t);
        }
        if (_msgLink != null  && numOfAttachments > 0) {
          attachments = AttachmentFetcherPopup.fetchAttachments(new MsgLinkRecord[] { _msgLink });
          try {
            final Record[] _attachments = attachments;
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
              public void run() {
                Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(getClass(), "MsgPreviewPanel.setAttachmentsPanel2.run()");
                // draw attachments in GUI only if preview is still pointing to the same message
                if (msgLinkRecord == _msgLink && msgDataRecord == _dataRecord) {
                  String subject = _dataRecord != null ? _dataRecord.getSubject() : null;
                  setAttachmentsPanel_updateGUI(_msgLink, _attachments, subject, _jAttachments, _jLineAttachments);
                } // end if still the same message in preview
                if (trace != null) trace.exit(getClass());
              }
            });
          } catch (Throwable t) {
            if (trace != null) trace.exception(getClass(), 200, t);
          }
        }

        if (trace != null) trace.exit(getClass());
      }
    };
    fetcher.setDaemon(true);
    fetcher.start();
    if (trace != null) trace.exit(MsgPreviewPanel.class);
  }


  private void setAttachmentsPanel_updateGUI(MsgLinkRecord parentMsgLink, Object[] attachments, String skipFileNameForSubject, JPanel jAttachments, JPanel jLineAttachments) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgPreviewPanel.class, "setAttachmentsPanel_updateGUI(MsgLinkRecord parentMsgLink, Object[] attachments, String skipFileNameForSubject, JPanel jAttachments, JPanel jLineAttachments)");
    if (trace != null) trace.args(parentMsgLink, attachments, skipFileNameForSubject);
    boolean visible = false;
    String skipFileName = skipFileNameForSubject != null ? FileTypes.getFileSafeShortString(skipFileNameForSubject) : null;
    jAttachments.removeAll();
    if (attachments != null && attachments.length > 0) {
      jAttachments.setBorder(new EmptyBorder(0, 0, 0, 0));

      Vector labelsV = new Vector();

      for (int i=0; i<attachments.length; i++) {
        boolean skip = false;
        String name = ListRenderer.getRenderedText(attachments[i]);
        if (skipFileName != null && attachments[i] instanceof FileLinkRecord && name.endsWith(".eml")) {
          String fileName = name.substring(0, name.length()-".eml".length());
          if (skipFileName.startsWith(fileName) && (skipFileName.length() == fileName.length() || fileName.length() > 25)) // at least first 25 chars have to match
            skip = true;
        }
        if (!skip) {
          JMyLinkLikeLabel label = new JMyLinkLikeLabel(name, LINK_RELATIVE_FONT_SIZE);
          label.setDescription(new Object[] { parentMsgLink, attachments[i] });
          label.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
              e.consume();
              JMyLinkLikeLabel source = (JMyLinkLikeLabel) e.getSource();
              Object[] objs = (Object[]) source.getDescription();
              MsgLinkRecord parentMsgLink = (MsgLinkRecord) objs[0];
              Object att = objs[1];
              if (att instanceof MsgLinkRecord) {
                MsgLinkRecord msgLink = (MsgLinkRecord) att;
                MsgLinkRecord[] parents = FetchedDataCache.getSingleInstance().getMsgLinkRecordsForMsg(msgLink.msgId);
                if (parents != null && parents.length > 0)
                  new MsgPreviewFrame(parents[0], new MsgLinkRecord[] { msgLink });
              } else if (att instanceof FileLinkRecord) {
                final FileLinkRecord _fileLink = (FileLinkRecord) att;
                final MsgLinkRecord _parentMsgLink = parentMsgLink;
                if (false && FileLauncher.isAudioWaveFilename(_fileLink.getFileName())) { // skip play, default to save
                  DownloadUtilities.downloadAndOpen(_fileLink, new MsgLinkRecord[] { _parentMsgLink }, MainFrame.getServerInterfaceLayer(), true, true);
                } else {
                  Runnable openTask = new Runnable() {
                    public void run() {
                      DownloadUtilities.downloadAndOpen(_fileLink, new MsgLinkRecord[] { _parentMsgLink }, MainFrame.getServerInterfaceLayer(), true, false);
                    }
                  };
                  Runnable saveTask = new Runnable() {
                    public void run() {
                      DownloadUtilsGui.downloadFilesChoice(new FileLinkRecord[] { _fileLink }, new MsgLinkRecord[] { _parentMsgLink }, MsgPreviewPanel.this, MainFrame.getServerInterfaceLayer());
                    }
                  };
                  Window w = SwingUtilities.windowForComponent(MsgPreviewPanel.this);
                  if (w instanceof Frame)
                    new OpenSaveCancelDialog((Frame) w, _fileLink, _parentMsgLink, openTask, saveTask);
                  else if (w instanceof Dialog)
                    new OpenSaveCancelDialog((Dialog) w, _fileLink, _parentMsgLink, openTask, saveTask);
                }
              }
            }
          });
          label.setIcon(ListRenderer.getRenderedIcon(attachments[i]));
          label.setIconTextGap(2);
          FileLinkRecord fileLink = null;
          if (attachments[i] instanceof FileLinkRecord) {
            fileLink = (FileLinkRecord) attachments[i];
          }
          
          JPanel panel = new JPanel();
          panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
          panel.add(label);
          final JLabel progress = new JMyLabel("", (float) LINK_RELATIVE_FONT_SIZE);
          progress.setBorder(new EmptyBorder(0,2,0,0));
          if (fileLink != null) {
            progress.setText(Misc.getFormattedSize(fileLink.origSize, 3, 2));
          }

          if (fileLink != null && (FileLauncher.isAudioWaveFilename(fileLink.getFileName()) || FileLauncher.isImageFilename(fileLink.getFileName()))) {
            final JLabel play = new JMyLabel();
            play.setBorder(new EmptyBorder(0,2,0,0));
            play.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            panel.add(play);
            JLabel stopTmp = null;
            JSlider sliderTmp = null;
            final Object _clipDownloadMonitor = new Object();
            if (FileLauncher.isAudioWaveFilename(fileLink.getFileName())) {
              //play.setIcon(Images.get(ImageNums.VOLUME16));
              play.setIcon(Images.get(ImageNums.PLAY16));
              stopTmp = new JMyLabel();
              stopTmp.setIcon(Images.get(ImageNums.STOP16));
              stopTmp.setBorder(new EmptyBorder(0,2,0,0));
              stopTmp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
              sliderTmp = new JSlider(0, 10000, 0);
              sliderTmp.setBorder(new EmptyBorder(0,2,0,0));
              sliderTmp.setEnabled(false);
              //sliderTmp.setVisible(false);
            } else if (FileLauncher.isImageFilename(fileLink.getFileName())) {
              play.setIcon(Images.get(ImageNums.FIND16));
            }
            final JLabel stop = stopTmp;
            if (stop != null)
              panel.add(stop);
            final JSlider slider = sliderTmp;
            if (slider != null)
              panel.add(slider);
            Dimension size = new JMyLabel("188:88 / 188:88").getPreferredSize();
            if (size.width < progress.getPreferredSize().width)
              size = progress.getPreferredSize();
            progress.setSize(size);
            progress.setPreferredSize(size);
            progress.setMinimumSize(size);
            progress.setMaximumSize(size);

            final JMyLinkLikeLabel link = label;
            play.addMouseListener(new MouseAdapter() {
              private CallbackI callback = null;
              private int millisecondLength = -1;
              public void mouseClicked(MouseEvent event) {
                Object[] objs = (Object[]) link.getDescription();
                final MsgLinkRecord parentMsgLink = (MsgLinkRecord) objs[0];
                final Object att = objs[1];
                Thread th = new ThreadTraced("Clip Player") { // we must do the action on non-GUI thread
                  public void runTraced() {
                    if (att instanceof FileLinkRecord) {
                      final FileLinkRecord fileLink = (FileLinkRecord) att;
                      File file = null;
                      synchronized (_clipDownloadMonitor) {
                        file = DownloadUtilities.download(fileLink, new MsgLinkRecord[] { parentMsgLink }, MainFrame.getServerInterfaceLayer(), true);
                      }
                      if (file != null) {
                        if (FileLauncher.isAudioWaveFilename(fileLink.getFileName())) {
                          if (callback == null) {
                            callback = new CallbackI() {
                              public void callback(Object value) {
                                if (value instanceof String) {
                                  String command = (String) value;
                                  if (command.equals("play")) {
                                    play.setIcon(Images.get(ImageNums.PAUSE16));
                                    //stop.setIcon(Images.get(ImageNums.STOP16));
                                    //slider.setVisible(true);
                                  } else if (command.equals("pause")) {
                                    play.setIcon(Images.get(ImageNums.PLAY16));
                                    //stop.setIcon(Images.get(ImageNums.STOP16));
                                    //slider.setVisible(true);
                                  } else if (command.equals("close")) {
                                    //play.setIcon(Images.get(ImageNums.VOLUME16));
                                    play.setIcon(Images.get(ImageNums.PLAY16));
                                    //stop.setIcon(null);
                                    //stop.setIcon(Images.get(ImageNums.STOP16));
                                    if (millisecondLength > 0) {
                                      String timeLength = Misc.getFormattedTime(millisecondLength/1000);
                                      progress.setText(timeLength);
                                    } else {
                                      progress.setText(Misc.getFormattedSize(fileLink.origSize, 3, 2));
                                    }
                                    //slider.setVisible(false);
                                    slider.setValue(0);
                                  }
                                  if (!slider.isEnabled())
                                    slider.setEnabled(true);
                                } else if (value instanceof Integer) {
                                  String timePosition = Misc.getFormattedTime(((Integer) value).intValue());
                                  String timeLength = Misc.getFormattedTime(slider.getMaximum()/1000);
                                  progress.setText(timePosition + " / " + timeLength);
                                } else if (value instanceof Long) {
                                  if (slider.isVisible() && !slider.getValueIsAdjusting()) {
                                    long millisecondPosition = ((Long) value).longValue();
                                    slider.setValue((int) millisecondPosition);
                                  }
                                } else if (value instanceof Double) {
                                  millisecondLength = ((Double) value).intValue();
                                  slider.setMaximum(millisecondLength);
                                }
                              }
                            };
                          }
                          if (millisecondLength == -1) {
                            DecodingAudioClipPlayer.play(file, callback);
                          } else {
                            if (play.getIcon().equals(Images.get(ImageNums.PLAY16)))
                              DecodingAudioClipPlayer.play(file, callback, slider.getValue());
                            else
                              DecodingAudioClipPlayer.pause(file, callback);
                          }
                        } else if (FileLauncher.isImageFilename(fileLink.getFileName())) {
                          ImageViewer.showImage(file, MsgPreviewPanel.this);
                        }
                      } // end if file != null
                    }
                  }
                };
                th.setDaemon(true);
                th.start();
              }
            });

            if (stop != null) {
              stop.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent event) {
                  Object[] objs = (Object[]) link.getDescription();
                  final MsgLinkRecord parentMsgLink = (MsgLinkRecord) objs[0];
                  final Object att = objs[1];
                  Thread th = new ThreadTraced("Clip Stopper") { // we must do the action on non-GUI thread
                    public void runTraced() {
                      if (att instanceof FileLinkRecord) {
                        FileLinkRecord fileLink = (FileLinkRecord) att;
                        File file = null;
                        synchronized (_clipDownloadMonitor) {
                          file = DownloadUtilities.download(fileLink, new MsgLinkRecord[] { parentMsgLink }, MainFrame.getServerInterfaceLayer(), true);
                        }
                        if (FileLauncher.isAudioWaveFilename(fileLink.getFileName())) {
                          DecodingAudioClipPlayer.close(file);
                        }
                      }
                    }
                  };
                  th.setDaemon(true);
                  th.start();
                }
              });
            }

            if (slider != null) {
              slider.addChangeListener(new ChangeListener() {
                public void stateChanged(final ChangeEvent e) {
                  Object[] objs = (Object[]) link.getDescription();
                  final MsgLinkRecord parentMsgLink = (MsgLinkRecord) objs[0];
                  final Object att = objs[1];
                  Thread th = new ThreadTraced("Clip Seeker") { // we must do the action on non-GUI thread
                    public void runTraced() {
                      if (att instanceof FileLinkRecord) {
                        FileLinkRecord fileLink = (FileLinkRecord) att;
                        File file = null;
                        synchronized (_clipDownloadMonitor) {
                          file = DownloadUtilities.download(fileLink, new MsgLinkRecord[] { parentMsgLink }, MainFrame.getServerInterfaceLayer(), true);
                        }
                        if (FileLauncher.isAudioWaveFilename(fileLink.getFileName())) {
                          int millisecondPosition = slider.getValue();
                          if (slider.getValueIsAdjusting()) {
                            DecodingAudioClipPlayer.pauseSeek(file, millisecondPosition, e);
                          } else {
                            DecodingAudioClipPlayer.seekPlayIfPaused(file, millisecondPosition, e);
                          }
                        }
                      }
                    }
                  };
                  th.setDaemon(true);
                  th.start();
                }
              });
              slider.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent me){slider.setValue(slider.getMinimum()+((slider.getMaximum()-slider.getMinimum())*me.getX())/slider.getWidth());}
              });
            }
          }
          
          panel.add(progress);
          labelsV.addElement(panel);
        }
      }
      setFlowGridPanel(jAttachments, labelsV, 5, 4);
      visible = true;
    }
    jLineAttachments.setVisible(visible);

    jAttachments.revalidate();
    jAttachments.doLayout();
    if (trace != null) trace.exit(MsgPreviewPanel.class);
  }

  private static void setFlowGridPanel(JPanel panel, Vector labelsV, int HORIZ_INSET_SPACE, int MAX_VISIBLE_ROWS) {
    int VERT_GAP = 2;
    boolean flowMode = false; // don't use flow because even with scroll pane flow panel has difficulty moving components to next line
    boolean gridMode = true;
    // resets previous minimum sizing in case larger multi-row panel was used
    panel.setPreferredSize(null);
    panel.setMinimumSize(null);
    panel.setMaximumSize(null);

    int totalPrefWidth = 0;
    // gather preferred width of all labels
    for (int i=0; i<labelsV.size(); i++) {
      totalPrefWidth += ((JComponent) labelsV.elementAt(i)).getPreferredSize().width + HORIZ_INSET_SPACE;
    }
    int avgWidth = totalPrefWidth / labelsV.size();

    // See if all labels took too much space, if they did then use multi-row grid
    boolean isMultiRowMode = totalPrefWidth > panel.getSize().width;
    int MAX_COLUMNS = Math.max(1, (int) (panel.getSize().width / (avgWidth + avgWidth*0.25)));
    int NUM_COLUMNS = Math.min(labelsV.size(), MAX_COLUMNS);

    // set panel layout
    if (isMultiRowMode) {
      int rows = 0;
      if (gridMode) {
        rows = (int) Math.ceil(((double) labelsV.size()) / ((double) MAX_COLUMNS));
      } else if (flowMode) {
        rows = 1;
        int rowPrefWidth = 0;
        for (int i=0; i<labelsV.size(); i++) {
          int prefWidth = ((JComponent) labelsV.elementAt(i)).getPreferredSize().width + HORIZ_INSET_SPACE;
          rowPrefWidth += prefWidth;
          if (rowPrefWidth > panel.getSize().width) {
            rows ++;
            rowPrefWidth = prefWidth;
          }
        }
      }
      if (rows > MAX_VISIBLE_ROWS) {
        panel.setLayout(new BorderLayout());
        JPanel innerPanel = new JPanel();
        JScrollPane sPane = new JScrollPane(innerPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sPane.getVerticalScrollBar().setUnitIncrement(5);
        panel.add(sPane, BorderLayout.CENTER);
        JLabel labelSizer = new JLabel("Height SIZER");
        Dimension dim = new Dimension(panel.getSize().width, 4+(MAX_VISIBLE_ROWS-1)*VERT_GAP+labelSizer.getPreferredSize().height*MAX_VISIBLE_ROWS);
        panel.setPreferredSize(dim);
        panel.setMinimumSize(dim);
        panel.setMaximumSize(dim);
        panel = innerPanel;
      }
      if (gridMode)
        panel.setLayout(new GridLayout(rows, NUM_COLUMNS, HORIZ_INSET_SPACE, VERT_GAP));
      else if (flowMode)
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, HORIZ_INSET_SPACE, VERT_GAP));
    } else {
      panel.setLayout(new GridBagLayout());
    }
    // add elements to panel
    int posX = 0;
    for (posX=0; posX<labelsV.size(); posX++) {
      JComponent label = (JComponent) labelsV.elementAt(posX);
      if (!isMultiRowMode && gridMode) {
        panel.add(label, new GridBagConstraints(posX, 0, 1, 1, 0, 0, 
          GridBagConstraints.WEST, GridBagConstraints.NONE, new MyInsets(0, 0, 0, HORIZ_INSET_SPACE), 0, 0));
      } else {
        panel.add(label);
      } // end isMultiRowMode==true
    }
    if (!isMultiRowMode && gridMode) {
      // add horizontal filler
      panel.add(new JLabel(), new GridBagConstraints(posX, 0, 1, 1, 10, 0, 
        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new MyInsets(0, 0, 0, 0), 0, 0));
    }
  }

  /**
   * Invoked to display info stating that no "Message" is selected.
   */
  private void setCurrentMessageText() {
    setCurrentMessageText(isHTML);
  }
  private void setCurrentMessageText(boolean displayHtmlMode) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgPreviewPanel.class, "setCurrentMessageText(boolean displayHtmlMode)");
    if (trace != null) trace.args(displayHtmlMode);
    while (true) { // single-pass-loop: conveniance loop so we can easily break out
      if ((msgDataRecord == null && !isHTML) ||
          (msgDataRecord != null && isHTML != displayHtmlMode))
      {
        // will call back to this method recursively...
        // but first we need to change display mode
        setHTMLMode(msgDataRecord != null ? displayHtmlMode : true);
        break;
      } else {
        if (msgDataRecord != null) {
          boolean skipHeaderClearing = false;
          String text = "";
          if (msgDataRecord.isTypeMessage()) {
            text = msgDataRecord.getText();
          } else if (msgDataRecord.isTypeAddress()) {
            JSplitPane splitPane = MiscGui.getParentSplitPane(MsgPreviewPanel.this);
            if (splitPane != null && splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
              text = msgDataRecord.parseAddressBody(true, false);
            } else {
              text = msgDataRecord.addressBody;
            }
            skipHeaderClearing = true;
          } else {
            throw new IllegalStateException("Don't know how to handle objType=" + msgDataRecord.objType);
          }
          boolean convertHTMLtoPLAIN = !isHTML && msgDataRecord.isHtmlMail();
          if (text == null && !isHTML) {
            setHTMLMode(true);
            break; // will come back here in a recursive call to set the actual text body...
          }
          if (text == null) {
            text = STR_MSG_BODY_UNAVAILABLE;
            convertHTMLtoPLAIN = !isHTML;
            isWaitingForMsgBody = true;
          } else if (msgDataRecord.bodyPassHash != null && msgDataRecord.getTextBody() == null && msgDataRecord.getEncText() != null && msgDataRecord.getEncText().size() > 0) {
            // Also waiting for body when it is password protected and password not yet entered.
            isWaitingForMsgBody = true;
          } else {
            isWaitingForMsgBody = false;
          }
          MsgPanelUtils.setPreviewContent_Threaded(text, isHTML, convertHTMLtoPLAIN, skipHeaderClearing, jMessage);
        } else {
          MsgPanelUtils.setPreviewContent_Threaded(no_selected_msg_html, isHTML, false, true, jMessage);
          // Clear panels and make them original size.
          setRecipientsPanel(null, jRecipients, jLineRecipients);
          setAttachmentsButton();
          setAttachmentsPanel(null, null, jAttachments, jLineAttachments);
        }
        updateHTMLModeIcon();
      }
      // always break out of the loop as it is a single-pass-loop only
      break;
    }
    if (trace != null) trace.exit(MsgPreviewPanel.class);
  }


  /**
   * Initialize the Preview Panel to display specified message data.
   * Update takes place right away.
   */
  public void initData(MsgLinkRecord msgLinkRecord) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgPreviewPanel.class, "initData(MsgLinkRecord msgLinkRecord)");
    if (trace != null) trace.args(msgLinkRecord);

    if (trace != null) trace.data(10, this.msgLinkRecord);
    if (trace != null) trace.data(11, msgLinkRecord);

    // Since it is possible to update a message with change of expiry date or content due to message revokation
    // update the preview everytime the message update comes
    if (true) {
//    if (this.msgLinkRecord != msgLinkRecord) {
      this.msgLinkRecord = msgLinkRecord;
      if (msgLinkRecord != null) {
        ourMsgFilter = new MsgFilter(new Long[] { msgLinkRecord.msgLinkId }, msgLinkRecord.ownerObjType, msgLinkRecord.ownerObjId);
      } else {
        ourMsgFilter = null;
      }
      addToMsgPreviewUpdateQueue(msgLinkRecord);
    } // end any changes

    if (trace != null) trace.exit(MsgPreviewPanel.class);
  } // end initData();


  /**
   * RecordSelectionListener interface implementation.
   */
  public void recordSelectionChanged(RecordSelectionEvent recordSelectionEvent) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgPreviewPanel.class, "recordSelectionChanged(RecordSelectionEvent recordSelectionEvent)");
    if (trace != null) trace.args(recordSelectionEvent);
    Record[] recs = recordSelectionEvent.getSelectedRecords();
    if (recs != null && recs.length == 1 && recs[0] != null)
      initData((MsgLinkRecord) recs[0]);
    else
      initData((MsgLinkRecord) null);
    if (trace != null) trace.exit(MsgPreviewPanel.class);
  }


  private void setGUIComponentsForObj(MsgDataRecord dataRecord) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgPreviewPanel.class, "setGUIComponentsForObj(MsgDataRecord dataRecord)");
    if (trace != null) trace.args(dataRecord);
    short objType = 0;
    ImageText priority = null;
    boolean isExpiry = false;
    boolean isPassword = false;
    boolean hasAttachments = false;
    if (dataRecord != null) {
      objType = dataRecord.objType.shortValue();
      priority = dataRecord.getPriorityTextAndIcon();
      String expiryText = dataRecord.getExpirationIconAndText(FetchedDataCache.getSingleInstance().getMyUserId(), true).getText();
      isExpiry = expiryText != null && expiryText.trim().length() > 0;
      isPassword = dataRecord.bodyPassHash != null && dataRecord.getTextBody() == null && dataRecord.getEncText() != null && dataRecord.getEncText().size() > 0;
      hasAttachments = dataRecord.attachedFiles.shortValue() + dataRecord.attachedMsgs.shortValue() > 0;
    }
    setGUIComponentsForObj(objType, priority, isExpiry, isPassword, hasAttachments);
    if (trace != null) trace.exit(MsgPreviewPanel.class);
  }


  private synchronized void setGUIComponentsForObj(short objType, ImageText priority, boolean isExpiry, boolean isPassword, boolean hasAttachments) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgPreviewPanel.class, "setGUIComponentsForObj(short objType, ImageText priority, boolean isExpiry, boolean isPassword, boolean hasAttachments)");
    if (trace != null) trace.args(objType);
    if (trace != null) trace.args(priority);
    if (trace != null) trace.args(isExpiry);
    if (trace != null) trace.args(isPassword);
    if (trace != null) trace.args(hasAttachments);

    String priorityLabel = priority != null ? priority.getText() : "";
    Icon priorityIcon = priority != null ? Images.get(priority) : null;
    if (lastGUIcomponentsMode != objType
        || !priorityLabel.equals(lastGUIpriorityMode)
        || lastGUIexpiryMode != isExpiry
        || lastGUIpasswordMode != isPassword
        || lastGUIhasAttachmentsMode != hasAttachments) {
      lastGUIcomponentsMode = objType;
      lastGUIpriorityMode = priorityLabel;
      lastGUIexpiryMode = isExpiry;
      lastGUIpasswordMode = isPassword;
      lastGUIhasAttachmentsMode = hasAttachments;
      boolean msgMode = objType == MsgDataRecord.OBJ_TYPE_MSG;
      boolean addrMode = objType == MsgDataRecord.OBJ_TYPE_ADDR;
      jPriority.setIcon(priorityIcon);
      jPriority.setText(priorityLabel);
      jLinePriority.setVisible(priorityIcon != null);
      jLineFrom.setVisible(msgMode);
      jLineRecipients.setVisible(msgMode);
      lineRecipientsVisibilityAllowed = msgMode;
      jLineAttachments.setVisible(hasAttachments);
      jLineExpiration.setVisible(isExpiry);
      jPasswordField.setText("");
      jLinePassword.setVisible(isPassword);
      //if (isPassword) jLinePassword.requestFocus();
      if (msgMode) {
        jSubjectLabel.setText(com.CH_gui.lang.Lang.rb.getString("label_Subject"));
      } else if (addrMode) {
        jSubjectLabel.setText(com.CH_gui.lang.Lang.rb.getString("label_Name"));
        setHTMLMode(true);
      }
    }

    if (trace != null) trace.exit(MsgPreviewPanel.class);
  }



  /*******************************************************
  *** V i s u a l s S a v a b l e    interface methods ***
  *******************************************************/
  public String getVisuals() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgPreviewPanel.class, "getVisuals()");

    StringBuffer visuals = new StringBuffer();
    visuals.append("Dimension width ");
    Dimension dim = getSize();
    visuals.append(dim.width);
    visuals.append(" height ");
    visuals.append(dim.height);

    String rc = visuals.toString();
    if (trace != null) trace.exit(MsgPreviewPanel.class, rc);
    return rc;
  }

  public void restoreVisuals(String visuals) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgPreviewPanel.class, "restoreVisuals(String visuals)");
    if (trace != null) trace.args(visuals);

    try {
      if (visuals == null) {
        // do nothing
      } else {
        StringTokenizer st = new StringTokenizer(visuals);  
        st.nextToken();
        st.nextToken();
        int width = Integer.parseInt(st.nextToken());
        st.nextToken();
        int height = Integer.parseInt(st.nextToken());
        setPreferredSize(new Dimension(width, height));
      }
    } catch (Throwable t) {
      if (trace != null) trace.exception(MsgPreviewPanel.class, 100, t);
    }

    if (trace != null) trace.exit(MsgPreviewPanel.class);
  }
  public String getExtension() {
    return null;
  }
  public static final String visualsClassKeyName = "MsgPreviewPanel";
  public String getVisualsClassKeyName() {
    return visualsClassKeyName;
  }
  public Integer getVisualsVersion() {
    return null;
  }
  public boolean isVisuallyTraversable() {
    return true;
  }


  /**  I N T E R F A C E   M E T H O D  ---   D i s p o s a b l e O b j  *****
   * Dispose the object and release resources to help in garbage collection.
   */
  public void disposeObj() {
    if (linkListener != null) {
      FetchedDataCache.getSingleInstance().removeMsgLinkRecordListener(linkListener);
      linkListener = null;
    }
    if (dataListener != null) {
      FetchedDataCache.getSingleInstance().removeMsgDataRecordListener(dataListener);
      dataListener = null;
    } 
    if (msgPreviewUpdateQueue != null) {
      msgPreviewUpdateQueue.kill();
      msgPreviewUpdateQueue = null;
    }
    componentsForPopupV.clear();
  }

  public static class PrintRunnable implements Runnable {
    private MsgLinkRecord msgLinkRecord;
    private MsgDataRecord msgDataRecord;
    private boolean isForceSimpleHTML;
    private Component context;
    public PrintRunnable(MsgLinkRecord msgLinkRecord, MsgDataRecord msgDataRecord, boolean isForceSimpleHTML, Component context) {
      Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(PrintRunnable.class, "PrintRunnable(MsgLinkRecord msgLinkRecord, MsgDataRecord msgDataRecord, boolean isForceSimpleHTML, Component context)");
      this.msgLinkRecord = msgLinkRecord;
      this.msgDataRecord = msgDataRecord;
      this.isForceSimpleHTML = isForceSimpleHTML;
      this.context = context;
      if (trace != null) trace.exit(PrintRunnable.class);
    }
    public void run() {
      Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(PrintRunnable.class, "PrintRunnable.run()");

      context.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      try {
        com.CH_gui.print.DocumentRenderer renderer = new com.CH_gui.print.DocumentRenderer();

        boolean simplifyHTML = msgDataRecord.isHtmlMail() && isForceSimpleHTML;
        String[] contentParts = MsgComposeComponents.makeReplyToContent(msgLinkRecord, msgDataRecord, simplifyHTML, true, false);
        String content = "<html><body>";
        if (contentParts[0].equalsIgnoreCase("text/html")) {
          content += "<p>" + contentParts[1] + "</p>\n";
        } else {
          content += "<p>" + Misc.encodePlainIntoHtml(contentParts[1]) + "</p>\n";
        }
        if (contentParts[2].equalsIgnoreCase("text/html")) {
          content += "<p><font size='-2' face='Monospaced, Curier, Arial, Verdana, Helvetica, sans-serif'>" + contentParts[3] + "</font></p>";
        } else {
          content += "<p><font size='-2' face='Monospaced, Curier, Arial, Verdana, Helvetica, sans-serif'>" + Misc.encodePlainIntoHtml(contentParts[3]) + "</font></p>";
        }
        content += "</body></html>";

        // condition and normalize text
        //JEditorPane normalization = new JEditorPane("text/html", content);
        JEditorPane normalization = new HTML_ClickablePane(content);
        // Use our own pane to fix display of internal icons because it will adjust document base
        //JEditorPane pane = new JEditorPane("text/html", "<html></html>");
        JEditorPane pane = new HTML_ClickablePane("");
        MsgPanelUtils.setMessageContent(normalization.getText(), true, pane);

        renderer.setDocument(pane);
        Window w = SwingUtilities.windowForComponent(context);
        if (w instanceof Dialog)
          new com.CH_gui.print.PrintPreview(renderer, "Print Preview", (Dialog) w);
        else if (w instanceof Frame)
          new com.CH_gui.print.PrintPreview(renderer, "Print Preview", (Frame) w);
      } catch (Throwable t) {
        MessageDialog.showErrorDialog(context, "Error encountered while constructing a print preview.\n\nReason: " + t.toString() + "\n\n" + t.getMessage() + "\n\n" + Misc.getStack(t), "Print error", true);
      }
      context.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

      if (trace != null) trace.exit(PrintRunnable.class);
    }
  }

  private class MsgPreviewUpdaterProcessor implements ProcessingFunctionI {

    public void processQueuedObject(Object obj) {
      Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(getClass(), "processQueuedObject(Object obj)");
      if (trace != null) trace.args(obj);

      MsgLinkRecord previewMsgLink = (MsgLinkRecord) obj;

      MsgDataRecord previewMsgData = null;
      // if we are still pointing to the same message, then make sure we have its data
      if (previewMsgLink != null && previewMsgLink == MsgPreviewPanel.this.msgLinkRecord) {
        // if message data was never fetched before || or we don't have the message body, fetch it
        try {
          ServerInterfaceLayer serverInterfaceLayer = MainFrame.getServerInterfaceLayer();
          FetchedDataCache cache = FetchedDataCache.getSingleInstance();
          previewMsgData = cache.getMsgDataRecord(previewMsgLink.msgId);
          if ((previewMsgLink.status.shortValue() & MsgLinkRecord.STATUS_FLAG__READ) == 0 || 
              previewMsgData == null || previewMsgData.getEncText() == null)
          {
            // fetching can take a while so show the "Loading..." sign
            jLoadingLabel.setVisible(true);
            // Prepare and send the request
            ProtocolMsgDataSet request = MsgDataOps.prepareRequestToFetchMsgBody(previewMsgLink);
            serverInterfaceLayer.submitAndWait(new MessageAction(CommandCodes.MSG_Q_GET_BODY, request), 60000, 3);
            if (previewMsgData == null)
              previewMsgData = cache.getMsgDataRecord(previewMsgLink.msgId);
          }
        } catch (Throwable t) {
          if (trace != null) trace.exception(getClass(), 100, t);
        }
      }

      Vector emailNicksV = new Vector();
      Vector emailStringRecordsV = new Vector();
      Vector emailRecordsOrigV = new Vector();
      // if we are still pointing to the same message, then make sure we check address books before displaying Sender Information
      if (previewMsgLink != null && previewMsgLink == MsgPreviewPanel.this.msgLinkRecord) {
        try {
          if (previewMsgData != null) {
            MsgActionTable.getEmailNickAndAddress(previewMsgData, emailNicksV, emailStringRecordsV, false);
            if (emailStringRecordsV.size() > 0) {
              emailRecordsOrigV = MsgComposePanel.checkEmailAddressesForAddressBookAdition(MsgPreviewPanel.this, emailNicksV, emailStringRecordsV, false, true, new FolderFilter(FolderRecord.ADDRESS_FOLDER));
            }
          }
        } catch (Throwable t) {
          if (trace != null) trace.exception(getClass(), 200, t);
        }
      }

      // if we are still pointing to the same message, then update
      if (previewMsgLink == MsgPreviewPanel.this.msgLinkRecord) {
        try {
          javax.swing.SwingUtilities.invokeAndWait(new PreviewGUIUpdater(previewMsgLink, previewMsgData, emailNicksV, emailStringRecordsV, emailRecordsOrigV));
        } catch (Throwable t) {
          if (trace != null) trace.exception(getClass(), 300, t);
        } finally {
          jLoadingLabel.setVisible(false);
        }
      } // end if still the same message

      // After we are done with this update, clear the queue leaving 1 last object iff different than what we have just displayed...
      if (msgPreviewUpdateFifo != null && msgPreviewUpdateFifo.size() > 0) {
        while (msgPreviewUpdateFifo.size() > 1)
          msgPreviewUpdateFifo.remove();
        MsgLinkRecord nextToUpdate = (MsgLinkRecord) msgPreviewUpdateFifo.peek();
        // If last object in queue is the same, discard it
        if (previewMsgLink == nextToUpdate || (previewMsgLink != null && previewMsgLink.equals(nextToUpdate)) || (nextToUpdate != null && nextToUpdate.equals(previewMsgLink)))
          msgPreviewUpdateFifo.remove();
      }
      if (trace != null) trace.exit(getClass());
    } // end run()
  } // end private class MsgPreviewUpdaterProcessor


  private class PreviewGUIUpdater implements Runnable {
    MsgLinkRecord msgLink;
    MsgDataRecord msgData;
    Vector emailNicksV;
    Vector emailStringRecordsV;
    Vector emailRecordsOrigV;
    private PreviewGUIUpdater(MsgLinkRecord msgLink, MsgDataRecord msgData, Vector emailNicksV, Vector emailStringRecordsV, Vector emailRecordsOrigV) {
      Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(PreviewGUIUpdater.class, "PreviewGUIUpdater(MsgLinkRecord msgLink, MsgDataRecord msgData, Vector emailNicksV, Vector emailStringRecordsV, Vector emailRecordsOrigV)");
      this.msgLink = msgLink;
      this.msgData = msgData;
      this.emailNicksV = emailNicksV;
      this.emailStringRecordsV = emailStringRecordsV;
      this.emailRecordsOrigV = emailRecordsOrigV;
      if (trace != null) trace.exit(PreviewGUIUpdater.class);
    }

    public void run() {
      Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(PreviewGUIUpdater.class, "PreviewGUIUpdater.run()");
      updatePreviewNow(msgLink);
      if (trace != null) trace.exit(PreviewGUIUpdater.class);
    }

    private void updatePreviewNow(MsgLinkRecord msgLink) {
      Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(PreviewGUIUpdater.class, "updatePreviewNow(MsgLinkRecord msgLink)");
      if (trace != null) trace.args(msgLink);

      // global try
      try {
        if (msgLink == null || msgData == null) {
          msgDataRecord = null; // nullify the global view msg data 
          jPriority.setIcon(null);
          jPriority.setText("");
          jFromName.setIcon(null);
          jFromName.setText("");
          //jFromNameAddContact.setText("");
          jFromNameAddContact.setVisible(false);
          jFromNameAddContact.setDescription(null);
          jMsgDate.setText("");
          jReplyTo.setIcon(null);
          jReplyTo.setText("");
          jLineReplyTo.setVisible(false);
          jRecipients.removeAll();
          jSubject.setIcon(null);
          jSubject.setText("");
          //jNotSpam.setText("");
          jNotSpam.setVisible(false);
          jNotSpam.setDescription(null);
          jExpiration.setIcon(null);
          jExpiration.setText("");
          jPasswordHintText.setText("None");
          setCurrentMessageText();
          setAttachmentsButton();
          setAttachmentsPanel(null, null, jAttachments, jLineAttachments);
        } else {
          FetchedDataCache cache = FetchedDataCache.getSingleInstance();
          MsgDataRecord dataRecord = msgData;

          boolean msgDataChanged = (msgDataRecord != null && !msgDataRecord.equals(dataRecord)) ||
                                   !dataRecord.equals(msgDataRecord) ||
                                   !dataRecord.isPrivilegedBodyAccess(cache.getMyUserId(), new Date()) || // in case the body got expired, update preview
                                   (isWaitingForMsgBody && dataRecord.getText() != null);

          // Force msgDataChanged if layout changed...
          JSplitPane splitPane = MiscGui.getParentSplitPane(MsgPreviewPanel.this);
          if (splitPane != null) {
            int orientation = splitPane.getOrientation();
            if (orientation != lastSplitOrientationMode)
              msgDataChanged = true;
            lastSplitOrientationMode = orientation;
          }

          // assign a global view msg data 
          msgDataRecord = dataRecord;

          // create the Add Contact link if email address is not in the Address Books
          //jFromNameAddContact.setText("");
          boolean addContactLinkVisible = false;
          jFromNameAddContact.setDescription(null);
          jNotSpam.setDescription(null);
          if (emailRecordsOrigV.size() > 0) {
            String[] contactInfo = new String[] { ""+emailNicksV.elementAt(0), ""+emailStringRecordsV.elementAt(0) };
            addContactLinkVisible = true;
            jFromNameAddContact.setDescription(contactInfo);
            //jNotSpam.setVisible(true); // visible for junk folder even if it has no email address (move operation is still active)
            jNotSpam.setDescription(contactInfo);
          }
          jFromNameAddContact.setVisible(addContactLinkVisible);
          jNotSpam.setVisible(msgLink.ownerObjType.shortValue() == Record.RECORD_TYPE_FOLDER &&
                              msgLink.ownerObjId.equals(cache.getUserRecord().junkFolderId));

          // Set text and icon for the From field in the preview.
          Record fromRec = null;
          {
            String fromEmailAddress = dataRecord.getFromEmailAddress();
            if (dataRecord.isEmail() || fromEmailAddress != null) {
              fromRec = CacheUtilities.convertToFamiliarEmailRecord(fromEmailAddress);
            } else {
              fromRec = MsgPanelUtils.convertUserIdToFamiliarUser(dataRecord.senderUserId, false, true);
            }
          }
          if (fromRec != null) {
            jFromName.setIcon(ListRenderer.getRenderedIcon(fromRec));
            jFromName.setText(ListRenderer.getRenderedText(fromRec, false, false, true));
          } else {
            jFromName.setIcon(Images.get(ImageNums.PERSON_SMALL));
            jFromName.setText(java.text.MessageFormat.format(com.CH_gui.lang.Lang.rb.getString("User_(USER-ID)"), new Object[] {dataRecord.senderUserId}));
          }

          if (msgDataChanged) {
            jMsgDate.setText(Misc.getFormattedTimestamp(dataRecord.dateCreated, DateFormat.MEDIUM, DateFormat.MEDIUM));

            String[] replyTos = dataRecord.getReplyToAddresses();
            if (replyTos != null && (replyTos.length > 1 || (replyTos.length == 1 && !EmailRecord.isAddressEqual(replyTos[0], dataRecord.getFromEmailAddress())))) {
              jReplyTo.setIcon(Images.get(ImageNums.EMAIL_SYMBOL_SMALL));
              StringBuffer _replyTos = new StringBuffer();
              for (int i=0; i<replyTos.length; i++) {
                if (i > 0)
                  _replyTos.append(", ");
                _replyTos.append(replyTos[i]);
              }
              jReplyTo.setText(_replyTos.toString());
              jLineReplyTo.setVisible(true);
            } else {
              jLineReplyTo.setVisible(false);
              jReplyTo.setIcon(null);
              jReplyTo.setText("");
            }

            setRecipientsPanel(dataRecord, jRecipients, jLineRecipients);

            jSubject.setIcon(ListRenderer.getRenderedIcon(dataRecord));
            jSubject.setText(ListRenderer.getRenderedText(dataRecord));
            jSubject.setToolTipText(jSubject.getText());

            jPasswordHintText.setText(dataRecord.bodyPassHint != null ? dataRecord.bodyPassHint : "None");
            setAttachmentsButton();
            setAttachmentsPanel(msgLink, dataRecord, jAttachments, jLineAttachments);
          }

          ImageText exp = dataRecord.getExpirationIconAndText(cache.getMyUserId());
          jExpiration.setIcon(Images.get(exp));
          jExpiration.setText(exp.getText());

          boolean displayHtmlMode = !isDefaultToPLAINpreferred(msgLink, dataRecord);

          // update text only if content changed
          if (isHTML != displayHtmlMode || msgDataChanged)
            setCurrentMessageText(displayHtmlMode);
        } // end else
        setGUIComponentsForObj(msgDataRecord);
        // we changed the message header data, lets display it
        try {
          revalidate();
        } catch (Throwable t) {
        } try {
          doLayout();
        } catch (Throwable t) {
        } 

      // global catch
      } catch (Throwable t) {
        if (trace != null) trace.exception(PreviewGUIUpdater.class, 400, t);
      }
      if (trace != null) trace.exit(PreviewGUIUpdater.class);
    } // end updatePreviewNow()
  } // end private class PreviewGUIUpdater

  /**
   * @param mData
   * @return true iff message should default to PLAIN mode given user's general settings
   */
  private static boolean isDefaultToPLAINpreferred(MsgDataRecord mData) {
    FetchedDataCache cache = FetchedDataCache.getSingleInstance();
    boolean plainPreferred = false;
    if (mData == null)
      plainPreferred = Misc.isBitSet(cache.getUserRecord().notifyByEmail, UserRecord.EMAIL_MANUAL_SELECT_PREVIEW_MODE);
    else
      plainPreferred = Misc.isBitSet(cache.getUserRecord().notifyByEmail, UserRecord.EMAIL_MANUAL_SELECT_PREVIEW_MODE) &&
                       !mData.senderUserId.equals(cache.getMyUserId()) && // Msgs created by myself never display in non-native mode
                       !(MsgPanelUtils.convertUserIdToFamiliarUser(mData.senderUserId, true, false, false) instanceof ContactRecord); // skip non-native mode for Msgs from your Contacts
    return plainPreferred;
  }

  /**
   * @param msgLink
   * @param dataRecord
   * @return true iff message should be displayed in PLAIN mode
   */
  public static boolean isDefaultToPLAINpreferred(MsgLinkRecord msgLink, MsgDataRecord dataRecord) {
    boolean plainPreferred = false;
    FolderRecord ownerFolder = null;
    FetchedDataCache cache = FetchedDataCache.getSingleInstance();
    if (dataRecord.isTypeAddress()) {
      // display Address Records always in HTML
      plainPreferred = false;
    } else if ( msgLink.getOwnerObjType().shortValue() == Record.RECORD_TYPE_FOLDER &&
                (ownerFolder = cache.getFolderRecord(msgLink.getOwnerObjId())) != null &&
                (ownerFolder.folderType.shortValue() == FolderRecord.POSTING_FOLDER || ownerFolder.folderType.shortValue() == FolderRecord.CHATTING_FOLDER)
                ) {
      // display Postings (Chatting msgs too) in NATIVE form because they were already visible in their full form the table anyway
      plainPreferred = !dataRecord.isHtmlMail();
    } else if (!Misc.isBitSet(msgLink.status, MsgLinkRecord.STATUS_FLAG__APPROVED_FOR_NATIVE_PREVIEW_MODE) && isDefaultToPLAINpreferred(dataRecord)) {
      plainPreferred = true;
    } else {
      plainPreferred = !dataRecord.isHtmlMail();
    }
    return plainPreferred;
  }


  /****************************************************************************/
  /*        A c t i o n P r o d u c e r I                                  
  /****************************************************************************/

  /**
   * @return all the acitons that this objects produces.
   */
  public Action[] getActions() {
    return actions;
  }

  /**
   * Final Action Producers will not be traversed to collect its containing objects' actions.
   * @return true if this object will gather all actions from its childeren or hide them counciously.
   */
  public boolean isFinalActionProducer() {
    return true;
  }

  /**
   * Enables or Disables actions based on the current state of the Action Producing component.
   */
  public void setEnabledActions() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        actions[COPY_ACTION].setEnabled(true); // always active
        actions[SELECT_ALL_ACTION].setEnabled(true); // always active
      }
    });
  }


  /****************************************************************************************/
  /************* LISTENERS ON CHANGES IN THE CACHE ****************************************/
  /****************************************************************************************/

  /** Listen on updates to the MsgLinkRecords in the cache.
    * if the event happens relevant to this preview, act on it.
    */
  private class MsgLinkListener implements MsgLinkRecordListener {
    public void msgLinkRecordUpdated(MsgLinkRecordEvent event) {
      // Exec on event thread to avoid potential GUI deadlocks
      javax.swing.SwingUtilities.invokeLater(new MsgGUIUpdater(event));
    }
  }

  /** Listen on updates to the MsgDataRecords in the cache.
    * if the event happens relevant to this preview, act on it.
    */
  private class MsgDataListener implements MsgDataRecordListener {
    public void msgDataRecordUpdated(MsgDataRecordEvent event) {
      // Exec on event thread 
      javax.swing.SwingUtilities.invokeLater(new MsgGUIUpdater(event));
    }
  }

  private class MsgGUIUpdater implements Runnable {
    private RecordEvent event;
    public MsgGUIUpdater(RecordEvent event) {
      Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgGUIUpdater.class, "MsgGUIUpdater(RecordEvent event)");
      this.event = event;
      if (trace != null) trace.exit(MsgGUIUpdater.class);
    }
    public void run() {
      Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MsgGUIUpdater.class, "MsgGUIUpdater.run()");

      if (event instanceof MsgLinkRecordEvent) {
        Record[] recs = event.getRecords();
        Record ourRec = null;
        for (int i=0; i<recs.length; i++) {
          if (ourMsgFilter != null && ourMsgFilter.keep(recs[i])) {
            ourRec = recs[i];
            break;
          }
        }
        if (ourRec != null) {
          initData(FetchedDataCache.getSingleInstance().getMsgLinkRecord(ourRec.getId()));
        }
      } else if (event instanceof MsgDataRecordEvent) {
        // and changes to Address Records may affect the view...
        // only for message views, exclude address book view
        if (msgDataRecord != null && msgDataRecord.isTypeMessage()) {
          if (event.getEventType() == RecordEvent.SET) {
            Record[] records = event.getRecords();
            for (int i=0; i<records.length; i++) {
              if (((MsgDataRecord) records[i]).isTypeAddress()) {
                addToMsgPreviewUpdateQueue(msgLinkRecord);
                break;
              }
            }
          }
        }
      }

      // Runnable, not a custom Thread -- DO NOT clear the trace stack as it is run by the AWT-EventQueue Thread.
      if (trace != null) trace.exit(MsgGUIUpdater.class);
    }
  }

  private synchronized void addToMsgPreviewUpdateQueue(MsgLinkRecord msgLinkRecord) {
    if (msgPreviewUpdateQueue == null) {
      msgPreviewUpdateFifo = new Fifo();
      msgPreviewUpdateQueue = new QueueMM1("Msg Preview Update Queue", msgPreviewUpdateFifo, new MsgPreviewUpdaterProcessor());
    }
    if (msgPreviewUpdateFifo != null)
      msgPreviewUpdateFifo.add(msgLinkRecord);
  }

} // end class MsgPreviewPanel