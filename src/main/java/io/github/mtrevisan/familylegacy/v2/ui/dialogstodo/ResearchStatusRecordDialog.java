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
package io.github.mtrevisan.familylegacy.v2.ui.dialogstodo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/* ONGOING */
/**
 * Dialog for editing a {@code RESEARCH_STATUS_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record ResearchStatusRecord {
 *   id: LocalID
 *   question: Text
 *   status?: enum {
 *     active,
 *     completed,
 *     blocked
 *   }
 *   priority?: enum {
 *     high, medium, low
 *   }
 *   association*: struct {
 *     target: XrefOrVoid&lt;LocalID&gt;
 *     name?: Text
 *   }
 *   blocked_by*: Xref&lt;ResearchStatusRecord&gt;
 *   plan?: Text
 *   resolution?: Text
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class ResearchStatusRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -3379415124738380266L;


	private static final String TAG_QUESTION = "QUESTION";
	private static final String TAG_STATUS = "STATUS";
	private static final String TAG_PRIORITY = "PRIORITY";
	private static final String TAG_ASSOCIATION = "ASSOCIATION";
	private static final String TAG_BLOCKED_BY = "BLOCKED_BY";
	private static final String TAG_PLAN = "PLAN";
	private static final String TAG_RESOLUTION = "RESOLUTION";


	static{
		HandlerRegistry.register(new NoteHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]5[]5[]"));

	private final BoundTextField questionField;
	//TODO status
//	private final BoundTextArea valueArea;
	//TODO priority
//	private final BoundComboBox<String> mimeCombo;
	//TODO association
//	private final BoundComboBox<String> localeCombo;
	//TODO blocked_by
//	private final TranslationListPanel translationPanel;
	//TODO plan
//	private final BoundTextField planField;
	//TODO resolution
//	private final BoundTextField resolutionField;
	private final ModificationPanel modificationPanel;


	public static ResearchStatusRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return new ResearchStatusRecordDialog(parent, model, null);
	}

	public static ResearchStatusRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new ResearchStatusRecordDialog(parent, model, record);
	}


	private ResearchStatusRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(NoteHandler.TYPE));

		questionField = new BoundTextField(TAG_QUESTION, 30);
//		valueArea = new BoundTextArea(TAG_VALUE, 3, 25);
//		valueArea.setToolTipText("Markdown supported. Use [text](@<XREF:ID>@) for references, [text](confidential) for confidential data.");
//		mimeCombo = new BoundComboBox<>(TAG_MIME, new String[]{StringUtils.EMPTY, "text/plain", "text/html", "text/markdown"});
//		localeCombo = new BoundComboBox<>(TAG_LOCALE, new String[]{StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});
//		translationPanel = new TranslationListPanel(TAG_TRANSLATION, this, model);
//		sourceCitationPanel = new SourceCitationListPanel(TAG_SOURCE, this, model);
//		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(questionField);
//		bindingManager.bind(valueArea);
//		bindingManager.bind(mimeCombo);
//		bindingManager.bind(localeCombo);

		setLayout(new MigLayout("ins 10,fillx,top"));

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
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
		mainPanel.add(questionField, "growx,wrap");

		// value
//		mainPanel.add(new JLabel("Value*:"), "align label,top");
//		mainPanel.add(GUIHelper.createScrollPane(valueArea), "growx, growy, wrap");

		// mime
//		mainPanel.add(new JLabel("MIME:"), "align label");
//		mainPanel.add(mimeCombo, "growx,wrap");

		// locale
//		mainPanel.add(new JLabel("Locale:"), "align label");
//		mainPanel.add(localeCombo, "growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]"));
//		panel.add(translationPanel, "growx");
//		panel.add(sourceCitationPanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

//		translationPanel.load(record);
//		sourceCitationPanel.load(record);
//		restrictionPanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
//		if(valueArea.isEmpty()){
//			GUIHelper.showValidationErrorAndFocus(this,
//				"Note value is required.",
//				tabbedPane, mainPanel, valueArea);
//
//			return false;
//		}

		return true;
	}

	@Override
	protected void saveData(){
		bindingManager.save(record);

//		translationPanel.save(record);
//		sourceCitationPanel.save(record);
//		restrictionPanel.save(record);
		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final ResearchStatusRecordDialog dialog = ResearchStatusRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
