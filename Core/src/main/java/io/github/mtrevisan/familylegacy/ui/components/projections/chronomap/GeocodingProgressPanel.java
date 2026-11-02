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
package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;


/**
 * Strip embedded in the chronomap, showing the progress of the geocoding and allowing to stop it.
 * <p>
 * It is hidden until the geocoding starts, and hidden again when it ends.
 * </p>
 */
final class GeocodingProgressPanel extends JPanel{

	private final JLabel statusLabel = new JLabel();
	private final JProgressBar progressBar = new JProgressBar(0, 100);
	private final JButton stopButton = new JButton(I18N.t("button.cancel"));

	private Runnable cancelAction;


	GeocodingProgressPanel(){
		super(new MigLayout("ins 2 8 2 8,fillx", "[][grow,fill][]", "[]"));

		statusLabel.setText(I18N.t("dialog.geocoding.status.starting"));
		//fixed width, so that the progress bar does not resize when the text changes (a longer text is clipped)
		add(statusLabel, "w 300!");

		progressBar.setStringPainted(true);
		add(progressBar, "growx");

		stopButton.addActionListener(e -> stop());
		add(stopButton);

		setVisible(false);
	}


	/**
	 * Shows the strip, resetting it.
	 *
	 * @param cancelAction	What to do when the user stops the geocoding.
	 */
	void begin(final Runnable cancelAction){
		this.cancelAction = cancelAction;

		progressBar.setValue(0);
		progressBar.setString(null);
		statusLabel.setText(I18N.t("dialog.geocoding.status.starting"));
		stopButton.setEnabled(true);
		setVisible(true);
	}

	void updateProgress(final int current, final int total, final String currentPlace){
		SwingUtilities.invokeLater(() -> {
			final int percent = (total > 0? (int)((current / (double)total) * 100): 0);
			progressBar.setValue(percent);
			progressBar.setString(current + " / " + total + " (" + percent + "%)");

			final String safePlaceName = (currentPlace != null? GUIHelper.limitTextLength(currentPlace): StringUtils.EMPTY);
			statusLabel.setText(I18N.tf("dialog.geocoding.status.resolving", safePlaceName));
			statusLabel.setToolTipText(statusLabel.getText());
		});
	}

	/** Hides the strip: the geocoding is over. */
	void end(){
		cancelAction = null;

		setVisible(false);
	}

	private void stop(){
		stopButton.setEnabled(false);

		final Runnable action = cancelAction;
		cancelAction = null;
		if(action != null)
			action.run();
	}

}
