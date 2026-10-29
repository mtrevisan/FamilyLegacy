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
import io.github.mtrevisan.familylegacy.io.model.readers.CropReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.MediaHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.IOException;
import java.util.List;


/**
 * Panel for managing document parts with image cropping, page, and time range functionality.
 */
public class MediaPartListPanel extends AbstractListPanel<FLEFRecord>{

	public static final String EXTENSION_PDF = ResourceHelper.EXTENSION_PDF;


	private final String path;

	private final ImageCropDialog cropDialog;
	private final PageInputDialog pageDialog;
	private final TimeRangeDialog timeRangeDialog;

	private final MediaHandler documentHandler = MediaHandler.getInstance();


	public MediaPartListPanel(final String path, final Window parent, final String panelTitle, final FLEFModel model){
		super(parent, panelTitle, model);

		this.path = path;

		cropDialog = ImageCropDialog.create(parent);
		pageDialog = PageInputDialog.create(parent);
		timeRangeDialog = TimeRangeDialog.create(parent);

		initComponents();
	}


	@Override
	protected void initComponents(){
		super.initComponents();

		BindingsHelper.installBehavior(list,
			this::editDocumentPart, null,
			null, this::removeItem,
			builder -> {
				builder.item(I18N.t("popupmenu.add_existing"), this::addItem);
				builder.separator();
				builder.selectionSensitiveItem(I18N.t("popupmenu.edit.crop"), this::editDocumentPart);
				builder.selectionSensitiveItem(I18N.t("popupmenu.remove"), this::removeItem);
			}
		);
	}

	/**
	 * Edits the metadata (crop, page, or time range) for the currently selected document part.
	 * Delegates to the appropriate dialog based on the media type.
	 */
	private void editDocumentPart(){
		final int itemIndex = list.getSelectedIndex();
		if(itemIndex < 0)
			return;

		final FLEFRecord documentPart = listModel.get(itemIndex);
		final String documentId = FLEFRecordHelper.getChildValue(documentPart, SourceReader.TAG_MEDIA);
		final FLEFRecord document = model.getRecordById(documentId);

		if(document == null)
			return;

		final String uri = FLEFRecordHelper.getChildValue(document, MediaReader.TAG_URI);

		if(isPagedDocument(uri)){
			final Integer currentPage = parseInteger(FLEFRecordHelper.getChildValue(documentPart, SourceCitationReader.TAG_PAGE));
			final FLEFRecord crop = FLEFRecordHelper.findChild(documentPart, PreferredImagePanel.TAG_CROP);
			final Rectangle imageCropRect = CropReader.extractPreferredImageCrop(crop);

			pageDialog.loadData(uri, currentPage, imageCropRect);
			pageDialog.setVisible(true);
			if(pageDialog.isSaved()){
				updateDocumentPart(documentPart, SourceCitationReader.TAG_PAGE, String.valueOf(pageDialog.getPage()));
				updateCrop(documentPart, pageDialog.getCropRect());
			}
		}
		else if(isAudioVideo(uri)){
			// TODO test
			final String startTime = FLEFRecordHelper.getChildValue(documentPart, SourceCitationReader.TAG_START_TIME);
			final String endTime = FLEFRecordHelper.getChildValue(documentPart, SourceCitationReader.TAG_END_TIME);
			timeRangeDialog.loadData(uri, startTime, endTime);
			timeRangeDialog.setVisible(true);
			if(timeRangeDialog.isSaved()){
				updateDocumentPart(documentPart, SourceCitationReader.TAG_START_TIME, timeRangeDialog.getStart());
				updateDocumentPart(documentPart, SourceCitationReader.TAG_END_TIME, timeRangeDialog.getEnd());
			}
		}
		else{
			// Default to image crop
			final FLEFRecord crop = FLEFRecordHelper.findChild(documentPart, PreferredImagePanel.TAG_CROP);
			final Rectangle imageCropRect = CropReader.extractPreferredImageCrop(crop);

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
					I18N.t("error.title"),
					JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	@Override
	protected String getDisplayText(final FLEFRecord documentPart){
		if(documentPart != null){
			final String documentId = FLEFRecordHelper.getChildValue(documentPart, SourceReader.TAG_MEDIA);
			if(documentId != null){
				final FLEFRecord doc = model.getRecordById(documentId);
				if(doc != null){
					String text = documentHandler.getDisplayText(doc, model);

					// Append specific metadata to the label if present
					final String page = FLEFRecordHelper.getChildValue(documentPart, SourceCitationReader.TAG_PAGE);
					final String startTime = FLEFRecordHelper.getChildValue(documentPart, SourceCitationReader.TAG_START_TIME);
					final String endTime = FLEFRecordHelper.getChildValue(documentPart, SourceCitationReader.TAG_END_TIME);

					if(page != null)
						text += " [" + I18N.t("dialog.preferred.page.input.label") + ": " + page + "]";
					if(startTime != null || endTime != null)
						text += " [" + (startTime != null? startTime: "0") + " - " + (endTime != null? endTime: "∞") + "]";

					return text;
				}
			}
			return documentHandler.getDisplayText(documentPart, model);
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
				if(document != null){
					// Wrap the document in a media_part structure as required by the FLEF protocol
					final FLEFRecord documentPart = FLEFRecord.createChildWithTag(SourceCitationReader.TAG_MEDIA_PART);
					documentPart.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_MEDIA, document.getId()));

					final String uri = FLEFRecordHelper.getChildValue(document, MediaReader.TAG_URI);

					// Prompt the user with the correct dialog based on the file type
					if(isPagedDocument(uri)){
						pageDialog.loadData(uri, 1, null);
						pageDialog.setVisible(true);
						if(pageDialog.isSaved()){
							updateDocumentPart(documentPart, SourceCitationReader.TAG_PAGE, String.valueOf(pageDialog.getPage()));
							updateCrop(documentPart, pageDialog.getCropRect());
						}
					}
					else if(isAudioVideo(uri)){
						timeRangeDialog.loadData(uri, null, null);
						timeRangeDialog.setVisible(true);
						if(timeRangeDialog.isSaved()){
							updateDocumentPart(documentPart, SourceCitationReader.TAG_START_TIME, timeRangeDialog.getStart());
							updateDocumentPart(documentPart, SourceCitationReader.TAG_END_TIME, timeRangeDialog.getEnd());
						}
					}
					else{
						try{
							cropDialog.loadData(uri, null);
							cropDialog.setVisible(true);
							if(cropDialog.isSaved())
								extractCrop(documentPart);
						}
						catch(final IOException ioe){
							ioe.printStackTrace();

							JOptionPane.showMessageDialog(parent,
								I18N.tf("error.image", uri),
								I18N.t("error.title"),
								JOptionPane.ERROR_MESSAGE);
						}
					}
					result[0] = documentPart;
				}
			},
			MediaHandler.class);
		dialog.setVisible(true);

		return result[0];
	}

	// --- Helper methods for media type detection and safe updating ---

	private boolean isPagedDocument(final String uri){
		if(uri == null)
			return false;

		final String lowerUri = uri.toLowerCase();
		return (lowerUri.endsWith(EXTENSION_PDF) || lowerUri.endsWith(".tif") || lowerUri.endsWith(".tiff"));
	}

	private boolean isAudioVideo(final String uri){
		if(uri == null)
			return false;

		final String lowerUri = uri.toLowerCase();
		return (lowerUri.endsWith(".mp3") || lowerUri.endsWith(".wav") || lowerUri.endsWith(".mp4")
			|| lowerUri.endsWith(".avi") || lowerUri.endsWith(".mov"));
	}

	private void updateDocumentPart(final FLEFRecord documentPart, final String tag, final String value){
		if(StringUtils.isEmpty(value))
			FLEFRecordHelper.removeChild(documentPart, tag);
		else
			FLEFRecordHelper.updateChildValue(documentPart, tag, value.trim());
	}

	private Integer parseInteger(final String value){
		try{
			return (value != null? Integer.parseInt(value): null);
		}
		catch(final NumberFormatException e){
			return null;
		}
	}

	private void extractCrop(final FLEFRecord documentPart){
		updateCrop(documentPart, cropDialog.getCrop());
	}

	private void updateCrop(final FLEFRecord documentPart, final Rectangle documentCropRect){
		if(documentCropRect != null && !documentCropRect.isEmpty()){
			// Correctly save the crop under the media_part structure
			final FLEFRecord crop = FLEFRecordHelper.getOrCreateTargetNode(documentPart, PreferredImagePanel.TAG_CROP);
			FLEFRecordHelper.updateChildValue(crop, CropReader.TAG_X, String.valueOf(documentCropRect.x));
			FLEFRecordHelper.updateChildValue(crop, CropReader.TAG_Y, String.valueOf(documentCropRect.y));
			FLEFRecordHelper.updateChildValue(crop, CropReader.TAG_WIDTH, String.valueOf(documentCropRect.width));
			FLEFRecordHelper.updateChildValue(crop, CropReader.TAG_HEIGHT, String.valueOf(documentCropRect.height));
		}
		else
			FLEFRecordHelper.removeChild(documentPart, PreferredImagePanel.TAG_CROP);
	}

	@Override
	protected FLEFRecord showCreateNewDialog(){
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	protected FLEFRecord showEditDialog(final FLEFRecord record){
		if(record == null){
			JOptionPane.showMessageDialog(parent,
				I18N.t("error.record_not_found"),
				I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);

			return null;
		}

		// Resolve the underlying document to edit its properties
		final String documentId = FLEFRecordHelper.getChildValue(record, SourceReader.TAG_MEDIA);
		final FLEFRecord document = (documentId != null ? model.getRecordById(documentId) : record);

		if(document != null){
			final RecordDialog dialog = documentHandler.createEditDialog(parent, model, document);
			dialog.setVisible(true);
		}

		// Return the same media_part structure (it was updated in place)
		return record;
	}

	/**
	 * Loads document parts from the given record.
	 * Extracts the actual media_part structures, which hold the document reference
	 * and the associated crop/page/timing data.
	 *
	 * @param record	the record containing the document parts
	 */
	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		// Extract the structures instead of resolving the records,
		// so we keep the crop/page/timing metadata
		final List<FLEFRecord> documentParts = FLEFRecordHelper.extractStructures(record, path);
		setItems(documentParts);
	}

	/**
	 * Saves document part structures to the given record.
	 * Appends the media_part structures directly, as they already contain
	 * the document references and their crop data.
	 *
	 * @param record	the record to save to
	 */
	public void saveReferences(final FLEFRecord record){
		for(final FLEFRecord documentPart : getItems())
			record.addChild(documentPart);
	}

}
