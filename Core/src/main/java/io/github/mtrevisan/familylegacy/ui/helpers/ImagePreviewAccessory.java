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
package io.github.mtrevisan.familylegacy.ui.helpers;

import io.github.mtrevisan.familylegacy.ui.images.ScaledImage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Iterator;


/**
 * Accessory panel for a JFileChooser that displays a fast, asynchronous preview
 * of the selected image or PDF file.
 */
public class ImagePreviewAccessory extends JPanel implements PropertyChangeListener{

	// Delay in milliseconds before starting the preview loading.
	// Prevents spawning multiple threads when the user scrolls rapidly through files.
	private static final int DEBOUNCE_DELAY_MS = 150;

	// Maximum preview dimensions to calculate the optimal subsampling factor
	private static final int MAX_PREVIEW_WIDTH = 250;
	private static final int MAX_PREVIEW_HEIGHT = 250;

	private final ScaledImage previewImage = ScaledImage.createViewOnly();
	private SwingWorker<BufferedImage, Void> loaderWorker;

	// Timer used for debouncing rapid file selection changes
	private final Timer debounceTimer;

	private volatile File currentSelectedFile;


	public ImagePreviewAccessory(final JFileChooser chooser){
		chooser.addPropertyChangeListener(this);

		setLayout(new BorderLayout());

		setPreferredSize(new Dimension(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT));

		setBorder(BorderFactory.createTitledBorder("Preview"));

		add(previewImage, BorderLayout.CENTER);

		// Initialize the debounce timer.
		// When it fires, it triggers the actual loading process.
		debounceTimer = new Timer(DEBOUNCE_DELAY_MS, e -> startLoadingPreview(currentSelectedFile));
		debounceTimer.setRepeats(false);
	}


	@Override
	public void propertyChange(final PropertyChangeEvent evt){
		final String propertyName = evt.getPropertyName();

		if(JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(propertyName)){
			currentSelectedFile = null;
			debounceTimer.stop();

			cancelCurrentWorker();

			previewImage.setRectangularImage(null);
		}

		else if(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(propertyName)){
			final File selectedFile = (File)evt.getNewValue();

			currentSelectedFile = selectedFile;

			// Clear previous preview immediately while waiting for the debounce timer
			previewImage.setRectangularImage(null);

			// Cancel any ongoing image loading task
			cancelCurrentWorker();
			debounceTimer.stop();

			if(selectedFile != null && selectedFile.isFile())
				// Restart the timer. If the user selects another file within 150ms,
				// the timer resets and the preview is not loaded for the discarded file.
				debounceTimer.restart();
		}
	}

	private void cancelCurrentWorker(){
		if(loaderWorker != null && !loaderWorker.isDone()){
			loaderWorker.cancel(true);
			loaderWorker = null;
		}
	}

	private void startLoadingPreview(final File file){
		if(file == null || !file.isFile())
			return;

		loaderWorker = new SwingWorker<>(){
			@Override
			protected BufferedImage doInBackground(){
				return readSubsampledImage(file);
			}

			@Override
			protected void done(){
				if(isCancelled())
					return;

				// Ignore stale workers that completed after another file was already selected
				if(!file.equals(currentSelectedFile))
					return;

				try{
					previewImage.setRectangularImage(get());
				}
				catch(final Exception e){
					previewImage.setRectangularImage(null);
				}
			}
		};
		loaderWorker.execute();
	}

	/**
	 * Reads a subsampled (downscaled) version of the image directly from the disk.
	 * If the file is a PDF, it renders the first page.
	 *
	 * @param file the image or PDF file to read
	 * @return the preview BufferedImage, or null if unreadable
	 */
	private BufferedImage readSubsampledImage(final File file){
		final String fileName = file.getName().toLowerCase();

		// 1. Handle PDF files using PDFBox
		if(fileName.endsWith(ResourceHelper.EXTENSION_PDF))
			return renderPdfPreview(file);

		// 2. Handle standard images and TIFFs using ImageIO + TwelveMonkeys
		try(ImageInputStream iis = ImageIO.createImageInputStream(file)){
			if(iis == null)
				return null;

			final Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
			if(!readers.hasNext())
				return null;

			final ImageReader reader = readers.next();
			try{
				reader.setInput(iis, true, true);

				// Determine the original dimensions of the first page/image
				final int origWidth = reader.getWidth(0);
				final int origHeight = reader.getHeight(0);

				// Calculate the subsampling factor (skip N pixels)
				int subsampling = 1;
				if(origWidth > MAX_PREVIEW_WIDTH || origHeight > MAX_PREVIEW_HEIGHT){
					final int scaleX = Math.max(1, origWidth / MAX_PREVIEW_WIDTH);
					final int scaleY = Math.max(1, origHeight / MAX_PREVIEW_HEIGHT);
					subsampling = Math.max(scaleX, scaleY);
				}

				// Configure the reader to decode only a fraction of the image
				final ImageReadParam param = reader.getDefaultReadParam();
				param.setSourceSubsampling(subsampling, subsampling, 0, 0);

				return reader.read(0, param);
			}
			finally{
				reader.dispose();
			}
		}
		catch(final Exception e){
			// Ignore reading errors for unsupported or corrupt files
			return null;
		}
	}

	/**
	 * Renders the first page of a PDF file as a BufferedImage.
	 */
	private BufferedImage renderPdfPreview(final File file){
		try(PDDocument document = Loader.loadPDF(file)){
			if(document.getNumberOfPages() > 0){
				final PDFRenderer renderer = new PDFRenderer(document);

				// Render first page at 72 DPI (standard baseline)
				final BufferedImage fullPage = renderer.renderImageWithDPI(0, 72, ImageType.RGB);

				// Scale down the image efficiently using Graphics2D to fit the preview area
				final int width = fullPage.getWidth();
				final int height = fullPage.getHeight();

				final double scale = Math.min((double)MAX_PREVIEW_WIDTH / width, (double)MAX_PREVIEW_HEIGHT / height);
				if(scale < 1.0){
					final int newWidth = Math.max(1, (int)(width * scale));
					final int newHeight = Math.max(1, (int)(height * scale));

					final BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
					final Graphics2D g2 = scaled.createGraphics();
					g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
					g2.drawImage(fullPage, 0, 0, newWidth, newHeight, null);
					g2.dispose();

					return scaled;
				}

				// If the rendered page is already small enough, return it directly
				return fullPage;
			}
		}
		catch(final Exception e){
			// Ignore and return null for unreadable/corrupt PDFs
		}
		return null;
	}

}
