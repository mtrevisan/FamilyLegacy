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
import io.github.mtrevisan.familylegacy.ui.utilities.LocaleFilteredComboBox;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewPane;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;


public class NoteDialog extends JDialog implements TextPreviewListenerInterface{

	private TextPreviewPane textPreviewView;
	private final JLabel localeLabel = new JLabel("Locale:");
	private final LocaleFilteredComboBox localeComboBox = new LocaleFilteredComboBox();
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private GedcomNode note;
	private int noteHash;

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

		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
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
		add(restrictionCheckBox, "wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button");
		add(okButton, "tag ok,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	@Override
	public void textChanged(){
		final int newTextHash = textPreviewView.getText()
			.hashCode();
		final int newLanguageTagHash = localeComboBox.getSelectedLanguageTag()
			.hashCode();
		final int newRestrictionHash = (restrictionCheckBox.isSelected()? "confidential": StringUtils.EMPTY)
			.hashCode();
		final int newNoteHash = newTextHash ^ newLanguageTagHash ^ newRestrictionHash;

		okButton.setEnabled(newNoteHash != noteHash);
	}

	@Override
	public void onPreviewStateChange(final boolean visible){
		TextPreviewListenerInterface.centerDivider(this, visible);
	}

	public void loadData(final GedcomNode note, final Runnable onCloseGracefully){
		this.note = note;
		this.onCloseGracefully = onCloseGracefully;

		final String id = note.getID();
		setTitle(id != null? "Note " + id: "New Note");

		final String text = note.getValue();
		final String languageTag = store.traverse(note, "LOCALE").getValue();
		final String restriction = store.traverse(note, "RESTRICTION").getValue();

		final int textHash = Objects.requireNonNullElse(text, StringUtils.EMPTY).hashCode();
		final int languageTagHash = Objects.requireNonNullElse(languageTag, StringUtils.EMPTY).hashCode();
		final int restrictionHash = Objects.requireNonNullElse(restriction, StringUtils.EMPTY).hashCode();
		noteHash = textHash ^ languageTagHash ^ restrictionHash;

		textPreviewView.setText(getTitle(), text, languageTag);

		if(languageTag != null)
			localeComboBox.setSelectedByLanguageTag(languageTag);

		restrictionCheckBox.setSelected("confidential".equals(restriction));

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
