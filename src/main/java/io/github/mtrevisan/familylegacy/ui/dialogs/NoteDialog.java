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
import io.github.mtrevisan.familylegacy.ui.utilities.FileHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.LocaleFilteredComboBox;
import io.github.mtrevisan.familylegacy.ui.utilities.PopupMouseAdapter;
import net.miginfocom.layout.ComponentWrapper;
import net.miginfocom.layout.LayoutCallback;
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
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.prefs.Preferences;


//https://github.com/admorsus/markdown-pad
public class NoteDialog extends JDialog{

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

	private static final String ACTION_MAP_KEY_UNDO = "undo";
	private static final String ACTION_MAP_KEY_REDO = "redo";

	private static final File FILE_HTML_CSS = new File("D:\\Mauro\\FamilyLegacy\\src\\main\\resources\\markdown\\css\\markdown-github.css");
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

	private static final Preferences PREFERENCES = Preferences.userNodeForPackage(NoteDialog.class);
	private static final String KEY_LINE_WRAP = "word.wrap";

	private static final Locale[] LOCALE_ITEMS;
	static{
		LOCALE_ITEMS = DateFormat.getAvailableLocales();
		Arrays.sort(LOCALE_ITEMS, Comparator.comparing(Locale::getDisplayName));
	}
	private static final DefaultComboBoxModel<String> RESTRICTION_MODEL = new DefaultComboBoxModel<>(new String[]{StringUtils.EMPTY,
		"confidential", "locked", "private"});

	private static final UndoManager UNDO_MANAGER = new UndoManager();
	private final JMenuItem htmlExportItem = new JMenuItem("Export to HTML…");


	private final JTextArea textView = new JTextArea();
	private final JEditorPane previewView = new JEditorPane();
	private final JLabel localeLabel = new JLabel("Locale:");
	private final LocaleFilteredComboBox localeComboBox = new LocaleFilteredComboBox(LOCALE_ITEMS);
	private final JLabel restrictionLabel = new JLabel("Restriction:");
	private final JComboBox<String> restrictionComboBox = new JComboBox<>(RESTRICTION_MODEL);
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Flef store;


	public NoteDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}

	private void initComponents(){
		setTitle("Note");

		textView.setDragEnabled(true);
		textView.setTabSize(3);
		attachPopUpMenu(textView);
		addUndoCapability(textView);

		final JScrollPane textScroll = new JScrollPane(textView);
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
		splitPane.setVisible(false);

		localeLabel.setLabelFor(localeComboBox);

		restrictionLabel.setLabelFor(restrictionComboBox);
		restrictionComboBox.setEditable(true);
		restrictionComboBox.addActionListener(e -> {
			if("comboBoxEdited".equals(e.getActionCommand())){
				final String newValue = (String)RESTRICTION_MODEL.getSelectedItem();
				RESTRICTION_MODEL.addElement(newValue);

				restrictionComboBox.setSelectedItem(newValue);
			}
		});

		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
//TODO
//			if(listener != null){
//				final GedcomNode selectedFamily = getSelectedFamily();
//				listener.onNodeSelected(selectedFamily, SelectedNodeType.FAMILY, panelReference);
//			}

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());


		final MigLayout layout = new MigLayout("debug");
		setLayout(layout);
		add(textScroll, "cell 0 0,hidemode 3");
		add(splitPane, "cell 0 0,hidemode 3");
		add(localeLabel, "cell 0 1,alignx right,split 2");
		add(localeComboBox, "cell 0 1");
		add(restrictionLabel, "cell 0 2,alignx right,split 2");
		add(restrictionComboBox, "cell 0 2,wrap paragraph");
		add(okButton, "tag ok,split 2,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");

		//NOTE: hide or show `previewScroll` based on dialog width
		layout.addLayoutCallback(new LayoutCallback(){
			@Override
			public void correctBounds(final ComponentWrapper wrapper){
				final int parentWidth = textScroll.getParent().getWidth();
				if(parentWidth > 600 && !splitPane.isVisible()){
					splitPane.setVisible(true);
//					splitPane.setDividerLocation(textView.getVisibleRect().width / 2);
//					splitPane.setDividerLocation(getWidth() / 2);
//					splitPane.setDividerLocation(0.5);
				}
				else if(parentWidth <= 600 && splitPane.isVisible())
					splitPane.setVisible(false);
			}
		});
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

	private void attachPopUpMenu(final JComponent component){
		final JPopupMenu popupMenu = new JPopupMenu();

		final JCheckBoxMenuItem previewItem = new JCheckBoxMenuItem("Preview");
		previewItem.addActionListener(event -> {
			final boolean preview = ((AbstractButton)event.getSource()).isSelected();
			//TODO expand component to reveal preview
			setSize((preview? 800: 400), getHeight());
			repaint();
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

		popupMenu.add(htmlExportItem);

		component.addMouseListener(new PopupMouseAdapter(popupMenu, component));
	}

	public void loadData(final GedcomNode note){
		final String text = note.getValue();

		textView.setText(text);
		//scroll to top
		textView.setCaretPosition(0);

		previewView.setText(renderHtml(text));
		//scroll to top
		previewView.setCaretPosition(0);

		htmlExportItem.addActionListener(event -> exportHtml(note));

		final String locale = store.traverse(note, "LOCALE")
			.getValue();
		localeComboBox.setSelectedItem(locale != null? new Locale(locale): null);

		restrictionComboBox.setSelectedItem(store.traverse(note, "RESTRICTION").getValue());

		repaint();
	}

	/**
	 * Creates the HTML document to visualize to the given Markdown code.
	 *
	 * @param markdown	Code string to be rendered.
	 * @return	HTML string.
	 */
	public String renderHtml(final String markdown){
		final Node document = MARKDOWN_PARSER.parse(markdown);
		return HTML_RENDERER.render(document);
	}

	/**
	 * Exports the markdown text to an HTML file.
	 */
	private void exportHtml(final GedcomNode note){
		final JFileChooser exportFileChooser = new JFileChooser();
		exportFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		exportFileChooser.removeChoosableFileFilter(exportFileChooser.getFileFilter());
		exportFileChooser.setFileFilter(new FileNameExtensionFilter("HTML Files (*.html)", "html"));
		if(exportFileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;

		File outputFile = exportFileChooser.getSelectedFile();
		if(!outputFile.getName().toLowerCase().endsWith(".html"))
			outputFile = new File(outputFile.getPath() + ".html");

		final String title = "NOTE " + note.getID();
		final String languageTag = store.traverse(note, "LOCALE").getValue();
		final Locale locale = Locale.forLanguageTag(languageTag != null? languageTag: "en-US");
		final String body = previewView.getText();
		try(final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))){
			out.write(extractHtml(title, locale, body).getBytes());
			out.close();
			JOptionPane.showMessageDialog(this, "Export HTML successful!");
		}
		catch(final IOException e){
			JOptionPane.showMessageDialog(this, "Export HTML failed! Please try again");
		}
	}

	private String extractHtml(final String title, final Locale locale, final String body){
		return HTML_START_LANGUAGE + locale.getLanguage()
			+ HTML_LANGUAGE_TITLE + title
			+ HTML_TITLE_STYLE + extractStyle()
			+ HTML_STYLE_BODY_BOUNDARY + body
			+ HTML_BODY_END;
	}

	private String extractStyle(){
		String style = StringUtils.EMPTY;
		try(final DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(FILE_HTML_CSS)))){
			if(FILE_HTML_CSS != null && FILE_HTML_CSS.exists()){
				final byte[] fileContent = new byte[(int)FILE_HTML_CSS.length()];
				input.read(fileContent);

				style = "<style type=\"text/css\">\n" + new String(fileContent, StandardCharsets.UTF_8) + "\n</style>";
			}
		}
		catch(final IOException e){
			e.printStackTrace();
		}
		return style;
	}


	private static class UndoAction extends AbstractAction{
		private static final long serialVersionUID = -167659402186653426L;

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
		private static final long serialVersionUID = 439250417104078123L;

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
		final GedcomNode note = store.getNotes().get(0);

		EventQueue.invokeLater(() -> {
			final NoteDialog dialog = new NoteDialog(store, new JFrame());
			dialog.loadData(note);

			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
//			dialog.setSize(1000, 500);
			dialog.setSize(500, 500);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
