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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.utils.FLEFRecordUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Placeholder dialog for NOTE_RECORD.
 * This is a minimal implementation that shows a "Not implemented" message.
 * TODO: Implement full NoteDialog functionality.
 */
public class NoteDialog extends BaseRecordDialog {

	public NoteDialog(Frame parent, FLEFModel model, FLEFRecord record) {
		super(parent, model, record, "Edit Note");
		initComponents();
		loadData();
		setMinimumSize(new Dimension(400, 200));
		pack();
		setLocationRelativeTo(parent);
	}

	public NoteDialog(Frame parent, FLEFModel model) {
		super(parent, model, "New Note");
		initComponents();
		loadData();
		setMinimumSize(new Dimension(400, 200));
		pack();
		setLocationRelativeTo(parent);
	}

	@Override
	protected void initComponents() {
		setLayout(new BorderLayout());
		JLabel label = new JLabel("Note dialog not yet implemented", SwingConstants.CENTER);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));
		add(label, BorderLayout.CENTER);

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(e -> dispose());
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(closeButton);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	@Override
	protected void loadData() {
		// Nothing to load
	}

	@Override
	protected void saveRecord() {
		JOptionPane.showMessageDialog(this, "Save not implemented", "Info", JOptionPane.INFORMATION_MESSAGE);
		dispose();
	}

	@Override
	protected FLEFRecord createNewRecord() {
		FLEFRecord newRecord = new FLEFRecord();
		newRecord.setType("NOTE");
		newRecord.setId(generateNewId());
		return newRecord;
	}

	@Override
	protected String generateNewId() {
		return FLEFRecordUtils.generateNewId(model, "NOTE", "N");
	}
}