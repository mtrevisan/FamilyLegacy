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
package io.github.mtrevisan.familylegacy.v2.ui.components.lists;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.ImageCropDialog;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.MultiTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dialog;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.Serial;
import java.util.List;


/**
 * Panel for managing document parts with image cropping functionality.
 */
public class DocumentPartListPanel extends AbstractListPanel<FLEFRecord>{

	@Serial
	private static final long serialVersionUID = -1788729052005475640L;


	private static final String TAG_URI = "URI";
	private static final String TAG_CROP = "CROP";
	private static final String TAG_X = "X";
	private static final String TAG_Y = "Y";
	private static final String TAG_WIDTH = "WIDTH";
	private static final String TAG_HEIGHT = "HEIGHT";

	private static final String TAG_DOCUMENT = "DOCUMENT";
	private static final String TAG_DOCUMENT_PART = "DOCUMENT_PART";


	private final String path;

	private final ImageCropDialog cropDialog;

	private final RecordTypeHandler<?> documentHandler = HandlerRegistry.getHandler(DocumentHandler.class);


	public DocumentPartListPanel(final String path, final Dialog parent, final String panelTitle, final FLEFModel model){
		super(parent, panelTitle, model);

		this.path = path;

		cropDialog = ImageCropDialog.create(parent);


		initComponents();
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		GUIHelper.installBehavior(list,
			this::editCrop, null,
			null, this::removeItem,
			builder -> {
				builder.item("Add Existing…", this::addItem);
				builder.separator();
				builder.selectionSensitiveItem("Edit Crop…", this::editCrop);
				builder.selectionSensitiveItem("Remove", this::removeItem);
			}
		);
	}

	/**
	 * Edits the crop rectangle for the currently selected document part.
	 * <p>
	 * This method:
	 * <ol>
	 *   <li>Gets the selected document part from the list</li>
	 *   <li>Extracts the current crop rectangle (if any)</li>
	 *   <li>Loads the image and shows the crop dialog</li>
	 *   <li>If a crop is confirmed, updates the crop rectangle</li>
	 * </ol>
	 */
	private void editCrop(){
		final int itemIndex = list.getSelectedIndex();
		if(itemIndex < 0)
			return;


		final FLEFRecord documentPart = items.get(itemIndex);
		final FLEFRecord imageCrop = FLEFRecordHelper.findChild(documentPart, TAG_CROP);
		Rectangle imageCropRect = null;
		try{
			final int cropX = Integer.parseInt(FLEFRecordHelper.getChildValue(imageCrop, TAG_X));
			final int cropY = Integer.parseInt(FLEFRecordHelper.getChildValue(imageCrop, TAG_Y));
			final int cropWidth = Integer.parseInt(FLEFRecordHelper.getChildValue(imageCrop, TAG_WIDTH));
			final int cropHeight = Integer.parseInt(FLEFRecordHelper.getChildValue(imageCrop, TAG_HEIGHT));
			imageCropRect = new Rectangle(cropX, cropY, cropWidth, cropHeight);
		}
		catch(final NumberFormatException ignored){}
		final String documentId = documentPart.getId();
		final FLEFRecord document = model.getRecordById(documentId);
		final String uri = FLEFRecordHelper.getChildValue(document, TAG_URI);

		try{
			cropDialog.loadData(uri, imageCropRect);
			cropDialog.setVisible(true);

			if(cropDialog.isSaved()){
				final Rectangle documentCropRect = cropDialog.getCrop();
				if(documentCropRect != null && !documentCropRect.isEmpty()){
					// temporarily save under DOCUMENT
					final FLEFRecord crop = FLEFRecordHelper.getOrCreateTargetNode(documentPart, TAG_CROP);
					FLEFRecordHelper.updateChildValue(crop, TAG_X, String.valueOf(documentCropRect.x));
					FLEFRecordHelper.updateChildValue(crop, TAG_Y, String.valueOf(documentCropRect.y));
					FLEFRecordHelper.updateChildValue(crop, TAG_WIDTH, String.valueOf(documentCropRect.width));
					FLEFRecordHelper.updateChildValue(crop, TAG_HEIGHT, String.valueOf(documentCropRect.height));
				}
			}
		}
		catch(final IOException ioe){
			ioe.printStackTrace();

			JOptionPane.showMessageDialog(parent,
				"Error loading image for cropping: " + ioe.getMessage(),
				"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	protected String getDisplayText(final FLEFRecord documentPart){
		if(documentPart != null){
			FLEFRecord doc = documentPart;
			if(documentPart.getId() == null){
				final String documentId = FLEFRecordHelper.getChildValue(documentPart, TAG_DOCUMENT);
				doc = model.getRecordById(documentId);
			}
			return documentHandler.getDisplayText(doc, model);
		}

		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(parent, model, DocumentHandler.class);
		dialog.addPropertyChangeListener(MultiTypeSelectionDialog.PROPERTY_TYPE_SELECTED, e -> {
			final FLEFRecord selectedRecord = dialog.getSelectedRecord();
			final FLEFRecord document = model.getRecordById(selectedRecord.getId());
			if(document != null && !items.contains(document)){
				final String uri = FLEFRecordHelper.getChildValue(document, TAG_URI);

				try{
					cropDialog.loadData(uri, null);
					cropDialog.setVisible(true);

					if(cropDialog.isSaved()){
						final Rectangle documentCropRect = cropDialog.getCrop();
						if(documentCropRect != null && !documentCropRect.isEmpty()){
							// temporarily save under DOCUMENT
							final FLEFRecord crop = FLEFRecordHelper.getOrCreateTargetNode(selectedRecord, TAG_CROP);
							FLEFRecordHelper.updateChildValue(crop, TAG_X, String.valueOf(documentCropRect.x));
							FLEFRecordHelper.updateChildValue(crop, TAG_Y, String.valueOf(documentCropRect.y));
							FLEFRecordHelper.updateChildValue(crop, TAG_WIDTH, String.valueOf(documentCropRect.width));
							FLEFRecordHelper.updateChildValue(crop, TAG_HEIGHT, String.valueOf(documentCropRect.height));
						}
					}
				}
				catch(final IOException ioe){
					ioe.printStackTrace();

					JOptionPane.showMessageDialog(parent,
						"Error loading image for cropping: " + ioe.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
				}

				result[0] = document;
			}
		});
		dialog.setVisible(true);

		return result[0];
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord record){
		if(record == null){
			JOptionPane.showMessageDialog(parent, documentHandler.getLabel() + " not found", "Error",
				JOptionPane.ERROR_MESSAGE);

			return null;
		}

		final JDialog dialog = documentHandler.createEditDialog(parent, model, record);
		dialog.setVisible(true);

		// Return the same record (it was updated in place)
		return record;
	}

	/**
	 * Loads document parts from the given record.
	 * <p>
	 * Resolves document references to actual document records from the model.
	 *
	 * @param record	the record containing the document parts
	 */
	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		final List<FLEFRecord> referencedEntities = FLEFRecordHelper.extractRecordsFromReference(record, path, model);
		setItems(referencedEntities);
	}

	/**
	 * Saves document part references to the given record.
	 * <p>
	 * Creates {@code DOCUMENT_PART} records with references to documents and their crop data.
	 *
	 * @param record	the record to save to
	 */
	public void saveReferences(final FLEFRecord record){
		for(final FLEFRecord documentPart : getItems()){
			final FLEFRecord part = FLEFRecord.createChildWithTag(TAG_DOCUMENT_PART);
			part.addChild(FLEFRecord.createChildWithTagAndValue(TAG_DOCUMENT, documentPart.getId()));
			final FLEFRecord crop = FLEFRecordHelper.findChild(documentPart, TAG_CROP);
			part.addChild(crop);
			record.addChild(part);
		}
	}

}
