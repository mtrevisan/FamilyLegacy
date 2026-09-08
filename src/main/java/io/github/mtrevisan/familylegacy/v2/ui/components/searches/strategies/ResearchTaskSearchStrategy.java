package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchTaskHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/* TODO */
/**
 * Search strategy for ResearchTask records.
 * Supports filtering by status, priority, and target repository.
 */
public class ResearchTaskSearchStrategy implements SearchStrategy{

	private static final String DOT = ".";

	private static final String TAG_STATUS = "status";
	private static final String TAG_PRIORITY = "priority";
	private static final String TAG_REPOSITORY = "repository";

	private static final String TAG_REPOSITORY_REPOSITORY = TAG_REPOSITORY + DOT + TAG_REPOSITORY;

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final ResearchTaskHandler HANDLER = ResearchTaskHandler.getInstance();

	private String status;
	private String priority;
	private String repositoryContains;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		status = criteria.getFilterFor("status");
		priority = criteria.getFilterFor("priority");
		repositoryContains = criteria.getFilterFor("repositoryContains");
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return task -> {
			// Status filter
			if(StringUtils.isNotEmpty(status)){
				final String recordStatus = FLEFRecordHelper.getChildValue(task, TAG_STATUS);
				if(!status.equalsIgnoreCase(recordStatus)){
					return false;
				}
			}

			// Priority filter
			if(StringUtils.isNotEmpty(priority)){
				final String recordPriority = FLEFRecordHelper.getChildValue(task, TAG_PRIORITY);
				if(!priority.equalsIgnoreCase(recordPriority)){
					return false;
				}
			}

			// Repository filter
			if(StringUtils.isNotEmpty(repositoryContains)){
				final String repositoryRef = FLEFRecordHelper.getChildValue(task, TAG_REPOSITORY_REPOSITORY);
				if(repositoryRef != null){
					final FLEFRecord repositoryRecord = model.getRecordById(repositoryRef);
					if(repositoryRecord != null){
						final String repoDisplayText = RepositoryHandler.getInstance().getDisplayText(repositoryRecord, model);
						if(!TextSearchHelper.matchesText(repoDisplayText, repositoryContains, fuzzy, wholeWord, FUZZY_THRESHOLD)){
							return false;
						}
					}
					else{
						return false;
					}
				}
				else{
					return false;
				}
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String statusVal = FLEFRecordHelper.getChildValue(record, TAG_STATUS);
		final String priorityVal = FLEFRecordHelper.getChildValue(record, TAG_PRIORITY);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(statusVal)){
			details.add("Status: " + statusVal);
		}
		if(StringUtils.isNotEmpty(priorityVal)){
			details.add("Priority: " + priorityVal);
		}

		return baseDisplayText + details;
	}

}
