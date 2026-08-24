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
package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.bindings.BoundTextField;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JPanel;


/**
 * Panel that manages the status and closure of a ResearchQuestionRecord.
 * Provides visual indicators, status combo, and close/reopen buttons (mutually exclusive).
 */
public class ResearchQuestionStatusPanel extends JPanel{

	private static final String TAG_STATUS = "STATUS";
	private static final String TAG_CLOSED = "CLOSED";

	private static final String STATUS_OPEN = "open";
	private static final String STATUS_ON_HOLD = "on_hold";
	private static final String STATUS_RESOLVED = "resolved";
	private static final String STATUS_DISPROVEN = "disproven";


	private final BindingManager bindingManager = new BindingManager();

	private final BoundComboBox<String> statusCombo;
	private final StatusIconLabel statusIcon;
//	private final CardLayout buttonCardLayout;
//	private final JPanel buttonCardPanel;
	private final BoundTextField closedDate;


	public ResearchQuestionStatusPanel(){
		// Status icon
		statusIcon = new StatusIconLabel();

		// Status combo
		statusCombo = new BoundComboBox<>(TAG_STATUS, new String[]{
			STATUS_OPEN, STATUS_ON_HOLD, STATUS_RESOLVED, STATUS_DISPROVEN});
		statusCombo.setSelectedItem(STATUS_OPEN);

		// Button card panel with Close and Reopen buttons (same position)
//		buttonCardLayout = new CardLayout();
//		buttonCardPanel = new JPanel(buttonCardLayout);

//		final JButton closeButton = new JButton("Close");
//		closeButton.setToolTipText("Mark as resolved and set closed date");
//		closeButton.addActionListener(e -> closeQuestion());

//		final JButton reopenButton = new JButton("Reopen");
//		reopenButton.setToolTipText("Reopen the question and clear closed date");
//		reopenButton.addActionListener(e -> reopenQuestion());

//		buttonCardPanel.add(closeButton, "close");
//		buttonCardPanel.add(reopenButton, "reopen");

		closedDate = new BoundTextField(TAG_CLOSED);
		closedDate.setEnabled(false);


		initComponents();

		updateUIState();
	}


	private void initComponents(){
		bindingManager.bind(statusCombo);
		bindingManager.bind(closedDate);


		setLayout(new MigLayout("ins 0,fillx", "[shrink 0][grow][shrink 0]", "[]"));

		add(statusIcon, "width 16!,height 16!,gapx 5");
		add(statusCombo, "growx,width 120!");
//		add(buttonCardPanel, "gapx 5");


		statusCombo.addActionListener(e -> updateUIState());
	}

	private void updateUIState(){
		String status = (String)statusCombo.getSelectedItem();
		if(status == null){
			status = STATUS_OPEN;
			statusCombo.setSelectedItem(status);
		}

		// Update icon
		statusIcon.setStatus(status);
		statusIcon.repaint();

		// Update tooltip
		final String tooltip = switch(status){
			case STATUS_OPEN -> "Open";
			case STATUS_ON_HOLD -> "On Hold";
			case STATUS_RESOLVED -> "Resolved" + (!closedDate.isEmpty()? " on " + closedDate.getText(): StringUtils.EMPTY);
			case STATUS_DISPROVEN -> "Disproven" + (!closedDate.isEmpty()? " on " + closedDate.getText(): StringUtils.EMPTY);
			default -> "Unknown";
		};
		statusIcon.setToolTipText(tooltip);

		// Show the appropriate button (Close or Reopen)
//		buttonCardLayout.show(buttonCardPanel, (isClosed? "reopen": "close"));
	}

//	private void closeQuestion(){
//		final String status = (String)statusCombo.getSelectedItem();
//		if(STATUS_OPEN.equals(status) || STATUS_ON_HOLD.equals(status)){
//			statusCombo.setSelectedItem(STATUS_RESOLVED);
//			closedDate.setText(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
//
//			updateUIState();
//		}
//	}

//	private void reopenQuestion(){
//		final String status = (String)statusCombo.getSelectedItem();
//		if(STATUS_RESOLVED.equals(status) || STATUS_DISPROVEN.equals(status)){
//			statusCombo.setSelectedItem(STATUS_OPEN);
//			closedDate.setText(null);
//
//			updateUIState();
//		}
//	}

	public void load(final FLEFRecord record){
		bindingManager.load(record);

		if(record == null || record.isEmpty())
			return;

		if(!statusCombo.isEnabled())
			statusCombo.setSelectedItem(STATUS_OPEN);
	}

	public void save(final FLEFRecord record){
		bindingManager.save(record);
	}

}