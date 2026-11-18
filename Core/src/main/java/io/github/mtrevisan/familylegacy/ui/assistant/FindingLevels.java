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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.features.FeatureVisibilityService;

import java.util.List;
import java.util.Set;


/**
 * The lowest interface level at which a finding is listed in the palette and counted by the indicator of the main window (progressive
 * disclosure): the findings about the research workflow are for the standard level, the ones about the historical context for the
 * advanced one, all the others (dates, relationships, places, sources...) are for everyone. A hidden finding is not lost: it is computed
 * all the same, and it is listed as soon as the level is raised. The editors suggest by their own rules (see the suggestions of the
 * editors).
 */
final class FindingLevels{

	private static final Set<String> STANDARD_KINDS = Set.of("conclusion.unresolved_identity", "gap.activities_without_result",
		"gap.no_follow_up", "gap.overdue_task", "gap.question_stalled", "gap.question_without_activities");
	/** The one of the context that a basic user is told about, in the editor of the date (a date estimated by a custom). */
	private static final String BASIC_CONTEXT_KIND = "context.norm_estimate";


	private FindingLevels(){}


	/** @return the lowest level the finding is listed at. */
	static UxLevel minimumLevel(final Diagnostic finding){
		final String kind = finding.kind();
		if(kind.startsWith("context.") && !kind.equals(BASIC_CONTEXT_KIND))
			return UxLevel.ADVANCED;
		if(STANDARD_KINDS.contains(kind) || kind.startsWith("planner."))
			return UxLevel.STANDARD;
		return UxLevel.BASIC;
	}

	/** @return the findings that are listed at the level of the user now. */
	static List<Diagnostic> visible(final List<Diagnostic> findings){
		return visibleAt(findings, FeatureVisibilityService.shared().level());
	}

	static List<Diagnostic> visibleAt(final List<Diagnostic> findings, final UxLevel level){
		return findings.stream()
			.filter(finding -> level.includes(minimumLevel(finding)))
			.toList();
	}

}
