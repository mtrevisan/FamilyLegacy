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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs.structures;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.names.Name;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundFilteredComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PartHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PersonalNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.LocaleHelper;
import io.github.mtrevisan.familylegacy.v2.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Window;


/**
 * Dialog for editing a {@code PERSONAL_NAME_STRUCTURE} according to FLEF 0.1.3.
 * <p>
 * Structure:
 * <pre>
 * struct PersonalNameStructure {
 *   type?: enum {
 *     official, religious, birth,
 *     married, maiden, divorce, adoption, fostering,
 *     legal, immigrant, adapted,
 *     alias, nickname, artistic, professional, user,
 *     regnal, slave_name
 *   } | Text
 *   part+: PartStructure
 *   locale?: LocaleCode | Text
 *   cultural_norm*: Xref&lt;CulturalNormRecord&gt;
 *   source*: SourceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 * }
 * struct NamePartStructure {
 *   type: enum {
 *     given, generation,
 *     patronymic, matronymic, kunya (كُنيَة),
 *     family, family_nickname, lineage, house, clan, tribal, caste,
 *     toponymic,
 *     title, occupational, prefix, suffix,
 *     nickname, regnal, religious, posthumous
 *   } | Text
 *   value: Text
 *   variant*: TextValueVariant
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): type, part, locale
 * Tab 5 (Context): cultural_norm
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 */
public class PersonalNameStructureDialog extends BaseRecordDialog{

	private final JPanel propertiesPanel;

	private final BoundComboBox<String> typeCombo;
	private final EntityListPanel partPanel;
	private final BoundFilteredComboBox<String> localeCombo;
	private final EntityListPanel culturalNormPanel;


	public static PersonalNameStructureDialog createNew(final Window parent, final FLEFModel model){
		return createNew(parent, model, PersonalNameStructureDialog::new);
	}

	public static PersonalNameStructureDialog createEdit(final Window parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, PersonalNameStructureDialog::new);
	}


	private PersonalNameStructureDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, PersonalNameHandler.getInstance());

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]");

		typeCombo = new BoundComboBox<>(PersonalNameHandler.TAG_TYPE, GUIHelper.fillCombo(Name.PERSONAL_TYPES, null));
		typeCombo.setI18NPrefix("enum.personal.name.type");
		typeCombo.setEditable(true);
		partPanel = EntityListPanel.createForStructure(PersonalNameHandler.TAG_PART, this, I18N.t("dialog.name.parts") + "*", model, PartHandler.class);
		localeCombo = new BoundFilteredComboBox<>(PersonalNameHandler.TAG_LOCALE, LocaleHelper.getAvailableLanguageTags());
		localeCombo.setEditable(true);

		culturalNormPanel = EntityListPanel.createForEntityReference(PersonalNameHandler.TAG_CULTURAL_NORM, parent, I18N.t("dialog.name.cultural.norms"),
			model, CulturalNormHandler.class);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.SOURCE, PersonalNameHandler.TAG_SOURCE, I18N.t("dialog.component.sources.with.citations"))
			.withComponent(PanelKey.NOTE, PersonalNameHandler.TAG_NOTE, null)
			.build();

		components.bind(typeCombo);
		components.bind(localeCombo);


		// Set up the image carousel selection listener on the source list
		setupSourceListSelection();

		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// type
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.name.type") + ":", typeCombo);

		// parts
		GUIHelper.addComponent(propertiesPanel, partPanel);

		// locale
		GUIHelper.addLabeledComponent(propertiesPanel, I18N.t("dialog.name.locale") + ":", localeCombo);

		return propertiesPanel;
	}

	@Override
	protected JPanel createContextPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		// cultural norm
		GUIHelper.addComponent(panel, culturalNormPanel);

		return panel;
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

		partPanel.load(record);

		culturalNormPanel.load(record);


		// Initially, update carousel based on the first selected source (if any)
		updateCarouselFromSelectedSource();
	}

	@Override
	protected boolean validData(){
		if(partPanel.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				I18N.tf("validation.at.least.one", I18N.t("dialog.name.parts")),
				tabbedPane, propertiesPanel, partPanel);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

		partPanel.save(record);

		culturalNormPanel.save(record);
	}

	public boolean hasData(){
		return !partPanel.isEmpty();
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final PersonalNameStructureDialog dialog = new PersonalNameStructureDialog(null, model, null);
			dialog.setVisible(true);
		});
	}


}
