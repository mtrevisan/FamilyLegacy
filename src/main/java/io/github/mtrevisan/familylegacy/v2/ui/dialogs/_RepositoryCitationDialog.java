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
import io.github.mtrevisan.familylegacy.v2.ui.components.RepositoryCitationPanel;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.Serial;


/**
 * Dialog for editing a {@code REPOSITORY_CITATION} according to FLEF 0.0.9.
 * Wraps the RepositoryCitationPanel in a dialog.
 */
public class _RepositoryCitationDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 5084034247069196652L;


	private final FLEFRecord citationRecord;
	private boolean saved = false;

	private final RepositoryCitationPanel panel;


	public _RepositoryCitationDialog(Frame parent, FLEFModel model, FLEFRecord citationRecord){
		super(parent, citationRecord == null? "Add Repository Citation": "Edit Repository Citation", true);

		this.citationRecord = citationRecord != null? citationRecord: FLEFRecord.createEmpty();
		this.panel = new RepositoryCitationPanel(this, model);
		initComponents();
		if(citationRecord != null){
			panel.loadFromRecord(citationRecord);
		}
		pack();
		setLocationRelativeTo(parent);
	}

	private void initComponents(){
		setLayout(new BorderLayout(10, 10));
		add(panel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton saveBtn = new JButton("Save");
		JButton cancelBtn = new JButton("Cancel");
		buttonPanel.add(saveBtn);
		buttonPanel.add(cancelBtn);
		add(buttonPanel, BorderLayout.SOUTH);

		saveBtn.addActionListener(e -> {
			if(panel.validateData()){
				saved = true;
				dispose();
			}
		});
		cancelBtn.addActionListener(e -> dispose());
	}

	public boolean isSaved(){
		return saved;
	}

	public FLEFRecord getCitationRecord(){
		if(!saved){
			return null;
		}
		return panel.saveToRecord(citationRecord);
	}

}
