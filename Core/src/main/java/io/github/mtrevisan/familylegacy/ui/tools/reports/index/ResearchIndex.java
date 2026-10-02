package io.github.mtrevisan.familylegacy.ui.tools.reports.index;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;


public final class ResearchIndex{

	private final Map<String, List<FLEFRecord>> questionToActivitiesMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> questionToTasksMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> questionToConclusionsMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> activityToTasksMap = new HashMap<>();
	private final Map<String, List<FLEFRecord>> contextImpactsByTarget = new HashMap<>();
	private final Map<String, List<FLEFRecord>> researchQuestionsByTarget = new HashMap<>();

	public ResearchIndex(final FLEFModel model, final Predicate<FLEFRecord> filter){
		if(model == null)
			return;

		final List<FLEFRecord> researchActivities = model.getRecordsByType(ResearchActivityHandler.TYPE);
		for(final FLEFRecord researchActivity : researchActivities){
			if(!filter.test(researchActivity))
				continue;
			for(final FLEFRecord qRef : FLEFRecordHelper.findChildren(researchActivity, "question")){
				if(qRef.getValue() != null){
					questionToActivitiesMap.computeIfAbsent(qRef.getValue(), k -> new ArrayList<>()).add(researchActivity);
				}
			}
		}

		final List<FLEFRecord> researchTasks = model.getRecordsByType(ResearchTaskHandler.TYPE);
		for(final FLEFRecord researchTask : researchTasks){
			if(!filter.test(researchTask))
				continue;
			for(final FLEFRecord qRef : FLEFRecordHelper.findChildren(researchTask, "question")){
				if(qRef.getValue() != null){
					questionToTasksMap.computeIfAbsent(qRef.getValue(), k -> new ArrayList<>()).add(researchTask);
				}
			}
			final String createdBy = FLEFRecordHelper.getChildValue(researchTask, "created_by");
			if(createdBy != null){
				activityToTasksMap.computeIfAbsent(createdBy, k -> new ArrayList<>()).add(researchTask);
			}
		}

		final List<FLEFRecord> conclusions = model.getRecordsByType(ConclusionHandler.TYPE);
		for(final FLEFRecord conclusion : conclusions){
			if(!filter.test(conclusion))
				continue;
			for(final FLEFRecord qRef : FLEFRecordHelper.findChildren(conclusion, "research")){
				if(qRef.getValue() != null){
					questionToConclusionsMap.computeIfAbsent(qRef.getValue(), k -> new ArrayList<>()).add(conclusion);
				}
			}
		}

		final List<FLEFRecord> contextImpacts = model.getRecordsByType(ContextImpactHandler.TYPE);
		for(final FLEFRecord contextImpact : contextImpacts){
			if(!filter.test(contextImpact))
				continue;
			for(final FLEFRecord t : FLEFRecordHelper.findChildren(contextImpact, "target")){
				final FLEFRecord ref = t.getTheOnlyChild();
				if(ref == null || "void".equalsIgnoreCase(ref.getTag()))
					continue;
				final String id = ref.getValue();
				if(id != null && !id.isBlank())
					contextImpactsByTarget.computeIfAbsent(id, k -> new ArrayList<>()).add(contextImpact);
			}
		}

		final List<FLEFRecord> researchQuestions = model.getRecordsByType(ResearchQuestionHandler.TYPE);
		for(final FLEFRecord researchQuestion : researchQuestions){
			if(!filter.test(researchQuestion))
				continue;
			for(final FLEFRecord t : FLEFRecordHelper.findChildren(researchQuestion, "target")){
				final FLEFRecord ref = t.getTheOnlyChild();
				if(ref == null || "void".equalsIgnoreCase(ref.getTag()))
					continue;
				final String id = ref.getValue();
				if(id != null && !id.isBlank())
					researchQuestionsByTarget.computeIfAbsent(id, k -> new ArrayList<>()).add(researchQuestion);
			}
		}
	}

	public List<FLEFRecord> activitiesForQuestion(final FLEFRecord q){
		return (q != null && q.getId() != null)? questionToActivitiesMap.getOrDefault(q.getId(), Collections.emptyList()): Collections.emptyList();
	}

	public List<FLEFRecord> tasksForQuestion(final FLEFRecord q){
		return (q != null && q.getId() != null)? questionToTasksMap.getOrDefault(q.getId(), Collections.emptyList()): Collections.emptyList();
	}

	public List<FLEFRecord> conclusionsForQuestion(final FLEFRecord q){
		return (q != null && q.getId() != null)? questionToConclusionsMap.getOrDefault(q.getId(), Collections.emptyList()): Collections.emptyList();
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
