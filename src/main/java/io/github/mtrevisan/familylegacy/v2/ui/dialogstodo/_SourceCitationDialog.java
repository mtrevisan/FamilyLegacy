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
package io.github.mtrevisan.familylegacy.v2.ui.dialogstodo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.io.Serial;


/* ONGOING */
/**
 * Dialog for editing a {@code SOURCE_CITATION} according to FLEF 0.0.9.
 * Wraps the SourceCitationPanel in a dialog.
 */
public class _SourceCitationDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = -7024588390352183760L;


	private static final String PARAM_SOURCE = "SOURCE";


	private final FLEFRecord citationRecord;
	private boolean saved = false;

	private final SourceCitationPanel panel;


	public static _SourceCitationDialog createNew(final Dialog parent, final FLEFModel model){
		return new _SourceCitationDialog(parent, model, null);
	}

	public static _SourceCitationDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new _SourceCitationDialog(parent, model, record);
	}


	private _SourceCitationDialog(final Dialog parent, final FLEFModel model, final FLEFRecord citationRecord){
		super(parent, citationRecord == null? "Add Source Citation": "Edit Source Citation", true);

		this.citationRecord = (citationRecord != null? citationRecord: FLEFRecord.createEmpty());
		panel = new SourceCitationPanel(this, model, findRecordSourceId(citationRecord));
		panel.load(citationRecord);

		initComponents();

		pack();

		setLocationRelativeTo(parent);
	}

	public String findRecordSourceId(final FLEFRecord sourceCitation){
		String id = null;
		for(final FLEFRecord child : sourceCitation.getChildren())
			if(PARAM_SOURCE.equals(child.getTag()))
				id = XRefHelper.extractXRef(child.getValue());
		return id;
	}


	private void initComponents(){
		setLayout(new BorderLayout(10, 10));

		// Wrap the panel in a container with top alignment to ensure content is at the top
		JPanel wrapper = new JPanel(new MigLayout("top", "[grow]", "[grow]"));
		wrapper.add(panel, "grow");
		add(wrapper, BorderLayout.CENTER);

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

	public FLEFRecord getRecord(){
		if(!saved)
			return null;
		return panel.save(citationRecord);
	}

}
