/**
 * Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
 *
 * This software is the confidential and proprietary information
 * of CryptoHeaven Corp. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with CryptoHeaven Corp.
 */
package com.CH_cl.service.cache;

import com.CH_cl.service.cache.event.*;
import com.CH_cl.service.engine.ServerInterfaceLayer;
import com.CH_cl.service.records.filters.FolderFilter;
import com.CH_cl.util.GlobalSubProperties;
import com.CH_co.cryptx.*;
import com.CH_co.service.msg.MessageAction;
import com.CH_co.service.msg.dataSets.obj.Obj_List_Co;
import com.CH_co.service.records.*;
import com.CH_co.service.records.filters.MsgFilter;
import com.CH_co.service.records.filters.RecordFilter;
import com.CH_co.trace.ThreadTraced;
import com.CH_co.trace.Trace;
import com.CH_co.util.*;
import java.security.MessageDigest;
import java.util.*;

/**
* Copyright 2001-2014 CryptoHeaven Corp. All Rights Reserved.
*
* <b>$Revision: 1.55 $</b>
*
* @author  Marcin Kurzawa
*/
public class FetchedDataCache extends Object {

  // in production set DEBUG to false
  public static final boolean DEBUG__SUPPRESS_EVENTS_STATS = false;
  public static final boolean DEBUG__SUPPRESS_EVENTS_MSGS = false;
  public static final boolean DEBUG__SUPPRESS_EVENTS_FILES = false;
  public static final boolean DEBUG__SUPPRESS_EVENTS_FOLDERS = false;
  public static final boolean DEBUG__SUPPRESS_EVENTS_INV_EMLS = false;
  public static final boolean DEBUG__SUPPRESS_EVENTS_KEYS = false;
  public static final boolean DEBUG__SUPPRESS_EVENTS_USERS = false;
  public static final boolean DEBUG__SUPPRESS_EVENTS_CONTACTS = false;

  // login user record
  private Long myUserId;

  // login user settings record
  private UserSettingsRecord myUserSettingsRecord;

  // login user password recovery record
  private PassRecoveryRecord myPassRecoveryRecord;

  // login user password
  private BAEncodedPassword encodedPassword;

  // new user private key
  private RSAPrivateKey newUserPrivateKey;


  private Map userRecordMap;
  private Map folderRecordMap;
  private Map folderShareRecordMap;
  private MultiHashMap folderShareRecordMap_byFldId;
  private MultiHashMap folderShareRecordMap_byOwnerId;
  private Map fileLinkRecordMap;
  private MultiHashMap fileLinkRecordMap_byFileId; // key is the fileId
  private Map fileDataRecordMap;
  private Map invEmlRecordMap;
  private Map keyRecordMap;
  private Map contactRecordMap;
  private Map msgLinkRecordMap;
  private MultiHashMap msgLinkRecordMap_byMsgId; // key is the msgId
  private Map msgDataRecordMap;
  private MultiHashMap msgDataRecordMap_byReplyToMsgId; // key is the replyToMsgId
  private Map emailRecordMap;
  private MultiHashMap addrHashRecordMap_byMsgId; // key is the msgId
  private MultiHashMap addrHashRecordMap_byHash; // key is the hash
  private Map statRecordMap;
  private MultiHashMap[] statRecordMap_byLinkId; // one map per each obj type
  private MultiHashMap[] statRecordMap_byObjId; // one map per each obj type
  private HashMap statsFetchedForMsgIds; // HashSet of fetched stats for msgIDs, and the stamps when they were fetched
  private ArrayList msgBodyKeys;
  private Set requestedAddrSet;

  // When folder contents is fetched, lets keep these folderIDs here.
  private Set fldIDsFetchRequestsIssuedSet;
  // When folder view is invalidated, lets keep these marks here.
  private Set fldIDsViewInvalidatedSet;
  // When staged fetching is enabled like in Android, store next fetch requests here
  private Map nextFolderFetchActionsMap;
  private Set nextFolderFetchProgressingIDsSet;

  private ArrayList viewIterators;

  public static final int STAT_TYPE_INDEX_FILE = StatRecord.STAT_TYPE_INDEX_FILE;
  public static final int STAT_TYPE_INDEX_FOLDER = StatRecord.STAT_TYPE_INDEX_FOLDER;
  public static final int STAT_TYPE_INDEX_MESSAGE = StatRecord.STAT_TYPE_INDEX_MESSAGE;

  EventListenerList myListenerList = new EventListenerList();

  /**
  * @returns a single instance of the cache.
  */
  public static FetchedDataCache getSingleInstance() {
    return SingletonHolder.INSTANCE;
  }
  private static class SingletonHolder {
    private static final FetchedDataCache INSTANCE = new FetchedDataCache();
  }

  /**
   * Instantiates new cache for use by web-server in web-applications.
   * @returns new instance of the cache.
   */
  public static FetchedDataCache getNewInstance() {
    return new FetchedDataCache();
  }

  /** Creates new FetchedDataCache */
  private FetchedDataCache() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "FetchedDataCache()");
    init();
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Starts a session for this instance and initializes all variables to empty.
  */
  private synchronized void init() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "init()");
    myUserId = null;
    myUserSettingsRecord = null;
    myPassRecoveryRecord = null;
    userRecordMap = new HashMap();
    folderRecordMap = new HashMap();
    folderShareRecordMap = new HashMap();
    folderShareRecordMap_byFldId = new MultiHashMap(true);
    folderShareRecordMap_byOwnerId = new MultiHashMap(true);
    fileLinkRecordMap = new HashMap();
    fileLinkRecordMap_byFileId = new MultiHashMap(true);
    fileDataRecordMap = new HashMap();
    invEmlRecordMap = new HashMap();
    keyRecordMap = new HashMap();
    contactRecordMap = new HashMap();
    msgLinkRecordMap = new HashMap();
    msgLinkRecordMap_byMsgId = new MultiHashMap(true);
    msgDataRecordMap = new HashMap();
    msgDataRecordMap_byReplyToMsgId = new MultiHashMap(true);
    emailRecordMap = new HashMap();
    addrHashRecordMap_byMsgId = new MultiHashMap(true);
    addrHashRecordMap_byHash = new MultiHashMap(true);
    statRecordMap = new HashMap();
    statRecordMap_byLinkId = new MultiHashMap[3];
    statRecordMap_byObjId = new MultiHashMap[3];
    for (int i=0; i<3; i++) {
      statRecordMap_byLinkId[i] = new MultiHashMap(true);
      statRecordMap_byObjId[i] = new MultiHashMap(true);
    }
    statsFetchedForMsgIds = new HashMap();
    msgBodyKeys = new ArrayList();
    requestedAddrSet = new HashSet();
    fldIDsFetchRequestsIssuedSet = new HashSet();
    fldIDsViewInvalidatedSet = new HashSet();
    nextFolderFetchActionsMap = new HashMap();
    nextFolderFetchProgressingIDsSet = new HashSet();
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Clears the cache to remove all of its data.
  */
  public void clear() {
    synchronized (this) {
      removeFileLinkRecords(getFileLinkRecords());
      removeMsgLinkRecords(getMsgLinkRecords());
      removeMsgDataRecords(getMsgDataRecords());
      removeContactRecords(getContactRecords());
      removeStatRecords(getStatRecords());
      removeEmailRecords(getEmailRecords());
      removeFolderRecords(getFolderRecords());
      removeFolderShareRecords(getFolderShareRecords());
      removeInvEmlRecords(getInvEmlRecords());
      removeKeyRecords(getKeyRecords());
      removeUserRecords(getUserRecords());

      myUserId = null;
      myUserSettingsRecord = null;
      myPassRecoveryRecord = null;
      encodedPassword = null;

      fileLinkRecordMap.clear();
      fileLinkRecordMap_byFileId.clear();
      fileDataRecordMap.clear();
      msgLinkRecordMap.clear();
      msgLinkRecordMap_byMsgId.clear();
      msgDataRecordMap.clear();
      msgDataRecordMap_byReplyToMsgId.clear();
      contactRecordMap.clear();
      statRecordMap.clear();
      for (int i=0; i<3; i++) {
        statRecordMap_byLinkId[i].clear();
        statRecordMap_byObjId[i].clear();
      }
      statsFetchedForMsgIds.clear();
      emailRecordMap.clear();
      folderRecordMap.clear();
      folderShareRecordMap.clear();
      folderShareRecordMap_byFldId.clear();
      folderShareRecordMap_byOwnerId.clear();
      invEmlRecordMap.clear();
      keyRecordMap.clear();
      userRecordMap.clear();
      msgBodyKeys.clear();
      requestedAddrSet.clear();
      addrHashRecordMap_byMsgId.clear();
      addrHashRecordMap_byHash.clear();

      fldIDsFetchRequestsIssuedSet.clear();
      fldIDsViewInvalidatedSet.clear();
      nextFolderFetchActionsMap.clear();
      nextFolderFetchProgressingIDsSet.clear();
    }
  }

  /**
  *  Sets the encoded password for the duration of this connection.
  */
  public synchronized void setEncodedPassword(BAEncodedPassword encPassword) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "setEncodedPassword(BAEncodedPassword)");
    encodedPassword = encPassword;
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  *  Gets the encoded password for the duration of this connection.
  */
  public synchronized BAEncodedPassword getEncodedPassword() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getEncodedPassword()");
    if (encodedPassword == null)
      throw new IllegalStateException("Encoded password is not available at this time.");
    if (trace != null) trace.exit(FetchedDataCache.class);
    return encodedPassword;
  }

  /**
  *  Sets the private key generated for the new user account.
  */
  public synchronized void setNewUserPrivateKey(RSAPrivateKey rsaPrivateKey) {
    newUserPrivateKey = rsaPrivateKey;
  }
  /**
  *  Sets the private key generated for the new user account.
  */
  public synchronized RSAPrivateKey getNewUserPrivateKey() {
    return newUserPrivateKey;
  }

  /**
  * @return the userId of the current user.
  */
  public synchronized Long getMyUserId() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getMyUserId()");
    Long myUId = myUserId;
    if (trace != null) trace.exit(FetchedDataCache.class, myUId);
    return myUId;
  }

  /**
  * @return the UserSettingsRecord of the current user.
  */
  public synchronized UserSettingsRecord getMyUserSettingsRecord() {
    return myUserSettingsRecord;
  }

  /**
  * @return the PassRecoveryRecord of the current user.
  */
  public synchronized PassRecoveryRecord getMyPassRecoveryRecord() {
    return myPassRecoveryRecord;
  }

  /**
  * @return number of FolderRecords in the cache
  */
  public synchronized int countFolderRecords() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "countFolderRecords()");
    int numOfFolders = folderRecordMap.size();
    if (trace != null) trace.exit(FetchedDataCache.class, numOfFolders);
    return numOfFolders;
  }

  /**
  * Add email address record hash to keep track of already requested ones.
  */
  public synchronized void addRequestedAddrHash(byte[] hash) {
    // Store the hash as string so when comparing different instances of the same data will match.
    requestedAddrSet.add(ArrayUtils.toString(hash));
  }

  /**
  * Add email address record hashes in batch mode.
  */
  public synchronized void addRequestedAddrHashes(List hashesL) {
    for (int i=0; i<hashesL.size(); i++)
      addRequestedAddrHash((byte[]) hashesL.get(i));
  }

  /**
  * @return true if hash exists in the requested cashe.
  */
  public synchronized boolean wasRequestedAddrHash(byte[] hash) {
    return requestedAddrSet.contains(ArrayUtils.toString(hash));
  }

  /*********************************
  ***   UserRecord operations   ***
  *********************************/

  /**
  * Adds new records or record updates into the cache and fires appropriate event.
  */
  public void addUserRecords(UserRecord[] userRecords) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addUserRecords(UserRecord[])");
    if (trace != null) trace.args(userRecords);

    if (userRecords != null && userRecords.length > 0) {
      // Un-seal my user record among other user records -- only when my User Record has not been un-sealed before.
      // At first login, there won't be any myUserRecord yet, when a keyRecord comes, we will unseal it then.
      UserRecord myUserRecord = getUserRecord();
      if (myUserRecord != null && myUserRecord.getSymKeyFldShares() == null && userRecords != null) {
        for (int i=0; i<userRecords.length; i++) {
          UserRecord uRec = userRecords[i];
          // only unwrap my user record
          if (uRec.userId.equals(myUserRecord.userId) && uRec.pubKeyId != null) {
            KeyRecord kRec = getKeyRecord(uRec.pubKeyId);
            if (kRec != null && kRec.getPrivateKey() != null) {
              uRec.unSeal(kRec);
            }
          }
        }
      }

      synchronized (this) {
        userRecords = (UserRecord[]) RecordUtils.merge(userRecordMap, userRecords);
      }

      // invalidate cached values for folders because rendering might have changed...
      FolderRecord[] fRecs = getFolderRecords();
      for (int i=0; i<fRecs.length; i++) {
        fRecs[i].invalidateCachedValues();
      }

      fireUserRecordUpdated(userRecords, RecordEvent.SET);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Removes records from the cache and fires appropriate event.
  */
  public void removeUserRecords(UserRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeUserRecords(UserRecord[])");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      synchronized (this) {
        records = (UserRecord[]) RecordUtils.remove(userRecordMap, records);
      }
      // fire removal of users
      fireUserRecordUpdated(records, RecordEvent.REMOVE);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Sets the logged-in user record and adds it into the cache.  Cannot change the userId during the same session.
  */
  public void setUserRecord(UserRecord userRecord) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "setUserRecord(UserRecord)");
    if (trace != null) trace.args(userRecord);

    synchronized (this) {
      if (myUserId == null)
        myUserId = userRecord.getId();
      else if (!userRecord.getId().equals(myUserId))
        throw new IllegalStateException("UserRecord already initialized with different id.");
    }

    addUserRecords(new UserRecord[] { userRecord });
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Sets the logged-in user settings record.  Cannot change the userId during the same session.
  */
  public void setUserSettingsRecord(UserSettingsRecord userSettingsRecord) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "setUserSettingsRecord(UserSettingsRecord)");
    if (trace != null) trace.args(userSettingsRecord);

    synchronized (this) {
      if (myUserId == null)
        throw new IllegalStateException("UserRecord should be initialized first.");
      else if (!userSettingsRecord.userId.equals(myUserId))
        throw new IllegalStateException("UserRecord already initialized with different id.");
    }

    KeyRecord kRec = getKeyRecord(userSettingsRecord.pubKeyId);
    if (kRec != null && kRec.getPrivateKey() != null) {
      StringBuffer errBuffer = new StringBuffer();
      userSettingsRecord.unSeal(kRec, errBuffer);
      if (errBuffer.length() > 0)
        NotificationCenter.show(NotificationCenter.ERROR_MESSAGE, "Invalid Settings", errBuffer.toString());
    }

    synchronized (this) {
      if (myUserSettingsRecord != null)
        myUserSettingsRecord.merge(userSettingsRecord);
      else
        myUserSettingsRecord = userSettingsRecord;
    }

    fireUserSettingsRecordUpdated(new UserSettingsRecord[] { myUserSettingsRecord }, RecordEvent.SET);

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Sets the password recovery record.
  */
  public synchronized void setPassRecoveryRecord(PassRecoveryRecord passRecoveryRecord) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "setPassRecoveryRecord(PassRecoveryRecord)");
    if (trace != null) trace.args(passRecoveryRecord);

    if (myPassRecoveryRecord != null && passRecoveryRecord != null)
      myPassRecoveryRecord.merge(passRecoveryRecord);
    else
      myPassRecoveryRecord = passRecoveryRecord;

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * @return stored User Record
  */
  public synchronized UserRecord getUserRecord() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getUserRecord()");
    UserRecord uRec = null;
    if (myUserId != null)
      uRec = getUserRecord(myUserId);
    if (trace != null) trace.exit(FetchedDataCache.class, uRec);
    return uRec;
  }

  public synchronized UserRecord getUserRecord(Long userId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getUserRecord(Long userId)");
    UserRecord uRec = (UserRecord) userRecordMap.get(userId);
    if (trace != null) trace.exit(FetchedDataCache.class, uRec);
    return uRec;
  }

  /**
  * @return all user records stored in the cache
  */
  public synchronized UserRecord[] getUserRecords() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getUserRecords()");

    UserRecord[] users = (UserRecord[]) ArrayUtils.toArray(userRecordMap.values(), UserRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, users);
    return users;
  }

  /**
  * @return all users matching specified user IDs
  */
  public synchronized UserRecord[] getUserRecords(Long[] userIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getUserRecords(Long[] userIDs)");
    if (trace != null) trace.args(userIDs);

    ArrayList usersL = new ArrayList();
    if (userIDs != null) {
      for (int i=0; i<userIDs.length; i++) {
        UserRecord uRec = (UserRecord) userRecordMap.get(userIDs[i]);
        if (uRec != null)
          usersL.add(uRec);
      }
    }
    UserRecord[] users = (UserRecord[]) ArrayUtils.toArray(usersL, UserRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, users);
    return users;
  }

  /***********************************
  ***   FolderRecord operations   ***
  ***********************************/

  /**
  * Adds new records or record updates into the cache and fires appropriate event.
  */
  public void addFolderRecords(FolderRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addFolderRecords(FolderRecord[])");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      // convert old chatting into new explicit type
      for (int i=0; i<records.length; i++) {
        if (records[i].folderType.shortValue() == FolderRecord.POSTING_FOLDER && records[i].isChatting())
          records[i].folderType = new Short(FolderRecord.CHATTING_FOLDER);
      }
      synchronized (this) {
        records = (FolderRecord[]) RecordUtils.merge(folderRecordMap, records);
      }
      fireFolderRecordUpdated(records, RecordEvent.SET);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Removes records from the cache and fires appropriate event.
  */
  public void removeFolderRecords(FolderRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeFolderRecords(FolderRecord[])");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      // gather all folders that will be removed.
      FolderRecord[] allDescendingFolders = getFoldersAllDescending(records);
      FolderRecord[] allToRemove = (FolderRecord[]) ArrayUtils.concatinate(records, allDescendingFolders);
      synchronized (this) {
        records = (FolderRecord[]) RecordUtils.remove(folderRecordMap, allToRemove);
      }
      // We have removed all specified folders AND all their descendants
      fireFolderRecordUpdated(records, RecordEvent.REMOVE);

      // remove all shares that belong to those folders
      FolderShareRecord[] removingShares = (FolderShareRecord[]) getFolderShareRecordsForFolders(RecordUtils.getIDs(allToRemove));
      removeFolderShareRecords(removingShares);
      //removeFoldersAndChildrenFolderRecords(records);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * @return all FolderRecords from cache.
  */
  public synchronized FolderRecord[] getFolderRecords() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderRecords()");
    FolderRecord[] allFolders = (FolderRecord[]) ArrayUtils.toArray(folderRecordMap.values(), FolderRecord.class);
    if (trace != null) trace.exit(FetchedDataCache.class, allFolders);
    return allFolders;
  }

  /**
  * @return all FolderRecords from cache with specified IDs
  */
  public synchronized FolderRecord[] getFolderRecords(Long[] folderIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderRecords(Long[] folderIDs)");
    if (trace != null) trace.args(folderIDs);

    ArrayList fRecsL = new ArrayList();
    if (folderIDs != null) {
      for (int i=0; i<folderIDs.length; i++) {
        if (folderIDs[i] != null) {
          FolderRecord fRec = (FolderRecord) folderRecordMap.get(folderIDs[i]);
          if (fRec != null)
            fRecsL.add(fRec);
        }
      }
    }
    FolderRecord[] folders = (FolderRecord[]) ArrayUtils.toArray(fRecsL, FolderRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, folders);
    return folders;
  }

  /**
  * @return all FolderRecords from cache that pass through specified filter
  */
  public synchronized FolderRecord[] getFolderRecords(RecordFilter filter) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderRecords(RecordFilter filter)");
    if (trace != null) trace.args(filter);

    ArrayList fRecsL = new ArrayList();
    Collection col = folderRecordMap.values();
    Iterator iter = col.iterator();
    while (iter.hasNext()) {
      FolderRecord folderRecord = (FolderRecord) iter.next();
      // if this is someone else's folder
      if (filter.keep(folderRecord)) {
        fRecsL.add(folderRecord);
      }
    }
    FolderRecord[] folders = (FolderRecord[]) ArrayUtils.toArray(fRecsL, FolderRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, folders);
    return folders;
  }

  /**
  * @return all FolderRecords from cache which current user is NOT the owner of.
  */
  public synchronized FolderRecord[] getFolderRecordsNotMine() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderRecordsNotMine()");

    ArrayList fRecsL = new ArrayList();
    Collection col = folderRecordMap.values();
    Iterator iter = col.iterator();
    while (iter.hasNext()) {
      FolderRecord folderRecord = (FolderRecord) iter.next();
      // if this is someone else's folder
      if (!folderRecord.ownerUserId.equals(myUserId)) {
        fRecsL.add(folderRecord);
      }
    }
    FolderRecord[] folders = (FolderRecord[]) ArrayUtils.toArray(fRecsL, FolderRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, folders);
    return folders;
  }


  /**
  * @return all FolderRecords from cache which match the criteria.
  */
  public synchronized FolderRecord[] getFolderRecordsForUser(Long userId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderRecordsForUser(Long userId)");

    ArrayList fRecsL = new ArrayList();
    Collection col = folderRecordMap.values();
    Iterator iter = col.iterator();
    while (iter.hasNext()) {
      FolderRecord folderRecord = (FolderRecord) iter.next();
      if (folderRecord.ownerUserId.equals(userId)) {
        fRecsL.add(folderRecord);
      }
    }
    FolderRecord[] folders = (FolderRecord[]) ArrayUtils.toArray(fRecsL, FolderRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, folders);
    return folders;
  }

  /**
  * @return all FolderRecords from cache which match the criteria.
  */
  public synchronized FolderRecord[] getFolderRecordsForUsers(Long[] userIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderRecordsForUsers(Long[] userIDs)");

    // user ID lookup table
    HashSet uIDsHS = new HashSet();
    uIDsHS.addAll(Arrays.asList(userIDs));
    // gathered folders
    HashSet fRecsHS = new HashSet();
    Iterator iter = folderRecordMap.values().iterator();
    while (iter.hasNext()) {
      FolderRecord folderRecord = (FolderRecord) iter.next();
      if (uIDsHS.contains(folderRecord.ownerUserId)) {
        if (!fRecsHS.contains(folderRecord)) {
          fRecsHS.add(folderRecord);
        }
      }
    }
    FolderRecord[] folders = (FolderRecord[]) ArrayUtils.toArray(fRecsHS, FolderRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, folders);
    return folders;
  }


  /**
  * @return all chatting FolderRecords from cache
  */
  public synchronized FolderRecord[] getFolderRecordsChatting() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderRecordsChatting()");

    ArrayList fRecsL = new ArrayList();
    Collection col = folderRecordMap.values();
    Iterator iter = col.iterator();
    while (iter.hasNext()) {
      FolderRecord folderRecord = (FolderRecord) iter.next();
      if (folderRecord.isChatting()) {
        fRecsL.add(folderRecord);
      }
    }
    FolderRecord[] folders = (FolderRecord[]) ArrayUtils.toArray(fRecsL, FolderRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, folders);
    return folders;
  }

  /**
  * Not traced for performance.
  * @return a FolderRecord from cache with a given id.
  */
  public synchronized FolderRecord getFolderRecord(Long folderId) {
    return folderId != null ? (FolderRecord) folderRecordMap.get(folderId) : null;
  }

  /**
  * Finds all folder records that carry the specified parentFolderId.
  * @return all children of the parent specified.
  */
  public synchronized FolderRecord[] getFoldersChildren(Long parentFolderId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFoldersChildren(Long parentFolderId)");
    if (trace != null) trace.args(parentFolderId);

    ArrayList childrenL = new ArrayList();
    Collection col = folderRecordMap.values();
    Iterator iter = col.iterator();
    while (iter.hasNext()) {
      FolderRecord folderRecord = (FolderRecord) iter.next();
      if (folderRecord.isChildToParent(parentFolderId)) {
        childrenL.add(folderRecord);
      }
    }
    FolderRecord[] folders = (FolderRecord[]) ArrayUtils.toArray(childrenL, FolderRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, folders);
    return folders;
  }

  /**
  * @return all children of the parents specified.
  */
  public synchronized FolderRecord[] getFoldersChildren(FolderRecord[] parentRecords) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFoldersChildren(FolderRecord[] parentRecords)");
    if (trace != null) trace.args(parentRecords);

    // load set with specified parents
    HashSet parentIDsHS = new HashSet(parentRecords.length);
    for (int i=0; i<parentRecords.length; i++) {
      parentIDsHS.add(parentRecords[i].folderId);
    }
    ArrayList childrenL = new ArrayList();
    Collection col = folderRecordMap.values();
    Iterator iter = col.iterator();
    while (iter.hasNext()) {
      FolderRecord folderRecord = (FolderRecord) iter.next();
      // if this is a child at all
      if (!folderRecord.parentFolderId.equals(folderRecord.folderId)) {
        // if this is one of the wanted children (quick lookup)
        if (parentIDsHS.contains(folderRecord.parentFolderId))
          childrenL.add(folderRecord);
      }
    }
    FolderRecord[] folders = (FolderRecord[]) ArrayUtils.toArray(childrenL, FolderRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, folders);
    return folders;
  }

  /**
  * @param parentFolders
  * @return children in the FolderRecord hierarchy, ignores view hierarchy
  */
  public synchronized FolderRecord[] getFoldersAllDescending(FolderRecord[] parentFolders) {
    HashSet allDescendantsHS = new HashSet();
    addFoldersAllChildren(allDescendantsHS, parentFolders);
    FolderRecord[] result = (FolderRecord[]) ArrayUtils.toArray(allDescendantsHS, FolderRecord.class);
    return result;
  }
  private synchronized void addFoldersAllChildren(Collection allDescendants, FolderRecord[] folders) {
    if (folders != null && folders.length >= 0) {
      FolderRecord[] childFolders = getFoldersChildren(folders);
      if (childFolders != null && childFolders.length > 0) {
        ArrayList realChildrenL = new ArrayList();
        for (int i=0; i<childFolders.length; i++) {
          if (!allDescendants.contains(childFolders[i])) {
            allDescendants.add(childFolders[i]);
            realChildrenL.add(childFolders[i]);
          }
        }
        FolderRecord[] realChildren = (FolderRecord[]) ArrayUtils.toArray(realChildrenL, FolderRecord.class);
        addFoldersAllChildren(allDescendants, realChildren);
      }
    }
  }

  /**
  * @return table of all Group Folders to which specified user belongs (recursively too)
  */
  public synchronized Long[] getFolderGroupIDsMy() {
    return getFolderGroupIDs(getMyUserId());
  }
  public synchronized Long[] getFolderGroupIDs(Long userId) {
    Set groupIDsSet = getFolderGroupIDsSet(userId);
    Long[] groupIDs = (Long[]) ArrayUtils.toArray(groupIDsSet, Long.class);
    return groupIDs;
  }
  public synchronized Set getFolderGroupIDsMySet() {
    return myUserId != null ? getFolderGroupIDsSet(myUserId) : null;
  }
  public synchronized Set getFolderGroupIDsSet(Long userId) {
    Set groupIDsSet = new HashSet();
    Collection sharesForUserV = folderShareRecordMap_byOwnerId.getAll(userId);
    if (sharesForUserV != null) {
      Iterator iter = sharesForUserV.iterator();
      while (iter.hasNext()) {
        FolderShareRecord sRec = (FolderShareRecord) iter.next();
        if (sRec.isOwnedBy(userId, (Long[]) null)) { // check if owner is USER type
          FolderRecord fRec = getFolderRecord(sRec.folderId);
          if (fRec != null && fRec.isGroupType())
            groupIDsSet.add(fRec.folderId);
        }
      }
    }
    if (groupIDsSet.size() > 0)
      addFolderGroupIDs(groupIDsSet);
    return groupIDsSet;
  }
  private synchronized void addFolderGroupIDs(Set groupIDsSet) {
    boolean anyAdded = false;
    Collection col = folderShareRecordMap.values();
    Iterator iter = col.iterator();
    while (iter.hasNext()) {
      FolderShareRecord sRec = (FolderShareRecord) iter.next();
      if (sRec.isOwnedByGroup() && groupIDsSet.contains(sRec.ownerUserId)) {
        FolderRecord fRec = getFolderRecord(sRec.folderId);
        if (fRec != null && fRec.isGroupType()) {
          if (!groupIDsSet.contains(fRec.folderId)) {
            groupIDsSet.add(fRec.folderId);
            anyAdded = true;
          }
        }
      }
    }
    if (anyAdded)
      addFolderGroupIDs(groupIDsSet);
  }

  /**
  * Clears all Folder Records from the cache no events are fired.
  */
  private synchronized void clearFolderRecords() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "clearFolderRecords()");
    folderRecordMap.clear();
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void clearFolderPairRecords() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "clearFolderPairRecords()");
    folderRecordMap.clear();
    folderShareRecordMap.clear();
    folderShareRecordMap_byFldId.clear();
    folderShareRecordMap_byOwnerId.clear();
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void clearFolderFetchedIDs() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "clearFolderFetchedIDs()");
    fldIDsFetchRequestsIssuedSet.clear();
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /****************************************
  ***   FolderShareRecord operations   ***
  ****************************************/

  /**
  * Adds new records or record updates into the cache, unseals them and fires appropriate event.
  */
  public void addFolderShareRecords(FolderShareRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addFolderShareRecords(FolderShareRecord[])");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      Long myUId = getMyUserId();
      Set groupIDsSet = getFolderGroupIDsSet(myUId);
      HashSet sharesChangedHS = unWrapFolderShareRecords(records, myUId, groupIDsSet);

      synchronized (this) {
        records = (FolderShareRecord[]) RecordUtils.merge(folderShareRecordMap, records);
        for (int i=0; i<records.length; i++) folderShareRecordMap_byFldId.put(records[i].folderId, records[i]);
        for (int i=0; i<records.length; i++) folderShareRecordMap_byOwnerId.put(records[i].ownerUserId, records[i]);
      }

      // use the changed shares instead of just the ones added
      FolderShareRecord[] sharesChanged = (FolderShareRecord[]) ArrayUtils.toArray(sharesChangedHS, FolderShareRecord.class);

      for (int i=0; i<sharesChanged.length; i++) {
        // Clear folder cached data if applicable.
        FolderShareRecord sRec = sharesChanged[i];
        if (sRec.isOwnedBy(myUId, groupIDsSet)) { // group changes
          FolderRecord fRec = getFolderRecord(sRec.folderId);
          if (fRec != null) {
            fRec.invalidateCachedValues();
          }
        }
      }

      fireFolderShareRecordUpdated(sharesChanged, RecordEvent.SET);

    } // end if records != null

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  private HashSet unWrapFolderShareRecords(final FolderShareRecord[] records, Long myUid, Set groupIDsSet) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "unWrapFolderShareRecords(FolderShareRecord[] records, Long myUid, Set groupIDsSet)");
    if (trace != null) trace.args(records, myUid, groupIDsSet);

    // New shares will be useful in finding unwrapping keys where we check cache and fall back on this additional sets.
    CallbackReturnI additionalSharesProvider = new CallbackReturnI() {
      private Object[] additionalSharesSet = null;
      public Object callback(Object value) {
        if (additionalSharesSet == null) {
          Map additionalShares = new HashMap();
          MultiHashMap additionalShares_byFldId = new MultiHashMap(true);
          for (int i=0; i<records.length; i++) {
            FolderShareRecord additionalShare = records[i];
            additionalShares.put(additionalShare.shareId, additionalShare);
            additionalShares_byFldId.put(additionalShare.folderId, additionalShare);
          }
          additionalSharesSet = new Object[] { additionalShares, additionalShares_byFldId };
        }
        return additionalSharesSet;
      }
    };

    // Tracking all changed shares for return
    HashSet sharesChanged = new HashSet();
    for (int i=0; i<records.length; i++) {
      sharesChanged.add(records[i]);
    }

    // gather all stored shares for potential unsealing;
    FolderShareRecord[] prevShares = getFolderShareRecords();

    // To unseal all the new shares, plus all previous that were not unsealed before
    ArrayList toUnsealL = new ArrayList(Arrays.asList(records));
    // Also add any previous shares that may need unsealing (nested encryption dependent on the new additions)
    if (prevShares != null) {
      for (int i=0; i<prevShares.length; i++) {
        FolderShareRecord prevShare = prevShares[i];
        if (!prevShare.isUnSealed() || prevShare.getSymmetricKey() == null || prevShare.getFolderName() == null)
          toUnsealL.add(prevShare);
      }
    }

    // Process all shares that need unsealing, new and previous...
    ArrayList exceptionL = new ArrayList();
    while (toUnsealL.size() > 0) {
      boolean anyUnsealed = false;
      for (int i=0; i<toUnsealL.size(); i++) {
        FolderShareRecord sRec = (FolderShareRecord) toUnsealL.get(i);
        if (sRec.getSymmetricKey() == null) {
          if (sRec.isOwnedBy(myUid, groupIDsSet)) { // group changes
            // local folder
            if (sRec.shareId.longValue() == FolderShareRecord.SHARE_LOCAL_ID) {
              sRec.setFolderName(FolderShareRecord.SHARE_LOCAL_NAME);
              sRec.setFolderDesc(FolderShareRecord.SHARE_LOCAL_DESC);
            } else if (sRec.shareId.longValue() == FolderShareRecord.CATEGORY_FILE_ID) {
              sRec.setFolderName(FolderShareRecord.CATEGORY_FILE_NAME);
              sRec.setFolderDesc(FolderShareRecord.CATEGORY_FILE_DESC);
            } else if (sRec.shareId.longValue() == FolderShareRecord.CATEGORY_MAIL_ID) {
              sRec.setFolderName(FolderShareRecord.CATEGORY_MAIL_NAME);
              sRec.setFolderDesc(FolderShareRecord.CATEGORY_MAIL_DESC);
            } else if (sRec.shareId.longValue() == FolderShareRecord.CATEGORY_CHAT_ID) {
              sRec.setFolderName(FolderShareRecord.CATEGORY_CHAT_NAME);
              sRec.setFolderDesc(FolderShareRecord.CATEGORY_CHAT_DESC);
            } else if (sRec.shareId.longValue() == FolderShareRecord.CATEGORY_GROUP_ID) {
              sRec.setFolderName(FolderShareRecord.CATEGORY_GROUP_NAME);
              sRec.setFolderDesc(FolderShareRecord.CATEGORY_GROUP_DESC);
            }
            try {
              if (attemptUnsealShare(sRec, exceptionL, groupIDsSet, additionalSharesProvider)) {
                anyUnsealed = true;
                if (!sharesChanged.contains(sRec))
                  sharesChanged.add(sRec);
              }
            } catch (Throwable t) {
              if (trace != null) trace.data(100, "Exception occured while attempting to unseal FolderShare", sRec);
              if (trace != null) trace.exception(FetchedDataCache.class, 101, t);
            }
          } else {
            // maybe this wasn't owned by me or so-far-known my groups... but maybe in next iteration a parent dependency will solve this one
            // Reverse the order for next iteration by inserting at the head, should help if odd case when a group has more than 1 share of its own.
            exceptionL.add(0, sRec);
          }
        }
      } // end for
      toUnsealL.clear();
      if (exceptionL.size() > 0 && anyUnsealed) {
        // another iteration
        toUnsealL.addAll(exceptionL);
      }
      exceptionL.clear();
    } // end while

    if (trace != null) trace.exit(FetchedDataCache.class, sharesChanged);
    return sharesChanged;
  }

  private boolean attemptUnsealShare(FolderShareRecord sRec, Collection exceptionList, Set groupIDsSet, CallbackReturnI additionalSharesProvider) {
    boolean wasUnsealed = false;
    // if there is anything to unseal
    if (sRec.getEncFolderName() != null) {
      // un-seal with user's global-folder symmetric key
      if (sRec.isOwnedByUser() && sRec.getPubKeyId() == null) {
        sRec.unSeal(getUserRecord().getSymKeyFldShares());
        wasUnsealed = true;
      }
      // un-seal with symmetric key of the group
      else if (sRec.isOwnedByGroup()) {
        FolderShareRecord myGroupShare = getFolderShareRecordMy(sRec.ownerUserId, groupIDsSet);
        BASymmetricKey symmetricKey = myGroupShare != null ? myGroupShare.getSymmetricKey() : null;
        if (symmetricKey == null && additionalSharesProvider != null) { //additionalShares != null && additionalShares_byFldId != null) {
          Object[] additionalSharesSet = (Object[]) additionalSharesProvider.callback(null);
          Map additionalShares = (Map) additionalSharesSet[0];
          MultiHashMap additionalShares_byFldId = (MultiHashMap) additionalSharesSet[1];
          myGroupShare = getFolderShareRecordMy(sRec.ownerUserId, groupIDsSet, myUserId, additionalShares, additionalShares_byFldId);
          symmetricKey = myGroupShare != null ? myGroupShare.getSymmetricKey() : null;
        }
        if (symmetricKey != null) {
          sRec.unSeal(symmetricKey);
          wasUnsealed = true;
        } else {
          exceptionList.add(sRec);
        }
      }
      // un-seal new folder share with private key
      else if (sRec.getPubKeyId() != null) {
        KeyRecord keyRec = getKeyRecord(sRec.getPubKeyId());
        if (keyRec != null) {
          sRec.unSeal(keyRec.getPrivateKey());
          wasUnsealed = true;
        } else {
          exceptionList.add(sRec);
        }
      }
    } // end if anything to unseal
    return wasUnsealed;
  }

  /**
  * Removes records from the cache and fires appropriate event.
  */
  public void removeFolderShareRecords(FolderShareRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeFolderShareRecords(FolderShareRecord[])");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      synchronized (this) {
        records = (FolderShareRecord[]) RecordUtils.remove(folderShareRecordMap, records);
        for (int i=0; i<records.length; i++) folderShareRecordMap_byFldId.remove(records[i].folderId, records[i]);
        for (int i=0; i<records.length; i++) folderShareRecordMap_byOwnerId.remove(records[i].ownerUserId, records[i]);
      }

      // removing shares sometimes causes folder tree description changes or table heading description changes
      for (int i=0; i<records.length; i++) {
        // Clear folder cached data if applicable.
        FolderShareRecord sRec = records[i];
        FolderRecord fRec = getFolderRecord(sRec.folderId);
        if (fRec != null) {
          fRec.invalidateCachedValues();
        }
      }

      fireFolderShareRecordUpdated(records, RecordEvent.REMOVE);

      // Convert our removed shares to folder records, so we can remove them from the listeners.
      Long userId = getMyUserId();
      ArrayList fRecsToRemoveL = new ArrayList();
      for (int i=0; i<records.length; i++) {
        FolderShareRecord share = (FolderShareRecord) records[i];
        if (share.isOwnedBy(userId, (Long[]) null)) {
          FolderRecord fRec = getFolderRecord(share.folderId);
          if (fRec != null && !fRecsToRemoveL.contains(fRec))
            fRecsToRemoveL.add(fRec);
        }
      }
      if (fRecsToRemoveL.size() > 0) {
        FolderRecord[] fRecsToRemove = (FolderRecord[]) ArrayUtils.toArray(fRecsToRemoveL, FolderRecord.class);
        removeFolderRecords(fRecsToRemove);
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeFolderShareRecords(Long[] shareIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeFolderShareRecords(Long[] shareIDs)");
    if (trace != null) trace.args(shareIDs);

    FolderShareRecord[] shares = getFolderShareRecords(shareIDs);
    if (shares != null && shares.length > 0) {
      removeFolderShareRecords(shares);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * @return a FolderShareRecord from cache with a given id.
  */
  public synchronized FolderShareRecord getFolderShareRecord(Long shareId) {
    return (FolderShareRecord) folderShareRecordMap.get(shareId);
  }


  /**
  * @return all FolderShareRecords from cache.
  */
  public synchronized FolderShareRecord[] getFolderShareRecords() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderShareRecords()");

    FolderShareRecord[] shareRecords = (FolderShareRecord[]) ArrayUtils.toArray(folderShareRecordMap.values(), FolderShareRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, shareRecords);
    return shareRecords;
  }

  /**
  * @return specified FolderShareRecords from cache.
  */
  public synchronized FolderShareRecord[] getFolderShareRecords(Long[] shareIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderShareRecords(Long[] shareIDs)");

    ArrayList sharesL = new ArrayList();
    if (shareIDs != null) {
      for (int i=0; i<shareIDs.length; i++) {
        if (shareIDs[i] != null) {
          FolderShareRecord share = (FolderShareRecord) folderShareRecordMap.get(shareIDs[i]);
          if (share != null)
            sharesL.add(share);
        }
      }
    }
    FolderShareRecord[] shareRecords = (FolderShareRecord[]) ArrayUtils.toArray(sharesL, FolderShareRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, shareRecords);
    return shareRecords;
  }

  /**
  * @return a FolderShareRecord from cache for a given folderId and current user.
  */
  private FolderShareRecord getFolderShareRecordMy(Long folderId) {
    return getFolderShareRecordMy(folderId, myUserId, folderShareRecordMap_byFldId);
  }
  private static FolderShareRecord getFolderShareRecordMy(Long folderId, Long myUserId, MultiHashMap shares_byFldId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderShareRecordMy(Long folderId)");
    if (trace != null) trace.args(folderId);

    FolderShareRecord shareRec = null;
    Collection sharesV = shares_byFldId.getAll(folderId);
    if (sharesV != null) {
      Iterator iter = sharesV.iterator();
      while (iter.hasNext()) {
        FolderShareRecord shareRecord = (FolderShareRecord) iter.next();
        if (shareRecord.isOwnedBy(myUserId, (Long[]) null)) {
          shareRec = shareRecord;
          break;
        }
      }
    }
    if (trace != null) trace.exit(FetchedDataCache.class, shareRec);
    return shareRec;
  }

  public synchronized FolderShareRecord getFolderShareRecordMy(Long folderId, boolean includeGroupOwned) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderShareRecordMy(Long folderId, boolean includeGroupOwned)");
    if (trace != null) trace.args(folderId);
    if (trace != null) trace.args(includeGroupOwned);

    // first try to find my own share of this folder...
    FolderShareRecord shareRec = getFolderShareRecordMy(folderId);
    if (shareRec == null && includeGroupOwned) {
      Set groupIDsSet = getFolderGroupIDsSet(myUserId);
      shareRec = getFolderShareRecordGroupOwnded(folderId, groupIDsSet);
    }

    if (trace != null) trace.exit(FetchedDataCache.class, shareRec);
    return shareRec;
  }
  public synchronized FolderShareRecord getFolderShareRecordMy(Long folderId, Set groupIDsSet) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderShareRecordMy(Long folderId, Set groupIDsSet)");
    if (trace != null) trace.args(folderId);
    if (trace != null) trace.args(groupIDsSet);

    FolderShareRecord shareRec = getFolderShareRecordMy(folderId);
    if (shareRec == null && groupIDsSet != null) {
      shareRec = getFolderShareRecordGroupOwnded(folderId, groupIDsSet);
    }

    if (trace != null) trace.exit(FetchedDataCache.class, shareRec);
    return shareRec;
  }
  private static FolderShareRecord getFolderShareRecordMy(Long folderId, Set groupIDsSet, Long myUserId, Map shares, MultiHashMap shares_byFldId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderShareRecordMy(Long folderId, Set groupIDsSet, Long myUserId, Map shares, MultiHashMap shares_byFldId)");
    if (trace != null) trace.args(folderId, groupIDsSet, myUserId, shares, shares_byFldId);

    FolderShareRecord shareRec = getFolderShareRecordMy(folderId, myUserId, shares_byFldId);
    if (shareRec == null && groupIDsSet != null) {
      shareRec = getFolderShareRecordGroupOwnded(folderId, groupIDsSet, shares);
    }

    if (trace != null) trace.exit(FetchedDataCache.class, shareRec);
    return shareRec;
  }
  private FolderShareRecord getFolderShareRecordGroupOwnded(Long folderId, Set groupIDsSet) {
    return getFolderShareRecordGroupOwnded(folderId, groupIDsSet, folderShareRecordMap);
  }
  private static FolderShareRecord getFolderShareRecordGroupOwnded(Long folderId, Set groupIDsSet, Map shares) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderShareRecordGroupOwnded(Long folderId, Set groupIDsSet, Map shares)");
    if (trace != null) trace.args(folderId);
    if (trace != null) trace.args(groupIDsSet); // if present - include folder shares through group memberships
    if (trace != null) trace.args(shares);

    FolderShareRecord shareRec = null;
    if (groupIDsSet != null && groupIDsSet.size() > 0) {
      Collection col = shares.values();
      Iterator iter = col.iterator();
      while (iter.hasNext()) {
        FolderShareRecord shareRecord = (FolderShareRecord) iter.next();
        if (shareRecord.folderId.equals(folderId) && shareRecord.isOwnedBy(null, groupIDsSet)) {
          shareRec = shareRecord;
          break;
        }
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class, shareRec);
    return shareRec;
  }

  /**
  * @return a FolderShareRecords from cache for a given folderId and current user.
  */
  public synchronized FolderShareRecord[] getFolderShareRecordsMy(Long folderId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderShareRecordsMy(Long folderId)");
    if (trace != null) trace.args(folderId);

    Set groupIDsSet = getFolderGroupIDsSet(myUserId);
    FolderShareRecord[] shareRecs = getFolderShareRecordsMy(folderId, groupIDsSet);

    if (trace != null) trace.exit(FetchedDataCache.class, shareRecs);
    return shareRecs;
  }
  public synchronized FolderShareRecord[] getFolderShareRecordsMy(Long folderId, Set groupIDsSet) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderShareRecordMy(Long folderId, Set groupIDsSet)");
    if (trace != null) trace.args(folderId);
    if (trace != null) trace.args(groupIDsSet); // if present - include folder shares through group memberships

    ArrayList shareRecsL = new ArrayList();
    Collection col = folderShareRecordMap.values();
    Iterator iter = col.iterator();
    while (iter.hasNext()) {
      FolderShareRecord shareRecord = (FolderShareRecord) iter.next();
      if (shareRecord.folderId.equals(folderId) && shareRecord.isOwnedBy(myUserId, groupIDsSet)) { // group changes
        shareRecsL.add(shareRecord);
      }
    }
    FolderShareRecord[] shareRecs = (FolderShareRecord[]) ArrayUtils.toArray(shareRecsL, FolderShareRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, shareRecs);
    return shareRecs;
  }

  /**
  * @return all shares (only 1 per folder) that belong to current user.
  */
  public synchronized FolderShareRecord[] getFolderSharesMy(boolean includeGroupOwned) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderSharesMy(boolean includeGroupOwned)");
    if (trace != null) trace.args(includeGroupOwned);

    HashMap folderIDsHM = new HashMap();
    if (includeGroupOwned) {
      Set groupIDsSet = getFolderGroupIDsSet(myUserId);
      Collection col = folderShareRecordMap.values();
      Iterator iter = col.iterator();
      while (iter.hasNext()) {
        FolderShareRecord shareRecord = (FolderShareRecord) iter.next();
        if (shareRecord.isOwnedBy(myUserId, groupIDsSet)) {
          // if user owned (not by group) then replace already existing group one, if not stored yet - store it
          if (shareRecord.isOwnedByUser() || folderIDsHM.get(shareRecord.folderId) == null)
            folderIDsHM.put(shareRecord.folderId, shareRecord);
        }
      }
    } else {
      // do not include group owned, use different Map for shortcut
      Collection sharesV = folderShareRecordMap_byOwnerId.getAll(myUserId);
      if (sharesV != null) {
        Iterator iter = sharesV.iterator();
        while (iter.hasNext()) {
          FolderShareRecord shareRecord = (FolderShareRecord) iter.next();
          if (shareRecord.isOwnedBy(myUserId, (Long[]) null))
            folderIDsHM.put(shareRecord.folderId, shareRecord);
        }
      }
    }
    FolderShareRecord[] shareRecs = (FolderShareRecord[]) ArrayUtils.toArray(folderIDsHM.values(), FolderShareRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, shareRecs);
    return shareRecs;
  }

  /**
  * @return all of my folder shares that belong to specified folders.
  */
  public synchronized FolderShareRecord[] getFolderSharesMyForFolders(Long[] folderIDs, boolean includeGroupOwned) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderSharesMyForFolders(Long[] folderIDs, boolean includeGroupOwned)");
    if (trace != null) trace.args(folderIDs);
    if (trace != null) trace.args(includeGroupOwned);

    FolderShareRecord[] shareRecords = null;
    if (folderIDs != null && folderIDs.length > 0) {
      // load group memberships
      Set groupIDsSet = null;
      if (includeGroupOwned) groupIDsSet = getFolderGroupIDsSet(myUserId);
      shareRecords = getFolderSharesMyForFolders(folderIDs, groupIDsSet);
    }

    if (trace != null) trace.exit(FetchedDataCache.class, shareRecords);
    return shareRecords;
  }

  /**
  * @return all of my folder shares that belong to specified folders.
  */
  public synchronized FolderShareRecord[] getFolderSharesMyForFolders(Long[] folderIDs, Set groupIDsSet) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderSharesMyForFolders(Long[] folderIDs, Set groupIDsSet)");
    if (trace != null) trace.args(folderIDs);
    if (trace != null) trace.args(groupIDsSet);

    FolderShareRecord[] shareRecords = null;
    if (folderIDs != null && folderIDs.length > 0) {
      // load lookup with wanted folder IDs
      HashSet folderIDsHS = new HashSet(Arrays.asList(folderIDs));
      // go through all shares and see if we want them
      HashMap folderIDsHM = new HashMap();
      Iterator iter = folderShareRecordMap.values().iterator();
      while (iter.hasNext()) {
        FolderShareRecord shareRecord = (FolderShareRecord) iter.next();
        if (folderIDsHS.contains(shareRecord.folderId)) {
          if (shareRecord.isOwnedBy(myUserId, groupIDsSet)) { // group changes
            if (shareRecord.isOwnedByUser() || !folderIDsHM.containsKey(shareRecord.folderId)) {
              folderIDsHM.put(shareRecord.folderId, shareRecord);
            }
          }
        }
      }
      shareRecords = (FolderShareRecord[]) ArrayUtils.toArray(folderIDsHM.values(), FolderShareRecord.class);
    }

    if (trace != null) trace.exit(FetchedDataCache.class, shareRecords);
    return shareRecords;
  }


  public synchronized FolderShareRecord[] getFolderShareRecordsMyRootsForMsgs(MsgLinkRecord[] msgLinks, boolean includeGroupOwned) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderShareRecordsMyRootsForMsgs(MsgLinkRecord[] msgLinks, boolean includeGroupOwned)");
    if (trace != null) trace.args(msgLinks);

    MsgLinkRecord[] childMsgs = msgLinks;
    FolderShareRecord[] parentShares = null;
    Set groupIDsSet = null;
    while (true) {
      MsgLinkRecord[] parentLinks = null;
      FolderShareRecord[] parentShareRecs = null;
      Long[] ownerIDs = MsgLinkRecord.getOwnerObjIDs(childMsgs, Record.RECORD_TYPE_MESSAGE);
      if (trace != null) trace.data(10, "find all msg links for msg IDs", ownerIDs);
      if (ownerIDs != null && ownerIDs.length > 0) {
        parentLinks = getMsgLinkRecordsForMsgs(ownerIDs);
      }
      ownerIDs = MsgLinkRecord.getOwnerObjIDs(childMsgs, Record.RECORD_TYPE_FOLDER);
      if (trace != null) trace.data(20, "find share records for folder IDs", ownerIDs);
      if (ownerIDs != null && ownerIDs.length > 0) {
        if (includeGroupOwned && groupIDsSet == null) groupIDsSet = getFolderGroupIDsSet(myUserId);
        parentShareRecs = getFolderSharesMyForFolders(ownerIDs, groupIDsSet);
        if (parentShareRecs != null && parentShareRecs.length > 0) {
          parentShares = (FolderShareRecord[]) ArrayUtils.concatinate(parentShares, parentShareRecs);
          parentShares = (FolderShareRecord[]) ArrayUtils.removeDuplicates(parentShares);
        }
      }
      // recursively make the fetched parents to be children so we fetch their parents until we hit the roots.
      if (parentLinks != null && parentLinks.length > 0) {
        childMsgs = parentLinks;
      } else {
        break;
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class, parentShares);
    return parentShares;
  }


  /**
  * @return FolderShareRecords from cache for a given folderId.
  */
  public synchronized FolderShareRecord[] getFolderShareRecordsForFolder(Long folderId) {

    Collection sharesV = folderShareRecordMap_byFldId.getAll(folderId);
    FolderShareRecord[] shareRecords = (FolderShareRecord[]) ArrayUtils.toArray(sharesV, FolderShareRecord.class);

    return shareRecords;
  }

  /**
  * @return all children of the parents specified.
  */
  public synchronized FolderShareRecord[] getFolderShareRecordsForFolders(Long[] folderIDs) {
    ArrayList sharesL = new ArrayList();
    folderIDs = (Long[]) ArrayUtils.removeDuplicates(folderIDs, Long.class);
    for (int i=0; i<folderIDs.length; i++) {
      Collection sharesForFolderV = folderShareRecordMap_byFldId.getAll(folderIDs[i]);
      if (sharesForFolderV != null) {
        sharesL.addAll(sharesForFolderV);
      }
    }
    FolderShareRecord[] shareRecords = (FolderShareRecord[]) ArrayUtils.toArray(sharesL, FolderShareRecord.class);
    return shareRecords;
  }

  /**
  * @return FolderShareRecords from cache for a given userId EXCLUDING shares accessed through group memberships
  */
  public synchronized FolderShareRecord[] getFolderShareRecordsForUsers(Long[] userIDs) {
    ArrayList sharesL = new ArrayList();
    userIDs = (Long[]) ArrayUtils.removeDuplicates(userIDs, Long.class);
    for (int i=0; i<userIDs.length; i++) {
      Collection sharesForOwnerV = folderShareRecordMap_byOwnerId.getAll(userIDs[i]);
      if (sharesForOwnerV != null) {
        Iterator iter = sharesForOwnerV.iterator();
        while (iter.hasNext()) {
          FolderShareRecord shareRecord = (FolderShareRecord) iter.next();
          if (shareRecord.isOwnedByUser())
            sharesL.add(shareRecord);
        }
      }
    }
    FolderShareRecord[] shareRecords = (FolderShareRecord[]) ArrayUtils.toArray(sharesL, FolderShareRecord.class);
    return shareRecords;
  }

  /**
  * Finds all folder pairs that are descendants in the VIEW tree to specified parents.
  * @return all descendant children of the view parents specified.
  */
  public synchronized FolderPair[] getFolderPairsViewAllDescending(FolderPair[] parentFolderPairs, boolean includeParents) {
    HashSet allDescendantsHS = new HashSet();
    Set groupIDsSet = getFolderGroupIDsSet(myUserId);
    addFolderPairsViewAllDescending(allDescendantsHS, parentFolderPairs, groupIDsSet);
    // include or exclude parents, sometimes a folder is its own parent in the view, so do this anyway
    for (int i=0; i<parentFolderPairs.length; i++) {
      if (allDescendantsHS.contains(parentFolderPairs[i]) != includeParents) {
        if (includeParents)
          allDescendantsHS.add(parentFolderPairs[i]);
        else
          allDescendantsHS.remove(parentFolderPairs[i]);
      }
    }
    // pack the result into an array
    FolderPair[] result = (FolderPair[]) ArrayUtils.toArray(allDescendantsHS, FolderPair.class);
    return result;
  }
  private synchronized void addFolderPairsViewAllDescending(Collection allDescendants, FolderPair[] folderPairs, Set groupIDsSet) {
    if (folderPairs != null && folderPairs.length >= 0) {
      FolderPair[] childPairs = getFolderPairsViewChildren(folderPairs, groupIDsSet);
      if (childPairs != null && childPairs.length > 0) {
        ArrayList realChildrenL = new ArrayList();
        for (int i=0; i<childPairs.length; i++) {
          if (!allDescendants.contains(childPairs[i])) {
            allDescendants.add(childPairs[i]);
            realChildrenL.add(childPairs[i]);
          }
        }
        FolderPair[] realChildren = (FolderPair[]) ArrayUtils.toArray(realChildrenL, FolderPair.class);
        addFolderPairsViewAllDescending(allDescendants, realChildren, groupIDsSet);
      }
    }
  }

  /**
  * Finds all folder pairs that are children in the VIEW tree to specified parent.
  * @return all children of the view parent specified.
  */
  public synchronized FolderPair[] getFolderPairsViewChildren(Long parentFolderId, boolean includeGroupOwned) {
    // exceptional case is when looking for children of Category folder, in that case allow root folders to match
    if (parentFolderId.longValue() < 0)
      return getFolderPairs(new FolderFilter(null, null, parentFolderId, null), includeGroupOwned);
    else
      return getFolderPairs(new FolderFilter(null, null, parentFolderId, Boolean.FALSE), includeGroupOwned);
  }
  public synchronized FolderPair[] getFolderPairsViewChildren(FolderPair[] parentFolderPairs, boolean includeGroupOwned) {
    return getFolderPairs(new FolderFilter(RecordUtils.getIDs(parentFolderPairs)), includeGroupOwned);
  }
  public synchronized FolderPair[] getFolderPairsViewChildren(FolderPair[] parentFolderPairs, Set groupIDsSet) {
    return getFolderPairs(new FolderFilter(RecordUtils.getIDs(parentFolderPairs)), groupIDsSet);
  }
  /**
  * @return all of My accessible Posting Folder Shares (for ie: message recipients) or other types
  */
  public synchronized FolderPair[] getFolderPairsMyOfType(short folderType, boolean includeGroupOwned) {
    return getFolderPairs(new FolderFilter(folderType), includeGroupOwned);
  }
  /**
  * @param filter is typically FolderFilter instance type.
  * @return all of My Folder Shares that pass through the specified filter.
  */
  public synchronized FolderPair[] getFolderPairs(RecordFilter filter, boolean includeGroupOwned) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderPairs(RecordFilter filter, boolean includeGroupOwned)");
    if (trace != null) trace.args(filter);
    if (trace != null) trace.args(includeGroupOwned);
    Set groupIDsSet = null;
    if (includeGroupOwned) {
      if (myUserId != null)
        groupIDsSet = getFolderGroupIDsSet(myUserId);
    }
    FolderPair[] folderPairs = getFolderPairs(filter, groupIDsSet);
    if (trace != null) trace.exit(FetchedDataCache.class, folderPairs);
    return folderPairs;
  }
  public synchronized FolderPair[] getFolderPairs(RecordFilter filter, Set groupIDsSet) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFolderPairs(RecordFilter filter, Set groupIDsSet)");
    if (trace != null) trace.args(filter);
    if (trace != null) trace.args(groupIDsSet);
    HashMap folderPairsHM = new HashMap();
    Collection col = folderShareRecordMap.values();
    Iterator iter = col.iterator();
    while (iter.hasNext()) {
      FolderShareRecord shareRecord = (FolderShareRecord) iter.next();
      // if this is one of the wanted shares
      if (shareRecord.isOwnedBy(myUserId, groupIDsSet)) { // group changes required
        FolderRecord folderRecord = getFolderRecord(shareRecord.folderId);
        if (folderRecord != null) {
          FolderPair fPair = new FolderPair(shareRecord, folderRecord);
          if (filter.keep(fPair)) {
            if (shareRecord.isOwnedByUser() || !folderPairsHM.containsKey(shareRecord.folderId)) {
              folderPairsHM.put(shareRecord.folderId, fPair);
            }
          }
        }
      }
    }
    FolderPair[] folderPairs = (FolderPair[]) ArrayUtils.toArray(folderPairsHM.values(), FolderPair.class);
    if (trace != null) trace.exit(FetchedDataCache.class, folderPairs);
    return folderPairs;
  }


  /**
  * Clears all Folder Share Records from the cache no events are fired.
  */
  private synchronized void clearFolderShareRecords() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "clearFolderShareRecords()");
    folderShareRecordMap.clear();
    folderShareRecordMap_byFldId.clear();
    folderShareRecordMap_byOwnerId.clear();
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /*************************************
  ***   FileLinkRecord operations   ***
  *************************************/

  /**
  * Adds new records or record updates into the cache and fires appropriate event.
  */
  public void addFileLinkRecords(FileLinkRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addFileLinkRecords(FileLinkRecord[])");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      // expensive unsealing outside of synchronized
      unWrapFileLinkRecords(records);
      synchronized (this) {
        records = (FileLinkRecord[]) RecordUtils.merge(fileLinkRecordMap, records);
        for (int i=0; i<records.length; i++) fileLinkRecordMap_byFileId.put(records[i].fileId, records[i]);
      } // end synchronized
      // for any attachments, clear parent message rendering cache
      for (int i=0; i<records.length; i++) {
        FileLinkRecord link = records[i];
        if (link.ownerObjType.shortValue() == Record.RECORD_TYPE_MESSAGE) {
          MsgLinkRecord[] msgParentLinks = getMsgLinkRecordsForMsg(link.ownerObjId);
          if (msgParentLinks != null && msgParentLinks.length > 0) {
            // clear parent message rendering cache as it might need to be updated with new attachments
            MsgLinkRecord.clearPostRenderingCache(msgParentLinks);
            // re-inject the parent message into cache to trigger listener updates
            addMsgLinkRecords(msgParentLinks);
          }
        }
      }
      fireFileLinkRecordUpdated(records, RecordEvent.SET);

      // recalculate flags in the involved folders
      statUpdatesInFoldersForVisualNotification(records, false, false);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  private void unWrapFileLinkRecords(FileLinkRecord[] linkRecords) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "unWrapFileLinkRecords(FileLinkRecord[] linkRecords)");
    if (trace != null) trace.args(linkRecords);
    Set groupIDsSet = null;
    // un-seal the records
    for (int i=0; i<linkRecords.length; i++) {
      FileLinkRecord fLink = linkRecords[i];
      try {
        BASymmetricKey unsealingKey = null;
        short ownerObjType = fLink.ownerObjType.shortValue();

        // find the symmetric key from the owner object's record.
        switch (ownerObjType) {
          case Record.RECORD_TYPE_FOLDER:
            if (groupIDsSet == null) groupIDsSet = getFolderGroupIDsSet(myUserId);
            FolderShareRecord shareRecord = getFolderShareRecordMy(fLink.ownerObjId, groupIDsSet);
            // Share Record may be null if this file was moved from a shared folder to a private on
            // not accessible to us, but some other user.
            if (shareRecord != null) {
              unsealingKey = shareRecord.getSymmetricKey();
            }
            break;
          case Record.RECORD_TYPE_MESSAGE:
            // any password protected message has the key
            MsgDataRecord msgDataRecord = getMsgDataRecord(fLink.ownerObjId);
            MsgLinkRecord[] msgLinkRecords = null;
            if (msgDataRecord.bodyPassHash != null) {
              unsealingKey = msgDataRecord.getSymmetricBodyKey();
            } else {
              // any message link has a symmetric key for the message data and attached files
              msgLinkRecords = getMsgLinkRecordsForMsg(fLink.ownerObjId);
              if (msgLinkRecords != null && msgLinkRecords.length > 0)
                unsealingKey = msgLinkRecords[0].getSymmetricKey();
            }
            break;
        }
        if (unsealingKey != null) {
          fLink.unSeal(unsealingKey);
        }
      } catch (Throwable t) {
        if (trace != null) trace.data(100, "Exception occured while processing file link", fLink);
        if (trace != null) trace.exception(FetchedDataCache.class, 101, t);
      }
    } // end for
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Removes records from the cache and fires appropriate event.
  */
  public void removeFileLinkRecords(FileLinkRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeFileLinkRecords(FileLinkRecord[])");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      synchronized (this) {
        records = (FileLinkRecord[]) RecordUtils.remove(fileLinkRecordMap, records);
        if (records != null) {
          // for removed links remove them from secondary hashtable
          for (int i=0; i<records.length; i++) fileLinkRecordMap_byFileId.remove(records[i].fileId, records[i]);
        }
      }
      fireFileLinkRecordUpdated(records, RecordEvent.REMOVE);

      // recalculate flags in the involved folders
      statUpdatesInFoldersForVisualNotification(records, true, true);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Removes records from the cache and fires appropriate event.
  */
  public synchronized void removeFileLinkRecords(Long[] fileLinkIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeFileLinkRecords(Long[] fileLinkIDs)");
    if (trace != null) trace.args(fileLinkIDs);

    FileLinkRecord[] fileLinks = getFileLinkRecords(fileLinkIDs);
    if (fileLinks != null && fileLinks.length > 0) {
      removeFileLinkRecords(fileLinks);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * @return a FileLinkRecord from cache with a given id.
  */
  public synchronized FileLinkRecord getFileLinkRecord(Long fileLinkId) {
    return (FileLinkRecord) fileLinkRecordMap.get(fileLinkId);
  }

  /**
  * @return all FileLinkRecords from cache
  */
  public synchronized FileLinkRecord[] getFileLinkRecords() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFileLinkRecords()");

    FileLinkRecord[] fileLinks = (FileLinkRecord[]) ArrayUtils.toArray(fileLinkRecordMap.values(), FileLinkRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, fileLinks);
    return fileLinks;
  }

  /**
  * @return all File Link Records for specified file link ids.
  * The records found in the cache are returned, the IDs which do not have
  * corresponding records in the cache are ignored.
  */
  public synchronized FileLinkRecord[] getFileLinkRecords(Long[] fileLinkIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFileLinkRecords(Long[] fileLinkIDs)");
    if (trace != null) trace.args(fileLinkIDs);

    ArrayList fileLinksL = new ArrayList();
    if (fileLinkIDs != null) {
      for (int i=0; i<fileLinkIDs.length; i++) {
        FileLinkRecord fileLink = (FileLinkRecord) fileLinkRecordMap.get(fileLinkIDs[i]);
        if (fileLink != null)
          fileLinksL.add(fileLink);
      }
    }
    FileLinkRecord[] fileLinks = (FileLinkRecord[]) ArrayUtils.toArray(fileLinksL, FileLinkRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, fileLinks);
    return fileLinks;
  }


  /**
  * @return a collection of FileLinkRecords for specified shareId.
  */
  public synchronized FileLinkRecord[] getFileLinkRecords(Long shareId) {
    // find the corresponding folderId
    Long folderId = getFolderShareRecord(shareId).folderId;
    return getFileLinkRecordsOwnerAndType(folderId, new Short(Record.RECORD_TYPE_FOLDER));
  }

  /**
  * @return all cached links for a given fileId
  */
  public synchronized FileLinkRecord[] getFileLinkRecordsForFile(Long fileId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFileLinkRecordsForFile(Long fileId)");
    if (trace != null) trace.args(fileId);

    Collection linksL = fileLinkRecordMap_byFileId.getAll(fileId);
    FileLinkRecord[] links = (FileLinkRecord[]) ArrayUtils.toArray(linksL, FileLinkRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, links);
    return links;
  }

  /**
  * Filters the map to collect FileLinkRecords for a given ownerId and ownerType.
  * @return a collection of FileLinkRecords for specified ownerId and ownerType.
  */
  public synchronized FileLinkRecord[] getFileLinkRecordsOwnerAndType(Long ownerId, Short ownerType) {
    ArrayList fileLinksL = new ArrayList();
    // Collect all file links for this folder
    Iterator iter = fileLinkRecordMap.values().iterator();
    while (iter.hasNext()) {
      FileLinkRecord fLink = (FileLinkRecord) iter.next();
      if (fLink.ownerObjId.equals(ownerId) && fLink.ownerObjType.equals(ownerType))
        fileLinksL.add(fLink);
    }
    FileLinkRecord[] fileLinks = (FileLinkRecord[]) ArrayUtils.toArray(fileLinksL, FileLinkRecord.class);
    return fileLinks;
  }


  /**
  * Filters the map to collect FileLinkRecords for a given ownerIDs and ownerType.
  * @return a collection of FileLinkRecords for specified ownerId and ownerType.
  */
  public synchronized FileLinkRecord[] getFileLinkRecordsOwnersAndType(Long[] ownerIDs, Short ownerType) {
    ArrayList fileLinksL = new ArrayList();
    // load a lookup with wanted ownerIDs
    HashSet ownerIDsHS = new HashSet(Arrays.asList(ownerIDs));
    // Collect all file links for this folder
    Iterator iter = fileLinkRecordMap.values().iterator();
    while (iter.hasNext()) {
      FileLinkRecord fLink = (FileLinkRecord) iter.next();
      if (fLink.ownerObjType.equals(ownerType) && ownerIDsHS.contains(fLink.ownerObjId))
        fileLinksL.add(fLink);
    }
    FileLinkRecord[] fileLinks = (FileLinkRecord[]) ArrayUtils.toArray(fileLinksL, FileLinkRecord.class);
    return fileLinks;
  }


  /*************************************
  ***   FileDataRecord operations   ***
  *************************************/

  /**
  * Adds new records or record updates into the cache and fires appropriate event.
  */
  public void addFileDataRecords(FileDataRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addFileDataRecords(FileDataRecord[])");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      synchronized (this) {
        records = (FileDataRecord[]) RecordUtils.merge(fileDataRecordMap, records);
      }
      //fireFileDataRecordUpdated(records, RecordEvent.SET);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized FileDataRecord getFileDataRecord(Long fileId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getFileDataRecord(Long fileId)");
    if (trace != null) trace.args(fileId);

    FileDataRecord fRec = null;
    if (fileId != null)
      fRec = (FileDataRecord) fileDataRecordMap.get(fileId);

    if (trace != null) trace.exit(FetchedDataCache.class, fRec);
    return fRec;
  }


  /***********************************
  ***   InvEmlRecord operations   ***
  ***********************************/

  /**
  * Adds new records or record updates into the cache and fires appropriate event.
  */
  public void addInvEmlRecords(InvEmlRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addInvEmlRecords(InvEmlRecord[] invEmlRecords)");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      synchronized (this) {
        records = (InvEmlRecord[]) RecordUtils.merge(invEmlRecordMap, records);
      }
      fireInvEmlRecordUpdated(records, RecordEvent.SET);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Removes records from the cache and fires appropriate event.
  */
  public void removeInvEmlRecords(InvEmlRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeInvEmlRecords(InvEmlRecord[])");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      synchronized (this) {
        records = (InvEmlRecord[]) RecordUtils.remove(invEmlRecordMap, records);
      }
      fireInvEmlRecordUpdated(records, RecordEvent.REMOVE);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * @return ALL InvEmlRecords stored in the cache.
  */
  public synchronized InvEmlRecord[] getInvEmlRecords() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getInvEmlRecords()");

    InvEmlRecord[] invEmls = (InvEmlRecord[]) ArrayUtils.toArray(invEmlRecordMap.values(), InvEmlRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, invEmls);
    return invEmls;
  }

  /**
  * @return all InvEmlRecords matching specified IDs
  */
  public synchronized InvEmlRecord[] getInvEmlRecords(Long[] IDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getInvEmlRecords(Long[] IDs)");
    if (trace != null) trace.args(IDs);

    ArrayList recsL = new ArrayList();
    if (IDs != null) {
      for (int i=0; i<IDs.length; i++) {
        InvEmlRecord rec = (InvEmlRecord) invEmlRecordMap.get(IDs[i]);
        if (rec != null)
          recsL.add(rec);
      }
    }
    InvEmlRecord[] recs = (InvEmlRecord[]) ArrayUtils.toArray(recsL, InvEmlRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, recs);
    return recs;
  }

  /********************************
  ***   KeyRecord operations   ***
  ********************************/

  /**
  * Adds new records or record updates into the cache and fires appropriate event.
  */
  public void addKeyRecords(KeyRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addKeyRecords(KeyRecord[])");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {

      // expensive unsealing outside of synchronized
      boolean isUserSettingsChange = false;
      StringBuffer errBuffer = new StringBuffer();
      isUserSettingsChange = unWrapKeyRecords(records, errBuffer);

      synchronized (this) {
        records = (KeyRecord[]) RecordUtils.merge(keyRecordMap, records);
      }

      // notify if user settings was just unsealed
      if (isUserSettingsChange) {
        fireUserSettingsRecordUpdated(new UserSettingsRecord[] { myUserSettingsRecord }, RecordEvent.SET);
      }

      if (errBuffer.length() > 0)
        NotificationCenter.show(NotificationCenter.ERROR_MESSAGE, "Invalid Settings", errBuffer.toString());

      fireKeyRecordUpdated(records, RecordEvent.SET);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  private boolean unWrapKeyRecords(KeyRecord[] records, StringBuffer errBuffer) {
    boolean isUserSettingsChange = false;
    for (int i=0; i<records.length; i++) {
      if (records[i].ownerUserId.equals(myUserId)) {
        // in case this is my key and doesn't have the encrypted private key, try to fetch it from properties
        BASymCipherBlock ba = records[i].getEncPrivateKey();
        if (ba == null) {
          String propertyKey = "Enc"+RSAPrivateKey.OBJECT_NAME+"_"+records[i].keyId;
          GlobalSubProperties keyProperties = new GlobalSubProperties(GlobalSubProperties.PROPERTY_EXTENSION_KEYS);
          String property = keyProperties.getProperty(propertyKey);

          if (property != null && property.length() > 0) {
            ba = new BASymCipherBlock(ArrayUtils.toByteArray(property));
            records[i].setEncPrivateKey(ba);
          }
        }
        records[i].unSeal(getEncodedPassword());
        // Now that we have the key, if my userRecord is not yet decrypted, decrypt it now.
        UserRecord myUserRecord = getUserRecord();
        if (myUserRecord != null &&
            records[i].keyId.equals(myUserRecord.pubKeyId) &&
            records[i].getPrivateKey() != null &&
            myUserRecord.getSymKeyFldShares() == null)
        {
          myUserRecord.unSeal(records[i]);
        }
        // Also decrypt the user settings if not yet decrypted
        if (myUserSettingsRecord != null &&
            myUserSettingsRecord.getXmlText() == null &&
            myUserSettingsRecord.pubKeyId.equals(records[i].keyId))
        {
          myUserSettingsRecord.unSeal(records[i], errBuffer);
          isUserSettingsChange = true;
        }
      }
    }
    return isUserSettingsChange;
  }

  /**
  * Removes records from the cache and fires appropriate event.
  */
  public void removeKeyRecords(KeyRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeKeyRecords(KeyRecord[])");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      synchronized (this) {
        records = (KeyRecord[]) RecordUtils.remove(keyRecordMap, records);
      }
      fireKeyRecordUpdated(records, RecordEvent.REMOVE);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * @return a KeyRecord from cache with a given id.
  */
  public synchronized KeyRecord getKeyRecord(Long keyId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getKeyRecord(Long)");
    if (trace != null) trace.args(keyId);

    KeyRecord kRec = null;
    if (keyId != null)
      kRec = (KeyRecord) keyRecordMap.get(keyId);

    if (trace != null) trace.exit(FetchedDataCache.class, kRec);
    return kRec;
  }

  /**
  * @return ALL KeyRecords stored in the cache.
  */
  public synchronized KeyRecord[] getKeyRecords() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getKeyRecords()");

    KeyRecord[] keys = (KeyRecord[]) ArrayUtils.toArray(keyRecordMap.values(), KeyRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, keys);
    return keys;
  }

  /**
  * @return the current KeyRecord (last key record created by the user -- user.currentKeyId keyId).
  */
  public synchronized KeyRecord getKeyRecordMyCurrent() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getCurrentKeyRecord()");

    KeyRecord keyRecord = getKeyRecord(getUserRecord().currentKeyId);

    if (trace != null) trace.exit(FetchedDataCache.class, keyRecord);
    return keyRecord;
  }

  /**
  * @return most recent KeyRecord for a given user
  */
  public synchronized KeyRecord getKeyRecordForUser(Long userId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getKeyRecordForUser(Long userId)");
    if (trace != null) trace.args(userId);

    KeyRecord keyRecord = null;
    Iterator iter = keyRecordMap.values().iterator();
    while (iter.hasNext()) {
      KeyRecord kRec = (KeyRecord) iter.next();
      if (kRec.ownerUserId.equals(userId)) {
        if (keyRecord == null || kRec.keyId.longValue() > keyRecord.keyId.longValue())
          keyRecord = kRec;
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class, keyRecord);
    return keyRecord;
  }




  /************************************
  ***   ContactRecord operations   ***
  ************************************/

  /**
  * Adds new records or record updates into the cache and fires appropriate event.
  */
  public void addContactRecords(ContactRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addContactRecords(ContactRecord[])");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      // expensive unsealing outside of synchronized
      unWrapContactRecords(records);
      boolean anyNew = false;
      synchronized (this) {
        // see if we got new records, or updates only
        for (int i=0; i<records.length; i++) {
          if (!contactRecordMap.containsKey(records[i].contactId)) {
            anyNew = true;
            break;
          }
        }
        records = (ContactRecord[]) RecordUtils.merge(contactRecordMap, records);
      }
      if (anyNew) {
        fireContactRecordUpdated(records, RecordEvent.SET);
      } else {
        fireContactRecordUpdated_Delayed(records, RecordEvent.SET);
      }

      // Gather any folders that may use the contact name and dispatch event because rendering may need to change ... skip my own shares ...
      Long[] contactUserIDs = ContactRecord.getInvolvedUserIDs(records);
      contactUserIDs = (Long[]) ArrayUtils.getDifference(contactUserIDs, new Long[] { myUserId });
      FolderShareRecord[] involvedShares = getFolderShareRecordsForUsers(contactUserIDs);
      FolderRecord[] involvedFolders = getFolderRecords(FolderShareRecord.getFolderIDs(involvedShares));
      for (int i=0; i<involvedFolders.length; i++) {
        involvedFolders[i].invalidateCachedValues();
      }

      if (anyNew) {
        fireFolderRecordUpdated(involvedFolders, RecordEvent.SET);
      } else {
        fireFolderRecordUpdated_Delayed(involvedFolders, RecordEvent.SET);
      }

      // After notification is done, make previous status equal current status,
      // so that late unnecessary notification are not triggered when managing contacts.
      final ContactRecord[] recs = records;
      Thread th = new ThreadTraced("Contact Status delayed setter") {
        public void runTraced() {
          try { Thread.sleep(3000); } catch (Throwable t) { }
          for (int i=0; recs!=null && i<recs.length; i++) {
            if (recs[i] != null)
              recs[i].previousStatus = recs[i].status;
          }
        }
      };
      th.setDaemon(true);
      th.start();
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Removes records from the cache and fires appropriate event.
  */
  public void removeContactRecords(ContactRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeContactRecords(ContactRecord[])");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      synchronized (this) {
        records = (ContactRecord[]) RecordUtils.remove(contactRecordMap, records);
      }
      fireContactRecordUpdated(records, RecordEvent.REMOVE);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeContactRecords(Long[] contactIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeContactRecords(Long[] contactIDs)");
    if (trace != null) trace.args(contactIDs);

    ContactRecord[] contacts = getContactRecords(contactIDs);
    if (contacts != null && contacts.length > 0) {
      removeContactRecords(contacts);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  private void unWrapContactRecords(ContactRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "unWrapContactRecords(ContactRecord[] records)");
    if (trace != null) trace.args(records);

    UserRecord myUserRecord = null;
    Set groupIDsSet = null;
    // Unwrap contact records.
    for (int i=0; i<records.length; i++) {
      ContactRecord cRec = records[i];
      try {
        // If status change to ONLINE, play sound.
        if (cRec.status != null && ContactRecord.isOnlineStatus(cRec.status)) {
          ContactRecord oldRec = getContactRecord(cRec.contactId);
          if (oldRec != null &&
                  !ContactRecord.isOnlineStatus(oldRec.status) &&
                  myUserId.equals(oldRec.ownerUserId) &&
                  (oldRec.permits.intValue() & ContactRecord.SETTING_DISABLE_AUDIBLE_STATUS_NOTIFY) == 0
                  )
          {
            Sounds.playAsynchronous(Sounds.ONLINE);
          }
        }

        if (groupIDsSet == null) groupIDsSet = getFolderGroupIDsSet(myUserId);
        FolderShareRecord shareRecord = getFolderShareRecordMy(cRec.folderId, groupIDsSet);
        // unSeal only OWNER part
        if (cRec.ownerUserId != null && cRec.ownerUserId.equals(myUserId)) {
          // OWNER part
          if (cRec.getEncOwnerNote() != null) {
            if (cRec.getEncOwnerNote().size() > 0) {
              if (shareRecord != null)
                cRec.unSeal(shareRecord.getSymmetricKey());
            } else {
              // If owner's note is blank, use default handle or make from user id.
              UserRecord uRec = getUserRecord(cRec.contactWithId);
              if (uRec != null)
                cRec.setOwnerNote(uRec.handle);
              else
                cRec.setOwnerNote("User (" + cRec.contactWithId + ")");
            }
          }
        }
        // unSeal only OTHER part
        else if (cRec.contactWithId != null && cRec.contactWithId.equals(myUserId)) {
          // OTHER part
          if (cRec.getEncOtherNote() != null) {
            if (cRec.getEncOtherNote().size() > 0) {
              Long keyId = cRec.getOtherKeyId();
              if (keyId != null) {
                KeyRecord otherKeyRec = getKeyRecord(keyId);
                if (otherKeyRec != null) {
                  cRec.unSeal(otherKeyRec);
                }
              } else {
                // if no keyId then this record must have already been recrypted
                if (myUserRecord == null) myUserRecord = getUserRecord();
                BASymmetricKey ba = myUserRecord.getSymKeyCntNotes();
                if (ba != null) cRec.unSealRecrypted(ba);
              }
            } else {
              // If other note is blank, use default handle or make from user id.
              UserRecord uRec = getUserRecord(cRec.ownerUserId);
              if (uRec != null)
                cRec.setOtherNote(uRec.handle);
              else
                cRec.setOtherNote("User (" + cRec.contactWithId + ")");
            }
          }
        } // end unSeal OTHER part
      } catch (Throwable t) {
        if (trace != null) trace.data(100, "Exception occured while processing contact", cRec);
        if (trace != null) trace.exception(FetchedDataCache.class, 101, t);
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  } // end unWrapContactRecords()

  /**
  * Clears all Contact Records from the cache no events are fired.
  */
  public synchronized void clearContactRecords() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "clearContactRecords()");

    contactRecordMap.clear();

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * @return a ContactRecord from cache with a given id.
  */
  public synchronized ContactRecord getContactRecord(Long contactId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getContactRecord(Long)");
    if (trace != null) trace.args(contactId);

    ContactRecord cRec = (ContactRecord) contactRecordMap.get(contactId);

    if (trace != null) trace.exit(FetchedDataCache.class, cRec);
    return cRec;
  }

  /**
  * @return all Contact Records.
  */
  public synchronized ContactRecord[] getContactRecords() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getContactRecords()");

    ContactRecord[] contacts = (ContactRecord[]) ArrayUtils.toArray(contactRecordMap.values(), ContactRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, contacts);
    return contacts;
  }

  public synchronized ContactRecord[] getContactRecords(Long[] contactIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getContactRecords(Long[] contactIDs)");
    if (trace != null) trace.args(contactIDs);

    ArrayList contactsL = new ArrayList();
    if (contactIDs != null) {
      for (int i=0; i<contactIDs.length; i++) {
        ContactRecord contact = (ContactRecord) contactRecordMap.get(contactIDs[i]);
        if (contact != null)
          contactsL.add(contact);
      }
    }
    ContactRecord[] contacts = (ContactRecord[]) ArrayUtils.toArray(contactsL, ContactRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, contacts);
    return contacts;
  }

  /**
  * @return all My Active Contact Records.
  */
  public synchronized ContactRecord[] getContactRecords(RecordFilter filter) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getContactRecords(RecordFilter filter)");

    Iterator iter = contactRecordMap.values().iterator();
    ArrayList contactsL = new ArrayList();
    while (iter.hasNext()) {
      ContactRecord cRec = (ContactRecord) iter.next();
      if (filter.keep(cRec))
        contactsL.add(cRec);
    }
    ContactRecord[] contacts = (ContactRecord[]) ArrayUtils.toArray(contactsL, ContactRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, contacts);
    return contacts;
  }

  /**
  * @return all My Active Contact Records.
  */
  public synchronized ContactRecord[] getContactRecordsMyActive(boolean includeInitiated) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getContactRecordsMyActive(boolean includeInitiated)");

    Iterator iter = contactRecordMap.values().iterator();
    ArrayList contactsL = new ArrayList();
    while (iter.hasNext()) {
      ContactRecord cRec = (ContactRecord) iter.next();
      if (cRec.ownerUserId.equals(myUserId)) {
        if (cRec.isOfActiveTypeAnyState() || (includeInitiated && cRec.isOfInitiatedType())) {
          contactsL.add(cRec);
        }
      }
    }
    ContactRecord[] contacts = (ContactRecord[]) ArrayUtils.toArray(contactsL, ContactRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, contacts);
    return contacts;
  }

  /**
  * @return contact matching search criteria.
  */
  public synchronized ContactRecord getContactRecordOwnerWith(Long ownerUserId, Long withUserId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getContactRecordOwnerWith(Long ownerUserId, Long withUserId)");

    ContactRecord contactRecord = null;
    Iterator iter = contactRecordMap.values().iterator();
    while (iter.hasNext()) {
      ContactRecord cRec = (ContactRecord) iter.next();
      if (cRec.ownerUserId.equals(ownerUserId) && cRec.contactWithId.equals(withUserId)) {
        contactRecord = cRec;
        break;
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class, contactRecord);
    return contactRecord;
  }


  /**
  * @return records matching search criteria.
  */
  public synchronized ContactRecord[] getContactRecordsForUsers(Long[] userIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getContactRecordsForUsers(Long[] userIDs)");

    HashSet userIDsHS = new HashSet(Arrays.asList(userIDs));
    HashSet recordsHS = new HashSet();
    Iterator iter = contactRecordMap.values().iterator();
    while (iter.hasNext()) {
      ContactRecord cRec = (ContactRecord) iter.next();
      if (userIDsHS.contains(cRec.ownerUserId) || userIDsHS.contains(cRec.contactWithId)) {
        if (!recordsHS.contains(cRec)) {
          recordsHS.add(cRec);
        }
      }
    }
    ContactRecord[] records = (ContactRecord[]) ArrayUtils.toArray(recordsHS, ContactRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, records);
    return records;
  }


  /************************************
  ***   MsgLinkRecord operations   ***
  ************************************/

  /**
  * Adds new records or record updates into the cache and fires appropriate event.
  */
  public void addMsgLinkRecords(MsgLinkRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addMsgLinkRecords(MsgLinkRecord[] records)");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      // expensive unsealing outside of synchronized
      unWrapMsgLinkRecords(records);
      synchronized (this) {
        records = (MsgLinkRecord[]) RecordUtils.merge(msgLinkRecordMap, records);
        for (int i=0; i<records.length; i++) msgLinkRecordMap_byMsgId.put(records[i].msgId, records[i]);
      }
      // for any attachments, clear parent message rendering cache
      for (int i=0; i<records.length; i++) {
        MsgLinkRecord link = records[i];
        if (link.ownerObjType.shortValue() == Record.RECORD_TYPE_MESSAGE) {
          // avoid infinite loop if by mistake owner would be the same message
          if (!link.ownerObjId.equals(link.msgId)) {
            MsgLinkRecord[] msgParentLinks = getMsgLinkRecordsForMsg(link.ownerObjId);
            if (msgParentLinks != null && msgParentLinks.length > 0) {
              // clear parent message rendering cache as it might need to be updated with new attachments
              MsgLinkRecord.clearPostRenderingCache(msgParentLinks);
              // re-inject the parent message into cache to trigger listener updates
              addMsgLinkRecords(msgParentLinks);
            }
          }
        }
      }
      fireMsgLinkRecordUpdated(records, RecordEvent.SET);

      // recalculate flags in the involved folders
      statUpdatesInFoldersForVisualNotification(records, false, false);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }
  /**
  * Called ONLY when new records are added to cache in SET mode.
  */
  private void unWrapMsgLinkRecords(MsgLinkRecord[] linkRecords) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "unWrapMsgLinkRecords(MsgLinkRecord[] linkRecords)");
    if (trace != null) trace.args(linkRecords);
    Set groupIDsSet = null;
    for (int i=0; i<linkRecords.length; i++) {
      MsgLinkRecord link = linkRecords[i];
      try {
        if (link.getRecPubKeyId() != null) {
          KeyRecord kRec = getKeyRecord(link.getRecPubKeyId());
          if (kRec != null && kRec.getPrivateKey() != null)
            link.unSeal(kRec);
        } else if (link.ownerObjType != null && link.ownerObjId != null) {
          // When a message BODY is received, it does not have msg link and brief's fields -- ignore unSealing
          if (link.ownerObjType.shortValue() == Record.RECORD_TYPE_FOLDER) {
            if (groupIDsSet == null) groupIDsSet = getFolderGroupIDsSet(myUserId);
            FolderShareRecord sRec = getFolderShareRecordMy(link.ownerObjId, groupIDsSet);
            if (sRec != null) {
              if (sRec.getSymmetricKey() == null) {
                // Last desperate attempt to unwrap the parent folder, maybe we got it last minute
                // as a request due to the incomming link which had no prior known parent folder.
                ArrayList exceptionL = new ArrayList();
                attemptUnsealShare(sRec, exceptionL, groupIDsSet, null);
              }
              link.unSeal(sRec.getSymmetricKey());
            }
          } else if (link.ownerObjType.shortValue() == Record.RECORD_TYPE_MESSAGE) {
            // any password protected message has the key
            MsgDataRecord msgDataRecord = getMsgDataRecord(link.ownerObjId);
            MsgLinkRecord[] msgLinkRecords = null;
            if (msgDataRecord.bodyPassHash != null) {
              link.unSeal(msgDataRecord.getSymmetricBodyKey());
            } else {
              // any message link has a symmetric key for the message data and attached files
              msgLinkRecords = getMsgLinkRecordsForMsg(link.ownerObjId);
              if (msgLinkRecords != null && msgLinkRecords.length > 0)
                link.unSeal(msgLinkRecords[0].getSymmetricKey());
            }
          }
        }
      } catch (Throwable t) {
        if (trace != null) trace.data(100, "Exception occured while processing message link", link);
        if (trace != null) trace.exception(FetchedDataCache.class, 101, t);
      }
    } // end for

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Visually notify users that certain folders have modified content, update the
  * updates' count.
  * @param records can be instances of StatRecord, FileLinkRecords, MsgLinkRecords
  * @param allowLoweringOfUpdateCounts Operations like REMOVE can lower update count, but ADD should not
  */
  public void statUpdatesInFoldersForVisualNotification(Record[] records, boolean allowLoweringOfUpdateCounts, boolean suppressAudibleNotification) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "statUpdatesInFoldersForVisualNotification(Record[] records, boolean allowLoweringOfUpdateCounts, boolean suppressAudibleNotification)");
    if (trace != null) trace.args(records);
    if (trace != null) trace.args(suppressAudibleNotification);

    // Many cache queries are done in possibly not synchronized block so catch
    // any exceptions due to possible inconsistant cache states.
    try {
      // gather all folders that the stats are for, but first fetch the links...
      FileLinkRecord[] fileLinks = null;
      MsgLinkRecord[] msgLinks = null;
      FolderRecord[] folderRecs = null;

      if (records instanceof StatRecord[]) {
        StatRecord[] stats = (StatRecord[]) records;
        Long[] msgLinkIDs = StatRecord.getLinkIDs(stats, StatRecord.STAT_TYPE_MESSAGE);
        Long[] fileLinkIDs = StatRecord.getLinkIDs(stats, StatRecord.STAT_TYPE_FILE);
        fileLinks = getFileLinkRecords(fileLinkIDs);
        msgLinks = getMsgLinkRecords(msgLinkIDs);
        if (trace != null) trace.data(10, msgLinkIDs, fileLinkIDs);
        if (trace != null) trace.data(11, fileLinks);
        if (trace != null) trace.data(12, msgLinks);
      }
      else if (records instanceof FileLinkRecord[]) {
        fileLinks = (FileLinkRecord[]) records;
        if (trace != null) trace.data(13, fileLinks);
      }
      else if (records instanceof MsgLinkRecord[]) {
        msgLinks = (MsgLinkRecord[]) records;
        if (trace != null) trace.data(14, msgLinks);
      }
      else if (records instanceof FolderRecord[]) {
        folderRecs = (FolderRecord[]) records;
        if (trace != null) trace.data(15, folderRecs);
      }

      Long[] folderIDs = null;
      if (fileLinks != null && fileLinks.length > 0) {
        folderIDs = FileLinkRecord.getOwnerObjIDs(fileLinks, Record.RECORD_TYPE_FOLDER);
        if (trace != null) trace.data(16, folderIDs);
      }

      if (msgLinks != null && msgLinks.length > 0) {
        folderIDs = (Long[]) ArrayUtils.concatinate(folderIDs, MsgLinkRecord.getOwnerObjIDs(msgLinks, Record.RECORD_TYPE_FOLDER));
        if (trace != null) trace.data(17, folderIDs);
      }

      if (folderRecs != null && folderRecs.length > 0) {
        folderIDs = (Long[]) ArrayUtils.concatinate(folderIDs, RecordUtils.getIDs(folderRecs));
        if (trace != null) trace.data(18, folderIDs);
      }

      FolderRecord[] updatedFolders = getFolderRecords(folderIDs);
      if (trace != null) trace.data(19, updatedFolders);

      ArrayList modifiedFoldersL = new ArrayList();

      // Go through all folders and count their red flags from the cache.
      if (updatedFolders != null) {
        for (int i=0; i<updatedFolders.length; i++) {
          FolderRecord fRec = updatedFolders[i];
          Long fRecId = fRec.folderId;
          int statType = -1;
          if (fRec != null) {
            Long[] linkIDs = null;
            Record[] links = null;
            if (fRec.isFileType()) {
              links = getFileLinkRecordsOwnerAndType(fRecId, new Short(Record.RECORD_TYPE_FOLDER));
              statType = STAT_TYPE_INDEX_FILE;
              if (trace != null) trace.data(30, links);
            }
            else if (fRec.isMsgType()) {
              links = getMsgLinkRecordsOwnerAndType(fRecId, new Short(Record.RECORD_TYPE_FOLDER));
              statType = STAT_TYPE_INDEX_MESSAGE;
              if (trace != null) trace.data(31, links);
            }
            if (links != null && links.length > 0)
              linkIDs = RecordUtils.getIDs(links);

            int redFlagCount = 0;
            int newFlagCount = 0;
            if (linkIDs != null && linkIDs.length > 0) {
              // Gather Stats for each folder
              for (int k=0; k<linkIDs.length; k++) {
                StatRecord stat = getStatRecordMyLinkId(linkIDs[k], statType);
                if (stat != null) {
                  if (stat.isFlagRed())
                    redFlagCount ++;
                  if (stat.isFlagNew())
                    newFlagCount ++;
                }
              }
            }
            boolean suppressSound = suppressAudibleNotification
                    || newFlagCount == 0
                    || fRec.folderId.equals(getUserRecord().junkFolderId)
                    || fRec.folderId.equals(getUserRecord().recycleFolderId);
            // Skip reporting ZERO count (or lower counts) if folder was never fetched as cached updates maybe incomplete... 
            // Fallback on previous (maybe now incorrect) last server reported count.
            int oldCount = fRec.getUpdateCount();
            if (redFlagCount > oldCount || (redFlagCount < oldCount && wasFolderFetchRequestIssued(fRecId) && allowLoweringOfUpdateCounts)) {
              fRec.setUpdated(redFlagCount, suppressSound);
              modifiedFoldersL.add(fRec);
            }
          }
        } // end for

        // cause the folder listeners to be notified
        if (modifiedFoldersL.size() > 0)
          addFolderRecords((FolderRecord[]) ArrayUtils.toArray(modifiedFoldersL, FolderRecord.class));
      }
    } catch (Throwable t) {
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * We need data Records in the cache before the message table can display contents.
  * For that reason, the event will be fired when we are done with both, links and datas.
  * Adds new records or record updates into the cache and fires appropriate event.
  */
  public void addMsgLinkAndDataRecords(MsgLinkRecord linkRecord, MsgDataRecord dataRecord) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addMsgLinkAndDataRecords(MsgLinkRecord linkRecord, MsgDataRecord dataRecord)");
    addMsgLinkAndDataRecords(new MsgLinkRecord[] { linkRecord }, new MsgDataRecord[] { dataRecord });
    if (trace != null) trace.exit(FetchedDataCache.class);
  }
  public void addMsgLinkAndDataRecords(MsgLinkRecord[] linkRecords, MsgDataRecord[] dataRecords) {
    addMsgLinkAndDataRecords(linkRecords, dataRecords, false);
  }
  public void addMsgLinkAndDataRecords(MsgLinkRecord[] linkRecords, MsgDataRecord[] dataRecords, boolean suppressEventFireing) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addMsgLinkAndDataRecords(MsgLinkRecord[] linkRecords, MsgDataRecord[] dataRecords, boolean suppressEventFireing)");
    if (trace != null) trace.args(linkRecords, dataRecords);
    if (trace != null) trace.args(suppressEventFireing);

    MsgDataRecord[] affectedMsgDatas = null;
    // unseal the Msg Links and Msg Datas
    if (linkRecords != null && linkRecords.length > 0) {
      unWrapMsgLinkRecords(linkRecords);
    }
    if (dataRecords != null && dataRecords.length > 0) {
      unWrapMsgDataRecords(dataRecords, linkRecords);
    } else if (linkRecords != null && linkRecords.length > 0) {
      // since there are no Msg Datas specified, maybe we should try unWrapping any datas pointed by the link from the cache...
      // this would cover symmetric recrypt case of shared inboxes
      Long[] msgIDs = MsgLinkRecord.getMsgIDs(linkRecords);
      affectedMsgDatas = getMsgDataRecords(msgIDs);
      if (affectedMsgDatas != null && affectedMsgDatas.length > 0)
        unWrapMsgDataRecords(affectedMsgDatas, linkRecords);
    }

    synchronized (this) {
      // merge Msg Links and Msg Datas
      if (linkRecords != null && linkRecords.length > 0) {
        linkRecords = (MsgLinkRecord[]) RecordUtils.merge(msgLinkRecordMap, linkRecords);
        for (int i=0; i<linkRecords.length; i++) msgLinkRecordMap_byMsgId.put(linkRecords[i].msgId, linkRecords[i]);
      }
      if (dataRecords != null && dataRecords.length > 0) {
        dataRecords = (MsgDataRecord[]) RecordUtils.merge(msgDataRecordMap, dataRecords);
        for (int i=0; i<dataRecords.length; i++) msgDataRecordMap_byReplyToMsgId.put(dataRecords[i].replyToMsgId, dataRecords[i]);
      }
    }

    if (!suppressEventFireing) {
      if (linkRecords == null || linkRecords.length == 0) {
        if (dataRecords != null && dataRecords.length > 0) {
          MsgLinkRecord[] relatedLinks = getMsgLinkRecordsForMsgs(RecordUtils.getIDs(dataRecords));
          fireMsgLinkRecordUpdated(relatedLinks, RecordEvent.SET);
        }
      } else {
        fireMsgLinkRecordUpdated(linkRecords, RecordEvent.SET);
      }
      if (dataRecords != null && dataRecords.length > 0) {
        fireMsgDataRecordUpdated(dataRecords, RecordEvent.SET);
      }
      if (affectedMsgDatas != null && affectedMsgDatas.length > 0) {
        fireMsgDataRecordUpdated(affectedMsgDatas, RecordEvent.SET);
      }
    }

    // recalculate flags in the involved folders
    if (linkRecords != null && linkRecords.length > 0) {
      statUpdatesInFoldersForVisualNotification(linkRecords, false, false);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Removes records from the cache and fires appropriate event.
  */
  public void removeMsgLinkRecords(MsgLinkRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeMsgLinkRecords(MsgLinkRecord[] records)");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      synchronized (this) {
        records = (MsgLinkRecord[]) RecordUtils.remove(msgLinkRecordMap, records);
        if (records != null) {
          // for removed links remove them from secondary hashtable
          for (int i=0; i<records.length; i++) msgLinkRecordMap_byMsgId.remove(records[i].msgId, records[i]);
        }
      }
      fireMsgLinkRecordUpdated(records, RecordEvent.REMOVE);

      // recalculate flags in the involved folders
      statUpdatesInFoldersForVisualNotification(records, true, true);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Removes records from the cache and fires appropriate event.
  */
  public synchronized void removeMsgLinkRecords(Long[] msgLinkIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeMsgLinkRecords(Long[] msgLinkIDs)");
    if (trace != null) trace.args(msgLinkIDs);

    MsgLinkRecord[] msgLinks = getMsgLinkRecords(msgLinkIDs);
    if (msgLinks != null && msgLinks.length > 0) {
      removeMsgLinkRecords(msgLinks);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * @return the requested message link object.
  */
  public synchronized MsgLinkRecord getMsgLinkRecord(Long msgLinkId) {
    return (MsgLinkRecord) msgLinkRecordMap.get(msgLinkId);
  }

  /**
  * @return all Message Link Records from cache.
  */
  public synchronized MsgLinkRecord[] getMsgLinkRecords() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getMsgLinkRecords()");

    MsgLinkRecord[] msgLinks = (MsgLinkRecord[]) ArrayUtils.toArray(msgLinkRecordMap.values(), MsgLinkRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, msgLinks);
    return msgLinks;
  }

  /**
  * @return all Message Link Records for specified message link ids.
  * The records found in the cache are returned, the IDs which do not have
  * corresponding records in the cache are ignored.
  */
  public synchronized MsgLinkRecord[] getMsgLinkRecords(Long[] msgLinkIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getMsgLinkRecords(Long[] msgLinkIDs)");
    if (trace != null) trace.args(msgLinkIDs);

    ArrayList msgLinksL = new ArrayList();
    if (msgLinkIDs != null) {
      for (int i=0; i<msgLinkIDs.length; i++) {
        MsgLinkRecord msgLink = (MsgLinkRecord) msgLinkRecordMap.get(msgLinkIDs[i]);
        if (msgLink != null)
          msgLinksL.add(msgLink);
      }
    }
    MsgLinkRecord[] msgLinks = (MsgLinkRecord[]) ArrayUtils.toArray(msgLinksL, MsgLinkRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, msgLinks);
    return msgLinks;
  }

/**
  * @return all Message Link Records created between specified times.
  * The records found in the cache are returned.
  */
  public synchronized MsgLinkRecord[] getMsgLinkRecords(Date dateCreatedFrom, Date dateCreatedTo, Long ownerObjId, Short ownerObjType) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getMsgLinkRecords(Date dateCreatedFrom, Date dateCreatedTo, Long ownerObjId, Short ownerObjType)");
    if (trace != null) trace.args(dateCreatedFrom, dateCreatedTo, ownerObjId, ownerObjType);

    Date dateFrom = null;
    Date dateTo = null;
    if (dateCreatedFrom.before(dateCreatedTo)) {
      dateFrom = dateCreatedFrom;
      dateTo = dateCreatedTo;
    } else {
      dateFrom = dateCreatedTo;
      dateTo = dateCreatedFrom;
    }
    ArrayList msgLinksL = new ArrayList();
    Iterator iter = msgLinkRecordMap.values().iterator();
    while (iter.hasNext()) {
      MsgLinkRecord msgLink = (MsgLinkRecord) iter.next();
      if ((ownerObjId == null || ownerObjId.equals(msgLink.ownerObjId)) && (ownerObjType == null || ownerObjType.equals(msgLink.ownerObjType))) {
        if (msgLink.dateCreated.compareTo(dateFrom) >= 0 && msgLink.dateCreated.compareTo(dateTo) <= 0) {
          msgLinksL.add(msgLink);
        }
      }
    }
    MsgLinkRecord[] msgLinks = (MsgLinkRecord[]) ArrayUtils.toArray(msgLinksL, MsgLinkRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, msgLinks);
    return msgLinks;
  }

  /**
  * @return all Message Link Records for a given folder id.
  */
  public synchronized MsgLinkRecord[] getMsgLinkRecordsForFolder(Long folderId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getMsgLinkRecordsForFolder(Long folderId)");
    if (trace != null) trace.args(folderId);

    ArrayList msgLinksL = new ArrayList();
    Iterator iter = msgLinkRecordMap.values().iterator();
    while (iter.hasNext()) {
      MsgLinkRecord linkRecord = (MsgLinkRecord) iter.next();
      if (linkRecord != null &&
          linkRecord.ownerObjType != null &&
          linkRecord.ownerObjId != null &&
          linkRecord.ownerObjType.shortValue() == Record.RECORD_TYPE_FOLDER &&
          linkRecord.ownerObjId.equals(folderId))
        msgLinksL.add(linkRecord);
    }
    MsgLinkRecord[] msgLinks = (MsgLinkRecord[]) ArrayUtils.toArray(msgLinksL, MsgLinkRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, msgLinks);
    return msgLinks;
  }

  /**
  * @return all Message Link Records for a given folder IDs.
  */
  public synchronized MsgLinkRecord[] getMsgLinkRecordsForFolders(Long[] folderIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getMsgLinkRecordsForFolder(Long[] folderIDs)");
    if (trace != null) trace.args(folderIDs);

    // load lookup table
    HashSet folderIDsHS = new HashSet(Arrays.asList(folderIDs));
    // list for gathering items
    ArrayList msgLinksL = new ArrayList();
    Iterator iter = msgLinkRecordMap.values().iterator();
    while (iter.hasNext()) {
      MsgLinkRecord linkRecord = (MsgLinkRecord) iter.next();
      if (linkRecord != null &&
          linkRecord.ownerObjType != null &&
          linkRecord.ownerObjType.shortValue() == Record.RECORD_TYPE_FOLDER &&
          linkRecord.ownerObjId != null &&
          folderIDsHS.contains(linkRecord.ownerObjId)
        )
        msgLinksL.add(linkRecord);
    }
    MsgLinkRecord[] msgLinks = (MsgLinkRecord[]) ArrayUtils.toArray(msgLinksL, MsgLinkRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, msgLinks);
    return msgLinks;
  }

  /**
  * @return all Message Link Records for a given msg id.
  */
  public synchronized MsgLinkRecord[] getMsgLinkRecordsForMsg(Long msgId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getMsgLinkRecordsForMsg(Long msgId)");
    if (trace != null) trace.args(msgId);

    Collection msgLinksV = msgLinkRecordMap_byMsgId.getAll(msgId);
    MsgLinkRecord[] msgLinks = (MsgLinkRecord[]) ArrayUtils.toArray(msgLinksV, MsgLinkRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, msgLinks);
    return msgLinks;
  }


  /**
  * @return all of my folder shares that belong to specified folders.
  */
  public synchronized MsgLinkRecord[] getMsgLinkRecordsForMsgs(Long[] msgIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getMsgLinkRecordsForMsgs(Long[] msgIDs)");
    if (trace != null) trace.args(msgIDs);

    MsgLinkRecord[] records = null;
    if (msgIDs != null) {
      ArrayList linksL = new ArrayList();
      for (int i=0; i<msgIDs.length; i++) {
        Collection v = msgLinkRecordMap_byMsgId.getAll(msgIDs[i]);
        if (v != null) linksL.addAll(v);
      }
      records = (MsgLinkRecord[]) ArrayUtils.toArray(linksL, MsgLinkRecord.class);
    }

    if (trace != null) trace.exit(FetchedDataCache.class, records);
    return records;
  }


  /**
  * @return all Message Link Records that are owned by ownerId and type ownerType.
  */
  public synchronized MsgLinkRecord[] getMsgLinkRecordsOwnerAndType(Long ownerId, Short ownerType) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getMsgLinkRecordsOwnerAndType(Long ownerId, Short ownerType)");
    if (trace != null) trace.args(ownerId, ownerType);

    ArrayList msgLinksL = new ArrayList();
    if (ownerId != null && ownerType != null) {
      Collection col = msgLinkRecordMap.values();
      if (col != null && !col.isEmpty()) {
        Iterator iter = col.iterator();
        while (iter.hasNext()) {
          MsgLinkRecord linkRecord = (MsgLinkRecord) iter.next();
          if (ownerId.equals(linkRecord.ownerObjId) && ownerType.equals(linkRecord.ownerObjType)) {
            msgLinksL.add(linkRecord);
          }
        }
      }
    }
    MsgLinkRecord[] msgLinks = (MsgLinkRecord[]) ArrayUtils.toArray(msgLinksL, MsgLinkRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, msgLinks);
    return msgLinks;
  }


  /**
  * @return all Message Link Records that are owned by ownerIDs and type ownerType.
  */
  public synchronized MsgLinkRecord[] getMsgLinkRecordsOwnersAndType(Long[] ownerIDs, Short ownerType) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getMsgLinkRecordsOwnersAndType(Long[] ownerIDs, Short ownerType)");
    if (trace != null) trace.args(ownerIDs, ownerType);

    ArrayList msgLinksL = new ArrayList();
    if (ownerIDs != null && ownerIDs.length > 0 && ownerType != null) {
      // load lookup with wanted ownerIDs
      HashSet ownerIDsHS = new HashSet(Arrays.asList(ownerIDs));
      Collection coll = msgLinkRecordMap.values();
      if (coll != null && !coll.isEmpty()) {
        Iterator iter = coll.iterator();
        while (iter.hasNext()) {
          MsgLinkRecord linkRecord = (MsgLinkRecord) iter.next();
          if (ownerType.equals(linkRecord.ownerObjType)) {
            Long ownerObjId = linkRecord.ownerObjId;
            if (ownerObjId != null && ownerIDsHS.contains(ownerObjId)) {
              msgLinksL.add(linkRecord);
            }
          }
        }
      }
    }
    MsgLinkRecord[] msgLinks = (MsgLinkRecord[]) ArrayUtils.toArray(msgLinksL, MsgLinkRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, msgLinks);
    return msgLinks;
  }


  /************************************
  ***   MsgDataRecord operations   ***
  ************************************/

  /**
  * Adds new records or record updates into the cache and fires appropriate event.
  */
  public void addMsgDataRecords(MsgDataRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addMsgDataRecords(MsgDataRecord[] records)");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      // expensive unsealing outside of synchronized
      unWrapMsgDataRecords(records, null);
      synchronized (this) {
        records = (MsgDataRecord[]) RecordUtils.merge(msgDataRecordMap, records);
        for (int i=0; i<records.length; i++) msgDataRecordMap_byReplyToMsgId.put(records[i].replyToMsgId, records[i]);
      }
      fireMsgDataRecordUpdated(records, RecordEvent.SET);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }
  /**
  * Unseals MsgDataRecords
  * @param dataRecords MsgDataRecords we want to unseal
  * @param additionalMsgLinks 'nullable' Additional links that may hold keys to datas, which are not yet inserted in the cache
  */
  private void unWrapMsgDataRecords(MsgDataRecord[] dataRecords, MsgLinkRecord[] additionalMsgLinks) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "unWrapMsgDataRecords(MsgDataRecord[] dataRecords, MsgLinkRecord[] additionalMsgLinks)");
    if (trace != null) trace.args(dataRecords);
    // performance search enhancement for additional links
    MultiHashMap additionalHM_byMsgId = null;
    // for each Message
    for (int i=0; i<dataRecords.length; i++) {
      MsgDataRecord data = dataRecords[i];
      try {
        MsgLinkRecord[] linkRecords = getMsgLinkRecordsForMsg(data.msgId);
        // Find a symmetric key from links that might have been password protected and not unsealed yet...
        BASymmetricKey symmetricKey = null;
        if (linkRecords != null) {
          for (int k=0; k<linkRecords.length; k++) {
            if (linkRecords[k].getSymmetricKey() != null) {
              symmetricKey = linkRecords[k].getSymmetricKey();
              break;
            }
          }
        }
        // If needed, try the additional links
        if (symmetricKey == null && additionalMsgLinks != null) {
          // prep performance enhancement table
          if (additionalHM_byMsgId == null) {
            additionalHM_byMsgId = new MultiHashMap(true);
            for (int k=0; k<additionalMsgLinks.length; k++) additionalHM_byMsgId.put(additionalMsgLinks[k].msgId, additionalMsgLinks[k]);
          }
          Collection msgLinks = additionalHM_byMsgId.getAll(data.msgId);
          Iterator iter = msgLinks.iterator();
          while (iter.hasNext()) {
            MsgLinkRecord msgLink = (MsgLinkRecord) iter.next();
            if (msgLink.getSymmetricKey() != null) {
              symmetricKey = msgLink.getSymmetricKey();
              break;
            }
          }
        }
        if (symmetricKey != null) {
          // if this data record contains sendPrivKeyId, then signature needs to be verified
          if (data.getSendPrivKeyId() != null) {
            // for performance don't verify everything, do it when person asks to see it
            //            KeyRecord msgSigningKeyRec = getKeyRecord(data.getSendPrivKeyId());
            //            if (msgSigningKeyRec != null)
            //              data.unSeal(linkRecords[0].getSymmetricKey(), msgSigningKeyRec);
            //            else
            // signing key no longer exists, maybe the user account was deleted..., just unseal the message.
            data.unSealWithoutVerify(symmetricKey, msgBodyKeys);
          } else {
            // unSeal the subject only, don't verify signatures as the text is not available yet
            data.unSealSubject(symmetricKey);
          }
        }
      } catch (Throwable t) {
        if (trace != null) trace.data(100, "Exception occured while processing message data", data);
        if (trace != null) trace.exception(FetchedDataCache.class, 101, t);
      }
    }
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Removes records from the cache and fires appropriate event.
  */
  public void removeMsgDataRecords(MsgDataRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeMsgDataRecords(MsgDataRecord[] records)");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      synchronized (this) {
        records = (MsgDataRecord[]) RecordUtils.remove(msgDataRecordMap, records);
        if (records != null) {
          // for removed datas remove them from secondary hashtable
          for (int i=0; i<records.length; i++) msgDataRecordMap_byReplyToMsgId.remove(records[i].replyToMsgId, records[i]);
          removeAddrHashRecords(RecordUtils.getIDs(records));
        }
      }
      fireMsgDataRecordUpdated(records, RecordEvent.REMOVE);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  *
  * @return copy of our message body keys collection
  */
  public synchronized List getMsgBodyKeys() {
    ArrayList list = new ArrayList(msgBodyKeys.size());
    list.addAll(msgBodyKeys);
    return list;
  }
  public synchronized void addMsgBodyKey(Hasher.Set key) {
    if (!msgBodyKeys.contains(key))
      msgBodyKeys.add(key);
  }

  /**
  * @return Message Data Record for a given message ID.
  */
  public synchronized MsgDataRecord getMsgDataRecord(Long msgId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getMsgDataRecord(Long msgId)");
    if (trace != null) trace.args(msgId);
    MsgDataRecord dataRecord = (MsgDataRecord) msgDataRecordMap.get(msgId);
    if (trace != null) trace.exit(FetchedDataCache.class, dataRecord);
    return dataRecord;
  }
  public synchronized MsgDataRecord getMsgDataRecordNoTrace(Long msgId) {
    return (MsgDataRecord) msgDataRecordMap.get(msgId);
  }


  /**
  * @return all Message Data Records from cache.
  */
  public synchronized MsgDataRecord[] getMsgDataRecords() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getMsgDataRecords()");

    MsgDataRecord[] msgDatas = (MsgDataRecord[]) ArrayUtils.toArray(msgDataRecordMap.values(), MsgDataRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, msgDatas);
    return msgDatas;
  }


  /**
  * @return Message Data Record for a given message ID.
  */
  public synchronized MsgDataRecord[] getMsgDataRecords(Long[] msgIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getMsgDataRecords(Long[] msgIDs)");
    if (trace != null) trace.args(msgIDs);

    ArrayList msgDatasL = new ArrayList();
    if (msgIDs != null) {
      for (int i=0; i<msgIDs.length; i++) {
        MsgDataRecord msgData = (MsgDataRecord) msgDataRecordMap.get(msgIDs[i]);
        if (msgData != null)
          msgDatasL.add(msgData);
      }
    }
    MsgDataRecord[] msgDatas = (MsgDataRecord[]) ArrayUtils.toArray(msgDatasL, MsgDataRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, msgDatas);
    return msgDatas;
  }

  /**
  * @return Message Data Record for a given filter.
  */
  public synchronized MsgDataRecord[] getMsgDataRecords(MsgFilter msgFilter) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getMsgDataRecords(MsgFilter msgFilter)");
    if (trace != null) trace.args(msgFilter);

    ArrayList msgDatasL = new ArrayList();
    Collection col = msgDataRecordMap.values();
    Iterator iter = col.iterator();
    while (iter.hasNext()) {
      MsgDataRecord mData = (MsgDataRecord) iter.next();
      if (msgFilter.keep(mData))
        msgDatasL.add(mData);
    }
    MsgDataRecord[] msgDatas = (MsgDataRecord[]) ArrayUtils.toArray(msgDatasL, MsgDataRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, msgDatas);
    return msgDatas;
  }

  /**
  * @return all Message Data Records for specified Message Links.
  */
  public synchronized MsgDataRecord[] getMsgDataRecordsForLinks(Long[] msgLinkIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getMsgDataRecordsForLinks(Long[] msgLinkIDs)");
    if (trace != null) trace.args(msgLinkIDs);

    MsgLinkRecord[] msgLinks = getMsgLinkRecords(msgLinkIDs);
    Long[] msgIDs = MsgLinkRecord.getMsgIDs(msgLinks);
    MsgDataRecord[] msgDatas = getMsgDataRecords(msgIDs);

    if (trace != null) trace.exit(FetchedDataCache.class, msgDatas);
    return msgDatas;
  }

  /************************************
  ***   StatRecord operations      ***
  ************************************/

  /**
  * Adds new records or record updates into the cache and fires appropriate event.
  */
  public void addStatRecords(StatRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addStatRecords(StatRecord[])");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {

      synchronized (this) {
        records = (StatRecord[]) RecordUtils.merge(statRecordMap, records);
        for (int i=0; i<records.length; i++) {
          int type = StatRecord.getTypeIndex(records[i].objType.byteValue());
          statRecordMap_byLinkId[type].put(records[i].objLinkId, records[i]);
          statRecordMap_byObjId[type].put(records[i].objId, records[i]);
        }
      }

      // for any message stats, clear rendering cache as it may change due to possibly different "Read" stamp
      HashSet msgLinksHS = new HashSet();
      for (int i=0; i<records.length; i++) {
        StatRecord stat = records[i];
        if (stat.objType.byteValue() == StatRecord.STAT_TYPE_MESSAGE) {
          MsgLinkRecord msgLink = getMsgLinkRecord(stat.objLinkId);
          if (msgLink != null && !msgLinksHS.contains(msgLink)) {
            msgLink.clearPostRenderingCache();
            msgLinksHS.add(msgLink);
          }
        }
      }
      // re-inject the messages into cache to trigger listener updates
      if (msgLinksHS.size() > 0) {
        MsgLinkRecord[] msgLinks = (MsgLinkRecord[]) ArrayUtils.toArray(msgLinksHS, MsgLinkRecord.class);
        addMsgLinkRecords(msgLinks);
      }

      fireStatRecordUpdated(records, RecordEvent.SET);
      // special case of allowing to lower flag counts for clearing of Chat flags to work when typing new msg.
      statUpdatesInFoldersForVisualNotification(records, true, false);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Removes records from the cache and fires appropriate event.
  */
  public void removeStatRecords(StatRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeStatRecords(StatRecord[] records)");
    if (trace != null) trace.args(records);
    removeStatRecords(records, false);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public void removeStatRecords(StatRecord[] records, boolean visualNotification) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeStatRecords(StatRecord[])");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      synchronized (this) {
        records = (StatRecord[]) RecordUtils.remove(statRecordMap, records);
        if (records != null) {
          // for removed records remove them from secondary hashtable
          for (int i=0; i<records.length; i++) {
            int type = StatRecord.getTypeIndex(records[i].objType.byteValue());
            statRecordMap_byLinkId[type].remove(records[i].objLinkId, records[i]);
            statRecordMap_byObjId[type].remove(records[i].objId, records[i]);
          }
        }
      }

      fireStatRecordUpdated(records, RecordEvent.REMOVE);
      if (visualNotification)
        statUpdatesInFoldersForVisualNotification(records, true, true);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Not traced for performance.
  * @return Stat Record for a given Link ID
  */
  public synchronized Collection getStatRecordsByLinkId(Long linkId, int statType) {
    return statRecordMap_byLinkId[statType].getAll(linkId);
  }
  private synchronized StatRecord getStatRecordByLinkId(Long linkId, int statType, Long userId) {
    StatRecord myStat = null;
    Collection recs = getStatRecordsByLinkId(linkId, statType);
    if (recs != null) {
      Iterator iter = recs.iterator();
      while (iter.hasNext()) {
        StatRecord rec = (StatRecord) iter.next();
        if (rec.ownerUserId.equals(userId)) {
          myStat = rec;
          break;
        }
      }
    }
    return myStat;
  }
  public synchronized StatRecord getStatRecordMyLinkId(Long linkId, int statType) {
    return getStatRecordByLinkId(linkId, statType, myUserId);
  }

  /**
  * Not traced for performance.
  * @return Stat Record for a given Link ID
  */
  public synchronized Collection getStatRecordsByObjId(Long objId, int statType) {
    return statRecordMap_byObjId[statType].getAll(objId);
  }
  private synchronized StatRecord getStatRecordByObjId(Long objId, int statType, Long userId) {
    StatRecord myStat = null;
    Collection recs = getStatRecordsByObjId(objId, statType);
    if (recs != null) {
      Iterator iter = recs.iterator();
      while (iter.hasNext()) {
        StatRecord rec = (StatRecord) iter.next();
        if (rec.ownerUserId.equals(userId)) {
          myStat = rec;
          break;
        }
      }
    }
    return myStat;
  }
  public synchronized StatRecord getStatRecordMyObjId(Long objId, int statType) {
    return getStatRecordByObjId(objId, statType, myUserId);
  }

  /**
  * @return all StatRecords from cache
  */
  public synchronized StatRecord[] getStatRecords() {
    StatRecord[] stats = (StatRecord[]) ArrayUtils.toArray(statRecordMap.values(), StatRecord.class);
    return stats;
  }


  /************************************
  ***   EmailRecord operations     ***
  ************************************/

  /**
  * Adds new records or record updates into the cache and fires appropriate event.
  */
  public void addEmailRecords(EmailRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addEmailRecords(EmailRecord[])");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      synchronized (this) {
        records = (EmailRecord[]) RecordUtils.merge(emailRecordMap, records);
      }
      fireEmailRecordUpdated(records, RecordEvent.SET);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Removes records from the cache and fires appropriate event.
  */
  public void removeEmailRecords(EmailRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeEmailRecords(EmailRecord[] records)");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      synchronized (this) {
        records = (EmailRecord[]) RecordUtils.remove(emailRecordMap, records);
      }
      fireEmailRecordUpdated(records, RecordEvent.REMOVE);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Removes records from the cache and fires appropriate event.
  */
  public void removeEmailRecords(Long[] emlIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeEmailRecords(Long[] emlIDs)");
    if (trace != null) trace.args(emlIDs);

    EmailRecord[] records = getEmailRecords(emlIDs);
    removeEmailRecords(records);

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * @return EmailRecord for a given ID
  */
  public synchronized EmailRecord getEmailRecord(Long emlId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getEmailRecord(Long emlId)");
    if (trace != null) trace.args(emlId);

    EmailRecord emailRecord = (EmailRecord) emailRecordMap.get(emlId);

    if (trace != null) trace.exit(FetchedDataCache.class, emailRecord);
    return emailRecord;
  }

  /**
  * @return EmailRecord for a given ID
  */
  public synchronized EmailRecord getEmailRecord(String emlAddr) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getEmailRecord(String emlAddr)");
    if (trace != null) trace.args(emlAddr);

    EmailRecord emlRec = null;
    Iterator iter = emailRecordMap.values().iterator();
    while (iter.hasNext()) {
      EmailRecord rec = (EmailRecord) iter.next();
      if (EmailRecord.isAddressEqual(rec.emailAddr, emlAddr)) {
        emlRec = rec;
        break;
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class, emlRec);
    return emlRec;
  }

  /**
  * @return all EmailRecords from cache
  */
  public synchronized EmailRecord[] getEmailRecords() {
    return (EmailRecord[]) ArrayUtils.toArray(emailRecordMap.values(), EmailRecord.class);
  }

  /**
  * @return all EmailRecords from cache with specified IDs
  */
  public synchronized EmailRecord[] getEmailRecords(Long userId) {
    ArrayList emailsL = new ArrayList();
    if (userId != null) {
      Iterator iter = emailRecordMap.values().iterator();
      while (iter.hasNext()) {
        EmailRecord rec = (EmailRecord) iter.next();
        if (rec.userId.equals(userId))
          emailsL.add(rec);
      }
    }
    EmailRecord[] emails = (EmailRecord[]) ArrayUtils.toArray(emailsL, EmailRecord.class);
    return emails;
  }

  /**
  * @return all EmailRecords from cache with specified IDs
  */
  public synchronized EmailRecord[] getEmailRecords(Long[] emailIDs) {
    ArrayList emailsL = new ArrayList();
    if (emailIDs != null) {
      for (int i=0; i<emailIDs.length; i++) {
        EmailRecord sRec = (EmailRecord) emailRecordMap.get(emailIDs[i]);
        if (sRec != null)
          emailsL.add(sRec);
      }
    }
    EmailRecord[] emails = (EmailRecord[]) ArrayUtils.toArray(emailsL, EmailRecord.class);
    return emails;
  }


  /***************************************
  ***   AddrHashRecord operations     ***
  ***************************************/

  /**
  * Adds new records or record updates into the cache and fires appropriate event.
  */
  public void addAddrHashRecords(AddrHashRecord[] records) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addAddrHashRecords(AddrHashRecord[] records)");
    if (trace != null) trace.args(records);

    if (records != null && records.length > 0) {
      synchronized (this) {
        for (int i=0; records!=null && i<records.length; i++) {
          addrHashRecordMap_byMsgId.put(records[i].msgId, records[i]);
          addrHashRecordMap_byHash.put(records[i].hash.getHexContent(), records[i]); // for some strange reason byte[] doesn't work as key, so use String equivalent instead
        }
      }
      //fireAddrHashRecordUpdated(records, RecordEvent.SET);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * Removes records from the cache and fires appropriate event. -- this is difficult because of 1 to many relationship between msgId and hash... leave this for now
  */
  public void removeAddrHashRecords(Long[] msgIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeAddrHashRecords(Long[] msgIDs)");
    if (trace != null) trace.args(msgIDs);

    if (msgIDs != null && msgIDs.length > 0) {
      synchronized (this) {
        for (int i=0; i<msgIDs.length; i++) {
          // remove the first element to find out the hash, then remove all other for the same key...
          AddrHashRecord addrHashRecord = (AddrHashRecord) addrHashRecordMap_byMsgId.remove(msgIDs[i]);
          if (addrHashRecord != null) {
            addrHashRecordMap_byMsgId.removeAll(msgIDs[i]);
            addrHashRecordMap_byHash.removeAll(addrHashRecord.hash.getHexContent());
          }
        }
      }
      //fireAddrHashRecordUpdated(records, RecordEvent.REMOVE);
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /**
  * @return AddrHashRecord for a given ID
  */
  public synchronized AddrHashRecord[] getAddrHashRecordsForMsgId(Long msgId) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getAddrHashRecordsForMsgId(Long msgId)");
    if (trace != null) trace.args(msgId);

    Collection addrHashRecordsV = addrHashRecordMap_byMsgId.getAll(msgId);
    AddrHashRecord[] addrHashRecords = (AddrHashRecord[]) ArrayUtils.toArray(addrHashRecordsV, AddrHashRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, addrHashRecords);
    return addrHashRecords;
  }

  /**
  * @return AddrHashRecord for a given hash
  */
  public synchronized AddrHashRecord[] getAddrHashRecords(byte[] hash) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getAddrHashRecords(byte[] hash)");
    if (trace != null) trace.args(hash);

    AddrHashRecord[] addrHashRecords = null;
    if (hash != null)
      addrHashRecords = getAddrHashRecords(new BADigestBlock(hash));

    if (trace != null) trace.exit(FetchedDataCache.class, addrHashRecords);
    return addrHashRecords;
  }

  /**
  * @return AddrHashRecord for a given hash
  */
  public synchronized AddrHashRecord[] getAddrHashRecords(BADigestBlock hash) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getAddrHashRecords(BADigestBlock hash)");
    if (trace != null) trace.args(hash);

    String hashHex = hash.getHexContent();
    Collection addrHashRecordsV = addrHashRecordMap_byHash.getAll(hashHex);
    AddrHashRecord[] addrHashRecords = (AddrHashRecord[]) ArrayUtils.toArray(addrHashRecordsV, AddrHashRecord.class);

    if (trace != null) trace.exit(FetchedDataCache.class, addrHashRecords);
    return addrHashRecords;
  }

  /**
  * @return AddrHashRecord for a given email string
  */
  public synchronized AddrHashRecord[] getAddrHashRecords(String emailAddr) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getAddrHashRecords(String emailAddr)");
    if (trace != null) trace.args(emailAddr);

    byte[] hash = getAddrHashForEmail(emailAddr);
    AddrHashRecord[] addrHashRecords = null;
    if (hash != null)
      addrHashRecords = getAddrHashRecords(hash);

    if (trace != null) trace.exit(FetchedDataCache.class, addrHashRecords);
    return addrHashRecords;
  }

  /**
  * @return Address Record for a given hash
  */
  public synchronized MsgDataRecord[] getAddrRecords(byte[] hash) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getAddrRecords(byte[] hash)");
    if (trace != null) trace.args(hash);

    AddrHashRecord[] addrHashRecords = getAddrHashRecords(hash);
    MsgDataRecord[] addrRecords = null;
    if (addrHashRecords != null) {
      Long[] msgIDs = AddrHashRecord.getMsgIDs(addrHashRecords);
      addrRecords = getMsgDataRecords(msgIDs);
    }

    if (trace != null) trace.exit(FetchedDataCache.class, addrRecords);
    return addrRecords;
  }

  /**
  * @return Address Record for a given hash looking between specified msgIDs
  */
  public synchronized MsgDataRecord[] getAddrRecords(byte[] hash, Long[] fromMsgIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getAddrRecords(byte[] hash, Long[] fromMsgIDs)");
    if (trace != null) trace.args(hash, fromMsgIDs);

    MsgDataRecord[] addrRecords = null;
    if (fromMsgIDs == null) {
      addrRecords = getAddrRecords(hash);
    } else {
      // if hash exists then look for it between specified msgIDs
      if (getAddrHashRecords(hash) != null) {
        BADigestBlock hashBA = new BADigestBlock(hash);
        HashSet msgIDsHS = new HashSet();
        for (int i=0; i<fromMsgIDs.length; i++) {
          Collection addrHashRecordsV = addrHashRecordMap_byMsgId.getAll(fromMsgIDs[i]);
          // all these records must have the same msgId so find only the first match
          if (addrHashRecordsV != null) {
            Iterator iter = addrHashRecordsV.iterator();
            while (iter.hasNext()) {
              AddrHashRecord addrHashRecord = (AddrHashRecord) iter.next();
              if (addrHashRecord.hash.equals(hashBA) && !msgIDsHS.contains(addrHashRecord.msgId)) {
                msgIDsHS.add(addrHashRecord.msgId);
                // break on first match, because we are finding UNIQUE msgIDs
                break;
              }
            }
          }
        }
        if (msgIDsHS.size() > 0) {
          addrRecords = getMsgDataRecords((Long[]) ArrayUtils.toArray(msgIDsHS, Long.class));
        }
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class, addrRecords);
    return addrRecords;
  }

  /**
  * @return Address Record for a given email string
  */
  public synchronized MsgDataRecord[] getAddrRecords(String emailAddr) {
    return getAddrRecords(emailAddr, null);
  }

  /**
  * @return Address Record for a given email string
  */
  public synchronized MsgDataRecord[] getAddrRecords(String emailAddr, Long[] fromMsgIDs) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "getAddrRecords(String emailAddr, Long[] fromMsgIDs)");
    if (trace != null) trace.args(emailAddr);

    byte[] hash = getAddrHashForEmail(emailAddr);
    MsgDataRecord[] addrRecords = null;
    if (hash != null)
      addrRecords = fromMsgIDs != null ? getAddrRecords(hash, fromMsgIDs) : getAddrRecords(hash);

    if (trace != null) trace.exit(FetchedDataCache.class, addrRecords);
    return addrRecords;
  }

  // normalize email address to a hash value
  public static byte[] getAddrHashForEmail(String emailAddr) {
    byte[] hash = null;
    if (emailAddr != null) {
      String[] addrs = EmailRecord.gatherAddresses(emailAddr);
      if (addrs != null && addrs.length > 0) {
        emailAddr = addrs[addrs.length-1].trim().toLowerCase(Locale.US);
        MessageDigest messageDigest = null;
        try {
          messageDigest = MessageDigest.getInstance("MD5");
          hash = messageDigest.digest(emailAddr.getBytes());
        } catch (Throwable t) {
          throw new IllegalStateException("Could not create MessageDigest.");
        }
      }
    }
    return hash;
  }

  //===========================================================================
  //=====================   L I S T E N E R S  ================================
  //===========================================================================

  /****************************************
  ***   UserRecord Listener handling   ***
  ****************************************/

  public synchronized void addUserRecordListener(UserRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addUserRecordListener(UserRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.add(UserRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeUserRecordListener(UserRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeUserRecordListener(UserRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.remove(UserRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Notify all listeners that have registered interest for
  * notification on this event type.  The event instance
  * is lazily created using the parameters passed into
  * the fire method.
  */
  protected void fireUserRecordUpdated(UserRecord[] records, int eventType) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "fireUserRecordUpdated(UserRecord[], int eventType)");
    if (trace != null) trace.args(records);
    if (trace != null) trace.args(eventType);

    if (!DEBUG__SUPPRESS_EVENTS_USERS) {
      // Guaranteed to return a non-null array
      Object[] listeners = myListenerList.getListenerList();
      UserRecordEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for (int i = listeners.length-2; i>=0; i-=2) {
        if (listeners[i] == UserRecordListener.class) {
          // Lazily create the event:
          if (e == null)
            e = new UserRecordEvent(this, records, eventType);
          ((UserRecordListener)listeners[i+1]).userRecordUpdated(e);
        }
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /************************************************
  ***   UserSettingsRecord Listener handling   ***
  ************************************************/

  public synchronized void addUserSettingsRecordListener(UserSettingsRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addUserSettingsRecordListener(UserSettingsRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.add(UserSettingsRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeUserSettingsRecordListener(UserSettingsRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeUserSettingsRecordListener(UserSettingsRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.remove(UserSettingsRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Notify all listeners that have registered interest for
  * notification on this event type.  The event instance
  * is lazily created using the parameters passed into
  * the fire method.
  */
  protected void fireUserSettingsRecordUpdated(UserSettingsRecord[] records, int eventType) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "fireUserSettingsRecordUpdated(UserSettingsRecord[], int eventType)");
    if (trace != null) trace.args(records);
    if (trace != null) trace.args(eventType);

    if (!DEBUG__SUPPRESS_EVENTS_USERS) {
      // Guaranteed to return a non-null array
      Object[] listeners = myListenerList.getListenerList();
      UserSettingsRecordEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for (int i = listeners.length-2; i>=0; i-=2) {
        if (listeners[i] == UserSettingsRecordListener.class) {
          // Lazily create the event:
          if (e == null)
            e = new UserSettingsRecordEvent(this, records, eventType);
          ((UserSettingsRecordListener)listeners[i+1]).userSettingsRecordUpdated(e);
        }
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /******************************************
  ***   FolderRecord Listener handling   ***
  ******************************************/

  public synchronized void addFolderRecordListener(FolderRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addFolderRecordListener(FolderRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.add(FolderRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeFolderRecordListener(FolderRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeFolderRecordListener(FolderRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.remove(FolderRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Notify all listeners that have registered interest for
  * notification on this event type.  The event instance
  * is lazily created using the parameters passed into
  * the fire method.
  */
  public void fireFolderRecordUpdated(FolderRecord[] records, int eventType) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "fireFolderRecordUpdated(FolderRecord[], int eventType)");
    if (trace != null) trace.args(records);
    if (trace != null) trace.args(eventType);

    if (!DEBUG__SUPPRESS_EVENTS_FOLDERS) {
      // Guaranteed to return a non-null array
      Object[] listeners = myListenerList.getListenerList();
      FolderRecordEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for (int i = listeners.length-2; i>=0; i-=2) {
        if (listeners[i] == FolderRecordListener.class) {
          // Lazily create the event:
          if (e == null)
            e = new FolderRecordEvent(this, records, eventType);
          ((FolderRecordListener)listeners[i+1]).folderRecordUpdated(e);
        }
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  private void fireFolderRecordUpdated_Delayed(FolderRecord[] records, int eventType) {
    fireRecordsUpdated_Delayed(records, eventType, delayedFoldersSet, delayedFoldersRemoved);
  }


  /***********************************************
  ***   FolderShareRecord Listener handling   ***
  ***********************************************/

  public synchronized void addFolderShareRecordListener(FolderShareRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addFolderShareRecordListener(FolderShareRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.add(FolderShareRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeFolderShareRecordListener(FolderShareRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeFolderShareRecordListener(FolderShareRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.remove(FolderShareRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Notify all listeners that have registered interest for
  * notification on this event type.  The event instance
  * is lazily created using the parameters passed into
  * the fire method.
  */
  protected void fireFolderShareRecordUpdated(FolderShareRecord[] records, int eventType) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "fireFolderShareRecordUpdated(FolderShareRecord[], int eventType)");
    if (trace != null) trace.args(records);
    if (trace != null) trace.args(eventType);

    if (!DEBUG__SUPPRESS_EVENTS_FOLDERS) {
      // Guaranteed to return a non-null array
      Object[] listeners = myListenerList.getListenerList();
      FolderShareRecordEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for (int i = listeners.length-2; i>=0; i-=2) {
        if (listeners[i] == FolderShareRecordListener.class) {
          // Lazily create the event:
          if (e == null)
            e = new FolderShareRecordEvent(this, records, eventType);
          ((FolderShareRecordListener)listeners[i+1]).folderShareRecordUpdated(e);
        }
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }



  /******************************************
  ***   FileLinkRecord Listener handling   ***
  ******************************************/

  public synchronized void addFileLinkRecordListener(FileLinkRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addFileLinkRecordListener(FileLinkRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.add(FileLinkRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeFileLinkRecordListener(FileLinkRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeFolderRecordListener(FolderRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.remove(FileLinkRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Notify all listeners that have registered interest for
  * notification on this event type.  The event instance
  * is lazily created using the parameters passed into
  * the fire method.
  */
  protected void fireFileLinkRecordUpdated(FileLinkRecord[] records, int eventType) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "fireFileLinkRecordUpdated(FileLinkRecord[], int eventType)");
    if (trace != null) trace.args(records);
    if (trace != null) trace.args(eventType);

    if (!DEBUG__SUPPRESS_EVENTS_FILES) {
      // Guaranteed to return a non-null array
      Object[] listeners = myListenerList.getListenerList();
      FileLinkRecordEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for (int i = listeners.length-2; i>=0; i-=2) {
        if (listeners[i] == FileLinkRecordListener.class) {
          // Lazily create the event:
          if (e == null)
            e = new FileLinkRecordEvent(this, records, eventType);
          ((FileLinkRecordListener)listeners[i+1]).fileLinkRecordUpdated(e);
        }
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }



  /******************************************
  ***   InvEmlRecord Listener handling   ***
  ******************************************/

  public synchronized void addInvEmlRecordListener(InvEmlRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addInvEmlRecordListener(InvEmlRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.add(InvEmlRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeInvEmlRecordListener(InvEmlRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeInvEmlRecordListener(InvEmlRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.remove(InvEmlRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Notify all listeners that have registered interest for
  * notification on this event type.  The event instance
  * is lazily created using the parameters passed into
  * the fire method.
  */
  protected void fireInvEmlRecordUpdated(InvEmlRecord[] records, int eventType) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "fireInvEmlRecordUpdated(InvEmlRecord[], int eventType)");
    if (trace != null) trace.args(records);
    if (trace != null) trace.args(eventType);

    if (!DEBUG__SUPPRESS_EVENTS_INV_EMLS) {
      // Guaranteed to return a non-null array
      Object[] listeners = myListenerList.getListenerList();
      InvEmlRecordEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for (int i = listeners.length-2; i>=0; i-=2) {
        if (listeners[i] == InvEmlRecordListener.class) {
          // Lazily create the event:
          if (e == null)
            e = new InvEmlRecordEvent(this, records, eventType);
          ((InvEmlRecordListener)listeners[i+1]).invEmlRecordUpdated(e);
        }
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }



  /***************************************
  ***   KeyRecord Listener handling   ***
  ***************************************/

  public synchronized void addKeyRecordListener(KeyRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addKeyRecordListener(KeyRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.add(KeyRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeKeyRecordListener(KeyRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeKeyRecordListener(KeyRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.remove(KeyRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Notify all listeners that have registered interest for
  * notification on this event type.  The event instance
  * is lazily created using the parameters passed into
  * the fire method.
  */
  protected void fireKeyRecordUpdated(KeyRecord[] records, int eventType) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "fireKeyRecordUpdated(KeyRecord[], int eventType)");
    if (trace != null) trace.args(records);
    if (trace != null) trace.args(eventType);

    if (!DEBUG__SUPPRESS_EVENTS_KEYS) {
      // Guaranteed to return a non-null array
      Object[] listeners = myListenerList.getListenerList();
      KeyRecordEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for (int i = listeners.length-2; i>=0; i-=2) {
        if (listeners[i] == KeyRecordListener.class) {
          // Lazily create the event:
          if (e == null)
            e = new KeyRecordEvent(this, records, eventType);
          ((KeyRecordListener)listeners[i+1]).keyRecordUpdated(e);
        }
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }



  /*******************************************
  ***   ContactRecord Listener handling   ***
  *******************************************/

  public synchronized void addContactRecordListener(ContactRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addContactRecordListener(ContactRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.add(ContactRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeContactRecordListener(ContactRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeContactRecordListener(ContactRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.remove(ContactRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Notify all listeners that have registered interest for
  * notification on this event type.  The event instance
  * is lazily created using the parameters passed into
  * the fire method.
  */
  protected void fireContactRecordUpdated(ContactRecord[] records, int eventType) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "fireContactRecordUpdated(ContactRecord[], int eventType)");
    if (trace != null) trace.args(records);
    if (trace != null) trace.args(eventType);

    if (!DEBUG__SUPPRESS_EVENTS_CONTACTS) {
      // Guaranteed to return a non-null array
      Object[] listeners = myListenerList.getListenerList();
      ContactRecordEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for (int i = listeners.length-2; i>=0; i-=2) {
        if (listeners[i] == ContactRecordListener.class) {
          // Lazily create the event:
          if (e == null)
            e = new ContactRecordEvent(this, records, eventType);
          ((ContactRecordListener)listeners[i+1]).contactRecordUpdated(e);
        }
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  private void fireContactRecordUpdated_Delayed(ContactRecord[] records, int eventType) {
    fireRecordsUpdated_Delayed(records, eventType, delayedContactsSet, delayedContactsRemoved);
  }


  /*******************************************
  ***   MsgLinkRecord Listener handling   ***
  *******************************************/

  public synchronized void addMsgLinkRecordListener(MsgLinkRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addMsgLinkRecordListener(MsgLinkRecordListener l)");
    if (trace != null) trace.args(l);
    myListenerList.add(MsgLinkRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeMsgLinkRecordListener(MsgLinkRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeMsgLinkRecordListener(MsgLinkRecordListener l)");
    if (trace != null) trace.args(l);
    myListenerList.remove(MsgLinkRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Notify all listeners that have registered interest for
  * notification on this event type.  The event instance
  * is lazily created using the parameters passed into
  * the fire method.
  * Mostly called internally when records are added or removed.
  */
  public void fireMsgLinkRecordUpdated(MsgLinkRecord[] records, int eventType) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "fireMsgLinkRecordUpdated(MsgLinkRecord[] records, int eventType)");
    if (trace != null) trace.args(records);
    if (trace != null) trace.args(eventType);

    if (!DEBUG__SUPPRESS_EVENTS_MSGS) {
      // Guaranteed to return a non-null array
      Object[] listeners = myListenerList.getListenerList();
      MsgLinkRecordEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for (int i = listeners.length-2; i>=0; i-=2) {
        if (listeners[i] == MsgLinkRecordListener.class) {
          // Lazily create the event:
          if (e == null)
            e = new MsgLinkRecordEvent(this, records, eventType);
          ((MsgLinkRecordListener)listeners[i+1]).msgLinkRecordUpdated(e);
        }
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }



  /*******************************************
  ***   MsgDataRecord Listener handling   ***
  *******************************************/

  public synchronized void addMsgDataRecordListener(MsgDataRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addMsgDataRecordListener(MsgDataRecordListener l)");
    if (trace != null) trace.args(l);
    myListenerList.add(MsgDataRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeMsgDataRecordListener(MsgDataRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeMsgDataRecordListener(MsgDataRecordListener l)");
    if (trace != null) trace.args(l);
    myListenerList.remove(MsgDataRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Notify all listeners that have registered interest for
  * notification on this event type.  The event instance
  * is lazily created using the parameters passed into
  * the fire method.
  * Mostly called internally when records are added or removed.
  */
  public void fireMsgDataRecordUpdated(MsgDataRecord[] records, int eventType) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "fireMsgDataRecordUpdated(MsgDataRecord[] records, int eventType)");
    if (trace != null) trace.args(records);
    if (trace != null) trace.args(eventType);

    if (!DEBUG__SUPPRESS_EVENTS_MSGS) {
      // Guaranteed to return a non-null array
      Object[] listeners = myListenerList.getListenerList();
      MsgDataRecordEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for (int i = listeners.length-2; i>=0; i-=2) {
        if (listeners[i] == MsgDataRecordListener.class) {
          // Lazily create the event:
          if (e == null)
            e = new MsgDataRecordEvent(this, records, eventType);
          ((MsgDataRecordListener)listeners[i+1]).msgDataRecordUpdated(e);
        }
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /*******************************************
  ***   StatRecord Listener handling      ***
  *******************************************/

  public synchronized void addStatRecordListener(StatRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addStatRecordListener(StatRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.add(StatRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeStatRecordListener(StatRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeStatRecordListener(StatRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.remove(StatRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Notify all listeners that have registered interest for
  * notification on this event type.  The event instance
  * is lazily created using the parameters passed into
  * the fire method.
  */
  protected void fireStatRecordUpdated(StatRecord[] records, int eventType) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "fireStatRecordUpdated(StatRecord[], int eventType)");
    if (trace != null) trace.args(records);
    if (trace != null) trace.args(eventType);

    if (!DEBUG__SUPPRESS_EVENTS_STATS) {
      // Guaranteed to return a non-null array
      Object[] listeners = myListenerList.getListenerList();
      StatRecordEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for (int i = listeners.length-2; i>=0; i-=2) {
        if (listeners[i] == StatRecordListener.class) {
          // Lazily create the event:
          if (e == null)
            e = new StatRecordEvent(this, records, eventType);
          ((StatRecordListener)listeners[i+1]).statRecordUpdated(e);
        }
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /*******************************************
  ***   EmailRecord Listener handling     ***
  *******************************************/

  public synchronized void addEmailRecordListener(EmailRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addEmailRecordListener(EmailRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.add(EmailRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeEmailRecordListener(EmailRecordListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeEmailRecordListener(EmailRecordListener)");
    if (trace != null) trace.args(l);
    myListenerList.remove(EmailRecordListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Notify all listeners that have registered interest for
  * notification on this event type.  The event instance
  * is lazily created using the parameters passed into
  * the fire method.
  */
  protected void fireEmailRecordUpdated(EmailRecord[] records, int eventType) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "fireEmailRecordUpdated(EmailRecord[], int eventType)");
    if (trace != null) trace.args(records);
    if (trace != null) trace.args(eventType);

    if (!DEBUG__SUPPRESS_EVENTS_STATS) {
      // Guaranteed to return a non-null array
      Object[] listeners = myListenerList.getListenerList();
      EmailRecordEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for (int i = listeners.length-2; i>=0; i-=2) {
        if (listeners[i] == EmailRecordListener.class) {
          // Lazily create the event:
          if (e == null)
            e = new EmailRecordEvent(this, records, eventType);
          ((EmailRecordListener)listeners[i+1]).emailRecordUpdated(e);
        }
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /*********************************************
  ***   FldRingRing Listener handling       ***
  *********************************************/

  public synchronized void addFolderRingListener(FolderRingListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addFolderRingListener(FolderRingListener))");
    if (trace != null) trace.args(l);
    myListenerList.add(FolderRingListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeFolderRingListener(FolderRingListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeFolderRingListener(FolderRingListener)");
    if (trace != null) trace.args(l);
    myListenerList.remove(FolderRingListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Notify all listeners that have registered interest for
  * notification on this event type.  The event instance
  * is lazily created using the parameters passed into
  * the fire method.
  */
  public void fireFolderRingEvent(Obj_List_Co source) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "fireFolderRingEvent(Obj_List_Co source)");
    if (trace != null) trace.args(source);

    // Guaranteed to return a non-null array
    Object[] listeners = myListenerList.getListenerList();
    EventObject e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i] == FolderRingListener.class) {
        // Lazily create the event:
        if (e == null)
          e = new EventObject(source);
        ((FolderRingListener)listeners[i+1]).fldRingRingUpdate(e);
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /*******************************************
  ***   MsgPopup Listener handling       ***
  *******************************************/

  public synchronized void addMsgPopupListener(MsgPopupListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addMsgPopupListener(MsgPopupListener)");
    if (trace != null) trace.args(l);
    myListenerList.add(MsgPopupListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeMsgPopupListener(MsgPopupListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeMsgPopupListener(MsgPopupListener)");
    if (trace != null) trace.args(l);
    myListenerList.remove(MsgPopupListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Notify all listeners that have registered interest for
  * notification on this event type.  The event instance
  * is lazily created using the parameters passed into
  * the fire method.
  */
  public void fireMsgPopupEvent(String htmlText) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "fireMsgPopupEvent(String htmlText)");
    if (trace != null) trace.args(htmlText);

    // Guaranteed to return a non-null array
    Object[] listeners = myListenerList.getListenerList();
    EventObject e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i] == MsgPopupListener.class) {
        // Lazily create the event:
        if (e == null)
          e = new EventObject(htmlText);
        ((MsgPopupListener)listeners[i+1]).msgPopupUpdate(e);
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /*******************************************
  ***   MsgTyping Listener handling       ***
  *******************************************/

  public synchronized void addMsgTypingListener(MsgTypingListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "addMsgTypingListener(MsgTypingListener)");
    if (trace != null) trace.args(l);
    myListenerList.add(MsgTypingListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  public synchronized void removeMsgTypingListener(MsgTypingListener l) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "removeMsgTypingListener(MsgTypingListener)");
    if (trace != null) trace.args(l);
    myListenerList.remove(MsgTypingListener.class, l);
    if (trace != null) trace.exit(FetchedDataCache.class);
  }


  /**
  * Notify all listeners that have registered interest for
  * notification on this event type.  The event instance
  * is lazily created using the parameters passed into
  * the fire method.
  */
  public void fireMsgTypingEvent(Obj_List_Co source) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "fireMsgTypingEvent(Obj_List_Co source)");
    if (trace != null) trace.args(source);

    // Guaranteed to return a non-null array
    Object[] listeners = myListenerList.getListenerList();
    EventObject e = null;
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i] == MsgTypingListener.class) {
        // Lazily create the event:
        if (e == null)
          e = new EventObject(source);
        ((MsgTypingListener)listeners[i+1]).msgTypingUpdate(e);
      }
    }

    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  /****************************************
  ***   RecordIteratorI handling       ***
  ****************************************/

  public synchronized boolean addViewIterator(RecordIteratorI viewIterator) {
    boolean added = false;
    if (viewIterators == null)
      viewIterators = new ArrayList();
    if (!viewIterators.contains(viewIterator))
      added = viewIterators.add(viewIterator);
    return added;
  }

  public synchronized boolean removeViewIterator(RecordIteratorI viewIterator) {
    boolean removed = false;
    if (viewIterators != null)
      removed = viewIterators.remove(viewIterator);
    return removed;
  }

  public Record getNextItem(Record item, int direction) {
    Record next = null;
    if (viewIterators != null) {
      synchronized (viewIterators) {
        for (int i=viewIterators.size()-1; i>=0; i--) {
          RecordIteratorI iter = (RecordIteratorI) viewIterators.get(i);
          int position = iter.getPosition(item);
          if (position != -1) {
            next = iter.getItemNext(item, direction);
            // If resolved, escape, else maybe another iterator will resolve this position...
            if (next != null) 
              break;
          }
        }
      }
    }
    return next;
  }

  public int getViewPosition(Record item) {
    int position = -1;
    if (viewIterators != null) {
      synchronized (viewIterators) {
        for (int i=viewIterators.size()-1; i>=0; i--) {
          RecordIteratorI iter = (RecordIteratorI) viewIterators.get(i);
          position = iter.getPosition(item);
          if (position != -1) {
            // see if we need to adjust for "LOADING" rendering items
            Record first = iter.getItem(0);
            if (first != null && first.getId().longValue() < 0)
              position --;
            break;
          }
        }
      }
    }
    return position;
  }

  public int getViewCount(Record item) {
    int count = -1;
    int position = -1;
    if (viewIterators != null) {
      synchronized (viewIterators) {
        for (int i=viewIterators.size()-1; i>=0; i--) {
          RecordIteratorI iter = (RecordIteratorI) viewIterators.get(i);
          position = iter.getPosition(item);
          if (position != -1) {
            count = iter.getCount();
            // see if we need to adjust for "LOADING" rendering items
            Record first = iter.getItem(0);
            Record last = iter.getItem(count-1);
            if (first != null && first.getId().longValue() < 0)
              count --;
            if (last != null && last.getId().longValue() < 0)
              count --;
            break;
          }
        }
      }
    }
    return count;
  }

  /************************************************
  * Folder Fetched/Invalidated handling
  ************************************************/

  public synchronized List getFolderIDsFetched() {
    ArrayList list = new ArrayList();
    Iterator iter = fldIDsFetchRequestsIssuedSet.iterator();
    while (iter.hasNext()) {
      Long folderId = (Long) iter.next();
      list.add(folderId);
    }
    return list;
  }

  public synchronized void markFolderFetchRequestIssued(Long folderId) {
    fldIDsFetchRequestsIssuedSet.add(folderId);
  }

  public synchronized boolean wasFolderFetchRequestIssued(Long folderId) {
    return folderId != null && fldIDsFetchRequestsIssuedSet.contains(folderId);
  }

  public synchronized void markFolderViewInvalidated(Long folderId, boolean isInvalidated) {
    if (isInvalidated)
      fldIDsViewInvalidatedSet.add(folderId);
    else
      fldIDsViewInvalidatedSet.remove(folderId);
  }

  public synchronized void markFolderViewInvalidated(Collection folderIDsL, boolean isInvalidated) {
    Iterator iter = folderIDsL.iterator();
    while (iter.hasNext()) {
      markFolderViewInvalidated((Long) iter.next(), isInvalidated);
    }
  }

  public synchronized boolean wasFolderViewInvalidated(Long folderId) {
    return fldIDsViewInvalidatedSet.contains(folderId);
  }

  public synchronized void markStatFetchedForMsgId(Long msgId) {
    statsFetchedForMsgIds.put(msgId, new Long(System.currentTimeMillis()));
  }

  public synchronized boolean wasStatFetchForMsgIdRecent(Long msgId) {
    long timeout = 3 * 60 * 1000; // 3 minutes
    return statsFetchedForMsgIds.containsKey(msgId) && ((Long) statsFetchedForMsgIds.get(msgId)).longValue() > System.currentTimeMillis() - timeout;
  }

  /**************************************************
  * Staged fetching handling
  *************************************************/

  public synchronized boolean hasNextFetchRequest(Long folderId) {
    return nextFolderFetchActionsMap != null && nextFolderFetchActionsMap.containsKey(folderId);
  }

  public synchronized boolean hasNextFetchRequestOrFetching(Long folderId) {
    return hasNextFetchRequest(folderId) || isFetchingNext(folderId);
  }

  public synchronized boolean isFetchingNext(Long folderId) {
    return nextFolderFetchProgressingIDsSet != null && nextFolderFetchProgressingIDsSet.contains(folderId);
  }

  public synchronized void removeNextFetchAction(Long folderId) {
    nextFolderFetchActionsMap.remove(folderId);
  }
  public synchronized void setNextFetchAction(Long folderId, MessageAction msgAction) {
    nextFolderFetchActionsMap.put(folderId, msgAction);
  }
  public synchronized boolean sendNextFetchRequest(ServerInterfaceLayer SIL, final Long folderId, final Runnable afterJob) {
    boolean anySent = false;
    // take off the request from pending to avoid client to repedetly sending the same request
    final MessageAction msgAction = (MessageAction) nextFolderFetchActionsMap.remove(folderId);
    if (msgAction != null) {
      nextFolderFetchProgressingIDsSet.add(folderId);
      msgAction.restamp();
      // take off the request from pending to avoid client to repedetly sending the same request
      Runnable reinsert = new Runnable() {
        public void run() {
          synchronized (FetchedDataCache.this) {
            // We'll reinsert the request right after reciving a reply or interrupt.
            // Proper execution of reply will remove this action from pending.
            // After-Job handles next request so it should not reinsert.
            nextFolderFetchActionsMap.put(folderId, msgAction);
            nextFolderFetchProgressingIDsSet.remove(folderId);
          }
        }
      };
      SIL.submitAndReturn(msgAction, 120000, reinsert, afterJob, reinsert);
      anySent = true;
    }
    return anySent;
  }


  /********************************************************
  * Handling of delayed events
  *******************************************************/
  private final Object delayedMonitor = new Object();
  private HashSet delayedContactsSet = new HashSet();
  private HashSet delayedContactsRemoved = new HashSet();
  private HashSet delayedFoldersSet = new HashSet();
  private HashSet delayedFoldersRemoved = new HashSet();
  private boolean delayedTaskScheduled = false;
  private Timer delayTimer = new Timer();

  private void fireRecordsUpdated_Delayed(Record[] records, int eventType, HashSet set, HashSet removed) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(FetchedDataCache.class, "fireRecordsUpdated_Delayed(ContactRecord[] records, int eventType, HashSet set, HashSet removed)");
    if (trace != null) trace.args(records);
    if (trace != null) trace.args(eventType);
    if (trace != null) trace.args(set, removed);
    if (!DEBUG__SUPPRESS_EVENTS_CONTACTS) {
      if (records != null && records.length > 0) {
        synchronized (delayedMonitor) {
          if (eventType == RecordEvent.SET) {
            for (int i=0; i<records.length; i++) {
              Record rec = records[i];
              set.add(rec);
              removed.remove(rec);
            }
          } else if (eventType == RecordEvent.REMOVE) {
            for (int i=0; i<records.length; i++) {
              Record rec = records[i];
              removed.add(rec);
              set.remove(rec);
            }
          }
          if (!delayedTaskScheduled) {
            try {
              // create new task, because they can't be reused...
              delayTimer.schedule(new DelayedTask(), 250);
              delayedTaskScheduled = true;
            } catch (Throwable t) {
              t.printStackTrace();
            }
          }
        }
      }
    }
    if (trace != null) trace.exit(FetchedDataCache.class);
  }

  private class DelayedTask extends TimerTask {
    public void run() {
      synchronized (delayedMonitor) {
        delayedTaskScheduled = false;
        try {
          // CONTACTS
          // SET
          if (delayedContactsSet.size() > 0) {
            ContactRecord[] recs = (ContactRecord[]) ArrayUtils.toArray(delayedContactsSet, ContactRecord.class);
            Long[] ids = RecordUtils.getIDs(recs);
            // use only records still existing in the cache
            recs = getContactRecords(ids);
            fireContactRecordUpdated(recs, RecordEvent.SET);
            delayedContactsSet.clear();
          }
          // REMOVE
          if (delayedContactsRemoved.size() > 0) {
            ArrayList records = new ArrayList();
            Iterator iter = delayedContactsRemoved.iterator();
            while (iter.hasNext()) {
              ContactRecord rec = (ContactRecord) iter.next();
              // use only records REMOVED from the cache
              if (getContactRecord(rec.contactId) == null) {
                records.add(rec);
              }
            }
            if (records.size() > 0) {
              ContactRecord[] recs = (ContactRecord[]) ArrayUtils.toArray(records, ContactRecord.class);
              fireContactRecordUpdated(recs, RecordEvent.REMOVE);
            }
            delayedContactsRemoved.clear();
          }
          // FOLDERS
          // SET
          if (delayedFoldersSet.size() > 0) {
            FolderRecord[] recs = (FolderRecord[]) ArrayUtils.toArray(delayedFoldersSet, FolderRecord.class);
            Long[] ids = RecordUtils.getIDs(recs);
            // use only records still existing in the cache
            recs = getFolderRecords(ids);
            fireFolderRecordUpdated(recs, RecordEvent.SET);
            delayedFoldersSet.clear();
          }
          // REMOVE
          if (delayedFoldersRemoved.size() > 0) {
            ArrayList records = new ArrayList();
            Iterator iter = delayedFoldersRemoved.iterator();
            while (iter.hasNext()) {
              FolderRecord rec = (FolderRecord) iter.next();
              // use only records REMOVED from the cache
              if (getFolderRecord(rec.folderId) == null) {
                records.add(rec);
              }
            }
            if (records.size() > 0) {
              FolderRecord[] recs = (FolderRecord[]) ArrayUtils.toArray(records, FolderRecord.class);
              fireFolderRecordUpdated(recs, RecordEvent.REMOVE);
            }
            delayedFoldersRemoved.clear();
          }
        } catch (Throwable t) {
        }
      } // end synchronized
    }
  }

}