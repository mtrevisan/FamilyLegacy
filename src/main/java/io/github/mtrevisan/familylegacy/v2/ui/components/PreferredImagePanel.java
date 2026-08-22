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

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.ImageCropDialog;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serial;


/**
 * Panel for selecting and managing a preferred image associated with a record.
 * The image is referenced via a Source record, and can be cropped.
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

	@Serial
	private static final long serialVersionUID = 6086547520717314054L;


	private static final String TAG_URI = "URI";
	private static final String TAG_CROP = "CROP";
	private static final String TAG_X = "X";
	private static final String TAG_Y = "Y";
	private static final String TAG_WIDTH = "WIDTH";
	private static final String TAG_HEIGHT = "HEIGHT";

	public static final Icon PLACEHOLDER_ICON = createPlaceholderIcon();

	private static final int MAX_DIMENSION_SIZE = 80;


	private final ImageCropDialog cropDialog;

	private final Dialog parent;

	private final String path;

	private final JButton imageButton;

	private String uri;
	private Rectangle cropRect;


	/**
	 * Constructs a PreferredImagePanel.
	 *
	 * @param parent	the parent dialog (for showing modal dialogs)
	 */
	public PreferredImagePanel(final String path, final Dialog parent){
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
		imageButton.setToolTipText("Left-click to select an image, right-click for options");

		GUIHelper.installBehavior(imageButton,
			null, null,
			null, null,
			builder -> {
				builder.item("Create New…", this::createNewItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit Crop…", this::editCrop);
				builder.selectionSensitiveItem("Remove", this::removeItem);
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

		uri = FLEFRecordHelper.getChildValue(preferredImage, TAG_URI);
		loadCropRectangle(preferredImage);

		try{
			cropDialog.loadData(uri, cropRect);
		}
		catch(final IOException ignored){}

		updatePreferredImage();
	}

	@SuppressWarnings("DataFlowIssue")
	private void loadCropRectangle(final FLEFRecord preferredImage){
		cropRect = null;
		try{
			final FLEFRecord crop = FLEFRecordHelper.findChild(preferredImage, TAG_CROP);
			final int x = Integer.parseInt(FLEFRecordHelper.getChildValue(crop, TAG_X));
			final int y = Integer.parseInt(FLEFRecordHelper.getChildValue(crop, TAG_Y));
			final int width = Integer.parseInt(FLEFRecordHelper.getChildValue(crop, TAG_WIDTH));
			final int height = Integer.parseInt(FLEFRecordHelper.getChildValue(crop, TAG_HEIGHT));
			if(x >= 0 && y >= 0 && width >= 0 && height >= 0)
				cropRect = new Rectangle(x, y, width, height);
		}
		catch(final Exception ignored){}
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
				FLEFRecordHelper.updateChildValue(crop, TAG_X, String.valueOf(cropRect.x));
				FLEFRecordHelper.updateChildValue(crop, TAG_Y, String.valueOf(cropRect.y));
				FLEFRecordHelper.updateChildValue(crop, TAG_WIDTH, String.valueOf(cropRect.width));
				FLEFRecordHelper.updateChildValue(crop, TAG_HEIGHT, String.valueOf(cropRect.height));
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
			createNewItem();

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
				"Error loading image for cropping: " + ioe.getMessage(),
				"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Opens a system file chooser to pick an image file and displays the {@link ImageCropDialog} to set the crop.
	 */
	private void createNewItem(){
		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Select Image File");
		final String[] extensions = ImageIO.getReaderFileSuffixes();
		final String description = "Supported Images (" + String.join(", ", extensions) + ")";
		fileChooser.setFileFilter(new FileNameExtensionFilter(description, extensions));
		fileChooser.setAccessory(new ImagePreviewAccessory(fileChooser));
		final int userSelection = fileChooser.showOpenDialog(parent);
		if(userSelection != JFileChooser.APPROVE_OPTION)
			return;

		final File selectedFile = fileChooser.getSelectedFile();
		if(selectedFile == null || !selectedFile.exists()){
			JOptionPane.showMessageDialog(parent,
				"Selected file does not exist.",
				"Error", JOptionPane.ERROR_MESSAGE);

			return;
		}

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
				"Error loading image for cropping: " + ioe.getMessage(),
				"Error", JOptionPane.ERROR_MESSAGE);
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
				"Are you sure you want to remove the preferred image?",
				"Confirm Removal",
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
		g2.drawString("[No img]", 10, 45);
		g2.dispose();
		return new ImageIcon(img);
	}

}
