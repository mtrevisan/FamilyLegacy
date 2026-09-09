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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.ui.helpers.ResourceHelper;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * A horizontal carousel panel that displays thumbnails of images asynchronously.
 * The thumbnails are loaded in the background to avoid blocking the UI.
 */
public class ImageCarouselPanel extends JPanel{

	@Serial
	private static final long serialVersionUID = 8132815632562553085L;


	private static final int THUMBNAIL_SIZE = 80;
	private static final int GAP = 4;


	private final DefaultListModel<ThumbnailInfo> model = new DefaultListModel<>();

	private List<String> imageUris = new ArrayList<>();
	private boolean loading;


	public ImageCarouselPanel(){
		setLayout(new CardLayout());
		setBorder(BorderFactory.createTitledBorder("Images"));

		// Scroll pane with the list
		final JList<ThumbnailInfo> list = new JList<>(model);
		list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		list.setVisibleRowCount(1);
		list.setCellRenderer(new ThumbnailCellRenderer());
		list.setFixedCellWidth(THUMBNAIL_SIZE + GAP);
		list.setFixedCellHeight(THUMBNAIL_SIZE + GAP);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		final JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());

		// Empty state label
		final JLabel emptyLabel = new JLabel("No images available", SwingConstants.CENTER);
		emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.ITALIC));
		emptyLabel.setForeground(Color.GRAY);
		emptyLabel.setPreferredSize(new Dimension(0, 80));

		add(scrollPane, "images");
		add(emptyLabel, "empty");

		// Start with empty state
		showEmpty(true);
	}

	private void showEmpty(final boolean show){
		final CardLayout cl = (CardLayout)getLayout();
		cl.show(this, show? "empty": "images");
	}

	/**
	 * Sets the list of image URIs to display as thumbnails.
	 * The thumbnails are loaded asynchronously.
	 *
	 * @param uris the list of image URIs ({@code null} or empty means no images)
	 */
	public void setImageUris(final List<String> uris){
		imageUris = (uris != null)? uris: new ArrayList<>();
		model.clear();

		if(imageUris.isEmpty()){
			showEmpty(true);

			return;
		}
		showEmpty(false);

		// Add placeholder entries for each URI
		for(final String uri : imageUris)
			model.addElement(new ThumbnailInfo(uri, null));

		// Start background loading
		loadThumbnails();
	}

	private void loadThumbnails(){
		if(loading || imageUris.isEmpty())
			return;

		loading = true;

		final SwingWorker<Void, ThumbnailInfo> worker = new SwingWorker<>(){
			@Override
			protected Void doInBackground(){
				for(final String uri : imageUris){
					final ImageIcon thumb = loadThumbnail(uri);
					publish(new ThumbnailInfo(uri, thumb));
				}

				return null;
			}

			@Override
			protected void process(final List<ThumbnailInfo> chunks){
				for(final ThumbnailInfo info : chunks)
					for(int i = 0, size = model.size(); i < size; i ++){
						final ThumbnailInfo existing = model.get(i);
						if(existing.uri.equals(info.uri)){
							model.set(i, info);

							break;
						}
					}
			}

			@Override
			protected void done(){
				loading = false;

				repaint();
			}
		};
		worker.execute();
	}

	private ImageIcon loadThumbnail(final String uri){
		try{
			final BufferedImage img = ResourceHelper.readBufferedImage(new File(uri));
			if(img == null)
				return createPlaceholderIcon(THUMBNAIL_SIZE);

			final int w = img.getWidth();
			final int h = img.getHeight();
			final int targetSize = THUMBNAIL_SIZE - 4;
			final double scale = Math.min((double)targetSize / w, (double)targetSize / h);
			final int newW = (int)(w * scale);
			final int newH = (int)(h * scale);
			final Image scaled = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
			return new ImageIcon(scaled);
		}
		catch(final Exception e){
			return createPlaceholderIcon(THUMBNAIL_SIZE);
		}
	}

	private static ImageIcon createPlaceholderIcon(final int size){
		final BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2 = img.createGraphics();
		g2.setColor(Color.LIGHT_GRAY);
		g2.fillRect(0, 0, size, size);
		g2.setColor(Color.DARK_GRAY);
		g2.setFont(g2.getFont().deriveFont(10f));
		g2.drawString("No img", 10, size / 2);
		g2.dispose();
		return new ImageIcon(img);
	}

	private record ThumbnailInfo(String uri, ImageIcon icon){}

	private static class ThumbnailCellRenderer extends DefaultListCellRenderer{
		@Serial
		private static final long serialVersionUID = 8155363623055502967L;

		@Override
		public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
				final boolean isSelected, final boolean cellHasFocus){
			final JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if(value instanceof ThumbnailInfo info){
				label.setIcon(info.icon);
				label.setText(null);
				label.setHorizontalAlignment(SwingConstants.CENTER);
				label.setVerticalAlignment(SwingConstants.CENTER);
				label.setPreferredSize(new Dimension(THUMBNAIL_SIZE + GAP, THUMBNAIL_SIZE + GAP));
				label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
				label.setToolTipText(info.uri);
			}
			return label;
		}
	}
}