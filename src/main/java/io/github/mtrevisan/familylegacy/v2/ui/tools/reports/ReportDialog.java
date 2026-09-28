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
package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.renderers.DocxReportRenderer;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.renderers.HtmlReportRenderer;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.renderers.MarkdownReportRenderer;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.renderers.PdfReportRenderer;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.renderers.ReportRenderer;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.renderers.TextReportRenderer;
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
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Swing dialog for configuring and executing report generation for a given {@link ReportType}.
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
	 * Resolves the root target record according to the required {@link ReportType}.
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
				// Global reports: fallback to active individual or first record in model
				selectedEntity = context.getSelectedIndividual();
			}
		}
	}

	private void initComponents(){
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout(0, 0));

		final JPanel mainPanel = new JPanel(new MigLayout("ins 10,wrap 1,fillx", "[grow,fill]", "[]10[]20[]10[]"));

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
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		// Bottom button panel
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
		final JButton cancelBtn = new JButton("Cancel");
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

		cbMarkdown = new JCheckBox("Markdown (.md)", true);
		cbText = new JCheckBox("Plain Text (.txt)", false);
		cbHtml = new JCheckBox("HTML (.html)", false);
		cbDocx = new JCheckBox("Word (.docx)", false);
		cbPdf = new JCheckBox("PDF (.pdf)", false);

		panel.add(cbMarkdown);
		panel.add(cbText);
		panel.add(cbHtml);
		panel.add(cbDocx);
		panel.add(cbPdf);

		return panel;
	}

	private JPanel createLanguageSelectionPanel(){
		final JPanel p = new JPanel(new MigLayout("ins 4 8 8 8,wrap 1,fillx", "[grow,fill]", "[]"));
		p.setBorder(BorderFactory.createTitledBorder("Language"));
		p.add(language);
		return p;
	}

	private Map<String, ReportRenderer> getSelectedRenderers(){
		final Map<String, ReportRenderer> renderers = new LinkedHashMap<>();
		final String baseName = reportType.name().toLowerCase() + "_report";

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
		if(selectedEntity == null && (reportType == ReportType.ANCESTOR || reportType == ReportType.DESCENDANT
				|| reportType == ReportType.INDIVIDUAL)){
			JOptionPane.showMessageDialog(this,
				"Cannot generate " + reportType.getTitle() + " without a selected individual.",
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

		try{
			final ReportDocument doc = buildReportDocument(config);

			final List<Path> generatedFiles = new ArrayList<>();
			for(final Map.Entry<String, ReportRenderer> entry : renderers.entrySet()){
				final Path targetFile = outDir.resolve(entry.getKey());
				entry.getValue()
					.render(doc, targetFile);
				generatedFiles.add(targetFile);
			}

			promptOpenFolder(outDir, generatedFiles);

			dispose();
		}
		catch(final IOException ex){
			JOptionPane.showMessageDialog(this,
				"Error while generating report:" + StringUtils.LF + ex.getMessage(),
				"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private ReportDocument buildReportDocument(final ReportConfig config){
		ReportGenerator generator = switch (reportType) {
			case INDIVIDUAL, GROUP -> new ReportGenerator(context.model(), selectedEntity, config);
			// TODO
//			case ANCESTOR -> new AncestorReportGenerator(context.model(), selectedEntity, config);
//			case DESCENDANT -> new DescendantReportGenerator(context.model(), selectedEntity, config);
//			case RESEARCH_PROGRESS -> new ResearchProgressReportGenerator(context.model(), selectedEntity, config);
//			case BIBLIOGRAPHY -> new BibliographyReportGenerator(context.model(), config);
//			case RELATIONSHIP -> new RelationshipReportGenerator(context.model(), selectedEntity, config);
			default -> null;
		};
		return generator.generate();
	}

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
				Desktop.getDesktop().open(dir.toFile());
			}
			catch(final Exception ex){
				JOptionPane.showMessageDialog(this,
					"Could not open directory:" + StringUtils.LF + ex.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

}
