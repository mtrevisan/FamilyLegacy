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

import io.github.mtrevisan.familylegacy.ui.components.lists.DocumentPartListPanel;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.images.ScaledImage;
import net.miginfocom.swing.MigLayout;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatter;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Dialog for selecting a specific page and defining a crop area within a paged document (e.g., a PDF or multi-page TIFF).
 * Includes zoom support (Ctrl + Mouse Wheel).
 */
public class PageInputDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(PageInputDialog.class);


	private static final String ACTION_COMMIT_AND_CLAMP = "commitAndClamp";


	private final JSpinner pageSpinner;
	private final ScaledImage imageHolder;
	private final JScrollPane previewScrollPane;

	private String documentUri;
	private boolean isSaved;

	// --- Caching & Optimization Fields ---
	private final Map<Integer, BufferedImage> pageCache = new HashMap<>();

	// --- Zoom State ---
	private BufferedImage currentOriginalImage;
	private double currentZoom = 1.0;
	private static final double ZOOM_STEP = 1.15;

	private Rectangle cropRectInOriginalSpace;


	public static PageInputDialog create(final Window parent){
		final PageInputDialog dialog = new PageInputDialog(parent);
		dialog.initialize();

		return dialog;
	}


	private PageInputDialog(final Window parent){
		super(parent, ModalityType.APPLICATION_MODAL);

		// Initialize spinner model with range [1, 1] (will be updated dynamically)
		final SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 1, 1);
		pageSpinner = new JSpinner(spinnerModel);

		configureSpinnerClamping();

		imageHolder = ScaledImage.create();

		previewScrollPane = new JScrollPane(imageHolder);
		previewScrollPane.setBorder(BorderFactory.createEtchedBorder());

		previewScrollPane.getVerticalScrollBar()
			.setUnitIncrement(20);
		previewScrollPane.getHorizontalScrollBar()
			.setUnitIncrement(20);
	}

	/**
	 * Configures the JSpinner editor to strictly clamp manual inputs, Enter key presses,
	 * focus loss, and arrow key edits within [minimum, maximum].
	 */
	private void configureSpinnerClamping(){
		final JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)pageSpinner.getEditor();
		final JFormattedTextField textField = editor.getTextField();

		// Set commit policy to COMMIT (forces text parsing on value change)
		final JFormattedTextField.AbstractFormatter formatter = textField.getFormatter();
		if(formatter instanceof DefaultFormatter defaultFormatter){
			// Disallow arbitrary invalid input
			defaultFormatter.setAllowsInvalid(false);
			// Commit instantly on typing
			defaultFormatter.setCommitsOnValidEdit(true);
		}

		// Intercept Enter key and focus loss to force clamping
		textField.addActionListener(e -> commitAndClampSpinner());
		textField.addFocusListener(new java.awt.event.FocusAdapter(){
			@Override
			public void focusLost(final java.awt.event.FocusEvent e){
				commitAndClampSpinner();
			}
		});

		// Ensure arrow keys up/down inside the text field trigger model clamping
		textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), ACTION_COMMIT_AND_CLAMP);
		textField.getActionMap().put(ACTION_COMMIT_AND_CLAMP, new AbstractAction(){
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent e){
				commitAndClampSpinner();
			}
		});
	}

	/**
	 * Parses current text/value and forces it within the model's [min, max] range.
	 */
	private void commitAndClampSpinner(){
		final JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)pageSpinner.getEditor();
		final JFormattedTextField textField = editor.getTextField();

		int value;
		try{
			// Try parsing text typed by user manually
			final String text = textField.getText().trim();
			value = Integer.parseInt(text);
		}
		catch(final NumberFormatException e){
			// Fallback to current model value if parsing fails
			value = (Integer)pageSpinner.getValue();
		}

		final SpinnerNumberModel model = (SpinnerNumberModel)pageSpinner.getModel();
		final int min = ((Number)model.getMinimum()).intValue();
		final int max = ((Number)model.getMaximum()).intValue();

		// Clamp the value between min and max
		final int clampedValue = Math.clamp(value, min, max);

		pageSpinner.setValue(clampedValue);
		textField.setText(String.valueOf(clampedValue));
	}

	/**
	 * Dynamically updates the upper page limit when a document is opened.
	 */
	private void updatePageLimit(final int maxPages){
		final int validMax = Math.max(1, maxPages);
		final SpinnerNumberModel model = (SpinnerNumberModel)pageSpinner.getModel();
		model.setMaximum(validMax);

		// Clamp current value if it exceeds new maximum
		final int currentValue = (Integer)pageSpinner.getValue();
		if(currentValue > validMax)
			pageSpinner.setValue(validMax);
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
		GUIHelper.addLabeledComponent(contentPanel, I18N.t("dialog.preferred.page.input.label"), pageSpinner);
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
	public void loadData(final String uri, final Integer page, final Rectangle initialCrop){
		isSaved = false;

		// If we are opening a different file, we must clear the cache and close the old document
		if(this.documentUri == null || !this.documentUri.equals(uri)){
			closeCurrentDocument();

			this.documentUri = uri;
		}

		this.cropRectInOriginalSpace = initialCrop;

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
			imageHolder.setRectangularImage(null);
			currentOriginalImage = null;
			return;
		}

		final int page = (Integer)pageSpinner.getValue();

		// If the page is already in the cache, show it immediately without a background task
		if(pageCache.containsKey(page)){
			displayImage(pageCache.get(page));

			return;
		}

		final SwingWorker<BufferedImage, Void> currentRenderWorker = new SwingWorker<>(){
			@Override
			protected BufferedImage doInBackground(){
				// Render the page in an isolated, thread-safe manner
				return renderPage(documentUri, page);
			}

			@Override
			protected void done(){
				if(isCancelled())
					return;

				try{
					final BufferedImage img = get();
					if(img != null){
						// Cache the successfully rendered page
						pageCache.put(page, img);

						// Only update if this is still the currently requested page
						if(page == (Integer)pageSpinner.getValue())
							displayImage(img);
					}
					else{
						imageHolder.setRectangularImage(null);
						currentOriginalImage = null;
					}
				}
				catch(final Exception e){
					LOGGER.error("Error rendering preview", e);

					imageHolder.setRectangularImage(null);
					currentOriginalImage = null;
				}
			}
		};
		currentRenderWorker.execute();
	}

	/**
	 * Displays the unscaled image and fits it to the window.
	 */
	private void displayImage(final BufferedImage img){
		this.currentOriginalImage = img;
		imageHolder.setRectangularImage(img);
		imageHolder.setCrop(cropRectInOriginalSpace);

		// Fit to window initially
		SwingUtilities.invokeLater(this::fitToWindow);
	}

	/**
	 * Calculates the scale factor to fit the entire page inside the current viewport.
	 */
	private void fitToWindow(){
		if(currentOriginalImage == null)
			return;

		final Dimension viewportSize = previewScrollPane.getViewport().getExtentSize();
		if(viewportSize.width <= 0 || viewportSize.height <= 0)
			return;

		final double scaleX = (double)viewportSize.width / currentOriginalImage.getWidth();
		final double scaleY = (double)viewportSize.height / currentOriginalImage.getHeight();

		// Use the smaller scale to ensure the whole page fits
		final double fitScale = Math.min(scaleX, scaleY);

		applyZoom(fitScale);
		previewScrollPane.getViewport().setViewPosition(new Point(0, 0));
	}

	/**
	 * Scales the original image dimensions by the given zoom factor.
	 */
	private void applyZoom(final double newZoom){
		if(currentOriginalImage == null)
			return;

		// Constrain zoom between 10% and 500%
		this.currentZoom = Math.clamp(newZoom, 0.1, 5.);

		final int w = (int)(currentOriginalImage.getWidth() * currentZoom);
		final int h = (int)(currentOriginalImage.getHeight() * currentZoom);

		imageHolder.setPreferredSize(new Dimension(w, h));
		imageHolder.revalidate();
		imageHolder.repaint();
	}

	/**
	 * Renders a specific page of a document to an Image.
	 * Uses Apache PDFBox for PDF files and ImageIO (with TwelveMonkeys) for images/TIFFs.
	 */
	private BufferedImage renderPage(final String uri, final int page){
		final File file = new File(uri);
		if(!file.exists())
			return null;

		final String lowerUri = uri.toLowerCase();

		// --- 1. PDF Rendering (Apache PDFBox 3.x) ---
		if(lowerUri.endsWith(DocumentPartListPanel.EXTENSION_PDF)){
			try(final PDDocument document = Loader.loadPDF(file)){
				final int totalPages = document.getNumberOfPages();
				// Update spinner maximum on the EDT thread
				SwingUtilities.invokeLater(() -> updatePageLimit(totalPages));

				final PDFRenderer renderer = new PDFRenderer(document);
				if(page > 0 && page <= totalPages){
					final Image renderedImg = renderer.renderImageWithDPI(page - 1, 150, ImageType.RGB);
					if(renderedImg instanceof BufferedImage bufferedImage)
						return bufferedImage;

					final BufferedImage bimage = new BufferedImage(renderedImg.getWidth(null),
						renderedImg.getHeight(null), BufferedImage.TYPE_INT_RGB);
					final java.awt.Graphics2D g = bimage.createGraphics();
					g.drawImage(renderedImg, 0, 0, null);
					g.dispose();
					return bimage;
				}
			}
			catch(final IOException e){
				// Log only if it's not a normal interruption/closed channel due to fast navigation
				if(!(e instanceof java.nio.channels.ClosedChannelException))
					LOGGER.error("Error reading PDF: {}", e.getMessage());
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
						final int totalPages = reader.getNumImages(true);
						SwingUtilities.invokeLater(() -> updatePageLimit(totalPages));

						if(page > 0 && page <= totalPages)
							return reader.read(page - 1);
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

	private void closeCurrentDocument(){
		pageCache.clear();
		cropRectInOriginalSpace = null;
		currentOriginalImage = null;
		imageHolder.setRectangularImage(null);
	}

	@Override
	public void dispose(){
		// Ensure the PDF document is closed when the dialog is destroyed
		closeCurrentDocument();

		super.dispose();
	}

	public void save(){
		isSaved = true;

		cropRectInOriginalSpace = imageHolder.getCrop();

		setVisible(false);
	}

	public boolean isSaved(){
		return isSaved;
	}

	public Integer getPage(){
		return (Integer)pageSpinner.getValue();
	}

	public Rectangle getCropRect(){
		return cropRectInOriginalSpace;
	}

}
