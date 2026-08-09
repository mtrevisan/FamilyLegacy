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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.TranslationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


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
 *   locale?: LocaleCode
 *   translation*: struct {
 *     value: Text
 *     locale?: LocaleCode
 *   }
 *   source*: SourceCitation
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 * </pre>
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
	private static final String TAG_RESTRICTION = "RESTRICTION";


	static{
		HandlerRegistry.register(new NoteHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]5[]5[]"));

	private final BoundTextField titleField;
	private final BoundTextArea valueArea;
	private final BoundComboBox<String> mimeCombo;
	private final BoundComboBox<String> localeCombo;
	private final TranslationListPanel translationPanel;
	private final SourceCitationListPanel sourceCitationPanel;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;


	/**
	 * Creates a new dialog to create a new record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @return	A new dialog instance.
	 */
	public static NoteRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return new NoteRecordDialog(parent, model, null);
	}

	/**
	 * Creates a new dialog to edit an existing record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @param record	The record to edit (must not be {@code null}).
	 * @return	A new dialog instance.
	 */
	public static NoteRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new NoteRecordDialog(parent, model, record);
	}


	private NoteRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(NoteHandler.TYPE));

		titleField = new BoundTextField(TAG_TITLE, 30);
		valueArea = new BoundTextArea(TAG_VALUE, 3, 25);
		valueArea.setToolTipText("Markdown supported. Use [text](@<XREF:ID>@) for references, [text](confidential) for confidential data.");
		mimeCombo = new BoundComboBox<>(TAG_MIME, new String[]{StringUtils.EMPTY, "text/plain", "text/html", "text/markdown"});
		localeCombo = new BoundComboBox<>(TAG_LOCALE, new String[]{StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});
		translationPanel = new TranslationListPanel(TAG_TRANSLATION, this, model);
		sourceCitationPanel = new SourceCitationListPanel(TAG_SOURCE, this, model);
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(titleField);
		bindingManager.bind(valueArea);
		bindingManager.bind(mimeCombo);
		bindingManager.bind(localeCombo);


		setLayout(new MigLayout("ins 10,fillx,top"));

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Modification", modificationPanel);
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		// title
		mainPanel.add(new JLabel("Title:"), "align label");
		mainPanel.add(titleField, "growx,wrap");

		// value
		mainPanel.add(new JLabel("Value*:"), "align label,top");
		mainPanel.add(GUIHelper.createScrollPane(valueArea), "growx, growy, wrap");

		// mime
		mainPanel.add(new JLabel("MIME:"), "align label");
		mainPanel.add(mimeCombo, "growx,wrap");

		// locale
		mainPanel.add(new JLabel("Locale:"), "align label");
		mainPanel.add(localeCombo, "growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]"));
		panel.add(translationPanel, "growx");
		panel.add(sourceCitationPanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		translationPanel.load(record);
		sourceCitationPanel.load(record);
		restrictionPanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(valueArea.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Note value is required.",
				tabbedPane, mainPanel, valueArea);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		bindingManager.save(record);

		translationPanel.save(record);
		sourceCitationPanel.save(record);
		restrictionPanel.save(record);
		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final NoteRecordDialog dialog = NoteRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
