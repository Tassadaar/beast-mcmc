/*
 * AlignmentViewer.java
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

package dr.app.beauti.alignmentviewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author Andrew Rambaut
 */
public class AlignmentViewer extends JPanel {
    private TaxonPane taxonPane;
    private JScrollPane taxonScrollPane;

    private RulerPane rulerPane;
    private JScrollPane rulerScrollPane;

    private PlotPane plotPane;
    private JScrollPane plotScrollPane;

    private AlignmentPane alignmentPane;
    private JScrollPane alignmentScrollPane;

    private JSplitPane splitPane;

    private Point dragPoint = null;

    /**
     * Creates new AlignmentPanel
     */
    public AlignmentViewer() {
        this(null);
    }

    /**
     * Creates new AlignmentPanel
     */
    public AlignmentViewer(PlotPane plotPane) {
        this.plotPane = plotPane;

        setOpaque(false);
        setMinimumSize(new Dimension(300, 150));
        setLayout(new BorderLayout(6, 6));

        initPanes();
        syncScrollBars();
        addEventListeners();

        JPanel leftPanel = createLeftPanel();
        JPanel rightPanel = createRightPanel();

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(120);

        add(splitPane, BorderLayout.CENTER);
    }

    private void initPanes() {
        taxonPane = new TaxonPane();
        taxonScrollPane = new JScrollPane(taxonPane, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        taxonScrollPane.setBorder(null);

        rulerPane = new RulerPane();
        rulerPane.setOpaque(false);
        rulerScrollPane = new JScrollPane(rulerPane, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rulerScrollPane.setBorder(null);

        alignmentPane = new AlignmentPane(taxonPane, rulerPane);
        alignmentScrollPane = new JScrollPane(alignmentPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        alignmentScrollPane.setBorder(null);

        if (plotPane != null) {
            plotScrollPane = new JScrollPane(plotPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            plotScrollPane.getHorizontalScrollBar().setModel(alignmentScrollPane.getHorizontalScrollBar().getModel());
        }
    }

    private void syncScrollBars() {
        taxonScrollPane.getVerticalScrollBar().setModel(alignmentScrollPane.getVerticalScrollBar().getModel());
        rulerScrollPane.getHorizontalScrollBar().setModel(alignmentScrollPane.getHorizontalScrollBar().getModel());
    }

    private void addEventListeners() {
        alignmentPane.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                dragPoint = mouseEvent.getPoint();
            }
        });

        alignmentPane.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent mouseEvent) {
                int deltaX = mouseEvent.getX() - dragPoint.x;
                int deltaY = mouseEvent.getY() - dragPoint.y;
                Rectangle visRect = alignmentPane.getVisibleRect();

                if (deltaX > 0) {
                    deltaX = visRect.x - deltaX;
                } else {
                    deltaX = visRect.x + visRect.width - deltaX;
                }

                if (deltaY > 0) {
                    deltaY = visRect.y - deltaY;
                } else {
                    deltaY = visRect.y + visRect.height - deltaY;
                }

                Rectangle r = new Rectangle(deltaX, deltaY, 1, 1);
                alignmentPane.scrollRectToVisible(r);
            }
        });
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(6, 6));
        leftPanel.add(taxonScrollPane, BorderLayout.CENTER);

        JPanel emptyPanel = new JPanel();
        emptyPanel.setPreferredSize(new Dimension(16, 16));
        leftPanel.add(emptyPanel, BorderLayout.NORTH);

        return leftPanel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(6, 6));
        rightPanel.add(alignmentScrollPane, BorderLayout.CENTER);
        rightPanel.add(rulerScrollPane, BorderLayout.NORTH);

        if (plotPane != null) {
            rightPanel.add(plotScrollPane, BorderLayout.SOUTH);
        }

        return rightPanel;
    }

    public void setAlignmentBuffer(AlignmentBuffer alignmentBuffer) {
        rulerPane.setAlignmentBuffer(alignmentBuffer);
        taxonPane.setAlignmentBuffer(alignmentBuffer);
        alignmentPane.setAlignmentBuffer(alignmentBuffer);
    }

    public void setRowDecorator(RowDecorator rowDecorator) {
        alignmentPane.setRowDecorator(rowDecorator);
    }

    public void setColumnDecorator(ColumnDecorator columnDecorator) {
        alignmentPane.setColumnDecorator(columnDecorator);
        rulerPane.setColumnDecorator(columnDecorator);
    }

    public void setCellDecorator(CellDecorator cellDecorator) {
        alignmentPane.setCellDecorator(cellDecorator);
    }

    public void addHorizontalScrollbarListener(AdjustmentListener adjustmentListener) {
        alignmentScrollPane.getHorizontalScrollBar().addAdjustmentListener(adjustmentListener);
    }

    public void addVerticalScrollbarListener(AdjustmentListener adjustmentListener) {
        alignmentScrollPane.getVerticalScrollBar().addAdjustmentListener(adjustmentListener);
    }

    public void addComponentListener(ComponentListener componentListener) {
        alignmentScrollPane.addComponentListener(componentListener);
    }

    public Rectangle getTaxonPaneBounds() {
        return taxonScrollPane.getViewportBorderBounds();
    }

    public Rectangle getAlignmentPaneBounds() {
        return alignmentScrollPane.getViewportBorderBounds();
    }

    public Rectangle getVisibleArea() {
        return alignmentPane.getVisibleArea();
    }

    public void setTopRow(int row) {
        alignmentPane.setTopRow(row);
    }

    public void setLeftColumn(int col) {
        alignmentPane.setLeftColumn(col);
    }

    public void setCentreColumn(int col) {
        alignmentPane.setCentreColumn(col);
    }

    public void setRightColumn(int col) {
        alignmentPane.setRightColumn(col);
    }
}
