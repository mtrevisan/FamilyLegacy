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
package io.github.mtrevisan.familylegacy.ui.tools.research;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.research.ResearchHelper;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
		return I18N.t("menu.research.proof.summary");
	}

	@Override
	public void run(final ToolContext context){
		final FLEFModel model = context.model();

		final Map<String, List<FLEFRecord>> byStatus = new LinkedHashMap<>();
		for(final FLEFRecord conclusion : ResearchHelper.listConclusions(model)){
			final String status = ResearchHelper.proofStatus(conclusion);
			final String key = (status != null && !status.isBlank()
				? status.toLowerCase(Locale.ROOT): "(not set)");
			byStatus.computeIfAbsent(key, k -> new ArrayList<>())
				.add(conclusion);
		}

		final List<String> statuses = new ArrayList<>(DECLARED_STATUSES);
		for(final String status : byStatus.keySet())
			if(!statuses.contains(status))
				statuses.add(status);
		new ProofSummaryDialog(context.owner(), model, byStatus, statuses)
			.setVisible(true);
	}


	/** The four proof statuses declared by the protocol, in reading order. */
	private static final List<String> DECLARED_STATUSES = List.of(
		"conflicting_evidence",
		"supported",
		"proven",
		"disproven"
	);

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasAnyConclusions());
	}

}
