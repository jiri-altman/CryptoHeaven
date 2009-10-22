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

package com.CH_gui.actionGui;

import java.awt.*;
import java.awt.event.*;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.*;

import com.CH_gui.action.*;
import com.CH_gui.dialog.*;
import com.CH_gui.menuing.*;
import com.CH_gui.toolBar.DualBox_Launcher;
import com.CH_gui.tree.*;

import com.CH_cl.service.ops.*;

import com.CH_co.service.records.*;
import com.CH_co.trace.Trace;
import com.CH_co.util.*;

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
 * <b>$Revision: 1.39 $</b>
 * @author  Marcin Kurzawa
 * @version 
 */
public abstract class JActionFrame extends JFrame implements ContainerListener, ActionProducerI, VisualsSavable {

  private static Integer versionedVisualsSavable = new Integer(3);

  private static final boolean ENABLE_LOOK_AND_FEEL_CHANGE_ACTIONS = false;
  private static final boolean ENABLE_MENU_CUSTOMIZATION_ACTION = false;

  protected MenuTreeModel menuTreeModel;
  protected ToolBarModel toolBarModel;

  private Action[] actions;

  private boolean isPackingSize = false;

  // title visual notification variables
  private String frameDefaultTitle = "";
  private String frameTitle = "";
  private Timer notifyTimer;
  private TitleUpdateNotifier updateNotifier;
  private boolean isWindowDeactivated = true;

  private FolderTreeComponent mainTreeComp;

  private Dimension lastNormalSize;
  private Point lastNormalLocation;

  /*****************************/
  /** Creates new JActionFrame */
  /*****************************/
  public JActionFrame(String title, boolean withMenuBar, boolean withToolBar) {
    super(title);
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(JActionFrame.class, "JActionFrame(String title, boolean withMenuBar, boolean withToolBar)");
    if (trace != null) trace.args(title);
    if (trace != null) trace.args(withMenuBar);
    if (trace != null) trace.args(withToolBar);
    frameDefaultTitle = title;
    frameTitle = title;

    if (withMenuBar) {
      if (trace != null) trace.data(10, "creating menu bar");
      menuTreeModel = new MenuTreeModel(MiscGui.getVisualsKeyName(this));
      setJMenuBar(menuTreeModel.getMenuBar());
    }
    if (withToolBar) {
      if (trace != null) trace.data(20, "creating tool bar");
      toolBarModel = new ToolBarModel(MiscGui.getVisualsKeyName(this), java.text.MessageFormat.format(com.CH_gui.lang.Lang.rb.getString("title_Toolbar"), new Object[] {title}));
    }

    Container contentPane = getContentPane();
    if (toolBarModel != null) {
      if (trace != null) trace.data(30, "placing tool bar");
      String placement = GlobalProperties.getProperty(MiscGui.getVisualsKeyName(this) + "_toolBarPosition", BorderLayout.NORTH);
      contentPane.add(toolBarModel.getToolBar(), placement);
    }

    if (trace != null) trace.data(40, "itself as container listener");
    contentPane.addContainerListener(this);


    if (menuTreeModel != null) {
      if (trace != null) trace.data(50, "menu not null, add mouse popup listener");
      // Default Popup menu creator.
      // When popup triger click is done on the general area in the frame, 
      // the deepest action producer at that spot should produce be asked to produce its own menu,
      // without registering that action producer with a seperate mouse adapter.
      // This doesn't seem to work in general as the event seems to be consumed before this
      // mouse adapter gets to act on it.  However, it works when clicking on the menu bar outside
      // of menu areas.
      addMouseListener(new MouseAdapter() { 
        public void mouseClicked(MouseEvent e) {
          Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(MouseAdapter.class, "mouseClicked(MouseEvent e)");
          if (trace != null) trace.args(e);
          // TO-DO: ????? popup triggering mouse clicks don't work!!!!
          // if (e.isPopupTrigger()) {
          if (!e.isConsumed() && SwingUtilities.isRightMouseButton(e)) {
            if (trace != null) trace.data(10, "popupTrigger");
            Component c = e.getComponent();
            Component deepestComponent = SwingUtilities.getDeepestComponentAt(c, e.getX(), e.getY());
            if (trace != null) trace.info(11, "deepestComponent", deepestComponent);

            while (!(deepestComponent instanceof ActionProducerI)) {
              deepestComponent = deepestComponent.getParent();
            }

            MouseEvent deepEvent = SwingUtilities.convertMouseEvent(c, e, deepestComponent);

            Window window = null;
            if (c instanceof JActionFrame)
              window = (JActionFrame) c;
            else
              window = SwingUtilities.windowForComponent(deepestComponent);
            if (trace != null) trace.data(20, window);
            if (window instanceof JActionFrame) {
              JActionFrame jActionFrame = (JActionFrame) window;
              Action[] actions = ActionUtils.getActionsRecursively(deepestComponent);
              JPopupMenu jPopup = jActionFrame.getMenuTreeModel().generatePopup(actions);
              if (jPopup != null) {
                // consume original event
                e.consume();
                // wrong-popup-location
                // Point point = MiscGui.getAdjustedPopupLocation(deepestComponent, jPopup, e.getX(), e.getY());
                // jPopup.show(deepestComponent, point.x, point.y);
                jPopup.show(deepestComponent, deepEvent.getX(), deepEvent.getY());
              }
            }
          }
          if (trace != null) trace.exit(MouseAdapter.class);
        }
      });
    } // if menuTreeModel != null


    if (trace != null) trace.data(100, "addint all actions to menus");
    // also add actions generated by this frame and its hierarchy
    addComponentActions(this);

    //setIconImage (ideIcon.getImage ());
    setDefaultCloseOperation (DO_NOTHING_ON_CLOSE);

    ImageIcon frameIcon = Images.get(ImageNums.FRAME_LOCK32);
    if (frameIcon != null) {
      setIconImage(frameIcon.getImage());
    }
    if (trace != null) trace.data(110, "restoring visuals");
    restoreVisuals(GlobalProperties.getProperty(MiscGui.getVisualsKeyName(this)));

    if (trace != null) trace.data(120, "adding window listener for activations");
    addWindowListener(new WindowAdapter() {
      public void windowActivated(WindowEvent e) {
        isWindowDeactivated = false;
        synchronized (JActionFrame.this) {
          if (updateNotifier != null) {
            updateNotifier.windowActivated();
          }
        }

        // try to get the menu bar to show when window changes state from ICONIFIED to NORMAL -- workaround a bug in Java
        invalidate();
        validate(); // this used to throw exceptions when rendering HTML panes and freeze the GUI
        repaint();

      }
      public void windowDeactivated(WindowEvent e) {
        isWindowDeactivated = true;
      }
      public void windowIconified(WindowEvent e) {
      }
    });

    addComponentListener(new ComponentListener() {
      public void componentHidden(ComponentEvent e) {
        noteNormalStateVisuals();
      }
      public void componentMoved(ComponentEvent e) {
        noteNormalStateVisuals();
      }
      public void componentResized(ComponentEvent e) {
        noteNormalStateVisuals();
      }
      public void componentShown(ComponentEvent e) {
        noteNormalStateVisuals();
      }
      private void noteNormalStateVisuals() {
        if (isNormalState()) {
          lastNormalSize = getSize();
          lastNormalLocation = getLocation();
        }
      }
    });

    if (trace != null) trace.data(130, "frame creation done");
    if (trace != null) trace.exit(JActionFrame.class);
  }

  private boolean isNormalState() {
    boolean normalState = getState() == Frame.NORMAL;
    if (normalState) {
      Integer extendedState = null;
      Integer maxBoth = null;
      Integer maxHoriz = null;
      Integer maxVert = null;
      java.lang.reflect.Method getExtendedState = null;
      java.lang.reflect.Field maxB = null;
      java.lang.reflect.Field maxH = null;
      java.lang.reflect.Field maxV = null;
      try {
        getExtendedState = JActionFrame.this.getClass().getMethod("getExtendedState", null);
        maxB = JActionFrame.this.getClass().getField("MAXIMIZED_BOTH");
        maxH = JActionFrame.this.getClass().getField("MAXIMIZED_HORIZ");
        maxV = JActionFrame.this.getClass().getField("MAXIMIZED_VERT");

        if (getExtendedState != null) {
          extendedState = (Integer) getExtendedState.invoke(JActionFrame.this, null);
        }
        if (maxB != null) {
          maxBoth = new Integer(maxB.getInt(JActionFrame.this));
        }
        if (maxH != null) {
          maxHoriz = new Integer(maxH.getInt(JActionFrame.this));
        }
        if (maxV != null) {
          maxVert = new Integer(maxV.getInt(JActionFrame.this));
        }
      } catch (Throwable t) {
      }

      if (extendedState != null) {
        if (extendedState.equals(maxBoth) || extendedState.equals(maxHoriz) || extendedState.equals(maxVert)) {
          normalState = false;
        }
      }
    }
    return normalState;
  }
  private int getXState() {
    Integer extendedState = null;
    java.lang.reflect.Method getExtendedState = null;
    try {
      getExtendedState = Frame.class.getMethod("getExtendedState", null);
      if (getExtendedState != null) {
        extendedState = (Integer) getExtendedState.invoke(JActionFrame.this, null);
      }
    } catch (Throwable t) {
    }
    return extendedState != null ? extendedState.intValue() : -1;
  }
  private void setXState(int xState) {
    if (xState != -1) {
      java.lang.reflect.Method setExtendedState = null;
      try {
        setExtendedState = Frame.class.getMethod("setExtendedState", new Class[] { Integer.TYPE });
        if (setExtendedState != null) {
          setExtendedState.invoke(JActionFrame.this, new Object[] { new Integer(xState) });
        }
      } catch (Throwable t) {
      }
    }
  }

  public void setMainTreeComponent(FolderTreeComponent treeComp) {
    mainTreeComp = treeComp;
  }
  public FolderTreeComponent getMainTreeComponent() {
    return mainTreeComp;
  }

  public MenuTreeModel getMenuTreeModel() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(JActionFrame.class, "getMenuTreeModel()");
    if (trace != null) trace.exit(JActionFrame.class, menuTreeModel);
    return menuTreeModel;
  }

  public ToolBarModel getToolBarModel() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(JActionFrame.class, "getToolBarModel()");
    if (trace != null) trace.exit(JActionFrame.class, toolBarModel);
    return toolBarModel;
  }


  /**
   * Default exit operation is to save frame properties and visual preferences
   * into the GlobalProperties.
   */
  public void saveFrameProperties() {
    MiscGui.storeVisualsSavable(this);
    if (menuTreeModel != null)
      menuTreeModel.putMenuProperties();
    if (toolBarModel != null) {
      toolBarModel.putToolBarProperties();

      // Get tool bar position (ie: NORTH, SOUTH, EAST, WEST).
      // Don't know how to do this, we will assume that horizontal allignment means NORTH, vertical means WEST.
      // TO-DO: Learn how to determine placement of toolbar (ie: NORTH, WEST, EAST, SOUTH) to save it into properties.
      /*
      Container contentPane = getContentPane();
      BorderLayout l = (BorderLayout) contentPane.getLayout();
      System.out.println("BorderLayout="+l.toString());
      for (int i=0; i<10; i++) {
        try {
          System.out.println(""+i+" "+contentPane.getComponent(i));
        } catch (Throwable t) {
        }
      }
       */
      JToolBar bar = toolBarModel.getToolBar();
      int orientation = bar.getOrientation();
      String placement = BorderLayout.NORTH;
      if (orientation == JToolBar.HORIZONTAL)
        placement = BorderLayout.NORTH;
      else if (orientation == JToolBar.VERTICAL)
        placement = BorderLayout.WEST;
      GlobalProperties.setProperty(MiscGui.getVisualsKeyName(this) + "_toolBarPosition", placement);
    }
  }

  public void addMainComponent(Component c) {
    Container contentPane = getContentPane();
    contentPane.add(c, BorderLayout.CENTER);
  }

  private void initActions() {
    int leadingActionId = Actions.LEADING_ACTION_ID_JACTION_FRAME;

    UIManager.LookAndFeelInfo looks[] = null;
    if (ENABLE_LOOK_AND_FEEL_CHANGE_ACTIONS)
      looks = UIManager.getInstalledLookAndFeels();

    // tool tips 
    int numActions = 1;
    // + L&F
    if (ENABLE_LOOK_AND_FEEL_CHANGE_ACTIONS)
      numActions += looks.length;
    // + customize toolbar
    if (toolBarModel != null)
      numActions ++;
    // + customize menus
    if (ENABLE_MENU_CUSTOMIZATION_ACTION) {
      if (menuTreeModel != null)
        numActions ++;
    }

    actions = new Action[numActions];

    // Lets leave the tool tips exclusively for the main frame.
    if ( !(this instanceof JActionFrameClosable) )
      actions[0] = new ToolTipsAction(leadingActionId);

    if (toolBarModel != null)
      actions[1] = new CustomizeToolsAction(leadingActionId+1);

    if (ENABLE_MENU_CUSTOMIZATION_ACTION) {
      if (menuTreeModel != null)
        actions[2] = new CustomizeMenuAction(leadingActionId+2);
    }

    if (ENABLE_LOOK_AND_FEEL_CHANGE_ACTIONS) {
      ButtonGroup lookAndFeelGroup = new ButtonGroup();
      for (int i=0, n=looks.length; i<n; i++) {
        actions[numActions-1 -i] = new LookAndFeelAction(looks[i], leadingActionId+10+i, lookAndFeelGroup);
      }
    }

  }


  /** Display a dialog so the user can customize the tool bar **/
  private class CustomizeToolsAction extends AbstractAction {
    public CustomizeToolsAction(int actionId) {
      super(com.CH_gui.lang.Lang.rb.getString("action_Customize_Toolbar_..."), Images.get(ImageNums.TOOLS16));
      putValue(Actions.TOOL_TIP, com.CH_gui.lang.Lang.rb.getString("actionTip_Customize_Toolbar"));
      putValue(Actions.ACTION_ID, new Integer(actionId));
      putValue(Actions.TOOL_ICON, Images.get(ImageNums.TOOLS24));
      putValue(Actions.IN_TOOLBAR, Boolean.FALSE);
    }
    public void actionPerformed(ActionEvent event) {
      new DualBox_Launcher(JActionFrame.this, toolBarModel);
    }
  }


  /** Display a dialog so the user can customize the menu bar **/
  private class CustomizeMenuAction extends AbstractAction {
    public CustomizeMenuAction(int actionId) {
      super(com.CH_gui.lang.Lang.rb.getString("action_Customize_Menus_..."), Images.get(ImageNums.TOOLS16));
      putValue(Actions.TOOL_TIP, com.CH_gui.lang.Lang.rb.getString("actionTip_Customize_Menu_Bar"));
      putValue(Actions.ACTION_ID, new Integer(actionId));
      putValue(Actions.TOOL_ICON, Images.get(ImageNums.TOOLS24));
    }
    public void actionPerformed(ActionEvent event) {
      new MenuEditorDialog(JActionFrame.this);
    }
  }


  private static class ToolTipsAction extends AbstractAction {
    public ToolTipsAction(int actionId) {
      super(com.CH_gui.lang.Lang.rb.getString("action_Display_Tool_Tips"));
      putValue(Actions.TOOL_TIP, com.CH_gui.lang.Lang.rb.getString("actionTip_Enable/Disable_Tool_Tip_help"));
      putValue(Actions.ACTION_ID, new Integer(actionId));
      putValue(Actions.STATE_CHECK, Boolean.TRUE);
      putValue(Actions.IN_TOOLBAR, Boolean.FALSE);
    }
    public void actionPerformed(ActionEvent event) {
      AbstractButton button = (AbstractButton) event.getSource();
      ToolTipManager.sharedInstance().setEnabled(button.isSelected());
    }
  }

  private class LookAndFeelAction extends AbstractAction {
    private String lafClassName;
    public LookAndFeelAction(UIManager.LookAndFeelInfo look, int actionId, ButtonGroup group) {
      super(look.getName());
      this.lafClassName = look.getClassName();
      putValue(Actions.TOOL_TIP, com.CH_gui.lang.Lang.rb.getString("actionTip_Change_the_application_Look_And_Feel"));
      putValue(Actions.ACTION_ID, new Integer(actionId));
      // set "selected" only if the current L&F is the one specified
      putValue(Actions.SELECTED_RADIO, Boolean.valueOf(UIManager.getLookAndFeel().getName().equals(look.getName())));
      putValue(Actions.BUTTON_GROUP, group);
      putValue(Actions.GENERATED_NAME, Boolean.TRUE);
      putValue(Actions.IN_TOOLBAR, Boolean.FALSE);
    }
    public void actionPerformed(ActionEvent actionEvent) {
      final String finalLafClassName = lafClassName;
      Runnable runnable = new Runnable() {
        public void run() {
          Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(getClass(), "run()");
          try {
            UIManager.setLookAndFeel(finalLafClassName);
            SwingUtilities.updateComponentTreeUI(JActionFrame.this);
          } catch (Exception exception) {
            String messageText = com.CH_gui.lang.Lang.rb.getString("msg_Can't_change_look_and_feel");
            String title = com.CH_gui.lang.Lang.rb.getString("msgTitle_Invalid_PLAF");
            MessageDialog.showErrorDialog(JActionFrame.this, messageText, title);
          }
          // Runnable, not a custom Thread -- DO NOT clear the trace stack as it is run by the AWT-EventQueue Thread.
          if (trace != null) trace.exit(getClass());
        }
      };
      SwingUtilities.invokeLater(runnable);
    }
  }


  /**
   * ComponentListener interface method.
   * When component has been added, add its actions.
   */
  public void componentAdded(final ContainerEvent event) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(JActionFrame.class, "componentAdded(ContainerEvent event)");
    if (trace != null) trace.args(event);
    addComponentActions(event.getChild());
    if (trace != null) trace.exit(JActionFrame.class);
  }

  /**
   * ComponentListener interface method.
   * When component has been added, DISABLE its actions.
   */
  public void componentRemoved(final ContainerEvent event) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(JActionFrame.class, "componentRemoved(ContainerEvent event)");
    if (trace != null) trace.args(event);
    removeComponentActions(event.getChild());
    if (trace != null) trace.exit(JActionFrame.class);
  }

  /**
   * Add menus and tools.
   * @return true if new component causes addition of menus or tools
   */
  public boolean addComponentActions(Component source) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(JActionFrame.class, "addComponentActions(Component)");
    if (trace != null) trace.args(source);
    if (trace != null) trace.args(Misc.getClassNameWithoutPackage(source.getClass()));

    Action[] actionArray = ActionUtils.getActionsRecursively(source);
    if (menuTreeModel != null)
      menuTreeModel.addActions(actionArray);
    if (toolBarModel != null)
      toolBarModel.addActions(actionArray);

    // When component is added, make sure the state of actions is updated.
    ActionUtils.setEnabledActionsRecur(source);

    boolean newMenus = actionArray != null && actionArray.length > 0;

    if (trace != null) trace.exit(JActionFrame.class, newMenus);
    return newMenus;
  }


  /**
    * This method does the actual work of removing menus and menu items when
    * a component is removed.  Removal == disabling menu items!!!
    * @return true if the component causes removal of any menus items
    */
  public boolean removeComponentActions(Component source) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(JActionFrame.class, "removeComponentActions(Component source)");
    if (trace != null) trace.args(source);
    if (trace != null) trace.args(Misc.getClassNameWithoutPackage(source.getClass()));

    Action[] actionArray = ActionUtils.getActionsRecursively(source);
    if (menuTreeModel != null)
      menuTreeModel.removeActions(actionArray);
    if (toolBarModel != null)
      toolBarModel.removeActions(actionArray);

    // When component is removed, make sure the state of actions is disabled.
    ActionUtils.disableAllActions(source);

    boolean newMenus = actionArray != null && actionArray.length > 0;

    if (trace != null) trace.exit(JActionFrame.class, newMenus);
    return newMenus;
  }

  /**
   * @param forComponent is the changed component, if null, rebuild entire Frame's actions
   * It does not remove the menu actions if they are no longer present.... just disables.
   */
  public void rebuildAllActions(Component forComponent) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(JActionFrame.class, "rebuildComponentActions()");

    Component c = null;
    if (forComponent == null) {
      c = getContentPane();
    } else {
      c = forComponent;
    }
    removeComponentActions(c);
    addComponentActions(c);

    // set enablement
    ActionUtils.setEnabledActionsRecur(c);

    if (trace != null) trace.exit(JActionFrame.class);
  }

  /**
   * Discards the current Menu Bar and rebuilds a new one.
   * Used after Customize Menus action -- currently unused as this function has been taken out of menus.
   */
  public void reconstructMenusFromScratch() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(JActionFrame.class, "reconstructMenusFromScratch()");

    if (menuTreeModel != null) {
      menuTreeModel.rebuildMenuBar();
      //addComponentActions(this);
      Action[] actionArray = ActionUtils.getActionsRecursively(this);
      if (menuTreeModel != null)
        menuTreeModel.addActions(actionArray);
      // set enablement
      ActionUtils.setEnabledActionsRecur(this);
    }

    if (trace != null) trace.exit(JActionFrame.class);
  }


  public void dispose() {
    // Remove and dissasemble menus.
    try {
      if (menuTreeModel != null) {
        menuTreeModel.clear();
        menuTreeModel = null;
      }
    } catch (Throwable t) { }
    // Remove and dissasemble toolbar.
    try {
      if (toolBarModel != null) {
        toolBarModel.clear();
        toolBarModel = null;
      }
    } catch (Throwable t) { }
    // remove and dissasemble all window containers
    try {
      MiscGui.removeAllComponentsAndListeners(this);
    } catch (Throwable t) { }

    // Try twice to dispose the screen resources, it sometimes fails when disposing owner frames to some
    // dialogs while they are still up (with NullPointerException)
    // So, retry after a delay.  There must be a bug in the java GUI.
    try {
      try { Thread.sleep(100); } catch (InterruptedException e) { }
      super.dispose();
    } catch (Throwable t1) {
      try {
        try { Thread.sleep(1000); } catch (InterruptedException e) { }
        super.dispose();
      } catch (Throwable t2) { }
    }
  }


  /****************************************************************************/
  /*        A c t i o n P r o d u c e r I                                  
  /****************************************************************************/
  /** 
   * ActionProducerI interface method.
   * @return all the acitons that this objects produces.
   */
  public Action[] getActions() {
    if (actions == null)
      initActions();
    return actions;
  }
  /**
   * ActionProducerI interface method.
   * Final Action Producers will not be traversed to collect its containing objects' actions.
   * @return true if this object will gather all actions from its childeren or hide them counciously.
   */
  public boolean isFinalActionProducer() {
    return false;
  }

  /**
   * Enables or Disables actions based on the current state of the Action Producing component.
   */
  public void setEnabledActions() {
  }


  /*******************************************************
  *** V i s u a l s S a v a b l e    interface methods ***
  *******************************************************/
  public String getVisuals() {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(JActionFrame.class, "getVisuals()");

    StringBuffer visuals = new StringBuffer();
    visuals.append("Dimension width ");
    Dimension dim = lastNormalSize != null ? lastNormalSize : getSize();
    visuals.append(dim.width);
    visuals.append(" height ");
    visuals.append(dim.height);

    Point p = lastNormalLocation != null ? lastNormalLocation : getLocation();
    visuals.append(" Location x ");
    visuals.append(p.x);
    visuals.append(" y ");
    visuals.append(p.y);

    int state = getState();
    int xState = getXState();
    visuals.append(" State ");
    visuals.append(state);
    visuals.append(" ExtendedState ");
    visuals.append(xState);

    String rc = visuals.toString();
    if (trace != null) trace.exit(JActionFrame.class, rc);
    return rc;
  }

  public void restoreVisuals(String visuals) {
    Trace trace = null;  if (Trace.DEBUG) trace = Trace.entry(JActionFrame.class, "restoreVisuals(String visuals)");
    if (trace != null) trace.args(visuals);

    try {
      if (visuals == null) {
        pack();
        isPackingSize = true;
      } else {
        StringTokenizer st = new StringTokenizer(visuals);  
        st.nextToken();
        st.nextToken();
        int width = Integer.parseInt(st.nextToken());
        st.nextToken();
        int height = Integer.parseInt(st.nextToken());

        // setSize is required for setLocation
        setSize(width, height);

        st.nextToken();
        st.nextToken();
        int x = Integer.parseInt(st.nextToken());
        st.nextToken();
        int y = Integer.parseInt(st.nextToken());
        setLocation(x, y);

        // adjust the size the 2nd time to make sure window is not greater than desktop size
        Dimension screenSize = MiscGui.getScreenUsableSize(x, y, this);
        boolean adjusted = false;
        if (screenSize.width >= 300 && screenSize.width < width) {
          width = screenSize.width;
          adjusted = true;
        }
        if (screenSize.height >= 200 && screenSize.height < height) {
          height = screenSize.height;
          adjusted = true;
        }
        if (adjusted)
          setSize(width, height);

        if (st.hasMoreTokens()) {
          st.nextToken();
          int state = Integer.parseInt(st.nextToken());
          st.nextToken();
          int xState = Integer.parseInt(st.nextToken());
          setState(state);
          setXState(xState);
        }
      }
    } catch (Throwable t) {
      if (trace != null) trace.exception(JActionFrame.class, 100, t);
    }

    if (trace != null) trace.exit(JActionFrame.class);
  }
  public String getExtension() {
    return null;
  }
  public Integer getVisualsVersion() {
    return versionedVisualsSavable;
  }
  public boolean isVisuallyTraversable() {
    return isNormalState();
  }


  /**
   * Overwrite super.setVisible() to pack the frame when it is first shown and no visuals were saved.
   */
  private boolean wasShown = false;
  public void setVisible(boolean b) {
    if (b && !wasShown) {
      wasShown = true;
      if (isPackingSize) {
        pack();
      }
      // see if we should spread the window location
      Point p = JActionFrameClosable.getSpreadWindowLocation(this);
      if (p != null) {
        setLocation(p);
        GlobalProperties.setProperty(MiscGui.getVisualsKeyName(this), getVisuals());
      }
    }
    super.setVisible(b);
  }


  public void setUserTitle(UserRecord uRec) {
    if (uRec != null) {
      String emailStr = "";
      if (!uRec.isWebAccount()) {
        String[] emailStrings = UserOps.getCachedDefaultEmail(uRec, false);
        emailStr = emailStrings != null ? " :: " + emailStrings[2] : "";
      }
      setTitle(getDefaultTitle() + " :: " + uRec.shortInfo() + emailStr);
    } else {
      setTitle(getDefaultTitle());
    }
  }

  /**
   * Overwrite super.setTitle() to accomodate visual update notifications through title animations.
   */
  public synchronized void setTitle(String title) {
    if (title != null) {
      if (!title.endsWith(" "))
        frameTitle = title + " ";
      else
        frameTitle = title;
    }
    super.setTitle(title);
  }
  public synchronized String getDefaultTitle() {
    return frameDefaultTitle;
  }
  public synchronized String getTitle() {
    return frameTitle;
  }
  public synchronized void triggerVisualUpdateNotificationAnim(String temporaryTitle, String titleAppendix, int millis) {
    triggerVisualUpdateNotification(temporaryTitle, titleAppendix, millis, TitleUpdateNotifier.MODE_ANIM);
  }
  public synchronized void triggerVisualUpdateNotificationStill(String temporaryTitle, String titleAppendix, int millis) {
    triggerVisualUpdateNotification(temporaryTitle, titleAppendix, millis, TitleUpdateNotifier.MODE_STILL);
  }
  private synchronized void triggerVisualUpdateNotification(String temporaryTitle, String titleAppendix, int millis, int mode) {
    if (temporaryTitle != null && millis > 0) {
      if (notifyTimer == null) {
        updateNotifier = new TitleUpdateNotifier();
        notifyTimer = new Timer(50, updateNotifier);
      }
      updateNotifier.addJob(mode, temporaryTitle, titleAppendix, System.currentTimeMillis()+millis, 50, false);
      if (!notifyTimer.isRunning()) notifyTimer.start();
    }
  }
  public synchronized void triggerVisualUpdateNotificationRoll() {
    triggerVisualUpdateNotificationRoll("   *new*");
  }
  public synchronized void triggerVisualUpdateNotificationRoll(String titleAppendix) {
    if (isWindowDeactivated) {
      if (notifyTimer == null) {
        updateNotifier = new TitleUpdateNotifier();
        notifyTimer = new Timer(100, updateNotifier);
      }
      updateNotifier.addJob(TitleUpdateNotifier.MODE_ROLL, null, titleAppendix, TitleUpdateNotifier.RUN_TIME__UNSPECIFIED, 100, true);
      if (!notifyTimer.isRunning()) notifyTimer.start();
    }
  }
  private class TitleUpdateNotifier implements ActionListener {
    private static final int MODE_ROLL = 0;
    private static final int MODE_ANIM = 1;
    private static final int MODE_STILL = 2;
    private static final int RUN_TIME__UNSPECIFIED = 0;
    private Vector jobsV;
    private synchronized void windowActivated() {
      // Go through all the jobs and remove them if they are sensitive to window activation.
      if (jobsV != null) {
        for (int i=jobsV.size()-1; i>=0; i--) {
          Settings state = (Settings) jobsV.elementAt(i);
          if (state.stopOnWindowActivation)
            jobsV.removeElementAt(i);
        }
      }
    }
    private synchronized void addJob(int mode, String temporaryTitle, String titleAppendix, long expiryTime, int delay, boolean stopOnWindowActivation) {
      if (jobsV == null) jobsV = new Vector();
      Settings settings = new Settings();
      settings.runningMode = mode;
      settings.tempTitle = temporaryTitle;
      settings.titleAppendix = titleAppendix;
      settings.expiryTime = expiryTime;
      settings.delay = delay;
      settings.stopOnWindowActivation = stopOnWindowActivation;
      jobsV.addElement(settings);
    }
    private synchronized Settings getJob() {
      Settings state = (Settings) (jobsV != null && jobsV.size() > 0 ? jobsV.lastElement() : null);
      return state;
    }
    private synchronized void removeJob() {
      jobsV.removeElementAt(jobsV.size()-1);
    }
    public synchronized void actionPerformed(ActionEvent event) {
      String newTitle = frameTitle;
      Settings state = getJob();
      if (state == null) {
        synchronized (JActionFrame.this) {
          if (notifyTimer != null)
            notifyTimer.stop();
        }
      } else {
        long time = System.currentTimeMillis();
        boolean jobsRemoved = false;
        // remove all jobs which expired or otherwise are invalid
        while (state != null && (
            (!isWindowDeactivated && state.stopOnWindowActivation) ||
            (state.expiryTime < time && state.expiryTime != RUN_TIME__UNSPECIFIED)
            ))
        {
          jobsRemoved = true;
          removeJob();
          state = getJob();
        }
        // if we have a job, do it
        if (state != null) {
          // Adjust the delay timer to the current job.
          synchronized (JActionFrame.this) {
            if (notifyTimer != null) {
              int currentDelay = notifyTimer.getDelay();
              if (currentDelay != state.delay)
                notifyTimer.setDelay(state.delay);
            }
          }

          if (state.runningMode == MODE_ROLL)
            newTitle = roll(state, (state.tempTitle != null ? state.tempTitle : frameTitle) + " ");
          else if (state.runningMode == MODE_ANIM)
            newTitle = anim(state, state.tempTitle != null ? state.tempTitle : frameTitle);
          else if (state.runningMode == MODE_STILL) 
            newTitle = state.tempTitle != null ? state.tempTitle : frameTitle;
        }
      }
      JActionFrame.super.setTitle(newTitle);
    }
    private synchronized String anim(Settings state, String str) {
      int nextNotifyState = 0;
      if (state.notifyState == 0 || state.notifyState == str.length()) state.pauseState *= -1;
      if (state.pauseState == 0) state.pauseState = 1;
      String newStr = caps(state.notifyState, str);
      if (state.titleAppendix != null)
        newStr += state.titleAppendix;
      if (state.pauseState == 1) {
        nextNotifyState = (state.notifyState+1) % (str.length()+1);
      } else if (state.pauseState == -1) {
        nextNotifyState = (state.notifyState-1) % (str.length()+1);
      }
      state.notifyState = nextNotifyState;
      return newStr;
    }
    private synchronized String roll(Settings state, String str) {
      String wrapped = wrap(state.notifyState, str.toUpperCase());
      String newStr = wrapped;//java.text.MessageFormat.format(com.CH_gui.lang.Lang.rb.getString("title_WRAPPED_TITLE_WITH"), new Object[] {wrapped});
      if (state.titleAppendix != null)
        newStr += state.titleAppendix;
      if (str.length() == 0)
        return newStr;
      int nextNotifyState = (state.notifyState+1) % str.length();
      if (nextNotifyState == 0) {
        if (state.pauseState > 6)
          state.notifyState = state.pauseState = 0;
        else {
          if (state.pauseState % 4 == 2 || state.pauseState % 4 == 3) {
            //newTitle = "";
          }
          state.pauseState ++;
        }
      } else {
        state.notifyState = nextNotifyState;
      }
      return newStr;
    }
    private String caps(int offset, String source) {
      int len = source.length();
      if (len == 0)
        return source;
      if (offset > len) offset %= len; // make sure its in a valid range
      if (offset == 0)
        return source.toLowerCase();
      if (offset == len)
        return source.toUpperCase();
      String pre = source.substring(0, offset).toUpperCase();
      String post = source.substring(offset).toLowerCase();
      return pre + post;
    }
    private String wrap(int offset, String source) {
      int len = source.length();
      if (len == 0)
        return source;
      if (offset > len) offset %= len; // make sure its in a valid range
      if (offset == 0 || offset == len-1)
        return source;
      String pre = source.substring(0, offset);
      String post = source.substring(offset);
      return post + pre;
    }
  }
  private static class Settings {
    private int pauseState;
    private int notifyState;
    private int runningMode;
    private long expiryTime;
    private String tempTitle;
    private String titleAppendix;
    private int delay;
    private boolean stopOnWindowActivation;
  }

}