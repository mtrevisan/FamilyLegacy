package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.components.CulturalNormListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.PartListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PersonalNameStructureHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.TextValueVariantHandler;
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


/* ONGOING */
/**
 * Dialog for editing a {@code PERSONAL_NAME_STRUCTURE} according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * struct PersonalNameStructure {
 *   type?: enum {
 *     official, religious, birth,
 *     married, maiden, divorce, adoption, fostering,
 *     legal, immigrant, adapted,
 *     aka, nickname, artistic, professional, user,
 *     regnal, slave_name
 *   } | Text
 *   part+: struct {
 *     type: enum {
 *       given, generation,
 *       patronymic, matronymic, kunya,
 *       family, family_nickname, lineage, house, clan, tribal, caste,
 *       toponymic,
 *       title, occupational, prefix, suffix,
 *       nickname, regnal, religious, posthumous
 *     } | Text
 *     value: Text
 *     variant*: TextValueVariant
 *   }
 *   cultural_norm*: Xref<CulturalNormRecord>
 *   note*: Xref<NoteRecord>
 *   source*: SourceCitation
 * }
 * </pre>
 */
public class PersonalNameStructureDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 6814016756734554747L;


	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_CULTURAL_NORM = "CULTURAL_NORM";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SOURCE = "SOURCE";


	static{
		HandlerRegistry.register(new PersonalNameStructureHandler());
		HandlerRegistry.register(new TextValueVariantHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new CulturalNormHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]5[]5[]5[]"));
	private final BoundComboBox<String> typeCombo;
	private final PartListPanel partPanel;
	private final CulturalNormListPanel culturalNormPanel;
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourcePanel;


	public static PersonalNameStructureDialog createNew(final Dialog parent, final FLEFModel model){
		return new PersonalNameStructureDialog(parent, model, null);
	}

	public static PersonalNameStructureDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new PersonalNameStructureDialog(parent, model, record);
	}


	private PersonalNameStructureDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(PersonalNameStructureHandler.TYPE));

		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY, "official", "colonial", "indigenous"
		});
		partPanel = new PartListPanel(this, model);
		culturalNormPanel = new CulturalNormListPanel(TAG_CULTURAL_NORM, this, model);
		notePanel = new NoteListPanel(TAG_NOTE, this, model);
		sourcePanel = new SourceCitationListPanel(TAG_SOURCE, this, model);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private void initComponents(){
		bindingManager.bind(typeCombo);

		setLayout(new MigLayout("ins 10,fillx,top"));

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(), this::save, this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		// type
		mainPanel.add(new JLabel("Type:"), "align label");
		mainPanel.add(typeCombo, "growx,wrap");

		// parts
		mainPanel.add(partPanel, "span 2,growx,wrap");

		// qualifiers
		mainPanel.add(culturalNormPanel, "span 2,growx");

		return mainPanel;
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

		partPanel.load(record);
		culturalNormPanel.load(record);
		notePanel.load(record);
		sourcePanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(partPanel.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"At least one name is required.",
				tabbedPane, mainPanel, partPanel);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		bindingManager.save(record);

		partPanel.save(record);
		culturalNormPanel.save(record);
		notePanel.save(record);
		sourcePanel.save(record);
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
