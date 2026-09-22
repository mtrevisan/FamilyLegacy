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
package io.github.mtrevisan.familylegacy.v2.ui.tools.research;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ReportDialog;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Report on the conclusions in the model, grouped by proof status.
 * <p>
 * The report answers the question "what has been decided, and how
 * solidly?", which is the natural companion of the research log: the
 * log shows what was searched, the proof summary shows what was
 * concluded.
 * <p>
 * The proof statuses are the four declared by the protocol:
 * {@code conflicting_evidence}, {@code supported}, {@code proven},
 * {@code disproven}. Conclusions with an unknown or missing status are
 * grouped under a separate heading, so nothing is silently dropped.
 */
public final class ProofSummaryTool implements ToolOperation{


	@Override
	public String getName(){
		return "Proof Summary…";
	}

	@Override
	public void run(final ToolContext context){
		final FLEFModel model = context.model();

		final Map<String, List<FLEFRecord>> byStatus = new LinkedHashMap<>();
		for(final FLEFRecord conclusion : ResearchHelper.listConclusions(model)){
			final String status = ResearchHelper.proofStatus(conclusion);
			final String key = (status != null && !status.isBlank()
				? status.toLowerCase(java.util.Locale.ROOT): "(not set)");
			byStatus.computeIfAbsent(key, k -> new ArrayList<>()).add(conclusion);
		}

		ReportDialog.showHtml(context.owner(), "Proof Summary",
			buildReport(byStatus));
	}


	/** The four proof statuses declared by the protocol, in reading order. */
	private static final List<String> DECLARED_STATUSES = List.of(
		"conflicting_evidence",
		"supported",
		"proven",
		"disproven"
	);

	private static String buildReport(final Map<String, List<FLEFRecord>> byStatus){
		final StringBuilder body = new StringBuilder();
		body.append("<h1>Proof Summary</h1>");

		int total = 0;
		for(final List<FLEFRecord> list : byStatus.values())
			total += list.size();
		body.append("<p>Total conclusions: <b>").append(total).append("</b></p>");

		if(total == 0){
			body.append("<p class='hint'>No conclusion has been recorded yet.</p>");
			return ReportDialog.document(body.toString());
		}

		// Counts per status, in declared order first, then any custom one.
		body.append("<h2>Status distribution</h2>");
		body.append("<table>");
		body.append("<tr><th>Status</th><th>Conclusions</th></tr>");
		for(final String status : DECLARED_STATUSES){
			final int count = byStatus.getOrDefault(status, List.of()).size();
			body.append("<tr><td>").append(ReportDialog.escape(status)).append("</td>")
				.append("<td>").append(count).append("</td></tr>");
		}
		for(final Map.Entry<String, List<FLEFRecord>> e : byStatus.entrySet()){
			if(DECLARED_STATUSES.contains(e.getKey()))
				continue;
			body.append("<tr><td>").append(ReportDialog.escape(e.getKey())).append("</td>")
				.append("<td>").append(e.getValue().size()).append("</td></tr>");
		}
		body.append("</table>");

		// Detail per status.
		for(final String status : DECLARED_STATUSES)
			appendStatusDetail(body, status, byStatus.getOrDefault(status, List.of()));
		for(final Map.Entry<String, List<FLEFRecord>> e : byStatus.entrySet()){
			if(DECLARED_STATUSES.contains(e.getKey()))
				continue;
			appendStatusDetail(body, e.getKey(), e.getValue());
		}

		return ReportDialog.document(body.toString());
	}

	private static void appendStatusDetail(final StringBuilder body, final String status,
		final List<FLEFRecord> conclusions){
		body.append("<h2>").append(ReportDialog.escape(status)).append("</h2>");
		if(conclusions.isEmpty()){
			body.append("<p class='hint'>No conclusion with this status.</p>");
			return;
		}
		body.append("<table>");
		body.append("<tr><th>Conclusion</th><th>Issue</th><th>Narrative</th></tr>");
		for(final FLEFRecord c : conclusions){
			body.append("<tr>");
			body.append("<td>").append(ReportDialog.escape(c.getId())).append("</td>");
			final String issue = ResearchHelper.conclusionIssue(c);
			body.append("<td>").append(ReportDialog.escape(issue != null? issue: StringUtils.EMPTY)).append("</td>");
			final String narrative = ResearchHelper.firstTextValue(c,
				ResearchHelper.TAG_NARRATIVE);
			body.append("<td>").append(ReportDialog.escape(narrative != null? narrative: StringUtils.EMPTY)).append("</td>");
			body.append("</tr>");
		}
		body.append("</table>");
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasAnyConclusions());
	}

}
