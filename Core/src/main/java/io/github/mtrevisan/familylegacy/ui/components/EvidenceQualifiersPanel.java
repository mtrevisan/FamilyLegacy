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

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * Reusable panel that groups all evidence qualifiers as defined in the FLEF protocol according to FLEF 0.1.3:
 * source_type, information_type, and evidence_type.
 * All combos are optional (empty selection allowed).
 * <p>
 * Structure:
 * <pre>
 * struct EvidenceQualifiers {
 *   source_type?: enum { original, derived }
 *   information_type?: enum { primary, secondary, undetermined }
 *   evidence_type?: enum { direct, indirect, negative }
 * }
* </pre>
 */
public class EvidenceQualifiersPanel extends JPanel{

	private final String path;

	private final BoundComboBox<String> sourceTypeCombo;
	private final BoundComboBox<String> informationTypeCombo;
	private final BoundComboBox<String> evidenceTypeCombo;


	/**
	 * Constructs a new panel with the given path prefix and title.
	 *
	 * @param path	the path prefix for child fields (e.g., "EVIDENCE" or "EVIDENCE.QUALIFIERS")
	 * @param panelTitle	the title to display in the TitledBorder
	 */
	public EvidenceQualifiersPanel(final String path, final String panelTitle){
		this.path = (StringUtils.isNotEmpty(path)? path: StringUtils.EMPTY);

		setLayout(GUIHelper.createLabelFieldLayout(5, "[]5[]5[]"));
		setBorder(BorderFactory.createTitledBorder(panelTitle));

		sourceTypeCombo = new BoundComboBox<>(EvidenceQualifiersReader.TAG_SOURCE_TYPE, GUIHelper.fillCombo(EvidenceQualifiersReader.SOURCE_TYPES, null));
		sourceTypeCombo.setI18NPrefix("enum.evidence.source.type");
		sourceTypeCombo.setToolTipText(I18N.t("dialog.evidence.qualifiers.source.type.tooltip"));
		informationTypeCombo = new BoundComboBox<>(EvidenceQualifiersReader.TAG_INFORMATION_TYPE, GUIHelper.fillCombo(EvidenceQualifiersReader.INFORMATION_TYPES, null));
		informationTypeCombo.setI18NPrefix("enum.evidence.information.type");
		informationTypeCombo.setToolTipText(I18N.t("dialog.evidence.qualifiers.information.type.tooltip"));
		evidenceTypeCombo = new BoundComboBox<>(EvidenceQualifiersReader.TAG_EVIDENCE_TYPE, GUIHelper.fillCombo(EvidenceQualifiersReader.EVIDENCE_TYPES, null));
		evidenceTypeCombo.setI18NPrefix("enum.evidence.evidence.type");
		evidenceTypeCombo.setToolTipText(I18N.t("dialog.evidence.qualifiers.evidence.type.tooltip"));

		// Layout: label + combo per row
		GUIHelper.addLabeledComponent(this, I18N.t("dialog.evidence.qualifiers.source.type") + ":", sourceTypeCombo);
		GUIHelper.addLabeledComponent(this, I18N.t("dialog.evidence.qualifiers.information.type") + ":", informationTypeCombo);
		GUIHelper.addLabeledComponent(this, I18N.t("dialog.evidence.qualifiers.evidence.type") + ":", evidenceTypeCombo);

		// Attach hover tooltip listeners
		attachTooltipListener(sourceTypeCombo);
		attachTooltipListener(informationTypeCombo);
		attachTooltipListener(evidenceTypeCombo);
	}

	private void attachTooltipListener(final JComboBox<String> combo){
		combo.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseEntered(final MouseEvent e){
				ToolTipManager.sharedInstance().mouseEntered(
					new MouseEvent(combo, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0,
						e.getX(), e.getY(), 0, false)
				);
			}
		});
	}

	/**
	 * Loads the selected values from the given record.
	 *
	 * @param record	the record to read from
	 */
	public void load(final FLEFRecord record){
		clear();

		if(record == null || record.isEmpty())
			return;

		String value = FLEFRecordHelper.getChildValue(record, FLEFRecordHelper.composePath(path, EvidenceQualifiersReader.TAG_SOURCE_TYPE));
		sourceTypeCombo.setSelectedItem(StringUtils.defaultString(value));

		value = FLEFRecordHelper.getChildValue(record, FLEFRecordHelper.composePath(path, EvidenceQualifiersReader.TAG_INFORMATION_TYPE));
		informationTypeCombo.setSelectedItem(StringUtils.defaultString(value));

		value = FLEFRecordHelper.getChildValue(record, FLEFRecordHelper.composePath(path, EvidenceQualifiersReader.TAG_EVIDENCE_TYPE));
		evidenceTypeCombo.setSelectedItem(StringUtils.defaultString(value));
	}

	/**
	 * Saves the selected values into the given record.
	 *
	 * @param record	the record to save into
	 */
	public void save(final FLEFRecord record){
		FLEFRecordHelper.updateChildValue(record, FLEFRecordHelper.composePath(path, EvidenceQualifiersReader.TAG_SOURCE_TYPE), getSourceType());
		FLEFRecordHelper.updateChildValue(record, FLEFRecordHelper.composePath(path, EvidenceQualifiersReader.TAG_INFORMATION_TYPE), getInformationType());
		FLEFRecordHelper.updateChildValue(record, FLEFRecordHelper.composePath(path, EvidenceQualifiersReader.TAG_EVIDENCE_TYPE), getEvidenceType());
	}

	public String getSourceType(){
		return (String)sourceTypeCombo.getSelectedItem();
	}

	public String getInformationType(){
		return (String)informationTypeCombo.getSelectedItem();
	}

	public String getEvidenceType(){
		return (String)evidenceTypeCombo.getSelectedItem();
	}

	/**
	 * Checks if any field has a non-empty selection.
	 *
	 * @return true if at least one combo has a value
	 */
	public boolean hasData(){
		return (!getSourceType().isEmpty() ||
			!getInformationType().isEmpty() ||
			!getEvidenceType().isEmpty());
	}

	/**
	 * Clears all combos to the empty string.
	 */
	public void clear(){
		sourceTypeCombo.setSelectedItem(StringUtils.EMPTY);
		informationTypeCombo.setSelectedItem(StringUtils.EMPTY);
		evidenceTypeCombo.setSelectedItem(StringUtils.EMPTY);
	}

	/**
	 * Sets the enabled state of all combos.
	 *
	 * @param enabled	true to enable, false to disable
	 */
	@Override
	public void setEnabled(final boolean enabled){
		super.setEnabled(enabled);

		sourceTypeCombo.setEnabled(enabled);
		informationTypeCombo.setEnabled(enabled);
		evidenceTypeCombo.setEnabled(enabled);
	}

}
