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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.privacy.PrivacyPolicy;
import io.github.mtrevisan.familylegacy.reports.OptionState;
import io.github.mtrevisan.familylegacy.reports.PersonOrder;
import io.github.mtrevisan.familylegacy.reports.ReportConfig;
import io.github.mtrevisan.familylegacy.reports.ReportOption;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import io.github.mtrevisan.familylegacy.reports.index.CollateralScope;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;


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
 * <p>The panel shows only what the report does with the options: an option that has no effect on it is not shown (and a group left
 * without options disappears), an option that the report always applies is shown on and cannot be changed. See
 * {@link OptionState}.</p>
 *
 * <p>{@link #toConfig(ReportLanguage)} reads the current state of the widgets
 * and returns a fresh immutable {@link ReportConfig}. The panel itself holds
 * no state beyond the widgets, so it can be shown, hidden or rebuilt without
 * leaking anything.</p>
 */
public final class ReportOptionsPanel extends JPanel{

	private final Function<ReportOption, OptionState> states;
	/** The options that the report always applies: they are on, whatever the configuration the panel was seeded with. */
	private final List<JCheckBox> alwaysOn = new ArrayList<>();


	/* ======================================================================
	 *                          Privacy
	 * ====================================================================== */

	private final JComboBox<PrivacyPolicy> privacyPolicy = createPrivacyPolicyCombo();
	private final JCheckBox showPrivacyDetails = cb("show_privacy_details");
	private final JCheckBox audit = cb("audit_trail");
	private final JCheckBox header = cb("document_header");


	/* ======================================================================
	 *                          Narrative
	 * ====================================================================== */

	private final JCheckBox introduction = cb("introduction");
	private final JCheckBox paternal = cb("paternal_ancestry");
	private final JCheckBox maternal = cb("maternal_ancestry");
	private final JCheckBox descendants = cb("descendants");
	private final JCheckBox directRel = cb("direct_relations");
	private final JCheckBox indirectRel = cb("indirect_relations");
	private final JComboBox<CollateralScope> collaterals = createCollateralCombo();
	private final JComboBox<Integer> generations = createGenerationsCombo();
	private final JComboBox<PersonOrder> personOrder = createPersonOrderCombo();


	/* ======================================================================
	 *                          Content sections
	 * ====================================================================== */

	private final JCheckBox notes = cb("notes");
	private final JCheckBox sources = cb("sources");
	private final JCheckBox media = cb("media");
	private final JCheckBox evidence = cb("evidence_qualifiers");
	private final JCheckBox contextResearch = cb("historic_context_research");
	private final JCheckBox culturalNorms = cb("cultural_norms");
	private final JCheckBox historicEvents = cb("historic_events");
	private final JCheckBox repositories = cb("repositories");
	private final JCheckBox originalSources = cb("include_original_sources");


	/* ======================================================================
	 *                          Groups
	 * ====================================================================== */

	private final JCheckBox groups = cb("groups_and_memberships");
	private final JCheckBox groupMembers = cb("show_group_members");
	private final JCheckBox groupSubgroups = cb("show_subgroups");
	private final JCheckBox groupAttributes = cb("show_group_attributes");


	/* ======================================================================
	 *                          Diagnostics
	 * ====================================================================== */

	private final JCheckBox eventFull = cb("full_event_participants");
	private final JCheckBox coverageReport = cb("coverage_report");
	private final JCheckBox timeline = cb("chronological_timeline");


	/* ======================================================================
	 *                          Indexes
	 * ====================================================================== */

	private final JCheckBox idxIndividuals = cb("index_of_individuals");
	private final JCheckBox idxPlaces = cb("index_of_places");


	/* ======================================================================
	 *                          Style
	 * ====================================================================== */

	private final JCheckBox descriptions = cb("include_descriptions");
	private final JCheckBox pictures = cb("include_pictures");


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
		this(initial, option -> OptionState.EDITABLE);
	}

	/**
	 * Builds a panel that shows the options as the report uses them.
	 *
	 * @param initial	The configuration to seed the panel with; must not be {@code null}.
	 * @param states	What the report does with each option.
	 */
	public ReportOptionsPanel(final ReportConfig initial, final Function<ReportOption, OptionState> states){
		super(new MigLayout("ins 0,wrap 1,fillx", "[grow,fill]"));

		this.states = states;

		for(final JPanel group : new JPanel[]{privacyGroup(), narrativeGroup(), contentGroup(), groupsGroup(), diagnosticsGroup(),
				indexesGroup(), styleGroup()})
			if(group != null)
				add(group, "growx");

		applyInitial(initial);
	}


	/**
	 * Puts the options of the configuration in the widgets (the ones that the report always applies stay on).
	 *
	 * @param config	The configuration to show.
	 */
	public void reset(final ReportConfig config){
		applyInitial(config);
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
		selectGenerations(initial.generations());
		personOrder.setSelectedItem(initial.personOrder());

		notes.setSelected(initial.notes());
		sources.setSelected(initial.sources());
		media.setSelected(initial.media());
		evidence.setSelected(initial.evidence());
		contextResearch.setSelected(initial.contextAndResearch());
		culturalNorms.setSelected(initial.culturalNorms());
		historicEvents.setSelected(initial.historicEvents());
		repositories.setSelected(initial.repositories());
		originalSources.setSelected(initial.includeOriginalSources());

		groups.setSelected(initial.groups());
		groupMembers.setSelected(initial.groupMembers());
		groupSubgroups.setSelected(initial.groupSubgroups());
		groupAttributes.setSelected(initial.groupAttributes());

		eventFull.setSelected(initial.eventFullParticipants());
		coverageReport.setSelected(initial.coverageReport());
		timeline.setSelected(initial.timeline());

		idxIndividuals.setSelected(initial.indexIndividuals());
		idxPlaces.setSelected(initial.indexPlaces());

		descriptions.setSelected(initial.includeDescriptions());
		pictures.setSelected(initial.includePictures());

		alwaysOn.forEach(box -> box.setSelected(true));
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
			(generations.getSelectedItem() instanceof final Integer count? count: 0),
			(PersonOrder)personOrder.getSelectedItem(),
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
			originalSources.isSelected(),
			eventFull.isSelected(),
			coverageReport.isSelected(),
			timeline.isSelected(),
			idxIndividuals.isSelected(),
			idxPlaces.isSelected(),
			descriptions.isSelected(),
			pictures.isSelected());
	}


	/** The orders of the persons of a book, by name ("alphabetical", "birth_date"), in the language of the user. */
	private static JComboBox<PersonOrder> createPersonOrderCombo(){
		final JComboBox<PersonOrder> combo = new JComboBox<>(PersonOrder.values());
		combo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus){
				final Object text = (value instanceof PersonOrder order? I18N.t("enum.person_order." + order.name().toLowerCase(Locale.ROOT)): value);
				return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
			}
		});
		return combo;
	}

	/** The numbers of generations to follow: all of them (0), or up to the number. */
	private static JComboBox<Integer> createGenerationsCombo(){
		final JComboBox<Integer> combo = new JComboBox<>(new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20});
		combo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus){
				final Object text = (Integer.valueOf(0).equals(value)? I18N.t("text.report.generations.all"): value);
				return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
			}
		});
		return combo;
	}

	/** Selects the number of generations, adding it to the list if the list does not have it (a value kept in the preferences). */
	private void selectGenerations(final int count){
		final DefaultComboBoxModel<Integer> model = (DefaultComboBoxModel<Integer>)generations.getModel();
		if(model.getIndexOf(count) < 0){
			int position = 0;
			while(position < model.getSize() && model.getElementAt(position) < count)
				position ++;
			model.insertElementAt(count, position);
		}
		generations.setSelectedItem(count);
	}

	/** The collateral scopes by name ("none", "spouses", ...), in the language of the user. */
	private static JComboBox<CollateralScope> createCollateralCombo(){
		final JComboBox<CollateralScope> combo = new JComboBox<>(CollateralScope.values());
		combo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus){
				final Object text = (value instanceof CollateralScope scope
					? I18N.t("enum.collateral_scope." + scope.name().toLowerCase(Locale.ROOT)): value);
				return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
			}
		});
		return combo;
	}

	/** The list of the privacy policies, shown with the same names as the View > Privacy menu. */
	private static JComboBox<PrivacyPolicy> createPrivacyPolicyCombo(){
		final JComboBox<PrivacyPolicy> combo = new JComboBox<>(PrivacyPolicy.values());
		combo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus){
				final Object text = (value instanceof PrivacyPolicy policy
					? I18N.t("menu.view.privacy." + policy.name().toLowerCase(Locale.ROOT)): value);
				return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
			}
		});
		return combo;
	}


	/* ======================================================================
	 *                          Group builders
	 * ====================================================================== */

	private JPanel privacyGroup(){
		final JPanel p = singleColumn("tab.title.privacy");
		p.add(privacyPolicy, "growx");
		addOption(p, ReportOption.SHOW_PRIVACY_DETAILS, showPrivacyDetails);
		addOption(p, ReportOption.AUDIT, audit);
		addOption(p, ReportOption.HEADER, header);
		return p;
	}


	/**
	 * Two-column group. Uses {@link GridLayout} so both columns have exactly
	 * the same width, regardless of the natural width of the labels.
	 */
	private JPanel narrativeGroup(){
		final JPanel p = twoColumns("border.narrative");
		addOption(p, ReportOption.INTRODUCTION, introduction);
		addOption(p, ReportOption.DESCENDANTS, descendants);
		addOption(p, ReportOption.PATERNAL_ANCESTRY, paternal);
		addOption(p, ReportOption.DIRECT_RELATIONS, directRel);
		addOption(p, ReportOption.MATERNAL_ANCESTRY, maternal);
		addOption(p, ReportOption.INDIRECT_RELATIONS, indirectRel);
		addCombo(p, ReportOption.COLLATERAL_SCOPE, "label.collaterals", "tooltip.report.collaterals", collaterals);
		addCombo(p, ReportOption.GENERATIONS, "label.generations", "tooltip.report.generations", generations);
		addCombo(p, ReportOption.PERSON_ORDER, "label.person_order", "tooltip.report.person_order", personOrder);
		return nonEmpty(p);
	}


	private JPanel contentGroup(){
		final JPanel p = twoColumns("border.content_sections");
		addOption(p, ReportOption.NOTES, notes);
		addOption(p, ReportOption.CONTEXT_AND_RESEARCH, contextResearch);
		addOption(p, ReportOption.SOURCES, sources);
		addOption(p, ReportOption.CULTURAL_NORMS, culturalNorms);
		addOption(p, ReportOption.MEDIA, media);
		addOption(p, ReportOption.HISTORIC_EVENTS, historicEvents);
		addOption(p, ReportOption.EVIDENCE, evidence);
		addOption(p, ReportOption.REPOSITORIES, repositories);
		addOption(p, ReportOption.INCLUDE_ORIGINAL_SOURCES, originalSources);
		return nonEmpty(p);
	}


	private JPanel groupsGroup(){
		final JPanel p = twoColumns("border.group_report");
		addOption(p, ReportOption.GROUPS, groups);
		addOption(p, ReportOption.GROUP_MEMBERS, groupMembers);
		addOption(p, ReportOption.GROUP_SUBGROUPS, groupSubgroups);
		addOption(p, ReportOption.GROUP_ATTRIBUTES, groupAttributes);
		return nonEmpty(p);
	}


	private JPanel diagnosticsGroup(){
		final JPanel p = twoColumns("border.diagnostics");
		addOption(p, ReportOption.EVENT_FULL_PARTICIPANTS, eventFull);
		addOption(p, ReportOption.COVERAGE_REPORT, coverageReport);
		addOption(p, ReportOption.TIMELINE, timeline);
		return nonEmpty(p);
	}


	private JPanel indexesGroup(){
		final JPanel p = singleColumn("border.indexes");
		addOption(p, ReportOption.INDEX_INDIVIDUALS, idxIndividuals);
		addOption(p, ReportOption.INDEX_PLACES, idxPlaces);
		return nonEmpty(p);
	}


	private JPanel styleGroup(){
		final JPanel p = singleColumn("border.style");
		addOption(p, ReportOption.INCLUDE_DESCRIPTIONS, descriptions);
		addOption(p, ReportOption.INCLUDE_PICTURES, pictures);
		return nonEmpty(p);
	}


	/** Adds the check box unless the report does nothing with the option; one that the report always applies is on and disabled. */
	private void addOption(final JPanel group, final ReportOption option, final JCheckBox box){
		switch(states.apply(option)){
			case HIDDEN -> {
				return;
			}
			case ALWAYS_ON -> {
				box.setEnabled(false);
				box.setToolTipText(I18N.t("tooltip.report.always_included"));
				alwaysOn.add(box);
			}
			case EDITABLE -> {
			}
		}
		group.add(box);
	}

	/** Adds a label and a combo, still inside the grid, on a row of their own. */
	private void addCombo(final JPanel group, final ReportOption option, final String labelKey, final String tooltipKey,
			final JComboBox<?> combo){
		if(states.apply(option) == OptionState.HIDDEN)
			return;

		if(group.getComponentCount() % 2 != 0)
			group.add(new JLabel());
		final JLabel label = new JLabel(I18N.l(labelKey));
		label.setToolTipText(I18N.t(tooltipKey, null));
		combo.setToolTipText(I18N.t(tooltipKey, null));
		group.add(label);
		group.add(combo);
	}

	private static JPanel nonEmpty(final JPanel group){
		return (group.getComponentCount() > 0? group: null);
	}

	private static JPanel twoColumns(final String titleKey){
		final JPanel p = new JPanel(new GridLayout(0, 2, 12, 2));
		p.setBorder(BorderFactory.createTitledBorder(I18N.t(titleKey)));
		return p;
	}

	private static JPanel singleColumn(final String titleKey){
		final JPanel p = new JPanel(new MigLayout("ins 4 8 8 8,wrap 1,fillx", "[grow,fill]", "[]2[]2[]2[]"));
		p.setBorder(BorderFactory.createTitledBorder(I18N.t(titleKey)));
		return p;
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static JCheckBox cb(final String key){
		final JCheckBox box = new JCheckBox(I18N.t("checkbox.report." + key));
		box.setToolTipText(I18N.t("tooltip.report." + key, null));
		return box;
	}

}
