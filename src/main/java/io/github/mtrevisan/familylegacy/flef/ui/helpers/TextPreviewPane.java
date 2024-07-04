/**
 * Copyright (c) 2020-2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.helpers;

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
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import java.util.Objects;
import java.util.StringJoiner;
import java.util.prefs.Preferences;


public class TextPreviewPane extends JSplitPane{

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

	private static final String HTML_START = "<html>";
	private static final String HTML_END = "</html>";
	@SuppressWarnings("ConstantConditions")
	private static final File FILE_HTML_STANDARD_CSS = new File(TextPreviewPane.class.getResource("/markdown/css/markdown.css")
		.getFile());
	@SuppressWarnings("ConstantConditions")
	private static final File FILE_HTML_GITHUB_CSS = new File(TextPreviewPane.class.getResource("/markdown/css/markdown-github.css")
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
		.add(HTML_END)
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

	private static final Preferences PREFERENCES = Preferences.userNodeForPackage(TextPreviewPane.class);
	private static final String KEY_LINE_WRAP = "preview.word.wrap";

	private final JMenuItem htmlExportStandardItem = new JMenuItem("Standard stylesheet…");
	private final JMenuItem htmlExportGithubItem = new JMenuItem("Github stylesheet…");

	private final JTextArea textView = new JTextArea();
	private JEditorPane previewView;
	private TextPreviewListenerInterface listener;


	public static TextPreviewPane createWithoutPreview(){

		final TextPreviewPane pane = new TextPreviewPane();

		pane.textView.setTabSize(3);
		pane.textView.setRows(10);

		final JScrollPane textScroll = new JScrollPane(pane.textView);
		textScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		pane.setLeftComponent(textScroll);

		return pane;
	}

	public static TextPreviewPane createWithoutPreview(final TextPreviewListenerInterface listener){
		Objects.requireNonNull(listener);

		final TextPreviewPane pane = new TextPreviewPane();

		pane.listener = listener;

		pane.textView.setTabSize(3);
		pane.textView.setRows(10);

		GUIHelper.bindLabelTextChangeUndo(null, pane.textView, evt -> listener.textChanged());

		final JScrollPane textScroll = new JScrollPane(pane.textView);
		textScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		pane.setLeftComponent(textScroll);

		return pane;
	}

	public static TextPreviewPane createWithPreview(){
		final TextPreviewPane pane = createWithoutPreview();
		addPreview(pane);
		return pane;
	}

	public static TextPreviewPane createWithPreview(final TextPreviewListenerInterface listener){
		final TextPreviewPane pane = createWithoutPreview(listener);
		addPreview(pane);
		return pane;
	}

	private static void addPreview(TextPreviewPane pane){
		pane.textView.addKeyListener(new KeyAdapter(){
			@Override
			public void keyReleased(final KeyEvent event){
				super.keyReleased(event);

				pane.previewView.setText(pane.renderHtml(pane.textView.getText()));
			}
		});

		pane.previewView = new JEditorPane();
		//TODO add inner padding
		pane.previewView.setEditable(false);
		pane.previewView.setContentType("text/html");
		//manage links
		pane.previewView.addHyperlinkListener(event -> {
			if(event.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
				FileHelper.browseURL(event.getURL().toString());
		});

		//https://tips4java.wordpress.com/2009/01/25/no-wrap-text-pane/
		//http://www.java2s.com/Code/Java/Swing-JFC/NonWrappingWrapTextPane.htm
		final JScrollPane previewScroll = new JScrollPane(new ScrollableContainerHost(pane.previewView,
			ScrollableContainerHost.ScrollType.VERTICAL));
		previewScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		previewScroll.setVisible(false);
		pane.attachPreviewPopUpMenu(previewScroll);
		pane.setRightComponent(previewScroll);
		pane.setResizeWeight(0.5);

		pane.setOneTouchExpandable(true);
		pane.setContinuousLayout(true);
		pane.setDividerLocation(1.);
		pane.setDividerSize(0);
	}


	private TextPreviewPane(){
		super(JSplitPane.HORIZONTAL_SPLIT);
	}


	public Color getTextViewackgroundColor(){
		return textView.getBackground();
	}

	public void setTextViewBackgroundColor(final Color c){
		GUIHelper.setBackgroundColor(textView, c);
	}

	public Font getTextViewFont(){
		return textView.getFont();
	}

	public void setTextViewFont(final Font f){
		textView.setFont(f);
	}

	public Font getTextPreviewFont(){
		return previewView.getFont();
	}

	public void setTextPreviewFont(final Font f){
		previewView.setFont(f);
	}

	/**
	 * Creates the HTML document to visualize to the given Markdown code.
	 *
	 * @param markdown	Code string to be rendered.
	 * @return	HTML string.
	 */
	private String renderHtml(final String markdown){
		if(markdown == null || markdown.isEmpty())
			return HTML_START + HTML_END;

		final Node document = MARKDOWN_PARSER.parse(markdown);
		final String renderedDocument = HTML_RENDERER.render(document);
		return  HTML_START + renderedDocument + HTML_END;
	}

	/**
	 * Exports the Markdown text to an HTML file.
	 */
	private void exportHtml(final String title, final String languageTag, final File htmlCssFile){
		if(EXPORT_FILE_CHOOSER.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;

		File outputFile = EXPORT_FILE_CHOOSER.getSelectedFile();
		if(!outputFile.getName().toLowerCase(Locale.ROOT).endsWith(".html"))
			outputFile = new File(outputFile.getPath() + ".html");

		final Locale locale = Locale.forLanguageTag(languageTag != null? languageTag: "en-US");
		final String body = textView.getText();
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

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private String extractStyle(final File htmlCssFile){
		String style = StringUtils.EMPTY;
		if(htmlCssFile != null && htmlCssFile.exists()){
			try(final DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(htmlCssFile)))){
				final byte[] fileContent = new byte[(int)htmlCssFile.length()];
				input.read(fileContent);

				style = "<style type=\"text/css\">\n" + new String(fileContent, StandardCharsets.UTF_8) + "\n</style>";
			}
			catch(final IOException ioe){
				ioe.printStackTrace();
			}
		}
		return style;
	}

	/**
	 * Registers the given observer to begin receiving notifications when changes are made to the document.
	 *
	 * @param listener	The observer to register.
	 */
	public void addDocumentListener(final DocumentListener listener){
		textView.getDocument().addDocumentListener(listener);
	}


	private void attachPreviewPopUpMenu(final JScrollPane previewScroll){
		final JPopupMenu popupMenu = new JPopupMenu();

		final JCheckBoxMenuItem previewItem = new JCheckBoxMenuItem("Preview");
		previewItem.addActionListener(event -> {
			final boolean previewVisible = ((AbstractButton)event.getSource()).isSelected();
			if(previewVisible){
				setDividerLocation(0.5);
				setDividerSize(5);
			}
			else{
				setDividerLocation(1.);
				setDividerSize(0);
			}
			previewScroll.setVisible(previewVisible);

			if(listener != null)
				listener.onPreviewStateChange(previewVisible);
		});
		popupMenu.add(previewItem);

		final JCheckBoxMenuItem lineWrapItem = new JCheckBoxMenuItem("Line wrap");
		lineWrapItem.addActionListener(event -> {
			final boolean lineWrap = ((AbstractButton)event.getSource()).isSelected();
			PREFERENCES.putBoolean(KEY_LINE_WRAP, lineWrap);

			textView.setWrapStyleWord(lineWrap);
			textView.setLineWrap(lineWrap);
		});
		lineWrapItem.setSelected(PREFERENCES.getBoolean(KEY_LINE_WRAP, false));
		popupMenu.add(lineWrapItem);

		final JMenu htmlExportMenu = new JMenu("Export to HTML");
		htmlExportMenu.add(htmlExportStandardItem);
		htmlExportMenu.add(htmlExportGithubItem);
		popupMenu.add(htmlExportMenu);

		textView.addMouseListener(new PopupMouseAdapter(popupMenu, textView));
	}


	public void clear(){
		setText(null);
	}

	public void setText(final String title, final String text, final String languageTag){
		if(listener != null){
			removeAllActionListeners(htmlExportStandardItem);
			htmlExportStandardItem.addActionListener(event -> exportHtml(title, languageTag, FILE_HTML_STANDARD_CSS));

			removeAllActionListeners(htmlExportGithubItem);
			htmlExportGithubItem.addActionListener(event -> exportHtml(title, languageTag, FILE_HTML_GITHUB_CSS));
		}

		setText(text);
	}

	private void setText(final String text){
		//store original text to change the ok button state
		textView.setText(text);
		//scroll to top
		textView.setCaretPosition(0);

		if(previewView != null && listener != null){
			final String html = renderHtml(text);
			previewView.setText(html);
			previewView.setCaretPosition(0);
		}
	}

	private void removeAllActionListeners(final JMenuItem menuItem){
		final ActionListener[] actionListeners = menuItem.getActionListeners();
		for(final ActionListener actionListener : actionListeners)
			menuItem.removeActionListener(actionListener);
	}

	public String getText(){
		return textView.getText()
			.trim();
	}

}
