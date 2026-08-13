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
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.EntityReferenceListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PartHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PersonalNameHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.VariantHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Dialog;
import java.io.Serial;


/* DONE /*/
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
 *   cultural_norm*: Xref&lt;CulturalNormRecord&gt;
 *   note*: Xref&lt;NoteRecord&gt;
 *   source*: SourceCitation
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
 */
public class PersonalNameStructureDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 6814016756734554747L;


	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_PART = "PART";
	private static final String TAG_CULTURAL_NORM = "CULTURAL_NORM";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SOURCE = "SOURCE";


	static{
		HandlerRegistry.register(new PersonalNameHandler());
		HandlerRegistry.register(new VariantHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new PartHandler());
		HandlerRegistry.register(new CulturalNormHandler());
	}


	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]10[]10[]"));

	private final BindingManager bindingManager = new BindingManager();

	private final BoundComboBox<String> typeCombo;
	private final EntityReferenceListPanel partPanel;
	private final EntityReferenceListPanel culturalNormPanel;
	private final EntityReferenceListPanel notePanel;
	private final EntityCitationListPanel sourcePanel;


	public static PersonalNameStructureDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, PersonalNameStructureDialog::new);
	}

	public static PersonalNameStructureDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		return createEdit(parent, model, record, PersonalNameStructureDialog::new);
	}


	private PersonalNameStructureDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(PersonalNameHandler.TYPE));

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
		partPanel = EntityReferenceListPanel.createForStructure(TAG_PART, this, "Parts*", model, PartHandler.TYPE);
		culturalNormPanel = EntityReferenceListPanel.createForRecord(TAG_CULTURAL_NORM, this, "Cultural Norms", model, CulturalNormHandler.TYPE);
		notePanel = EntityReferenceListPanel.createForRecord(TAG_NOTE, this, "Notes", model, NoteHandler.TYPE);
		sourcePanel = new EntityCitationListPanel(TAG_SOURCE, this, "Sources", model, SourceHandler.TYPE);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(typeCombo);


		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());

		finalizeLayout(tabbedPane);
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
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]10[]"));
		panel.add(culturalNormPanel, "growx");
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
				"At least one part is required.",
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
