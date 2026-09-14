package io.github.mtrevisan.familylegacy.v2.ui.tools.research;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ReportDialog;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;


/**
 * Report on the distribution of {@code EvidenceQualifiers} across the
 * model.
 * <p>
 * Three dimensions are counted, as defined by the protocol:
 * <ul>
 *   <li>{@code source_type} — original or derived;</li>
 *   <li>{@code information_type} — primary, secondary, or
 *       undetermined;</li>
 *   <li>{@code evidence_type} — direct, indirect, or negative.</li>
 * </ul>
 * The report also lists the records that carry no evidence qualifiers at
 * all, which is the most common gap in a model that has not been fully
 * assessed.
 * <p>
 * The scan is done in a single pass over every record type that can
 * carry an {@code evidence} block, and the qualifiers are read from the
 * block once per record. No O(n²) lookup is involved.
 */
public final class EvidenceQualifiersTool implements ToolOperation{


	@Override
	public String getName(){
		return "Evidence Qualifiers…";
	}

	@Override
	public void run(final ToolContext context){
		final FLEFModel model = context.model();

		final Map<String, Integer> sourceTypes = new LinkedHashMap<>();
		final Map<String, Integer> informationTypes = new LinkedHashMap<>();
		final Map<String, Integer> evidenceTypes = new LinkedHashMap<>();
		int withQualifiers = 0;
		int withoutQualifiers = 0;

		for(final String type : SCANNED_TYPES){
			for(final FLEFRecord record : model.getRecordsByType(type)){
				final FLEFRecord qualifiers = FLEFRecordHelper.findChild(record,
					ResearchHelper.TAG_EVIDENCE);
				if(qualifiers == null){
					withoutQualifiers++;
					continue;
				}
				withQualifiers++;
				merge(sourceTypes, ResearchHelper.firstTextValue(qualifiers,
					ResearchHelper.TAG_SOURCE_TYPE));
				merge(informationTypes, ResearchHelper.firstTextValue(qualifiers,
					ResearchHelper.TAG_INFORMATION_TYPE));
				merge(evidenceTypes, ResearchHelper.firstTextValue(qualifiers,
					ResearchHelper.TAG_EVIDENCE_TYPE));
			}
		}

		ReportDialog.showHtml(context.owner(), "Evidence Qualifiers",
			buildReport(sourceTypes, informationTypes, evidenceTypes,
				withQualifiers, withoutQualifiers));
	}


	/**
	 * Record types that can carry an {@code evidence} block. The list is
	 * limited to the types where evidence qualifiers are meaningful in
	 * practice.
	 */
	private static final String[] SCANNED_TYPES = {
		"event", "individual_attribute", "group_attribute", "relationship",
		"place", "place_relationship", "event_participation", "source",
		"cultural_norm", "historic_event", "context_impact",
		"identity_hypothesis"
	};

	private static void merge(final Map<String, Integer> map, final String value){
		final String key = (value != null && !value.isBlank()? value: "(not set)");
		map.merge(key, 1, Integer::sum);
	}

	private static String buildReport(final Map<String, Integer> sourceTypes,
		final Map<String, Integer> informationTypes,
		final Map<String, Integer> evidenceTypes,
		final int withQualifiers, final int withoutQualifiers){
		final StringBuilder body = new StringBuilder();
		body.append("<h1>Evidence Qualifiers</h1>");

		body.append("<h2>Summary</h2>");
		body.append("<table>");
		body.append("<tr><td>Records with qualifiers</td><td>").append(withQualifiers).append("</td></tr>");
		body.append("<tr><td>Records without qualifiers</td><td>").append(withoutQualifiers).append("</td></tr>");
		body.append("</table>");

		appendDimension(body, "Source type", sourceTypes);
		appendDimension(body, "Information type", informationTypes);
		appendDimension(body, "Evidence type", evidenceTypes);

		if(withoutQualifiers > 0)
			body.append("<p class='hint'>Records without qualifiers are those whose "
				+ "evidence has not yet been assessed. Their count is not an error: "
				+ "it is a measure of how much of the model remains to be reviewed.</p>");

		return ReportDialog.document(body.toString());
	}

	private static void appendDimension(final StringBuilder body, final String title,
		final Map<String, Integer> counts){
		body.append("<h2>").append(title).append("</h2>");
		if(counts.isEmpty()){
			body.append("<p class='hint'>No value recorded.</p>");
			return;
		}
		body.append("<table>");
		body.append("<tr><th>Value</th><th>Count</th></tr>");
		final Map<String, Integer> sorted = new TreeMap<>(counts);
		for(final Map.Entry<String, Integer> e : sorted.entrySet()){
			body.append("<tr><td>").append(ReportDialog.escape(e.getKey()))
				.append("</td><td>").append(e.getValue()).append("</td></tr>");
		}
		body.append("</table>");
	}

}
