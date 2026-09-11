package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Toolbar of the map view.
 * <p>
 * Controls:
 * <ul>
 *   <li>projection type combo;</li>
 *   <li>zoom in / zoom out / fit buttons;</li>
 *   <li>show places / events / routes / graticule checkboxes;</li>
 *   <li>focus picker.</li>
 * </ul>
 */
public final class GeoMapToolbar extends JPanel{

	public interface Listener{
		void onProjectionTypeChanged(GeoProjectionType type);
		void onZoomIn();
		void onZoomOut();
		void onFitRequested();
		void onPickFocus();
		void onFiltersChanged();
	}


	private final JComboBox<GeoProjectionType> projectionCombo = new JComboBox<>(GeoProjectionType.values());
	private final JButton zoomInBtn = new JButton("+");
	private final JButton zoomOutBtn = new JButton("−");
	private final JButton fitBtn = new JButton("Fit");
	private final JButton focusBtn = new JButton("Pick focus…");
	private final JLabel focusLabel = new JLabel("(none)");
	private final JCheckBox showPlacesCheck = new JCheckBox("Places", true);
	private final JCheckBox showEventsCheck = new JCheckBox("Events", true);
	private final JCheckBox showRoutesCheck = new JCheckBox("Routes", true);
	private final JCheckBox showGraticuleCheck = new JCheckBox("Graticule", true);


	public GeoMapToolbar(final Listener listener){
		initComponents();
		installListeners(listener);
	}


	public GeoProjectionType getProjectionType(){
		return (GeoProjectionType)projectionCombo.getSelectedItem();
	}

	public boolean isShowPlaces(){ return showPlacesCheck.isSelected(); }
	public boolean isShowEvents(){ return showEventsCheck.isSelected(); }
	public boolean isShowRoutes(){ return showRoutesCheck.isSelected(); }
	public boolean isShowGraticule(){ return showGraticuleCheck.isSelected(); }

	public void setFocusLabel(final String text){
		focusLabel.setText(text != null && !text.isBlank()? text: "(none)");
	}


	private void initComponents(){
		setLayout(new BorderLayout());
		final JPanel content = new JPanel(new MigLayout("ins 6,gapx 6", "[]", "[]"));
		content.add(new JLabel("Projection:"));
		content.add(projectionCombo);
		content.add(new JLabel("  Zoom:"));
		content.add(zoomOutBtn);
		content.add(zoomInBtn);
		content.add(fitBtn);
		content.add(new JLabel("  Show:"));
		content.add(showPlacesCheck);
		content.add(showEventsCheck);
		content.add(showRoutesCheck);
		content.add(showGraticuleCheck);
		content.add(new JLabel("  Focus:"));
		content.add(focusBtn);
		content.add(focusLabel);
		add(content, BorderLayout.WEST);
	}

	private void installListeners(final Listener listener){
		if(listener == null)
			return;
		projectionCombo.addActionListener(e -> listener.onProjectionTypeChanged(getProjectionType()));
		zoomInBtn.addActionListener(e -> listener.onZoomIn());
		zoomOutBtn.addActionListener(e -> listener.onZoomOut());
		fitBtn.addActionListener(e -> listener.onFitRequested());
		focusBtn.addActionListener(e -> listener.onPickFocus());
		showPlacesCheck.addActionListener(e -> listener.onFiltersChanged());
		showEventsCheck.addActionListener(e -> listener.onFiltersChanged());
		showRoutesCheck.addActionListener(e -> listener.onFiltersChanged());
		showGraticuleCheck.addActionListener(e -> listener.onFiltersChanged());
	}

}
