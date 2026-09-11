package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;


/**
 * Toolbar of the General Temporal Projection.
 * <p>
 * Exposes:
 * <ul>
 *   <li>zoom controls — zoom in, zoom out, fit to domain, and a zoom level
 *       combo;</li>
 *   <li>layer toggles — context bands, connections, impact links;</li>
 *   <li>entity type filter — all, individuals, groups, places.</li>
 * </ul>
 * The toolbar holds the state of its toggles and combo boxes and exposes it
 * through getters; it notifies the panel of user actions through a
 * {@link Listener}.
 */
public final class TemporalProjectionToolbar extends JPanel{

	/**
	 * Listener for toolbar actions.
	 */
	public interface Listener{
		void onZoomIn();
		void onZoomOut();
		void onFitToDomain();
		void onFiltersChanged();
	}

	/** Entity type filter option values. */
	public enum EntityFilter{
		ALL("All"),
		INDIVIDUALS("Individuals"),
		GROUPS("Groups"),
		PLACES("Places");

		private final String label;

		EntityFilter(final String label){
			this.label = label;
		}

		@Override
		public String toString(){
			return label;
		}
	}


	private final JButton zoomInBtn = new JButton("+");
	private final JButton zoomOutBtn = new JButton("−");
	private final JButton fitBtn = new JButton("Fit");
	private final JCheckBox showBandsCheck = new JCheckBox("Bands", true);
	private final JCheckBox showConnectionsCheck = new JCheckBox("Connections", true);
	private final JCheckBox showImpactLinksCheck = new JCheckBox("Impacts", true);
	private final JComboBox<EntityFilter> entityFilterCombo = new JComboBox<>(EntityFilter.values());
	private final Listener listener;


	public TemporalProjectionToolbar(final Listener listener){
		this.listener = listener;

		initComponents();
		installListeners(listener);
	}


	/* ======================================================================
	 *                          State accessors
	 * ====================================================================== */

	public boolean isShowBands(){
		return showBandsCheck.isSelected();
	}

	public boolean isShowConnections(){
		return showConnectionsCheck.isSelected();
	}

	public boolean isShowImpactLinks(){
		return showImpactLinksCheck.isSelected();
	}

	public EntityFilter getEntityFilter(){
		return (EntityFilter)entityFilterCombo.getSelectedItem();
	}


	/* ======================================================================
	 *                          Setup
	 * ====================================================================== */

	private void initComponents(){
		setLayout(new BorderLayout());

		final JPanel content = new JPanel(new MigLayout("ins 6,gapx 6", "[]", "[]"));
		content.add(new JLabel("Zoom:"));
		content.add(zoomOutBtn);
		content.add(zoomInBtn);
		content.add(fitBtn);
		content.add(new JLabel("  Show:"));
		content.add(showBandsCheck);
		content.add(showConnectionsCheck);
		content.add(showImpactLinksCheck);
		content.add(new JLabel("  Entities:"));
		content.add(entityFilterCombo);

		add(content, BorderLayout.WEST);
	}

	private void installListeners(final Listener listener){
		if(listener == null)
			return;

		zoomInBtn.addActionListener(e -> listener.onZoomIn());
		zoomOutBtn.addActionListener(e -> listener.onZoomOut());
		fitBtn.addActionListener(e -> listener.onFitToDomain());
		showBandsCheck.addActionListener(e -> listener.onFiltersChanged());
		showConnectionsCheck.addActionListener(e -> listener.onFiltersChanged());
		showImpactLinksCheck.addActionListener(e -> listener.onFiltersChanged());
		entityFilterCombo.addActionListener(e -> listener.onFiltersChanged());
	}

}
