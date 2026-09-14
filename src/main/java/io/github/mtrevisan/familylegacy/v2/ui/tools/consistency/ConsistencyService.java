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
package io.github.mtrevisan.familylegacy.v2.ui.tools.consistency;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ReportDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Detects structural inconsistencies in a {@link FLEFModel}.
 * <p>
 * The checks are conservative: they only flag issues that are provably
 * wrong from the graph alone, without requiring date parsing or external
 * knowledge. This avoids false positives that would train the user to
 * ignore the report.
 * <p>
 * The checks are:
 * <ul>
 *   <li><b>Individual without a name</b> — every individual record
 *       should carry at least one name;</li>
 *   <li><b>Circular parentage</b> — a chain of {@code biological_child}
 *       relationships must not return to its starting individual;</li>
 *   <li><b>Self-referencing relationship</b> — a relationship whose
 *       subject and target resolve to the same id.</li>
 * </ul>
 * More checks (dates, ages, marriage validity) require field-level
 * parsing and are left for a future, date-aware pass.
 */
public final class ConsistencyService{

	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TYPE_BIOLOGICAL_CHILD = "biological_child";


	/** One inconsistency detected. */
	public record Issue(Severity severity, String recordId, String message){}

	public enum Severity{ WARNING, ERROR }

	/** Result of the consistency check. */
	public record Report(int individualsChecked, int relationshipsChecked, List<Issue> issues){

		public boolean isClean(){
			return issues.isEmpty();
		}

		public String toHtml(){
			final StringBuilder body = new StringBuilder();
			body.append("<h1>Consistency Check</h1>");
			body.append("<p>Individuals checked: <b>").append(individualsChecked).append("</b></p>");
			body.append("<p>Relationships checked: <b>").append(relationshipsChecked).append("</b></p>");

			if(isClean()){
				body.append("<p class='ok'><b>✓ No structural inconsistency found.</b></p>");
				return ReportDialog.document(body.toString());
			}

			body.append("<p class='warn'><b>Issues found: ").append(issues.size()).append("</b></p>");
			body.append("<table>");
			body.append("<tr><th>Severity</th><th>Record</th><th>Description</th></tr>");
			for(final Issue issue : issues){
				final String cls = (issue.severity() == Severity.ERROR? "err": "warn");
				body.append("<tr>");
				body.append("<td class='").append(cls).append("'>").append(issue.severity()).append("</td>");
				body.append("<td>").append(ReportDialog.escape(issue.recordId())).append("</td>");
				body.append("<td>").append(ReportDialog.escape(issue.message())).append("</td>");
				body.append("</tr>");
			}
			body.append("</table>");

			return ReportDialog.document(body.toString());
		}
	}


	private ConsistencyService(){
	}


	public static Report check(final FLEFModel model){
		final List<Issue> issues = new ArrayList<>();

		final List<FLEFRecord> individuals = model.getRecordsByType(IndividualHandler.TYPE);
		for(final FLEFRecord individual : individuals)
			checkIndividualName(individual, issues);

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		final Map<String, List<String>> parentsOf = new HashMap<>();
		for(final FLEFRecord rel : relationships){
			checkSelfReference(rel, issues);
			collectParentEdge(rel, parentsOf);
		}
		checkCircularParentage(parentsOf, individuals, issues);

		return new Report(individuals.size(), relationships.size(), issues);
	}


	private static void checkIndividualName(final FLEFRecord individual, final List<Issue> issues){
		// A name is expected as the first child named "name". Its presence
		// is what the model API exposes without deep field inspection.
		final boolean hasName = individual.getChildren()
			.stream()
			.anyMatch(c -> "name".equalsIgnoreCase(c.getTag()));
		if(!hasName)
			issues.add(new Issue(Severity.WARNING, individual.getId(),
				"Individual has no name."));
	}

	private static void checkSelfReference(final FLEFRecord rel, final List<Issue> issues){
		final String subjectId = rel.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
		final String targetId = rel.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
		if(subjectId != null && subjectId.equals(targetId))
			issues.add(new Issue(Severity.ERROR, rel.getId(),
				"Relationship points to itself (" + subjectId + ")."));
	}

	private static void collectParentEdge(final FLEFRecord rel, final Map<String, List<String>> parentsOf){
		final String type = FLEFRecordHelper.getChildValue(rel, "type");
		if(!TYPE_BIOLOGICAL_CHILD.equalsIgnoreCase(type))
			return;
		final String childId = rel.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
		final String parentId = rel.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);
		if(childId != null && parentId != null)
			parentsOf.computeIfAbsent(childId, k -> new ArrayList<>()).add(parentId);
	}

	private static void checkCircularParentage(final Map<String, List<String>> parentsOf,
		final List<FLEFRecord> individuals, final List<Issue> issues){
		final Set<String> reported = new HashSet<>();
		for(final FLEFRecord individual : individuals){
			final String start = individual.getId();
			if(start == null || reported.contains(start))
				continue;
			if(hasCycle(start, parentsOf))
				issues.add(new Issue(Severity.ERROR, start,
					"Circular parentage chain detected."));
		}
	}

	private static boolean hasCycle(final String start, final Map<String, List<String>> parentsOf){
		final Set<String> visited = new HashSet<>();
		final java.util.Deque<String> stack = new java.util.ArrayDeque<>();
		stack.push(start);
		while(!stack.isEmpty()){
			final String current = stack.pop();
			for(final String parent : parentsOf.getOrDefault(current, List.of())){
				if(parent.equals(start))
					return true;
				if(visited.add(parent))
					stack.push(parent);
			}
		}
		return false;
	}

}
