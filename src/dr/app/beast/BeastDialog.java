/*
 * BeastDialog.java
 *
 * Copyright © 2002-2024 the BEAST Development Team
 * http://beast.community/about
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 *
 */

package dr.app.beast;

import dr.app.checkpoint.BeastCheckpointer;
import dr.app.gui.FileDrop;
import dr.app.gui.components.WholeNumberField;
import jam.html.SimpleLinkListener;
import jam.panels.OptionsPanel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

/**
 * @author Andrew Rambaut
 * @author Guy Baele
 */

public class BeastDialog {
    private final JFrame frame;

    private final OptionsPanel optionPanel;

    private final WholeNumberField seedText = new WholeNumberField((long) 1, Long.MAX_VALUE);
    private final JCheckBox overwriteCheckBox = new JCheckBox("Allow overwriting of log files");
    private final JCheckBox beagleCheckBox = new JCheckBox("The BEAGLE library is required to run BEAST:");
    private final JCheckBox beagleInfoCheckBox = new JCheckBox("Show list of available BEAGLE resources and Quit");
    private final JComboBox beagleResourceCombo = new JComboBox(new Object[]{"CPU", "GPU"});
    private final JCheckBox beagleSSECheckBox = new JCheckBox("Use CPU's SSE extensions when possible");
    private final JComboBox beaglePrecisionCombo = new JComboBox(new Object[]{"Double", "Single"});
    private final JComboBox beagleScalingCombo = new JComboBox(new Object[]{"Default", "Dynamic", "Delayed", "Always", "Never"});

    private final JComboBox threadsCombo = new JComboBox(new Object[]{"Automatic", 0, 1, 2, 3, 4, 5, 6, 7, 8});

    private File inputFile = null;

    public BeastDialog(final JFrame frame, final String titleString, final Icon icon) {
        this.frame = frame;
        this.optionPanel = new OptionsPanel(12, 12);

        initializeMainPanel(icon, titleString);
        initializeInputFileSection();
        initializeCheckpointingSection();
        initializeAdditionalOptions();
        initializeBeagleOptions();
    }

    private void initializeMainPanel(final Icon icon, final String titleString) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        final OptionsPanel optionPanel3 = new OptionsPanel(0, 3);
        final JLabel titleIcon = new JLabel();
        titleIcon.setIcon(icon);

        final JEditorPane titleText = new JEditorPane("text/html", "<html>" + titleString + "</html>");
        titleText.setOpaque(false);
        titleText.setEditable(false);
        titleText.addHyperlinkListener(new SimpleLinkListener());
        optionPanel3.addComponent(titleText);

        optionPanel.addComponents(titleIcon, optionPanel3);
    }

    private void initializeInputFileSection() {
        final JButton inputFileButton = new JButton("Choose File...");
        final JTextField inputFileNameText = new JTextField("not selected", 16);

        inputFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                FileDialog dialog = new FileDialog(frame, "Select target file...", FileDialog.LOAD);
                dialog.setVisible(true);
                if (dialog.getFile() == null) {
                    return;
                }
                inputFile = new File(dialog.getDirectory(), dialog.getFile());
                inputFileNameText.setText(inputFile.getName());
            }
        });

        inputFileNameText.setEditable(false);
        setupInputFileDragAndDrop(inputFileNameText);

        JPanel panel1 = new JPanel(new BorderLayout(0, 0));
        panel1.add(inputFileNameText, BorderLayout.CENTER);
        panel1.add(inputFileButton, BorderLayout.EAST);
        inputFileNameText.setToolTipText("<html>Drag a BEAST XML file here or use the button to<br>select one from a file dialog box.</html>");
        inputFileButton.setToolTipText("<html>Drag a BEAST XML file here or use the button to<br>select one from a file dialog box.</html>");
        optionPanel.addComponentWithLabel("BEAST XML File: ", panel1);
    }

    private void setupInputFileDragAndDrop(final JTextField inputFileNameText) {
        Color focusColor = UIManager.getColor("Focus.color");
        Border focusBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, focusColor);
        new FileDrop(null, inputFileNameText, focusBorder, new FileDrop.Listener() {
            public void filesDropped(java.io.File[] files) {
                inputFile = files[0];
                inputFileNameText.setText(inputFile.getName());
            }
        });
    }

    private void initializeCheckpointingSection() {
        final JButton chkptButton = new JButton("Custom settings");
        chkptButton.setToolTipText("<html>By default, a checkpoint file will be written according to the<br>specifications in your XML file. No previous checkpointed file<br>will be loaded and no custom file name can be provided.</html>");

        chkptButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showCheckpointSettingsDialog(chkptButton);
            }
        });

        JPanel chkptPanel = new JPanel(new BorderLayout(0, 0));
        chkptPanel.add(chkptButton, BorderLayout.CENTER);
        optionPanel.addComponentWithLabel("Checkpointing:", chkptPanel);

        optionPanel.addSeparator();
    }

    private void showCheckpointSettingsDialog(final JButton chkptButton) {
        JDialog dialog = new JDialog(frame, "Checkpointing settings");
        dialog.setLocationRelativeTo(chkptButton);
        dialog.setModal(true);
        dialog.setAlwaysOnTop(true);

        OptionsPanel chkptPanel = new OptionsPanel(0, 3);
        chkptPanel.setOpaque(false);
        JCheckBox overruleXML = new JCheckBox();
        overruleXML.setToolTipText("This will ignore the checkpointing settings in your XML file.");
        overruleXML.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.setProperty("checkpointOverrule", Boolean.toString(overruleXML.isSelected()));
            }
        });
        chkptPanel.addComponentWithLabel("Overrule XML checkpointing settings", overruleXML);

        chkptPanel.addSeparator();

        JTextField checkpointInput = createCheckpointTextField(BeastCheckpointer.LOAD_STATE_FILE);
        JTextField checkpointOutput = createCheckpointTextField(BeastCheckpointer.SAVE_STATE_FILE);
        WholeNumberField checkpointEvery = createCheckpointEveryField();

        chkptPanel.addComponentWithLabel("Load previous checkpoint file: ", checkpointInput);
        chkptPanel.addComponentWithLabel("Save new checkpoint file (will overwrite): ", checkpointOutput);
        chkptPanel.addComponentWithLabel("Save checkpoint every: ", checkpointEvery);

        dialog.add(chkptPanel);
        dialog.pack();
        dialog.setVisible(true);
    }

    private JTextField createCheckpointTextField(final String property) {
        JTextField checkpointTextField = new JTextField(15);
        checkpointTextField.addKeyListener(new java.awt.event.KeyListener() {
            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
                System.setProperty(property, checkpointTextField.getText());
            }
        });
        return checkpointTextField;
    }

    private WholeNumberField createCheckpointEveryField() {
        WholeNumberField checkpointEvery = new WholeNumberField(1, Integer.MAX_VALUE);
        checkpointEvery.addKeyListener(new java.awt.event.KeyListener() {
            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
                System.setProperty(BeastCheckpointer.SAVE_STATE_EVERY, checkpointEvery.getValue() + "");
            }
        });
        checkpointEvery.setValue(1000000);
        checkpointEvery.setColumns(10);
        return checkpointEvery;
    }

    private void initializeAdditionalOptions() {
        seedText.setColumns(12);
        seedText.setToolTipText("<html>Specify a particular random number seed to replicate<br>precisely the sequence of steps in the MCMC chain. By<br>default this uses system information to provide a new<br>seed each run.</html>");
        optionPanel.addComponentWithLabel("Random number seed: ", seedText);

        threadsCombo.setToolTipText("<html>Specify how large a thread pool to use.<br>In most circumstances this should be set to 'automatic'<br>but in some circumstances it may be desirable to restict<br>the number of cores being used. 0 will turn off threading</html>");
        optionPanel.addComponentWithLabel("Thread pool size: ", threadsCombo);

        optionPanel.addSeparator();

        optionPanel.addSpanningComponent(beagleCheckBox);
        beagleCheckBox.setSelected(true);
    }

    private void initializeBeagleOptions() {
        final OptionsPanel optionPanel1 = new OptionsPanel(0, 6);
        optionPanel1.setBorder(new TitledBorder(""));

        OptionsPanel optionPanel2 = new OptionsPanel(0, 3);
        optionPanel2.setBorder(BorderFactory.createEmptyBorder());
        final JLabel label1 = optionPanel2.addComponentWithLabel("Prefer use of: ", beagleResourceCombo);
        optionPanel2.addComponent(beagleSSECheckBox);
        beagleSSECheckBox.setSelected(true);
        final JLabel label2 = optionPanel2.addComponentWithLabel("Prefer precision: ", beaglePrecisionCombo);
        final JLabel label3 = optionPanel2.addComponentWithLabel("Rescaling scheme: ", beagleScalingCombo);
        optionPanel2.addComponent(beagleInfoCheckBox);
        optionPanel2.setBorder(BorderFactory.createEmptyBorder());

        optionPanel1.addComponent(optionPanel2);

        final JEditorPane beagleInfo = createBeagleInfoPane();
        optionPanel1.addComponent(beagleInfo);
        optionPanel1.setBorder(BorderFactory.createEmptyBorder());
        optionPanel.addSpanningComponent(optionPanel1);

        setupBeagleCheckBoxListeners(beagleInfo, label1, label2, label3);

        beagleCheckBox.setSelected(true);
        beagleCheckBox.setEnabled(false);
        beagleResourceCombo.setSelectedItem("CPU");
    }

    private JEditorPane createBeagleInfoPane() {
        final JEditorPane beagleInfo = new JEditorPane("text/html",
                "<html><div style=\"font-family:'helvetica neue light',helvetica,sans-serif;font-size:12;\"><p>BEAGLE is a high-performance phylogenetic library that can make use of<br>" +
                        "additional computational resources such as graphics boards. It must be<br>" +
                        "downloaded and installed independently of BEAST:</p>" +
                        "<pre><a href=\"http://github.com/beagle-dev/beagle-lib/\">http://github.com/beagle-dev/beagle-lib/</a></pre></div></html>");
        beagleInfo.setOpaque(false);
        beagleInfo.setEditable(false);
        beagleInfo.addHyperlinkListener(new SimpleLinkListener());
        return beagleInfo;
    }

    private void setupBeagleCheckBoxListeners(final JEditorPane beagleInfo, final JLabel label1, final JLabel label2, final JLabel label3) {
        beagleCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                boolean selected = beagleCheckBox.isSelected();
                beagleInfo.setEnabled(selected);
                beagleInfoCheckBox.setEnabled(selected);
                label1.setEnabled(selected);
                beagleResourceCombo.setEnabled(selected);
                beagleSSECheckBox.setEnabled(selected);
                label2.setEnabled(selected);
                beaglePrecisionCombo.setEnabled(selected);
                label3.setEnabled(selected);
                beagleScalingCombo.setEnabled(selected);
            }
        });
    }

    public boolean showDialog(String title) {

        JOptionPane optionPane = new JOptionPane(optionPanel,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                new String[]{"Run", "Quit"},
                "Run");
        optionPane.setBorder(new EmptyBorder(12, 12, 12, 12));

        final JDialog dialog = optionPane.createDialog(frame, title);
        //dialog.setResizable(true);
        dialog.pack();

        dialog.setVisible(true);

        return (optionPane.getValue() != null ? optionPane.getValue().equals("Run") : false);
    }

    public long getSeed() {
        return seedText.getLongValue();
    }

    public void setSeed(long seed) {
        seedText.setValue(seed);
    }

    public boolean allowOverwrite() {
        return overwriteCheckBox.isSelected();
    }

    public void setAllowOverwrite(boolean allowOverwrite) {
        overwriteCheckBox.setSelected(allowOverwrite);
    }

    /*public void setUseBeagle(boolean useBeagle) {
         beagleCheckBox.setSelected(useBeagle);
         beagleCheckBox.setEnabled(false);
    }*/

    public boolean preferBeagleGPU() {
        return beagleResourceCombo.getSelectedItem().equals("GPU");
    }

    public boolean preferBeagleCPU() {
        return (beagleResourceCombo.getSelectedItem().equals("CPU"));
    }

    public void setPreferBeagleGPU() {
        beagleResourceCombo.setSelectedItem("GPU");
    }

    public boolean preferBeagleSSE() {
        return beagleSSECheckBox.isSelected();
    }

    public void setPreferBeagleSSE(boolean preferBeagleSSE) {
        beagleSSECheckBox.setSelected(preferBeagleSSE);
    }

    public boolean preferBeagleSingle() {
        return beaglePrecisionCombo.getSelectedItem().equals("Single");
    }

    public boolean preferBeagleDouble() {
        return beaglePrecisionCombo.getSelectedItem().equals("Double");
    }

    public void setPreferBeagleSingle() {
         beaglePrecisionCombo.setSelectedItem("Single");
    }

    public String scalingScheme() {
        return ((String) beagleScalingCombo.getSelectedItem()).toLowerCase();
    }

    public void setScalingScheme(String scalingScheme) {
        beagleScalingCombo.setSelectedItem(scalingScheme);
    }

    public boolean showBeagleInfo() {
        return beagleInfoCheckBox.isSelected();
    }

    public int getThreadPoolSize() {
        if (threadsCombo.getSelectedIndex() == 0) {
            // Automatic
            return -1;
        }
        return (Integer) threadsCombo.getSelectedItem();
    }

    public File getInputFile() {
        return inputFile;
    }
}