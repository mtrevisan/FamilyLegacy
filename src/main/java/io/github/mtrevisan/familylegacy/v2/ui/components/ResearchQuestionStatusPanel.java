package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.time.Instant;
import java.time.format.DateTimeFormatter;


/* DONE */
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
	private final CardLayout buttonCardLayout;
	private final JPanel buttonCardPanel;
	private final BoundTextField closedDate;


	public ResearchQuestionStatusPanel(){
		// Status icon
		statusIcon = new StatusIconLabel();

		// Status combo
		statusCombo = new BoundComboBox<>(TAG_STATUS, new String[]{STATUS_OPEN, STATUS_ON_HOLD, STATUS_RESOLVED,
			STATUS_DISPROVEN});
		statusCombo.setSelectedItem(STATUS_OPEN);

		// Button card panel with Close and Reopen buttons (same position)
		buttonCardLayout = new CardLayout();
		buttonCardPanel = new JPanel(buttonCardLayout);

		final JButton closeButton = new JButton("Close");
		closeButton.setToolTipText("Mark as resolved and set closed date");
		closeButton.addActionListener(e -> closeQuestion());

		final JButton reopenButton = new JButton("Reopen");
		reopenButton.setToolTipText("Reopen the question and clear closed date");
		reopenButton.addActionListener(e -> reopenQuestion());

		buttonCardPanel.add(closeButton, "close");
		buttonCardPanel.add(reopenButton, "reopen");

		closedDate = new BoundTextField(TAG_CLOSED, 20);
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
		add(buttonCardPanel, "gapx 5");


		statusCombo.addActionListener(e -> updateUIState());
	}

	private void updateUIState(){
		final String status = (String)statusCombo.getSelectedItem();
		final boolean isClosed = (STATUS_RESOLVED.equals(status) || STATUS_DISPROVEN.equals(status));

		// Update icon
		statusIcon.setStatus(status);
		statusIcon.repaint();

		// Update tooltip
		final String tooltip = switch(status){
			case STATUS_OPEN -> "Open";
			case STATUS_ON_HOLD -> "On Hold";
			case STATUS_RESOLVED -> "Resolved" + (!closedDate.isEmpty()? " on " + closedDate.getText(): "");
			case STATUS_DISPROVEN -> "Disproven" + (!closedDate.isEmpty()? " on " + closedDate.getText(): "");
			default -> "Unknown";
		};
		statusIcon.setToolTipText(tooltip);

		// Show the appropriate button (Close or Reopen)
		buttonCardLayout.show(buttonCardPanel, (isClosed? "reopen": "close"));
	}

	private void closeQuestion(){
		final String status = (String)statusCombo.getSelectedItem();
		if(STATUS_OPEN.equals(status) || STATUS_ON_HOLD.equals(status)){
			statusCombo.setSelectedItem(STATUS_RESOLVED);
			closedDate.setText(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

			updateUIState();
		}
	}

	private void reopenQuestion(){
		final String status = (String)statusCombo.getSelectedItem();
		if(STATUS_RESOLVED.equals(status) || STATUS_DISPROVEN.equals(status)){
			statusCombo.setSelectedItem(STATUS_OPEN);
			closedDate.setText(null);

			updateUIState();
		}
	}

	public void load(final FLEFRecord record){
		bindingManager.load(record);

		if(!statusCombo.isEnabled())
			statusCombo.setSelectedItem(STATUS_OPEN);
	}

	public void save(final FLEFRecord record){
		bindingManager.save(record);
	}

}