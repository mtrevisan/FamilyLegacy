package io.github.mtrevisan.familylegacy.v2.ui.tools.sources;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Dialog that shows every document in the model as a grid of thumbnails.
 * <p>
 * The grid is filterable by substring on the document URI or
 * description. Clicking a thumbnail shows the full metadata in the side
 * panel and offers to open the document in the system viewer.
 * <p>
 * Only local files are thumbnailed. Remote URIs appear as a placeholder
 * tile with the URI text: fetching remote images from the EDT would make
 * the dialog unresponsive and would require a network stack the tool
 * does not need.
 */
public final class MediaManagerDialog extends JDialog{

	private static final int THUMB_WIDTH = 128;
	private static final int THUMB_HEIGHT = 128;
	private static final int TILE_WIDTH = 160;
	private static final int TILE_HEIGHT = 180;


	private final ToolContext context;
	private final JTextField searchField = new JTextField(24);
	private final JPanel grid = new JPanel(new GridLayout(0, 4, 8, 8));
	private final JLabel detailsLabel = new JLabel("Select a document", JLabel.LEFT);
	private final JButton openButton = new JButton("Open in viewer");

	private final List<FLEFRecord> documents = new ArrayList<>();
	private FLEFRecord selectedDocument;


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


	private JPanel createToolbar(){
		final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
		toolbar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		toolbar.add(new JLabel("Filter:"));
		toolbar.add(searchField);
		searchField.getDocument().addDocumentListener(new DocumentListener(){
			@Override public void insertUpdate(final DocumentEvent e){ applyFilter(); }
			@Override public void removeUpdate(final DocumentEvent e){ applyFilter(); }
			@Override public void changedUpdate(final DocumentEvent e){ applyFilter(); }
		});
		return toolbar;
	}

	private JSplitPane createCenter(){
		grid.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		final JScrollPane gridScroll = new JScrollPane(grid);
		gridScroll.getVerticalScrollBar().setUnitIncrement(20);

		final JPanel details = new JPanel(new BorderLayout(6, 6));
		details.setBorder(BorderFactory.createTitledBorder("Details"));
		detailsLabel.setVerticalAlignment(JLabel.TOP);
		detailsLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		details.add(new JScrollPane(detailsLabel), BorderLayout.CENTER);

		final JPanel detailsButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		openButton.setEnabled(false);
		openButton.addActionListener(e -> openSelectedInViewer());
		detailsButtons.add(openButton);
		details.add(detailsButtons, BorderLayout.SOUTH);

		final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
			gridScroll, details);
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
		grid.removeAll();
		final String needle = searchField.getText() == null
			? "": searchField.getText().trim().toLowerCase(Locale.ROOT);
		for(final FLEFRecord doc : documents){
			if(!needle.isEmpty() && !matches(doc, needle))
				continue;
			grid.add(createTile(doc));
		}
		grid.revalidate();
		grid.repaint();
	}

	private static boolean matches(final FLEFRecord doc, final String needle){
		final String uri = SourceHelper.documentUri(doc);
		final String desc = SourceHelper.documentDescription(doc);
		return (uri != null && uri.toLowerCase(Locale.ROOT).contains(needle))
			|| (desc != null && desc.toLowerCase(Locale.ROOT).contains(needle));
	}

	private JPanel createTile(final FLEFRecord doc){
		final JPanel tile = new JPanel(new BorderLayout(4, 4));
		tile.setPreferredSize(new Dimension(TILE_WIDTH, TILE_HEIGHT));
		tile.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

		final String uri = SourceHelper.documentUri(doc);
		final ImageIcon icon = tryLoadThumbnail(uri);
		final JLabel imageLabel;
		if(icon != null)
			imageLabel = new JLabel(icon, JLabel.CENTER);
		else{
			imageLabel = new JLabel("<html><center>" + escape(shorten(uri)) + "</center></html>",
				JLabel.CENTER);
			imageLabel.setForeground(Color.GRAY);
		}
		tile.add(imageLabel, BorderLayout.CENTER);

		final String desc = SourceHelper.documentDescription(doc);
		final JLabel caption = new JLabel("<html><center>"
			+ escape(desc != null? desc: doc.getId()) + "</center></html>", JLabel.CENTER);
		caption.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 2));
		tile.add(caption, BorderLayout.SOUTH);

		tile.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				selectedDocument = doc;
				updateDetails();
			}
		});
		return tile;
	}

	private static String shorten(final String uri){
		if(uri == null)
			return "(no uri)";
		return uri.length() > 40? "…" + uri.substring(uri.length() - 38): uri;
	}

	private static ImageIcon tryLoadThumbnail(final String uri){
		final Image image = tryLoadImage(uri);
		if(image == null)
			return null;
		final int w = image.getWidth(null);
		final int h = image.getHeight(null);
		if(w <= 0 || h <= 0)
			return null;
		final double scale = Math.min(THUMB_WIDTH / (double)w, THUMB_HEIGHT / (double)h);
		final int nw = (int)Math.round(w * scale);
		final int nh = (int)Math.round(h * scale);
		return new ImageIcon(image.getScaledInstance(nw, nh, Image.SCALE_SMOOTH));
	}

	private static Image tryLoadImage(final String uri){
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
			final ImageIcon icon = new ImageIcon(path.toString());
			return (icon.getIconWidth() > 0? icon.getImage(): null);
		}
		catch(final Exception ignored){
			return null;
		}
	}

	private void updateDetails(){
		if(selectedDocument == null){
			detailsLabel.setText("Select a document");
			openButton.setEnabled(false);
			return;
		}
		final StringBuilder sb = new StringBuilder("<html><body style='font-family:SansSerif;font-size:12px'>");
		sb.append("<b>").append(escape(selectedDocument.getId())).append("</b><br><br>");
		final String uri = SourceHelper.documentUri(selectedDocument);
		final String desc = SourceHelper.documentDescription(selectedDocument);
		final String mapping = SourceHelper.firstTextValue(selectedDocument, SourceHelper.TAG_MAPPING);
		if(desc != null)
			sb.append("<b>Description:</b> ").append(escape(desc)).append("<br>");
		if(mapping != null)
			sb.append("<b>Mapping:</b> ").append(escape(mapping)).append("<br>");
		if(uri != null)
			sb.append("<b>URI:</b> ").append(escape(uri)).append("<br>");
		sb.append("</body></html>");
		detailsLabel.setText(sb.toString());
		openButton.setEnabled(uri != null && !uri.isBlank());
	}

	private void openSelectedInViewer(){
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
			javax.swing.JOptionPane.showMessageDialog(this,
				"Unable to open the document:\n" + e.getMessage(),
				"Media Manager", javax.swing.JOptionPane.ERROR_MESSAGE);
		}
	}

	private static String escape(final String s){
		if(s == null)
			return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

}
