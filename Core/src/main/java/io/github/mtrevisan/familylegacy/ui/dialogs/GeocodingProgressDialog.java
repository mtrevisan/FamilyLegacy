package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;


public final class GeocodingProgressDialog extends JDialog{

	private static final String ACTION_CANCEL_GEOCODING = "cancelGeocoding";


	private final JLabel statusLabel = new JLabel();
	private final JProgressBar progressBar = new JProgressBar(0, 100);
	private final JButton cancelButton = new JButton(I18N.t("button.cancel"));

	private Runnable cancelAction;


	public GeocodingProgressDialog(final Window owner, final String title, final Runnable cancelAction){
		super(owner, title, ModalityType.MODELESS);

		this.cancelAction = cancelAction;

		initComponents();
		setupEscapeKey();

		pack();
		setMinimumSize(new Dimension(380, 130));
		setLocationRelativeTo(owner);
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 12,fill", "[grow,fill]", "[][][]"));

		statusLabel.setText(I18N.t("dialog.geocoding.status.starting"));
		add(statusLabel, "wmax 350,wrap");

		progressBar.setStringPainted(true);
		progressBar.setValue(0);
		add(progressBar, "growx,wrap");

		cancelButton.addActionListener(e -> cancel());
		add(cancelButton, "right");
	}

	private void setupEscapeKey(){
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
			.put(GUIHelper.ESCAPE_STROKE, ACTION_CANCEL_GEOCODING);
		getRootPane().getActionMap()
			.put(ACTION_CANCEL_GEOCODING, new AbstractAction(){
				@Override
				public void actionPerformed(final ActionEvent e){
					cancel();
				}
			});
	}

	public void updateProgress(final int current, final int total, final String currentPlace){
		SwingUtilities.invokeLater(() -> {
			final int percent = (total > 0 ? (int)((current / (double)total) * 100) : 0);
			progressBar.setValue(percent);
			progressBar.setString(current + " / " + total + " (" + percent + "%)");

			final String safePlaceName = (currentPlace != null ? GUIHelper.limitTextLength(currentPlace) : "");
			statusLabel.setText(I18N.tf("dialog.geocoding.status.resolving", safePlaceName));
		});
	}

	public void cancel(){
		if(cancelAction != null){
			cancelAction.run();
			cancelAction = null;
		}

		dispose();
	}

}
