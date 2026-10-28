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
package io.github.mtrevisan.familylegacy.ui.assistant.citation;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Consumer;


/**
 * Turns a free-form archival reference (<em>vol. 3, p. 45, n. 12</em>) into the locator of a citation (<em>Volume: 3, Page: 45,
 * Entry: 12</em>).
 * <p>
 * The locator is a draft: it can be edited, and it is written in the citation only if the user applies it.
 * What is not understood is shown, never guessed.
 * </p>
 */
public class CitationAssistantDialog extends JDialog{

	private final FLEFModel model;
	private final String currentLocator;
	private final Consumer<String> locatorApplier;
	private final Consumer<FLEFRecord> repositoryApplier;

	private final JTextArea referenceArea = new JTextArea(3, 50);
	private final JTextArea findingsArea = new JTextArea(5, 50);
	private final JTextField locatorField = new JTextField(50);
	private final DefaultListModel<CitationHints.Hint> hintsModel = new DefaultListModel<>();
	private final JList<CitationHints.Hint> hintsList = new JList<>(hintsModel);
	private final DefaultListModel<FLEFRecord> repositoriesModel = new DefaultListModel<>();
	private final JList<FLEFRecord> repositoriesList = new JList<>(repositoriesModel);
	private final JButton applyButton = new JButton(I18N.t("dialog.citation_assistant.apply"));
	private final JButton useHintButton = new JButton(I18N.t("dialog.citation_assistant.use_hint"));
	private final JButton repositoryButton = new JButton(I18N.t("dialog.citation_assistant.use_repository"));
	private final JButton copyButton = new JButton(I18N.t("dialog.citation_assistant.copy"));


	/**
	 * @param parent            the parent window
	 * @param model             the model, to look for repositories (can be {@code null})
	 * @param source            the cited source, to read the clues of its media (can be {@code null})
	 * @param currentLocator    the locator currently entered, that is the first text to interpret (can be {@code null})
	 * @param repositoryApplier called when the user chooses a repository; {@code null} if the citation has none
	 * @param locatorApplier    called when the user applies the locator
	 */
	public CitationAssistantDialog(final Window parent, final FLEFModel model, final FLEFRecord source,
			final String currentLocator, final Consumer<String> locatorApplier, final Consumer<FLEFRecord> repositoryApplier){
		super(parent, I18N.t("dialog.citation_assistant.title"), ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.currentLocator = StringUtils.defaultString(currentLocator);
		this.locatorApplier = locatorApplier;
		this.repositoryApplier = repositoryApplier;

		initComponents(CitationHints.mediaHints(model, source));

		//ESC closes the assistant from any field, without a button
		getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);

		referenceArea.setText(this.currentLocator);
		interpret();

		pack();
		setLocationRelativeTo(parent);
	}


	private void initComponents(final List<CitationHints.Hint> hints){
		referenceArea.setLineWrap(true);
		referenceArea.setWrapStyleWord(true);
		referenceArea.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				interpret();
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				interpret();
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				interpret();
			}
		});

		findingsArea.setEditable(false);
		findingsArea.setLineWrap(true);
		findingsArea.setWrapStyleWord(true);

		hints.forEach(hintsModel::addElement);
		hintsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		hintsList.setCellRenderer((list, value, index, selected, focus) -> row(list, value.origin() + " → " + value.locator(),
			selected));
		repositoriesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		repositoriesList.setCellRenderer((list, value, index, selected, focus) -> row(list,
			StringUtils.defaultString(RepositoryReader.extractPrimaryName(value), value.getId()), selected));

		applyButton.addActionListener(e -> apply());
		useHintButton.addActionListener(e -> {
			final CitationHints.Hint hint = hintsList.getSelectedValue();
			if(hint != null)
				referenceArea.setText(hint.locator());
		});
		repositoryButton.addActionListener(e -> {
			final FLEFRecord repository = repositoriesList.getSelectedValue();
			if(repository != null && repositoryApplier != null){
				repositoryApplier.accept(repository);
				dispose();
			}
		});
		copyButton.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
			.setContents(new StringSelection(locatorField.getText()), null));

		final JPanel buttons = new JPanel(new MigLayout("ins 0", "[][]"));
		buttons.add(applyButton);
		buttons.add(copyButton);

		setLayout(new MigLayout("fill,wrap 1", "[grow,fill]", "[]5[]5[]5[]5[]5[]5[]5[]"));
		add(new JLabel("<html><i>" + I18N.t("dialog.citation_assistant.disclaimer") + "</i></html>"));
		add(new JLabel(I18N.t("dialog.citation_assistant.reference")));
		add(new JScrollPane(referenceArea), "grow");
		add(new JScrollPane(findingsArea), "grow");
		add(new JLabel(I18N.t("dialog.citation_assistant.locator")));
		add(locatorField);
		if(!hints.isEmpty()){
			add(new JLabel(I18N.t("dialog.citation_assistant.hints")));
			add(new JScrollPane(hintsList), "grow,h 60!");
			add(useHintButton, "align right");
		}
		if(repositoryApplier != null){
			add(new JLabel(I18N.t("dialog.citation_assistant.repositories")));
			add(new JScrollPane(repositoriesList), "grow,h 60!");
			add(repositoryButton, "align right");
		}
		add(buttons, "align right");
	}

	private static JLabel row(final JList<?> list, final String text, final boolean selected){
		final JLabel label = new JLabel(text);
		label.setOpaque(true);
		label.setBackground(selected? list.getSelectionBackground(): list.getBackground());
		label.setForeground(selected? list.getSelectionForeground(): list.getForeground());
		return label;
	}


	private void interpret(){
		final ArchivalReferenceParser.Result result = ArchivalReferenceParser.parse(referenceArea.getText());

		locatorField.setText(result.locator());

		final StringBuilder sb = new StringBuilder();
		for(final ArchivalReferenceParser.Part part : result.parts())
			sb.append(part.label()).append(": ").append(part.value()).append('\n');
		if(!result.unrecognized().isEmpty())
			sb.append('\n').append(I18N.t("dialog.citation_assistant.unrecognized")).append(' ')
				.append(String.join("; ", result.unrecognized())).append('\n');
		for(final String note : result.notes())
			sb.append('\n').append(note);
		findingsArea.setText(sb.toString());
		findingsArea.setCaretPosition(0);

		repositoriesModel.clear();
		CitationHints.repositoryMatches(model, result.unrecognized()).forEach(repositoriesModel::addElement);
		if(!repositoriesModel.isEmpty())
			repositoriesList.setSelectedIndex(0);

		applyButton.setEnabled(!locatorField.getText().isBlank());
		repositoryButton.setEnabled(!repositoriesModel.isEmpty());
	}

	private void apply(){
		final String locator = locatorField.getText().strip();
		if(locator.isEmpty())
			return;

		if(!currentLocator.isBlank() && !currentLocator.strip().equals(locator)
				&& JOptionPane.showConfirmDialog(this, I18N.t("dialog.citation_assistant.replace_confirm"), getTitle(),
				JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
			return;

		locatorApplier.accept(locator);
		dispose();
	}

}
