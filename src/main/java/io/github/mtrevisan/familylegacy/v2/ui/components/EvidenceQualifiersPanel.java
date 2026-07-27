package io.github.mtrevisan.familylegacy.v2.ui.components;

import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * Reusable panel that groups a CERTAINTY combo and a CREDIBILITY combo
 * inside a titled border.
 * <p>
 * The panel provides methods to load, retrieve, and clear the selected values.
 * Both combos are optional (empty selection allowed).
 */
public class EvidenceQualifiersPanel extends JPanel{

	private static final String[] CERTAINTY_VALUES = {StringUtils.EMPTY, "challenged", "disproven", "proven"};
	private static final String[] CREDIBILITY_VALUES = {StringUtils.EMPTY, "0", "1", "2", "3"};

	private final JComboBox<String> certaintyCombo;
	private final JComboBox<String> credibilityCombo;

	/**
	 * Constructs a new panel with the given title.
	 *
	 * @param title the title to display in the TitledBorder
	 */
	public EvidenceQualifiersPanel(String title){
		this(title, CERTAINTY_VALUES, CREDIBILITY_VALUES);
	}

	/**
	 * Constructs a new panel with custom values for certainty and credibility.
	 *
	 * @param title             the title to display in the TitledBorder
	 * @param certaintyValues   the values for the certainty combo (may not be null)
	 * @param credibilityValues the values for the credibility combo (may not be null)
	 */
	public EvidenceQualifiersPanel(String title, String[] certaintyValues, String[] credibilityValues){
		setLayout(new MigLayout("ins 4", "[right]rel[grow]", "[][]"));
		setBorder(BorderFactory.createTitledBorder(title));

		certaintyCombo = new JComboBox<>(certaintyValues);
		credibilityCombo = new JComboBox<>(credibilityValues);

		// Set tooltips
		certaintyCombo.setToolTipText("Status code for the evidence: " +
			"challenged (suspect but unproven), disproven (proven false), proven (confirmed)");
		credibilityCombo.setToolTipText("Quantitative evaluation of credibility: " +
			"0=unreliable, 1=questionable, 2=secondary evidence, 3=direct/primary evidence");

		add(new JLabel("Certainty:"), "align label");
		add(certaintyCombo, "growx,wrap");
		add(new JLabel("Credibility:"), "align label");
		add(credibilityCombo, "growx");

		// Add mouse listeners to show tooltips more prominently on hover
		attachTooltipListener(certaintyCombo);
		attachTooltipListener(credibilityCombo);
	}

	/**
	 * Attaches a mouse listener that shows the tooltip on hover.
	 */
	private void attachTooltipListener(JComboBox<String> combo){
		combo.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseEntered(MouseEvent e){
				// Show tooltip immediately
				ToolTipManager.sharedInstance().mouseEntered(
					new MouseEvent(combo, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0,
						e.getX(), e.getY(), 0, false)
				);
			}
		});
	}


	/**
	 * Loads the selected values from the given strings.
	 *
	 * @param certainty   the certainty value (may be null)
	 * @param credibility the credibility value (may be null)
	 */
	public void load(String certainty, String credibility){
		certaintyCombo.setSelectedItem(certainty != null? certainty: StringUtils.EMPTY);
		credibilityCombo.setSelectedItem(credibility != null? credibility: StringUtils.EMPTY);
	}

	/**
	 * Returns the selected certainty.
	 *
	 * @return the certainty string, or an empty string if none selected
	 */
	public String getCertainty(){
		return (String)certaintyCombo.getSelectedItem();
	}

	/**
	 * Returns the selected credibility.
	 *
	 * @return the credibility string, or an empty string if none selected
	 */
	public String getCredibility(){
		return (String)credibilityCombo.getSelectedItem();
	}

	/**
	 * Checks if the panel has any data (i.e., a non-empty selection).
	 *
	 * @return true if either combo has a non-empty selection
	 */
	public boolean hasData(){
		String cert = getCertainty();
		String cred = getCredibility();
		return (cert != null && !cert.isEmpty()) || (cred != null && !cred.isEmpty());
	}

	/**
	 * Clears both combo selections to the empty string.
	 */
	public void clear(){
		certaintyCombo.setSelectedItem(StringUtils.EMPTY);
		credibilityCombo.setSelectedItem(StringUtils.EMPTY);
	}

	/**
	 * Sets the enabled state of both combos.
	 *
	 * @param enabled true to enable, false to disable
	 */
	@Override
	public void setEnabled(boolean enabled){
		super.setEnabled(enabled);
		certaintyCombo.setEnabled(enabled);
		credibilityCombo.setEnabled(enabled);
	}

	/**
	 * Returns the certainty combo for advanced customization.
	 *
	 * @return the certainty JComboBox
	 */
	public JComboBox<String> getCertaintyCombo(){
		return certaintyCombo;
	}

	/**
	 * Returns the credibility combo for advanced customization.
	 *
	 * @return the credibility JComboBox
	 */
	public JComboBox<String> getCredibilityCombo(){
		return credibilityCombo;
	}

	/**
	 * Sets a custom tooltip for the certainty combo.
	 *
	 * @param tooltip the tooltip text
	 */
	public void setCertaintyToolTip(String tooltip){
		certaintyCombo.setToolTipText(tooltip);
	}

	/**
	 * Sets a custom tooltip for the credibility combo.
	 *
	 * @param tooltip the tooltip text
	 */
	public void setCredibilityToolTip(String tooltip){
		credibilityCombo.setToolTipText(tooltip);
	}

}
