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
import java.util.Arrays;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.prefs.Preferences;


//https://github.com/admorsus/markdown-pad
public class MarkdownEditorDialog extends JDialog{

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

	private static final Preferences PREFERENCES = Preferences.userNodeForPackage(MarkdownEditorDialog.class);
	private static final String KEY_LINE_WRAP = "word.wrap";
	private static final String KEY_PREVIEW = "preview";

	private static final String PREVIEW_AUTOHIDE_WIDTH = "autohide.width";

	private static final UndoManager UNDO_MANAGER = new UndoManager();
	private final JMenuItem htmlExportItem = new JMenuItem("Export to HTML…");


	private final JTextArea textView = new JTextArea();
	private final JScrollPane textScroll = new JScrollPane(textView);
	private final JTextPane previewView = new JTextPane();
	private final JScrollPane previewScroll = new JScrollPane(previewView);

	private final Flef store;


	public MarkdownEditorDialog(final Flef store){
		this.store = store;

		initComponents();
	}

	private void initComponents(){
		textView.setDragEnabled(true);
		textView.setTabSize(3);
		attachPopUpMenu(textView);
		addUndoCapability(textView);

		textScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		textScroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(),
			BorderFactory.createLoweredBevelBorder()));

		previewView.setEditable(false);
		//FIXME remove word wrap
//		previewView.setEditorKit(new NoWrapEditorKit());
		previewView.setContentType("text/html");
		//manage links
		previewView.addHyperlinkListener(event -> {
			if(event.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
				FileHelper.browseURL(event.getURL().toString());
		});

		previewScroll.putClientProperty(PREVIEW_AUTOHIDE_WIDTH, 600);
		previewScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		previewScroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(),
			BorderFactory.createLoweredBevelBorder()));

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
			if(PREFERENCES.getBoolean(KEY_PREVIEW, false)){
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
			}
		});


		final MigLayout layout = new MigLayout("debug,insets 0", "[400,grow]0[400,grow]");
		setLayout(layout);
		add(textScroll, "grow");
		//FIXME expand/contract windows following previewScroll's visibility
		add(previewScroll, "gapx rel,grow,hidemode 3");

		//NOTE: hide or show `previewScroll` based on dialog width
		layout.addLayoutCallback(new LayoutCallback(){
			@Override
			public void correctBounds(final ComponentWrapper wrapper){
				final Number width = (Number)previewScroll.getClientProperty(PREVIEW_AUTOHIDE_WIDTH);
				if(width != null){
					final int parentWidth = previewScroll.getParent().getWidth();
					final int selfWidth = width.intValue();
					if(previewScroll.isVisible() && parentWidth < selfWidth)
						previewScroll.setVisible(false);
					else if(!previewScroll.isVisible() && parentWidth > selfWidth)
						previewScroll.setVisible(true);
				}
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
			PREFERENCES.putBoolean(KEY_PREVIEW, preview);

			//TODO expand component to reveal preview
			final Dimension size = getSize();
			size.width = (preview? 800: 400);
			setSize(size);
			textScroll.setSize(400, textScroll.getHeight());
			if(preview)
				previewScroll.setSize(400, previewScroll.getHeight());
			repaint();
		});
		previewItem.setSelected(PREFERENCES.getBoolean(KEY_PREVIEW, false));
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

	//FIXME
//	private static final class NoWrapEditorKit extends HTMLEditorKit{
//		private final ViewFactory viewFactory = new HTMLEditorKit.HTMLFactory(){
//			@Override
//			public View create(final Element elem){
//				final AttributeSet attrs = elem.getAttributes();
//				final Object elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute);
//				final Object attribute = (elementName != null? null: attrs.getAttribute(StyleConstants.NameAttribute));
//				if(attribute instanceof HTML.Tag){
//					final HTML.Tag kind = (HTML.Tag)attribute;
//					if(kind == HTML.Tag.IMG)
//						return new ImageView(elem);
//					else if(kind == HTML.Tag.IMPLIED)
//						return new ParagraphView(elem);
//				}
//				return super.create(elem);
//			}
//		};
//
//		@Override
//		public ViewFactory getViewFactory(){
//			return viewFactory;
//		}
//	}

//	private static class NoWrapEditorKit extends StyledEditorKit{
//
//		private static final String LINE_BREAK_ATTRIBUTE_NAME = "line_break_attribute";
//
//		public ViewFactory getViewFactory(){
//			return new StyledViewFactory();
//		}
//
//		public MutableAttributeSet getInputAttributes(){
//			final MutableAttributeSet attributes = super.getInputAttributes();
//			attributes.removeAttribute(LINE_BREAK_ATTRIBUTE_NAME);
//			return attributes;
//		}
//
//		private static class StyledViewFactory implements ViewFactory{
//			@Override
//			public View create(final Element elem){
//				final String kind = elem.getName();
//				if(kind != null){
//					if(kind.equals(AbstractDocument.ParagraphElementName))
////						return new ParagraphView(elem);
//						return new NoWrapParagraphView(elem);
//					else if(kind.equals(AbstractDocument.ContentElementName))
////						return new LabelView(elem);
//						return new NoWrapLabelView(elem);
//					else if(kind.equals(AbstractDocument.SectionElementName))
//						return new NoWrapBoxView(elem, View.Y_AXIS);
////						return new BoxView(elem, View.Y_AXIS);
//					else if(kind.equals(StyleConstants.ComponentElementName))
//						return new ComponentView(elem);
//					else if(kind.equals(StyleConstants.IconElementName))
//						return new IconView(elem);
//				}
//
//				return new LabelView(elem);
//			}
//		}
//
//		private static class NoWrapBoxView extends BoxView{
//			NoWrapBoxView(final Element elem, final int axis){
//				super(elem, axis);
//			}
//
//			@Override
//			public void layout(final int width, final int height){
//				super.layout(Short.MAX_VALUE, height);
//			}
//
//			@Override
//			public float getMinimumSpan(final int axis){
//				return getPreferredSpan(axis);
//			}
//		}
//
//		private static class NoWrapLabelView extends LabelView{
//			NoWrapLabelView(final Element elem){
//				super(elem);
//			}
//
//			@Override
//			public float getTabbedSpan(final float x, final TabExpander expander){
//				final float result = super.getTabbedSpan(x, expander);
//				preferenceChanged(this, true, false);
//				return result;
//			}
//		}
//
//		private static class NoWrapParagraphView extends ParagraphView{
//			NoWrapParagraphView(final Element elem){
//				super(elem);
//			}
//
//			@Override
//			public void layout(final int width, final int height){
//				super.layout(Short.MAX_VALUE, height);
//			}
//
//			@Override
//			public float getMinimumSpan(final int axis){
//				return getPreferredSpan(axis);
//			}
//		}
//	}


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
			final MarkdownEditorDialog dialog = new MarkdownEditorDialog(store);
//			final String content = "# Boxon _[boˈzoŋ]_\n" + "\n" + "![Java-11+](https://img.shields.io/badge/java-11%2B-orange.svg) [![License: GPL v3](https://img.shields.io/badge/License-MIT-blue.svg)](https://www.gnu.org/licenses/gpl-3.0) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.mtrevisan/boxon/badge.svg)](https://mvnrepository.com/artifact/io.github.mtrevisan/boxon)\n" + "\n" + "<a href=\"https://codeclimate.com/github/mtrevisan/Boxon/maintainability\"><img src=\"https://api.codeclimate.com/v1/badges/bff8577200d792e1e197/maintainability\" /></a>\n" + "\n" + "[![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](https://www.repostatus.org/badges/latest/active.svg)](https://www.repostatus.org/#active)\n" + "==========\n" + "\n" + "<br />\n" + "\n" + "## Forewords\n" + "This is a declarative, bit-level, message parser. All you have to do is write a [POJO](https://en.wikipedia.org/wiki/Plain_old_Java_object) that represents your message and annotate it. That's all. [Boxon](https://en.wikipedia.org/wiki/Boson) will take care of the rest for you.\n" + "\n" + "If you want to use the parser straight away, just go [here](#examples).\n" + "\n" + "<br />\n" + "\n" + "| This project adheres to the **[Zero Bugs Commitment](https://github.com/classgraph/classgraph/blob/master/Zero-Bugs-Commitment.md)**. |\n" + "|-----------------------------|\n" + "\n" + "<br />\n" + "\n" + "(Like [Preon](https://github.com/preon/preon) — currently not maintained anymore —, but the code is understandable, shorter, easier to extend, uses the more powerful (and maintained) [SpEL expressions](https://docs.spring.io/spring/docs/4.3.10.RELEASE/spring-framework-reference/html/expressions.html), and the documentation is __really__ free...)\n" + "\n" + "<br/>\n" + "\n" + "### Notable features\n" + "Boxon...\n" + " - Is easily extensible through the use of [converters](#how-to).\n" + " - Contains a minimal set of [annotations](#annotation-base) capable of handling \"all\" the primitive data (aside `char`, but this could be easily handled with a converter).\n" + " - Contains a set of [special annotations](#annotation-special) that handles the various messages peculiarities (defining message header properties, conditional choosing of converter, or object while reading an array, skip bits, checksum, 'constant' assignments)\n" + " - Is capable of handle concatenation of messages, using the correct template under the hood.\n" + " - Can handle [SpEL expressions](https://docs.spring.io/spring/docs/4.3.10.RELEASE/spring-framework-reference/html/expressions.html) on certain fields, thus more powerful and simpler than [Limbo](http://limbo.sourceforge.net/apidocs/)<sup>[1](#footnote-1)</sup> (but less than [janino](https://github.com/janino-compiler/janino), that has other problems).\n" + " - Can do decode and encode of data on the fly with a single annotated class (thus avoiding separate decoder and encoder going out-of-sync).\n" + " - Has templates (annotated classes) that are not complex: they do not call each other uselessly complicating the structure (apart, necessarily, for `@BindArray`), no complicated chains of factories: it's just a parser that works.\n" + " - Supports [SLF4J](http://www.slf4j.org/).\n" + " - Hides the complexities of encoding and decoding, thus simplifying the changes to be made to the code due to frequent protocol changes.\n" + " - Can automatically scans and loads all the binding annotations and/or templates from a package.\n" + "\n" + "---\n" + "\n" + "<a name=\"footnote-1\"></a>\n" + "<sub><sup>1</sup> Currently Limbo is merged with Preon... thus rendering Preon not only a parser, but also an evaluator, over-complicating and cluttering the code.</sub>\n" + "\n" + "<br/>\n" + "\n" + "### Differences from...\n" + "#### Preon\n" + "Boxon differs from Preon in...\n" + " - Does not have a generic `Bound` annotation: it uses converters instead.\n" + " - Does not need the \"native byte order\" constant. This is because the bytes of the message have little chance to be generated from the very same machine that will parse them, what if a message consider 24 bits as an Integer? If the code should be portable and installed and run everywhere it should not rely on the native properties of any machine.\n" + "   Moreover, `@Bound boolean visible;` is 1 bit- or 1 byte-length?\n" + " - Does not have `BoundList`: since the message is a finite sequence of bytes, then any array is of finite length, and thus the standard java array (`[]`) is sufficient. If someone wants a `List` a converter can be used.\n" + " - Does not rely on the type of the annotated variable (because of the existence of the converters); in fact, the annotation, eventually, serves the purpose to pass a predefined type of data to a converter.<br/>\n" + "   For this reason too, there is no need for the `Init` annotation, thus the annotated file can contain the least amount of data necessary for its decoding (moreover, this annotation has NOT the inverse operation -- so it seems to me... so it's pretty useless anyway).\n" + " - (By personal experience) enumerations can have different representations, or change between a version and the next of a protocol, even inside the same protocol (!), so having an annotation that tells the value of a particular element of this enum is at least risky. So, for this reason, the `BoundEnumOption` is not present in this library.\n" + " - Does read and write more than 64 bits at a time (`BitBuffer.readBits`)\n" + "\n" + "<br/>\n" + "\n" + "### Pre-compiled executables\n" + "Get them [here](https://github.com/mtrevisan/Boxon/releases/).\n" + "\n" + "### Maven dependency\n" + "In order to include Boxon in a Maven project add the following dependency to your pom.xml (<b>Java 11 required</b>).\n" + "\n" + "Replace `x.y.z` below int the version tag with the latest [release number](https://github.com/mtrevisan/Boxon/releases).\n" + "\n" + "```xml\n" + "<dependency>\n" + "    <groupId>io.github.mtrevisan</groupId>\n" + "    <artifactId>boxon</artifactId>\n" + "    <version>x.y.z</version>\n" + "</dependency>\n" + "```\n" + "\n" + "### Pre-built JARs\n" + "\n" + "You can get pre-built JARs (usable on JRE 11 or newer) from [Sonatype](https://oss.sonatype.org/#nexus-search;quick~io.github.mtrevisan).\n" + "\n" + "<br/>\n" + "<br/>\n" + "\n" + "## Table of Contents\n" + "1. [Base annotations](#annotation-base)\n" + "    1. [Summary](#annotation-summary)\n" + "    2. [BindObject](#annotation-bindobject)\n" + "    3. [BindArray](#annotation-bindarray)\n" + "    4. [BindArrayPrimitive](#annotation-bindarrayprimitive)\n" + "    5. [BindBits](#annotation-bindbits)\n" + "    6. [BindByte](#annotation-bindbyte)\n" + "    7. [BindShort](#annotation-bindshort)\n" + "    8. [BindInt](#annotation-bindint)\n" + "    9. [BindLong](#annotation-bindlong)\n" + "    10. [BindInteger](#annotation-bindinteger)\n" + "    11. [BindFloat](#annotation-bindfloat)\n" + "    12. [BindDouble](#annotation-binddouble)\n" + "    13. [BindDecimal](#annotation-binddecimal)\n" + "    14. [BindString](#annotation-bindstring)\n" + "    15. [BindStringTerminated](#annotation-bindstringterminated)\n" + "2. [Special annotations](#annotation-special)\n" + "    1. [MessageHeader](#annotation-messageheader)\n" + "    2. [Skip](#annotation-skip)\n" + "    3. [Checksum](#annotation-checksum)\n" + "    4. [Evaluate](#annotation-evaluate)\n" + "3. [How to write SpEL expressions](#how-to-spel)\n" + "4. [How to extend the functionalities](#how-to-extend)\n" + "5. [Digging into the code](#digging)\n" + "    1. [Converters](#how-to-converters)\n" + "    2. [Custom annotations](#how-to-annotations)\n" + "6. [Examples](#examples)\n" + "    1. [Multi-message parser](#example-multi)\n" + "    2. [Message composer](#example-composer)\n" + "7. [Changelog](#changelog)\n" + "    1. [version 1.1.0](#changelog-1.1.0)\n" + "    2. [version 1.0.0](#changelog-1.0.0)\n" + "    3. [version 0.0.2](#changelog-0.0.2)\n" + "    4. [version 0.0.1](#changelog-0.0.1)\n" + "    5. [version 0.0.0](#changelog-0.0.0)\n" + "8. [License](#license)\n" + "9. [Attributions](#attributions)\n" + "\n" + "<br/>\n" + "\n" + "<a name=\"annotation-base\"></a>\n" + "## Base annotations\n" + "Here the build-in base annotations are described.\n" + "\n" + "You can use them as a starting point to build your own customized readers.\n" + "\n" + "<a name=\"annotation-summary\"></a>\n" + "### Summary\n" + "\n" + "Here is a brief summary of the parameters (described in detail below) for each annotation.\n" + "\n" + "|                      | condition |  type   | charset | terminator | consumeTerminator |  size   | byteOrder | selectFrom | selectDefault | validator | converter | selectConverterFrom |                      |\n" + "|----------------------|:---------:|:-------:|:-------:|:----------:|:-----------------:|:-------:|:---------:|:----------:|:-------------:|:---------:|:---------:|:-------------------:|---------------------:|\n" + "| BindObject           |  &#9745;  | &#9745; |         |            |                   |         |           |  &#9745;   |    &#9745;    |  &#9745;  |  &#9745;  |       &#9745;       | BindObject           |\n" + "| BindArray            |  &#9745;  | &#9745; |         |            |                   | &#9745; |           |  &#9745;   |    &#9745;    |  &#9745;  |  &#9745;  |       &#9745;       | BindArray            |\n" + "| BindArrayPrimitive   |  &#9745;  | &#9745; |         |            |                   | &#9745; |  &#9745;  |            |               |  &#9745;  |  &#9745;  |       &#9745;       | BindArrayPrimitive   |\n" + "| BindBits             |  &#9745;  |         |         |            |                   | &#9745; |  &#9745;  |            |               |  &#9745;  |  &#9745;  |       &#9745;       | BindBits             |\n" + "| BindByte             |  &#9745;  |         |         |            |                   |         |           |            |               |  &#9745;  |  &#9745;  |       &#9745;       | BindByte             |\n" + "| BindShort            |  &#9745;  |         |         |            |                   |         |  &#9745;  |            |               |  &#9745;  |  &#9745;  |       &#9745;       | BindShort            |\n" + "| BindInt              |  &#9745;  |         |         |            |                   |         |  &#9745;  |            |               |  &#9745;  |  &#9745;  |       &#9745;       | BindInt              |\n" + "| BindLong             |  &#9745;  |         |         |            |                   |         |  &#9745;  |            |               |  &#9745;  |  &#9745;  |       &#9745;       | BindLong             |\n" + "| BindInteger          |  &#9745;  |         |         |            |                   | &#9745; |  &#9745;  |            |               |  &#9745;  |  &#9745;  |       &#9745;       | BindInteger          |\n" + "| BindFloat            |  &#9745;  |         |         |            |                   |         |  &#9745;  |            |               |  &#9745;  |  &#9745;  |       &#9745;       | BindFloat            |\n" + "| BindDouble           |  &#9745;  |         |         |            |                   |         |  &#9745;  |            |               |  &#9745;  |  &#9745;  |       &#9745;       | BindDouble           |\n" + "| BindDecimal          |  &#9745;  | &#9745; |         |            |                   |         |  &#9745;  |            |               |  &#9745;  |  &#9745;  |       &#9745;       | BindDecimal          |\n" + "| BindString           |  &#9745;  |         | &#9745; |            |                   | &#9745; |           |            |               |  &#9745;  |  &#9745;  |       &#9745;       | BindString           |\n" + "| BindStringTerminated |  &#9745;  |         | &#9745; |  &#9745;   |     &#9745;       |         |           |            |               |  &#9745;  |  &#9745;  |       &#9745;       | BindStringTerminated |\n" + "\n" + "|                      | condition |  start  |   end   | charset |   size  | terminator | consumeTerminator |  type   | byteOrder | skipStart | skipEnd | algorithm | startValue |  value  |               |\n" + "|----------------------|:---------:|:-------:|:-------:|:-------:|:-------:|:----------:|:-----------------:|:-------:|:---------:|:---------:|:-------:|:---------:|:----------:|:-------:|--------------:|\n" + "| MessageHeader        |           | &#9745; | &#9745; | &#9745; |         |            |                   |         |           |           |         |           |            |         | MessageHeader |\n" + "| Skip                 |  &#9745;  |         |         |         | &#9745; |  &#9745;   |      &#9745;      |         |           |           |         |           |            |         | Skip          |\n" + "| Checksum             |           |         |         |         |         |            |                   | &#9745; |  &#9745;  |  &#9745;  | &#9745; |  &#9745;  |  &#9745;   |         | Checksum      |\n" + "| Evaluate             |  &#9745;  |         |         |         |         |            |                   |         |           |           |         |           |            | &#9745; | Evaluate      |\n" + "\n" + "\n" + "<a name=\"annotation-bindobject\"></a>\n" + "### BindObject\n" + "\n" + "#### parameters\n" + " - `condition`: The SpEL expression that determines if this field has to be read.\n" + " - `type`: the Class of the Object of the single element of the array (defaults to `Object`).\n" + " - `selectFrom`: the selection from which to choose the instance type.\n" + " - `selectDefault`: the default selection if none can be chosen from `selectFrom` (defaults to `void.class`).\n" + " - `validator`: the Class of a validator (applied BEFORE the converter).\n" + " - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable.\n" + " - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).\n" + "\n" + "#### description\n" + "Reads a single Object.\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "class Version{\n" + "    @BindByte\n" + "    public byte major;\n" + "    @BindByte\n" + "    public byte minor;\n" + "    public byte build;\n" + "}\n" + "\n" + "@BindBits(size = \"1\", converter = BitToBooleanConverter.class)\n" + "private boolean versionPresent;\n" + "@BindObject(condition = \"versionPresent\", type = Version.class)\n" + "private Version version;\n" + "```\n" + "\n" + "\n" + "<a name=\"annotation-bindarray\"></a>\n" + "### BindArray\n" + "\n" + "#### parameters\n" + " - `condition`: The SpEL expression that determines if this field has to be read.\n" + " - `type`: the Class of the Object of the single element of the array (defaults to `Object`).\n" + " - `size`: the size of the array (can be a SpEL expression).\n" + " - `selectFrom`: the selection from which to choose the instance type.\n" + " - `selectDefault`: the default selection if none can be chosen from `selectFrom` (defaults to `void.class`).\n" + " - `validator`: the Class of a validator (applied BEFORE the converter).\n" + " - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. \n" + " - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).\n" + "\n" + "#### description\n" + "Reads an array of Objects.\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "class Version{\n" + "    @BindByte\n" + "    public byte major;\n" + "    @BindByte\n" + "    public byte minor;\n" + "    public byte build;\n" + "}\n" + "\n" + "@BindArray(size = \"2\", type = Version.class)\n" + "private Version[] versions;\n" + "```\n" + "\n" + "```java\n" + "@BindByte\n" + "private byte positionsCount;\n" + "@BindArray(size = \"positionsCount\", type = Position.class,\n" + "   selectFrom = @ObjectChoices(prefixSize = 8,\n" + "        alternatives = {\n" + "          @ObjectChoices.ObjectChoice(condition = \"#prefix == 0\", prefix = 0, type = PositionInvalid.class),\n" + "          @ObjectChoices.ObjectChoice(condition = \"#prefix == 1\", prefix = 1, type = PositionAbsolute.class),\n" + "          @ObjectChoices.ObjectChoice(condition = \"#prefix == 2\", prefix = 2, type = PositionRelative.class),\n" + "          @ObjectChoices.ObjectChoice(condition = \"#prefix == 3\", prefix = 3, type = PositionSameAsPrevious.class)\n" + "       }\n" + "    ),\n" + "   converter = PositionsConverter.class)\n" + "private Position[] positions;\n" + "```\n" + "\n" + "\n" + "<a name=\"annotation-bindarrayprimitive\"></a>\n" + "### BindArrayPrimitive\n" + "\n" + "#### parameters\n" + " - `condition`: The SpEL expression that determines if this field has to be read.\n" + " - `type`: the Class of primitive of the single element of the array.\n" + " - `size`: the size of the array (can be a SpEL expression).\n" + " - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN` (used for primitives other than `byte`).\n" + " - `validator`: the Class of a validator (applied BEFORE the converter).\n" + " - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. \n" + " - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).\n" + "\n" + "#### description\n" + "Reads an array of primitives.\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "@BindArrayPrimitive(size = \"2\", type = byte.class)\n" + "private byte[] array;\n" + "```\n" + "\n" + "```java\n" + "@BindBits(size = \"1\", converter = BitToBooleanConverter.class)\n" + "private boolean angularDataPresent;\n" + "@BindArrayPrimitive(condition = \"angularDataPresent\", size = \"dataLength\", type = byte.class,\n" + "    selectConverterFrom = @ConverterChoices(\n" + "        alternatives = {\n" + "            @ConverterChoices.ConverterChoice(condition = \"angularDataPresent\", converter = CrashDataWithAngularDataConverter.class),\n" + "            @ConverterChoices.ConverterChoice(condition = \"!angularDataPresent\", converter = CrashDataWithoutAngularDataConverter.class)\n" + "        })\n" + "    )\n" + "private BigDecimal[][] crashData;\n" + "```\n" + "\n" + "\n" + "<a name=\"annotation-bindbits\"></a>\n" + "### BindBits\n" + "\n" + "#### parameters\n" + " - `condition`: The SpEL expression that determines if this field has to be read.\n" + " - `size`: the number of bits to read (can be a SpEL expression).\n" + " - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.\n" + " - `validator`: the Class of a validator (applied BEFORE the converter).\n" + " - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. \n" + " - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).\n" + "\n" + "#### description\n" + "Reads a `BitMap`.\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "@BindBits(size = \"2\")\n" + "private BitMap bits;\n" + "```\n" + "\n" + "\n" + "<a name=\"annotation-bindbyte\"></a>\n" + "### BindByte\n" + "\n" + "#### parameters\n" + " - `condition`: The SpEL expression that determines if this field has to be read.\n" + " - `validator`: the Class of a validator (applied BEFORE the converter).\n" + " - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. \n" + " - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).\n" + "\n" + "#### description\n" + "Reads a byte (or Byte).\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "@BindByte\n" + "public Byte mask;\n" + "```\n" + "\n" + "\n" + "<a name=\"annotation-bindshort\"></a>\n" + "### BindShort\n" + "\n" + "#### parameters\n" + " - `condition`: The SpEL expression that determines if this field has to be read.\n" + " - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.\n" + " - `validator`: the Class of a validator (applied BEFORE the converter).\n" + " - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. \n" + " - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).\n" + "\n" + "#### description\n" + "Reads a short (or Short).\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "@BindShort\n" + "private short numberShort;\n" + "```\n" + "\n" + "\n" + "<a name=\"annotation-bindint\"></a>\n" + "### BindInt\n" + "\n" + "#### parameters\n" + " - `condition`: The SpEL expression that determines if this field has to be read.\n" + " - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.\n" + " - `validator`: the Class of a validator (applied BEFORE the converter).\n" + " - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. \n" + " - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).\n" + "\n" + "#### description\n" + "Reads an int (or Integer).\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "@BindInt\n" + "private int numberInt;\n" + "```\n" + "\n" + "\n" + "<a name=\"annotation-bindlong\"></a>\n" + "### BindLong\n" + "\n" + "#### parameters\n" + " - `condition`: The SpEL expression that determines if this field has to be read.\n" + " - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.\n" + " - `validator`: the Class of a validator (applied BEFORE the converter).\n" + " - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. \n" + " - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).\n" + "\n" + "#### description\n" + "Reads a long (or Long).\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "@BindLong\n" + "private long numberLong;\n" + "```\n" + "\n" + "\n" + "<a name=\"annotation-bindinteger\"></a>\n" + "### BindInteger\n" + "\n" + "#### parameters\n" + " - `condition`: The SpEL expression that determines if this field has to be read.\n" + " - `size`: the number of bits to read (can be a SpEL expression).\n" + " - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.\n" + " - `validator`: the Class of a validator (applied BEFORE the converter).\n" + " - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. \n" + " - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).\n" + "\n" + "#### description\n" + "Reads a long number (primitive or not) or a BigInteger given the amount of bits.\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "@BindInteger(size = \"3\")\n" + "private BigInteger number;\n" + "\n" + "@BindInteger(size = \"Long.SIZE+10\")\n" + "private BigInteger number;\n" + "```\n" + "\n" + "\n" + "<a name=\"annotation-bindfloat\"></a>\n" + "### BindFloat\n" + "\n" + "#### parameters\n" + " - `condition`: The SpEL expression that determines if this field has to be read.\n" + " - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.\n" + " - `validator`: the Class of a validator (applied BEFORE the converter).\n" + " - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. \n" + " - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).\n" + "\n" + "#### description\n" + "Reads a float (or Float).\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "@BindFloat\n" + "private float number;\n" + "```\n" + "\n" + "\n" + "<a name=\"annotation-binddouble\"></a>\n" + "### BindDouble\n" + "\n" + "#### parameters\n" + " - `condition`: The SpEL expression that determines if this field has to be read.\n" + " - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.\n" + " - `validator`: the Class of a validator (applied BEFORE the converter).\n" + " - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. \n" + " - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).\n" + "\n" + "#### description\n" + "Reads a double (or Double).\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "@BindDouble\n" + "private double number;\n" + "```\n" + "\n" + "\n" + "<a name=\"annotation-binddecimal\"></a>\n" + "### BindDecimal\n" + "\n" + "#### parameters\n" + " - `condition`: The SpEL expression that determines if this field has to be read.\n" + " - `type`: the Class of variable to be read (SHOULD BE `Float.class`, or `Double.class`).\n" + " - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.\n" + " - `validator`: the Class of a validator (applied BEFORE the converter).\n" + " - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. \n" + " - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).\n" + "\n" + "#### description\n" + "Reads a float or decimal (or Float or Double), depending on `type`, as a BigDecimal.\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "@BindDecimal(type = Double.class)\n" + "private BigDecimal number;\n" + "```\n" + "\n" + "\n" + "<a name=\"annotation-bindstring\"></a>\n" + "### BindString\n" + "\n" + "#### parameters\n" + " - `condition`: The SpEL expression that determines if this field has to be read.\n" + " - `charset`: the charset to be interpreted the string into (SHOULD BE the charset name, eg. `UTF-8` (the default), `ISO-8859-1`, etc).\n" + " - `size`: the size of the string (can be a SpEL expression).\n" + " - `validator`: the Class of a validator (applied BEFORE the converter).\n" + " - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. \n" + " - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).\n" + "\n" + "#### description\n" + "Reads a String.\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "@BindString(size = \"4\")\n" + "public String text;\n" + "```\n" + "\n" + "\n" + "<a name=\"annotation-bindstringterminated\"></a>\n" + "### BindStringTerminated\n" + "\n" + "#### parameters\n" + " - `condition`: The SpEL expression that determines if this field has to be read.\n" + " - `charset`: the charset to be interpreted the string into (SHOULD BE the charset name, eg. `UTF-8` (the default), `ISO-8859-1`, etc).\n" + " - `terminator`: the byte that terminates the string (defaults to `\\0`).\n" + " - `consumeTerminator`: whether to consume the terminator (defaults to `true`).\n" + " - `validator`: the Class of a validator (applied BEFORE the converter).\n" + " - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. \n" + " - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).\n" + "\n" + "#### description\n" + "Reads a String.\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "@BindStringTerminated(terminator = ',')\n" + "public String text;\n" + "```\n" + "\n" + "<br/>\n" + "\n" + "<a name=\"annotation-special\"></a>\n" + "## Special annotations\n" + "Here are described the build-in special annotations.\n" + "\n" + "<a name=\"annotation-messageheader\"></a>\n" + "### MessageHeader\n" + "\n" + "#### parameters\n" + " - `start`: an array of possible start sequences (as string) for this message (defaults to empty).\n" + " - `end`: a possible end sequence (as string) for this message (default to empty).\n" + " - `charset`: the charset to be interpreted the `start` and `end` strings into (SHOULD BE the charset name, eg. `UTF-8` (the default), `ISO-8859-1`, etc).\n" + "\n" + "#### description\n" + "Marks a POJO as an annotated message.\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a class.\n" + "\n" + "#### example\n" + "```java\n" + "@MessageHeader(start = \"+\", end = \"-\")\n" + "private class Message{\n" + "    ...\n" + "}\n" + "```\n" + "\n" + "\n" + "<a name=\"annotation-skip\"></a>\n" + "### Skip\n" + "\n" + "#### parameters\n" + " - `condition`: The SpEL expression that determines if this field has to be read.\n" + " - `size`: the number of bits to be skipped (can be a SpEL expression).\n" + " - `terminator`: the byte that terminates the skip (defaults to `\\0`).\n" + " - `consumeTerminator`: whether to consume the terminator (defaults to `true`).\n" + "\n" + "#### description\n" + "Skips `size` bits, or until a terminator is found.\n" + "\n" + "If this should be placed at the end of the message, then a placeholder variable (that WILL NOT be read, and thus can be of any type) should be added.\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "@Skip(size = \"3\")\n" + "@Skip(size = \"1\")\n" + "@BindString(size = \"4\")\n" + "public String text1;\n" + "\n" + "@Skip(terminator = 'x', consumeTerminator = false)\n" + "@BindString(size = \"10\")\n" + "public String text2;\n" + "\n" + "\n" + "@Skip(size = \"10\")\n" + "public Void lastUnreadPlaceholder;\n" + "```\n" + "\n" + "\n" + "<a name=\"annotation-checksum\"></a>\n" + "### Checksum\n" + "\n" + "#### parameters\n" + " - `type`: the Class of variable to be read.\n" + " - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN` (used for primitives other than `byte`).\n" + " - `skipStart`: how many bytes are to be skipped from the start of the message for the calculation of the checksum (defaults to 0).\n" + " - `skipEnd`: how many bytes are to be skipped from the end of the message for the calculation of the checksum (default to 0).\n" + " - `algorithm`: the algorithm to be applied to calculate the checksum.\n" + "\n" + "#### description\n" + "Reads a checksum.\n" + "\n" + "Compute the message checksum and compare it to the read variable once a message has been completely read.\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "@Checksum(type = short.class, skipStart = 4, skipEnd = 4, algorithm = CRC16.class, startValue = CRC16.START_VALUE_0xFFFF)\n" + "private short checksum;\n" + "```\n" + "\n" + "\n" + "<a name=\"annotation-evaluate\"></a>\n" + "### Evaluate\n" + "\n" + "#### parameters\n" + " - `condition`: The SpEL expression that determines if this field has to be read.\n" + " - `value`: the value to be assigned, or calculated (can be a SpEL expression).\n" + "\n" + "#### description\n" + "Assign a constant, calculated value to a field.\n" + "\n" + "Note that the evaluations are done AFTER parsing the entire message.\n" + "\n" + "#### annotation type\n" + "This annotation is bounded to a variable.\n" + "\n" + "#### example\n" + "```java\n" + "@BindString(size = \"4\")\n" + "private String messageHeader;\n" + "\n" + "@Evaluate(\"T(java.time.ZonedDateTime).now()\")\n" + "private ZonedDateTime receptionTime;\n" + "\n" + "@Evaluate(\"messageHeader.startsWith('+B')\")\n" + "private boolean buffered;\n" + "\n" + "//from the variable `deviceTypes` passed in the context\n" + "@Evaluate(\"#deviceTypes.getDeviceTypeName(deviceTypeCode)\")\n" + "private String deviceTypeName;\n" + "```\n" + "\n" + "\n" + "<br/>\n" + "\n" + "<a name=\"how-to-spel\"></a>\n" + "## How to write SpEL expressions\n" + "Care should be taken in writing [SpEL expressions](https://docs.spring.io/spring/docs/4.3.10.RELEASE/spring-framework-reference/html/expressions.html) for the fields `condition`, and `size`.\n" + "\n" + "The root object is the outermost object. In order to evaluate a variable of a parent object the complete path should be used, as in `object1.variable1`. In order to evaluate a variable of a children object, that is the object currently scanned, the relative path should be used using by the special keyword `#self`, as in `#self.variable2`).\n" + "\n" + "See also [Spring Expression Language (SpEL) Primer](https://dhruba.wordpress.com/2009/12/30/spring-expression-language-spel-primer/).\n" + "\n" + "### Example:\n" + "```java\n" + "class A{\n" + "    @BindByte\n" + "    private byte value;\n" + "\n" + "    @BindObject(type = OtherClass.class)\n" + "    private OtherClass other;\n" + "\n" + "    @BindString(condition = \"value == 2\", size = \"1\")\n" + "    private String var3;\n" + "}\n" + "\n" + "class OtherClass{\n" + "    @BindString(condition = \"value == 1\", size = \"1\")\n" + "    private String var1;\n" + "    @BindString(condition = \"#self.var1.equals('2')\", size = \"1\")\n" + "    private String var2;\n" + "}\n" + "```\n" + "\n" + "<br/>\n" + "\n" + "<a name=\"how-to-extend\"></a>\n" + "## How to extend the functionalities\n" + "Boxon can handle array of primitives, bit, byte, short, int, long, float, double, and their object counterpart, as long as Object, BigInteger, BigDecimal, string (with a given size, or with a terminator), and the special \"[checksum](#annotation-checksum)\".\n" + "\n" + "You can extend the basic functionalities through the application of converters as shown below in some examples. Here lies the power of Boxon.\n" + "\n" + "Boxon already provides some build-in converters: BitToBoolean, ShortToChar, UnsignedByte, UnsignedInt, and UnsignedShort.\n" + "\n" + "<a name=\"how-to-converters\"></a>\n" + "### Converters\n" + "NOTE that `decode` and `encode` MUST BE one the inverse of the other, that is they MUST BE invertible (injective), or partly invertible, that is, otherwise said, `decode(x) = y iff encode(y) = x` (eventually in a restricted domain).\n" + "\n" + "#### DateTime converter (from Unix timestamp to ZonedDateTime)\n" + "```java\n" + "@BindLong(converter = DateTimeUnixConverter.class)\n" + "private ZonedDateTime eventTime;\n" + "\n" + "public class DateTimeUnixConverter implements Converter<Long, ZonedDateTime>{\n" + "    @Override\n" + "    public ZonedDateTime decode(final Long unixTimestamp){\n" + "        return DateTimeUtils.createFrom(unixTimestamp);\n" + "    }\n" + "\n" + "    @Override\n" + "    public Long encode(final ZonedDateTime value){\n" + "        return value.toEpochSecond();\n" + "    }\n" + "}\n" + "```\n" + "\n" + "#### DateTime converter (from YYYYMMDDHHMMSS as bytes to ZonedDateTime)\n" + "```java\n" + "@BindArrayPrimitive(size = \"7\", type = byte.class, converter = DateTimeYYYYMMDDHHMMSSConverter.class)\n" + "private ZonedDateTime eventTime;\n" + "\n" + "public class DateTimeYYYYMMDDHHMMSSConverter implements Converter<byte[], ZonedDateTime>{\n" + "    @Override\n" + "    public ZonedDateTime decode(final byte[] value){\n" + "        final ByteBuffer bb = ByteBuffer.wrap(value);\n" + "        final int year = bb.getShort();\n" + "        final int month = bb.get();\n" + "        final int dayOfMonth = bb.get();\n" + "        final int hour = bb.get();\n" + "        final int minute = bb.get();\n" + "        final int second = bb.get();\n" + "        return DateTimeUtils.createFrom(year, month, dayOfMonth, hour, minute, second);\n" + "    }\n" + "\n" + "    @Override\n" + "    public byte[] encode(final ZonedDateTime value){\n" + "        return ByteBuffer.allocate(7)\n" + "            .putShort((short)value.getYear())\n" + "            .put((byte)value.getMonthValue())\n" + "            .put((byte)value.getDayOfMonth())\n" + "            .put((byte)value.getHour())\n" + "            .put((byte)value.getMinute())\n" + "            .put((byte)value.getSecond())\n" + "            .array();\n" + "    }\n" + "}\n" + "```\n" + "\n" + "#### IMEI converter (from 'nibble' array to String, that is, each nibble represents a character of the IMEI)\n" + "```java\n" + "@BindArrayPrimitive(size = \"8\", type = byte.class, converter = IMEIConverter.class, validator = IMEIValidator.class)\n" + "private String imei;\n" + "\n" + "public class IMEIConverter implements Converter<byte[], String>{\n" + "    @Override\n" + "    public String decode(final byte[] value){\n" + "        final StringBuilder sb = new StringBuilder();\n" + "        for(int i = 0; i < 7; i ++)\n" + "            sb.append(String.format(\"%02d\", value[i] & 255));\n" + "        sb.append(ByteHelper.applyMaskAndShift(value[7], Byte.SIZE, (byte)0x0F));\n" + "        return sb.toString();\n" + "    }\n" + "\n" + "    @Override\n" + "    public byte[] encode(final String value){\n" + "        final byte[] imei = new byte[8];\n" + "        final String[] components = value.split(\"(?<=\\\\G\\\\d{2})\", 8);\n" + "        for(int i = 0; i < 8; i ++)\n" + "            imei[i] = Integer.valueOf(components[i]).byteValue();\n" + "        return imei;\n" + "    }\n" + "}\n" + "```\n" + "\n" + "#### RSSI converter (from encoded byte to short value)\n" + "```java\n" + "@BindByte(converter = RSSIConverter.class)\n" + "private short rssi;\n" + "\n" + "/**\n" + " * input:\toutput:\n" + " * -----------------------\n" + " * 0:\t\t< -133 dBm\n" + " * 1:\t\t-111 dBm\n" + " * 2-30:\t-109 - -53 dBm\n" + " * 31:\t\t> -51 dBm\n" + " * 99:\t\tunknown\n" + " */\n" + "public class RSSIConverter implements Converter<Byte, Short>{\n" + "\n" + "    public static final int RSSI_UNKNOWN = 0;\n" + "\n" + "    @Override\n" + "    public Short decode(final Byte value){\n" + "        if(value == 0)\n" + "            //< -133 dBm\n" + "            return (byte)-133;\n" + "        if(value == 99)\n" + "            return RSSI_UNKNOWN;\n" + "        //31 is > -51 dBm\n" + "        return (short)(value * 2 - 113);\n" + "    }\n" + "\n" + "    @Override\n" + "    public Byte encode(final Short value){\n" + "        if(value == -133)\n" + "            return 0;\n" + "        if(value == RSSI_UNKNOWN)\n" + "            return 99;\n" + "        return (byte)((value + 133) / 2);\n" + "    }\n" + "}\n" + "```\n" + "\n" + "<a name=\"how-to-annotations\"></a>\n" + "### Custom annotations\n" + "You can also define your own annotation by define an annotation and implementing `CodecInterface` as in the following example.\n" + "\n" + "Optionally, the method `String condition()` could be defined.\n" + "\n" + "<b>... and remember to add it to the `Parser`!</b>\n" + "\n" + "```java\n" + "//annotation\n" + "@Retention(RetentionPolicy.RUNTIME)\n" + "@Target(ElementType.FIELD)\n" + "@interface VarLengthEncoded{}\n" + "```\n" + "```java\n" + "//codec\n" + "//the number of bytes to read is determined by the leading bit of each individual bytes\n" + "//(if the first bit of a byte is 1, then another byte is expected to follow)\n" + "class VariableLengthByteArray implements CodecInterface<VarLengthEncoded>{\n" + "    public Object decode(TemplateParser templateParser, BitBuffer reader, VarLengthEncoded annotation, Object data){\n" + "        final ByteArrayOutputStream baos = new ByteArrayOutputStream();\n" + "        boolean continuing = true;\n" + "        while(continuing){\n" + "            final byte b = reader.getByte();\n" + "            baos.write(b & 0x7F);\n" + "\n" + "            continuing = ((b & 0x80) != 0x00);\n" + "        }\n" + "        return baos.toByteArray();\n" + "    }\n" + "\n" + "    public void encode(TemplateParser templateParser, BitWriter writer, VarLengthEncoded annotation, Object data, Object value){\n" + "        final int size = Array.getLength(value);\n" + "        for(int i = 0; i < size; i ++)\n" + "            writer.put((byte)((byte)Array.get(value, i) | (i < size - 1? (byte)0x80: 0x00)), ByteOrder.BIG_ENDIAN);\n" + "    }\n" + "}\n" + "```\n" + "\n" + "```java\n" + "//add the custom codec to the list of available codecs\n" + "//(use one of the lines below)\n" + "parser.withDefaultCodecs(); //loads all codecs from the package where this call was made\n" + "parser.withCodecs(CodecCustomTest.class); //this class is where the custom codec resides\n" + "parser.withCodecs(new VariableLengthByteArray());\n" + "```\n" + "\n" + "<br/>\n" + "\n" + "<a name=\"digging\"></a>\n" + "## Digging into the code\n" + "Almost for each base annotation there is a corresponding class defined into `Template.java` that manages the encoding and decoding of the underlying data.\n" + "\n" + "The other annotations are managed directly into `TemplateParser.java`, that is the main class that orchestrates the parsing of a single message with all of its annotations.\n" + "If an error occurs an `AnnotationException` (an error occurs on an annotation definition), `CodecException` (an error occurs while finding the appropriate codec), `TemplateException` (an error occurs if a template class is badly annotated), or `DecodeException`/`EncodeException` (a container exception for the previous ones for decoding and encoding respectively) is thrown.\n" + "\n" + "Messages can be concatenated, and the `Parser.java` class manages them, returning a [DTO](https://en.wikipedia.org/wiki/Data_transfer_object), `ParseResponse.java`, which contains a list of all successfully read messages and a list of all errors from problematic messages.\n" + "\n" + "<br/>\n" + "\n" + "Each annotated class is processed by `Template.class`, that is later retrieved by `Parser.java` depending on the starting header.\n" + "For that reason each starting header defined into `MessageHeader` annotation MUST BE unique. This class can also accept a context.\n" + "\n" + "All the SpEL expressions are evaluated by `Evaluator.java`.\n" + "\n" + "<br/>\n" + "\n" + "All the annotated classes are conveniently loaded using the `Loader.java` as is done automatically in the `Parser.java`.\n" + "\n" + "Note that all codecs MUST BE loaded before the templates that use them, as they are used to verifying the annotations. \n" + "\n" + "If you want to provide your own classes you can use the appropriate `with...` method of `Parser`.\n" + "\n" + "<br/>\n" + "\n" + "The `Parser` is also used to encode a message.\n" + "\n" + "<br/>\n" + "\n" + "`BitBuffer.java` has the task to read the bits, whereas `BitWriter.java` has the task to write the bits.\n" + "\n" + "<br/>\n" + "\n" + "`BitSet.java` is the container for the bits (like `java.utils.BitSet`, but enhanced for speed).\n" + "\n" + "<br/>\n" + "\n" + "`ByteOrder.java` is the enum that is used to indicate the byte order.\n" + "\n" + "\n" + "<br/>\n" + "\n" + "<a name=\"examples\"></a>\n" + "## Examples\n" + "\n" + "<a name=\"example-multi\"></a>\n" + "### Multi-message parser\n" + "\n" + "All you have to care about, for a simple example on multi-message automatically-loaded templates, is the `Parser`.\n" + "```java\n" + "//optionally create a context\n" + "Map<String, Object> context = ...\n" + "//read all the codecs and annotated classes from where the parser resides and all of its children packages\n" + "Parser parser = Parser.create()\n" + "   .withContext(context);\n" + "//... or pass the parent package (see all the `with...` methods of Parser for more)\n" + "Parser parser = Parser.create()\n" + "   .withContext(context)\n" + "   .withContextFunction(VersionHelper.class, \"compareVersion\", String.class, String.class)\n" + "   .withContextFunction(VersionHelper.class.getDeclaredMethod(\"compareVersion\", new Class[]{String.class, String.class}))\n" + "   //scans the parent package and all of its children, searching and loading all the codecs found\n" + "   .withDefaultCodecs()\n" + "   //scans the parent package and all of its children, searching and loading all the templates found\n" + "   .withDefaultTemplates();\n" + "\n" + "//parse the message\n" + "byte[] payload = ...\n" + "ParseResponse result = parser.parse(payload);\n" + "\n" + "//process the errors\n" + "for(int index = 0; index < result.getErrorCount(); index ++)\n" + "   LOGGER.error(\"An error occurred while parsing:\\r\\n   {}\", result.getMessageForError(index));\n" + "\n" + "//process the successfully parsed messages\n" + "for(int index = 0; index < result.getParsedMessageCount(); index ++){\n" + "    Object parsedMessage = result.getParsedMessageAt(index);\n" + "    ...\n" + "}\n" + "```\n" + "\n" + "or, if you want to pass your templates by hand:\n" + "```java\n" + "//optionally create a context ('null' otherwise)\n" + "Map<String, Object> context = ...\n" + "Template<Message> template = Template.createFrom(Message.class);\n" + "Parser parser = Parser.create()\n" + "   .withTemplates(template);\n" + "\n" + "//parse the message\n" + "byte[] payload = ...\n" + "ParseResponse result = parser.parse(payload);\n" + "```\n" + "\n" + "<a name=\"example-composer\"></a>\n" + "### Message composer\n" + "\n" + "The inverse of parsing is composing, and it's simply done as follows.\n" + "```java\n" + "//compose the message\n" + "Message data = ...;\n" + "ComposeResponse composeResult = parser.compose(data);\n" + "\n" + "//process the read messages\n" + "if(!composeResult.hasErrors()){\n" + "    byte[] message = result.getComposedMessage();\n" + "    ...\n" + "}\n" + "//process the errors\n" + "else{\n" + "    for(int i = 0; i < result.getErrorCount(); i ++){\n" + "        EncodeException exc = result.getErrorAt(i);\n" + "        ...\n" + "    }\n" + "}\n" + "```\n" + "\n" + "Remember that the header that will be written is the first in `@MessageHeader`.\n" + "\n" + "\n" + "<br/>\n" + "\n" + "<a name=\"changelog\"></a>\n" + "## Changelog\n" + "\n" + "<a name=\"changelog-1.1.0\"></a>\n" + "### version 1.1.0 - 20200901\n" + "- Better handling of NOP logger.\n" + "- Abandoned [Reflections](https://github.com/ronmamo/reflections) in favor of [ClassGraph](https://github.com/classgraph/classgraph).\n" + "- Added BindArray.selectDefault and BindObject.selectDefault to cope with default selector that has no prefix.\n" + "- Added some feasibility checks on annotation data.\n" + "- Added public constructor to Parser to allow for extensions.\n" + "- Changed the signature of Checksummer.calculateChecksum returning short instead of long.\n" + "- Changed method Validator.validate into Validator.isValid.\n" + "- Changed method ParseResponse.getMessageForError into ParseResponse.getErrorMessageAt to align it to other method name's conventions.\n" + "- Moved classes ParseResponse and ComposeResponse from io.github.mtrevisan.boxon.external to io.github.mtrevisan.boxon.codecs in order to hide add methods; the constructors are also hidden.\n" + "- Minor refactorings.\n" + "- Added `originator` variable (and its getter) to ComposeResponse to hold the given objects used to create the message.\n" + "- Added/modified javadocs to better explain some classes.\n" + "- Removed ComposeResponse.getErrors, BindInteger.unsigned and BitReader.getInteger(int, ByteOrder, boolean) as they are useless.\n" + "- Removed BitWriter.putText(String, byte, boolean) because of the [Boolean Trap](https://ariya.io/2011/08/hall-of-api-shame-boolean-trap).\n" + "- Removed useless `match()` parameter from bindings.\n" + "- Enhanced the exception message thrown if the type of BitReader.get(Class, ByteOrder) is not recognized.\n" + "- Renamed BindChecksum into Checksum.\n" + "- Relocated all binding annotations inside annotations.bindings (Bind* and *Choices).\n" + "- Corrected bug while reading skips in TemplateParser.decode.\n" + "\n" + "<a name=\"changelog-1.0.0\"></a>\n" + "### version 1.0.0 - 20200825\n" + "- Speed-up execution.\n" + "- Revision of the packages with removal of cycles.\n" + "- Better handling of class retrieval (codecs and templates).\n" + "\n" + "<a name=\"changelog-0.0.2\"></a>\n" + "### version 0.0.2 - 20200731\n" + "- Final revision.\n" + "\n" + "<a name=\"changelog-0.0.1\"></a>\n" + "### version 0.0.1 - 20200721\n" + "- First revision.\n" + "- Some more thoughts on how it should work.\n" + "\n" + "<a name=\"changelog-0.0.0\"></a>\n" + "### version 0.0.0 - 20200629\n" + "- First version.\n" + "\n" + "\n" + "<br/>\n" + "\n" + "<a name=\"license\"></a>\n" + "## License\n" + "This project is licensed under [MIT license](http://opensource.org/licenses/MIT).\n" + "For the full text of the license, see the [LICENSE](LICENSE) file.\n" + "\n" + "<a name=\"attributions\"></a>\n" + "## Attributions\n" + "Logo for the project by TimothyRias - Own work, CC BY 3.0, [https://commons.wikimedia.org/w/index.php?curid=4943351](https://commons.wikimedia.org/w/index.php?curid=4943351).";
			dialog.loadData(note);

			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(1000, 500);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
