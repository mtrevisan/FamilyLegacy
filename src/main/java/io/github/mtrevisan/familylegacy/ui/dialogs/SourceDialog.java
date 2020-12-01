/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gitlab.GitLabExtension;
import com.vladsch.flexmark.ext.superscript.SuperscriptExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.FileHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.LocaleFilteredComboBox;
import io.github.mtrevisan.familylegacy.ui.utilities.PopupMouseAdapter;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.prefs.Preferences;


public class SourceDialog extends JDialog{

	private static final double DATE_HEIGHT = 17.;
	private static final double DATE_ASPECT_RATIO = 270 / 248.;
	private static final Dimension DATE_SIZE = new Dimension((int)(DATE_HEIGHT / DATE_ASPECT_RATIO), (int)DATE_HEIGHT);

	//https://thenounproject.com/term/weekly-calendar/541199/
	private static final ImageIcon DATE = ResourceHelper.getImage("/images/date.png", DATE_SIZE);

	private static final Parser MARKDOWN_PARSER;
	private static final HtmlRenderer HTML_RENDERER;
	static{
		final MutableDataHolder markdownOptions = new MutableDataSet();
		/** @see <a href="https://github.com/vsch/flexmark-java/wiki/Extensions">FlexMark Extensions</a> */
		markdownOptions.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), AutolinkExtension.create(),
			StrikethroughExtension.create(), SuperscriptExtension.create(), FootnoteExtension.create(), GitLabExtension.create()
		));
		MARKDOWN_PARSER = Parser.builder(markdownOptions)
			.build();
		HTML_RENDERER = HtmlRenderer.builder(markdownOptions)
			.build();
	}

	private static final UndoManager UNDO_MANAGER = new UndoManager();
	private static final String ACTION_MAP_KEY_UNDO = "undo";
	private static final String ACTION_MAP_KEY_REDO = "redo";

	private static final File FILE_HTML_STANDARD_CSS = new File(NoteDialog.class.getResource("/markdown/css/markdown.css")
		.getFile());
	private static final File FILE_HTML_GITHUB_CSS = new File(NoteDialog.class.getResource("/markdown/css/markdown-github.css")
		.getFile());
	private static final String HTML_NEWLINE = "\n";
	private static final String HTML_START_LANGUAGE = new StringJoiner(HTML_NEWLINE)
		.add("<!DOCTYPE HTML>")
		.add("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"")
		.toString();
	private static final String HTML_LANGUAGE_TITLE = new StringJoiner(HTML_NEWLINE)
		.add("\">")
		.add("<head>")
		.add("<title>")
		.toString();
	private static final String HTML_TITLE_STYLE = new StringJoiner(HTML_NEWLINE)
		.add("</title>")
		.add("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />")
		.add(StringUtils.EMPTY)
		.toString();
	private static final String HTML_STYLE_BODY_BOUNDARY = new StringJoiner(HTML_NEWLINE)
		.add(StringUtils.EMPTY)
		.add("</head>")
		.add("<body>")
		.add(StringUtils.EMPTY)
		.toString();
	private static final String HTML_BODY_END = new StringJoiner(HTML_NEWLINE)
		.add(StringUtils.EMPTY)
		.add("</body>")
		.add("</html>")
		.toString();
	private static final JFileChooser EXPORT_FILE_CHOOSER;
	static{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		EXPORT_FILE_CHOOSER = new JFileChooser();
		EXPORT_FILE_CHOOSER.setFileSelectionMode(JFileChooser.FILES_ONLY);
		EXPORT_FILE_CHOOSER.removeChoosableFileFilter(EXPORT_FILE_CHOOSER.getFileFilter());
		EXPORT_FILE_CHOOSER.setFileFilter(new FileNameExtensionFilter("HTML Files (*.html)", "html"));
	}

	private static final Preferences PREFERENCES = Preferences.userNodeForPackage(SourceDialog.class);
	private static final String KEY_LINE_WRAP = "source.word.wrap";

	private static final DefaultComboBoxModel<String> EXTRACT_TYPE_MODEL = new DefaultComboBoxModel<>(new String[]{StringUtils.EMPTY,
		"transcript", "extract", "abstract"});

	private final JMenuItem htmlExportStandardItem = new JMenuItem("Standard stylesheet…");
	private final JMenuItem htmlExportGithubItem = new JMenuItem("Github stylesheet…");

	private final JLabel typeLabel = new JLabel("Type:");
	private final JTextField typeField = new JTextField();
	private final JLabel titleLabel = new JLabel("Title:");
	private final JTextField titleField = new JTextField();
	private final JButton eventsButton = new JButton("Events");
	private final JLabel dateLabel = new JLabel("Date:");
	private final JTextField dateField = new JTextField();
	private final JButton dateButton = new JButton(DATE);
	private final JLabel localeLabel = new JLabel("Locale:");
	private final LocaleFilteredComboBox localeComboBox = new LocaleFilteredComboBox();
	private final JLabel extractLabel = new JLabel("Extract:");
	private final JTextArea extractView = new JTextArea();
	private final JEditorPane previewView = new JEditorPane();
	private final JLabel extractTypeLabel = new JLabel("Extract type:");
	private final JComboBox<String> extractTypeComboBox = new JComboBox<>(EXTRACT_TYPE_MODEL);
	private final JButton repositoriesButton = new JButton("Repositories");
	private final JButton filesButton = new JButton("Files");
	private final JLabel urlLabel = new JLabel("URL:");
	private final JTextField urlField = new JTextField();
	private final JButton notesButton = new JButton("Notes");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private GedcomNode source;
	private Runnable onCloseGracefully;
	private final Flef store;


	public SourceDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}

	private void initComponents(){
		setTitle("Source");

		typeLabel.setLabelFor(typeField);

		titleLabel.setLabelFor(titleField);

		eventsButton.setEnabled(false);
		eventsButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.EVENT_CITATION, source)));

		dateLabel.setLabelFor(dateField);

		localeLabel.setLabelFor(localeComboBox);

		extractView.setTabSize(3);
		addUndoCapability(extractView);

		final JScrollPane textScroll = new JScrollPane(extractView);
		textScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		previewView.setEditable(false);
		previewView.setContentType("text/html");
		//manage links
		previewView.addHyperlinkListener(event -> {
			if(event.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
				FileHelper.browseURL(event.getURL().toString());
		});

		//https://tips4java.wordpress.com/2009/01/25/no-wrap-text-pane/
		//http://www.java2s.com/Code/Java/Swing-JFC/NonWrappingWrapTextPane.htm
		final JPanel intermediatePreviewPanel = new JPanel();
		intermediatePreviewPanel.add(previewView);
		final JScrollPane previewScroll = new JScrollPane(intermediatePreviewPanel);
		previewScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		previewScroll.setVisible(false);

		final JScrollBar textVerticalScrollBar = textScroll.getVerticalScrollBar();
		final JScrollBar previewVerticalScrollBar = previewScroll.getVerticalScrollBar();

		//set the two vertical scrollbar scroll at the same time
		textVerticalScrollBar.addAdjustmentListener(e -> {
			final double textMin = textVerticalScrollBar.getMinimum();
			final double textMax = textVerticalScrollBar.getMaximum();
			final double textVisibleAmount = textVerticalScrollBar.getVisibleAmount();
			final double previewMin = previewVerticalScrollBar.getMinimum();
			final double previewMax = previewVerticalScrollBar.getMaximum();
			final double previewVisibleAmount = previewVerticalScrollBar.getVisibleAmount();
			final double percent = textVerticalScrollBar.getValue() / (textMax - textMin - textVisibleAmount);
			//remove the AdjustmentListener of previewScrollPane temporarily
			final AdjustmentListener listener = previewVerticalScrollBar.getAdjustmentListeners()[0];
			previewVerticalScrollBar.removeAdjustmentListener(listener);
			//set the value of scrollbar in previewScroll
			previewVerticalScrollBar.setValue((int)(previewMin + percent * (previewMax - previewMin - previewVisibleAmount)));
			//add back the AdjustmentListener of previewScroll
			previewVerticalScrollBar.addAdjustmentListener(listener);
		});
		previewVerticalScrollBar.addAdjustmentListener(e -> {
			final double textMin = textVerticalScrollBar.getMinimum();
			final double textMax = textVerticalScrollBar.getMaximum();
			final double textVisibleAmount = textVerticalScrollBar.getVisibleAmount();
			final double previewMin = previewVerticalScrollBar.getMinimum();
			final double previewMax = previewVerticalScrollBar.getMaximum();
			final double previewVisibleAmount = previewVerticalScrollBar.getVisibleAmount();
			final double percent = previewVerticalScrollBar.getValue() / (previewMax - previewMin - previewVisibleAmount);
			//remove the AdjustmentListener of textScroll
			final AdjustmentListener listener = textVerticalScrollBar.getAdjustmentListeners()[0];
			textVerticalScrollBar.removeAdjustmentListener(listener);
			//set the value of scrollbar in textScroll
			textVerticalScrollBar.setValue((int)(textMin + percent * (textMax - textMin - textVisibleAmount)));
			//add back the AdjustmentListener of textScroll
			textVerticalScrollBar.addAdjustmentListener(listener);
		});

		final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textScroll, previewScroll);
		splitPane.setOneTouchExpandable(true);
		splitPane.setContinuousLayout(true);
		SwingUtilities.invokeLater(() -> {
			splitPane.setDividerLocation(1.);
			splitPane.setDividerSize(0);
		});
		attachPopUpMenu(extractView, splitPane, previewScroll);

		extractTypeLabel.setLabelFor(extractTypeComboBox);

		repositoriesButton.setEnabled(false);
		repositoriesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.REPOSITORY_CITATION, source)));

		filesButton.setEnabled(false);
		filesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.FILE_CITATION, source)));

		urlLabel.setLabelFor(urlField);

		notesButton.setEnabled(false);
		notesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, source)));

		okButton.addActionListener(evt -> {
			final String type = typeField.getText();
			final String title = titleField.getText();
			final String languageTag = ((Locale)localeComboBox.getModel().getSelectedItem()).toLanguageTag();
			final String extract = extractView.getText();
			final String extractType = (extractTypeComboBox.getSelectedIndex() > 0?
				Integer.toString(extractTypeComboBox.getSelectedIndex() + 1): null);
			final String url = urlField.getText();

			source.replaceChildValue("TYPE", type);
			source.replaceChildValue("TITLE", title);
			//TODO
			//date
			source.replaceChildValue("LOCALE", languageTag);
			source.replaceChildValue("EXTRACT", extract);
			final GedcomNode extractTypeNode = store.traverse(source, "EXTRACT.TYPE");
			if(!extractTypeNode.isEmpty())
				extractTypeNode.withValue(extractType);
			source.replaceChildValue("URL", url);

			if(onCloseGracefully != null)
				onCloseGracefully.run();

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());


		setLayout(new MigLayout("", "[400]"));
		add(typeLabel, "align label,split 2");
		add(typeField, "grow,wrap");
		add(titleLabel, "align label,split 2");
		add(titleField, "grow,wrap paragraph");
		add(eventsButton, "sizegroup button2,grow,wrap paragraph");
		add(dateLabel, "align label,split 3");
		add(dateField, "grow");
		add(dateButton, "wrap");
		add(localeLabel, "align label,split 2");
		add(localeComboBox, "wrap");
		add(extractLabel, "align label,wrap");
		add(splitPane, "span 2,grow,wrap");
		add(extractTypeLabel, "align label,split 2");
		add(extractTypeComboBox, "wrap paragraph");
		add(repositoriesButton, "sizegroup button2,grow,wrap");
		add(filesButton, "sizegroup button2,grow,wrap paragraph");
		add(urlLabel, "align label,split 2");
		add(urlField, "grow,wrap paragraph");
		add(notesButton, "sizegroup button2,grow,wrap paragraph");
		add(okButton, "tag ok,span,split 2,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	private void addUndoCapability(final JTextComponent textComponent){
		final Document doc = textComponent.getDocument();
		doc.addUndoableEditListener(event -> UNDO_MANAGER.addEdit(event.getEdit()));
		final InputMap textInputMap = textComponent.getInputMap(JComponent.WHEN_FOCUSED);
		textInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), ACTION_MAP_KEY_UNDO);
		textInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), ACTION_MAP_KEY_REDO);
		final ActionMap textActionMap = textComponent.getActionMap();
		textActionMap.put(ACTION_MAP_KEY_UNDO, new UndoAction());
		textActionMap.put(ACTION_MAP_KEY_REDO, new RedoAction());
	}

	private void attachPopUpMenu(final JComponent component, final JSplitPane splitPane, final JScrollPane previewScroll){
		final JPopupMenu popupMenu = new JPopupMenu();

		final JCheckBoxMenuItem previewItem = new JCheckBoxMenuItem("Preview");
		previewItem.addActionListener(event -> {
			final boolean preview = ((AbstractButton)event.getSource()).isSelected();
			setSize((preview? getWidth() * 2: getWidth() / 2), getHeight());
			if(preview)
				SwingUtilities.invokeLater(() -> {
					splitPane.setDividerLocation(0.5);
					splitPane.setDividerSize(5);
				});
			else
				SwingUtilities.invokeLater(() -> {
					splitPane.setDividerLocation(1.);
					splitPane.setDividerSize(0);
				});
			previewScroll.setVisible(preview);
		});
		popupMenu.add(previewItem);

		final JCheckBoxMenuItem lineWrapItem = new JCheckBoxMenuItem("Line wrap");
		lineWrapItem.addActionListener(event -> {
			final boolean lineWrap = ((AbstractButton)event.getSource()).isSelected();
			PREFERENCES.putBoolean(KEY_LINE_WRAP, lineWrap);

			extractView.setWrapStyleWord(lineWrap);
			extractView.setLineWrap(lineWrap);
		});
		lineWrapItem.setSelected(PREFERENCES.getBoolean(KEY_LINE_WRAP, false));
		popupMenu.add(lineWrapItem);

		final JMenu htmlExportMenu = new JMenu("Export to HTML");
		htmlExportMenu.add(htmlExportStandardItem);
		htmlExportMenu.add(htmlExportGithubItem);
		popupMenu.add(htmlExportMenu);

		component.addMouseListener(new PopupMouseAdapter(popupMenu, component));
	}

	public void loadData(final GedcomNode source, final Runnable onCloseGracefully){
		this.source = source;
		this.onCloseGracefully = onCloseGracefully;

		setTitle("Source " + source.getID());

		final String type = store.traverse(source, "TYPE").getValue();
		final String title = store.traverse(source, "TITLE").getValue();
		final GedcomNode dateNode = store.traverse(source, "DATE");
		final String url = store.traverse(source, "URL").getValue();

		typeField.setText(type);
		titleField.setText(title);
		//TODO
		//date node
		urlField.setText(url);

		//scroll to top
		extractView.setCaretPosition(0);
		previewView.setCaretPosition(0);

		htmlExportStandardItem.addActionListener(event -> exportHtml(source, FILE_HTML_STANDARD_CSS));
		htmlExportGithubItem.addActionListener(event -> exportHtml(source, FILE_HTML_GITHUB_CSS));

		repaint();
	}

	/**
	 * Creates the HTML document to visualize to the given Markdown code.
	 *
	 * @param markdown	Code string to be rendered.
	 * @return	HTML string.
	 */
	private String renderHtml(final String markdown){
		final Node document = MARKDOWN_PARSER.parse(markdown);
		return HTML_RENDERER.render(document);
	}

	/**
	 * Exports the markdown text to an HTML file.
	 */
	private void exportHtml(final GedcomNode source, final File htmlCssFile){
		if(EXPORT_FILE_CHOOSER.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;

		File outputFile = EXPORT_FILE_CHOOSER.getSelectedFile();
		if(!outputFile.getName().toLowerCase().endsWith(".html"))
			outputFile = new File(outputFile.getPath() + ".html");

		final String title = "SOURCE " + source.getID();
		final String languageTag = store.traverse(source, "LOCALE").getValue();
		final Locale locale = Locale.forLanguageTag(languageTag != null? languageTag: "en-US");
		final String body = previewView.getText();
		try(final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))){
			out.write(extractHtml(locale, htmlCssFile, title, body).getBytes());
			out.close();
			JOptionPane.showMessageDialog(this, "Export HTML successful!");
		}
		catch(final IOException e){
			JOptionPane.showMessageDialog(this, "Export HTML failed! Please try again");
		}
	}

	private String extractHtml(final Locale locale, final File htmlCssFile, final String title, final String body){
		return HTML_START_LANGUAGE + locale.getLanguage()
			+ HTML_LANGUAGE_TITLE + title
			+ HTML_TITLE_STYLE + extractStyle(htmlCssFile)
			+ HTML_STYLE_BODY_BOUNDARY + body
			+ HTML_BODY_END;
	}

	private String extractStyle(final File htmlCssFile){
		String style = StringUtils.EMPTY;
		if(htmlCssFile != null && htmlCssFile.exists()){
			try(final DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(htmlCssFile)))){
				final byte[] fileContent = new byte[(int)htmlCssFile.length()];
				input.read(fileContent);

				style = "<style type=\"text/css\">\n" + new String(fileContent, StandardCharsets.UTF_8) + "\n</style>";
			}
			catch(final IOException e){
				e.printStackTrace();
			}
		}
		return style;
	}


	private static class UndoAction extends AbstractAction{
		private static final long serialVersionUID = -3974682914632160277L;

		@Override
		public void actionPerformed(final ActionEvent event){
			try{
				if(UNDO_MANAGER.canUndo())
					UNDO_MANAGER.undo();
			}
			catch(final CannotUndoException e){
				e.printStackTrace();
			}
		}
	}

	private static class RedoAction extends AbstractAction{
		private static final long serialVersionUID = -4415532769601693910L;

		@Override
		public void actionPerformed(final ActionEvent event){
			try{
				if(UNDO_MANAGER.canRedo())
					UNDO_MANAGER.redo();
			}
			catch(final CannotUndoException e){
				e.printStackTrace();
			}
		}
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Flef store = new Flef();
		store.load("/gedg/flef_0.0.3.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		final GedcomNode source = store.getSources().get(0);

		EventQueue.invokeLater(() -> {
			final SourceDialog dialog = new SourceDialog(store, new JFrame());
			dialog.loadData(source, null);

			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(500, 500);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
