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
package io.github.mtrevisan.familylegacy.ui.components;

import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeListener;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Dialog for selecting a specific page within a paged document (e.g., a PDF or multi-page TIFF).
 * Includes an asynchronous, cached preview with zoom (Ctrl + Mouse Wheel) and fit-to-window support.
 */
public class PageInputDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(PageInputDialog.class);


	private final JSpinner pageSpinner;
	private final JLabel previewLabel;
	private final JScrollPane previewScrollPane;

	private String documentUri;
	private boolean isSaved;

	// --- Caching & Optimization Fields ---
	private PDDocument currentPdfDocument;
	private PDFRenderer currentPdfRenderer;

	// Cache for rendered pages (unscaled) to avoid re-rendering when navigating back and forth
	private final Map<Integer, Image> pageCache = new HashMap<>();

	// Keep track of the current worker to cancel it if the user changes page rapidly
	private SwingWorker<Image, Void> currentRenderWorker;

	// --- Zoom State ---
	private Image currentOriginalImage;
	private double currentZoom = 1.0;
	private static final double ZOOM_STEP = 1.15;


	public static PageInputDialog create(final Window parent){
		final PageInputDialog dialog = new PageInputDialog(parent);
		dialog.initialize();

		return dialog;
	}


	private PageInputDialog(final Window parent){
		super(parent, ModalityType.APPLICATION_MODAL);

		pageSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
		previewLabel = new JLabel();

		// Standard Swing approach: put the label inside the scroll pane
		previewScrollPane = new JScrollPane(previewLabel);
		previewScrollPane.setBorder(BorderFactory.createEtchedBorder());

		// Enable fast scrolling
		previewScrollPane.getVerticalScrollBar().setUnitIncrement(20);
		previewScrollPane.getHorizontalScrollBar().setUnitIncrement(20);
	}


	private void initialize(){
		initComponents();
		initLayout();

		pack();

		setMinimumSize(new Dimension(500, 600));
		setLocationRelativeTo(getOwner());
	}

	private void initComponents(){
		setTitle(I18N.t("dialog.preferred.page.input.title"));

		// Center the preview content
		previewLabel.setHorizontalAlignment(JLabel.CENTER);
		previewLabel.setVerticalAlignment(JLabel.CENTER);

		// Add a listener to update the preview when the page changes
		pageSpinner.addChangeListener(e -> updatePreview());

		// Add listener for Ctrl + Mouse Wheel Zoom
		previewScrollPane.addMouseWheelListener(new MouseAdapter(){
			@Override
			public void mouseWheelMoved(final MouseWheelEvent e){
				if(e.isControlDown()){
					e.consume(); // Prevent normal scrolling
					if(e.getWheelRotation() < 0)
						applyZoom(currentZoom * ZOOM_STEP);
					else
						applyZoom(currentZoom / ZOOM_STEP);
				}
			}
		});
	}

	private void initLayout(){
		// Layout with 3 rows: 
		// 1. Preview (takes all available space)
		// 2. Page selection
		// 3. Buttons
		setLayout(new MigLayout("ins 10,hidemode 3,fill,wrap 1", "[grow,fill]", "[grow,push]10[]10[]"));

		// 1. Center panel: The preview (inside the scroll pane)
		// The scroll pane takes all available space and will show scrollbars if needed
		add(previewScrollPane, "grow,push");

		// 2. Below the preview: Page selection
		final JPanel contentPanel = GUIHelper.createLabelFieldPanel(0, "[grow,fill]");
		GUIHelper.addComponent(contentPanel, new JLabel(I18N.t("dialog.preferred.page.input.label")));
		GUIHelper.addComponent(contentPanel, pageSpinner);
		add(contentPanel, "growx");

		// 3. Bottom panel: Buttons
		final JPanel buttonPanel = GUIHelper.createButtonPanel(this,
			this::save,
			() -> setVisible(false));
		add(buttonPanel, "growx");
	}

	/**
	 * Loads the document URI and initializes the spinner with the current page.
	 * If the document changes, clears the cache and closes the previous document.
	 */
	public void loadData(final String uri, final Integer page){
		isSaved = false;

		// If we are opening a different file, we must clear the cache and close the old document
		if(this.documentUri == null || !this.documentUri.equals(uri)){
			closeCurrentDocument();

			this.documentUri = uri;
		}

		final ChangeListener[] listeners = pageSpinner.getChangeListeners();
		for(final ChangeListener l : listeners)
			pageSpinner.removeChangeListener(l);

		pageSpinner.setValue(page != null ? page : 1);

		for(final ChangeListener l : listeners)
			pageSpinner.addChangeListener(l);

		updatePreview();
	}

	/**
	 * Updates the preview image asynchronously.
	 */
	private void updatePreview(){
		if(documentUri == null || documentUri.trim().isEmpty()){
			previewLabel.setIcon(null);
			previewLabel.setText("No document loaded");
			return;
		}

		final int page = (Integer)pageSpinner.getValue();

		// If the page is already in the cache, show it immediately without a background task
		if(pageCache.containsKey(page)){
			displayImage(pageCache.get(page));

			return;
		}

		// Cancel the previous rendering task if it's still running
		if(currentRenderWorker != null && !currentRenderWorker.isDone())
			currentRenderWorker.cancel(true);

		previewLabel.setIcon(null);
		previewLabel.setText("Loading page " + page + "...");

		currentRenderWorker = new SwingWorker<>(){
			@Override
			protected Image doInBackground(){
				final Image img = renderPage(documentUri, page);
				if(img != null && !isCancelled())
					// Cache the original unscaled image
					pageCache.put(page, img);

				return img;
			}

			@Override
			protected void done(){
				if(isCancelled())
					return;

				try{
					final Image img = get();
					if(img != null)
						displayImage(img);
					else{
						previewLabel.setText("Preview not available or page out of bounds.");
						previewLabel.setPreferredSize(null);
					}
				}
				catch(final Exception e){
					LOGGER.error("Error rendering preview", e);

					previewLabel.setText("Error loading preview.");
					previewLabel.setPreferredSize(null);
				}
			}
		};
		currentRenderWorker.execute();
	}

	/**
	 * Displays the unscaled image and fits it to the window.
	 */
	private void displayImage(final Image img){
		this.currentOriginalImage = img;

		// Fit to window when a new page is loaded.
		// We use invokeLater to ensure the scroll pane has been laid out and has a valid viewport size.
		SwingUtilities.invokeLater(this::fitToWindow);
	}

	/**
	 * Calculates the scale factor to fit the entire image inside the current viewport.
	 */
	private void fitToWindow(){
		if(currentOriginalImage == null)
			return;

		final Dimension viewportSize = previewScrollPane.getViewport().getExtentSize();
		if(viewportSize.width <= 0 || viewportSize.height <= 0)
			return;

		final double scaleX = (double) viewportSize.width / currentOriginalImage.getWidth(null);
		final double scaleY = (double) viewportSize.height / currentOriginalImage.getHeight(null);

		// Use the smaller scale to ensure the whole image fits
		final double fitScale = Math.min(scaleX, scaleY);

		// Apply zoom and reset scroll position to top-left
		applyZoom(fitScale);
		previewScrollPane.getViewport().setViewPosition(new Point(0, 0));
	}

	/**
	 * Scales the original image by the given factor and updates the UI.
	 * Uses Graphics2D for fast and high-quality rendering.
	 */
	private void applyZoom(final double newZoom){
		if(currentOriginalImage == null)
			return;

		// Constrain zoom between 10% and 500% to avoid extreme sizes
		this.currentZoom = Math.max(0.1, Math.min(newZoom, 5.0));

		final int w = (int) (currentOriginalImage.getWidth(null) * currentZoom);
		final int h = (int) (currentOriginalImage.getHeight(null) * currentZoom);

		// Create a new scaled image
		final BufferedImage scaledImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g2 = scaledImage.createGraphics();

		// Use bilinear interpolation for a good balance of speed and quality
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(currentOriginalImage, 0, 0, w, h, null);
		g2.dispose();

		// Update the label
		previewLabel.setText(null);
		previewLabel.setPreferredSize(new Dimension(w, h));
		previewLabel.setIcon(new ImageIcon(scaledImage));

		// Refresh the layout
		previewLabel.revalidate();
		previewLabel.repaint();
	}

	/**
	 * Renders a specific page of a document to an Image.
	 * Uses Apache PDFBox for PDF files and ImageIO (with TwelveMonkeys) for images/TIFFs.
	 */
	private Image renderPage(final String uri, final int page){
		final File file = new File(uri);
		if(!file.exists())
			return null;

		final String lowerUri = uri.toLowerCase();

		// --- 1. PDF Rendering (Apache PDFBox 3.x) ---
		if(lowerUri.endsWith(".pdf")){
			try{
				// Keep the PDFDocument open if it's already loaded
				if(currentPdfDocument == null){
					currentPdfDocument = Loader.loadPDF(file);
					currentPdfRenderer = new PDFRenderer(currentPdfDocument);
				}

				if(page > 0 && page <= currentPdfDocument.getNumberOfPages()){
					// 72 DPI is usually sufficient for a preview
					return currentPdfRenderer.renderImageWithDPI(page - 1, 72, ImageType.RGB);
				}
			}
			catch(final IOException e){
				LOGGER.error("Error reading PDF: {}", e.getMessage());

				// Reset on error
				closeCurrentDocument();
			}
			return null;
		}

		// --- 2. Image/TIFF Rendering (ImageIO + TwelveMonkeys) ---
		try(var imageInputStream = ImageIO.createImageInputStream(file)){
			if(imageInputStream != null){
				final var readers = ImageIO.getImageReaders(imageInputStream);
				if(readers.hasNext()){
					final var reader = readers.next();
					try{
						reader.setInput(imageInputStream, true, true);

						// For multi-page formats (like TIFF), read the requested page
						if(reader.getNumImages(true) >= page)
							return reader.read(page - 1);
						// Fallback for single-page images (JPG, PNG, etc.)
						else if(reader.getNumImages(true) == 1 && page == 1)
							return reader.read(0);
					}
					finally{
						reader.dispose();
					}
				}
			}
		}
		catch(final IOException e){
			LOGGER.error("Error reading image file: {}", e.getMessage());
		}

		return null;
	}

	/**
	 * Safely closes the currently open PDF document to free up memory.
	 */
	private void closeCurrentDocument(){
		if(currentPdfDocument != null){
			try{
				currentPdfDocument.close();
			}
			catch(final IOException e){
				LOGGER.error("Error closing PDF document", e);
			}
			currentPdfDocument = null;
			currentPdfRenderer = null;
		}
		pageCache.clear();
		currentOriginalImage = null;
	}

	@Override
	public void dispose(){
		// Ensure the PDF document is closed when the dialog is destroyed
		closeCurrentDocument();

		super.dispose();
	}

	public void save(){
		isSaved = true;

		setVisible(false);
	}

	public boolean isSaved(){
		return isSaved;
	}

	public Integer getPage(){
		return (Integer)pageSpinner.getValue();
	}

}
