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

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchTaskReader;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * Builds the historic-context and research sections.
 *
 * <p>Context impacts and research questions are matched by the complete set
 * of records semantically tied to the root (see
 * {@link ReportContext#relatedRecordIds()}), so every branch of the
 * {@code ImpactTarget} and {@code ResearchTarget} oneofs is reachable:
 * individual, group, place, event, relationship, individual_attribute,
 * group_attribute, conclusion, event_participation, place_relationship,
 * identity_hypothesis, cultural_norm, historic_event, source, document.</p>
 */
final class ContextResearchSection implements SectionBuilder{

	private final ReportContext ctx;


	ContextResearchSection(final ReportContext ctx){
		this.ctx = ctx;
	}


	@Override
	public List<ReportSection> build(){
		if(!ctx.config.contextAndResearch())
			return List.of();
		final List<ReportSection> out = new ArrayList<>();
		appendContextImpacts(out);
		appendResearchQuestions(out);
		appendOrphanTasks(out);
		appendConclusions(out);
		appendIdentityHypotheses(out);
		return out;
	}


	/* ======================================================================
	 *                          Context impacts
	 * ====================================================================== */

	private void appendContextImpacts(final List<ReportSection> out){
		final List<FLEFRecord> impacts = ctx.index.contextImpactsFor(ctx.relatedRecordIds());
		if(impacts.isEmpty())
			return;

		out.add(new ReportSection.Heading(1, ctx.labels.sections().contextSection()));
		for(final FLEFRecord imp : impacts){
			final String impactType = ReportFormatters.orEmpty(
				FLEFRecordHelper.getChildValue(imp, ContextImpactReader.TAG_IMPACT_TYPE));
			out.add(new ReportSection.Heading(2,
				ReportFormatters.escape(impactType) + ": "
					+ ReportFormatters.escape(contextLabel(imp))));

			final String rationale = FLEFRecordHelper.getChildValue(imp, ContextImpactReader.TAG_RATIONALE);
			if(rationale != null)
				out.add(new ReportSection.Paragraph(
					"**" + ctx.labels.sections().contextRationale() + ":** "
						+ ReportFormatters.escape(rationale)));

			if(ctx.config.sources())
				out.addAll(ctx.citations().citations(imp));
			if(ctx.config.evidence())
				ctx.citations().addEvidence(imp, out);
			out.addAll(ctx.citations().audit(imp));
		}
	}

	private String contextLabel(final FLEFRecord contextImpact){
		final FLEFRecord contextField = FLEFRecordHelper.findChild(contextImpact, ContextImpactReader.TAG_CONTEXT);
		if(contextField == null)
			return StringUtils.EMPTY;
		final FLEFRecord ref = contextField.getTheOnlyChild();
		if(ref == null)
			return StringUtils.EMPTY;
		final String refId = ref.getValue();
		if(refId == null)
			return StringUtils.EMPTY;
		final String label = ctx.resolveContextLabel(refId);
		return (label != null? label: StringUtils.EMPTY);
	}


	/* ======================================================================
	 *                          Research questions
	 * ====================================================================== */

	private void appendResearchQuestions(final List<ReportSection> out){
		final List<FLEFRecord> questions = ctx.index.researchQuestionsFor(ctx.relatedRecordIds());
		if(questions.isEmpty())
			return;
		out.add(new ReportSection.Heading(1, ctx.labels.sections().researchSection()));
		for(final FLEFRecord q : questions)
			appendQuestion(out, q);
	}

	private void appendQuestion(final List<ReportSection> out, final FLEFRecord q){
		out.add(new ReportSection.Heading(2, ReportFormatters.escape(
			ReportFormatters.orEmpty(FLEFRecordHelper.getChildValue(q, ResearchQuestionReader.TAG_TITLE)))));

		final String question = FLEFRecordHelper.getChildValue(q, ResearchQuestionReader.TAG_QUESTION);
		if(question != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().question() + ":** " + ReportFormatters.escape(question)));

		final String status = FLEFRecordHelper.getChildValue(q, ResearchQuestionReader.TAG_STATUS);
		if(status != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().status() + ":** "
					+ ReportFormatters.escape(ReportFormatters.orEmpty(
					ReportFormatters.enumLabel(status)))));

		final String conclusion = FLEFRecordHelper.getChildValue(q, ResearchQuestionReader.TAG_CONCLUSION);
		if(conclusion != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().conclusion() + ":** " + ReportFormatters.escape(conclusion)));

		final String confidence = FLEFRecordHelper.getChildValue(q, ResearchQuestionReader.TAG_CONCLUSION_CONFIDENCE);
		if(confidence != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().researchConclusionConfidence() + ":** "
					+ ReportFormatters.escape(ReportFormatters.orEmpty(
					ReportFormatters.enumLabel(confidence)))));

		final String rationale = FLEFRecordHelper.getChildValue(q, ResearchQuestionReader.TAG_RATIONALE);
		if(rationale != null)
			out.add(new ReportSection.Paragraph(ReportFormatters.escape(rationale)));

		final String closed = FLEFRecordHelper.getChildValue(q, ResearchQuestionReader.TAG_CLOSED_DATE);
		if(closed != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().researchClosedDate() + ":** "
					+ ReportFormatters.escape(closed)));

		final List<FLEFRecord> activities = activitiesForQuestion(q.getId());
		if(!activities.isEmpty()){
			out.add(new ReportSection.Heading(3, ctx.labels.sections().researchActivities()));
			for(final FLEFRecord a : activities)
				appendActivity(out, a, 4);
		}

		final List<FLEFRecord> tasks = tasksForQuestion(q.getId());
		if(!tasks.isEmpty()){
			out.add(new ReportSection.Heading(3, ctx.labels.sections().researchTasks()));
			for(final FLEFRecord t : tasks)
				appendTask(out, t, 4);
		}

		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(q));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(q, out);
		out.addAll(ctx.citations().audit(q));
	}


	/* ======================================================================
	 *                          Activities
	 * ====================================================================== */

	private List<FLEFRecord> activitiesForQuestion(final String questionId){
		final List<FLEFRecord> out = new ArrayList<>();
		for(final FLEFRecord a : ctx.visibleRecordsByType(ResearchActivityHandler.TYPE)){
			for(final FLEFRecord q : FLEFRecordHelper.findChildren(a, ResearchActivityReader.TAG_QUESTION))
				if(Objects.equals(questionId, q.getValue())){
					out.add(a);
					break;
				}
		}
		return out;
	}

	private void appendActivity(final List<ReportSection> out, final FLEFRecord a, final int headingLevel){
		final String type = ReportFormatters.enumLabel(
			FLEFRecordHelper.getChildValue(a, ResearchActivityReader.TAG_ACTIVITY_TYPE));
		final String action = FLEFRecordHelper.getChildValue(a, ResearchActivityReader.TAG_ACTION);
		final StringBuilder heading = new StringBuilder();
		if(type != null)
			heading.append(type);
		if(action != null && !action.isBlank()){
			if(!heading.isEmpty())
				heading.append(" — ");
			heading.append(action);
		}
		if(heading.isEmpty())
			heading.append(ReportFormatters.orEmpty(a.getId()));
		out.add(new ReportSection.Heading(headingLevel, ReportFormatters.escape(heading.toString())));

		final List<String> meta = new ArrayList<>();
		ReportFormatters.appendIfPresent(meta, ctx.labels.sections().status(),
			ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(a, ResearchActivityReader.TAG_STATUS)));
		ReportFormatters.appendIfPresent(meta, ctx.labels.sections().researchResult(),
			ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(a, ResearchActivityReader.TAG_RESULT)));
		ReportFormatters.appendIfPresent(meta, ctx.labels.sections().researchConclusionConfidence(),
			ReportFormatters.enumLabel(
				FLEFRecordHelper.getChildValue(a, ResearchActivityReader.TAG_CONCLUSION_CONFIDENCE)));
		if(!meta.isEmpty())
			out.add(new ReportSection.BulletList(meta));

		final String scopeType = FLEFRecordHelper.getChildValue(a, ResearchActivityReader.TAG_SEARCH_SCOPE_TYPE);
		final String scopeDetail = FLEFRecordHelper.getChildValue(a, ResearchActivityReader.TAG_SEARCH_SCOPE_DETAIL);
		if(scopeType != null || scopeDetail != null){
			final StringBuilder sb = new StringBuilder();
			sb.append("**").append(ctx.labels.sections().researchSearchScope()).append(":** ");
			if(scopeType != null)
				sb.append(ReportFormatters.escape(
					ReportFormatters.orEmpty(ReportFormatters.enumLabel(scopeType))));
			if(scopeDetail != null){
				if(scopeType != null)
					sb.append(" — ");
				sb.append(ReportFormatters.escape(scopeDetail));
			}
			out.add(new ReportSection.Paragraph(sb.toString()));
		}

		final String targetLabel = describeOneOf(FLEFRecordHelper.findChild(a, ResearchActivityReader.TAG_TARGET));
		if(targetLabel != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().researchTarget() + ":** "
					+ ReportFormatters.escape(targetLabel)));

		final String obs = FLEFRecordHelper.getChildValue(a, ResearchActivityReader.TAG_OBSERVATION);
		if(obs != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().researchObservation() + ":** "
					+ ReportFormatters.escape(obs)));

		final String conclusion = FLEFRecordHelper.getChildValue(a, ResearchActivityReader.TAG_CONCLUSION);
		if(conclusion != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().conclusion() + ":** " + ReportFormatters.escape(conclusion)));

		final String parentLabel = describeOneOf(
			FLEFRecordHelper.findChild(a, ResearchActivityReader.TAG_PARENT_ACTIVITY));
		if(parentLabel != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().researchParentActivity() + ":** "
					+ ReportFormatters.escape(parentLabel)));

		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(a));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(a, out);
		out.addAll(ctx.citations().audit(a));
	}


	/* ======================================================================
	 *                          Tasks
	 * ====================================================================== */

	private List<FLEFRecord> tasksForQuestion(final String questionId){
		final List<FLEFRecord> out = new ArrayList<>();
		for(final FLEFRecord t : ctx.visibleRecordsByType(ResearchTaskHandler.TYPE)){
			for(final FLEFRecord q : FLEFRecordHelper.findChildren(t, ResearchTaskReader.TAG_QUESTION))
				if(Objects.equals(questionId, q.getValue())){
					out.add(t);
					break;
				}
		}
		return out;
	}

	private void appendOrphanTasks(final List<ReportSection> out){
		final Set<String> shown = new HashSet<>();
		for(final FLEFRecord q : ctx.index.researchQuestionsFor(ctx.relatedRecordIds()))
			for(final FLEFRecord t : tasksForQuestion(q.getId()))
				shown.add(t.getId());

		final List<FLEFRecord> orphans = new ArrayList<>();
		for(final FLEFRecord t : ctx.visibleRecordsByType(ResearchTaskHandler.TYPE))
			if(!shown.contains(t.getId()) && relatesToRoot(t))
				orphans.add(t);

		if(orphans.isEmpty())
			return;

		out.add(new ReportSection.Heading(1, ctx.labels.sections().researchTasks()));
		for(final FLEFRecord t : orphans)
			appendTask(out, t, 2);
	}

	private boolean relatesToRoot(final FLEFRecord task){
		for(final FLEFRecord q : FLEFRecordHelper.findChildren(task, ResearchTaskReader.TAG_QUESTION)){
			final FLEFRecord qRec = ctx.model.getRecordById(q.getValue());
			if(qRec == null)
				continue;
			for(final FLEFRecord t : ctx.index.researchQuestionsFor(ctx.relatedRecordIds()))
				if(Objects.equals(t.getId(), qRec.getId()))
					return true;
		}
		return false;
	}

	private void appendTask(final List<ReportSection> out, final FLEFRecord t,
		final int headingLevel){
		final String description = FLEFRecordHelper.getChildValue(t, ResearchTaskReader.TAG_DESCRIPTION);
		out.add(new ReportSection.Heading(headingLevel, ReportFormatters.escape(
			ReportFormatters.orEmpty(description))));

		final List<String> meta = new ArrayList<>();
		ReportFormatters.appendIfPresent(meta, ctx.labels.sections().status(),
			ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(t, ResearchTaskReader.TAG_STATUS)));
		ReportFormatters.appendIfPresent(meta, ctx.labels.sections().researchTaskPriority(),
			ReportFormatters.enumLabel(FLEFRecordHelper.getChildValue(t, ResearchTaskReader.TAG_PRIORITY)));
		ReportFormatters.appendIfPresent(meta, ctx.labels.sections().researchTaskDueDate(),
			FLEFRecordHelper.getChildValue(t, ResearchTaskReader.TAG_DUE_DATE));
		if(!meta.isEmpty())
			out.add(new ReportSection.BulletList(meta));

		final String createdBy = describeOneOf(FLEFRecordHelper.findChild(t, ResearchTaskReader.TAG_CREATED_BY));
		if(createdBy != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().researchTaskCreatedBy() + ":** "
					+ ReportFormatters.escape(createdBy)));

		final String outcome = FLEFRecordHelper.getChildValue(t, ResearchTaskReader.TAG_OUTCOME);
		if(outcome != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().researchTaskOutcome() + ":** "
					+ ReportFormatters.escape(outcome)));

		out.addAll(ctx.citations().audit(t));
	}


	/* ======================================================================
	 *                          Conclusions
	 * ====================================================================== */

	private void appendConclusions(final List<ReportSection> out){
		final List<FLEFRecord> conclusions = conclusionsFor();
		if(conclusions.isEmpty())
			return;
		out.add(new ReportSection.Heading(1, ctx.labels.sections().conclusions()));
		for(final FLEFRecord c : conclusions)
			appendConclusion(out, c);
	}

	private List<FLEFRecord> conclusionsFor(){
		final Set<String> related = ctx.relatedRecordIds();
		final List<FLEFRecord> out = new ArrayList<>();
		for(final FLEFRecord c : ctx.visibleRecordsByType(ConclusionHandler.TYPE)){
			for(final FLEFRecord t : FLEFRecordHelper.findChildren(c, ConclusionReader.TAG_RESOLVES)){
				final FLEFRecord ref = t.getTheOnlyChild();
				if(ref != null && related.contains(ref.getValue())){
					out.add(c);
					break;
				}
			}
		}
		return out;
	}

	private void appendConclusion(final List<ReportSection> out, final FLEFRecord c){
		out.add(new ReportSection.Heading(2, ReportFormatters.escape(
			ReportFormatters.orEmpty(FLEFRecordHelper.getChildValue(c, ConclusionReader.TAG_ISSUE)))));

		final String proof = FLEFRecordHelper.getChildValue(c, ConclusionReader.TAG_PROOF_STATUS);
		if(proof != null)
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().researchProofStatus() + ":** "
					+ ReportFormatters.escape(ReportFormatters.orEmpty(
					ReportFormatters.enumLabel(proof)))));

		final String narrative = FLEFRecordHelper.getChildValue(c, ConclusionReader.TAG_NARRATIVE);
		if(narrative != null)
			out.add(new ReportSection.Paragraph(ReportFormatters.escape(narrative)));

		final List<FLEFRecord> resolves = FLEFRecordHelper.findChildren(c, ConclusionReader.TAG_RESOLVES);
		final List<String> resolvedLabels = new ArrayList<>();
		String preferredLabel = describeOneOf(FLEFRecordHelper.findChild(c, ConclusionReader.TAG_PREFERRED));

		for(final FLEFRecord r : resolves){
			final String label = describeOneOf(r);
			if(label != null)
				resolvedLabels.add(label);
		}

		if(!resolvedLabels.isEmpty()){
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().researchResolves() + ":**"));
			final List<String> items = new ArrayList<>(resolvedLabels.size());
			for(final String s : resolvedLabels){
				final boolean preferred = (preferredLabel != null && preferredLabel.equals(s));
				items.add(s + (preferred? "  ← *" + ctx.labels.sections().researchPreferred() + "*": StringUtils.EMPTY));
			}
			out.add(new ReportSection.BulletList(items));
		}

		final List<FLEFRecord> links = FLEFRecordHelper.findChildren(c, ConclusionReader.TAG_RESEARCH);
		final List<String> qLabels = new ArrayList<>();
		for(final FLEFRecord r : links){
			final String qid = r.getValue();
			if(qid == null)
				continue;
			final FLEFRecord q = ctx.visible(ctx.model.getRecordById(qid));
			if(q == null)
				continue;
			final String title = FLEFRecordHelper.getChildValue(q, ResearchQuestionReader.TAG_TITLE);
			qLabels.add(title != null && !title.isBlank()
				? title.trim()
				: ReportFormatters.orEmpty(qid));
		}
		if(!qLabels.isEmpty()){
			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().researchLinkedQuestions() + ":**"));
			out.add(new ReportSection.BulletList(qLabels.stream()
				.map(ReportFormatters::escape)
				.toList()));
		}

		if(ctx.config.sources())
			out.addAll(ctx.citations().citations(c));
		if(ctx.config.evidence())
			ctx.citations().addEvidence(c, out);
		out.addAll(ctx.citations().audit(c));
	}


	/* ======================================================================
	 *                          Identity hypotheses
	 * ====================================================================== */

	private void appendIdentityHypotheses(final List<ReportSection> out){
		final List<FLEFRecord> hyps = identityHypothesesFor();
		if(hyps.isEmpty())
			return;
		out.add(new ReportSection.Heading(1, ctx.labels.sections().identityHypotheses()));
		for(final FLEFRecord h : hyps){
			final List<String> candidates = new ArrayList<>();
			for(final FLEFRecord cand : FLEFRecordHelper.findChildren(h, IdentityHypothesisReader.TAG_IDENTITY)){
				final FLEFRecord ref = cand.getTheOnlyChild();
				if(ref == null || FLEFRecord.TAG_VOID.equalsIgnoreCase(ref.getTag()))
					continue;
				final String id = ref.getValue();
				final FLEFRecord rec = (id != null? ctx.model.getRecordById(id): null);
				if(rec != null && !ctx.isVisible(rec))
					continue;
				candidates.add(rec != null? ctx.displayText(rec): ReportFormatters.orEmpty(id));
			}
			if(candidates.isEmpty())
				continue;

			out.add(new ReportSection.Paragraph(
				"**" + ctx.labels.sections().identityCandidates() + ":** "
					+ ReportFormatters.escape(String.join(" ↔ ", candidates))));

			final String comment = FLEFRecordHelper.getChildValue(h, IdentityHypothesisReader.TAG_COMMENT);
			if(comment != null)
				out.add(new ReportSection.Paragraph(
					"**" + ctx.labels.sections().identityComment() + ":** "
						+ ReportFormatters.escape(comment)));

			if(ctx.config.notes())
				out.addAll(ctx.citations().notes(h));
			if(ctx.config.sources())
				out.addAll(ctx.citations().citations(h));
			if(ctx.config.evidence())
				ctx.citations().addEvidence(h, out);
			out.addAll(ctx.citations().audit(h));
		}
	}

	private List<FLEFRecord> identityHypothesesFor(){
		final Set<String> related = ctx.relatedRecordIds();
		final List<FLEFRecord> out = new ArrayList<>();
		for(final FLEFRecord h : ctx.visibleRecordsByType(IdentityHypothesisHandler.TYPE)){
			if(related.contains(h.getId())){
				out.add(h);
				continue;
			}
			for(final FLEFRecord cand : FLEFRecordHelper.findChildren(h, IdentityHypothesisReader.TAG_IDENTITY)){
				final FLEFRecord ref = cand.getTheOnlyChild();
				if(ref != null && related.contains(ref.getValue())){
					out.add(h);
					break;
				}
			}
		}
		return out;
	}


	/* ======================================================================
	 *                          oneof resolver
	 * ====================================================================== */

	private String describeOneOf(final FLEFRecord field){
		if(field == null)
			return null;
		final FLEFRecord ref = field.getTheOnlyChild();
		if(ref == null || FLEFRecord.TAG_VOID.equalsIgnoreCase(ref.getTag()))
			return null;
		final String tag = ref.getTag();
		final String id = ref.getValue();
		if(id == null || id.isBlank())
			return null;

		final FLEFRecord rec = ctx.model.getRecordById(id);
		if(!ctx.isVisible(rec))
			return tag + StringUtils.SPACE + id;
		return ctx.displayText(rec);
	}

}
