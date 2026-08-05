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
 * Dialog for editing a {@code NAME_STRUCTURE} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
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
public class NameStructureDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 7526263144620538539L;


	public static final String TAG_VALUE = "VALUE";
	private static final String TAG_VARIANT = "VARIANT";
	private static final String TAG_LOCALE = "LOCALE";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SOURCE = "SOURCE";


	static{
		HandlerRegistry.register(new ClassifiedNameHandler());
		HandlerRegistry.register(new VariantHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField valueField;
	private final VariantListPanel variantPanel;
	private final BoundComboBox<String> localeCombo;
	private final DateField dateField;
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourcePanel;


	public static NameStructureDialog createNew(final Dialog parent, final FLEFModel model){
		return new NameStructureDialog(parent, model, null);
	}

	public static NameStructureDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new NameStructureDialog(parent, model, record);
	}


	private NameStructureDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(ClassifiedNameHandler.TYPE));

		valueField = new BoundTextField(TAG_VALUE, 30);
		variantPanel = new VariantListPanel(TAG_VARIANT, this, model);
		localeCombo = new BoundComboBox<>(TAG_LOCALE, new String[]{
			StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"
		});
		dateField = DateField.createWithWrapperTag(TAG_DATE, this, "Valid On", model);
		notePanel = new NoteListPanel(TAG_NOTE, this, model);
		sourcePanel = new SourceCitationListPanel(TAG_SOURCE, this, model);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(valueField);
		bindingManager.bind(localeCombo);

		setLayout(new MigLayout("ins 10,fillx,top", "[grow]", "[]10[]"));

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

		// locale
		localeCombo.setEditable(true);
		panel.add(new JLabel("Locale:"), "align label");
		panel.add(localeCombo, "growx, wrap");

		// date
		panel.add(new JLabel("Date:"), "align label,top");
		panel.add(dateField, "growx,wrap");

		return panel;
	}

	private JPanel createVariantPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]5[]"));
		panel.add(variantPanel, "growx");
		return panel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]5[]"));
		panel.add(notePanel, "growx");
		panel.add(sourcePanel, "growx");
		return panel;
	}

	@Override
	protected void loadData(){
		bindingManager.load(record);

		dateField.load(record);
		variantPanel.load(record);
		notePanel.load(record);
		sourcePanel.load(record);
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
		sourcePanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final NameStructureDialog dialog = new NameStructureDialog(null, model, null);
			dialog.setVisible(true);
		});
	}

}
