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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.PanelKey;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogBuilder;
import io.github.mtrevisan.familylegacy.v2.ui.components.RecordDialogComponents;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.VariantListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ClassifiedNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Dialog for editing a {@code CLASSIFIED_NAME_STRUCTURE} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * struct ClassifiedNameStructure {
 *   type?: enum {
 *     official, legal,
 *     colonial, indigenous, traditional,
 *     translated, romanized,
 *     historic, former,
 *     common, colloquial,
 *     abbreviated, acronym,
 *     religious,
 *     administrative, archival
 *   } | Text
 *   text: NameStructure
 * }
 * struct NameStructure {
 *   value: Text
 *   variant*: TextValueVariant
 *   locale?: LocaleCode
 *   date?: DateStructure
 *   source*: SourceCitation
 *   note*: Xref&lt;NoteRecord&gt;
 * }
 * </pre>
 * <p>
 * Tabs:
 * Tab 1 (Properties): value, type, localw, variant
 * Tab 7 (Sources): source
 * Tab 8 (Notes): note
 */
public class ClassifiedNameStructureDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 4890876589041527256L;


	private static final String DOT = ".";

	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_TEXT = "TEXT";
	public static final String TAG_VALUE = TAG_TEXT + DOT + "VALUE";
	private static final String TAG_VARIANT = TAG_TEXT + DOT + "VARIANT";
	private static final String TAG_LOCALE = TAG_TEXT + DOT + "LOCALE";
	private static final String TAG_NOTE = TAG_TEXT + DOT + "NOTE";
	private static final String TAG_SOURCE = TAG_TEXT + DOT + "SOURCE";


	private final RecordDialogComponents components;

	private final BoundTextField valueField;
	private final BoundComboBox<String> typeCombo;
	private final VariantListPanel variantPanel;
	private final BoundComboBox<String> localeCombo;


	public static ClassifiedNameStructureDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, ClassifiedNameStructureDialog::new);
	}

	public static ClassifiedNameStructureDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, ClassifiedNameStructureDialog::new);
	}


	private ClassifiedNameStructureDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, ClassifiedNameHandler.class);

		valueField = new BoundTextField(TAG_VALUE);
		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY,
			// official and legal names
			"official", "legal",
			// historical naming traditions
			"colonial", "indigenous", "traditional",
			// language and localization variants
			"translated", "romanized",
			// historical variants
			"historic", "former",
			// common usage
			"common", "colloquial",
			// abbreviated forms
			"abbreviated", "acronym",
			// religious and ecclesiastical forms
			"religious",
			// administrative and archival forms
			"administrative", "archival"
		});
		typeCombo.setEditable(true);
		variantPanel = new VariantListPanel(TAG_VARIANT, this, "Variant", model);
		localeCombo = new BoundComboBox<>(TAG_LOCALE, new String[]{
			StringUtils.EMPTY,
			"en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"
		});
		localeCombo.setEditable(true);

		// Build common panels using the builder
		components = new RecordDialogBuilder(this, model, record)
			.withCitationComponent(PanelKey.SOURCE, TAG_SOURCE, TAG_SOURCE, "Sources", SourceHandler.class, SourceHandler.class)
			.withComponent(PanelKey.NOTE, TAG_NOTE, "Notes", NoteHandler.class, NoteHandler.class)
			.build();

		components.bind(valueField);
		components.bind(typeCombo);
		components.bind(localeCombo);


		finalizeDialog(parent);
	}


	@Override
	protected JPanel createPropertiesPanel(){
		final JPanel propertiesPanel = GUIHelper.createLabelFieldPanel(10, "[]10[]10[]15[]");

		// value
		GUIHelper.addLabeledComponent(propertiesPanel, "Name Value*:", valueField);

		// type
		GUIHelper.addLabeledComponent(propertiesPanel, "Type*:", typeCombo);

		// locale
		GUIHelper.addLabeledComponent(propertiesPanel, "Locale:", localeCombo);

		// variant
		GUIHelper.addComponent(propertiesPanel, variantPanel);

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
	}

	@Override
	protected boolean validData(){
		if(valueField.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Name value cannot be empty.", "Validation Error",
				JOptionPane.ERROR_MESSAGE);
			valueField.requestFocus();

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		components.save(record);

		variantPanel.save(record);
	}


	public static void main(final String[] args){
		GUIHelper.launch(ClassifiedNameStructureDialog::createNew);
	}

}
