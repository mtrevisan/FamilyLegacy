package io.github.mtrevisan.familylegacy.v2.ui.components;

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
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


	private String path;

	private final JComboBox<String> certaintyCombo;
	private final JComboBox<String> credibilityCombo;


	/**
	 * Constructs a new panel with the given title.
	 *
	 * @param title the title to display in the TitledBorder
	 */
	public EvidenceQualifiersPanel(final String path, final String title){
		this(path, title, CERTAINTY_VALUES, CREDIBILITY_VALUES);
	}

	/**
	 * Constructs a new panel with custom values for certainty and credibility.
	 *
	 * @param title	The title to display in the {@code TitledBorder}.
	 * @param certaintyValues	The values for the certainty combo (may not be {@code null}).
	 * @param credibilityValues	The values for the credibility combo (may not be {@code null}).
	 */
	public EvidenceQualifiersPanel(final String path, final String title, final String[] certaintyValues, final String[] credibilityValues){
		this.path = (path != null && !path.isEmpty()? path + ".": StringUtils.EMPTY);


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
	private void attachTooltipListener(final JComboBox<String> combo){
		combo.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseEntered(final MouseEvent e){
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
	 * @param record the record to read from
	 */
	public void load(final FLEFRecord record){
		final String certainty = FLEFRecordUtils.getChildValue(record, path + "CERTAINTY");
		certaintyCombo.setSelectedItem(certainty != null? certainty: StringUtils.EMPTY);

		final String credibility = FLEFRecordUtils.getChildValue(record, path + "CREDIBILITY");
		credibilityCombo.setSelectedItem(credibility != null? credibility: StringUtils.EMPTY);
	}

	public void save(final FLEFRecord record){
		final String certainty = getCertainty();
		FLEFRecordUtils.updateChildValue(record, path + "CERTAINTY", certainty);

		final String credibility = getCredibility();
		FLEFRecordUtils.updateChildValue(record, path + "CREDIBILITY", credibility);
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
		final String certainty = getCertainty();
		final String credibility = getCredibility();
		return (certainty != null && !certainty.isEmpty()) || (credibility != null && !credibility.isEmpty());
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
	public void setEnabled(final boolean enabled){
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
	public void setCertaintyToolTip(final String tooltip){
		certaintyCombo.setToolTipText(tooltip);
	}

	/**
	 * Sets a custom tooltip for the credibility combo.
	 *
	 * @param tooltip the tooltip text
	 */
	public void setCredibilityToolTip(final String tooltip){
		credibilityCombo.setToolTipText(tooltip);
	}

}
