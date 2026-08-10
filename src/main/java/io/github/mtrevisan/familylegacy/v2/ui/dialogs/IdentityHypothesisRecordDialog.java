package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextArea;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.ParticipantField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
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
 * Dialog for editing an {@code IdentityHypothesisRecord} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record IdentityHypothesisRecord {
 *   id: LocalID
 *   subject: IdentityCandidate
 *   candidate: IdentityCandidate
 *   comment?: Text
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   modification: ModificationStructure
 *
 *   require subject != candidate
 * }
 *
 * IdentityCandidate = oneof {
 *   individual: Xref&lt;IndividualRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 *   place: Xref&lt;PlaceRecord&gt;
 * }
 * </pre>
 */
public class IdentityHypothesisRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -3743748718107492890L;


	private static final String TAG_SUBJECT = "SUBJECT";
	private static final String TAG_CANDIDATE = "CANDIDATE";
	private static final String TAG_COMMENT = "COMMENT";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_EVIDENCE = "EVIDENCE";


	static{
		HandlerRegistry.register(new IdentityHypothesisHandler());
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new SourceHandler());
	}


	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]10[]10[]"));

	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField subject;
	private final ParticipantField candidateField;
	private final BoundTextArea commentArea;
	private final SourceCitationListPanel sourcePanel;
	private final EvidenceQualifiersPanel evidencePanel;
	private final ModificationPanel modificationPanel;


	/**
	 * Creates a new dialog to create a new record.
	 */
	public static IdentityHypothesisRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return new IdentityHypothesisRecordDialog(parent, model, null);
	}

	/**
	 * Creates a new dialog to edit an existing record.
	 */
	public static IdentityHypothesisRecordDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new IdentityHypothesisRecordDialog(parent, model, record);
	}

	private IdentityHypothesisRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(IdentityHypothesisHandler.TYPE));

		subject = new BoundTextField(TAG_SUBJECT);
		candidateField = ParticipantField.create(TAG_CANDIDATE, this, model);
		commentArea = new BoundTextArea(TAG_COMMENT, 3, 30);
		sourcePanel = new SourceCitationListPanel(TAG_SOURCE, this, model);
		evidencePanel = new EvidenceQualifiersPanel(TAG_EVIDENCE, "Evidence");
		modificationPanel = new ModificationPanel(this);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private void initComponents(){
		bindingManager.bind(subject);
		bindingManager.bind(commentArea);

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
		// Candidate
		mainPanel.add(new JLabel("Candidate*:"), "align label");
		mainPanel.add(candidateField, "growx,wrap");

		// Comment
		mainPanel.add(new JLabel("Comment:"), "align label");
		mainPanel.add(commentArea, "growx,wrap");

		// Evidence
		mainPanel.add(evidencePanel, "span 2,growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]"));
		panel.add(sourcePanel, "growx");
		return panel;
	}


	public void setSubject(final String subjectId, final String subjectHandlerType){
		if(StringUtils.isNotEmpty(subjectId) && StringUtils.isNotEmpty(subjectHandlerType)){
			subject.setText(subjectId);

			candidateField.setHandlerType(subjectHandlerType);
		}
	}


	@Override
	protected void loadData(){
		bindingManager.save(record);

		candidateField.load(record);
		sourcePanel.load(record);
		evidencePanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(StringUtils.isEmpty(subject.getText())){
			JOptionPane.showMessageDialog(this,
				"Subject is required.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		if(!candidateField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Candidate is required.",
				tabbedPane, mainPanel, candidateField);
			return false;
		}

		// Subject and candidate must be different records
		final String subjectId = subject.getText();
		final FLEFRecord candidate = candidateField.getParticipantRecord();
		final String candidateId = (candidate != null? candidate.getId(): null);
		if(subjectId != null && subjectId.equals(candidateId)){
			GUIHelper.showValidationErrorAndFocus(this,
				"Subject and candidate must be different records.",
				tabbedPane, mainPanel, candidateField);
			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		FLEFRecordHelper.removeChildren(record, TAG_SUBJECT);
		FLEFRecordHelper.removeChildren(record, TAG_CANDIDATE);
		FLEFRecordHelper.removeChildren(record, TAG_COMMENT);

		bindingManager.save(record);

		candidateField.save(record);
		sourcePanel.save(record);
		evidencePanel.save(record);
		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final IdentityHypothesisRecordDialog dialog = IdentityHypothesisRecordDialog.createNew(null, model);
			dialog.setSubject("$I1$", IndividualHandler.TYPE);
			dialog.setVisible(true);
		});
	}

}
