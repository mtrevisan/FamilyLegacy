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
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PartHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PersonalNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Dialog for editing a {@code PERSONAL_NAME_STRUCTURE} according to FLEF 0.1.1.
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
 * struct PartStructure {
 *   type: enum {
 *     given, generation,
 *     patronymic, matronymic, kunya,
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
 * Tab 1 (Properties): type, part
 * Tab 5 (Context): context
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 */
public class PersonalNameStructureDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 6814016756734554747L;


	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_PART = "PART";
	private static final String TAG_LOCALE = "LOCALE";
	private static final String TAG_CONTEXT_IMPACT = "CONTEXT_IMPACT";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_NOTE = "NOTE";


	private final RecordDialogComponents components;

	private final JPanel propertiesPanel;

	private final BoundComboBox<String> typeCombo;
	private final EntityReferenceListPanel partPanel;
	private final BoundComboBox<String> localeCombo;


	public static PersonalNameStructureDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, PersonalNameStructureDialog::new);
	}

	public static PersonalNameStructureDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, PersonalNameStructureDialog::new);
	}


	private PersonalNameStructureDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, PersonalNameHandler.class);

		propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]");

		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			// marital status and origins at birth
			"official", "religious", "birth",
			// changes in marital status and family events
			"married", "maiden", "divorce", "adoption", "fostering",
			// legal, immigration, and naturalization changes
			"legal", "immigrant", "adapted",
			// informal, stage, and social names
			"alias", "nickname", "artistic", "professional", "user",
			// historical and dynastic contexts
			"regnal", "slave_name"
		});
		typeCombo.setEditable(true);
		partPanel = EntityReferenceListPanel.createForStructure(TAG_PART, this, "Parts*", model)
			.withHandlerTypes(PartHandler.class);
		localeCombo = new BoundComboBox<>(TAG_LOCALE, new String[]{
			StringUtils.EMPTY,
			"en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"
		});
		localeCombo.setEditable(true);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withComponent(PanelKey.CONTEXT_IMPACT, TAG_CONTEXT_IMPACT, "Context Impacts", ContextImpactHandler.class, PersonalNameHandler.class)
			.withComponent(PanelKey.SOURCE, TAG_SOURCE, "Sources with Citations", SourceHandler.class, SourceCitationHandler.class)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, NoteHandler.class)
			.build();

		components.bind(typeCombo);
		components.bind(localeCombo);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		// type
		GUIHelper.addLabeledComponent(propertiesPanel, "Type:", typeCombo);

		// parts
		GUIHelper.addComponent(propertiesPanel, partPanel);

		// locale
		GUIHelper.addLabeledComponent(propertiesPanel, "Locale:", localeCombo);

		return propertiesPanel;
	}

	@Override
	protected JPanel createContextPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel contextPanel = components.getPanel(PanelKey.CONTEXT_IMPACT);
		GUIHelper.addComponent(panel, contextPanel);

		return panel;
	}

	@Override
	protected JPanel createSourcesPanel(){
		final JPanel panel = GUIHelper.createLabelFieldPanel(10, "[]");

		final JPanel sourcePanel = components.getPanel(PanelKey.SOURCE);
		GUIHelper.addComponent(panel, sourcePanel);

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
	}

	@Override
	protected boolean validData(){
		if(partPanel.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"At least one part is required.",
				tabbedPane, propertiesPanel, partPanel);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

		partPanel.save(record);
	}

	public boolean hasData(){
		return !partPanel.isEmpty();
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		HandlerRegistry.scanHandlers();

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final PersonalNameStructureDialog dialog = new PersonalNameStructureDialog(null, model, null);
			dialog.setVisible(true);
		});
	}


}
