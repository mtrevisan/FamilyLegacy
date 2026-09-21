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
package io.github.mtrevisan.familylegacy.v2.ui.tools.sources;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ReportDialog;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Dialog that shows every document in the model as a grid of thumbnails.
 * <p>
 * Uses a JList with HORIZONTAL_WRAP and a bounded LRU cache for high-performance
 * asynchronous rendering of large image collections without memory leaks.
 */
public final class MediaManagerDialog extends JDialog{

	private static final int THUMB_WIDTH = 128;
	private static final int THUMB_HEIGHT = 128;
	private static final int TILE_WIDTH = 160;
	private static final int TILE_HEIGHT = 180;
	private static final int MAX_CACHE_SIZE = 200;

	private final ToolContext context;
	private final JTextField searchField = new JTextField(24);

	private final DefaultListModel<FLEFRecord> listModel = new DefaultListModel<>();
	private final JList<FLEFRecord> documentList = new JList<>(listModel);

	private final JLabel detailsLabel = new JLabel("Select a document", JLabel.LEFT);
	private final JButton openButton = new JButton("Open in viewer");

	private final List<FLEFRecord> documents = new ArrayList<>();

	// Thread pool and LRU Cache for Async Image Loading
	private final ExecutorService imageLoaderExecutor = Executors.newFixedThreadPool(
		Math.max(2, Runtime.getRuntime().availableProcessors() - 1));

	private final Map<String, ImageIcon> thumbnailCache = new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true){
		@Override
		protected boolean removeEldestEntry(final Map.Entry<String, ImageIcon> eldest){
			return (size() > MAX_CACHE_SIZE);
		}
	};
	private final Map<String, Boolean> loadingStatus = new LinkedHashMap<>();


	public MediaManagerDialog(final ToolContext context){
		super(context.owner(), "Media Manager", ModalityType.APPLICATION_MODAL);

		this.context = context;

		setLayout(new BorderLayout(6, 6));
		add(createToolbar(), BorderLayout.NORTH);
		add(createCenter(), BorderLayout.CENTER);
		add(createFooter(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(1000, 700));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		reload();
	}

	@Override
	public void dispose(){
		imageLoaderExecutor.shutdownNow();

		super.dispose();
	}

	private JPanel createToolbar(){
		final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
		toolbar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		toolbar.add(new JLabel("Filter:"));
		toolbar.add(searchField);
		searchField.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				applyFilter();
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				applyFilter();
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				applyFilter();
			}
		});
		return toolbar;
	}

	private JSplitPane createCenter(){
		documentList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		documentList.setVisibleRowCount(-1);
		documentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		documentList.setFixedCellWidth(TILE_WIDTH);
		documentList.setFixedCellHeight(TILE_HEIGHT);
		documentList.setCellRenderer(new DocumentTileCellRenderer());

		documentList.addListSelectionListener(e -> {
			if(!e.getValueIsAdjusting())
				updateDetails(documentList.getSelectedValue());
		});

		final JScrollPane gridScroll = new JScrollPane(documentList);
		gridScroll.getVerticalScrollBar().setUnitIncrement(20);

		final JPanel details = new JPanel(new BorderLayout(6, 6));
		details.setBorder(BorderFactory.createTitledBorder("Details"));
		detailsLabel.setVerticalAlignment(JLabel.TOP);
		detailsLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		details.add(new JScrollPane(detailsLabel), BorderLayout.CENTER);

		final JPanel detailsButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		openButton.setEnabled(false);
		openButton.addActionListener(e -> openSelectedInViewer(documentList.getSelectedValue()));
		detailsButtons.add(openButton);
		details.add(detailsButtons, BorderLayout.SOUTH);

		final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gridScroll, details);
		split.setResizeWeight(0.75);
		split.setDividerLocation(0.75);
		split.setContinuousLayout(true);
		return split;
	}

	private JPanel createFooter(){
		final JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		footer.setBorder(BorderFactory.createEmptyBorder(2, 6, 6, 6));
		final JButton close = new JButton("Close");
		close.addActionListener(e -> dispose());
		footer.add(close);
		return footer;
	}

	private void reload(){
		documents.clear();
		documents.addAll(SourceHelper.listAllDocuments(context.model()));

		rebuildGrid();
	}

	private void applyFilter(){
		rebuildGrid();
	}

	private void rebuildGrid(){
		listModel.clear();
		final String needle = searchField.getText() == null
			? ""
			: searchField.getText().trim().toLowerCase(Locale.ROOT);

		final List<FLEFRecord> filtered = new ArrayList<>();
		for(final FLEFRecord doc : documents)
			if(needle.isEmpty() || matches(doc, needle))
				filtered.add(doc);
		listModel.addAll(filtered);
	}

	private static boolean matches(final FLEFRecord doc, final String needle){
		final String uri = SourceHelper.documentUri(doc);
		final String desc = SourceHelper.documentDescription(doc);
		return (uri != null && uri.toLowerCase(Locale.ROOT).contains(needle)
			|| desc != null && desc.toLowerCase(Locale.ROOT).contains(needle));
	}

	/**
	 * Requests thumbnail loading asynchronously.
	 */
	private void requestThumbnailLoad(final String uri, final int index){
		synchronized(this){
			if(thumbnailCache.containsKey(uri) || Boolean.TRUE.equals(loadingStatus.get(uri)))
				return;

			loadingStatus.put(uri, Boolean.TRUE);
		}

		imageLoaderExecutor.submit(() -> {
			final ImageIcon icon = createThumbnail(uri);
			SwingUtilities.invokeLater(() -> {
				synchronized(MediaManagerDialog.this){
					loadingStatus.remove(uri);
					if(icon != null)
						thumbnailCache.put(uri, icon);
				}
				if(index >= 0 && index < listModel.getSize())
					listModel.set(index, listModel.getElementAt(index));
			});
		});
	}

	private static ImageIcon createThumbnail(final String uri){
		if(uri == null || uri.isBlank())
			return null;

		try{
			final URI parsed = URI.create(uri);
			Path path = null;
			if("file".equalsIgnoreCase(parsed.getScheme()))
				path = Path.of(parsed);
			else if(parsed.getScheme() == null)
				path = Path.of(uri);
			if(path == null || !Files.isReadable(path))
				return null;

			final BufferedImage original = ImageIO.read(path.toFile());
			if(original == null)
				return null;

			final int w = original.getWidth();
			final int h = original.getHeight();
			final double scale = Math.min((double)THUMB_WIDTH / w, (double)THUMB_HEIGHT / h);
			final int nw = Math.max(1, (int)Math.round(w * scale));
			final int nh = Math.max(1, (int)Math.round(h * scale));

			final BufferedImage resized = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g2d = resized.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2d.drawImage(original, 0, 0, nw, nh, null);
			g2d.dispose();

			return new ImageIcon(resized);
		}
		catch(final Exception ignored){
			return null;
		}
	}

	/**
	 * Custom cell renderer for rendering document items inside the virtualized JList grid.
	 */
	private class DocumentTileCellRenderer extends DefaultListCellRenderer{

		private final JPanel tile = new JPanel(new BorderLayout(4, 4));
		private final JLabel imageLabel = new JLabel("", JLabel.CENTER);
		private final JLabel captionLabel = new JLabel("", JLabel.CENTER);

		public DocumentTileCellRenderer(){
			tile.setPreferredSize(new Dimension(TILE_WIDTH, TILE_HEIGHT));
			tile.add(imageLabel, BorderLayout.CENTER);
			captionLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 2));
			captionLabel.setHorizontalAlignment(SwingConstants.CENTER);
			tile.add(captionLabel, BorderLayout.SOUTH);
		}

		@Override
		public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
				final boolean isSelected, final boolean cellHasFocus){
			if(!(value instanceof final FLEFRecord doc))
				return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			final String uri = SourceHelper.documentUri(doc);
			final String desc = SourceHelper.documentDescription(doc);

			// Tile background and borders based on selection state
			if(isSelected){
				tile.setBackground(list.getSelectionBackground());
				tile.setBorder(BorderFactory.createLineBorder(list.getSelectionBackground().darker(), 2));
				captionLabel.setForeground(list.getSelectionForeground());
			}
			else{
				tile.setBackground(list.getBackground());
				tile.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
				captionLabel.setForeground(list.getForeground());
			}

			// Image Loading Logic
			ImageIcon icon = null;
			synchronized(MediaManagerDialog.this){
				if(uri != null)
					icon = thumbnailCache.get(uri);
			}

			if(icon != null){
				imageLabel.setIcon(icon);
				imageLabel.setText(null);
			}
			else{
				imageLabel.setIcon(null);
				imageLabel.setText("<html><center>" + escape(shorten(uri)) + "</center></html>");
				imageLabel.setForeground(Color.GRAY);

				if(uri != null && !uri.isBlank())
					requestThumbnailLoad(uri, index);
			}

			captionLabel.setText("<html><center>" + escape(desc != null? desc: doc.getId()) + "</center></html>");

			return tile;
		}

	}

	private static String shorten(final String uri){
		if(uri == null)
			return "(no uri)";
		return uri.length() > 40? "…" + uri.substring(uri.length() - 38): uri;
	}

	private void updateDetails(final FLEFRecord selectedDocument){
		if(selectedDocument == null){
			detailsLabel.setText("Select a document");
			openButton.setEnabled(false);

			return;
		}

		final StringBuilder sb = new StringBuilder("<html><body style='font-family:SansSerif;font-size:12px'>");
		sb.append("<b>")
			.append(escape(selectedDocument.getId()))
			.append("</b><br><br>");
		final String uri = SourceHelper.documentUri(selectedDocument);
		final String desc = SourceHelper.documentDescription(selectedDocument);
		final String mapping = SourceHelper.firstTextValue(selectedDocument, SourceHelper.TAG_MAPPING);
		if(desc != null)
			sb.append("<b>Description:</b> ")
				.append(escape(desc))
				.append("<br>");
		if(mapping != null)
			sb.append("<b>Mapping:</b> ")
				.append(escape(mapping))
				.append("<br>");
		if(uri != null)
			sb.append("<b>URI:</b> ")
				.append(escape(uri))
				.append("<br>");
		sb.append("</body></html>");
		detailsLabel.setText(sb.toString());
		openButton.setEnabled(uri != null && !uri.isBlank());
	}

	private void openSelectedInViewer(final FLEFRecord selectedDocument){
		if(selectedDocument == null)
			return;

		final String uri = SourceHelper.documentUri(selectedDocument);
		if(uri == null || uri.isBlank())
			return;

		try{
			final URI parsed = URI.create(uri);
			if(java.awt.Desktop.isDesktopSupported()
				&& java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)){
				final Path path;
				if("file".equalsIgnoreCase(parsed.getScheme()))
					path = Path.of(parsed);
				else if(parsed.getScheme() == null)
					path = Path.of(uri);
				else{
					java.awt.Desktop.getDesktop().browse(parsed);

					return;
				}
				if(Files.isReadable(path))
					java.awt.Desktop.getDesktop().open(path.toFile());
			}
		}
		catch(final Exception e){
			JOptionPane.showMessageDialog(this,
				"Unable to open the document:\n" + e.getMessage(),
				"Media Manager", JOptionPane.ERROR_MESSAGE);
		}
	}

	private static String escape(final String s){
		return ReportDialog.escape(s);
	}

}
