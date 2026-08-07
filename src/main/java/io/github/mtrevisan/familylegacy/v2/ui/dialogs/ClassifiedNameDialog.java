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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.VariantListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ClassifiedNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.VariantHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
/**
 * Dialog for editing a {@code CLASSIFIED_NAME} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * struct ClassifiedName {
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
 *   note*: Xref&lt;NoteRecord&gt;
 *   source*: SourceCitation
 * }
 * </pre>
 */
public class ClassifiedNameDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 4890876589041527256L;


	private static final String DOT = ".";

	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_TEXT = "TEXT";
	public static final String TAG_VALUE = TAG_TEXT + DOT + "VALUE";
	private static final String TAG_VARIANT = TAG_TEXT + DOT + "VARIANT";
	private static final String TAG_LOCALE = TAG_TEXT + DOT + "LOCALE";
	private static final String TAG_DATE = TAG_TEXT + DOT + "DATE";
	private static final String TAG_NOTE = TAG_TEXT + DOT + "NOTE";
	private static final String TAG_SOURCE = TAG_TEXT + DOT + "SOURCE";


	static{
		HandlerRegistry.register(new ClassifiedNameHandler());
		HandlerRegistry.register(new VariantHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField valueField;
	private final BoundComboBox<String> typeCombo;
	private final VariantListPanel variantPanel;
	private final BoundComboBox<String> localeCombo;
	private final DateField dateField;
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourceCitationPanel;


	public static ClassifiedNameDialog createNew(final Dialog parent, final FLEFModel model){
		return new ClassifiedNameDialog(parent, model, null);
	}

	public static ClassifiedNameDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new ClassifiedNameDialog(parent, model, record);
	}


	private ClassifiedNameDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(ClassifiedNameHandler.TYPE));

		valueField = new BoundTextField(TAG_VALUE, 30);
		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{StringUtils.EMPTY,
			// official and legal names
			"official",
			"legal",
			// historical naming traditions
			"colonial",
			"indigenous",
			"traditional",
			// language and localization variants
			"translated",
			"romanized",
			// historical variants
			"historic",
			"former",
			// common usage
			"common",
			"colloquial",
			// abbreviated forms
			"abbreviated",
			"acronym",
			// religious and ecclesiastical forms
			"religious",
			// administrative and archival forms
			"administrative",
			"archival"
		});
		typeCombo.setEditable(true);
		variantPanel = new VariantListPanel(TAG_VARIANT, this, model);
		localeCombo = new BoundComboBox<>(TAG_LOCALE, new String[]{
			StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"
		});
		dateField = DateField.createWithWrapperTag(TAG_DATE, this, "Valid On", model);
		notePanel = new NoteListPanel(TAG_NOTE, this, model);
		sourceCitationPanel = new SourceCitationListPanel(TAG_SOURCE, this, model);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(valueField);
		bindingManager.bind(typeCombo);
		bindingManager.bind(localeCombo);

		setLayout(new MigLayout("ins 10,fillx,top"));

		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("Variants", createVariantPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]5[]10[]"));

		// value
		panel.add(new JLabel("Name Value*:"), "align label");
		panel.add(valueField, "growx, wrap");

		// type
		panel.add(new JLabel("Type:"), "align label");
		panel.add(typeCombo, "growx, wrap");

		// locale
		localeCombo.setEditable(true);
		panel.add(new JLabel("Locale:"), "align label");
		panel.add(localeCombo, "growx, wrap");

		// date
		panel.add(new JLabel("Date:"), "align label");
		panel.add(dateField, "growx,wrap");

		return panel;
	}

	private JPanel createVariantPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]"));
		panel.add(variantPanel, "growx");
		return panel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]"));
		panel.add(notePanel, "growx");
		panel.add(sourceCitationPanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		dateField.load(record);
		variantPanel.load(record);
		notePanel.load(record);
		sourceCitationPanel.load(record);
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
		bindingManager.save(record);

		dateField.save(record);
		variantPanel.save(record);
		notePanel.saveReferences(record);
		sourceCitationPanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final ClassifiedNameDialog dialog = new ClassifiedNameDialog(null, model, null);
			dialog.setVisible(true);
		});
	}

}
