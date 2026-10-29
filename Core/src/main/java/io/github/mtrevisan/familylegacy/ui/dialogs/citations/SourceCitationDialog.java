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
package io.github.mtrevisan.familylegacy.ui.dialogs.citations;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.ui.components.lists.ExtractListPanel;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.PanelKey;
import io.github.mtrevisan.familylegacy.ui.dialogs.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Window;


/**
 * Dialog for editing a {@code SOURCE_CITATION} according to FLEF 0.2.0.
 * <p>
 * Structure:
 * <pre>
 * struct SourceCitation {
 *   source: Xref&lt;SourceRecord&gt;
 *   locator?: Text
 *   extract*: ExtractStructure
 *   stance: enum { supports, contradicts }
 *   note*: Xref&lt;NoteRecord&gt;
 *   evidence?: EvidenceQualifiers
 *   privacy?: PrivacyStructure
 *
 *   require extract.media_part.media in source.media
 * }
 *
 * struct ExtractStructure {
 *   media_part*: struct {
 *     media: Xref&lt;MediaRecord&gt;
 *     crop?: CropRect
 *   }
 *   text?: Text
 *   type?: enum { verbatim, summarized, translated, normalized }
 *   locale?: LocaleCode
 *   remark*: Text
 *
 *   require one_of(media_part, text)
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): locator, extract, evidence
 * Tab 8 (Notes): remark
 * Tab 9 (Privacy): privacy
 */
public class SourceCitationDialog extends BaseRecordDialog implements CitationHost{

	private final JPanel propertiesPanel;

	private final EntityField sourceField;
	private final BoundTextField locatorField;
	private final ExtractListPanel extractPanel;
	private final BoundComboBox<String> stanceCombo;


	public static SourceCitationDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, SourceCitationDialog::new);
	}

	public static SourceCitationDialog createEdit(final Window parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, SourceCitationDialog::new);
	}


	private SourceCitationDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, SourceCitationHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]10[]");

		sourceField = EntityField.createForRecordFromReference(SourceCitationReader.TAG_SOURCE, this, model, SourceHandler.class);
		locatorField = new BoundTextField(SourceCitationReader.TAG_LOCATOR);
		extractPanel = new ExtractListPanel(SourceCitationReader.TAG_EXTRACT, this, I18N.t("dialog.source_citation.extract.title"), model);

		stanceCombo = new BoundComboBox<>(SourceCitationReader.TAG_STANCE, SourceCitationReader.STANCES);
		stanceCombo.setI18NPrefix("enum.source_citation.stance");

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, this.record)
			.withComponent(PanelKey.EVIDENCE, SourceCitationReader.TAG_EVIDENCE, I18N.t("dialog.component.evidence"))
			.withComponent(PanelKey.PRIVACY, SourceCitationReader.TAG_PRIVACY, null)
			.build();

		components.bind(locatorField);
		components.bind(stanceCombo);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// source
		GUIHelper.addLabeledComponent(propertiesPanel, GUIHelper.requiredLabel("dialog.source_citation.source"), sourceField);

		// locator
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.l("dialog.source_citation.locator"), locatorField);

		// extract
		GUIHelper.addComponent(propertiesPanel, extractPanel);

		// stance
		GUIHelper.addLabeledComponent(propertiesPanel, GUIHelper.requiredLabel("dialog.source_citation.stance"), stanceCombo);

		// evidence
		final JPanel evidencePanel = components.getPanel(PanelKey.EVIDENCE);
		GUIHelper.addComponent(propertiesPanel, evidencePanel);

		return propertiesPanel;
	}

	@Override
	protected JPanel createPrivacyPanel(){
		return components.getPanel(PanelKey.PRIVACY);
	}


	public void setSource(final String sourceId){
		if(StringUtils.isNotEmpty(sourceId)){
			if(!confirmRecordExistsForType(sourceId, SourceHandler.getInstance().getLabel()))
				return;

			sourceField.setText(sourceId);
		}
	}


	@Override
	public String locator(){
		return locatorField.getText();
	}

	@Override
	public void applyLocator(final String locator){
		locatorField.setText(locator);
	}

	@Override
	public FLEFRecord citedSource(){
		return sourceField.getEntity();
	}


	@Override
	protected void loadData(){
		sourceField.load(record);

		components.load(record);

		extractPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(sourceField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.source_citation.source")),
				tabbedPane, propertiesPanel, sourceField);

			return false;
		}
		if(!stanceCombo.isValued()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.source_citation.stance")),
				tabbedPane, propertiesPanel, sourceField);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		sourceField.saveReferences(record);

		components.save(record);

		extractPanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final FLEFRecord source = FLEFRecord.createMainRecord("S1", SourceHandler.TYPE);
			model.addRecord(source);

			final SourceCitationDialog dialog = SourceCitationDialog.createNew(null, model);
			dialog.setSource("S1");
			dialog.setVisible(true);
		});
	}

}
