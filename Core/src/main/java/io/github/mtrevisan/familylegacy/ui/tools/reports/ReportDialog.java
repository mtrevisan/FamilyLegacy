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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.reports.renderers.DocxReportRenderer;
import io.github.mtrevisan.familylegacy.ui.tools.reports.renderers.HtmlReportRenderer;
import io.github.mtrevisan.familylegacy.ui.tools.reports.renderers.MarkdownReportRenderer;
import io.github.mtrevisan.familylegacy.ui.tools.reports.renderers.PdfReportRenderer;
import io.github.mtrevisan.familylegacy.ui.tools.reports.renderers.ReportRenderer;
import io.github.mtrevisan.familylegacy.ui.tools.reports.renderers.TextReportRenderer;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Swing dialog for configuring and executing report generation for a given
 * {@link ReportType}.
 *
 * <p>The dialog exposes:</p>
 * <ul>
 *   <li>a header line showing the root subject;</li>
 *   <li>an output-format selector (Markdown, text, HTML, DOCX, PDF);</li>
 *   <li>a language selector;</li>
 *   <li>the full {@link ReportOptionsPanel};</li>
 *   <li>Generate / Cancel buttons.</li>
 * </ul>
 *
 * <p>The {@link ReportType} chosen by the caller drives both the resolution
 * of the root record and the {@link ReportConfig} tailoring performed by
 * {@link #buildReportDocument(ReportConfig, ReportProgressListener)}. Section-level filtering is
 * delegated to {@link ReportGenerator}, which dispatches by root kind.</p>
 */
public final class ReportDialog extends JDialog{

	private final ToolContext context;
	private final ReportType reportType;
	private final ReportOptionsPanel optionsPanel;
	private FLEFRecord selectedEntity;

	private JCheckBox cbMarkdown;
	private JCheckBox cbText;
	private JCheckBox cbHtml;
	private JCheckBox cbDocx;
	private JCheckBox cbPdf;

	private final JComboBox<ReportLanguage> language = new JComboBox<>(ReportLanguage.values());


	private ReportDialog(final ToolContext context, final ReportType reportType){
		super(context.owner(), reportType.getTitle(), ModalityType.APPLICATION_MODAL);

		this.context = Objects.requireNonNull(context);
		this.reportType = Objects.requireNonNull(reportType);
		this.optionsPanel = new ReportOptionsPanel(ReportConfig.defaults());

		resolveSelectedEntity();

		initComponents();
	}

	public static void showDialog(final ToolContext context, final ReportType reportType){
		SwingUtilities.invokeLater(() -> {
			final ReportDialog dialog = new ReportDialog(context, reportType);

			dialog.pack();

			dialog.setLocationRelativeTo(context.owner());

			dialog.setVisible(true);
		});
	}

	/**
	 * Resolves the root target record according to the required
	 * {@link ReportType}.
	 */
	private void resolveSelectedEntity(){
		final String selectedId = context.selectedEntityId();

		switch(reportType){
			case GROUP -> {
				if(selectedId != null && selectedId.startsWith(GroupHandler.ID_PREFIX))
					selectedEntity = context.model()
						.getRecordById(selectedId);
			}
			case ANCESTOR, DESCENDANT, INDIVIDUAL, RELATIONSHIP -> {
				if(selectedId != null && selectedId.startsWith(IndividualHandler.ID_PREFIX))
					selectedEntity = context.model()
						.getRecordById(selectedId);
			}
			case BIBLIOGRAPHY, RESEARCH_PROGRESS -> {
				// Global reports: fallback to active individual or first
				// record in the model.
				selectedEntity = context.getSelectedIndividual();
			}
		}
	}

	private void initComponents(){
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout(0, 0));

		final JPanel mainPanel = new JPanel(new MigLayout(
			"ins 10,wrap 1,fillx", "[grow,fill]", "[]10[]20[]10[]"));

		// 1. Header panel displaying report type and targeted root entity
		String targetText = "Global / Entire Dataset";
		if(selectedEntity != null){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(selectedEntity.getTag());
			targetText = handler.getDisplayText(selectedEntity, context.model());
		}
		final JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		headerPanel.add(new JLabel("<html><b>Target Subject:</b> " + targetText + "</html>"));
		mainPanel.add(headerPanel);

		// 2. Format selection panel
		mainPanel.add(createFormatSelectionPanel());

		// 3. Language selection panel
		mainPanel.add(createLanguageSelectionPanel());

		// 4. Options configuration panel
		mainPanel.add(optionsPanel);

		final JScrollPane scrollPane = new JScrollPane(mainPanel);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar()
			.setUnitIncrement(16);

		// Bottom button panel
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
		final JButton cancelBtn = new JButton(I18N.t("button.cancel"));
		final JButton generateBtn = new JButton("Generate…");

		cancelBtn.addActionListener(e -> dispose());
		generateBtn.addActionListener(e -> onGenerate());

		buttons.add(cancelBtn);
		buttons.add(generateBtn);

		add(scrollPane, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);

		setPreferredSize(new Dimension(580, 620));
	}

	private JPanel createFormatSelectionPanel(){
		final JPanel panel = new JPanel(new GridLayout(2, 3, 8, 4));
		panel.setBorder(BorderFactory.createTitledBorder("Output Formats"));

		cbText = new JCheckBox("Plain Text (.txt)", false);
		cbMarkdown = new JCheckBox("Markdown (.md)", true);
		cbHtml = new JCheckBox("HTML (.html)", false);
		cbDocx = new JCheckBox("Word (.docx)", false);
		cbPdf = new JCheckBox("PDF (.pdf)", false);

		panel.add(cbText);
		panel.add(cbMarkdown);
		panel.add(cbHtml);
		panel.add(cbDocx);
		panel.add(cbPdf);

		return panel;
	}

	private JPanel createLanguageSelectionPanel(){
		final JPanel p = new JPanel(new MigLayout(
			"ins 4 8 8 8,wrap 1,fillx", "[grow,fill]", "[]"));
		p.setBorder(BorderFactory.createTitledBorder("Language"));
		p.add(language);
		return p;
	}

	private Map<String, ReportRenderer> getSelectedRenderers(){
		final Map<String, ReportRenderer> renderers = new LinkedHashMap<>();
		final String baseName = reportType.name()
			.toLowerCase() + "_report";

		if(cbMarkdown.isSelected())
			renderers.put(baseName + ".md", new MarkdownReportRenderer());
		if(cbText.isSelected())
			renderers.put(baseName + ".txt", new TextReportRenderer());
		if(cbHtml.isSelected())
			renderers.put(baseName + ".html", new HtmlReportRenderer());
		if(cbDocx.isSelected())
			renderers.put(baseName + ".docx", new DocxReportRenderer());
		if(cbPdf.isSelected())
			renderers.put(baseName + ".pdf", new PdfReportRenderer());

		return renderers;
	}

	private void onGenerate(){
		if(selectedEntity == null && (reportType == ReportType.ANCESTOR
			|| reportType == ReportType.DESCENDANT
			|| reportType == ReportType.INDIVIDUAL)){
			JOptionPane.showMessageDialog(this,
				"Cannot generate " + reportType.getTitle()
					+ " without a selected individual.",
				"Missing Subject", JOptionPane.WARNING_MESSAGE);

			return;
		}

		final Map<String, ReportRenderer> renderers = getSelectedRenderers();
		if(renderers.isEmpty()){
			JOptionPane.showMessageDialog(this,
				"Please select at least one output format.",
				"No Format Selected", JOptionPane.WARNING_MESSAGE);

			return;
		}

		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Select Output Directory");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if(chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;

		final File selectedDir = chooser.getSelectedFile();
		if(selectedDir == null)
			return;

		final Path outDir = selectedDir.toPath();
		final ReportLanguage selectedLang = (ReportLanguage)language.getSelectedItem();
		final ReportConfig config = optionsPanel.toConfig(selectedLang);

		runWithProgress(outDir, renderers, config);
	}


	/* ======================================================================
	 *                          Background generation
	 * ====================================================================== */

	/** Result of the background work, passed back to {@code done()}. */
	private record GenerationResult(Path directory, List<Path> files){}

	/** A single progress update, published from the worker thread. */
	private record ProgressUpdate(int percent, String message){}


	private void runWithProgress(final Path outDir,
		final Map<String, ReportRenderer> renderers,
		final ReportConfig config){

		final ProgressDialog progressDialog =
			new ProgressDialog(this, reportType.getTitle() + " — generating…");

		final SwingWorker<GenerationResult, ProgressUpdate> worker =
			new SwingWorker<>(){

				@Override
				protected GenerationResult doInBackground() throws Exception{
					final ReportProgressListener listener = (percent, message) -> {
						if(isCancelled())
							throw new RuntimeException("Report generation cancelled");
						publish(new ProgressUpdate(percent, message));
					};

					// ----- Phase 1: build the document (0..50%) ---------
					listener.onProgress(0, "Building report…");
					final ReportProgressListener documentListener = scale(listener, 0, 50);
					final ReportDocument doc = buildReportDocument(config, documentListener);

					// ----- Phase 2: render the files (50..100%) ---------
					final List<Path> generatedFiles = new ArrayList<>();
					final int n = renderers.size();
					int i = 0;
					for(final Map.Entry<String, ReportRenderer> entry : renderers.entrySet()){
						if(isCancelled())
							throw new RuntimeException("Report generation cancelled");

						final Path targetFile = outDir.resolve(entry.getKey());
						entry.getValue()
							.render(doc, targetFile);
						generatedFiles.add(targetFile);

						i ++;
						listener.onProgress(50 + (i * 50) / n,
							"Writing " + entry.getKey());
					}
					listener.onProgress(100, "Done");
					return new GenerationResult(outDir, generatedFiles);
				}


				@Override
				protected void process(final List<ProgressUpdate> chunks){
					// Only the last chunk matters for a progress bar.
					final ProgressUpdate last = chunks.getLast();
					progressDialog.update(last.percent(), last.message());
				}


				@Override
				protected void done(){
					progressDialog.dispose();

					if(isCancelled()){
						JOptionPane.showMessageDialog(ReportDialog.this,
							"Report generation was cancelled.",
							"Cancelled", JOptionPane.INFORMATION_MESSAGE);
						return;
					}

					try{
						final GenerationResult result = get();
						promptOpenFolder(result.directory(), result.files());
						dispose();
					}
					catch(final Exception ex){
						final Throwable cause = (ex.getCause() != null? ex.getCause(): ex);
						JOptionPane.showMessageDialog(ReportDialog.this,
							"Error while generating report:"
								+ StringUtils.LF + cause.getMessage(),
							I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
					}
				}


				/**
				 * Wraps {@code delegate} so that a progress range
				 * {@code [0, 100]} is remapped onto {@code [from, to]}.
				 */
				private ReportProgressListener scale(final ReportProgressListener delegate,
					final int from, final int to){
					return (percent, message) -> {
						final int scaled = from + (percent * (to - from)) / 100;
						delegate.onProgress(scaled, message);
					};
				}
			};

		// If the user cancels, propagate the cancellation to the worker.
		progressDialog.addWindowListener(new java.awt.event.WindowAdapter(){
			@Override
			public void windowClosed(final java.awt.event.WindowEvent e){
				if(progressDialog.isCancelled())
					worker.cancel(true);
			}
		});

		worker.execute();
		progressDialog.setVisible(true);
	}


	/* ======================================================================
	 *                          Report document assembly
	 * ====================================================================== */

	/**
	 * Assembles the {@link ReportDocument} for the current
	 * {@link ReportType}.
	 *
	 * <p>The report type drives two orthogonal decisions:</p>
	 * <ol>
	 *   <li><b>which root record</b> to feed to the generator — resolved by
	 *       {@link #resolveRootFor(ReportType)};</li>
	 *   <li><b>which sections</b> to enable — resolved by
	 *       {@link #tailorConfigFor(ReportType, ReportConfig)}, which starts
	 *       from the user's {@link ReportConfig} and toggles the narrative
	 *       flags that do not make sense for the chosen perspective.</li>
	 * </ol>
	 *
	 * <p>The generator itself (see {@link ReportGenerator}) dispatches by
	 * root kind, so passing a group, a source, a place, etc. as root is
	 * already supported without any change here.</p>
	 *
	 * @param userConfig the configuration selected by the user in the dialog
	 * @return the assembled document, never {@code null}
	 */
	private ReportDocument buildReportDocument(final ReportConfig userConfig, final ReportProgressListener listener){
		if(selectedEntity == null)
			return placeholderDocument(reportType);

		final ReportConfig tailored = tailorConfigFor(reportType, userConfig);

		return switch(reportType){
			case INDIVIDUAL, GROUP, RELATIONSHIP,
				  BIBLIOGRAPHY, RESEARCH_PROGRESS ->
				new ReportGenerator(context.model(), selectedEntity, tailored)
					.generate(listener);

			case ANCESTOR ->
				new AncestorReportGenerator(context.model(), selectedEntity, tailored)
					.generate(listener);

			case DESCENDANT ->
				new DescendantReportGenerator(context.model(), selectedEntity, tailored)
					.generate(listener);
		};
	}


	/* ----- Root resolution ------------------------------------------------- */

	/**
	 * Chooses the record to use as root for the report generator.
	 *
	 * <p>Individual-scoped and group reports use the entity selected in the
	 * caller's context. Global reports (bibliography, research progress) fall
	 * back to the currently selected individual, or to the first individual
	 * of the model when nothing is selected — this lets them still produce a
	 * document, even though the "true" global scope is not fully expressed
	 * by the current single-root architecture.</p>
	 */
	private FLEFRecord resolveRootFor(final ReportType type){
		return switch(type){
			case INDIVIDUAL, ANCESTOR, DESCENDANT, RELATIONSHIP -> selectedEntity;
			case GROUP -> selectedEntity;
			case BIBLIOGRAPHY, RESEARCH_PROGRESS -> firstAvailableIndividual();
		};
	}

	/**
	 * Fallback used by global reports when no individual is selected:
	 * returns the first individual of the model, or {@code null} when the
	 * model contains no individuals at all.
	 */
	private FLEFRecord firstAvailableIndividual(){
		if(selectedEntity != null)
			return selectedEntity;
		final FLEFModel model = context.model();
		final var individuals = model.getRecordsByType(IndividualHandler.TYPE);
		for(final FLEFRecord r : individuals)
			return r;
		return null;
	}


	/* ----- Config tailoring ----------------------------------------------- */

	/**
	 * Adapts the user's {@link ReportConfig} to the perspective requested
	 * by the report type.
	 *
	 * <p>Only the <em>narrative</em> flags are toggled: privacy, audit,
	 * sources, evidence, media, cultural norms, groups, indexes and style
	 * are left exactly as the user chose them, because they are
	 * perspective-independent.</p>
	 *
	 * <ul>
	 *   <li><b>INDIVIDUAL</b> — user config unchanged.</li>
	 *   <li><b>GROUP</b> — user config unchanged; the group sections are
	 *       selected automatically by the generator's dispatch.</li>
	 *   <li><b>ANCESTOR</b> — ancestry and introduction only; descendants
	 *       and relations disabled.</li>
	 *   <li><b>DESCENDANT</b> — descendants, direct relations and
	 *       introduction; ancestry disabled.</li>
	 *   <li><b>RELATIONSHIP</b> — direct and indirect relations; ancestry
	 *       and descendants disabled.</li>
	 *   <li><b>BIBLIOGRAPHY</b> — sources, media, repositories and evidence;
	 *       everything else minimized.</li>
	 *   <li><b>RESEARCH_PROGRESS</b> — context and research, evidence,
	 *       coverage report; narrative sections disabled.</li>
	 * </ul>
	 */
	private static ReportConfig tailorConfigFor(final ReportType type, final ReportConfig base){
		return switch(type){
			case INDIVIDUAL, GROUP, ANCESTOR, DESCENDANT, RELATIONSHIP -> base;
			case BIBLIOGRAPHY -> withSourcesFocus(base);
			case RESEARCH_PROGRESS -> withResearchFocus(base);
		};
	}


	/* ----- Config copy helpers -------------------------------------------- */

	/**
	 * Returns a copy of {@code base} with the six narrative flags replaced.
	 * Everything else — including {@code collateralScope} — is preserved
	 * verbatim, because it is the user's choice for this report.
	 */
	private static ReportConfig withNarrative(final ReportConfig base,
		final boolean introduction,
		final boolean paternal, final boolean maternal,
		final boolean descendants,
		final boolean direct, final boolean indirect){
		return new ReportConfig(
			base.language(),
			base.privacyPolicy(),
			base.showPrivacyDetails(),
			base.audit(),
			base.header(),
			introduction,
			paternal,
			maternal,
			descendants,
			direct,
			indirect,
			base.collateralScope(),          // <-- preservato
			base.notes(),
			base.sources(),
			base.media(),
			base.evidence(),
			base.contextAndResearch(),
			base.culturalNorms(),
			base.groups(),
			base.groupMembers(),
			base.groupSubgroups(),
			base.groupAttributes(),
			base.historicEvents(),
			base.repositories(),
			base.eventFullParticipants(),
			base.dateProvenance(),
			base.coverageReport(),
			base.timeline(),
			base.indexIndividuals(),
			base.indexPlaces(),
			base.includeDescriptions(),
			base.includePictures());
	}


	/**
	 * Config for a bibliography report: sources, media, repositories and
	 * evidence in focus, with every narrative section disabled.
	 * {@code collateralScope} is forced to {@link CollateralScope#NONE}
	 * because a bibliography does not follow a direct line.
	 */
	private static ReportConfig withSourcesFocus(final ReportConfig base){
		return new ReportConfig(
			base.language(),
			base.privacyPolicy(),
			base.showPrivacyDetails(),
			base.audit(),
			base.header(),
			/*introduction*/          false,
			/*paternalAncestry*/      false,
			/*maternalAncestry*/      false,
			/*descendants*/           false,
			/*directRelations*/       false,
			/*indirectRelations*/     false,
			/*collateralScope*/       CollateralScope.NONE,
			/*notes*/                 base.notes(),
			/*sources*/               true,
			/*media*/                 base.media(),
			/*evidence*/              true,
			/*contextAndResearch*/    false,
			/*culturalNormIds*/         false,
			/*groups*/                false,
			/*groupMembers*/          false,
			/*groupSubgroups*/        false,
			/*groupAttributes*/       false,
			/*historicEvents*/        false,
			/*repositories*/          true,
			/*eventFullParticipants*/ false,
			/*dateProvenance*/        true,
			/*coverageReport*/        base.coverageReport(),
			/*timeline*/              false,
			/*indexIndividuals*/      false,
			/*indexPlaces*/           true,
			base.includeDescriptions(),
			base.includePictures());
	}


	/**
	 * Config for a research-progress report: research questions, context
	 * impacts, evidence and coverage in focus; narrative sections disabled.
	 * {@code collateralScope} is forced to {@link CollateralScope#NONE}
	 * because a research-progress report does not follow a direct line.
	 */
	private static ReportConfig withResearchFocus(final ReportConfig base){
		return new ReportConfig(
			base.language(),
			base.privacyPolicy(),
			base.showPrivacyDetails(),
			base.audit(),
			base.header(),
			/*introduction*/          true,
			/*paternalAncestry*/      false,
			/*maternalAncestry*/      false,
			/*descendants*/           false,
			/*directRelations*/       false,
			/*indirectRelations*/     false,
			/*collateralScope*/       CollateralScope.NONE,
			/*notes*/                 base.notes(),
			/*sources*/               true,
			/*media*/                 false,
			/*evidence*/              true,
			/*contextAndResearch*/    true,
			/*culturalNormIds*/         base.culturalNorms(),
			/*groups*/                false,
			/*groupMembers*/          false,
			/*groupSubgroups*/        false,
			/*groupAttributes*/       false,
			/*historicEvents*/        true,
			/*repositories*/          true,
			/*eventFullParticipants*/ false,
			/*dateProvenance*/        true,
			/*coverageReport*/        true,
			/*timeline*/              base.timeline(),
			/*indexIndividuals*/      true,
			/*indexPlaces*/           false,
			base.includeDescriptions(),
			base.includePictures());
	}


	/* ----- Placeholder ----------------------------------------------------- */

	/**
	 * Builds a minimal document explaining why the report could not be
	 * produced, used when no root record can be resolved (e.g. an empty
	 * model). The dialog still writes the file so the user is not left
	 * without feedback.
	 */
	private static ReportDocument placeholderDocument(final ReportType type){
		final List<ReportSection> sections = new ArrayList<>();
		sections.add(new ReportSection.Heading(1, type.getTitle()));
		sections.add(new ReportSection.Paragraph(
			"No target record is available for this report. "
				+ "Select an individual or a group in the editor and try again."));
		return new ReportDocument(type.getTitle(), null, sections);
	}


	/* ======================================================================
	 *                          Post-generation prompt
	 * ====================================================================== */

	private void promptOpenFolder(final Path dir, final List<Path> files){
		final Object[] dialogOptions = {"OK", "Open Directory"};
		final StringBuilder msg = new StringBuilder("Report successfully written to:")
			.append(StringUtils.LF);
		for(final Path f : files)
			msg.append(" • ")
				.append(f.getFileName())
				.append(StringUtils.LF);

		final int choice = JOptionPane.showOptionDialog(this,
			msg.toString(),
			"Report Complete",
			JOptionPane.DEFAULT_OPTION,
			JOptionPane.INFORMATION_MESSAGE,
			null,
			dialogOptions,
			dialogOptions[0]);

		if(choice == 1 && Desktop.isDesktopSupported()){
			try{
				Desktop.getDesktop()
					.open(dir.toFile());
			}
			catch(final Exception ex){
				JOptionPane.showMessageDialog(this,
					"Could not open directory:" + StringUtils.LF + ex.getMessage(),
					I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
			}
		}
	}

}
