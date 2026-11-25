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
package io.github.mtrevisan.familylegacy.ui.tools.research;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.research.GpsText;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Assessment;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Element;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Gap;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Kind;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Readiness;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.StageStatus;
import io.github.mtrevisan.familylegacy.research.ResearchHelper;
import io.github.mtrevisan.familylegacy.sources.EvidenceTrace;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.RowTableModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.SortableTable;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ConclusionRecordDialog;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * The Genealogical Proof Standard workflow of a research question (or of a conclusion): the stages (research question, evidence
 * collection, conflict analysis, conclusion, proof statement), the five elements of the standard, and, above all, the list of what is
 * still missing before the conclusion can be considered reasonably proven (see {@link GpsWorkflow}).
 * <p>
 * From the list the work goes on in the tools that already exist: the record that is missing something opens in its editor, a conclusion
 * that is still to be made starts from a draft that already knows the question and the competing assertions, and the proof statement
 * is written from the same data (see {@link GpsProofStatementDialog}). The dialog changes nothing by itself; after anything is edited
 * the assessment is made again.
 */
public final class GpsWorkflowDialog extends JDialog{

	/** The gaps that are closed by making a conclusion. */
	private static final Set<Kind> CONCLUDABLE = Set.of(Kind.NO_CONCLUSION, Kind.UNRESOLVED_CONFLICT, Kind.CONTRADICTED,
		Kind.UNADDRESSED_CONTRADICTION);

	private final FLEFModel model;

	private final JComboBox<FLEFRecord> subjectCombo = new JComboBox<>();
	private final JLabel readiness = new JLabel(StringUtils.SPACE);
	private final JLabel evidenceLine = new JLabel(StringUtils.SPACE);
	private final JLabel researchLine = new JLabel(StringUtils.SPACE);
	private final JTable stageTable = new SortableTable();
	private final JTable elementTable = new SortableTable();
	private final JTable gapTable = new SortableTable();
	private final JButton openRecord = new JButton(I18N.t("button.gps.open_gap"));
	private final JButton createConclusion = new JButton(I18N.t("button.gps.create_conclusion"));
	private final JButton statement = new JButton(I18N.t("button.gps.statement"));

	private Assessment assessment;


	/**
	 * @param owner   the owner window
	 * @param model   the model
	 * @param initial the research question or the conclusion to start from (any other record, or {@code null}, starts with the first
	 *                research question)
	 */
	public GpsWorkflowDialog(final Window owner, final FLEFModel model, final FLEFRecord initial){
		super(owner, I18N.t("dialog.gps.title"), ModalityType.APPLICATION_MODAL);

		ToolDialogs.installEscapeToClose(this);

		this.model = model;

		for(final JTable table : List.of(stageTable, elementTable, gapTable)){
			table.setRowHeight(22);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.setAutoCreateRowSorter(true);
		}
		stageTable.setFocusable(false);
		stageTable.setRowSelectionAllowed(false);
		elementTable.setFocusable(false);
		elementTable.setRowSelectionAllowed(false);
		gapTable.getSelectionModel()
			.addListSelectionListener(e -> updateButtons());
		gapTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && selectedGap() != null)
					work();
			}
		});

		subjectCombo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean selected,
					final boolean focus){
				return super.getListCellRendererComponent(list, (value instanceof FLEFRecord record? describe(record): StringUtils.EMPTY),
					index, selected, focus);
			}
		});
		subjectCombo.setModel(new DefaultComboBoxModel<>(subjects(initial).toArray(new FLEFRecord[0])));
		if(initial != null)
			for(int i = 0; i < subjectCombo.getItemCount(); i ++)
				if(subjectCombo.getItemAt(i).getId().equals(initial.getId()))
					subjectCombo.setSelectedIndex(i);
		subjectCombo.addActionListener(e -> reload());

		openRecord.addActionListener(e -> openSelected());
		createConclusion.addActionListener(e -> concludeSelected());
		statement.addActionListener(e -> {
			final FLEFRecord subject = selectedSubject();
			if(subject != null)
				new GpsProofStatementDialog(this, model, subject)
					.setVisible(true);
			reload();
		});
		final JButton refresh = new JButton(I18N.t("button.gps.refresh"));
		refresh.addActionListener(e -> reload());
		final JButton close = new JButton(I18N.t("button.close"));
		close.addActionListener(e -> dispose());

		final JPanel chooser = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
		chooser.add(new JLabel(I18N.t("label.gps.question")));
		chooser.add(subjectCombo);

		final JPanel summary = new JPanel();
		summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
		summary.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 8));
		for(final JLabel label : List.of(readiness, evidenceLine, researchLine))
			summary.add(label);
		final JPanel north = new JPanel(new BorderLayout());
		north.add(chooser, BorderLayout.NORTH);
		north.add(summary, BorderLayout.CENTER);

		final JPanel overview = new JPanel(new GridLayout(1, 2, 8, 0));
		overview.add(titled(stageTable, I18N.t("border.gps.stages"), 150));
		overview.add(titled(elementTable, I18N.t("label.gps.elements"), 150));
		final JScrollPane gapScroll = titled(gapTable, I18N.t("border.gps.gaps"), 220);
		final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, overview, gapScroll);
		split.setResizeWeight(0.35);
		split.setBorder(null);
		split.setPreferredSize(new Dimension(900, 440));

		setLayout(new BorderLayout(8, 8));
		add(north, BorderLayout.NORTH);
		add(split, BorderLayout.CENTER);
		add(ToolDialogs.createFooter(StringUtils.SPACE, openRecord, createConclusion, statement, refresh, close), BorderLayout.SOUTH);

		reload();

		pack();
		setLocationRelativeTo(owner);
	}


	private static JScrollPane titled(final JTable table, final String title, final int height){
		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createTitledBorder(title));
		scroll.setPreferredSize(new Dimension(430, height));
		return scroll;
	}

	/** The research questions, and the conclusion the dialog is opened from if it is not already one of them. */
	private List<FLEFRecord> subjects(final FLEFRecord initial){
		final List<FLEFRecord> subjects = new ArrayList<>();
		if(initial != null && ConclusionReader.isOfType(initial) && !PrivacyService.isHidden(model, initial))
			subjects.add(initial);
		final List<FLEFRecord> questions = new ArrayList<>(PrivacyService.visible(model, ResearchHelper.listQuestions(model)));
		questions.sort((a, b) -> ResearchHelper.questionTitle(a).compareToIgnoreCase(ResearchHelper.questionTitle(b)));
		subjects.addAll(questions);
		return subjects;
	}

	private String describe(final FLEFRecord record){
		return (ResearchQuestionReader.isOfType(record)
			? ResearchHelper.questionTitle(record) + " (" + record.getId() + ")"
			: RecordDisplay.describe(record, model));
	}

	private FLEFRecord selectedSubject(){
		return (FLEFRecord)subjectCombo.getSelectedItem();
	}

	private Gap selectedGap(){
		final int viewRow = gapTable.getSelectedRow();
		final int row = (viewRow >= 0? gapTable.convertRowIndexToModel(viewRow): -1);
		return (assessment != null && row >= 0 && row < assessment.gaps().size()? assessment.gaps().get(row): null);
	}


	/* ======================================================================
	 *                          The assessment
	 * ====================================================================== */

	/** Makes the assessment again from the model, keeping the subject and the gap in view if they are still there. */
	private void reload(){
		final Gap selected = selectedGap();
		final FLEFRecord subject = selectedSubject();
		if(subject == null){
			assessment = null;
			readiness.setText(I18N.t("text.gps.none"));
			readiness.setForeground(ToolDialogs.WARN_COLOR);
			evidenceLine.setText(StringUtils.SPACE);
			researchLine.setText(StringUtils.SPACE);
			show(new Object[0][], stageTable, "column.gps.stage", "column.gps.state", "column.gps.required", "column.gps.recommended");
			show(new Object[0][], elementTable, "column.gps.element", "column.gps.state");
			show(new Object[0][], gapTable, "column.gps.severity", "column.gps.element", "column.gps.missing");
			updateButtons();
			return;
		}

		assessment = GpsWorkflow.assess(model, subject);
		readiness.setText(GpsText.readiness(assessment.readiness()));
		readiness.setForeground(assessment.readiness() == Readiness.REASONABLY_PROVEN || assessment.readiness() == Readiness.COMPLETE
			? ToolDialogs.OK_COLOR: ToolDialogs.WARN_COLOR);
		final GpsWorkflow.EvidenceSummary evidence = assessment.evidence();
		evidenceLine.setText(I18N.tf("text.gps.evidence", evidence.assertions(), evidence.citations(), evidence.supporting(),
			evidence.sources().size(), evidence.originals()));
		researchLine.setText(I18N.tf("text.gps.activities", assessment.activities().size(), assessment.tasks().size(),
			assessment.conclusions().size()));

		final List<Object[]> stages = new ArrayList<>();
		for(final StageStatus status : assessment.stages())
			stages.add(new Object[]{GpsText.stage(status.stage()), GpsText.stageState(status.state()), status.required(), status.recommended()});
		show(stages.toArray(new Object[0][]), stageTable, "column.gps.stage", "column.gps.state", "column.gps.required",
			"column.gps.recommended");
		stageTable.getColumnModel().getColumn(0).setPreferredWidth(170);
		stageTable.getColumnModel().getColumn(1).setPreferredWidth(170);
		stageTable.getColumnModel().getColumn(2).setPreferredWidth(80);
		stageTable.getColumnModel().getColumn(3).setPreferredWidth(110);

		final List<Object[]> elements = new ArrayList<>();
		for(final Element element : Element.values()){
			final int missing = assessment.gaps(element).size();
			//with no assertion (and no conclusion) there is nothing to check yet: "met" would say more than the file shows
			final boolean nothingToCheck = (assessment.evidence().assertions() == 0
				&& (element == Element.CITATIONS || element == Element.ANALYSIS
				|| element == Element.CONFLICT_RESOLUTION && assessment.conclusions().isEmpty()));
			elements.add(new Object[]{GpsText.element(element), (missing > 0? I18N.tf("text.gps.element_state.missing", missing)
				: I18N.t(nothingToCheck? "text.gps.element_state.none": "text.gps.element_state.met"))});
		}
		show(elements.toArray(new Object[0][]), elementTable, "column.gps.element", "column.gps.state");
		elementTable.getColumnModel().getColumn(0).setPreferredWidth(260);

		final List<Object[]> gaps = new ArrayList<>();
		for(final Gap gap : assessment.gaps())
			gaps.add(new Object[]{GpsText.severity(gap.severity()), GpsText.element(gap.kind().element()), GpsText.gap(gap)});
		if(gaps.isEmpty())
			gaps.add(new Object[]{StringUtils.EMPTY, StringUtils.EMPTY, I18N.t("text.gps.nothing_missing")});
		show(gaps.toArray(new Object[0][]), gapTable, "column.gps.severity", "column.gps.element", "column.gps.missing");
		gapTable.getColumnModel().getColumn(0).setPreferredWidth(90);
		gapTable.getColumnModel().getColumn(1).setPreferredWidth(230);
		gapTable.getColumnModel().getColumn(2).setPreferredWidth(580);

		if(selected != null)
			for(int row = 0; row < assessment.gaps().size(); row ++){
				final Gap gap = assessment.gaps().get(row);
				if(gap.kind() == selected.kind() && StringUtils.equals(gap.subjectId(), selected.subjectId())){
					final int viewRow = gapTable.convertRowIndexToView(row);
					gapTable.setRowSelectionInterval(viewRow, viewRow);
					break;
				}
			}
		updateButtons();
	}

	private static void show(final Object[][] rows, final JTable table, final String... columns){
		final String[] names = new String[columns.length];
		for(int i = 0; i < columns.length; i ++)
			names[i] = I18N.t(columns[i]);
		//the table creates its row sorter again each time the model is replaced (it has auto-create on): the sort and the selection are put back
		RowTableModel.replaceModel(table, new RowTableModel(names, List.of(rows)));
	}

	private void updateButtons(){
		final Gap gap = selectedGap();
		final boolean subject = (selectedSubject() != null);
		openRecord.setEnabled(subject && (gap == null || gap.subjectId() != null || recordToOpen(gap) != null));
		createConclusion.setEnabled(subject && !assessment.questions().isEmpty() && (gap == null? assessment.gaps().stream()
			.anyMatch(g -> g.kind() == Kind.NO_CONCLUSION): CONCLUDABLE.contains(gap.kind())));
		statement.setEnabled(subject);
	}


	/* ======================================================================
	 *                          The work
	 * ====================================================================== */

	/** The record a gap is about; the question for a gap that is about the whole research. */
	private FLEFRecord recordToOpen(final Gap gap){
		if(gap != null && gap.subjectId() != null)
			return model.getRecordById(gap.subjectId());

		return (!assessment.questions().isEmpty()? assessment.questions().getFirst(): selectedSubject());
	}

	private void openSelected(){
		final FLEFRecord record = recordToOpen(selectedGap());
		if(record == null)
			return;

		ToolDialogs.openRecordEditor(this, model, record);
		reload();
	}

	/** Double click: makes the conclusion if that is what is missing, otherwise opens the record. */
	private void work(){
		final Gap gap = selectedGap();
		if(gap != null && CONCLUDABLE.contains(gap.kind()) && createConclusion.isEnabled())
			concludeSelected();
		else
			openSelected();
	}

	/**
	 * Starts a conclusion for the question: with the assertions that compete (the one the gap is about and its competitors) as the
	 * records it resolves, and the question already set. Nothing is saved until the user saves it.
	 */
	private void concludeSelected(){
		if(assessment == null || assessment.questions().isEmpty())
			return;

		final Gap gap = selectedGap();
		final ConclusionRecordDialog dialog = ConclusionRecordDialog.createNew(this, model)
			.withQuestion(assessment.questions().getFirst());
		if(gap != null && gap.subjectId() != null){
			final FLEFRecord subject = model.getRecordById(gap.subjectId());
			if(subject != null){
				dialog.withResolvedTarget(subject, null);
				final EvidenceTrace trace = EvidenceTrace.of(model, gap.subjectId());
				if(trace != null)
					for(final EvidenceTrace.Assertion alternative : trace.alternatives())
						dialog.withResolvedTarget(alternative.record(), null);
			}
		}
		dialog.setVisible(true);

		reload();
	}

}
