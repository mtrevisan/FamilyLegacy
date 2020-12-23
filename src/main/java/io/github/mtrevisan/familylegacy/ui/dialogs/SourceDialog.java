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

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.LocaleFilteredComboBox;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewPane;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.StringJoiner;


public class SourceDialog extends JDialog implements TextPreviewListenerInterface{

	private static final double DATE_HEIGHT = 17.;
	private static final double DATE_ASPECT_RATIO = 270 / 248.;
	private static final Dimension DATE_SIZE = new Dimension((int)(DATE_HEIGHT / DATE_ASPECT_RATIO), (int)DATE_HEIGHT);

	//https://thenounproject.com/term/weekly-calendar/541199/
	private static final ImageIcon DATE = ResourceHelper.getImage("/images/date.png", DATE_SIZE);

	private static final DefaultComboBoxModel<String> CREDIBILITY_MODEL = new DefaultComboBoxModel<>(new String[]{
		StringUtils.EMPTY,
		"Unreliable/estimated data",
		"Questionable reliability of evidence",
		"Secondary evidence, data officially recorded sometime after event",
		"Direct and primary evidence used, or by dominance of the evidence"});
	private static final DefaultComboBoxModel<String> EXTRACT_TYPE_MODEL = new DefaultComboBoxModel<>(new String[]{StringUtils.EMPTY,
		"transcript", "extract", "abstract"});

	private final JLabel eventLabel = new JLabel("Event(s):");
	private final JTextField eventField = new JTextField();
	private final JLabel titleLabel = new JLabel("Title:");
	private final JTextField titleField = new JTextField();
	private final JLabel authorLabel = new JLabel("Author:");
	private final JTextField authorField = new JTextField();
	private final JLabel publicationFactsLabel = new JLabel("Publication facts:");
	private final JTextField publicationFactsField = new JTextField();
	private final JLabel dateLabel = new JLabel("Date:");
	private final JTextField dateField = new JTextField();
	private final JButton dateButton = new JButton(DATE);
	private final JLabel dateOriginalTextLabel = new JLabel("Original text:");
	private final JTextField dateOriginalTextField = new JTextField();
	private final JLabel dateCredibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> dateCredibilityComboBox = new JComboBox<>(CREDIBILITY_MODEL);
	private final LocaleFilteredComboBox extractLocaleComboBox = new LocaleFilteredComboBox();
	private final JLabel extractTypeLabel = new JLabel("Extract type:");
	private final JLabel extractLocaleLabel = new JLabel("Locale:");
	private final JComboBox<String> extractTypeComboBox = new JComboBox<>(EXTRACT_TYPE_MODEL);
	private TextPreviewPane textPreviewView;
	private final JButton repositoriesButton = new JButton("Repositories");
	private final JButton filesButton = new JButton("Files");
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

		eventLabel.setLabelFor(eventField);

		titleLabel.setLabelFor(titleField);

		dateLabel.setLabelFor(dateField);

		dateCredibilityLabel.setLabelFor(dateCredibilityComboBox);

		final JPanel datePanel = new JPanel();
		datePanel.setBorder(BorderFactory.createTitledBorder("Date"));
		datePanel.setLayout(new MigLayout("", "[grow]"));
		datePanel.add(dateLabel, "align label,split 3,sizegroup label");
		datePanel.add(dateField, "grow");
		datePanel.add(dateButton, "wrap");
		datePanel.add(dateOriginalTextLabel, "align label,split 2,sizegroup label");
		datePanel.add(dateOriginalTextField, "grow,wrap");
		datePanel.add(dateCredibilityLabel, "align label,split 2,sizegroup label");
		datePanel.add(dateCredibilityComboBox, "grow");

		extractTypeLabel.setLabelFor(extractTypeComboBox);

		extractLocaleLabel.setLabelFor(extractLocaleComboBox);

		textPreviewView = new TextPreviewPane(this);

		final JPanel extractPanel = new JPanel();
		extractPanel.setBorder(BorderFactory.createTitledBorder("Extract"));
		extractPanel.setLayout(new MigLayout("", "[grow]"));
		extractPanel.add(extractTypeLabel, "align label,split 2,sizegroup label");
		extractPanel.add(extractTypeComboBox, "wrap");
		extractPanel.add(extractLocaleLabel, "align label,split 2,sizegroup label");
		extractPanel.add(extractLocaleComboBox, "wrap");
		extractPanel.add(textPreviewView, "span 2,grow");

		repositoriesButton.setEnabled(false);
		repositoriesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.REPOSITORY_CITATION, source)));

		filesButton.setEnabled(false);
		filesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.FILE_CITATION, source)));

		notesButton.setEnabled(false);
		notesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, source)));

		okButton.addActionListener(evt -> {
			final String event = eventField.getText();
			final String title = titleField.getText();
			final String extractType = (extractTypeComboBox.getSelectedIndex() > 0?
				Integer.toString(extractTypeComboBox.getSelectedIndex() + 1): null);
			final String extractLanguageTag = ((LocaleFilteredComboBox.FlefLocale)extractLocaleComboBox.getModel().getSelectedItem())
				.toLanguageTag();
			final String extract = textPreviewView.getText();

			source.replaceChildValue("EVENT", event);
			source.replaceChildValue("TITLE", title);
			source.replaceChildValue("EXTRACT", extract);
			final GedcomNode extractLocaleNode = store.traverse(source, "EXTRACT.LOCALE");
			if(!extractLocaleNode.isEmpty())
				extractLocaleNode.withValue(extractLanguageTag);
			final GedcomNode extractTypeNode = store.traverse(source, "EXTRACT.TYPE");
			if(!extractTypeNode.isEmpty())
				extractTypeNode.withValue(extractType);

			if(onCloseGracefully != null)
				onCloseGracefully.run();

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());


		setLayout(new MigLayout("", "[grow]"));
		add(eventLabel, "align label,split 2");
		add(eventField, "grow,wrap");
		add(titleLabel, "align label,split 2");
		add(titleField, "grow,wrap");
		add(authorLabel, "align label,split 2");
		add(authorField, "grow,wrap");
		add(publicationFactsLabel, "align label,split 2");
		add(publicationFactsField, "grow,wrap paragraph");
		add(datePanel, "grow,wrap paragraph");
		add(repositoriesButton, "sizegroup button2,grow,wrap");
		add(filesButton, "sizegroup button2,grow,wrap");
		add(notesButton, "sizegroup button2,grow,wrap paragraph");
		add(okButton, "tag ok,span,split 2,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	@Override
	public void onPreviewStateChange(final boolean previewVisible){
		TextPreviewListenerInterface.centerDivider(this, previewVisible);
	}

	public void loadData(final GedcomNode source, final Runnable onCloseGracefully){
		this.source = source;
		this.onCloseGracefully = onCloseGracefully;

		setTitle("Source " + source.getID());

		final StringJoiner events = new StringJoiner(", ");
		for(final GedcomNode event : store.traverseAsList(source, "EVENT[]"))
			events.add(event.getValue());
		final String title = store.traverse(source, "TITLE").getValue();
		final String author = store.traverse(source, "AUTHOR").getValue();
		final String publicationFacts = store.traverse(source, "PUBLICATION_FACTS").getValue();
		final GedcomNode dateNode = store.traverse(source, "DATE");
		final GedcomNode place = store.traverse(source, "PLACE");
		final GedcomNode placeCertainty = store.traverse(source, "PLACE.CERTAINTY");
		final GedcomNode placeCredibility = store.traverse(source, "PLACE.CREDIBILITY");
		final boolean hasRepositories = !store.traverseAsList(source, "REPOSITORY[]").isEmpty();
		final String mediaType = store.traverse(source, "MEDIA_TYPE").getValue();
		final boolean hasFiles = !store.traverseAsList(source, "FILE[]").isEmpty();
		final boolean hasSources = !store.traverseAsList(source, "SOURCE[]").isEmpty();
		final boolean hasNotes = !store.traverseAsList(source, "NOTE[]").isEmpty();

		eventField.setText(events.toString());
		titleField.setText(title);
		authorField.setText(author);
		publicationFactsField.setText(publicationFacts);
		dateField.setText(dateNode.getValue());
//		textPreviewView.setText(getTitle(), extractLanguageTag, extract);
//		extractTypeComboBox.setSelectedItem(extractType);
//		extractLocaleComboBox.setSelectedByLanguageTag(extractLanguageTag);
		repositoriesButton.setEnabled(hasRepositories);
		filesButton.setEnabled(hasFiles);
		notesButton.setEnabled(hasNotes);

		repaint();
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Flef store = new Flef();
		store.load("/gedg/flef_0.0.5.gedg", "src/main/resources/ged/small.flef.ged")
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
			dialog.setSize(500, 460);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
