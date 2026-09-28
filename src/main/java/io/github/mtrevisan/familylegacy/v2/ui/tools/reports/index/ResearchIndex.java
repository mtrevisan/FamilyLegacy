package io.github.mtrevisan.familylegacy.v2.ui.tools.reports.index;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchTaskHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class ResearchIndex {

	private final Map<String, List<FLEFRecord>> questionToActivitiesMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> questionToTasksMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> questionToConclusionsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> activityToTasksMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> contextImpactsByTarget = new HashMap<>();
	private final Map<String, List<FLEFRecord>> researchQuestionsByTarget = new HashMap<>();

	public ResearchIndex(final FLEFModel model, final Predicate<FLEFRecord> filter){
		if(model == null)
			return;

		for(final FLEFRecord act : model.getRecordsByType(ResearchActivityHandler.TYPE)){
			if(!filter.test(act))
				continue;
			for(final FLEFRecord qRef : FLEFRecordHelper.findChildren(act, "question")){
				if(qRef.getValue() != null){
					questionToActivitiesMap.computeIfAbsent(qRef.getValue(), k -> new ArrayList<>()).add(act);
				}
			}
		}

		for(final FLEFRecord task : model.getRecordsByType(ResearchTaskHandler.TYPE)){
			if(!filter.test(task))
				continue;
			for(final FLEFRecord qRef : FLEFRecordHelper.findChildren(task, "question")){
				if(qRef.getValue() != null){
					questionToTasksMap.computeIfAbsent(qRef.getValue(), k -> new ArrayList<>()).add(task);
				}
			}
			final String createdBy = FLEFRecordHelper.getChildValue(task, "created_by");
			if(createdBy != null){
				activityToTasksMap.computeIfAbsent(createdBy, k -> new ArrayList<>()).add(task);
			}
		}

		for(final FLEFRecord conc : model.getRecordsByType(ConclusionHandler.TYPE)){
			if(!filter.test(conc))
				continue;
			for(final FLEFRecord qRef : FLEFRecordHelper.findChildren(conc, "research")){
				if(qRef.getValue() != null){
					questionToConclusionsMap.computeIfAbsent(qRef.getValue(), k -> new ArrayList<>()).add(conc);
				}
			}
		}

		for(final FLEFRecord ci : model.getRecordsByType(ContextImpactHandler.TYPE)){
			if(!filter.test(ci))
				continue;
			for(final FLEFRecord t : FLEFRecordHelper.findChildren(ci, "target")){
				final FLEFRecord ref = t.getTheOnlyChild();
				if(ref == null || "void".equalsIgnoreCase(ref.getTag()))
					continue;
				final String id = ref.getValue();
				if(id != null && !id.isBlank())
					contextImpactsByTarget.computeIfAbsent(id, k -> new ArrayList<>()).add(ci);
			}
		}

		for(final FLEFRecord q : model.getRecordsByType(ResearchQuestionHandler.TYPE)){
			if(!filter.test(q))
				continue;
			for(final FLEFRecord t : FLEFRecordHelper.findChildren(q, "target")){
				final FLEFRecord ref = t.getTheOnlyChild();
				if(ref == null || "void".equalsIgnoreCase(ref.getTag()))
					continue;
				final String id = ref.getValue();
				if(id != null && !id.isBlank())
					researchQuestionsByTarget.computeIfAbsent(id, k -> new ArrayList<>()).add(q);
			}
		}
	}

	public List<FLEFRecord> activitiesForQuestion(final FLEFRecord q){
		return (q != null && q.getId() != null) ? questionToActivitiesMap.getOrDefault(q.getId(), Collections.emptyList()) : Collections.emptyList();
	}

	public List<FLEFRecord> tasksForQuestion(final FLEFRecord q){
		return (q != null && q.getId() != null) ? questionToTasksMap.getOrDefault(q.getId(), Collections.emptyList()) : Collections.emptyList();
	}

	public List<FLEFRecord> conclusionsForQuestion(final FLEFRecord q){
		return (q != null && q.getId() != null) ? questionToConclusionsMap.getOrDefault(q.getId(), Collections.emptyList()) : Collections.emptyList();
	}

	public List<FLEFRecord> tasksForActivity(final FLEFRecord activity){
		if(activity == null || activity.getId() == null)
			return Collections.emptyList();
		return activityToTasksMap.getOrDefault(activity.getId(), Collections.emptyList());
	}

	public List<FLEFRecord> contextImpactsFor(final Set<String> targetIds){
		final Set<String> seen = new HashSet<>();
		final List<FLEFRecord> out = new ArrayList<>();
		for(final String id : targetIds)
			for(final FLEFRecord ci : contextImpactsByTarget.getOrDefault(id, Collections.emptyList()))
				if(seen.add(ci.getId()))
					out.add(ci);
		return out;
	}

	public List<FLEFRecord> researchQuestionsFor(final Set<String> targetIds){
		final Set<String> seen = new HashSet<>();
		final List<FLEFRecord> out = new ArrayList<>();
		for(final String id : targetIds)
			for(final FLEFRecord q : researchQuestionsByTarget.getOrDefault(id, Collections.emptyList()))
				if(seen.add(q.getId()))
					out.add(q);
		return out;
	}

}
