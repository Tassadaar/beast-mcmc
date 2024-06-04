/*
 * PartitionTreeModelPanel.java
 *
 * Copyright (c) 2002-2015 Alexei Drummond, Andrew Rambaut and Marc Suchard
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
 */

package dr.app.beauti.treespanel;

import dr.app.beauti.BeautiFrame;
import dr.app.beauti.options.BeautiOptions;
import dr.app.beauti.options.PartitionTreeModel;
import dr.app.beauti.options.TreeHolder;
import dr.app.beauti.types.StartingTreeType;
import dr.app.beauti.util.BEAUTiImporter;
import dr.app.beauti.util.PanelUtils;
import dr.app.gui.components.RealNumberField;
import dr.app.util.OSType;
import dr.evolution.datatype.DataType;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import jam.panels.OptionsPanel;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.*;
import java.io.*;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @author Walter Xie
 * @version $Id: PriorsPanel.java,v 1.9 2006/09/05 13:29:34 rambaut Exp $
 */
public class PartitionTreeModelPanel extends OptionsPanel {

    private static final long serialVersionUID = 8096349200725353543L;

    private final String NO_TREE = "no tree loaded";

    private ButtonGroup startingTreeGroup = new ButtonGroup();
    private JRadioButton randomTreeRadio = new JRadioButton("Random starting tree");
    private JRadioButton upgmaTreeRadio = new JRadioButton("UPGMA starting tree");
    private JRadioButton userTreeRadio = new JRadioButton("User-specified starting tree");
    private JRadioButton empiricalTreeRadio = new JRadioButton("Empirical tree set");
    private ImportTreeAction importTreeAction = new ImportTreeAction();
    private JButton importTreeButton = new JButton(importTreeAction);
    private JLabel userTreeLabel = new JLabel("User-specified tree:");
    private JComboBox userTreeCombo = new JComboBox();

    private JLabel empiricalTreeLabel = new JLabel("Trees filename:");
    private JTextArea empiricalFilenameField = new JTextArea("empirical.trees");

    private JLabel userTreeInfo = new JLabel("<html>" +
            "Use a tree imported using the 'Import Data' menu option.<br>" +
            "Starting trees that are not rooted and strictly bifurcating <br>" +
            " (binary) will be randomly resolved.</html>");

    private JLabel empiricalTreeInfo = new JLabel("<html>" +
            "Use trees from a specified <b>NEXUS</b> or <b>Newick</b> format data file<br>" +
            "as a set of empirical trees to sample over. It should have the same taxon<br>" +
            "names as the data partition. BEAST will look for the file in the current<br>" +
            "working directory or the folder containing the XML file.</html>");

    private RealNumberField initRootHeightField = new RealNumberField(Double.MIN_VALUE, Double.POSITIVE_INFINITY, "Init root height");

    private BeautiOptions options = null;
    private final BeautiFrame parent;
    private boolean settingOptions = false;

    PartitionTreeModel partitionTreeModel;

    public PartitionTreeModelPanel(final BeautiFrame parent, PartitionTreeModel parTreeModel, final BeautiOptions options) {
        super(12, (OSType.isMac() ? 6 : 24));

        this.partitionTreeModel = parTreeModel;
        this.options = options;
        this.parent = parent;

        PanelUtils.setupComponent(initRootHeightField);
        initRootHeightField.setColumns(10);
        initRootHeightField.setEnabled(false);

        PanelUtils.setupComponent(randomTreeRadio);
        PanelUtils.setupComponent(upgmaTreeRadio);
        PanelUtils.setupComponent(userTreeRadio);
        PanelUtils.setupComponent(empiricalTreeRadio);

        startingTreeGroup.add(randomTreeRadio);
        startingTreeGroup.add(upgmaTreeRadio);
        startingTreeGroup.add(userTreeRadio);
        startingTreeGroup.add(empiricalTreeRadio);

        randomTreeRadio.setSelected(partitionTreeModel.getStartingTreeType() == StartingTreeType.RANDOM);
        upgmaTreeRadio.setSelected(partitionTreeModel.getStartingTreeType() == StartingTreeType.UPGMA);
        userTreeRadio.setSelected(partitionTreeModel.getStartingTreeType() == StartingTreeType.USER);
        userTreeRadio.setEnabled(options.userTrees.size() > 0);
        empiricalTreeRadio.setSelected(partitionTreeModel.getStartingTreeType() == StartingTreeType.EMPIRICAL);

        boolean enabled = partitionTreeModel.getStartingTreeType() == StartingTreeType.USER;
        userTreeLabel.setEnabled(enabled);
        userTreeCombo.setEnabled(enabled);
        userTreeInfo.setEnabled(enabled);

        enabled = partitionTreeModel.getStartingTreeType() == StartingTreeType.EMPIRICAL;
        empiricalTreeInfo.setEnabled(enabled);
        empiricalTreeLabel.setEnabled(enabled);
        empiricalFilenameField.setEnabled(enabled);

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if (randomTreeRadio.isSelected()) {
                    partitionTreeModel.setStartingTreeType(StartingTreeType.RANDOM);
                } else if (upgmaTreeRadio.isSelected()) {
                    partitionTreeModel.setStartingTreeType(StartingTreeType.UPGMA);
                } else if (userTreeRadio.isSelected()) {
                    partitionTreeModel.setStartingTreeType(StartingTreeType.USER);
                } else  if (empiricalTreeRadio.isSelected()) {
                    partitionTreeModel.setStartingTreeType(StartingTreeType.EMPIRICAL);
                }
                boolean enabled = partitionTreeModel.getStartingTreeType() == StartingTreeType.USER;
                userTreeLabel.setEnabled(enabled);
                userTreeCombo.setEnabled(enabled);
                userTreeInfo.setEnabled(enabled);

                enabled = partitionTreeModel.getStartingTreeType() == StartingTreeType.EMPIRICAL;
                empiricalTreeInfo.setEnabled(enabled);
                empiricalTreeLabel.setEnabled(enabled);
                empiricalFilenameField.setEnabled(enabled);
            }
        };
        randomTreeRadio.addActionListener(listener);
        upgmaTreeRadio.addActionListener(listener);
        userTreeRadio.addActionListener(listener);
        empiricalTreeRadio.addActionListener(listener);

        PanelUtils.setupComponent(userTreeCombo);

        userTreeCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
                setUserSpecifiedStartingTree();
            }
        });

        setupPanel();
        setOptions();
    }

    private void setUserSpecifiedStartingTree() {
        if (userTreeCombo.getSelectedItem() != null && (!userTreeCombo.getSelectedItem().toString().equalsIgnoreCase(NO_TREE))) {
            Tree seleTree = getSelectedUserTree();
            if (seleTree != null) {
                partitionTreeModel.setUserStartingTree(seleTree);
            } else {
                JOptionPane.showMessageDialog(parent, "The selected user-specified starting tree " +
                                "is not fully bifurcating.\nBEAST requires rooted, bifurcating (binary) trees.",
                        "Illegal user-specified starting tree",
                        JOptionPane.ERROR_MESSAGE);

                userTreeCombo.setSelectedItem(NO_TREE);
                partitionTreeModel.setUserStartingTree(null);
            }
//            else {
//                JOptionPane.showMessageDialog(parent, "The selected user-specified starting tree " +
//                        "is not fully bifurcating.\nBEAST requires rooted, bifurcating (binary) trees.",
//                        "Illegal user-specified starting tree",
//                        JOptionPane.ERROR_MESSAGE);
//
//                userTreeCombo.setSelectedItem(NO_TREE);
//                partitionTreeModel.setUserStartingTree(null);
//            }
        }
    }

    public void setupPanel() {

        removeAll();

        if (partitionTreeModel.getDataType().getType() != DataType.TREE) {
            addSpanningComponent(randomTreeRadio);
            addSpanningComponent(upgmaTreeRadio);
            addSpanningComponent(userTreeRadio);

            addComponents(userTreeLabel, userTreeCombo);
            userTreeCombo.removeAllItems();
            if (options.userTrees.size() < 1) {
                userTreeCombo.addItem(NO_TREE);
            } else {
                Object selectedItem = userTreeCombo.getSelectedItem();
                for (TreeHolder tree : options.userTrees.values()) {
                    userTreeCombo.addItem(tree);
                }
                if (selectedItem != null) {
                    userTreeCombo.setSelectedItem(selectedItem);
                } else {
                    userTreeCombo.setSelectedIndex(0);
                }
            }

            addComponent(userTreeInfo);

            // hiding this as the text says import trees from File menu
//            addComponent(importTreeButton);

            addSpanningComponent(empiricalTreeRadio);
            addComponents(empiricalTreeLabel, empiricalFilenameField);
            empiricalFilenameField.setColumns(32);
            empiricalFilenameField.setEditable(true);
            addComponent(empiricalTreeInfo);

            empiricalFilenameField.addKeyListener(new java.awt.event.KeyListener() {
                public void keyTyped(KeyEvent e) {
                }

                public void keyPressed(KeyEvent e) {
                }

                public void keyReleased(KeyEvent e) {
                    partitionTreeModel.setEmpiricalTreesFilename(empiricalFilenameField.getText());
                    parent.setDirty();
                }
            });

        } else {
            JTextArea citationText = new JTextArea(1, 40);
            citationText.setLineWrap(true);
            citationText.setWrapStyleWord(true);
            citationText.setEditable(false);
            citationText.setFont(this.getFont());
            citationText.setOpaque(false);

            addComponentWithLabel("Tree as data model:", new JLabel("ThorneyBEAST"));
            String citation = //citationCoalescent +  "\n" +
                    "McCrone et al.";
            addComponentWithLabel("Citation:", citationText);
            citationText.setText(citation);
        }

        validate();
        repaint();
    }

    public void setOptions() {

        if (partitionTreeModel == null) {
            return;
        }

        settingOptions = true;
        initRootHeightField.setValue(partitionTreeModel.getInitialRootHeight());

        userTreeRadio.setEnabled(options.userTrees.size() > 0);

        settingOptions = false;

        String empiricalFilename = "empirical.trees";
        if (partitionTreeModel.getEmpiricalTreesFilename() != null) {
            empiricalFilename = partitionTreeModel.getEmpiricalTreesFilename();
        }
        empiricalFilenameField.setText(empiricalFilename);
    }

    public void getOptions(BeautiOptions options) {
        if (settingOptions) return;
    }

    public boolean isBifurcatingTree(Tree tree, NodeRef node) {
        if (tree.getChildCount(node) > 2) return false;
        for (int i = 0; i < tree.getChildCount(node); i++) {
            if (!isBifurcatingTree(tree, tree.getChild(node, i))) return false;
        }
        return true;
    }

    private Tree getSelectedUserTree() {
        TreeHolder treeHolder = (TreeHolder) userTreeCombo.getSelectedItem();
        return treeHolder.getTrees().get(0);
    }

    private class ImportTreeAction extends AbstractAction {
        public ImportTreeAction() {
            super("Import additional tree(s) from file ...");
            setToolTipText("Import newick-formatted trees from a file. These trees can be used as a starting tree.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            doImportTrees();
        }
    }

    private boolean doImportTrees() {
        File[] files = parent.selectImportFiles("Import Trees File...", false, new FileNameExtensionFilter[]{
                new FileNameExtensionFilter("Nexus files or text files containing newick trees",
                        "tree", "trees", "treefile", "txt", "nex", "nexus", "nwk", "newick")});

        BEAUTiImporter beautiImporter = new BEAUTiImporter(parent, options);

        if (files != null && files.length != 0) {

            File file = files[0];
            int nTreesBefore = options.userTrees.size();

            try {

                String line = beautiImporter.findFirstLine(file);

                if ((line != null && line.toUpperCase().contains("#NEXUS"))) {
                    // is a NEXUS file
                    beautiImporter.importNexusFile(file, true);
                } else {
                    beautiImporter.importNewickFile(files[0]);
                }

            } catch (FileNotFoundException fnfe) {
                JOptionPane.showMessageDialog(this, "Unable to open file: File not found",
                        "Unable to open file",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(this, "Unable to read file: " + ioe.getMessage(),
                        "Unable to read file",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                JOptionPane.showMessageDialog(this, "Fatal exception: " + ex,
                        "Error reading file",
                        JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
                return false;
            }

            int nTreesAfter = options.userTrees.size();
            if (nTreesAfter == nTreesBefore) {
                JOptionPane.showMessageDialog(this,
                        "Did not find any trees in file '" + file.getName() + "'",
                        "No trees found",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            return false;
        }

        setupPanel();
        setOptions();

        return true;
    }

}