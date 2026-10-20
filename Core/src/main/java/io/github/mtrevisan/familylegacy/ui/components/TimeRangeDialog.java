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
package io.github.mtrevisan.familylegacy.ui.components;

import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Window;


/**
 * Dialog for selecting a time range within an audio or video document.
 * The times are handled as ISO 8601 Durations (e.g., "PT1M30S").
 */
public class TimeRangeDialog extends JDialog{

	private JTextField startField;
	private JTextField endField;
	private boolean isSaved;


	public static TimeRangeDialog create(final Window parent){
		final TimeRangeDialog dialog = new TimeRangeDialog(parent);
		dialog.initialize();

		return dialog;
	}


	private TimeRangeDialog(final Window parent){
		super(parent, ModalityType.APPLICATION_MODAL);
	}


	private void initialize(){
		initComponents();
		initLayout();

		pack();

		setLocationRelativeTo(getOwner());
	}

	private void initComponents(){
		setTitle(I18N.t("dialog.preferred.time_range.title"));

		startField = new JTextField(10);
		endField = new JTextField(10);
	}

	private void initLayout(){
		setLayout(new MigLayout("ins 20,hidemode 3,fillx,wrap 2", "[][grow,fill]", "[]"));

		final JPanel contentPanel = GUIHelper.createLabelFieldPanel(0, "[grow,fill]");

		GUIHelper.addComponent(contentPanel, new JLabel(I18N.t("dialog.preferred.time_range.start")));
		GUIHelper.addComponent(contentPanel, startField);

		GUIHelper.addComponent(contentPanel, new JLabel(I18N.t("dialog.preferred.time_range.end")));
		GUIHelper.addComponent(contentPanel, endField);

		add(contentPanel, "center,grow,push");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(this,
			this::save,
			() -> setVisible(false));
		add(buttonPanel, BorderLayout.SOUTH);
	}

	public void loadData(final String start, final String end){
		isSaved = false;
		startField.setText(start != null? start: StringUtils.EMPTY);
		endField.setText(end != null? end: StringUtils.EMPTY);
	}

	public void save(){
		isSaved = true;
		setVisible(false);
	}

	public boolean isSaved(){
		return isSaved;
	}

	public String getStart(){
		return startField.getText().trim();
	}

	public String getEnd(){
		return endField.getText().trim();
	}

}
