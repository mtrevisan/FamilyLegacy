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
import io.github.mtrevisan.familylegacy.services.JavaHelper;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.FileHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.LocaleFilteredComboBox;
import io.github.mtrevisan.familylegacy.ui.utilities.PopupMouseAdapter;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewPane;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class SourceDialog extends JDialog implements TextPreviewListenerInterface{

	private static final double DATE_HEIGHT = 17.;
	private static final double DATE_ASPECT_RATIO = 270 / 248.;
	private static final Dimension DATE_SIZE = new Dimension((int)(DATE_HEIGHT / DATE_ASPECT_RATIO), (int)DATE_HEIGHT);

	//https://thenounproject.com/term/weekly-calendar/541199/
	private static final ImageIcon DATE = ResourceHelper.getImage("/images/date.png", DATE_SIZE);

	private static final DefaultComboBoxModel<String> EXTRACT_TYPE_MODEL = new DefaultComboBoxModel<>(new String[]{StringUtils.EMPTY,
		"transcript", "extract", "abstract"});

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
	private TextPreviewPane textPreviewView;
	private final JLabel extractTypeLabel = new JLabel("Extract type:");
	private final JComboBox<String> extractTypeComboBox = new JComboBox<>(EXTRACT_TYPE_MODEL);
	private final JButton repositoriesButton = new JButton("Repositories");
	private final JButton filesButton = new JButton("Files");
	private final JLabel urlLabel = new JLabel("URL:");
	private final JTextField urlField = new JTextField();
	private final JMenuItem testLinkItem = new JMenuItem("Test link");
	private final JMenuItem openLinkItem = new JMenuItem("Open link…");
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
		dateField.setEditable(false);

		localeLabel.setLabelFor(localeComboBox);

		textPreviewView = new TextPreviewPane(this);

		extractTypeLabel.setLabelFor(extractTypeComboBox);

		final JPanel extractPanel = new JPanel();
		extractPanel.setBorder(BorderFactory.createTitledBorder("Extract"));
		extractPanel.setLayout(new MigLayout("", "[grow]"));
		extractPanel.add(textPreviewView, "span 2,grow,wrap");
		extractPanel.add(extractTypeLabel, "align label,split 2");
		extractPanel.add(extractTypeComboBox);

		repositoriesButton.setEnabled(false);
		repositoriesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.REPOSITORY_CITATION, source)));

		filesButton.setEnabled(false);
		filesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.FILE_CITATION, source)));

		urlLabel.setLabelFor(urlField);
		urlField.addKeyListener(new KeyAdapter(){
			@Override
			public void keyReleased(final KeyEvent event){
				super.keyReleased(event);

				final boolean enabled = StringUtils.isNotBlank(urlField.getText());
				testLinkItem.setEnabled(enabled);
				openLinkItem.setEnabled(enabled);
			}
		});
		//manage links
		attachOpenLinkPopUpMenu(urlField);

		notesButton.setEnabled(false);
		notesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, source)));

		okButton.addActionListener(evt -> {
			final String type = typeField.getText();
			final String title = titleField.getText();
			final String languageTag = ((LocaleFilteredComboBox.FlefLocale)localeComboBox.getModel().getSelectedItem()).toLanguageTag();
			final String extract = textPreviewView.getText();
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


		setLayout(new MigLayout("", "[grow]"));
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
		add(extractPanel, "grow,wrap paragraph");
		add(repositoriesButton, "sizegroup button2,grow,wrap");
		add(filesButton, "sizegroup button2,grow,wrap paragraph");
		add(urlLabel, "align label,split 2");
		add(urlField, "grow,wrap paragraph");
		add(notesButton, "sizegroup button2,grow,wrap paragraph");
		add(okButton, "tag ok,span,split 2,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	private void attachOpenLinkPopUpMenu(final JTextField component){
		final JPopupMenu popupMenu = new JPopupMenu();

		testLinkItem.addActionListener(event -> {
			final String url = component.getText();
			final boolean urlReachable = FileHelper.testURL(url);
			final String message = JavaHelper.format((urlReachable? "Success, the link `{}` is reachable.":
				"The connection attempt to `{}` failed."), url);
			JOptionPane.showMessageDialog(this, message, "Test link result",
				(urlReachable? JOptionPane.INFORMATION_MESSAGE: JOptionPane.ERROR_MESSAGE));
		});
		openLinkItem.addActionListener(event -> FileHelper.browseURL(component.getText()));
		popupMenu.add(testLinkItem);
		popupMenu.add(openLinkItem);

		component.addMouseListener(new PopupMouseAdapter(popupMenu, component));
	}

	@Override
	public void onPreviewVisibleStateChange(final boolean previewVisible){
		setSize((previewVisible? getWidth() * 2: getWidth() / 2), getHeight());
	}

	public void loadData(final GedcomNode source, final Runnable onCloseGracefully){
		this.source = source;
		this.onCloseGracefully = onCloseGracefully;

		setTitle("Source " + source.getID());

		final String type = store.traverse(source, "TYPE").getValue();
		final String title = store.traverse(source, "TITLE").getValue();
		final boolean hasEvents = !store.traverseAsList(source, "EVENT[]").isEmpty();
		final GedcomNode dateNode = store.traverse(source, "DATE");
		final String extract = store.traverse(source, "EXTRACT").getValue();
		final String extractType = store.traverse(source, "EXTRACT.TYPE").getValue();
		final String extractLanguageTag = store.traverse(source, "EXTRACT.LOCALE").getValue();
		final boolean hasRepositories = !store.traverseAsList(source, "REPOSITORY[]").isEmpty();
		final boolean hasFiles = !store.traverseAsList(source, "FILE[]").isEmpty();
		final String url = store.traverse(source, "URL").getValue();
		final boolean hasNotes = !store.traverseAsList(source, "NOTE[]").isEmpty();

		typeField.setText(type);
		titleField.setText(title);
		eventsButton.setEnabled(hasEvents);
		dateField.setText(dateNode.getValue());
		textPreviewView.setText(getTitle(), extractLanguageTag, extract);
		extractTypeComboBox.setSelectedItem(extractType);
		localeComboBox.setSelectedByLanguageTag(extractLanguageTag);
		repositoriesButton.setEnabled(hasRepositories);
		filesButton.setEnabled(hasFiles);
		urlField.setText(url);
		openLinkItem.setEnabled(StringUtils.isNotBlank(url));
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
		store.load("/gedg/flef_0.0.3.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		final GedcomNode source = store.getSources().get(1);

		EventQueue.invokeLater(() -> {
			final SourceDialog dialog = new SourceDialog(store, new JFrame());
			dialog.loadData(source, null);

			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(450, 700);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
