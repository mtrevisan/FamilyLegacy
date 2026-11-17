/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.components.lists;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.imaging.FaceDetector;
import io.github.mtrevisan.familylegacy.imaging.FaceDetectors;
import io.github.mtrevisan.familylegacy.imaging.PortraitCrops;
import io.github.mtrevisan.familylegacy.ui.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.images.ScaledImage;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;


/**
 * Defines the crop of an image: drawn by hand, or, when the face detector plugin is there, proposed with a click on a face (the whole head, in
 * the proportions of the portrait; it can be edited as the one drawn by hand). The faces are looked for once, away from the event dispatch
 * thread, when the dialog is shown.
 */
public class ImageCropDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(ImageCropDialog.class);

	private static final float MAX_SCREEN_FRACTION = 0.75f;


	private BufferedImage image;
	private ScaledImage imageHolder;
	private boolean viewOnly;
	private SwingWorker<List<PortraitCrops.Proposal>, Void> faceSearch;

	private boolean isSaved;


	public static ImageCropDialog create(final Window parent){
		final ImageCropDialog dialog = new ImageCropDialog(parent);
		dialog.initialize(false);
		return dialog;
	}

	public static ImageCropDialog createViewOnly(final Window parent){
		final ImageCropDialog dialog = new ImageCropDialog(parent);
		dialog.initialize(true);
		return dialog;
	}


	private ImageCropDialog(final Window parent){
		super(parent, ModalityType.APPLICATION_MODAL);
	}


	private void initialize(final boolean viewOnly){
		initComponents(viewOnly);

		initLayout();
	}

	private void initComponents(final boolean viewOnly){
		this.viewOnly = viewOnly;

		setTitle(I18N.t("dialog.preferred.image.crop.title"));

		if(viewOnly)
			imageHolder = ScaledImage.createViewOnly();
		else
			imageHolder = ScaledImage.create();
	}

	//http://www.migcalendar.com/miglayout/cheatsheet.html
	private void initLayout(){
		setLayout(new MigLayout("ins " + GUIHelper.DIALOG_MARGIN + ",hidemode 3,fillx,wrap 2", "[]", "[]"));

		final JPanel recordPanel = GUIHelper.createLabelFieldPanel(0, "[grow,fill]");
		GUIHelper.addComponent(recordPanel, imageHolder);
		add(recordPanel, "center,grow,push");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(this,
			this::save,
			() -> setVisible(false));
		add(buttonPanel, BorderLayout.SOUTH);
	}

	public void loadData(final String filename, final Rectangle crop) throws IOException{
		final File file = FileHelper.loadFile(filename);
		if(file == null || !file.exists())
			throw new IOException(I18N.t("dialog.preferred.image.no_file"));

		loadData(file, crop);
	}

	public void loadData(final File file, final Rectangle crop) throws IOException{
		final BufferedImage newImage = ResourceHelper.readBufferedImage(file);
		//the callers show the error, and must not use the previous image
		if(newImage == null)
			throw new IOException(I18N.tf("error.image", file.getAbsolutePath()));

		isSaved = false;
		cancelFaceSearch();
		imageHolder.setCropProposals(null);
		imageHolder.setToolTipText(null);
		image = newImage;
		imageHolder.setRectangularImage(image);
		imageHolder.setCrop(crop);
		// Set the image holder to the original image size initially
		imageHolder.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));

		// Adjust the dialog size to fit the image (or a scaled version)
		adjustDialogSize();

		setLocationRelativeTo(getOwner());
	}

	/**
	 * Adjusts the dialog size so that it fits the image within 75% of the screen,
	 * preserving aspect ratio and accounting for extra components (button panel, borders).
	 */
	private void adjustDialogSize(){
		// First, pack to get the natural size and measure extras
		pack();

		final int extraWidth = getWidth() - imageHolder.getWidth();
		final int extraHeight = getHeight() - imageHolder.getHeight();

		// Maximum dialog size: 75% of screen
		final Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
			.getMaximumWindowBounds();
		final int maxDialogWidth = (int)(screenBounds.width * MAX_SCREEN_FRACTION);
		final int maxDialogHeight = (int)(screenBounds.height * MAX_SCREEN_FRACTION);

		// Maximum image size after deducting extras
		final int maxImageWidth = Math.max(10, maxDialogWidth - extraWidth);
		final int maxImageHeight = Math.max(10, maxDialogHeight - extraHeight);

		// Desired image size (original)
		int desiredWidth = image.getWidth();
		int desiredHeight = image.getHeight();

		// Scale if needed
		if(desiredWidth > maxImageWidth || desiredHeight > maxImageHeight){
			final double scaleX = (double)maxImageWidth / desiredWidth;
			final double scaleY = (double)maxImageHeight / desiredHeight;
			final double scale = Math.min(scaleX, scaleY);
			desiredWidth = Math.max(10, (int)(desiredWidth * scale));
			desiredHeight = Math.max(10, (int)(desiredHeight * scale));
		}

		// Set the image holder's preferred size to the (possibly scaled) size
		imageHolder.setPreferredSize(new Dimension(desiredWidth, desiredHeight));

		// Compute the desired dialog size
		int dialogWidth = desiredWidth + extraWidth;
		int dialogHeight = desiredHeight + extraHeight;

		// Cap the dialog size to the maximum allowed
		dialogWidth = Math.min(dialogWidth, maxDialogWidth);
		dialogHeight = Math.min(dialogHeight, maxDialogHeight);

		// Apply the size to the dialog
		setSize(dialogWidth, dialogHeight);

		// Ensure the dialog is not larger than the screen bounds (safety)
		if(getWidth() > screenBounds.width || getHeight() > screenBounds.height)
			setSize(Math.min(getWidth(), screenBounds.width),
				Math.min(getHeight(), screenBounds.height));
	}

	/** Looks for the faces when the dialog is shown (not when the image is only read), and gives up when it is hidden. */
	@Override
	public void setVisible(final boolean visible){
		if(visible)
			startFaceSearch();
		else
			cancelFaceSearch();

		super.setVisible(visible);
	}

	/** Finds the faces once for the image, away from the event dispatch thread: the proposals appear when they are ready. */
	private void startFaceSearch(){
		cancelFaceSearch();
		if(viewOnly || image == null || imageHolder.hasCropProposals())
			return;

		final BufferedImage target = image;
		final SwingWorker<List<PortraitCrops.Proposal>, Void> search = new SwingWorker<>(){
			@Override
			protected List<PortraitCrops.Proposal> doInBackground() throws IOException{
				final Optional<FaceDetector> detector = FaceDetectors.available();
				if(detector.isEmpty())
					return List.of();

				final List<Rectangle> faces = detector.get().detectFaces(target);
				return PortraitCrops.proposals(faces, target.getWidth(), target.getHeight());
			}

			@Override
			protected void done(){
				if(isCancelled() || image != target)
					return;

				try{
					final List<PortraitCrops.Proposal> proposals = get();
					imageHolder.setCropProposals(proposals);
					imageHolder.setToolTipText(proposals.isEmpty()? null: I18N.t("dialog.preferred.image.crop.faces.tooltip"));
				}
				catch(final ExecutionException ee){
					//the plugin cannot work (missing native library, ...): the crop is only drawn by hand
					LOGGER.warn("The face detector failed", ee.getCause());
				}
				catch(final InterruptedException ie){
					Thread.currentThread().interrupt();
				}
			}
		};
		faceSearch = search;
		search.execute();
	}

	private void cancelFaceSearch(){
		if(faceSearch != null){
			faceSearch.cancel(true);
			faceSearch = null;
		}
	}

	public void save(){
		isSaved = true;

		setVisible(false);
	}

	public boolean isSaved(){
		return isSaved;
	}

	public BufferedImage getImage(){
		return image;
	}

	public Rectangle getCrop(){
		return imageHolder.getCrop();
	}

}