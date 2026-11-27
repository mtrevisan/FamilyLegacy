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

import io.github.mtrevisan.familylegacy.i18n.I18N;
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
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Window;
import java.util.List;
import java.util.StringJoiner;


/**
 * Dialog for editing a {@code SOURCE_CITATION} according to FLEF 0.2.0.
 * <p>
 * Structure:
 * <pre>
 * struct SourceCitation {
 *   source: Xref&lt;SourceRecord&gt;
 *   locator?: Text
 *   extract*: Xref&lt;ExtractRecord&gt;
 *   stance: enum { supports, contradicts }
 *   evidence?: EvidenceQualifiers
 *   privacy?: PrivacyStructure
 *
 *   require every e in extract: e.source == source
 * }
 * </pre>
 * <p>
 * The extracts are records of the source cited (see {@code ExtractRecordDialog}): the citation can be saved only if every extract it
 * references belongs to the source it cites. The extracts created from this dialog are discarded if the citation is not saved.
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
		extractPanel = new ExtractListPanel(SourceCitationReader.TAG_EXTRACT, this, I18N.t("dialog.source_citation.extract.title"), model,
			this::citedSourceId);

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

			sourceField.setEntity(model.getRecordById(sourceId));
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

	private String citedSourceId(){
		final FLEFRecord source = sourceField.getEntity();
		return (source != null? source.getId(): null);
	}


	@Override
	protected void loadData(){
		sourceField.load(record);

		components.load(record);
		//a citation with no stance supports what it is cited for (almost always the case)
		if(!stanceCombo.isValued())
			stanceCombo.setSelectedItem(SourceCitationReader.STANCE_SUPPORTS);

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
				tabbedPane, propertiesPanel, stanceCombo);

			return false;
		}
		//every extract the citation references belongs to the source it cites
		final List<FLEFRecord> extractsOfOtherSources = extractPanel.extractsNotOf(citedSourceId());
		if(!extractsOfOtherSources.isEmpty()){
			final StringJoiner ids = new StringJoiner(", ");
			for(final FLEFRecord extract : extractsOfOtherSources)
				ids.add(extract.getId());
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.extract.other_source", ids.toString(), citedSourceId()),
				tabbedPane, propertiesPanel, extractPanel);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		sourceField.saveReferences(record);

		components.save(record);

		extractPanel.save(record);
		extractPanel.keepCreated();
	}

	/** Closes the dialog; the extracts created from it are discarded if the citation was not saved (nothing references them). */
	@Override
	public void dispose(){
		if(!isSaved())
			extractPanel.discardCreated();

		super.dispose();
	}


	static void main(final String[] args){
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
