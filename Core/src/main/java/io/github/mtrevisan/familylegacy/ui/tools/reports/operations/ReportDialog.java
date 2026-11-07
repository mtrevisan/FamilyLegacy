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
package io.github.mtrevisan.familylegacy.ui.tools.reports.operations;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.reports.OptionState;
import io.github.mtrevisan.familylegacy.reports.ReportBuilder;
import io.github.mtrevisan.familylegacy.reports.ReportConfig;
import io.github.mtrevisan.familylegacy.reports.ReportGenerator;
import io.github.mtrevisan.familylegacy.reports.ReportOption;
import io.github.mtrevisan.familylegacy.reports.ReportPreferences;
import io.github.mtrevisan.familylegacy.reports.ReportProgressListener;
import io.github.mtrevisan.familylegacy.reports.ReportType;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.familylegacy.reports.renderers.ReportRenderers;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.tools.reports.ProgressDialog;
import io.github.mtrevisan.familylegacy.ui.tools.reports.ReportOptionsPanel;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportRenderer;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


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

	private final Map<ReportRenderer, JCheckBox> formatBoxes = new LinkedHashMap<>();

	private final JComboBox<ReportLanguage> language = new JComboBox<>(ReportLanguage.values());
	/** What the user chose the last time: the language, the formats, the options and the folder. */
	private final ReportPreferences preferences = ReportPreferences.application();


	/** The record the report is about when it is not the selected one (the place, source or question the user picked). */
	private final FLEFRecord subject;


	private ReportDialog(final ToolContext context, final ReportType reportType, final FLEFRecord subject){
		super(context.owner(), reportType.getTitle(), ModalityType.APPLICATION_MODAL);

		ToolDialogs.installEscapeToClose(this);

		this.context = context;
		this.reportType = reportType;
		this.subject = subject;

		resolveSelectedEntity();

		//the options the report does not use are not shown, the ones it always applies cannot be changed; they start from what the user
		//chose the last time
		this.optionsPanel = new ReportOptionsPanel(preferences.options(reportType, ReportConfig.defaults()),
			option -> ReportBuilder.optionState(reportType, selectedEntity, option));
		language.setSelectedItem(preferences.language());

		initComponents();
	}

	public static void showDialog(final ToolContext context, final ReportType reportType){
		showDialog(context, reportType, null);
	}

	/**
	 * @param context	The context.
	 * @param reportType	The type of the report.
	 * @param subject	The record the report is about, for the reports about a place, a source or a research question (that are not
	 * 	selected in the views); {@code null} for the others, that are about the selected record.
	 */
	public static void showDialog(final ToolContext context, final ReportType reportType, final FLEFRecord subject){
		SwingUtilities.invokeLater(() -> {
			final ReportDialog dialog = new ReportDialog(context, reportType, subject);

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
			case PLACE, SOURCE, RESEARCH_QUESTION -> selectedEntity = subject;
			case FULL_BIBLIOGRAPHY, FAMILY_BOOK -> selectedEntity = null;
			case BIBLIOGRAPHY, RESEARCH_PROGRESS -> {
				// Global reports: fallback to active individual or first
				// record in the model.
				selectedEntity = context.getSelectedEntity();
			}
		}
	}

	private void initComponents(){
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout(0, 0));

		final JPanel mainPanel = new JPanel(new MigLayout(
			"ins 10,wrap 1,fillx", "[grow,fill]", "[]10[]20[]10[]"));

		// 1. Header panel displaying report type and targeted root entity
		String targetText = I18N.t(reportType.isGlobal()? "text.report.whole_file": "text.report.no_subject");
		if(selectedEntity != null){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(selectedEntity.getTag());
			targetText = handler.getDisplayText(selectedEntity, context.model());
		}
		final JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		headerPanel.add(new JLabel("<html><b>" + I18N.l("label.target_subject") + "</b> " + escapeHtml(targetText) + "</html>"));
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
			.setUnitIncrement(GUIHelper.SCROLLBAR_UNIT_INCREMENT);

		// Bottom button panel
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
		final JButton restoreBtn = new JButton(I18N.t("button.report.restore_defaults"));
		final JButton cancelBtn = new JButton(I18N.t("button.cancel"));
		final JButton generateBtn = new JButton(I18N.t("button.generate"));

		restoreBtn.addActionListener(e -> restoreDefaults());
		cancelBtn.addActionListener(e -> dispose());
		generateBtn.addActionListener(e -> onGenerate());

		buttons.add(restoreBtn);
		buttons.add(cancelBtn);
		buttons.add(generateBtn);

		add(scrollPane, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);

		setPreferredSize(new Dimension(580, 620));
	}

	private JPanel createFormatSelectionPanel(){
		final Map<String, ReportRenderer> available = new LinkedHashMap<>();
		for(final ReportRenderer renderer : ReportRenderers.available())
			available.put(renderer.id(), renderer);
		//the formats the application knows are always listed, the ones whose plugin is missing are disabled; a format
		//that only a plugin knows is listed when the plugin is there
		final List<String> ids = new ArrayList<>(ReportRenderers.KNOWN_IDS);
		available.keySet().stream()
			.filter(id -> !ids.contains(id))
			.forEach(ids::add);

		final List<String> chosen = new ArrayList<>(preferences.formats());
		chosen.retainAll(available.keySet());
		if(chosen.isEmpty())
			chosen.add(ReportRenderers.DEFAULT_ID);

		final JPanel panel = new JPanel(new GridLayout(Math.max(1, (ids.size() + 2) / 3), 3, 8, 4));
		panel.setBorder(BorderFactory.createTitledBorder(I18N.t("border.output_formats")));

		for(final String id : ids){
			final ReportRenderer renderer = available.get(id);
			final String label = I18N.t("checkbox.format_" + id, (renderer != null? renderer.displayName(): id));
			final JCheckBox checkBox = new JCheckBox(label, renderer != null && chosen.contains(id));
			if(renderer != null)
				formatBoxes.put(renderer, checkBox);
			else{
				checkBox.setEnabled(false);
				checkBox.setToolTipText(I18N.tf("tooltip.format_unavailable", ReportRenderers.pluginFileName(id)));
			}
			panel.add(checkBox);
		}

		return panel;
	}

	private JPanel createLanguageSelectionPanel(){
		final JPanel p = new JPanel(new MigLayout(
			"ins 4 8 8 8,wrap 1,fillx", "[grow,fill]", "[]"));
		p.setBorder(BorderFactory.createTitledBorder(I18N.t("border.language")));
		p.add(language);
		return p;
	}

	private Map<String, ReportRenderer> getSelectedRenderers(){
		final Map<String, ReportRenderer> renderers = new LinkedHashMap<>();
		final String baseName = reportType.name()
			.toLowerCase() + "_report";

		for(final Map.Entry<ReportRenderer, JCheckBox> entry : formatBoxes.entrySet())
			if(entry.getValue().isSelected())
				renderers.put(baseName + entry.getKey().extension(), entry.getKey());

		return renderers;
	}

	private void onGenerate(){
		//every report is about a record: without one nothing is written (it would be a file with a note and nothing else)
		if(selectedEntity == null && !reportType.isGlobal()){
			JOptionPane.showMessageDialog(this,
				I18N.tf("dialog.report.missing_subject.message", reportType.getTitle()), I18N.t("dialog.report.missing_subject.title"), JOptionPane.WARNING_MESSAGE);

			return;
		}

		final Map<String, ReportRenderer> renderers = getSelectedRenderers();
		if(renderers.isEmpty()){
			JOptionPane.showMessageDialog(this,
				I18N.t("dialog.report.no_format.message"), I18N.t("dialog.report.no_format.title"), JOptionPane.WARNING_MESSAGE);

			return;
		}

		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(I18N.t("dialog.report.output_directory.title"));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		final String lastDirectory = preferences.directory();
		if(lastDirectory != null && Files.isDirectory(Path.of(lastDirectory)))
			chooser.setSelectedFile(new File(lastDirectory));

		if(chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;

		final File selectedDir = chooser.getSelectedFile();
		if(selectedDir == null)
			return;

		final Path outDir = selectedDir.toPath();
		if(!confirmOverwrite(outDir, renderers))
			return;

		final ReportLanguage selectedLang = (ReportLanguage)language.getSelectedItem();
		final ReportConfig config = optionsPanel.toConfig(selectedLang);

		remember(outDir, renderers, selectedLang, config);
		runWithProgress(outDir, renderers, config);
	}

	/** Keeps what the user chose, to offer it again the next time (not the privacy policy: see {@link ReportPreferences}). */
	private void remember(final Path outDir, final Map<String, ReportRenderer> renderers, final ReportLanguage selectedLang,
			final ReportConfig config){
		preferences.setDirectory(outDir.toString());
		preferences.setFormats(renderers.values().stream()
			.map(ReportRenderer::id)
			.toList());
		preferences.setLanguage(selectedLang);
		preferences.setOptions(reportType, config, Arrays.stream(ReportOption.values())
			.filter(option -> ReportBuilder.optionState(reportType, selectedEntity, option) == OptionState.EDITABLE)
			.toList());
	}

	/** Back to the options, the language and the formats a report starts with when nothing was chosen yet. */
	private void restoreDefaults(){
		optionsPanel.reset(ReportConfig.defaults());
		language.setSelectedItem(preferences.defaultLanguage());
		formatBoxes.forEach((renderer, box) -> box.setSelected(ReportRenderers.DEFAULT_ID.equals(renderer.id())));
	}


	/** Asks before overwriting: the report is written with a fixed name, so a previous one in the same folder would be lost. */
	private boolean confirmOverwrite(final Path outDir, final Map<String, ReportRenderer> renderers){
		final List<String> existing = renderers.keySet().stream()
			.filter(name -> Files.exists(outDir.resolve(name)))
			.toList();
		if(existing.isEmpty())
			return true;

		return (JOptionPane.showConfirmDialog(this,
			I18N.l("dialog.report.overwrite.message") + StringUtils.LF + " \u2022 " + String.join(StringUtils.LF + " \u2022 ", existing),
			I18N.t("dialog.report.overwrite.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION);
	}

	private static String escapeHtml(final String text){
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
			new ProgressDialog(this, I18N.tf("dialog.report.generating", reportType.getTitle()));

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
					listener.onProgress(0, I18N.t("text.report.building"));
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
							I18N.tf("text.report.writing", entry.getKey()));
					}
					listener.onProgress(100, I18N.t("text.report.done"));
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
							I18N.t("dialog.report.cancelled.message"), I18N.t("dialog.report.cancelled.title"), JOptionPane.INFORMATION_MESSAGE);
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
							I18N.l("dialog.report.error.message") + StringUtils.LF + cause.getMessage(),
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
		progressDialog.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosed(final WindowEvent e){
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

	/** Assembles the document of the report, from the options the user chose. */
	private ReportDocument buildReportDocument(final ReportConfig userConfig, final ReportProgressListener listener){
		return ReportBuilder.build(reportType, context.model(), selectedEntity, userConfig, listener);
	}


	/* ======================================================================
	 *                          Post-generation prompt
	 * ====================================================================== */

	private void promptOpenFolder(final Path dir, final List<Path> files){
		final Object[] dialogOptions = {I18N.t("button.ok"), I18N.t("button.open.directory")};
		final StringBuilder msg = new StringBuilder(I18N.l("dialog.report.complete.message"))
			.append(StringUtils.LF);
		for(final Path f : files)
			msg.append(" • ")
				.append(f.getFileName())
				.append(StringUtils.LF);

		final int choice = JOptionPane.showOptionDialog(this,
			msg.toString(),
			I18N.t("dialog.report.complete.title"),
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
					I18N.l("dialog.report.open_directory.error") + StringUtils.LF + ex.getMessage(),
					I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
			}
		}
	}

}
