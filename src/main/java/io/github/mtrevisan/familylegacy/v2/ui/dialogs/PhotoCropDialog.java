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
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serial;


public final class PhotoCropDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 3777867436237271707L;


	private BufferedImage image;
	private ScaledImage imageHolder;

	private boolean isSaved;


	public static PhotoCropDialog create(final Dialog parent){
		final PhotoCropDialog dialog = new PhotoCropDialog(parent);
		dialog.initialize(parent, false);
		return dialog;
	}

	public static PhotoCropDialog createViewOnly(final Dialog parent){
		final PhotoCropDialog dialog = new PhotoCropDialog(parent);
		dialog.initialize(parent, true);
		return dialog;
	}


	private PhotoCropDialog(final Dialog parent){
		super(parent, true);
	}


	private void initialize(final Dialog parent, final boolean viewOnly){
		initComponents(viewOnly);

		initLayout();

		pack();

		// Calculate dialog size dynamically based on screen bounds (e.g. 60% width, 70% height)
		final Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
			.getMaximumWindowBounds();
		final int width = (int)(screenBounds.width * 0.3 * (4. / 3.));
		final int height = (int)(screenBounds.height * 0.3);
		setSize(width, height);

		setLocationRelativeTo(parent);
	}

	private void initComponents(final boolean viewOnly){
		setTitle("Define crop");

		if(viewOnly)
			imageHolder = ScaledImage.createViewOnly();
		else{
			imageHolder = ScaledImage.create();

			imageHolder.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		}
	}

	//http://www.migcalendar.com/miglayout/cheatsheet.html
	private void initLayout(){
		final JPanel recordPanel = new JPanel();
		recordPanel.setLayout(new MigLayout(StringUtils.EMPTY, "[grow]", "[grow,fill]"));

		recordPanel.add(imageHolder, "grow");

		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]", "[grow]"));
		add(recordPanel, "grow");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			() -> setVisible(false));
		add(buttonPanel, BorderLayout.SOUTH);
	}

	public void loadData(final String filename) throws IOException{
		final File file = FileHelper.loadFile(filename);
		if(file == null || !file.exists())
			throw new IOException("File does not exists");

		loadData(file);
	}

	public void loadData(final File file) throws IOException{
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
