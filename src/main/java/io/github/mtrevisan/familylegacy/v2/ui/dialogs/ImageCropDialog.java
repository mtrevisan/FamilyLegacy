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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.ui.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ResourceHelper;
import io.github.mtrevisan.familylegacy.v2.ui.images.ScaledImage;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serial;


public class ImageCropDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 3777867436237271707L;


	private BufferedImage image;
	private ScaledImage imageHolder;

	private boolean isSaved;


	public static ImageCropDialog create(final Dialog parent){
		final ImageCropDialog dialog = new ImageCropDialog(parent);
		dialog.initialize(false);
		return dialog;
	}

	public static ImageCropDialog createViewOnly(final Dialog parent){
		final ImageCropDialog dialog = new ImageCropDialog(parent);
		dialog.initialize(true);
		return dialog;
	}


	private ImageCropDialog(final Dialog parent){
		super(parent, ModalityType.APPLICATION_MODAL);
	}


	private void initialize(final boolean viewOnly){
		initComponents(viewOnly);

		initLayout();
	}

	private void initComponents(final boolean viewOnly){
		setTitle("Define crop");

		if(viewOnly)
			imageHolder = ScaledImage.createViewOnly();
		else
			imageHolder = ScaledImage.create();
	}

	//http://www.migcalendar.com/miglayout/cheatsheet.html
	private void initLayout(){
		GUIHelper.setLayoutLabelFieldPanel(this, 0, "[]");

		final JPanel recordPanel = GUIHelper.createLabelFieldPanel(0, "[grow,fill]");
		GUIHelper.addComponent(recordPanel, imageHolder);
		add(recordPanel, "grow,push");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			() -> setVisible(false));
		add(buttonPanel, BorderLayout.SOUTH);
	}

	public void loadData(final String filename, final Rectangle crop) throws IOException{
		final File file = FileHelper.loadFile(filename);
		if(file == null || !file.exists())
			throw new IOException("File does not exists");

		loadData(file, crop);
	}

	public void loadData(final File file, final Rectangle crop) throws IOException{
		final BufferedImage newImage = ResourceHelper.readImage(file);
		if(newImage == null){
			JOptionPane.showMessageDialog(getParent(),
				"Could not load image from the current source.",
				"Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

		isSaved = false;
		image = newImage;
		imageHolder.setRectangularImage(image);
		imageHolder.setCrop(crop);
		imageHolder.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));

		fitWindowToImage();
	}

	private void fitWindowToImage(){
		pack();

		final Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
			.getMaximumWindowBounds();
		if(getWidth() > screenBounds.width || getHeight() > screenBounds.height){
			final int maxWidth = Math.min(getWidth(), (int)(screenBounds.width * 0.3 * (4. / 3.)));
			final int maxHeight = Math.min(getHeight(), (int)(screenBounds.height * 0.3));
			setSize(maxWidth, maxHeight);
		}

		setLocationRelativeTo(getOwner());
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
