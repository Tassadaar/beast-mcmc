package dr.app.bss;

import jam.panels.ActionPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import dr.evolution.util.Taxa;
import dr.evolution.util.Taxon;

public class TaxaEditor {

	private PartitionDataList dataList;
	private MainFrame frame;

	// private MutableTaxonList taxonList;
	private int taxonCount;
	private static final int minCount = 0;

	// Window
	private JDialog window;
	private Frame owner;

	// Menubar
	private JMenuBar menu;

	// Buttons with options
	private JButton load;
	private JButton save;
	private JButton done;
	private JButton cancel;

	// Data, model & stuff for JTable
	private JTable table;
	private TaxaEditorTableModel taxaEditorTableModel;

	@SuppressWarnings("serial")
	private Action addTaxonAction = new AbstractAction("+") {
		public void actionPerformed(ActionEvent ae) {

			taxaEditorTableModel.addEmptyRow();
			setTaxa();

		}// END: actionPerformed
	};

	@SuppressWarnings("serial")
	private Action removeTaxonAction = new AbstractAction("-") {
		public void actionPerformed(ActionEvent ae) {
			if (taxonCount > minCount) {

				taxaEditorTableModel.removeLastRow();
				setTaxa();

			}
		}// END: actionPerformed
	};

	public TaxaEditor(MainFrame frame, PartitionDataList dataList, int row) {

		this.frame = frame;
		this.dataList = dataList;
		// taxonList = new Taxa();
		taxaEditorTableModel = new TaxaEditorTableModel();

		// Setup Main Menu buttons
		load = new JButton("Load", Utils.createImageIcon(Utils.TEXT_FILE_ICON));
		save = new JButton("Save", Utils.createImageIcon(Utils.SAVE_ICON));
		done = new JButton("Done", Utils.createImageIcon(Utils.CHECK_ICON));
		cancel = new JButton("Cancel", Utils.createImageIcon(Utils.CLOSE_ICON));

		// Add Main Menu buttons listeners
		load.addActionListener(new ListenLoadTaxaFile());
		save.addActionListener(new ListenSaveTaxaFile());
		done.addActionListener(new ListenOk());
		cancel.addActionListener(new ListenCancel());

		// Setup menu
		menu = new JMenuBar();
		menu.setLayout(new BorderLayout());
		JPanel buttonsHolder = new JPanel();
		buttonsHolder.setOpaque(false);
		buttonsHolder.add(load);
		buttonsHolder.add(save);
		buttonsHolder.add(done);
		buttonsHolder.add(cancel);
		menu.add(buttonsHolder, BorderLayout.WEST);

		// Setup table
		table = new JTable();
		table.setModel(taxaEditorTableModel);
		table.setSurrendersFocusOnKeystroke(true);

		JScrollPane scrollPane = new JScrollPane(table,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		RowNumberTable rowNumberTable = new RowNumberTable(table);
		scrollPane.setRowHeaderView(rowNumberTable);
		scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER,
				rowNumberTable.getTableHeader());

		ActionPanel actionPanel = new ActionPanel(false);
		actionPanel.setAddAction(addTaxonAction);
		actionPanel.setRemoveAction(removeTaxonAction);

		// Setup window
		owner = Utils.getActiveFrame();
		window = new JDialog(owner, "Edit taxa set...");
		window.getContentPane().add(menu, BorderLayout.NORTH);
		window.getContentPane().add(scrollPane);
		window.getContentPane().add(actionPanel, BorderLayout.SOUTH);

		window.pack();
		window.setLocationRelativeTo(owner);

		setTaxa();

	}// END: LocationCoordinatesEditor()

	public void launch() {

		if (SwingUtilities.isEventDispatchThread()) {
			showWindow();
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					showWindow();
				}
			});
		}// END: edt check

	}// END: launch

	public void showWindow() {

		// Display Frame
		window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		window.setSize(new Dimension(400, 400));
		window.setMinimumSize(new Dimension(100, 100));
		window.setResizable(true);
		window.setVisible(true);

	}// END: showWindow

	private void setTaxa() {

		taxonCount = taxaEditorTableModel.getRowCount();

		addTaxonAction.setEnabled(true);
		if (taxonCount == minCount) {
			removeTaxonAction.setEnabled(false);
		} else {
			removeTaxonAction.setEnabled(true);
		}

		ColumnResizer.adjustColumnPreferredWidths(table);
	}// END: setPartitions

	private class ListenLoadTaxaFile implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Select taxa file...");
			chooser.setMultiSelectionEnabled(false);
			chooser.setCurrentDirectory(frame.getWorkingDirectory());

			int returnValue = chooser.showOpenDialog(Utils.getActiveFrame());

			if (returnValue == JFileChooser.APPROVE_OPTION) {

				File file = chooser.getSelectedFile();

				if (file != null) {

					loadTaxaFile(file);

					File tmpDir = chooser.getCurrentDirectory();
					if (tmpDir != null) {
						frame.setWorkingDirectory(tmpDir);
					}

				}// END: file opened check
			}// END: dialog cancelled check

		}// END: actionPerformed
	}// END: ListenLoadTaxaFile

	private void loadTaxaFile(File file) {

		try {

			Taxa taxonList = new Taxa();
			String[] lines = loadStrings(file.getAbsolutePath());

			Taxon taxon;
			for (int i = 0; i < lines.length; i++) {

				String[] line = lines[i].split("\\s+");

				taxon = new Taxon(line[TaxaEditorTableModel.NAME_INDEX]);
				taxon.setAttribute(Utils.ABSOLUTE_HEIGHT,
						Double.valueOf(line[TaxaEditorTableModel.HEIGHT_INDEX]));

				taxonList.addTaxon(taxon);

			}// END: i loop

			updateTable(taxonList);

		} catch (Exception e) {
			Utils.handleException(e);
		}// END: try-catch block

	}// END: loadTaxaFile

	private String[] loadStrings(String filename) throws IOException {

		int linesCount = countLines(filename);
		String[] lines = new String[linesCount];

		FileInputStream inputStream;
		BufferedReader reader;
		String line;

		inputStream = new FileInputStream(filename);
		reader = new BufferedReader(new InputStreamReader(inputStream,
				Charset.forName("UTF-8")));
		int i = 0;
		while ((line = reader.readLine()) != null) {
			lines[i] = line;
			i++;
		}

		// Clean up
		reader.close();
		reader = null;
		inputStream = null;

		return lines;
	}// END: loadStrings

	public int countLines(String filename) throws IOException {

		InputStream is = new BufferedInputStream(new FileInputStream(filename));

		byte[] c = new byte[1024];
		int count = 0;
		int readChars = 0;
		boolean empty = true;
		while ((readChars = is.read(c)) != -1) {
			empty = false;
			for (int i = 0; i < readChars; ++i) {
				if (c[i] == '\n') {
					++count;
				}
			}
		}

		is.close();

		return (count == 0 && !empty) ? 1 : count;

	}// END: countLines

	private class ListenSaveTaxaFile implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Save taxa file...");
			chooser.setMultiSelectionEnabled(false);
			chooser.setCurrentDirectory(frame.getWorkingDirectory());

			int returnVal = chooser.showSaveDialog(Utils.getActiveFrame());
			if (returnVal == JFileChooser.APPROVE_OPTION) {

				File file = chooser.getSelectedFile();

				saveTaxaFile(file.getAbsolutePath());

				File tmpDir = chooser.getCurrentDirectory();
				if (tmpDir != null) {
					frame.setWorkingDirectory(tmpDir);
				}

			}// END: approve check
		}// END: actionPerformed
	}// END: ListenSaveLocationCoordinates

	private void saveTaxaFile(String filename) {

		try {

			boolean header = false;
			PrintWriter writer = new PrintWriter(filename);
			writer.print(taxaEditorTableModel.toString(header));
			writer.close();

		} catch (Exception e) {
			Utils.handleException(e);
		}

	}// END: save2DArray

	private class ListenOk implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			doOk();

		}// END: actionPerformed
	}// END: ListenOk

	private void doOk() {

		frame.setBusy();
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			// Executed in background thread
			public Void doInBackground() {

				try {

					int lastIndex = dataList.recordsList.size() - 1;
					String name = String.valueOf("TaxaSet").concat(
							String.valueOf(lastIndex + 1));
					Taxa taxa = taxaEditorTableModel.getTaxaSet();
					TreesTableRecord record = new TreesTableRecord(name, taxa);
					dataList.recordsList.set(lastIndex, record);

					dataList.allTaxa.addTaxa(taxa);

				} catch (Exception e) {
					Utils.handleException(e);
				}// END: try-catch block

				return null;
			}// END: doInBackground()

			// Executed in event dispatch thread
			public void done() {
				//TODO: force table repaint
				frame.setIdle();
				frame.fireTaxaChanged();
				window.setVisible(false);
			}// END: done
		};

		worker.execute();

	}// END: doOk

	private class ListenCancel implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			window.setVisible(false);

		}// END: actionPerformed
	}// END: ListenCancel

	// ////////////////////////
	// ---END: DRAG & DROP---//
	// ////////////////////////

	// ////////////////////////
	// ---END: DRAG & DROP---//
	// ////////////////////////

	public void updateTable(Taxa taxonList) {
		taxaEditorTableModel.setTaxaSet(taxonList);
		taxaEditorTableModel.fireTaxonListChanged();
		setTaxa();
	}// END: updateTable

}// END: class