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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.TranslationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;


/* DONE */
/**
 * Dialog for editing a {@code NOTE_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record NoteRecord {
 *   id: LocalID
 *   title?: Text
 *   value: Text
 *   mime?: Text
 *   locale?: LocaleCode | Text
 *   translation*: struct {
 *     value: Text
 *     locale?: LocaleCode | Text
 *   }
 *   source*: SourceCitation
 *   privacy?: PrivacyStructure
 *   audit: AuditStructure
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): title, value, mime, locale, translation
 * Tab 7 (Sources): source
 * Tab 9 (Privacy): privacy
 * Tab 10 (Audit): audit
 */
public class NoteRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212975L;


	private static final String TAG_TITLE = "TITLE";
	private static final String TAG_VALUE = "VALUE";
	private static final String TAG_MIME = "MIME";
	private static final String TAG_LOCALE = "LOCALE";
	private static final String TAG_TRANSLATION = "TRANSLATION";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_PRIVACY = "PRIVACY";
	private static final String TAG_AUDIT = "AUDIT";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final BoundTextField titleField;
	private final BoundTextArea valueArea;
	private final BoundComboBox<String> mimeCombo;
	private final BoundComboBox<String> localeCombo;
	private final TranslationListPanel translationPanel;


	public static NoteRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, NoteRecordDialog::new);
	}

	public static NoteRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, NoteRecordDialog::new);
	}


	private NoteRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, NoteHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]5[]5[]10[]");

		titleField = new BoundTextField(TAG_TITLE);
		valueArea = new BoundTextArea(TAG_VALUE, 3, 25);
		valueArea.setToolTipText("Markdown supported. Use [text](@<XREF:ID>@) for references, [text](confidential) for confidential data.");
		mimeCombo = new BoundComboBox<>(TAG_MIME, new String[]{
			StringUtils.EMPTY,
			"text/plain", "text/html", "text/markdown"});
		localeCombo = new BoundComboBox<>(TAG_LOCALE, new String[]{
			StringUtils.EMPTY,
			"en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});
		localeCombo.setEditable(true);
		translationPanel = new TranslationListPanel(TAG_TRANSLATION, this, "Translations");

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources with Citations", SourceHandler.class, SourceCitationHandler.class)
			.withComponent(PanelKey.PRIVACY, TAG_PRIVACY, null, null, null)
			.withComponent(PanelKey.AUDIT, TAG_AUDIT, null, null, null)
			.build();

		components.bind(titleField);
		components.bind(valueArea);
		components.bind(mimeCombo);
		components.bind(localeCombo);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// title
		GUIHelper.addLabeledComponent(propertiesPanel, "Title:", titleField);

		// value
		GUIHelper.addLabeledComponent(propertiesPanel, "Value*:", valueArea);

		// mime
		GUIHelper.addLabeledComponent(propertiesPanel, "MIME type:", mimeCombo);

		// locale
		GUIHelper.addLabeledComponent(propertiesPanel, "Locale:", localeCombo);

		// translation
		GUIHelper.addComponent(propertiesPanel, translationPanel);

		return propertiesPanel;
	}

	@Override
	protected JPanel createSourcesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel sourcePanel = components.getPanel(PanelKey.SOURCE);
		GUIHelper.addComponent(panel, sourcePanel);

		return panel;
	}

	@Override
	protected JPanel createPrivacyPanel(){
		return components.getPanel(PanelKey.PRIVACY);
	}

	@Override
	protected JPanel createAuditPanel(){
		return components.getPanel(PanelKey.AUDIT);
	}


	@Override
	protected void loadData(){
		components.load(record);

		translationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(valueArea.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Note value is required.",
				tabbedPane, propertiesPanel, valueArea);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

		translationPanel.save(record);
	}


	public static void main(final String[] args) throws IOException{
		try(final InputStream is = NoteRecordDialog.class.getResourceAsStream("/tests/test.flef")){
			final String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			GUIHelper.launch(NoteRecordDialog::createEdit, content, "N1");
		}
	}

}
