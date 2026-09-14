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
package io.github.mtrevisan.familylegacy.v2.ui.tools.validate;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ReportDialog;

import java.util.ArrayList;
import java.util.List;


/**
 * Validates the referential integrity of a {@link FLEFModel}.
 * <p>
 * The validation walks every record that carries a reference and checks
 * that the target exists in the model. References are extracted from the
 * same fields used by the rest of the application, so a broken reference
 * here is a reference that would fail to resolve elsewhere.
 * <p>
 * The service is pure: it takes a model and returns a report, without
 * touching the UI. This makes it testable and keeps the tool class
 * focused on wiring.
 */
public final class FileValidationService{

	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_EVENT = "event";
	private static final String TAG_PARTICIPANT = "participant";


	/** One broken reference found during validation. */
	public record Issue(String recordId, String recordTag, String field, String missingId){}

	/** Result of the validation. */
	public record Report(int recordsScanned, int relationshipIssues, int eventIssues, List<Issue> issues){

		public boolean isClean(){
			return issues.isEmpty();
		}

		/** Renders the report as an HTML document. */
		public String toHtml(){
			final StringBuilder body = new StringBuilder();
			body.append("<h1>File Validation</h1>");
			body.append("<p>Records scanned: <b>").append(recordsScanned).append("</b></p>");

			if(isClean()){
				body.append("<p class='ok'><b>✓ No referential integrity issues found.</b></p>");
				return ReportDialog.document(body.toString());
			}

			body.append("<p class='err'><b>Issues found: ").append(issues.size()).append("</b></p>");

			if(relationshipIssues > 0)
				body.append("<p>Broken relationships: <b>").append(relationshipIssues).append("</b></p>");
			if(eventIssues > 0)
				body.append("<p>Broken event participations: <b>").append(eventIssues).append("</b></p>");

			body.append("<h2>Details</h2>");
			body.append("<table>");
			body.append("<tr><th>Record</th><th>Tag</th><th>Field</th><th>Missing target</th></tr>");
			for(final Issue issue : issues){
				body.append("<tr>");
				body.append("<td>").append(ReportDialog.escape(issue.recordId())).append("</td>");
				body.append("<td>").append(ReportDialog.escape(issue.recordTag())).append("</td>");
				body.append("<td>").append(ReportDialog.escape(issue.field())).append("</td>");
				body.append("<td class='err'>").append(ReportDialog.escape(issue.missingId())).append("</td>");
				body.append("</tr>");
			}
			body.append("</table>");

			return ReportDialog.document(body.toString());
		}
	}


	private FileValidationService(){
	}


	public static Report validate(final FLEFModel model){
		final List<Issue> issues = new ArrayList<>();

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		int relationshipIssues = 0;
		for(final FLEFRecord rel : relationships){
			relationshipIssues += validateRelationship(rel, model, issues);
		}

		final List<FLEFRecord> participations = model.getRecordsByType(EventParticipationHandler.TYPE);
		int eventIssues = 0;
		for(final FLEFRecord ep : participations){
			eventIssues += validateEventParticipation(ep, model, issues);
		}

		final int total = relationships.size() + participations.size();
		return new Report(total, relationshipIssues, eventIssues, issues);
	}


	private static int validateRelationship(final FLEFRecord rel, final FLEFModel model,
		final List<Issue> issues){
		int count = 0;
		final String subjectId = rel.extractReferencedId(TAG_SUBJECT, IndividualHandler.TYPE);
		final String targetId = rel.extractReferencedId(TAG_TARGET, IndividualHandler.TYPE);

		if(subjectId != null && !model.hasRecord(subjectId)){
			issues.add(new Issue(rel.getId(), rel.getTag(), TAG_SUBJECT, subjectId));
			count++;
		}
		if(targetId != null && !model.hasRecord(targetId)){
			issues.add(new Issue(rel.getId(), rel.getTag(), TAG_TARGET, targetId));
			count++;
		}
		return count;
	}

	private static int validateEventParticipation(final FLEFRecord ep, final FLEFModel model,
		final List<Issue> issues){
		int count = 0;
		final String eventId = FLEFRecordHelper.getChildValue(ep, TAG_EVENT);
		if(eventId != null && !model.hasRecord(eventId)){
			issues.add(new Issue(ep.getId(), ep.getTag(), TAG_EVENT, eventId));
			count++;
		}

		// The participant reference is nested, so we only check that the
		// participant block exists. A deeper check would require knowing
		// which record type is referenced (individual, group, place),
		// which the current model API does not expose generically.
		final FLEFRecord participantBlock = FLEFRecordHelper.findChild(ep, TAG_PARTICIPANT);
		if(participantBlock == null){
			issues.add(new Issue(ep.getId(), ep.getTag(), TAG_PARTICIPANT, "<missing block>"));
			count++;
		}
		return count;
	}

}
