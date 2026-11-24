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
package io.github.mtrevisan.familylegacy.ui.components.projections.actions.provenance;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.sources.EvidenceTrace;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.RowTableModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.ProgressDialog;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;


/**
 * Answers "why do we believe this?": for a record (or, for an individual, for each fact that is known about them) the chain
 * assertion - citation - extract - source, with the citations that support and the ones that contradict it, the confidence, the
 * competing assertions, the conclusions that evaluate them and the reasoning path. The numbers come from {@link EvidenceTrace}; the
 * dialog never changes the file by itself, but a row can be opened to fix the record or the source, and the trace is read again.
 */
public final class EvidenceTraceDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(EvidenceTraceDialog.class);

	private static final int COLUMN_STANCE = 2;

	private final FLEFModel model;
	private final String recordId;
	private final DefaultListModel<EvidenceTrace> facts = new DefaultListModel<>();
	private final JList<EvidenceTrace> factList = new JList<>(facts);
	private final JLabel verdict = new JLabel(StringUtils.SPACE);
	private final JTable table = new JTable();
	private final JTextArea reasoning = new JTextArea();
	private final JButton openSource = new JButton(I18N.t("button.open_source"));
	private List<EvidenceTrace.Assertion> assertions = List.of();
	private List<EvidenceTrace.Link> rowLinks = List.of();


	/**
	 * @param owner	The owner window.
	 * @param model	The model.
	 * @param recordId	The record to explain; if it is an individual, every fact about them is explained.
	 */
	public EvidenceTraceDialog(final Window owner, final FLEFModel model, final String recordId){
		this(owner, model, recordId, tracesOf(model, recordId));
	}

	/**
	 * Shows the trace of a record. Reading the evidence of a big file takes a while, so it is done in the background, with a progress
	 * dialog that tells the user that something is going on, and the dialog is shown when the traces are ready.
	 *
	 * @param owner	The owner window.
	 * @param model	The model.
	 * @param recordId	The record to explain.
	 */
	public static void open(final Window owner, final FLEFModel model, final String recordId){
		final String title = I18N.tf("dialog.evidence_trace.title", recordId);
		final ProgressDialog progress = new ProgressDialog(owner, title, I18N.t("progress.evidence_trace.computing"));

		final SwingWorker<List<EvidenceTrace>, Void> worker = new SwingWorker<>(){
			@Override
			protected List<EvidenceTrace> doInBackground(){
				return tracesOf(model, recordId);
			}

			@Override
			protected void done(){
				progress.close();
				try{
					new EvidenceTraceDialog(owner, model, recordId, get())
						.setVisible(true);
				}
				catch(final InterruptedException ie){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ee){
					fail(owner, title, (ee.getCause() != null? ee.getCause(): ee));
				}
				catch(final RuntimeException | Error ex){
					fail(owner, title, ex);
				}
			}
		};
		worker.execute();
		//modal: it blocks the caller until done() disposes the dialog
		progress.setVisible(true);
	}

	private static void fail(final Window owner, final String title, final Throwable cause){
		LOGGER.error("Evidence trace failed", cause);
		MessageDialogs.show(owner, I18N.t("dialog.evidence_trace.error") + StringUtils.LF
			+ (cause.getMessage() != null? cause.getMessage(): cause.toString()), title, JOptionPane.ERROR_MESSAGE);
	}

	private EvidenceTraceDialog(final Window owner, final FLEFModel model, final String recordId, final List<EvidenceTrace> traces){
		super(owner, I18N.tf("dialog.evidence_trace.title", recordId), ModalityType.APPLICATION_MODAL);

		ToolDialogs.installEscapeToClose(this);

		this.model = model;
		this.recordId = recordId;

		factList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		factList.setCellRenderer(new FactRenderer());
		factList.addListSelectionListener(e -> {
			if(!e.getValueIsAdjusting())
				showSelected();
		});

		table.setRowHeight(22);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel()
			.addListSelectionListener(e -> updateButtons());
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					openSelectedRecord();
			}
		});

		reasoning.setEditable(false);
		reasoning.setLineWrap(true);
		reasoning.setWrapStyleWord(true);
		reasoning.setRows(8);

		openSource.addActionListener(e -> openSelectedSource());

		final JScrollPane factsScroll = new JScrollPane(factList);
		factsScroll.setPreferredSize(new Dimension(280, 420));
		factsScroll.setBorder(BorderFactory.createTitledBorder(I18N.t("text.evidence_trace.facts")));

		final JScrollPane tableScroll = new JScrollPane(table);
		tableScroll.setPreferredSize(new Dimension(760, 240));
		final JScrollPane reasoningScroll = new JScrollPane(reasoning);
		reasoningScroll.setBorder(BorderFactory.createTitledBorder(I18N.t("text.evidence_trace.reasoning")));

		final JPanel detail = new JPanel(new BorderLayout(4, 4));
		detail.add(verdict, BorderLayout.NORTH);
		detail.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, reasoningScroll), BorderLayout.CENTER);

		setLayout(new BorderLayout(8, 8));
		add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, factsScroll, detail), BorderLayout.CENTER);
		add(ToolDialogs.createFooter(StringUtils.EMPTY, openSource,
			ToolDialogs.createCopyButton(this::toText)), BorderLayout.SOUTH);

		load(traces);

		pack();
		setLocationRelativeTo(owner);
	}


	/** Reads the model again: it is called after a record has been edited, so that the trace is what the file is now. */
	private void reload(){
		load(tracesOf(model, recordId));
	}

	private void load(final List<EvidenceTrace> traces){
		final int selected = Math.max(0, factList.getSelectedIndex());
		facts.clear();
		for(final EvidenceTrace trace : traces)
			facts.addElement(trace);
		if(!facts.isEmpty())
			factList.setSelectedIndex(Math.min(selected, facts.size() - 1));
		else
			showSelected();
	}

	/** @return	The traces to show for the record: every fact of an individual, the one trace of any other record. */
	static List<EvidenceTrace> tracesOf(final FLEFModel model, final String recordId){
		final List<EvidenceTrace> traces = EvidenceTrace.ofIndividual(model, recordId);
		if(traces.isEmpty()){
			final EvidenceTrace trace = EvidenceTrace.of(model, recordId);
			if(trace != null)
				traces.add(trace);
		}
		return traces;
	}

	private void showSelected(){
		final EvidenceTrace trace = factList.getSelectedValue();
		final List<Object[]> rows = new ArrayList<>();
		final List<EvidenceTrace.Assertion> owners = new ArrayList<>();
		final List<EvidenceTrace.Link> links = new ArrayList<>();
		if(trace == null){
			verdict.setText(I18N.t("text.evidence_trace.empty"));
			reasoning.setText(StringUtils.EMPTY);
		}
		else{
			verdict.setText(verdictText(trace));
			reasoning.setText(reasoningText(trace));
			reasoning.setCaretPosition(0);

			final List<EvidenceTrace.Assertion> all = new ArrayList<>();
			all.add(trace.subject());
			all.addAll(trace.alternatives());
			for(final EvidenceTrace.Assertion assertion : all){
				final String role = I18N.t(assertion == trace.subject()
						? "text.evidence_trace.role.subject": "text.evidence_trace.role.alternative")
					+ (assertion.preferred()? " (" + I18N.t("text.evidence_trace.preferred") + ")": StringUtils.EMPTY);
				final String label = role + ": " + assertion.text()
					+ (assertion.confidence().isEmpty()? StringUtils.EMPTY: " [" + assertion.confidence() + "]");
				if(assertion.links().isEmpty()){
					rows.add(new Object[]{label, I18N.t("text.evidence_trace.uncited"), StringUtils.EMPTY, StringUtils.EMPTY,
						StringUtils.EMPTY, StringUtils.EMPTY});
					owners.add(assertion);
					links.add(null);
				}
				for(final EvidenceTrace.Link link : assertion.links()){
					rows.add(new Object[]{label, link.sourceTitle() + (link.locator().isEmpty()? StringUtils.EMPTY: ", " + link.locator()),
						link.stance(), extractText(link), link.evidence(), Integer.toString(link.weight())});
					owners.add(assertion);
					links.add(link);
				}
			}
		}
		assertions = owners;
		rowLinks = links;
		table.setModel(new RowTableModel(new String[]{I18N.t("column.evidence_trace.assertion"), I18N.t("column.source"),
			I18N.t("column.provenance.stance"), I18N.t("column.provenance.extract"), I18N.t("column.provenance.evidence"),
			I18N.t("column.evidence_trace.weight")}, rows));
		final int[] widths = {300, 220, 80, 220, 130, 60};
		for(int column = 0; column < widths.length; column ++)
			table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
		table.getColumnModel().getColumn(COLUMN_STANCE).setCellRenderer(new StanceRenderer());
		updateButtons();
	}

	private static String extractText(final EvidenceTrace.Link link){
		final List<String> texts = new ArrayList<>(link.extracts().size());
		for(final var extract : link.extracts()){
			final String text = ExtractReader.extractText(extract);
			if(StringUtils.isNotBlank(text))
				texts.add(text.strip());
		}
		return String.join(" | ", texts);
	}

	/** @return	The line that tells how the evidence stands. */
	static String verdictText(final EvidenceTrace trace){
		return I18N.t("text.evidence_trace.verdict." + trace.verdict().name().toLowerCase(Locale.ROOT));
	}

	/** @return	The reasoning path as text, one line for each step, then the narrative of the conclusions. */
	static String reasoningText(final EvidenceTrace trace){
		final StringBuilder sb = new StringBuilder();
		int number = 1;
		for(final EvidenceTrace.Step step : trace.reasoning())
			sb.append(number ++).append(". ")
				.append(I18N.tf("text.evidence_trace.step." + step.key(), step.args().toArray()))
				.append(StringUtils.LF);
		for(final EvidenceTrace.ConclusionLink conclusion : trace.conclusions())
			if(!conclusion.narrative().isEmpty())
				sb.append(StringUtils.LF)
					.append(conclusion.conclusion().getId()).append(": ").append(conclusion.narrative())
					.append(StringUtils.LF);
		return sb.toString();
	}

	private String toText(){
		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < facts.size(); i ++){
			final EvidenceTrace trace = facts.get(i);
			sb.append(trace.subject().text()).append(": ").append(verdictText(trace)).append(StringUtils.LF)
				.append(reasoningText(trace)).append(StringUtils.LF);
		}
		return sb.toString();
	}


	private EvidenceTrace.Link selectedLink(){
		final int viewRow = table.getSelectedRow();
		return (viewRow >= 0? rowLinks.get(table.convertRowIndexToModel(viewRow)): null);
	}

	private EvidenceTrace.Assertion selectedAssertion(){
		final int viewRow = table.getSelectedRow();
		return (viewRow >= 0? assertions.get(table.convertRowIndexToModel(viewRow)): null);
	}

	private void updateButtons(){
		final EvidenceTrace.Link link = selectedLink();
		openSource.setEnabled(link != null && link.source() != null);
	}

	private void openSelectedRecord(){
		final EvidenceTrace.Assertion assertion = selectedAssertion();
		if(assertion == null)
			return;

		ToolDialogs.openRecordEditor(this, model, assertion.record());
		reload();
	}

	private void openSelectedSource(){
		final EvidenceTrace.Link link = selectedLink();
		if(link == null || link.source() == null)
			return;

		ToolDialogs.openRecordEditor(this, model, link.source());
		reload();
	}


	/** Shows a fact with how its evidence stands. */
	private static final class FactRenderer implements ListCellRenderer<EvidenceTrace>{

		private final DefaultListCellRenderer delegate = new DefaultListCellRenderer();

		@Override
		public Component getListCellRendererComponent(final JList<? extends EvidenceTrace> list, final EvidenceTrace value, final int index,
				final boolean isSelected, final boolean cellHasFocus){
			final JLabel label = (JLabel)delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			label.setText(value.subject().text() + " - " + verdictText(value));
			return label;
		}

	}

	/** Shows a contradicting citation in red. */
	private static final class StanceRenderer extends DefaultTableCellRenderer{

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column){
			final Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			final boolean contradicting = SourceCitationReader.STANCE_CONTRADICTS.equals(value);
			component.setForeground(contradicting && !isSelected? ToolDialogs.ERROR_COLOR
				: (isSelected? table.getSelectionForeground(): table.getForeground()));
			return component;
		}

	}

}
