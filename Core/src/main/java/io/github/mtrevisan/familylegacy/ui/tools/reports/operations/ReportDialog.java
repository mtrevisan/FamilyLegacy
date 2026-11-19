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
import io.github.mtrevisan.familylegacy.ui.bindings.ComboBoxSearch;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.dialogs.help.ComponentsDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.SavedFileNotice;
import io.github.mtrevisan.familylegacy.ui.tools.reports.ProgressDialog;
import io.github.mtrevisan.familylegacy.ui.tools.reports.ReportLayoutPanel;
import io.github.mtrevisan.familylegacy.ui.tools.reports.ReportOptionsPanel;
import io.github.mtrevisan.familylegacy.ui.tools.reports.ReportPresetsPanel;
import io.github.mtrevisan.familylegacy.util.PluginLoader;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportLayout;
import io.github.mtrevisan.reports.ReportRenderer;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Swing dialog for configuring and executing report generation for a given
 * {@link ReportType}.
 *
 * <p>The dialog exposes:</p>
 * <ul>
 *   <li>a header line showing the root subject;</li>
 *   <li>a language selector;</li>
 *   <li>the full {@link ReportOptionsPanel};</li>
 *   <li>Restore Defaults / Generate buttons: Generate asks where to save the report and in which format (the available renderers).</li>
 * </ul>
 *
 * <p>The {@link ReportType} chosen by the caller drives both the resolution
 * of the root record and the {@link ReportConfig} tailoring performed by
 * {@link #buildReportDocument(ReportConfig, ReportProgressListener)}. Section-level filtering is
 * delegated to {@link ReportGenerator}, which dispatches by root kind.</p>
 */
public final class ReportDialog extends JDialog{

	/** The longest part of the name of a file that comes from the subject of the report. */
	private static final int MAX_SUBJECT_NAME = 60;

	private final ToolContext context;
	private final ReportType reportType;
	private final ReportOptionsPanel optionsPanel;
	private final ReportPresetsPanel presetsPanel;
	private final ReportLayoutPanel layoutPanel;
	private FLEFRecord selectedEntity;

	private final JComboBox<ReportLanguage> language = ComboBoxSearch.searchable(new JComboBox<>(ReportLanguage.available().toArray(ReportLanguage[]::new)));
	/** The font the user chose the last time ({@code null} for the automatic one): it is offered again when the report needs one. */
	private String rememberedFont;
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
		language.setSelectedItem(ReportLanguage.orEnglish(preferences.language()));
		rememberedFont = preferences.font();
		this.layoutPanel = new ReportLayoutPanel(preferences.layout());
		this.presetsPanel = new ReportPresetsPanel(preferences, reportType, this::applyPreset, this::savePreset, () -> this);

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
			case ANCESTOR, DESCENDANT, INDIVIDUAL, RELATIONSHIP, FAMILY_GROUP_SHEET -> {
				if(selectedId != null && selectedId.startsWith(IndividualHandler.ID_PREFIX))
					selectedEntity = context.model()
						.getRecordById(selectedId);
			}
			case PLACE, SOURCE, RESEARCH_QUESTION -> selectedEntity = subject;
			case FULL_BIBLIOGRAPHY, FAMILY_BOOK, TO_VERIFY, SURNAMES -> selectedEntity = null;
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
			"ins 10,wrap 1,fillx", "[grow,fill]", "[]10[]5[]10[]"));

		// 1. Header panel displaying report type and targeted root entity
		String targetText = I18N.t(reportType.isGlobal()? "text.report.whole_file": "text.report.no_subject");
		if(selectedEntity != null){
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(selectedEntity.getTag());
			targetText = handler.getDisplayText(selectedEntity, context.model());
		}
		final JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		headerPanel.add(new JLabel("<html><b>" + I18N.l("label.target_subject") + "</b> " + escapeHtml(targetText) + "</html>"));
		mainPanel.add(headerPanel);

		// 2. The extra fonts of the PDF, to download when they are missing
		mainPanel.add(createFontPackPanel());

		// 3. Language selection panel
		mainPanel.add(createLanguageSelectionPanel());

		// 4. The saved sets of options
		mainPanel.add(presetsPanel);

		// 5. Options configuration panel
		mainPanel.add(optionsPanel);

		// 6. The layout of the pages (PDF and Word)
		mainPanel.add(layoutPanel);

		final JScrollPane scrollPane = new JScrollPane(mainPanel);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar()
			.setUnitIncrement(GUIHelper.SCROLLBAR_UNIT_INCREMENT);

		// Bottom button panel
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
		final JButton restoreBtn = new JButton(I18N.t("button.report.restore_defaults"));
		final JButton generateBtn = new JButton(I18N.t("button.generate"));

		restoreBtn.addActionListener(e -> restoreDefaults());
		generateBtn.addActionListener(e -> onGenerate());

		buttons.add(restoreBtn);
		buttons.add(generateBtn);

		add(scrollPane, BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);

		setPreferredSize(new Dimension(580, 680));
	}

	/**
	 * The note that the extra fonts of the PDF (the scripts that the fonts of the system may lack) are not there, with the button that downloads
	 * them; hidden when they are there, or when no format needs them. The PDF is written without them anyway.
	 */
	private JPanel createFontPackPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 0 8 0 8,fillx", "[grow,fill][]", "[]"));
		final boolean needed = (PluginLoader.fontsDirectory() != null && !ComponentsDialog.hasFonts()
			&& ReportRenderers.available().stream().anyMatch(ReportRenderer::choosesFont));
		panel.setVisible(needed);
		if(!needed)
			return panel;

		final JLabel note = new JLabel("<html>" + escapeHtml(I18N.t("text.report.font_pack_missing")) + "</html>");
		final JButton download = new JButton(I18N.t("button.report.download_fonts"));
		download.setToolTipText(I18N.t("tooltip.report.download_fonts"));
		download.addActionListener(e -> {
			ComponentsDialog.show(this);
			panel.setVisible(!ComponentsDialog.hasFonts());
		});
		panel.add(note, "growx");
		panel.add(download);
		return panel;
	}

	private JPanel createLanguageSelectionPanel(){
		final JPanel p = new JPanel(new MigLayout(
			"ins 4 8 8 8,wrap 1,fillx", "[grow,fill]", "[]"));
		p.setBorder(BorderFactory.createTitledBorder(I18N.t("border.language")));
		p.add(language);
		return p;
	}

	/** @return	The options the user can choose in this report (the ones a saved set keeps). */
	private List<ReportOption> editableOptions(){
		return Arrays.stream(ReportOption.values())
			.filter(option -> ReportBuilder.optionState(reportType, selectedEntity, option) == OptionState.EDITABLE)
			.toList();
	}

	/** Puts the options of a saved set in the dialog (what the set does not keep, the privacy policy for one, stays as it is). */
	private void applyPreset(final String name){
		final ReportConfig current = optionsPanel.toConfig((ReportLanguage)language.getSelectedItem());
		optionsPanel.reset(preferences.preset(reportType, name, current));
	}

	/** Saves the options as they are in the dialog under the name. */
	private void savePreset(final String name){
		preferences.savePreset(reportType, name, optionsPanel.toConfig((ReportLanguage)language.getSelectedItem()), editableOptions());
	}

	/**
	 * @return	The name the file of the report is proposed with: the subject and the type of the report ({@code Antonia_Pastrelo_I1430-ancestor_report}),
	 * 	only the type for a report of the whole file.
	 */
	private String suggestedBaseName(){
		final String type = reportType.name().toLowerCase(Locale.ROOT) + "_report";
		if(selectedEntity == null)
			return type;

		final String subject = HandlerRegistry.getHandler(selectedEntity.getTag()).getDisplayText(selectedEntity, context.model());
		final StringBuilder name = new StringBuilder();
		for(int i = 0; i < subject.length() && name.length() < MAX_SUBJECT_NAME; i ++){
			final char c = subject.charAt(i);
			if(Character.isLetterOrDigit(c))
				name.append(c);
			else if(!name.isEmpty() && name.charAt(name.length() - 1) != '_')
				//what is not a letter or a digit (blanks, brackets, punctuation) is a single underscore: no character that a file name refuses
				name.append('_');
		}
		final String clean = StringUtils.strip(name.toString(), "_");
		return (clean.isEmpty()? type: clean + "-" + type);
	}

	/** @return	The label of the format in the dialog that asks where to save the report. */
	private static String formatLabel(final ReportRenderer renderer){
		return I18N.t("checkbox.format_" + renderer.id(), renderer.displayName());
	}

	/**
	 * Asks where to save the report and in which format: the formats offered are the ones of the available renderers, so a format whose plugin
	 * is missing is not listed.
	 *
	 * @return	The chosen file (or directory, for a format that is one) with the renderer that writes it, {@code null} if the user gave up.
	 */
	private Map.Entry<Path, ReportRenderer> askTarget(){
		final List<ReportRenderer> renderers = ReportRenderers.available();
		if(renderers.isEmpty()){
			MessageDialogs.show(this,
				I18N.t("dialog.report.no_format.message"), I18N.t("dialog.report.no_format.title"), JOptionPane.WARNING_MESSAGE);

			return null;
		}

		final Map<FileFilter, ReportRenderer> filters = new LinkedHashMap<>();
		for(final ReportRenderer renderer : renderers)
			filters.put(new RendererFilter(renderer), renderer);

		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(I18N.t("dialog.report.save.title"));
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		filters.keySet()
			.forEach(chooser::addChoosableFileFilter);
		final String baseName = suggestedBaseName();

		//the format of the last time if it is still available, else the default one, else the first
		final List<String> remembered = preferences.formats();
		final String preferred = (!remembered.isEmpty()? remembered.getFirst(): ReportRenderers.DEFAULT_ID);
		FileFilter initial = filters.keySet().iterator().next();
		for(final Map.Entry<FileFilter, ReportRenderer> entry : filters.entrySet())
			if(entry.getValue().id().equals(preferred)){
				initial = entry.getKey();
				break;
			}
		chooser.setFileFilter(initial);

		final String lastDirectory = preferences.directory();
		final File directory = (lastDirectory != null? new File(lastDirectory): null);
		if(directory != null && directory.isDirectory())
			chooser.setCurrentDirectory(directory);
		chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), baseName + filters.get(initial).extension()));
		//changing the format changes the suggested name too
		chooser.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, event -> {
			final ReportRenderer renderer = filters.get(chooser.getFileFilter());
			if(renderer == null)
				return;

			final File current = chooser.getSelectedFile();
			final String name = (current != null? current.getName(): baseName);
			chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), withExtension(name, renderers, renderer)));
		});

		if(chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null)
			return null;

		final ReportRenderer renderer = filters.get(chooser.getFileFilter());
		final File chosen = chooser.getSelectedFile();
		return Map.entry(new File(chosen.getParentFile(), withExtension(chosen.getName(), renderers, renderer)).toPath(), renderer);
	}

	/** @return	The name with the extension of the renderer instead of the one of another format (added if there is none). */
	private static String withExtension(final String name, final List<ReportRenderer> renderers, final ReportRenderer renderer){
		String base = name;
		for(final ReportRenderer candidate : renderers)
			if(base.endsWith(candidate.extension())){
				base = base.substring(0, base.length() - candidate.extension().length());
				break;
			}
		return base + renderer.extension();
	}

	/** Shows the files of a format (or the directories, for the format that is one). */
	private static final class RendererFilter extends FileFilter{

		private final ReportRenderer renderer;


		private RendererFilter(final ReportRenderer renderer){
			this.renderer = renderer;
		}

		@Override
		public boolean accept(final File file){
			return (file.isDirectory() || !renderer.isDirectory() && file.getName().endsWith(renderer.extension()));
		}

		@Override
		public String getDescription(){
			return formatLabel(renderer);
		}

	}

	private void onGenerate(){
		//every report is about a record: without one nothing is written (it would be a file with a note and nothing else)
		if(selectedEntity == null && !reportType.isGlobal()){
			MessageDialogs.show(this,
				I18N.tf("dialog.report.missing_subject.message", reportType.getTitle()), I18N.t("dialog.report.missing_subject.title"), JOptionPane.WARNING_MESSAGE);

			return;
		}

		final Map.Entry<Path, ReportRenderer> target = askTarget();
		if(target == null)
			return;

		final Path outDir = target.getKey().getParent();
		final Map<String, ReportRenderer> renderers = new LinkedHashMap<>();
		renderers.put(target.getKey().getFileName().toString(), target.getValue());
		if(!confirmOverwrite(outDir, renderers))
			return;

		final ReportLanguage selectedLang = (ReportLanguage)language.getSelectedItem();
		final ReportConfig config = optionsPanel.toConfig(selectedLang);
		final ReportLayout layout = layoutPanel.toLayout();

		remember(outDir, renderers, selectedLang, config, layout);
		runWithProgress(outDir, renderers, config, layout);
	}

	/** Keeps what the user chose, to offer it again the next time (not the privacy policy: see {@link ReportPreferences}). */
	private void remember(final Path outDir, final Map<String, ReportRenderer> renderers, final ReportLanguage selectedLang,
			final ReportConfig config, final ReportLayout layout){
		preferences.setDirectory(outDir.toString());
		preferences.setLayout(layout);
		preferences.setFormats(renderers.values().stream()
			.map(ReportRenderer::id)
			.toList());
		preferences.setLanguage(selectedLang);
		preferences.setOptions(reportType, config, editableOptions());
	}

	/** Back to the options, the language and the formats a report starts with when nothing was chosen yet. */
	private void restoreDefaults(){
		optionsPanel.reset(ReportConfig.defaults());
		layoutPanel.reset(ReportLayout.DEFAULT);
		language.setSelectedItem(ReportLanguage.orEnglish(preferences.defaultLanguage()));
		rememberedFont = null;
		preferences.setFont(null);
		preferences.setFormats(List.of());
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
		final ReportConfig config,
		final ReportLayout layout){

		final ProgressDialog progressDialog =
			new ProgressDialog(this, I18N.tf("dialog.report.generating", reportType.getTitle()));

		final SwingWorker<GenerationResult, ProgressUpdate> worker =
			new SwingWorker<>(){

				/** The files this run writes (the one being written too), to be removed if the run is canceled: a report that is not complete is not a report. */
				private final List<Path> written = new CopyOnWriteArrayList<>();


				@Override
				protected GenerationResult doInBackground() throws Exception{
					try{
						final GenerationResult result = generate();
						if(isCancelled())
							deleteWritten();
						return result;
					}
					catch(final Exception e){
						if(isCancelled())
							deleteWritten();
						throw e;
					}
				}

				private void deleteWritten(){
					for(final Path file : written)
						try{
							Files.deleteIfExists(file);
						}
						catch(final Exception ignored){
							//a file that cannot be removed stays: nothing else can be done
						}
				}

				/** @return	The font the user chose for the formats that need one ({@code null} for the automatic one, or when no format needs one). */
				private String askFont(final Map<String, ReportRenderer> renderers, final ReportDocument doc) throws Exception{
					final List<String> fonts = new ArrayList<>();
					for(final ReportRenderer renderer : renderers.values())
						if(renderer.choosesFont())
							for(final String candidate : renderer.fonts(doc))
								if(!fonts.contains(candidate))
									fonts.add(candidate);
					//without a font that has all the characters there is nothing to choose: the automatic one takes them from several fonts
					if(fonts.isEmpty())
						return null;

					final String automatic = I18N.t("text.report.font_automatic");
					final List<String> items = new ArrayList<>(fonts.size() + 1);
					items.add(automatic);
					items.addAll(fonts);
					final String[] chosen = new String[1];
					SwingUtilities.invokeAndWait(() -> {
						Object initial = automatic;
						for(final String candidate : fonts)
							if(candidate.equalsIgnoreCase(rememberedFont))
								initial = candidate;
						final Object answer = JOptionPane.showInputDialog(progressDialog, I18N.t("dialog.report.font.message"),
							I18N.t("dialog.report.font.title"), JOptionPane.QUESTION_MESSAGE, null, items.toArray(), initial);
						chosen[0] = (String)answer;
					});
					if(chosen[0] == null){
						cancel(true);
						throw new RuntimeException("Report generation cancelled");
					}

					final String font = (automatic.equals(chosen[0])? null: chosen[0]);
					rememberedFont = font;
					preferences.setFont(font);
					return font;
				}

				private GenerationResult generate() throws Exception{
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
					//the font is asked once, when the document is known: the list has only the fonts that have all its characters
					final String font = askFont(renderers, doc);
					final List<Path> generatedFiles = new ArrayList<>();
					final int n = renderers.size();
					int i = 0;
					for(final Map.Entry<String, ReportRenderer> entry : renderers.entrySet()){
						if(isCancelled())
							throw new RuntimeException("Report generation cancelled");

						final Path targetFile = outDir.resolve(entry.getKey());
						written.add(targetFile);
						entry.getValue()
							.render(doc, targetFile, (entry.getValue().choosesFont()? font: null), layout);
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
						MessageDialogs.show(ReportDialog.this,
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
						MessageDialogs.show(ReportDialog.this,
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

		// If the user cancels, the worker is told at once: the dialog stays open until the worker is over, so the closing of the
		// dialog would be too late (the report would be written, and the user asked where to find it)
		progressDialog.onCancel(() -> worker.cancel(true));

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
		//only the action: Esc (or closing the window) dismisses the message, an Ok button would add nothing
		final Object[] dialogOptions = {I18N.t("button.open.directory")};
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

		//the directory is opened with the first file of the report selected, if the system can do it
		if(choice == 0 && !SavedFileNotice.reveal((files.isEmpty()? dir: files.getFirst()).toFile()))
			MessageDialogs.show(this, I18N.l("dialog.report.open_directory.error") + StringUtils.LF + dir,
				I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
	}

}
