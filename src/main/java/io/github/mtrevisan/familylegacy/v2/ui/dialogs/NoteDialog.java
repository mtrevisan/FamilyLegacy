package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
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
import io.github.mtrevisan.familylegacy.v2.ui.components.TranslationListPanel.TranslationEntry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog for editing a {@code NOTE_RECORD} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * NOTE_RECORD :=
 * n @<XREF:NOTE>@ NOTE    {1:1}
 *   +1 TITLE <TEXT>    {0:1}
 *   +1 VALUE <SUBMITTER_TEXT>    {1:1}
 *   +1 MIME <MIME_TYPE>    {0:1}
 *   +1 LOCALE <LOCALE_CODE>    {0:1}
 *   +1 TRANSLATION    {0:M}
 *     +2 VALUE <TEXT>    {1:1}
 *     +2 LOCALE <LOCALE_CODE>    {0:1}
 *   +1 <<SOURCE_CITATION>>    {0:M}
 *   +1 <<RESTRICTION_STRUCTURE>>    {0:1}
 *   +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class NoteDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212975L;


	// Handlers
	static{
		HandlerRegistry.register(new NoteHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField titleField = new BoundTextField("TITLE", 30);
	private final BoundTextArea valueArea = new BoundTextArea("VALUE", 10, 30);
	private final BoundComboBox<String> mimeCombo = new BoundComboBox<>("MIME", new String[]{StringUtils.EMPTY, "text/plain", "text/html", "text/markdown"});
	private final BoundComboBox<String> localeCombo = new BoundComboBox<>("LOCALE", new String[]{StringUtils.EMPTY, "en", "en-US", "en-GB", "it", "fr", "de", "es", "pt", "la", "zh", "ja", "ru"});
	private final TranslationListPanel translationPanel;
	private final SourceCitationListPanel sourceCitationPanel;
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]5[]5[]"));
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;


	public static NoteDialog createNew(final Frame parent, final FLEFModel model){
		return new NoteDialog(parent, model, null);
	}

	public static NoteDialog createEdit(final Frame parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new NoteDialog(parent, model, record);
	}


	private NoteDialog(final Frame parent, final FLEFModel model, final FLEFRecord record){
		super(parent, buildTitle(record), model, record, HandlerRegistry.getHandler(NoteHandler.TYPE));

		translationPanel = new TranslationListPanel(model, this);
		restrictionPanel = new RestrictionPanel(this);
		modificationPanel = new ModificationPanel(this);
		sourceCitationPanel = new SourceCitationListPanel(model, this);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private static String buildTitle(final FLEFRecord record){
		return (record == null? "New Note": "Edit Note - " + record.getId());
	}

	@Override
	protected void initComponents(){
		// Register bound components
		bindingManager.bind(titleField);
		bindingManager.bind(valueArea);
		bindingManager.bind(mimeCombo);
		bindingManager.bind(localeCombo);

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());

		final JPanel restrictionContainer = new JPanel(new MigLayout("top", "[grow]", "[grow]"));
		restrictionContainer.add(restrictionPanel, "grow");
		tabbedPane.addTab("Restriction", restrictionContainer);

		final JPanel modificationContainer = new JPanel(new MigLayout("top", "[grow]", "[grow]"));
		modificationContainer.add(modificationPanel, "grow");
		tabbedPane.addTab("Modification", modificationContainer);

		setLayout(new MigLayout("fillx,top"));
		add(tabbedPane, "growx");

		add(createButtonPanel(), BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// TITLE
		mainPanel.add(new JLabel("Title:"), "align label");
		mainPanel.add(titleField, "growx,wrap");

		// VALUE
		mainPanel.add(new JLabel("Value*:"), "align label,top");
		valueArea.setLineWrap(true);
		valueArea.setWrapStyleWord(true);
		valueArea.setToolTipText("Markdown supported. Use [text](@<XREF:ID>@) for references, [text](confidential) for confidential data.");
		mainPanel.add(GUIHelper.createScrollPane(valueArea), "growx, growy, wrap");

		// MIME
		mainPanel.add(new JLabel("MIME:"), "align label");
		mainPanel.add(mimeCombo, "growx,wrap");

		// LOCALE
		mainPanel.add(new JLabel("Locale:"), "align label");
		mainPanel.add(localeCombo, "growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 5,fillx,top,wrap 1", "[grow]", "[]5[]"));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Translations panel
		panel.add(translationPanel, "growx");

		// Source Citations panel
		panel.add(sourceCitationPanel, "growx");

		return panel;
	}

	// ==================== Load / Save ====================

	@Override
	protected void loadData(){
		setTitle(buildTitle(record));

		// ---- Simple fields: load via binding manager ----
		bindingManager.loadFromRecord(record);

		// Translations
		final List<TranslationEntry> translations = new ArrayList<>();
		for(final FLEFRecord child : record.getChildren())
			if("TRANSLATION".equals(child.getTag())){
				final String translationLocale = FLEFRecordUtils.getChildValue(child, "LOCALE");
				final String translationValue = FLEFRecordUtils.getChildValue(child, "VALUE");
				if(StringUtils.isNotEmpty(translationValue))
					translations.add(new TranslationEntry(translationLocale, translationValue));
			}
		translationPanel.setItems(translations);

		// Source Citations
		final List<FLEFRecord> citations = new ArrayList<>();
		for(final FLEFRecord child : record.getChildren())
			if("SOURCE".equals(child.getTag()))
				citations.add(child);
		sourceCitationPanel.loadFromCitations(citations);

		// Restriction & Modification
		final FLEFRecord restrictionStruct = FLEFRecordUtils.findChild(record, "RESTRICTION");
		restrictionPanel.loadFromRecord(restrictionStruct);

		// MODIFICATION
		modificationPanel.loadFromRecord(record);
	}

	@Override
	protected boolean validateData(){
		if(valueArea.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this, "Note VALUE is required.",
				tabbedPane, mainPanel, valueArea);

			return false;
		}

		return (!restrictionPanel.hasData() || restrictionPanel.validateRequiredFields());
	}

	@Override
	protected void saveRecord(){
		FLEFRecordUtils.removeAllChildren(record);

		bindingManager.saveToRecord(record);

		// Translations
		final List<TranslationEntry> translations = translationPanel.getItems();
		for(int i = 0; i < translations.size(); i ++){
			final TranslationEntry entry = translations.get(i);
			FLEFRecordUtils.addChild(record, "TRANSLATION[" + i + "].VALUE", entry.getValue());
			if(StringUtils.isNotEmpty(entry.getLocale()))
				FLEFRecordUtils.addChild(record, "TRANSLATION[" + i + "].LOCALE", entry.getLocale());
		}

		// Source Citations
		for(final FLEFRecord citation : sourceCitationPanel.getCitations()){
			citation.setTag("SOURCE");
			record.addChild(citation);
		}

		// RESTRICTION
		restrictionPanel.saveToRecord(record);

		// MODIFICATION
		modificationPanel.saveToRecord(record);

		if(isNew)
			model.addRecord(record);
		isSaved = true;

		dispose();
	}


	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ignored){}

		FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			NoteDialog dialog = NoteDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
