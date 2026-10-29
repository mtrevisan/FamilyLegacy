package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BoundComboBox;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


class TooltipTextsTest{

	private static void assertAllExplained(final List<String> missing, final String prefix, final String... codes){
		for(final String code : codes)
			if(code != null && !code.isEmpty() && I18N.t(prefix + "." + code + ".tooltip", null) == null)
				missing.add(prefix + "." + code + ".tooltip");
	}


	@Test
	void everyValueOfTheNonObviousEnumsIsExplained(){
		final List<String> missing = new ArrayList<>();
		assertAllExplained(missing, "enum.confidence", ConfidenceReader.CONFIDENCES);
		assertAllExplained(missing, "enum.proof_status", ConclusionReader.PROOF_STATUSES);
		assertAllExplained(missing, "enum.cultural_norm.rule_type", CulturalNormReader.TYPES);
		assertAllExplained(missing, "enum.context_impact.type", ContextImpactReader.IMPACT_TYPES);
		assertAllExplained(missing, "enum.research_activity.type", ResearchActivityReader.TYPES);
		assertAllExplained(missing, "enum.research_activity.search_scope", ResearchActivityReader.SEARCH_SCOPES);
		assertAllExplained(missing, "enum.research_activity.result", ResearchActivityReader.RESULTS);
		assertAllExplained(missing, "enum.historic_event.type", HistoricEventReader.TYPES);
		assertAllExplained(missing, "enum.research_task.status", ResearchTaskReader.STATUSES);
		assertAllExplained(missing, "enum.research_task.priority", ResearchTaskReader.PRIORITIES);
		assertAllExplained(missing, "enum.research_activity.status", ResearchActivityReader.STATUSES);
		assertAllExplained(missing, "enum.evidence.source_type", EvidenceQualifiersReader.SOURCE_TYPES);
		assertAllExplained(missing, "enum.evidence.information_type", EvidenceQualifiersReader.INFORMATION_TYPES);
		assertAllExplained(missing, "enum.evidence.evidence_type", EvidenceQualifiersReader.EVIDENCE_TYPES);

		Assertions.assertEquals(List.of(), missing);
	}

	@Test
	void tooltipHtmlEscapesAndSkipsBlankParagraphs(){
		Assertions.assertNull(GUIHelper.tooltipHtml(null, StringUtils.SPACE));

		final String html = GUIHelper.tooltipHtml("a < b & c", null, "second");
		Assertions.assertTrue(html.startsWith("<html>"));
		Assertions.assertTrue(html.contains("a &lt; b &amp; c<br><br>second"));
	}

	@Test
	void comboTooltipIsTheFieldTextFollowedByTheExplanationOfTheSelectedValue(){
		final BoundComboBox<String> combo = new BoundComboBox<>("confidence", ConfidenceReader.CONFIDENCES);
		combo.setI18NPrefix("enum.confidence");
		combo.setSelectedItem("high");

		final String tooltip = combo.getToolTipText();

		//field text (default of the prefix) and explanation of the value
		Assertions.assertTrue(tooltip.contains(I18N.t("enum.confidence.tooltip")));
		Assertions.assertTrue(tooltip.contains(Objects.requireNonNull(I18N.t("enum.confidence.high.tooltip", null))));
	}

}
