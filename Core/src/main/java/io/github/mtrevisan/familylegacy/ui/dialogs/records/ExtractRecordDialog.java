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
package io.github.mtrevisan.familylegacy.ui.dialogs.records;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.api.LinkRole;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundFilteredComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextArea;
import io.github.mtrevisan.familylegacy.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.ui.components.lists.MediaPartListPanel;
import io.github.mtrevisan.familylegacy.ui.components.lists.TextListPanel;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.PanelKey;
import io.github.mtrevisan.familylegacy.ui.dialogs.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.handlers.ExtractHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.util.LocaleHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Window;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * Dialog for editing an {@code EXTRACT_RECORD} according to FLEF 0.2.0.
 * <p>
 * Structure:
 * <pre>
 * record ExtractRecord {
 *   source: Xref&lt;SourceRecord&gt;
 *   media_part*: struct { media: Xref&lt;MediaRecord&gt;, page?: Int, crop?: CropRect, start_time?: Duration, end_time?: Duration }
 *   text?: Text
 *   type?: enum { verbatim, summarized, translated, normalized }
 *   locale?: LocaleCode
 *   remark*: Text
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 *
 *   require at_least_one(media_part, text)
 *   require every media_part.media in source.media
 * }
 * </pre>
 * <p>
 * The source of an extract opened from a citation is the source cited, and it cannot be changed there; the source of an extract
 * already cited cannot be changed either (every extract a citation references belongs to the source it cites). A media part whose
 * media is not a media of the source is added to the media of the source, if the user agrees.
 * <p>
 * Tabs:
 * Tab 1 (Properties): source, media parts, text, type, locale, remarks
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class ExtractRecordDialog extends BaseRecordDialog{

	private final JPanel propertiesPanel;

	private final EntityField sourceField;
	private final MediaPartListPanel mediaPartPanel;
	private final BoundTextArea textArea;
	private final BoundComboBox<String> typeCombo;
	private final BoundFilteredComboBox<String> localeCombo;
	private final TextListPanel remarkPanel;

	/** The media of the media parts that the source does not have yet, to add to it when the extract is saved. */
	private final Set<String> mediaToAddToSource = new LinkedHashSet<>();


	public static ExtractRecordDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, ExtractRecordDialog::new);
	}

	public static ExtractRecordDialog createEdit(final Window parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, ExtractRecordDialog::new);
	}


	private ExtractRecordDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, ExtractHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]10[]10[]10[]");

		sourceField = EntityField.createForRecordFromReference(ExtractReader.TAG_SOURCE, this, model, SourceHandler.class);
		mediaPartPanel = new MediaPartListPanel(ExtractReader.TAG_MEDIA_PART, this, I18N.t("dialog.extract.media.parts.title"), model);
		textArea = new BoundTextArea(ExtractReader.TAG_TEXT, 5, 40);
		typeCombo = new BoundComboBox<>(ExtractReader.TAG_TYPE, GUIHelper.fillCombo(ExtractReader.TYPES, null));
		typeCombo.setI18NPrefix("enum.extract_type");
		localeCombo = new BoundFilteredComboBox<>(ExtractReader.TAG_LOCALE,
			GUIHelper.fillCombo(LocaleHelper.getAvailableLanguageTags().toArray(String[]::new), null));
		remarkPanel = new TextListPanel(ExtractReader.TAG_REMARK, this, I18N.t("dialog.extract.remarks"), "dialog.extract.remark");

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, this.record)
			.withComponent(PanelKey.PRIVACY, ExtractReader.TAG_PRIVACY, null)
			.withComponent(PanelKey.AUDIT, ExtractReader.TAG_AUDIT, null)
			.build();

		components.bind(textArea);
		components.bind(typeCombo);
		components.bind(localeCombo);


		finalizeDialog(parent);

		//the source of an extract that is already cited is the source of its citations
		if(!isNew && !citationsOf(this.record.getId()).isEmpty())
			sourceField.setEnabled(false);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// source
		GUIHelper.addLabeledComponent(propertiesPanel, GUIHelper.requiredLabel("dialog.extract.source"), sourceField);

		// media parts
		GUIHelper.addComponent(propertiesPanel, mediaPartPanel);

		// text
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.l("dialog.extract.text"), textArea);

		// type
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.l("dialog.extract.type"), typeCombo);

		// locale
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.l("dialog.extract.locale"), localeCombo);

		// remarks
		GUIHelper.addComponent(propertiesPanel, remarkPanel);

		return propertiesPanel;
	}

	@Override
	protected JPanel createPrivacyPanel(){
		return components.getPanel(PanelKey.PRIVACY);
	}

	@Override
	protected JPanel createAuditPanel(){
		return components.getPanel(PanelKey.AUDIT);
	}


	/** Presets the source, from the citation the extract is created for: it cannot be changed then. */
	@Override
	public void link(final LinkRole role, final FLEFRecord record){
		if(role == LinkRole.SOURCE && record != null && record.getId() != null){
			final FLEFRecord source = model.getRecordById(record.getId());
			sourceField.setEntity(source != null? source: record);
			sourceField.setEnabled(false);
		}
	}


	@Override
	protected void loadData(){
		components.load(record);

		sourceField.load(record);
		mediaPartPanel.load(record);
		remarkPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(!sourceField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.extract.source")),
				tabbedPane, propertiesPanel, sourceField);

			return false;
		}
		if(mediaPartPanel.isEmpty() && textArea.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.at_least_one_of", I18N.t("dialog.extract.media_part"), I18N.t("dialog.extract.text")),
				tabbedPane, propertiesPanel, mediaPartPanel);

			return false;
		}

		//every extract a citation references belongs to the source it cites
		final String sourceId = sourceField.getEntity().getId();
		if(!isNew && !sourceId.equals(ExtractReader.extractSource(record)) && !citationsOf(record.getId()).isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.t("validation.extract.source_of_citations"),
				tabbedPane, propertiesPanel, sourceField);

			return false;
		}

		//every media of the extract is a media of the source
		mediaToAddToSource.clear();
		final FLEFRecord source = model.getRecordById(sourceId);
		final List<String> sourceMedia = (source != null
			? FLEFRecordHelper.findChildren(source, SourceReader.TAG_MEDIA).stream()
				.map(FLEFRecord::getValue)
				.toList()
			: List.of());
		for(final FLEFRecord mediaPart : mediaPartPanel.getItems()){
			final String mediaId = FLEFRecordHelper.getChildValue(mediaPart, ExtractReader.TAG_MEDIA);
			if(mediaId != null && !sourceMedia.contains(mediaId))
				mediaToAddToSource.add(mediaId);
		}
		if(source != null && !mediaToAddToSource.isEmpty()){
			final int answer = JOptionPane.showConfirmDialog(this,
				I18N.tf("confirmation.extract.add_media_to_source", String.join(", ", mediaToAddToSource), sourceId),
				I18N.t("dialog.extract.media.parts.title"),
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if(answer != JOptionPane.YES_OPTION){
				mediaToAddToSource.clear();
				tabbedPane.setSelectedIndex(0);
				mediaPartPanel.requestFocusInWindow();

				return false;
			}
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

		FLEFRecordHelper.removeChildren(record, ExtractReader.TAG_SOURCE);
		sourceField.saveReferences(record);
		FLEFRecordHelper.removeChildren(record, ExtractReader.TAG_MEDIA_PART);
		mediaPartPanel.saveReferences(record);
		remarkPanel.save(record);

		final FLEFRecord source = model.getRecordById(ExtractReader.extractSource(record));
		if(source != null)
			for(final String mediaId : mediaToAddToSource)
				source.addChild(FLEFRecord.createChildWithTagAndValue(SourceReader.TAG_MEDIA, mediaId));
	}


	/**
	 * The citations, anywhere in the model, that reference an extract.
	 *
	 * @param extractId	The id of the extract.
	 * @return	The citations.
	 */
	private List<FLEFRecord> citationsOf(final String extractId){
		final List<FLEFRecord> citations = new ArrayList<>(0);
		if(StringUtils.isEmpty(extractId))
			return citations;

		final Deque<FLEFRecord> stack = new ArrayDeque<>();
		for(final FLEFRecord root : model.getRecords())
			if(!ExtractReader.isOfType(root))
				stack.push(root);
		while(!stack.isEmpty()){
			final FLEFRecord node = stack.pop();
			for(final FLEFRecord child : node.getChildren()){
				if(SourceCitationReader.TAG_EXTRACT.equalsIgnoreCase(child.getTag()) && extractId.equals(child.getValue()))
					citations.add(node);
				stack.push(child);
			}
		}
		return citations;
	}


	static void main(final String[] args) throws IOException{
		GUIHelper.launch(ExtractRecordDialog::createEdit, "/tests/test.flef", "X1");
	}

}
