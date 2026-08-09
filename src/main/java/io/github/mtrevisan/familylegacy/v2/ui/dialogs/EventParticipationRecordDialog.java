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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.EventField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.ParticipantField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
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
import java.util.List;


/* DONE */
/**
 * Dialog for editing an {@code EVENT_PARTICIPATION_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record EventParticipationRecord {
 *   id: LocalID
 *   event: Xref&lt;EventRecord&gt;
 *   participant: EntityParticipant
 *   role?: enum {
 *     child, parent, spouse, power_of_attorney, prisoner, witness, officiant, informant, executor, grantor, grantee,
 *     landlord, tenant, soldier, commander, victim, survivor, accused, judge
 *   } | Text
 *   note*: Xref&lt;NoteRecord&gt;
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 *
 * EntityParticipant = oneof {
 *   individual: Xref&lt;IndividualRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 * }
 * </pre>
 */
public class EventParticipationRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 3519955064561398245L;


	private static final String TAG_EVENT = "EVENT";
	private static final String TAG_PARTICIPANT = "PARTICIPANT";
	private static final String TAG_ROLE = "ROLE";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_RESTRICTION = "RESTRICTION";


	static{
		HandlerRegistry.register(new EventParticipationHandler());
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new GroupHandler());

	}


	private final BindingManager bindingManager = new BindingManager();

	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]"));

	private final EventField eventField;
	private final ParticipantField participantField;
	private final BoundComboBox<String> roleCombo;
	private final NoteListPanel notePanel;
	private final SourceCitationListPanel sourcePanel;
	private final EvidenceQualifiersPanel evidencePanel;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;


	/**
	 * Creates a new dialog to create a new record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @return	A new dialog instance.
	 */
	public static EventParticipationRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return new EventParticipationRecordDialog(parent, model, null);
	}

	/**
	 * Creates a new dialog to edit an existing record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @param record	The record to edit (must not be {@code null}).
	 * @return	A new dialog instance.
	 */
	public static EventParticipationRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new EventParticipationRecordDialog(parent, model, record);
	}


	private EventParticipationRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(EventParticipationHandler.TYPE));

		eventField = EventField.create(TAG_EVENT, this, model);
		participantField = ParticipantField.create(TAG_PARTICIPANT, this, model,
			List.of(IndividualHandler.TYPE, GroupHandler.TYPE));
		roleCombo = new BoundComboBox<>(TAG_ROLE, new String[]{
			StringUtils.EMPTY,
			"child", "parent", "spouse", "power_of_attorney", "prisoner", "witness",
			"officiant", "informant", "executor", "grantor", "grantee",
			"landlord", "tenant", "soldier", "commander", "victim", "survivor",
			"accused", "judge"
		});
		roleCombo.setEditable(true);
		notePanel = new NoteListPanel(TAG_NOTE, this, model);
		sourcePanel = new SourceCitationListPanel(TAG_SOURCE, this, model);
		evidencePanel = new EvidenceQualifiersPanel(TAG_EVIDENCE, "Evidence");
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(roleCombo);

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
		// event
		mainPanel.add(new JLabel("Event*:"), "align label");
		mainPanel.add(eventField, "growx,wrap");

		// participant
		mainPanel.add(new JLabel("Participant*:"), "align label");
		mainPanel.add(participantField, "growx,wrap");

		// role
		mainPanel.add(new JLabel("Role:"), "align label");
		mainPanel.add(roleCombo, "growx,wrap");

		// evidence
		mainPanel.add(evidencePanel, "span 2,growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]"));
		panel.add(notePanel, "growx");
		panel.add(sourcePanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
		eventField.load(record);
		participantField.load(record);

		bindingManager.load(record);

		notePanel.load(record);
		sourcePanel.load(record);
		evidencePanel.load(record);
		restrictionPanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		final String eventId = (String)eventField.getClientProperty("selectedId");
		if(StringUtils.isEmpty(eventId)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Event is required.",
				tabbedPane, mainPanel, eventField);
			return false;
		}

		if(!participantField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Participant is required.",
				tabbedPane, mainPanel, participantField);
			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		FLEFRecordHelper.removeChildren(record, TAG_EVENT);
		FLEFRecordHelper.removeChildren(record, TAG_PARTICIPANT);
		FLEFRecordHelper.removeChildren(record, TAG_ROLE);

		eventField.save(record);
		participantField.save(record);

		bindingManager.save(record);

		notePanel.saveReferences(record);
		sourcePanel.save(record);
		evidencePanel.save(record);
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
			final EventParticipationRecordDialog dialog = EventParticipationRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
