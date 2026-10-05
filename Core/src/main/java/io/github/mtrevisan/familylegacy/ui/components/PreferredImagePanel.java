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

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.CropReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


/**
 * Panel for selecting and managing a preferred image associated with a record.
 * The image is referenced via a Source record and can be cropped.
 * <p>
 * Structure:
 * <pre>
 * struct {
 *   uri: Uri
 *   crop?: CropRect
 * }
 * </pre>
 */
public class PreferredImagePanel extends JPanel{

	public static final String TAG_URI = "URI";
	public static final String TAG_CROP = "CROP";

	public static final Icon PLACEHOLDER_ICON = createPlaceholderIcon();

	private static final int MAX_DIMENSION_SIZE = 80;


	private final ImageCropDialog cropDialog;

	private final Window parent;

	private final String path;

	private final JButton imageButton;

	private String uri;
	private Rectangle cropRect;


	/**
	 * Constructs a PreferredImagePanel.
	 *
	 * @param parent	the parent dialog (for showing modal dialogs)
	 */
	public PreferredImagePanel(final String path, final Window parent){
		this.parent = parent;

		this.path = path;

		this.imageButton = new JButton();

		cropDialog = ImageCropDialog.create(parent);


		initComponents();
	}


	private void initComponents(){
		setLayout(new MigLayout("ins 0,fillx", "[grow,align center]"));

		imageButton.setPreferredSize(new Dimension(80, 80));
		imageButton.setIcon(PLACEHOLDER_ICON);
		imageButton.setToolTipText(I18N.t("dialog.preferred.image.image.tooltip"));

		BindingsHelper.installBehavior(imageButton,
			() -> {
				final Icon icon = imageButton.getIcon();
				return (icon != null && icon != PLACEHOLDER_ICON);
			},
			this::setNewItem, null,
			null, null,
			builder -> {
				builder.item(I18N.t("popupmenu.set"), this::setNewItem);
				builder.separator();
				builder.selectionSensitiveItem(I18N.t("popupmenu.edit.crop"), this::editCrop);
				builder.separator();
				builder.selectionSensitiveItem(I18N.t("popupmenu.remove"), this::removeItem);
			}
		);

		add(imageButton, "growx");
	}

	/**
	 * Loads the preferred image data from the given record.
	 *
	 * @param record	the record containing the PREFERRED_IMAGE child
	 */
	public void load(final FLEFRecord record){
		clearImage();

		if(record == null || record.isEmpty())
			return;

		final FLEFRecord preferredImage = FLEFRecordHelper.findChild(record, path);
		if(preferredImage == null)
			return;

		final boolean isIndividual = IndividualAttributeHandler.TYPE.equalsIgnoreCase(record.getTag());
		uri = (isIndividual? IndividualReader.extractPreferredImageUri(record): GroupReader.extractPreferredImageUri(record));
		cropRect = (isIndividual? IndividualReader.extractPreferredImageCrop(record): GroupReader.extractPreferredImageCrop(record));

		try{
			cropDialog.loadData(uri, cropRect);
		}
		catch(final IOException ignored){}

		updatePreferredImage();
	}

	/**
	 * Saves the preferred image data to the given record.
	 * If no image is selected, does nothing.
	 *
	 * @param record	the record to save into
	 */
	public void save(final FLEFRecord record){
		if(StringUtils.isNotEmpty(uri)){
			final FLEFRecord preferredImage = FLEFRecordHelper.getOrCreateTargetNode(record, path);
			FLEFRecordHelper.updateChildValue(preferredImage, TAG_URI, uri);
			if(cropRect != null && !cropRect.isEmpty()){
				final FLEFRecord crop = FLEFRecordHelper.getOrCreateTargetNode(preferredImage, TAG_CROP);
				FLEFRecordHelper.updateChildValue(crop, CropReader.TAG_X, String.valueOf(cropRect.x));
				FLEFRecordHelper.updateChildValue(crop, CropReader.TAG_Y, String.valueOf(cropRect.y));
				FLEFRecordHelper.updateChildValue(crop, CropReader.TAG_WIDTH, String.valueOf(cropRect.width));
				FLEFRecordHelper.updateChildValue(crop, CropReader.TAG_HEIGHT, String.valueOf(cropRect.height));
			}
		}
	}

	/**
	 * Returns whether an image is currently selected.
	 *
	 * @return {@code true} if an image is selected, {@code false} otherwise
	 */
	public boolean hasImage(){
		return StringUtils.isNotEmpty(uri);
	}

	/**
	 * Edits the crop rectangle for the currently selected image.
	 */
	private void editCrop(){
		if(!hasImage()){
			setNewItem();

			return;
		}

		try{
			cropDialog.loadData(uri, cropRect);
			cropDialog.setVisible(true);

			if(cropDialog.isSaved()){
				cropRect = cropDialog.getCrop();

				updatePreferredImage();
			}
		}
		catch(final IOException ioe){
			ioe.printStackTrace();

			JOptionPane.showMessageDialog(parent,
				I18N.tf("error.image", uri),
				I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Opens a system file chooser to pick an image file and displays the {@link ImageCropDialog} to set the crop.
	 */
	private void setNewItem(){
		final File selectedFile = GUIHelper.selectImageFile(parent, uri);
		if(selectedFile == null)
			return;

		try{
			cropDialog.loadData(selectedFile, null);
			cropDialog.setVisible(true);

			if(cropDialog.isSaved()){
				uri = selectedFile.getAbsolutePath();
				cropRect = cropDialog.getCrop();

				updatePreferredImage();
			}
		}
		catch(final IOException ioe){
			ioe.printStackTrace();

			JOptionPane.showMessageDialog(parent,
				I18N.tf("error.image", uri),
				I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
		}
	}

	private void updatePreferredImage(){
		final BufferedImage img = cropDialog.getImage();
		if(img != null){
			BufferedImage source = img;
			if(cropRect != null){
				// Intersect rectangle with image bounds to prevent RasterFormatException
				final Rectangle imgBounds = new Rectangle(0, 0, img.getWidth(), img.getHeight());
				final Rectangle validCrop = cropRect.intersection(imgBounds);

				if(!validCrop.isEmpty())
					source = img.getSubimage(validCrop.x, validCrop.y, validCrop.width, validCrop.height);
			}

			final int origWidth = source.getWidth();
			final int origHeight = source.getHeight();
			int newWidth = MAX_DIMENSION_SIZE;
			int newHeight = MAX_DIMENSION_SIZE;
			if(origWidth > origHeight)
				newHeight = (int)(origHeight * (double)MAX_DIMENSION_SIZE / origWidth);
			else
				newWidth = (int)(origWidth * (double)MAX_DIMENSION_SIZE / origHeight);
			final Image scaled = source.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);

			imageButton.setIcon(new ImageIcon(scaled));
		}
		else
			imageButton.setIcon(PLACEHOLDER_ICON);
	}

	/**
	 * Removes the selected image after user confirmation.
	 */
	private void removeItem(){
		if(hasImage()){
			final int response = JOptionPane.showConfirmDialog(parent,
				I18N.t("confirmation.remove.image.message"),
				I18N.t("confirmation.remove.title"),
				JOptionPane.YES_NO_OPTION);
			if(response == JOptionPane.YES_OPTION)
				clearImage();
		}
	}

	private void clearImage(){
		uri = null;
		cropRect = null;
		imageButton.setIcon(PLACEHOLDER_ICON);
	}

	private static Icon createPlaceholderIcon(){
		final BufferedImage img = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2 = img.createGraphics();
		g2.setColor(Color.LIGHT_GRAY);
		g2.fillRect(0, 0, 80, 80);
		g2.setColor(Color.DARK_GRAY);
		g2.drawString(I18N.t("dialog.preferred.image.no.image"), 10, 45);
		g2.dispose();
		return new ImageIcon(img);
	}

}
