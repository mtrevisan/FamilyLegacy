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
package io.github.mtrevisan.familylegacy.v2.ui.tools.sources;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.ImageCropDialog;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog that helps the user define an {@code ExtractStructure} bound to
 * a rectangle of a document image.
 * <p>
 * The crop rectangle is chosen visually through {@link ImageCropDialog},
 * the same dialog used elsewhere in the application: the user opens the
 * document image, draws the rectangle with the mouse, and confirms. The
 * coordinates are never typed by hand.
 * <p>
 * The dialog does not write into the model. It builds the FLEF text of
 * the {@code extract} block and copies it to the clipboard, so the user
 * can paste it into the Sources tab of the target record's editor. This
 * avoids duplicating the citation editor and keeps the tool a pure
 * helper.
 * <p>
 * Only local documents can be cropped. When the document URI is remote,
 * the "Define crop…" button is disabled and the user is told why: the
 * application does not fetch images from the network.
 */
public final class ExtractFromImageDialog extends JDialog{

	private final ToolContext context;

	private final JComboBox<String> documentCombo = new JComboBox<>();
	private final JTextField cropField = new JTextField(28);
	private final JButton defineCropButton = new JButton("Define crop…");
	private final JTextField typeField = new JTextField(16);
	private final JTextArea textArea = new JTextArea(4, 30);

	private final List<FLEFRecord> documents = new ArrayList<>();

	/** Currently selected crop, or {@code null} when none has been defined. */
	private Rectangle crop;


	public ExtractFromImageDialog(final ToolContext context){
		super(context.owner(), "Extract from Image", ModalityType.APPLICATION_MODAL);
		this.context = context;

		loadDocuments();

		typeField.setText("verbatim");
		cropField.setEditable(false);

		setLayout(new BorderLayout(8, 8));
		add(createForm(), BorderLayout.CENTER);
		add(createButtons(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(620, 360));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		wireSelectionChange();
		updateCropState();
	}


	private void loadDocuments(){
		documentCombo.removeAllItems();
		documents.clear();
		documents.addAll(SourceHelper.listAllDocuments(context.model()));
		for(final FLEFRecord doc : documents)
			documentCombo.addItem(describe(doc));
		if(!documents.isEmpty())
			documentCombo.setSelectedIndex(0);
	}

	private static String describe(final FLEFRecord doc){
		final String desc = SourceHelper.documentDescription(doc);
		final String uri = SourceHelper.documentUri(doc);
		final String label = (desc != null && !desc.isBlank()? desc: uri);
		return doc.getId() + " — " + (label != null? label: "(no uri)");
	}

	private JPanel createForm(){
		final JPanel form = new JPanel(new GridBagLayout());
		form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Document row.
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		form.add(new JLabel("Document:"), gbc);
		gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1;
		form.add(documentCombo, gbc);
		gbc.gridwidth = 1;

		// Crop row.
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		form.add(new JLabel("Crop:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1;
		form.add(cropField, gbc);
		gbc.gridx = 2; gbc.weightx = 0;
		defineCropButton.addActionListener(e -> defineCrop());
		form.add(defineCropButton, gbc);

		// Extract type.
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
		form.add(new JLabel("Extract type:"), gbc);
		gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1;
		form.add(typeField, gbc);
		gbc.gridwidth = 1;

		// Extract text.
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		form.add(new JLabel("Extract text:"), gbc);
		gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1; gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		form.add(new JScrollPane(textArea), gbc);

		return form;
	}

	private JPanel createButtons(){
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

		final JButton copy = new JButton("Copy Extract");
		copy.addActionListener(e -> copyExtractToClipboard());
		buttons.add(copy);

		final JButton close = new JButton("Close");
		close.addActionListener(e -> dispose());
		buttons.add(close);

		return buttons;
	}


	/* ======================================================================
	 *                          State management
	 * ====================================================================== */

	private void wireSelectionChange(){
		documentCombo.addActionListener(e -> {
			// Changing the document invalidates any crop defined for the
			// previous one.
			crop = null;
			updateCropState();
		});
	}

	/**
	 * Updates the crop field and the "Define crop…" button to reflect
	 * the currently selected document and the current crop. The button
	 * is disabled when the selected document has no local file to open,
	 * because the visual crop editor requires a real image.
	 */
	private void updateCropState(){
		final FLEFRecord doc = selectedDocument();
		if(doc == null){
			cropField.setText("");
			defineCropButton.setEnabled(false);
			return;
		}

		final boolean hasLocalFile = (localFileOf(doc) != null);
		defineCropButton.setEnabled(hasLocalFile);

		if(crop == null)
			cropField.setText(hasLocalFile? "(not defined)": "(no local file to crop)");
		else
			cropField.setText(crop.x + ", " + crop.y + "  " + crop.width + " × " + crop.height);
	}

	private FLEFRecord selectedDocument(){
		final int idx = documentCombo.getSelectedIndex();
		if(idx < 0 || idx >= documents.size())
			return null;
		return documents.get(idx);
	}

	/**
	 * Returns the local file behind the given document, or {@code null}
	 * when the URI is missing, remote, or not parseable as a local path.
	 */
	private static File localFileOf(final FLEFRecord document){
		final String uri = SourceHelper.documentUri(document);
		if(uri == null || uri.isBlank())
			return null;
		try{
			final URI parsed = URI.create(uri);
			if("file".equalsIgnoreCase(parsed.getScheme()))
				return new File(parsed);
			if(parsed.getScheme() == null)
				return new File(uri);
			// Remote scheme (http, https, ftp, …): not supported.
			return null;
		}
		catch(final Exception ignored){
			return null;
		}
	}


	/* ======================================================================
	 *                          Crop definition
	 * ====================================================================== */

	/**
	 * Opens the visual crop editor on the selected document, preselecting
	 * the current crop when one exists. When the editor is confirmed, the
	 * new rectangle replaces the previous one.
	 */
	private void defineCrop(){
		final FLEFRecord doc = selectedDocument();
		if(doc == null){
			JOptionPane.showMessageDialog(this,
				"Select a document first.",
				"Extract from Image", JOptionPane.WARNING_MESSAGE);
			return;
		}
		final File file = localFileOf(doc);
		if(file == null){
			JOptionPane.showMessageDialog(this,
				"This document has no local image file to crop.",
				"Extract from Image", JOptionPane.WARNING_MESSAGE);
			return;
		}

		final ImageCropDialog cropDialog = ImageCropDialog.create(getOwner());
		try{
			cropDialog.loadData(file, crop);
		}
		catch(final IOException e){
			JOptionPane.showMessageDialog(this,
				"Unable to load the image:\n" + e.getMessage(),
				"Extract from Image", JOptionPane.ERROR_MESSAGE);
			return;
		}
		cropDialog.setVisible(true);

		if(cropDialog.isSaved()){
			final Rectangle chosen = cropDialog.getCrop();
			if(chosen != null && chosen.width > 0 && chosen.height > 0){
				crop = chosen;
				updateCropState();
			}
		}
	}


	/* ======================================================================
	 *                          Extract output
	 * ====================================================================== */

	private void copyExtractToClipboard(){
		final FLEFRecord doc = selectedDocument();
		if(doc == null){
			JOptionPane.showMessageDialog(this,
				"Select a document first.",
				"Extract from Image", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if(crop == null){
			JOptionPane.showMessageDialog(this,
				"Define the crop rectangle first.",
				"Extract from Image", JOptionPane.WARNING_MESSAGE);
			return;
		}

		final String text = textArea.getText();
		if(text == null || text.isBlank()){
			JOptionPane.showMessageDialog(this,
				"Enter the extract text. A document_part without a text "
					+ "or a text without a document_part is not a valid extract.",
				"Extract from Image", JOptionPane.WARNING_MESSAGE);
			return;
		}

		final StringBuilder sb = new StringBuilder();
		sb.append("extract {\n");
		sb.append("  document_part {\n");
		sb.append("    document: ").append(doc.getId()).append('\n');
		sb.append("    crop { x: ").append(crop.x)
			.append(", y: ").append(crop.y)
			.append(", width: ").append(crop.width)
			.append(", height: ").append(crop.height).append(" }\n");
		sb.append("  }\n");
		final String type = typeField.getText();
		if(type != null && !type.isBlank())
			sb.append("  type: ").append(type.trim()).append('\n');
		sb.append("  text: \"").append(text.trim().replace("\"", "\\\"")).append("\"\n");
		sb.append("}\n");

		final java.awt.datatransfer.StringSelection selection =
			new java.awt.datatransfer.StringSelection(sb.toString());
		java.awt.Toolkit.getDefaultToolkit()
			.getSystemClipboard()
			.setContents(selection, selection);

		JOptionPane.showMessageDialog(this,
			"Extract copied to clipboard. Paste it into the Sources tab "
				+ "of the target record.",
			"Extract from Image", JOptionPane.INFORMATION_MESSAGE);
	}

}
