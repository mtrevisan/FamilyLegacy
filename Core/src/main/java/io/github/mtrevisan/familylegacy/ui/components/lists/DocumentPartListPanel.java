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
package io.github.mtrevisan.familylegacy.ui.components.lists;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.CropReader;
import io.github.mtrevisan.familylegacy.io.model.readers.DocumentReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.ui.components.ImageCropDialog;
import io.github.mtrevisan.familylegacy.ui.components.PreferredImagePanel;
import io.github.mtrevisan.familylegacy.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.IOException;
import java.util.List;


/**
 * Panel for managing document parts with image cropping functionality.
 */
public class DocumentPartListPanel extends AbstractListPanel<FLEFRecord>{

	private final String path;

	private final ImageCropDialog cropDialog;

	private final DocumentHandler documentHandler = DocumentHandler.getInstance();


	public DocumentPartListPanel(final String path, final Window parent, final String panelTitle, final FLEFModel model){
		super(parent, panelTitle, model);

		this.path = path;

		cropDialog = ImageCropDialog.create(parent);


		initComponents();
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		BindingsHelper.installBehavior(list,
			this::editCrop, null,
			null, this::removeItem,
			builder -> {
				builder.item(I18N.t("popupmenu.add.existing"), this::addItem);
				builder.separator();
				builder.selectionSensitiveItem(I18N.t("popupmenu.edit.crop"), this::editCrop);
				builder.selectionSensitiveItem(I18N.t("popupmenu.remove"), this::removeItem);
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


		final FLEFRecord documentPart = listModel.get(itemIndex);
		final FLEFRecord crop = FLEFRecordHelper.findChild(documentPart, PreferredImagePanel.TAG_CROP);
		Rectangle imageCropRect = CropReader.extractPreferredImageCrop(crop);
		final String documentId = FLEFRecordHelper.findChild(documentPart, SourceReader.TAG_DOCUMENT)
			.getValue();
		final FLEFRecord document = model.getRecordById(documentId);
		final String uri = FLEFRecordHelper.getChildValue(document, DocumentReader.TAG_URI);

		try{
			cropDialog.loadData(uri, imageCropRect);
			cropDialog.setVisible(true);

			if(cropDialog.isSaved())
				extractCrop(documentPart);
		}
		catch(final IOException ioe){
			ioe.printStackTrace();

			JOptionPane.showMessageDialog(parent,
				I18N.tf("error.image", uri),
				I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	protected String getDisplayText(final FLEFRecord documentPart){
		if(documentPart != null){
			FLEFRecord doc = documentPart;
			if(documentPart.getId() == null){
				final String documentId = SourceReader.extractDocument(documentPart);
				doc = model.getRecordById(documentId);
			}
			return documentHandler.getDisplayText(doc, model);
		}

		return "--";
	}

	@Override
	protected FLEFRecord showAddDialog(){
		final FLEFRecord[] result = {null};
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(parent, model,
			(record, handler) -> {
				final FLEFRecord document = model.getRecordById(record.getId());
				if(document != null && !listModel.contains(document)){
					final String uri = FLEFRecordHelper.getChildValue(document, DocumentReader.TAG_URI);

					try{
						cropDialog.loadData(uri, null);
						cropDialog.setVisible(true);

						if(cropDialog.isSaved())
							extractCrop(record);
					}
					catch(final IOException ioe){
						ioe.printStackTrace();

						JOptionPane.showMessageDialog(parent,
							I18N.tf("error.image", uri),
							I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
					}

					result[0] = document;
				}
			},
			DocumentHandler.class);
		dialog.setVisible(true);

		return result[0];
	}

	private void extractCrop(final FLEFRecord documentPart){
		final Rectangle documentCropRect = cropDialog.getCrop();
		if(documentCropRect != null && !documentCropRect.isEmpty()){
			// FIXME temporarily save under DOCUMENT
			final FLEFRecord crop = FLEFRecordHelper.getOrCreateTargetNode(documentPart, PreferredImagePanel.TAG_CROP);
			FLEFRecordHelper.updateChildValue(crop, CropReader.TAG_X, String.valueOf(documentCropRect.x));
			FLEFRecordHelper.updateChildValue(crop, CropReader.TAG_Y, String.valueOf(documentCropRect.y));
			FLEFRecordHelper.updateChildValue(crop, CropReader.TAG_WIDTH, String.valueOf(documentCropRect.width));
			FLEFRecordHelper.updateChildValue(crop, CropReader.TAG_HEIGHT, String.valueOf(documentCropRect.height));
		}
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord record){
		if(record == null){
			JOptionPane.showMessageDialog(parent,
				I18N.t("error.record.not.found"),
				I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);

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
			final FLEFRecord part = FLEFRecord.createChildWithTag(SourceCitationReader.TAG_DOCUMENT_PART);
			part.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_DOCUMENT, documentPart.getId()));
			final FLEFRecord crop = FLEFRecordHelper.findChild(documentPart, PreferredImagePanel.TAG_CROP);
			part.addChild(crop);
			record.addChild(part);
		}
	}

}
