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
package io.github.mtrevisan.familylegacy.research;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Assessment;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Gap;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Kind;
import io.github.mtrevisan.familylegacy.research.GpsWorkflow.Severity;
import io.github.mtrevisan.familylegacy.research.InferenceInspection.Assertion;
import io.github.mtrevisan.familylegacy.research.InferenceInspection.Citation;
import io.github.mtrevisan.familylegacy.research.InferenceInspection.Inspection;
import io.github.mtrevisan.familylegacy.sources.EvidenceTrace;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * The proof statement: the argument that leads from the evidence to the conclusion, written from what the file holds (see
 * {@link GpsWorkflow}). It states the question, the research that was done, the evidence with its sources, the conflicts and how they were
 * resolved, and the conclusion with its reasoning. Whatever is not yet shown by the file is said at the end, as the limits of the
 * statement: it is a draft for the researcher to complete, never a verdict, and nothing is invented.
 * <p>
 * The text is Markdown, so that it can be kept as a note of the research question.
 */
public final class GpsProofStatement{

	/** How many assertions are listed one by one in the evidence. */
	private static final int MAX_ASSERTIONS = 40;


	private GpsProofStatement(){}


	/**
	 * @param model      the model the assessment was made on
	 * @param assessment what {@link GpsWorkflow#assess} found
	 * @return the proof statement, in Markdown
	 */
	public static String render(final FLEFModel model, final Assessment assessment){
		final StringBuilder text = new StringBuilder();
		text.append("# ").append(I18N.tf("text.gps.statement.title", title(model, assessment))).append('\n');

		heading(text, "text.gps.statement.question");
		if(assessment.questions().isEmpty())
			line(text, I18N.t("text.gps.statement.question.none"));
		for(final FLEFRecord question : assessment.questions()){
			final String asked = ResearchHelper.questionText(question);
			line(text, (StringUtils.isNotBlank(asked)? asked: ResearchHelper.questionTitle(question)));
		}
		final List<String> subjects = new ArrayList<>();
		for(final String id : assessment.targets()){
			final FLEFRecord record = model.getRecordById(id);
			if(record != null)
				subjects.add(RecordDisplay.describe(record, model));
		}
		if(!subjects.isEmpty())
			line(text, '\n' + I18N.tf("text.gps.statement.subjects", String.join("; ", subjects)));

		heading(text, "text.gps.statement.research");
		research(text, assessment);

		heading(text, "text.gps.statement.evidence");
		evidence(text, assessment);

		heading(text, "text.gps.statement.conflicts");
		conflicts(text, assessment);

		heading(text, "text.gps.statement.conclusion");
		conclusion(text, assessment);

		final List<Gap> limits = assessment.gaps();
		if(!limits.isEmpty()){
			heading(text, "text.gps.statement.limits");
			line(text, I18N.t("text.gps.statement.limits.intro"));
			for(final Gap gap : limits)
				if(gap.severity() == Severity.REQUIRED)
					line(text, "- " + GpsText.gap(gap));
			for(final Gap gap : limits)
				if(gap.severity() == Severity.RECOMMENDED)
					line(text, "- " + I18N.tf("text.gps.statement.recommended", GpsText.gap(gap)));
		}
		return text.toString();
	}

	private static String title(final FLEFModel model, final Assessment assessment){
		return (assessment.questions().isEmpty()
			? RecordDisplay.describe(assessment.subject(), model)
			: String.join("; ", assessment.questions().stream()
				.map(ResearchHelper::questionTitle)
				.toList()));
	}

	private static void research(final StringBuilder text, final Assessment assessment){
		final List<FLEFRecord> finished = assessment.activities().stream()
			.filter(activity -> ResearchActivityReader.ENUM_STATUS_COMPLETED.equalsIgnoreCase(ResearchActivityReader.extractStatus(activity)))
			.toList();
		if(finished.isEmpty())
			line(text, I18N.t("text.gps.statement.research.none"));
		for(final FLEFRecord activity : finished){
			final String type = ResearchActivityReader.extractActivityType(activity);
			final String result = ResearchActivityReader.extractResult(activity);
			final String action = ResearchActivityReader.extractAction(activity);
			line(text, "- " + (type != null? I18N.t("enum.research_activity.type." + type.toLowerCase(Locale.ROOT), type) + ": ": StringUtils.EMPTY)
				+ StringUtils.defaultString(action, activity.getId())
				+ (result != null? " (" + I18N.t("enum.research_activity.result." + result.toLowerCase(Locale.ROOT), result) + ")": StringUtils.EMPTY));
		}
		final GpsWorkflow.EvidenceSummary evidence = assessment.evidence();
		if(evidence.citations() > 0)
			line(text, '\n' + I18N.tf("text.gps.statement.research.sources", evidence.sources().size(), evidence.originals(),
				evidence.citations()));
	}

	private static void evidence(final StringBuilder text, final Assessment assessment){
		if(assessment.traces().isEmpty()){
			line(text, I18N.t("text.gps.statement.evidence.none"));
			return;
		}

		int listed = 0;
		for(final EvidenceTrace trace : assessment.traces()){
			if(listed ++ >= MAX_ASSERTIONS)
				break;

			final EvidenceTrace.Assertion subject = trace.subject();
			if(subject.links().isEmpty()){
				line(text, "- " + I18N.tf("text.gps.statement.evidence.uncited", subject.text()));
				continue;
			}

			final List<String> sources = new ArrayList<>();
			for(final EvidenceTrace.Link link : subject.links())
				sources.add((link.contradicts()? I18N.t("text.gps.statement.contradicts") + " ": StringUtils.EMPTY) + link.sourceTitle()
					+ (StringUtils.isNotBlank(link.locator())? ", " + link.locator(): StringUtils.EMPTY)
					+ (StringUtils.isNotBlank(link.evidence())? " (" + link.evidence() + ")": StringUtils.EMPTY));
			line(text, "- " + I18N.tf("text.gps.statement.evidence.cited", subject.text(), String.join("; ", sources)));
		}
		if(assessment.traces().size() > MAX_ASSERTIONS)
			line(text, I18N.tf("text.gps.statement.evidence.more", assessment.traces().size() - MAX_ASSERTIONS));
	}

	private static void conflicts(final StringBuilder text, final Assessment assessment){
		boolean any = false;
		for(final Inspection inspection : assessment.inspections()){
			if(inspection.assertions().size() < 2)
				continue;

			any = true;
			line(text, "- " + I18N.tf("text.gps.statement.conflict", StringUtils.defaultString(inspection.issue(), inspection.conclusionId()),
				String.join("; ", inspection.assertions().stream()
					.map(Assertion::label)
					.toList())));
			final Assertion preferred = inspection.preferred();
			if(preferred != null)
				line(text, "  " + I18N.tf("text.gps.statement.conflict.preferred", preferred.label(), preferred.weight(),
					inspection.alternatives().stream()
						.mapToInt(Assertion::weight)
						.max()
						.orElse(0)));
			for(final Citation citation : (preferred != null? preferred.opposing(): List.<Citation>of()))
				line(text, "  " + I18N.tf("text.gps.statement.conflict.against", citation.source()));
			if(StringUtils.isNotBlank(inspection.narrative()))
				line(text, "  " + StringUtils.normalizeSpace(inspection.narrative()));
		}
		for(final Gap gap : assessment.gaps())
			if(gap.kind() == Kind.UNRESOLVED_CONFLICT && gap.subjectText() != null){
				any = true;
				line(text, "- " + I18N.tf("text.gps.statement.conflict.open", gap.subjectText()));
			}
		if(!any)
			line(text, I18N.t("text.gps.statement.conflicts.none"));
	}

	private static void conclusion(final StringBuilder text, final Assessment assessment){
		for(final FLEFRecord question : assessment.questions()){
			final String summary = ResearchHelper.firstTextValue(question, ResearchQuestionReader.TAG_CONCLUSION);
			if(StringUtils.isNotBlank(summary)){
				final String confidence = ResearchQuestionReader.extractConclusionConfidence(question);
				line(text, summary + (StringUtils.isNotBlank(confidence)
					? " (" + I18N.tf("text.gps.statement.confidence", I18N.t("enum.confidence." + confidence.toLowerCase(Locale.ROOT), confidence)) + ")"
					: StringUtils.EMPTY));
			}
			final String rationale = ResearchHelper.firstTextValue(question, ResearchQuestionReader.TAG_RATIONALE);
			if(StringUtils.isNotBlank(rationale))
				line(text, '\n' + rationale);
		}
		if(assessment.conclusions().isEmpty())
			line(text, I18N.t("text.gps.statement.conclusion.none"));
		for(final Inspection inspection : assessment.inspections()){
			final Assertion preferred = inspection.preferred();
			final String status = inspection.proofStatus();
			line(text, "\n- " + I18N.tf("text.gps.statement.conclusion.item", StringUtils.defaultString(inspection.issue(), inspection.conclusionId()),
				(StringUtils.isBlank(status)
					? I18N.t("text.proof_summary.not_set")
					: I18N.t("enum.proof_status." + status.toLowerCase(Locale.ROOT), status)),
				(preferred != null? preferred.label(): "-")));
			if(StringUtils.isNotBlank(inspection.narrative()))
				line(text, "  " + StringUtils.normalizeSpace(inspection.narrative()));
		}
	}

	private static void heading(final StringBuilder text, final String key){
		text.append('\n').append("## ").append(I18N.t(key)).append("\n\n");
	}

	private static void line(final StringBuilder text, final String line){
		text.append(line).append('\n');
	}

}
