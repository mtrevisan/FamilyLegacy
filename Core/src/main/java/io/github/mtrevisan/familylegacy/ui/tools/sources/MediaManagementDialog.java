/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.ui.tools.sources;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.preferences.MediaPathDisplay;
import io.github.mtrevisan.familylegacy.quality.MediaFiles;
import io.github.mtrevisan.familylegacy.search.SearchMode;
import io.github.mtrevisan.familylegacy.sources.SourceHelper;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchBox;
import io.github.mtrevisan.familylegacy.ui.handlers.MediaHandler;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;


/**
 * Modal dialog that lists every {@code MediaRecord}, shows a
 * thumbnail preview of the selected document, and allows the user to
 * create, edit, and delete documents.
 * <p>
 * The preview loads the image from the document's {@code uri} when it is
 * a local file ({@code file:} scheme or a plain path) and exists on
 * disk. Remote URIs are not fetched: the dialog shows the URI as a
 * placeholder instead. This keeps the dialog responsive and avoids
 * network access from the EDT.
 */
public final class MediaManagementDialog extends JDialog{

	private static final int PREVIEW_WIDTH = 360;
	private static final int PREVIEW_HEIGHT = 360;


	private final ToolContext context;
	private final DocumentTableModel tableModel = new DocumentTableModel();
	private final JTable table = new JTable(tableModel);
	private final SearchBox searchBox = new SearchBox(24, false);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);
	private final JLabel previewLabel = new JLabel(I18N.t("label.select_document"), JLabel.CENTER);


	public MediaManagementDialog(final ToolContext context){
		super(context.owner(), I18N.t("search.title.manage.media"), ModalityType.APPLICATION_MODAL);

		this.context = context;

		setLayout(new BorderLayout(6, 6));
		add(createToolbar(), BorderLayout.NORTH);
		add(createCenter(), BorderLayout.CENTER);
		add(createFooter(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(1000, 620));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		reload();
	}


	private JPanel createToolbar(){
		final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
		toolbar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

		toolbar.add(new JLabel(I18N.l("label.search")));
		toolbar.add(searchBox);
		searchBox.onChange(this::applyFilter);

		final JButton newButton = new JButton(I18N.t("button.new"));
		newButton.addActionListener(e -> openEditor(null));
		toolbar.add(newButton);

		final JButton editButton = new JButton(I18N.t("button.edit"));
		editButton.addActionListener(e -> openEditor(selectedDocument()));
		toolbar.add(editButton);

		final JButton deleteButton = new JButton(I18N.t("button.delete"));
		deleteButton.addActionListener(e -> deleteSelected());
		toolbar.add(deleteButton);

		return toolbar;
	}

	private JSplitPane createCenter(){
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(22);
		table.setAutoCreateRowSorter(true);
		final TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(280);
		columnModel.getColumn(1).setPreferredWidth(200);
		columnModel.getColumn(2).setPreferredWidth(120);
		columnModel.getColumn(3).setPreferredWidth(80);

		table.getSelectionModel().addListSelectionListener(e -> {
			if(!e.getValueIsAdjusting())
				updatePreview();
		});
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					openEditor(selectedDocument());
			}
		});

		final JScrollPane tableScroll = new JScrollPane(table);
		tableScroll.setBorder(BorderFactory.createTitledBorder(I18N.t("border.media")));

		previewLabel.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
		previewLabel.setBorder(BorderFactory.createTitledBorder(I18N.t("border.preview")));
		final JScrollPane previewScroll = new JScrollPane(previewLabel);

		final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
			tableScroll, previewScroll);
		split.setResizeWeight(0.65);
		split.setDividerLocation(0.65);
		split.setContinuousLayout(true);
		return split;
	}

	private JPanel createFooter(){
		return GUIHelper.createStatusFooter(statusLabel);
	}


	private void reload(){
		final FLEFModel model = context.model();
		final Map<String, Integer> references = SourceHelper.countDocumentReferences(model);
		final List<SourceHelper.DocumentRow> rows = new ArrayList<>();
		for(final FLEFRecord doc : SourceHelper.listAllDocuments(model))
			rows.add(SourceHelper.toDocumentRow(doc, references));
		tableModel.setRows(rows);
		updateStatus(rows.size());
		updatePreview();
	}

	private void applyFilter(){
				@SuppressWarnings("unchecked")
		final TableRowSorter<DocumentTableModel> sorter =
			(TableRowSorter<DocumentTableModel>)table.getRowSorter();
		if(searchBox.isBlank())
			sorter.setRowFilter(null);
		else{
			final String query = searchBox.query();
			final Set<SearchMode> modes = searchBox.modes();
			sorter.setRowFilter(new RowFilter<>(){
				@Override
				public boolean include(final Entry<? extends DocumentTableModel, ? extends Integer> entry){
					final SourceHelper.DocumentRow row = tableModel.getRow(entry.getIdentifier());
					return SearchBox.matches(query, modes, row.uri(), row.description(), row.mapping());
				}
			});
		}
		updateStatus(table.getRowCount());
	}

	private FLEFRecord selectedDocument(){
		final int viewRow = table.getSelectedRow();
		if(viewRow < 0)
			return null;

		final String rowId = tableModel.getRow(table.convertRowIndexToModel(viewRow))
			.id();
		final FLEFModel model = context.model();
		return model.getRecordById(rowId);
	}

	private long previewTicket;

	private void updatePreview(){
		final FLEFRecord document = selectedDocument();
		if(document == null){
			previewTicket ++;
			previewLabel.setIcon(null);
			previewLabel.setText(I18N.t("label.select_document"));

			return;
		}

		final String uri = SourceHelper.documentUri(document);
		//the image is read off the EDT; a result that is no longer the current one is discarded
		final long ticket = ++previewTicket;
		previewLabel.setIcon(null);
		previewLabel.setText(StringUtils.EMPTY);
		new SwingWorker<ImageIcon, Void>(){
			@Override
			protected ImageIcon doInBackground(){
				final ImageIcon icon = tryLoadImage(uri);
				return (icon != null? scaleIcon(icon, PREVIEW_WIDTH, PREVIEW_HEIGHT): null);
			}

			@Override
			protected void done(){
				if(ticket != previewTicket)
					return;

				ImageIcon icon = null;
				try{
					icon = get();
				}
				catch(final InterruptedException ie){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ignored){
					//no preview
				}
				if(icon != null){
					previewLabel.setText(StringUtils.EMPTY);
					previewLabel.setIcon(icon);
				}
				else{
					previewLabel.setIcon(null);
					previewLabel.setText(uri != null? "<html><center>" + escape(MediaPathDisplay.display(uri)) + "</center></html>"
						: "<html><center>" + I18N.t("label.no_uri") + "</center></html>");
				}
			}
		}.execute();
	}

	/**
	 * Tries to load an image from the given location. Only local files are loaded: remote locations are ignored to keep the dialog
	 * responsive. The location is resolved as the validation does (a {@code file:} URI, an absolute path, or a path relative to the
	 * base directory), so a Windows path or one with spaces is found too.
	 * Returns {@code null} when the location is missing, remote, or the file cannot be read as an image.
	 */
	private static ImageIcon tryLoadImage(final String uri){
		if(StringUtils.isBlank(uri))
			return null;

		final Path path = MediaFiles.localPath(uri.strip(), MediaPathDisplay.baseDirectory());
		if(path == null || !Files.isReadable(path))
			return null;

		try{
			final BufferedImage image = ImageIO.read(path.toFile());
			if(image != null)
				return new ImageIcon(image);
		}
		catch(final IOException | RuntimeException ignored){
			//fall back to the toolkit below
		}
		try{
			final ImageIcon raw = new ImageIcon(path.toString());
			return (raw.getIconWidth() > 0? raw: null);
		}
		catch(final RuntimeException ignored){
			return null;
		}
	}

	private static ImageIcon scaleIcon(final ImageIcon icon, final int maxW, final int maxH){
		final int w = icon.getIconWidth();
		final int h = icon.getIconHeight();
		if(w <= 0 || h <= 0)
			return icon;
		final double scale = Math.min(maxW / (double)w, maxH / (double)h);
		if(scale >= 1.)
			return icon;
		final int nw = (int)Math.round(w * scale);
		final int nh = (int)Math.round(h * scale);
		final Image scaled = icon.getImage().getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
		return new ImageIcon(scaled);
	}

	private void openEditor(final FLEFRecord document){
		final MediaHandler handler = MediaHandler.getInstance();
		final RecordDialog dialog;
		final FLEFModel model = context.model();
		if(document == null)
			dialog = handler.createNewDialog(this, model);
		else
			dialog = handler.createEditDialog(this, model, document);
		dialog.setVisible(true);

		if(dialog.isSaved())
			reload();
	}

	private void deleteSelected(){
		final FLEFRecord document = selectedDocument();
		if(document == null)
			return;

		final int confirm = JOptionPane.showConfirmDialog(this,
			I18N.tf("confirmation.delete.media.message", MediaReader.extractDescription(document)),
			I18N.t("confirmation.delete.media.title"),
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if(confirm != JOptionPane.YES_OPTION)
			return;

		final FLEFModel model = context.model();
		model.removeRecord(document.getId());

		reload();
	}

	private void updateStatus(final int count){
		statusLabel.setText(I18N.tf("text.media.documents", count));
	}

	private static String escape(final String s){
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}


	private static final class DocumentTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {I18N.t("column.uri"), I18N.t("column.description"), I18N.t("column.mapping"), I18N.t("column.references")};

		private final List<SourceHelper.DocumentRow> rows = new ArrayList<>();

		void setRows(final List<SourceHelper.DocumentRow> rows){
			this.rows.clear();
			this.rows.addAll(rows);
			fireTableDataChanged();
		}

		SourceHelper.DocumentRow getRow(final int index){
			return rows.get(index);
		}

		@Override public int getRowCount(){ return rows.size(); }
		@Override public int getColumnCount(){ return COLUMNS.length; }
		@Override public String getColumnName(final int column){ return COLUMNS[column]; }
		//(the references are numbers: the sorter must not sort them as text, "10" before "2")
		@Override public Class<?> getColumnClass(final int column){ return (column == 3? Integer.class: String.class); }

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final SourceHelper.DocumentRow row = rows.get(rowIndex);
			return switch(columnIndex){
				case 0 -> row.uri();
				case 1 -> row.description();
				case 2 -> row.mapping();
				case 3 -> row.referenceCount();
				default -> StringUtils.EMPTY;
			};
		}
	}

}
