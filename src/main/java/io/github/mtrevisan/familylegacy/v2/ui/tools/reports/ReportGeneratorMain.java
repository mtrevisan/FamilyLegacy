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

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.renderers.MarkdownReportRenderer;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.renderers.ReportRenderer;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Development / smoke-test entry point for the report subsystem.
 *
 * <p>Two run modes are supported:</p>
 * <ul>
 *   <li><b>CLI</b> — pass {@code --cli} to generate every supported format
 *       without any user interaction. Useful for regression runs and for
 *       inspecting the raw output of the renderers side by side.</li>
 *   <li><b>UI</b> (default) — opens a window hosting {@link ReportOptionsPanel}
 *       and a "Generate…" button. Once the user picks an output directory,
 *       all five formats are written there.</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>
 *   ReportGeneratorMain [modelUri] [rootId] [--cli] [outputDir]
 * </pre>
 *
 * <ul>
 *   <li>{@code modelUri} — either a classpath resource starting with "/"
 *       (default {@code /tests/out.flef}) or a filesystem path.</li>
 *   <li>{@code rootId}  — LocalID of the individual to use as report root.
 *       If omitted or not found, the first individual in the model is used.</li>
 *   <li>{@code --cli}   — run headless and skip the Swing UI.</li>
 *   <li>{@code outputDir} — only meaningful with {@code --cli}
 *       (default {@code target/reports}).</li>
 * </ul>
 *
 * <p>This class is intentionally kept next to {@link ReportGenerator} in the
 * main source set, mirroring the pattern already used by
 * {@code IndividualTreeGraphPanel} which exposes a similar {@code main} for
 * manual testing.</p>
 */
public final class ReportGeneratorMain{

	private static final String DEFAULT_MODEL_RESOURCE = "/tests/TGMZ.flef";
	private static final String DEFAULT_ROOT_ID = "I1";


	private ReportGeneratorMain(){}


	public static void main(final String[] args) throws Exception{
		final String modelUri = (args.length > 0? args[0]: DEFAULT_MODEL_RESOURCE);
		final String rootId = (args.length > 1? args[1]: DEFAULT_ROOT_ID);
		final boolean cli = (args.length > 2 && "--cli".equalsIgnoreCase(args[2]));
		final Path outDir = (args.length > 3? Path.of(args[3]): Path.of("target", "reports"));

		// Install the native look-and-feel only when the UI is going to be shown.
		if(!cli){
			try{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			catch(final Exception ignored){
				// Fall back to the default L&F when the system one is unavailable.
			}
		}

		final FLEFModel model = loadModel(modelUri);
		final FLEFRecord root = findRoot(model, rootId);
		if(root == null){
			System.err.println("No individual found in the model; cannot generate a report.");
			System.exit(1);
		}

		if(cli)
			generateAll(model, root, ReportConfig.defaults(), outDir);
		else
			SwingUtilities.invokeLater(() -> openUI(model, root));
	}


	/* ======================================================================
	 *                          Model loading
	 * ====================================================================== */

	private static FLEFModel loadModel(final String uriOrResource) throws IOException{
		final String content;
		if(uriOrResource.startsWith("/")){
			// Classpath resource, mirroring the convention used by
			// IndividualTreeGraphPanel.main().
			try(final InputStream is = ReportGeneratorMain.class.getResourceAsStream(uriOrResource)){
				if(is == null)
					throw new IOException("Classpath resource not found: " + uriOrResource);
				content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			}
		}
		else{
			content = Files.readString(Path.of(uriOrResource), StandardCharsets.UTF_8);
		}
		return new FLEFParser().parse(content);
	}

	private static FLEFRecord findRoot(final FLEFModel model, final String rootId){
		if(rootId != null){
			final FLEFRecord byId = model.getRecordById(rootId);
			if(byId != null)
				return byId;
		}

		// Fall back to the first individual in the model.
		for(final FLEFRecord r : model.getRecordsByType(IndividualHandler.TYPE))
			return r;
		return null;
	}


	/* ======================================================================
	 *                          Headless generation
	 * ====================================================================== */

	/**
	 * Renders the report in every supported format and returns the produced
	 * file paths keyed by format extension.
	 */
	private static Map<String, Path> generateAll(final FLEFModel model, final FLEFRecord root,
		final ReportConfig config, final Path outDir) throws IOException{
		Files.createDirectories(outDir);

		final ReportDocument doc = new ReportGenerator(model, root, config).generate();

		final Map<String, ReportRenderer> renderers = new LinkedHashMap<>();
//		renderers.put("report.txt", new TextReportRenderer());
		renderers.put("report.md", new MarkdownReportRenderer());
//		renderers.put("report.html", new HtmlReportRenderer());
//		renderers.put("report.docx", new DocxReportRenderer());
//		renderers.put("report.pdf", new PdfReportRenderer());

		final Map<String, Path> produced = new LinkedHashMap<>();
		for(final Map.Entry<String, ReportRenderer> e : renderers.entrySet()){
			final Path target = outDir.resolve(e.getKey());
			e.getValue()
				.render(doc, target);
			produced.put(e.getKey(), target);

			System.out.println("Wrote " + target.toAbsolutePath());
		}
		return produced;
	}


	/* ======================================================================
	 *                          Swing UI
	 * ====================================================================== */

	private static void openUI(final FLEFModel model, final FLEFRecord root){
		final ReportOptionsPanel options = new ReportOptionsPanel(ReportConfig.defaults());

		final JButton generate = new JButton("Generate…");
		final JButton exit = new JButton("Exit");

		final JPanel content = new JPanel(new BorderLayout(8, 8));
		content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		content.add(options, BorderLayout.CENTER);

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(exit);
		buttons.add(generate);
		content.add(buttons, BorderLayout.SOUTH);

		final JFrame frame = new JFrame("Report Generator — " + displayId(root));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(content);
		frame.pack();
		frame.setLocationRelativeTo(null);

		exit.addActionListener(e -> frame.dispose());

		generate.addActionListener(e -> {
			final JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Select the output directory");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if(chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION)
				return;

			final Path dir = chooser.getSelectedFile().toPath();
			try{
				final JComboBox<ReportLanguage> language = new JComboBox<>(ReportLanguage.values());
				final ReportLanguage selectedLang = (ReportLanguage)language.getSelectedItem();
				generateAll(model, root, options.toConfig(selectedLang), dir);

				final Object[] dialogOptions = {"OK", "Open Directory"};
				final int choice = JOptionPane.showOptionDialog(frame,
					"Reports written to:" + StringUtils.LF + dir.toAbsolutePath(),
					"Done",
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
						JOptionPane.showMessageDialog(frame,
							"Could not open directory:" + StringUtils.LF + ex.getMessage(),
							"Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
			catch(final IOException ex){
				JOptionPane.showMessageDialog(frame,
					"Error while generating the reports:" + StringUtils.LF + ex.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
			}
		});

		frame.setVisible(true);
	}

	private static String displayId(final FLEFRecord rec){
		final String id = rec.getId();
		return (id != null? id: "<unknown>");
	}

}
