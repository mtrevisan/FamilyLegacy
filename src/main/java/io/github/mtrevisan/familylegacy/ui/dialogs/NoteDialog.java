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
import io.github.mtrevisan.familylegacy.services.JavaHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.FileHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.LocaleFilteredComboBox;
import io.github.mtrevisan.familylegacy.ui.utilities.PopupMouseAdapter;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewPane;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class NoteDialog extends JDialog implements TextPreviewListenerInterface{

	private static final DefaultComboBoxModel<String> RESTRICTION_MODEL = new DefaultComboBoxModel<>(new String[]{StringUtils.EMPTY,
		"confidential", "locked", "private"});


	private TextPreviewPane textPreviewView;
	private final JLabel localeLabel = new JLabel("Locale:");
	private final LocaleFilteredComboBox localeComboBox = new LocaleFilteredComboBox();
	private final JLabel urlLabel = new JLabel("URL:");
	private final JTextField urlField = new JTextField();
	private final JMenuItem testLinkItem = new JMenuItem("Test link");
	private final JMenuItem openLinkItem = new JMenuItem("Open link…");
	private final JLabel restrictionLabel = new JLabel("Restriction:");
	private final JComboBox<String> restrictionComboBox = new JComboBox<>(RESTRICTION_MODEL);
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private GedcomNode note;
	private Runnable onCloseGracefully;
	private final Flef store;


	public NoteDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}

	private void initComponents(){
		setTitle("Note");

		textPreviewView = new TextPreviewPane(this);

		localeLabel.setLabelFor(localeComboBox);

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
			final String text = textPreviewView.getText();
			note.withValue(text);

			if(onCloseGracefully != null)
				onCloseGracefully.run();

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());


		setLayout(new MigLayout("", "[grow]"));
		add(textPreviewView, "span 2,grow,wrap");
		add(localeLabel, "align label,split 2,sizegroup label");
		add(localeComboBox, "wrap");
		add(urlLabel, "align label,split 2,sizegroup label");
		add(urlField, "grow,wrap");
		add(restrictionLabel, "align label,split 2,sizegroup label");
		add(restrictionComboBox, "wrap paragraph");
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
	public void onPreviewStateChange(final boolean previewVisible){
		TextPreviewListenerInterface.centerDivider(this, previewVisible);
	}

	public void loadData(final GedcomNode note, final Runnable onCloseGracefully){
		this.note = note;
		this.onCloseGracefully = onCloseGracefully;

		setTitle("Note " + note.getID());

		final String languageTag = store.traverse(note, "LOCALE").getValue();
		final String text = note.getValue();
		//FIXME this is an array
		final String url = store.traverse(note, "URL").getValue();
		final String restriction = store.traverse(note, "RESTRICTION").getValue();

		textPreviewView.setText(getTitle(), languageTag, text);

		localeComboBox.setSelectedByLanguageTag(languageTag);

		restrictionComboBox.setSelectedItem(restriction);

		testLinkItem.setEnabled(StringUtils.isNotBlank(url));
		openLinkItem.setEnabled(StringUtils.isNotBlank(url));

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
		final GedcomNode note = store.getNotes().get(0);

		EventQueue.invokeLater(() -> {
			final NoteDialog dialog = new NoteDialog(store, new JFrame());
			dialog.loadData(note, null);

			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(500, 400);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
