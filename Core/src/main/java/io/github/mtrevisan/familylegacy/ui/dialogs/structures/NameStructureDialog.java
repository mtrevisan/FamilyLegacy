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
package io.github.mtrevisan.familylegacy.ui.dialogs.structures;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundFilteredComboBox;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundTextField;
import io.github.mtrevisan.familylegacy.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.ui.components.lists.TextValueVariantListPanel;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.NameHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.LocaleHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Window;


/**
 * Dialog for editing a {@code NAME_STRUCTURE} according to FLEF 0.2.0.
 * <p>
 * Structure:
 * <pre>
 * struct NameStructure {
 *   type?: enum {
 *     // official and legal names
 *     official,         // officially recognized name used by a government, institution,
 *     legal,            // name defined by law, regulation, charter, or legal act
 *     // historical naming traditions
 *     colonial,         // name introduced or imposed during a colonial period
 *     indigenous,       // name originating from the local or native population
 *     traditional,      // name established through long-standing customary use
 *     // language and localization variants
 *     translated,       // name translated into another language
 *     transcribed,      // name transliterated or transcribed into another writing system
 *     // historical variants
 *     historic,         // historical name used during a specific period
 *     // common usage
 *     common,           // commonly used name regardless of official status
 *     colloquial,       // informal or popular name used in everyday speech
 *     // abbreviated forms
 *     abbreviated,      // shortened form of a name
 *     acronym,          // name formed from initials or abbreviations
 *     // religious and ecclesiastical forms
 *     religious,        // name used in a religious, ecclesiastical, or liturgical context
 *     // administrative and archival forms
 *     administrative,   // name used by an administrative authority
 *     archival          // name appearing in archival, documentary, or cataloging contexts
 *   } | Text
 *   value: Text
 *   locale?: LocaleCode | Text
 *   variant*: TextValueVariant
 *   valid_from?: DateStructure    // The date from which this name is known or believed to have been valid.
 *   valid_to?: DateStructure      // The date until which this name is known or believed to have been valid.
 *   source*: SourceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): value, type, variant, valid_from, valid_to, locale
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 */
public class NameStructureDialog extends BaseRecordDialog{

	private final JPanel propertiesPanel;

	private final BoundTextField valueField;
	private final BoundComboBox<String> typeCombo;
	private final TextValueVariantListPanel variantPanel;
	private final DateField validFromField;
	private final DateField validToField;
	private final BoundFilteredComboBox<String> localeCombo;


	public static NameStructureDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, NameStructureDialog::new);
	}

	public static NameStructureDialog createEdit(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, NameStructureDialog::new);
	}


	private NameStructureDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, NameHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]15[]10[]");

		valueField = new BoundTextField(NameReader.TAG_VALUE);
		typeCombo = new BoundComboBox<>(NameReader.TAG_TYPE, GUIHelper.fillCombo(NameReader.TYPES, null));
		typeCombo.setI18NPrefix("enum.name.type");
		typeCombo.setEditable(true);
		variantPanel = new TextValueVariantListPanel(NameReader.TAG_PART_VARIANT, this, I18N.t("dialog.name.variant"), model);
		validFromField = DateField.createWithWrapperTag(NameReader.TAG_VALID_FROM, this, I18N.t("dialog.date.valid_from"), model);
		validToField = DateField.createWithWrapperTag(NameReader.TAG_VALID_TO, this, I18N.t("dialog.date.valid_to"), model);
		localeCombo = new BoundFilteredComboBox<>(NameReader.TAG_LOCALE, LocaleHelper.getAvailableLanguageTags());
		localeCombo.setEditable(true);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, this.record)
			.withComponent(PanelKey.SOURCE, NameReader.TAG_SOURCE, I18N.t("dialog.component.sources_with_citations"))
			.withComponent(PanelKey.NOTE, NameReader.TAG_NOTE, null)
			.build();

		components.bind(valueField);
		components.bind(typeCombo);
		components.bind(localeCombo);


		// Set up the image carousel selection listener on the source list
		setupSourceListSelection();

		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// value
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.name.name") + "*:", valueField);

		// type
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.name.type") + "*:", typeCombo);

		// locale
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.name.locale") + ":", localeCombo);

		// variant
		GUIHelper.addComponent(propertiesPanel, variantPanel);

		// validity range:
		final JPanel validityPanel = GUIHelper.createLabelFieldPanel(5, "[]10[]");
		validityPanel.setBorder(BorderFactory.createTitledBorder(I18N.t("dialog.validity.range")));
		// valid from
		GUIHelper.addLabeledComponent(validityPanel, I18N.t("dialog.valid_from") + ":", validFromField);
		// valid to
		GUIHelper.addLabeledComponent(validityPanel, I18N.t("dialog.valid_to") + ":", validToField);
		GUIHelper.addComponent(propertiesPanel, validityPanel);

		return propertiesPanel;
	}

	@Override
	protected JPanel createSourcesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel sourcePanel = components.getPanel(PanelKey.SOURCE);
		GUIHelper.addComponent(panel, sourcePanel);

		// Image carousel below the source list
		// It will be hidden if no images are found
		GUIHelper.addComponent(panel, imageCarouselPanel);

		return panel;
	}

	@Override
	protected JPanel createNotesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel notePanel = components.getPanel(PanelKey.NOTE);
		GUIHelper.addComponent(panel, notePanel);

		return panel;
	}


	@Override
	protected void loadData(){
		components.load(record);

		variantPanel.load(record);
		validFromField.load(record);
		validToField.load(record);


		// Initially, update carousel based on the first selected source (if any)
		updateCarouselFromSelectedSource();
	}

	@Override
	protected boolean validData(){
		if(valueField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.required", I18N.t("dialog.name.name")),
				tabbedPane, propertiesPanel, valueField);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

		variantPanel.save(record);
		validFromField.save(record);
		validToField.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(NameStructureDialog::createNew);
	}

}
