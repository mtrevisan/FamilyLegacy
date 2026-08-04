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
import io.github.mtrevisan.familylegacy.v2.ui.components.ClassifiedNameListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ConclusionListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.CulturalNormListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.GeneralRelationshipListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.MemberRelationshipListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.PreferredImagePanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
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
 * Dialog for editing a {@code GROUP_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record GroupRecord {
 *   id: LocalID
 *   name*: ClassifiedName
 *   type?: enum { family, household, neighborhood, fraternity, club, literary_society, association, organization, tribe } | Text
 *   cultural_norm*: Xref&lt;CulturalNormRecord&gt;
 *   note*: Xref&lt;NoteRecord&gt;
 *   source*: SourceCitation
 *   preferred_image?: struct {
 *     uri: Uri
 *     crop?: CropRect
 *   }
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class GroupRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -4670126000119212972L;


	private static final String TAG_NAME = "NAME";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_CULTURAL_NORM = "CULTURAL_NORM";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_PREFERRED_IMAGE = "PREFERRED_IMAGE";
	private static final String TAG_RESTRICTION = "RESTRICTION";
	private static final String TAG_CONCLUSION = "CONCLUSION";

	private static final String TAG_RELATIONSHIP = "RELATIONSHIP";


	static{
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new RelationshipHandler());
		HandlerRegistry.register(new GroupHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final ClassifiedNameListPanel namePanel;
	private final BoundComboBox<String> typeCombo;
	private final CulturalNormListPanel culturalNormPanel;
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourcePanel;
	private final PreferredImagePanel preferredImagePanel;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;

	// Other
	private final ConclusionListPanel conclusionPanel;
	private final MemberRelationshipListPanel memberPanel;
	private final GeneralRelationshipListPanel relationshipPanel;


	public static GroupRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return new GroupRecordDialog(parent, model, null);
	}

	public static GroupRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new GroupRecordDialog(parent, model, record);
	}


	private GroupRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(GroupHandler.TYPE));

		namePanel = new ClassifiedNameListPanel(TAG_NAME, this, "Names", model);
		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{StringUtils.EMPTY, "family", "household",
			"neighbourhood", "fraternity", "club", "research group", "literary society", "association", "organisation",
			"tribe"});
		typeCombo.setEditable(true);
		culturalNormPanel = new CulturalNormListPanel(TAG_CULTURAL_NORM, this, model);
		notePanel = new NoteListPanel(TAG_NOTE, this, model);
		sourcePanel = new SourceCitationListPanel(TAG_SOURCE, this, model);
		preferredImagePanel = new PreferredImagePanel(TAG_PREFERRED_IMAGE, this);
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this, model);

		conclusionPanel = new ConclusionListPanel(TAG_CONCLUSION, this, model);
		memberPanel = new MemberRelationshipListPanel(this, model, () -> (record != null? record.getId(): null));
		relationshipPanel = new GeneralRelationshipListPanel(TAG_RELATIONSHIP, this, model);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	protected void initComponents(){
		bindingManager.bind(typeCombo);

		setLayout(new MigLayout("ins 10,fillx,top"));

		final JTabbedPane tabbedPane = new JTabbedPane();
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
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,wrap 1", "[grow]", "[]5[]5[]10[]"));

		panel.add(preferredImagePanel, "growx,align center");

		// type
		final JPanel typePanel = new JPanel(new MigLayout("ins 0,fillx", "[right]rel[grow]"));
		typePanel.add(new JLabel("Type:"), "align label");
		typePanel.add(typeCombo, "growx");
		panel.add(typePanel, "growx");

		// names
		panel.add(namePanel, "growx");

		// members
		panel.add(memberPanel, "growx");

		return panel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]5[]5[]"));
		panel.add(conclusionPanel, "growx");
		panel.add(relationshipPanel, "growx");
		panel.add(culturalNormPanel, "growx");
		panel.add(notePanel, "growx");
		panel.add(sourcePanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		namePanel.load(record);
		culturalNormPanel.load(record);
		notePanel.load(record);
		sourcePanel.load(record);
		preferredImagePanel.load(record);
		restrictionPanel.load(record);
		modificationPanel.load(record);

		conclusionPanel.load(record);
		memberPanel.load(record);
		relationshipPanel.load(record);
	}

	@Override
	protected boolean validData(){
		return true;
	}

	@Override
	protected void saveData(){
		bindingManager.save(record);

		namePanel.save(record);
		culturalNormPanel.save(record);
		notePanel.save(record);
		sourcePanel.save(record);
		preferredImagePanel.save(record);
		restrictionPanel.save(record);
		modificationPanel.save(record);

		conclusionPanel.save(record);
		memberPanel.save(record);
		relationshipPanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final GroupRecordDialog dialog = GroupRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
