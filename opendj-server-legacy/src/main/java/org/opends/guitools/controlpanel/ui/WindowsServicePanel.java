/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2008-2009 Sun Microsystems, Inc.
 * Portions Copyright 2014-2016 ForgeRock AS.
 */

package org.opends.guitools.controlpanel.ui;

import static org.opends.messages.AdminToolMessages.*;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.forgerock.i18n.LocalizableMessage;
import org.opends.guitools.controlpanel.datamodel.ControlPanelInfo;
import org.opends.guitools.controlpanel.event.ConfigurationChangeEvent;
import org.opends.guitools.controlpanel.task.Task;
import org.opends.guitools.controlpanel.util.Utilities;
import org.opends.server.tools.ConfigureWindowsService;

/** The panel that displays the Windows Service panel configuration for the server. */
public class WindowsServicePanel extends StatusGenericPanel
{
  private static final long serialVersionUID = 6415350296295459469L;
  private JLabel lState;
  private JButton bEnable;
  private JButton bDisable;

  private boolean previousLocal = true;

  private boolean isWindowsServiceEnabled;

  /** Default constructor. */
  public WindowsServicePanel()
  {
    super();
    createLayout();
  }

  @Override
  public LocalizableMessage getTitle()
  {
    return INFO_CTRL_PANEL_WINDOWS_SERVICE_TITLE.get();
  }

  /** Creates the layout of the panel (but the contents are not populated here). */
  private void createLayout()
  {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.WEST;
    gbc.weightx = 0.0;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 4;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.BOTH;

    String text = INFO_CTRL_PANEL_WINDOWS_SERVICE_PANEL_TEXT.get().toString();

    JEditorPane pane = Utilities.makeHtmlPane(text,
        ColorAndFontConstants.defaultFont);

    Utilities.updatePreferredSize(pane, 100, text,
        ColorAndFontConstants.defaultFont, false);
    gbc.weighty = 0.0;
    add(pane, gbc);

    gbc.gridy = 1;
    gbc.gridwidth = 1;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    JLabel lWindowsService =Utilities.createPrimaryLabel(
        INFO_CTRL_PANEL_WINDOWS_SERVICE_INTEGRATION_LABEL.get());
    gbc.insets.top = 10;
    add(lWindowsService, gbc);
    lState = Utilities.createDefaultLabel();
    lState.setText(isWindowsServiceEnabled ?
        INFO_ENABLED_LABEL.get().toString() :
          INFO_DISABLED_LABEL.get().toString());
    gbc.insets.left = 10;
    gbc.gridx = 1;
    add(lState, gbc);

    bEnable = Utilities.createButton(
        INFO_CTRL_PANEL_ENABLE_WINDOWS_SERVICE_BUTTON.get());
    bDisable = Utilities.createButton(
        INFO_CTRL_PANEL_DISABLE_WINDOWS_SERVICE_BUTTON.get());
    bEnable.setOpaque(false);
    bDisable.setOpaque(false);
    int maxWidth = Math.max(bEnable.getPreferredSize().width,
        bDisable.getPreferredSize().width);
    int maxHeight = Math.max(bEnable.getPreferredSize().height,
        bDisable.getPreferredSize().height);
    bEnable.setPreferredSize(new Dimension(maxWidth, maxHeight));
    bDisable.setPreferredSize(new Dimension(maxWidth, maxHeight));

    ActionListener listener = new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent ev)
      {
        updateWindowsService();
      }
    };
    bEnable.addActionListener(listener);
    bDisable.addActionListener(listener);

    gbc.gridx = 2;
    add(bEnable, gbc);
    add(bDisable, gbc);

    gbc.weightx = 1.0;
    gbc.gridx = 3;
    add(Box.createHorizontalGlue(), gbc);

    bEnable.setVisible(!isWindowsServiceEnabled);
    bDisable.setVisible(isWindowsServiceEnabled);
    addBottomGlue(gbc);
  }

  @Override
  public GenericDialog.ButtonType getButtonType()
  {
    return GenericDialog.ButtonType.CLOSE;
  }

  @Override
  public Component getPreferredFocusComponent()
  {
    if (!isWindowsServiceEnabled)
    {
      return bEnable;
    }
    else
    {
      return bDisable;
    }
  }

  @Override
  public void configurationChanged(ConfigurationChangeEvent ev)
  {
    boolean previousValue = isWindowsServiceEnabled;
    isWindowsServiceEnabled = ev.getNewDescriptor().isWindowsServiceEnabled();

    final boolean isLocal = ev.getNewDescriptor().isLocal();
    if (isLocal != previousLocal || isWindowsServiceEnabled != previousValue)
    {
      previousLocal = isLocal;
      SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          lState.setText(isWindowsServiceEnabled ?
              INFO_ENABLED_LABEL.get().toString() :
                INFO_DISABLED_LABEL.get().toString());
          bEnable.setVisible(!isWindowsServiceEnabled);
          bDisable.setVisible(isWindowsServiceEnabled);

          if (!isLocal)
          {
            displayErrorMessage(INFO_CTRL_PANEL_SERVER_REMOTE_SUMMARY.get(),
            INFO_CTRL_PANEL_SERVER_MUST_BE_LOCAL_WINDOWS_SERVICE_SUMMARY.get());
            packParentDialog();
          }
          else
          {
            displayMainPanel();
          }
        }
      });
    }
  }

  @Override
  public void okClicked()
  {
    // NO ok button
  }

  private void updateWindowsService()
  {
    LinkedHashSet<LocalizableMessage> errors = new LinkedHashSet<>();
    ProgressDialog progressDialog = new ProgressDialog(
        Utilities.createFrame(), Utilities.getParentDialog(this), getTitle(),
        getInfo());
    WindowsServiceTask newTask = new WindowsServiceTask(getInfo(),
        progressDialog, !isWindowsServiceEnabled);
    for (Task task : getInfo().getTasks())
    {
      task.canLaunch(newTask, errors);
    }
    if (errors.isEmpty())
    {
      if (isWindowsServiceEnabled)
      {
        launchOperation(newTask,
            INFO_CTRL_PANEL_DISABLING_WINDOWS_SERVICE_SUMMARY.get(),
            INFO_CTRL_PANEL_DISABLING_WINDOWS_SERVICE_SUCCESSFUL_SUMMARY.get(),
            INFO_CTRL_PANEL_DISABLING_WINDOWS_SERVICE_SUCCESSFUL_DETAILS.get(),
            ERR_CTRL_PANEL_DISABLING_WINDOWS_SERVICE_ERROR_SUMMARY.get(),
            null,
            ERR_CTRL_PANEL_DISABLING_WINDOWS_SERVICE_ERROR_DETAILS,
            progressDialog);
      }
      else
      {
        launchOperation(newTask,
            INFO_CTRL_PANEL_ENABLING_WINDOWS_SERVICE_SUMMARY.get(),
            INFO_CTRL_PANEL_ENABLING_WINDOWS_SERVICE_SUCCESSFUL_SUMMARY.get(),
            INFO_CTRL_PANEL_ENABLING_WINDOWS_SERVICE_SUCCESSFUL_DETAILS.get(),
            ERR_CTRL_PANEL_ENABLING_WINDOWS_SERVICE_ERROR_SUMMARY.get(),
            null,
            ERR_CTRL_PANEL_ENABLING_WINDOWS_SERVICE_ERROR_DETAILS,
            progressDialog);
      }
      progressDialog.setVisible(true);
    }
    else
    {
      displayErrorDialog(errors);
    }
  }

  /** The task in charge of updating the windows service configuration. */
  private class WindowsServiceTask extends Task
  {
    private Set<String> backendSet;
    private boolean enableService;
    /**
     * The constructor of the task.
     * @param info the control panel info.
     * @param dlg the progress dialog that shows the progress of the task.
     * @param enableService whether the windows service must be enabled or
     * disabled.
     */
    private WindowsServiceTask(ControlPanelInfo info, ProgressDialog dlg,
        boolean enableService)
    {
      super(info, dlg);
      this.enableService = enableService;
      backendSet = new HashSet<>();
    }

    @Override
    public Type getType()
    {
      if (enableService)
      {
        return Type.ENABLE_WINDOWS_SERVICE;
      }
      else
      {
        return Type.DISABLE_WINDOWS_SERVICE;
      }
    }

    @Override
    public LocalizableMessage getTaskDescription()
    {
      if (enableService)
      {
        return INFO_CTRL_PANEL_ENABLE_WINDOWS_SERVICE_TASK_DESCRIPTION.get();
      }
      else
      {
        return INFO_CTRL_PANEL_DISABLE_WINDOWS_SERVICE_TASK_DESCRIPTION.get();
      }
    }

    @Override
    public boolean canLaunch(Task taskToBeLaunched,
        Collection<LocalizableMessage> incompatibilityReasons)
    {
      Type type = taskToBeLaunched.getType();
      if (state == State.RUNNING
          && runningOnSameServer(taskToBeLaunched)
          && (type == Type.ENABLE_WINDOWS_SERVICE
              || type == Type.DISABLE_WINDOWS_SERVICE))
      {
        incompatibilityReasons.add(getIncompatibilityMessage(this, taskToBeLaunched));
        return false;
      }
      return true;
    }

    @Override
    public void runTask()
    {
      state = State.RUNNING;
      lastException = null;
      try
      {
        if (enableService)
        {
          returnCode = ConfigureWindowsService.enableService(outPrintStream, errorPrintStream);
          if (returnCode != ConfigureWindowsService.SERVICE_ALREADY_ENABLED &&
              returnCode != ConfigureWindowsService.SERVICE_ENABLE_SUCCESS)
          {
            state = State.FINISHED_WITH_ERROR;
          }
          else
          {
            state = State.FINISHED_SUCCESSFULLY;
          }
        }
        else
        {
          returnCode = ConfigureWindowsService.disableService(outPrintStream, errorPrintStream);
          if (returnCode != ConfigureWindowsService.SERVICE_ALREADY_DISABLED
              && returnCode != ConfigureWindowsService.SERVICE_DISABLE_SUCCESS)
          {
            state = State.FINISHED_WITH_ERROR;
          }
          else
          {
            state = State.FINISHED_SUCCESSFULLY;
          }
        }
      }
      catch (Throwable t)
      {
        lastException = t;
        state = State.FINISHED_WITH_ERROR;
      }
    }

    @Override
    public Set<String> getBackends()
    {
      return backendSet;
    }

    @Override
    protected ArrayList<String> getCommandLineArguments()
    {
      ArrayList<String> args = new ArrayList<>();

      if (enableService)
      {
        args.add("--enableService");
      }
      else
      {
        args.add("--disableService");
      }

      return args;
    }

    @Override
    protected String getCommandLinePath()
    {
      return getCommandLinePath("windows-service");
    }
  }
}
