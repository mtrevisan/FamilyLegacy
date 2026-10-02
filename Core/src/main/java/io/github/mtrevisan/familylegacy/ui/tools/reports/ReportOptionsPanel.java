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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridLayout;


/**
 * Swing panel exposing every {@link ReportConfig} option.
 *
 * <p>The panel is a flat form composed of titled groups. Each group mirrors
 * a logical cluster of the configuration record, so the user can reason
 * about the report in terms of intent (privacy, content, diagnostics, ...)
 * rather than by the underlying field order.</p>
 *
 * <p>Groups that lay their checkboxes out in two columns use
 * {@link GridLayout} rather than {@link MigLayout}: because {@code GridLayout}
 * forces both columns to the same width (half of the panel minus the gap),
 * the split point is identical across every two-column group, and the
 * checkboxes of the second column line up vertically from section to
 * section. With {@code MigLayout} each group would compute its own column
 * widths from its own natural content, producing the misalignment this
 * layout is designed to prevent.</p>
 *
 * <p>{@link #toConfig(ReportLanguage)} reads the current state of the widgets
 * and returns a fresh immutable {@link ReportConfig}. The panel itself holds
 * no state beyond the widgets, so it can be shown, hidden or rebuilt without
 * leaking anything.</p>
 */
public final class ReportOptionsPanel extends JPanel{

	/* ======================================================================
	 *                          Privacy
	 * ====================================================================== */

	private final JComboBox<PrivacyPolicy> privacyPolicy = new JComboBox<>(PrivacyPolicy.values());
	private final JCheckBox showPrivacyDetails = cb("Show privacy details");
	private final JCheckBox audit = cb("Audit trail");
	private final JCheckBox header = cb("Document header");


	/* ======================================================================
	 *                          Narrative
	 * ====================================================================== */

	private final JCheckBox introduction = cb("Introduction");
	private final JCheckBox paternal = cb("Paternal ancestry");
	private final JCheckBox maternal = cb("Maternal ancestry");
	private final JCheckBox descendants = cb("Descendants");
	private final JCheckBox directRel = cb("Direct relations");
	private final JCheckBox indirectRel = cb("Indirect relations");
	private final JComboBox<CollateralScope> collaterals = new JComboBox<>(CollateralScope.values());


	/* ======================================================================
	 *                          Content sections
	 * ====================================================================== */

	private final JCheckBox notes = cb("Notes");
	private final JCheckBox sources = cb("Sources");
	private final JCheckBox media = cb("Media");
	private final JCheckBox evidence = cb("Evidence qualifiers");
	private final JCheckBox contextResearch = cb("Historic context & research");
	private final JCheckBox culturalNorms = cb("Cultural norms");
	private final JCheckBox historicEvents = cb("Historic events");
	private final JCheckBox repositories = cb("Repositories");


	/* ======================================================================
	 *                          Groups
	 * ====================================================================== */

	private final JCheckBox groups = cb("Groups and memberships");
	private final JCheckBox groupMembers = cb("Show group members");
	private final JCheckBox groupSubgroups = cb("Show subgroups");
	private final JCheckBox groupAttributes = cb("Show group attributes");


	/* ======================================================================
	 *                          Diagnostics
	 * ====================================================================== */

	private final JCheckBox eventFull = cb("Full event participants");
	private final JCheckBox dateProvenance = cb("Date provenance");
	private final JCheckBox coverageReport = cb("Coverage report");
	private final JCheckBox timeline = cb("Chronological timeline");


	/* ======================================================================
	 *                          Indexes
	 * ====================================================================== */

	private final JCheckBox idxIndividuals = cb("Index of individuals");
	private final JCheckBox idxPlaces = cb("Index of places");


	/* ======================================================================
	 *                          Style
	 * ====================================================================== */

	private final JCheckBox descriptions = cb("Include descriptions");
	private final JCheckBox pictures = cb("Include pictures");


	/* ======================================================================
	 *                          Construction
	 * ====================================================================== */

	/**
	 * Builds a panel whose initial state mirrors the given configuration.
	 *
	 * @param initial the configuration to seed the panel with; must not be
	 *                {@code null}
	 */
	public ReportOptionsPanel(final ReportConfig initial){
		super(new MigLayout("ins 0,wrap 1,fillx", "[grow,fill]", "[]8[]8[]8[]8[]8[]8[]"));

		add(privacyGroup(), "growx");
		add(narrativeGroup(), "growx");
		add(contentGroup(), "growx");
		add(groupsGroup(), "growx");
		add(diagnosticsGroup(), "growx");
		add(indexesGroup(), "growx");
		add(styleGroup(), "growx");

		applyInitial(initial);
	}


	/** Copies every field of {@code initial} into the corresponding widget. */
	private void applyInitial(final ReportConfig initial){
		privacyPolicy.setSelectedItem(initial.privacyPolicy());
		showPrivacyDetails.setSelected(initial.showPrivacyDetails());
		audit.setSelected(initial.audit());
		header.setSelected(initial.header());

		introduction.setSelected(initial.introduction());
		paternal.setSelected(initial.paternalAncestry());
		maternal.setSelected(initial.maternalAncestry());
		descendants.setSelected(initial.descendants());
		directRel.setSelected(initial.directRelations());
		indirectRel.setSelected(initial.indirectRelations());
		collaterals.setSelectedItem(initial.collateralScope());

		notes.setSelected(initial.notes());
		sources.setSelected(initial.sources());
		media.setSelected(initial.media());
		evidence.setSelected(initial.evidence());
		contextResearch.setSelected(initial.contextAndResearch());
		culturalNorms.setSelected(initial.culturalNorms());
		historicEvents.setSelected(initial.historicEvents());
		repositories.setSelected(initial.repositories());

		groups.setSelected(initial.groups());
		groupMembers.setSelected(initial.groupMembers());
		groupSubgroups.setSelected(initial.groupSubgroups());
		groupAttributes.setSelected(initial.groupAttributes());

		eventFull.setSelected(initial.eventFullParticipants());
		dateProvenance.setSelected(initial.dateProvenance());
		coverageReport.setSelected(initial.coverageReport());
		timeline.setSelected(initial.timeline());

		idxIndividuals.setSelected(initial.indexIndividuals());
		idxPlaces.setSelected(initial.indexPlaces());

		descriptions.setSelected(initial.includeDescriptions());
		pictures.setSelected(initial.includePictures());
	}


	/* ======================================================================
	 *                          Read-back
	 * ====================================================================== */

	/**
	 * Reads the current widget state and returns a fresh configuration.
	 *
	 * @param selectedLanguage the selected language from external dialog
	 * @return the current configuration, never {@code null}
	 */
	public ReportConfig toConfig(final ReportLanguage selectedLanguage){
		final PrivacyPolicy policy = (PrivacyPolicy)privacyPolicy.getSelectedItem();
		final ReportConfig defaults = ReportConfig.defaults();

		return new ReportConfig(
			selectedLanguage != null? selectedLanguage: defaults.language(),
			policy != null? policy: defaults.privacyPolicy(),
			showPrivacyDetails.isSelected(),
			audit.isSelected(),
			header.isSelected(),
			introduction.isSelected(),
			paternal.isSelected(),
			maternal.isSelected(),
			descendants.isSelected(),
			directRel.isSelected(),
			indirectRel.isSelected(),
			(CollateralScope)collaterals.getSelectedItem(),
			notes.isSelected(),
			sources.isSelected(),
			media.isSelected(),
			evidence.isSelected(),
			contextResearch.isSelected(),
			culturalNorms.isSelected(),
			groups.isSelected(),
			groupMembers.isSelected(),
			groupSubgroups.isSelected(),
			groupAttributes.isSelected(),
			historicEvents.isSelected(),
			repositories.isSelected(),
			eventFull.isSelected(),
			dateProvenance.isSelected(),
			coverageReport.isSelected(),
			timeline.isSelected(),
			idxIndividuals.isSelected(),
			idxPlaces.isSelected(),
			descriptions.isSelected(),
			pictures.isSelected());
	}


	/* ======================================================================
	 *                          Group builders
	 * ====================================================================== */

	private JPanel privacyGroup(){
		final JPanel p = new JPanel(new MigLayout("ins 4 8 8 8,wrap 1,fillx", "[grow,fill]", "[]2[]2[]2[]"));
		p.setBorder(BorderFactory.createTitledBorder("Privacy"));
		p.add(privacyPolicy, "growx");
		p.add(showPrivacyDetails);
		p.add(audit);
		p.add(header);
		return p;
	}


	/**
	 * Two-column group. Uses {@link GridLayout} so both columns have exactly
	 * the same width, regardless of the natural width of the labels.
	 */
	private JPanel narrativeGroup(){
		final JPanel p = new JPanel(new GridLayout(0, 2, 12, 2));
		p.setBorder(BorderFactory.createTitledBorder("Narrative"));
		p.add(introduction);
		p.add(descendants);
		p.add(paternal);
		p.add(directRel);
		p.add(maternal);
		p.add(indirectRel);
		// A small label + combo, still inside the grid.
		p.add(new JLabel("Collaterals:"));
		p.add(collaterals);
		return p;
	}


	private JPanel contentGroup(){
		final JPanel p = new JPanel(new GridLayout(0, 2, 12, 2));
		p.setBorder(BorderFactory.createTitledBorder("Content sections"));
		p.add(notes);
		p.add(contextResearch);
		p.add(sources);
		p.add(culturalNorms);
		p.add(media);
		p.add(historicEvents);
		p.add(evidence);
		p.add(repositories);
		return p;
	}


	private JPanel groupsGroup(){
		final JPanel p = new JPanel(new GridLayout(0, 2, 12, 2));
		p.setBorder(BorderFactory.createTitledBorder("Group report"));
		p.add(groups);
		p.add(groupMembers);
		p.add(groupSubgroups);
		p.add(groupAttributes);
		return p;
	}


	private JPanel diagnosticsGroup(){
		final JPanel p = new JPanel(new GridLayout(0, 2, 12, 2));
		p.setBorder(BorderFactory.createTitledBorder("Diagnostics"));
		p.add(eventFull);
		p.add(dateProvenance);
		p.add(coverageReport);
		p.add(timeline);
		return p;
	}


	private JPanel indexesGroup(){
		final JPanel p = new JPanel(new MigLayout("ins 4 8 8 8,wrap 1,fillx", "[grow,fill]", "[]2[]"));
		p.setBorder(BorderFactory.createTitledBorder("Indexes"));
		p.add(idxIndividuals);
		p.add(idxPlaces);
		return p;
	}


	private JPanel styleGroup(){
		final JPanel p = new JPanel(new MigLayout("ins 4 8 8 8,wrap 1,fillx", "[grow,fill]", "[]2[]"));
		p.setBorder(BorderFactory.createTitledBorder("Style"));
		p.add(descriptions);
		p.add(pictures);
		return p;
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static JCheckBox cb(final String text){
		return new JCheckBox(text);
	}

}
