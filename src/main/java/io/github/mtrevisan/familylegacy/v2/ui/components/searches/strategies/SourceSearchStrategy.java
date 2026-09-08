package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.StringJoiner;
import java.util.function.Predicate;


/* TODO */
/**
 * Search strategy for Source records.
 * Supports filtering by medium, repository, and author.
 */
public class SourceSearchStrategy implements SearchStrategy{

	private static final String DOT = ".";

	private static final String TAG_MEDIUM = "medium";
	private static final String TAG_AUTHOR = "author";
	private static final String TAG_REPOSITORY = "repository";
	private static final String TAG_VALUE = "value";

	private static final String TAG_REPOSITORY_REPOSITORY = TAG_REPOSITORY + DOT + TAG_REPOSITORY;

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final SourceHandler HANDLER = SourceHandler.getInstance();

	private String medium;
	private String repository;
	private String author;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		medium = criteria.getFilterFor("medium");
		repository = criteria.getFilterFor("repository");
		author = criteria.getFilterFor("author");
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return source -> {
			// Medium filter
			if(StringUtils.isNotEmpty(medium)){
				final String recordMedium = FLEFRecordHelper.getChildValue(source, TAG_MEDIUM);
				if(!medium.equalsIgnoreCase(recordMedium)){
					return false;
				}
			}

			// Author filter
			if(StringUtils.isNotEmpty(author)){
				final String recordAuthor = FLEFRecordHelper.getChildValue(source, TAG_AUTHOR + DOT + TAG_VALUE);
				if(!TextSearchHelper.matchesText(recordAuthor, author, fuzzy, wholeWord, FUZZY_THRESHOLD)){
					return false;
				}
			}

			// Repository filter
			if(StringUtils.isNotEmpty(repository)){
				final String repositoryRef = FLEFRecordHelper.getChildValue(source, TAG_REPOSITORY_REPOSITORY);
				if(repositoryRef != null){
					final FLEFRecord repositoryRecord = model.getRecordById(repositoryRef);
					if(repositoryRecord != null){
						final String repoDisplayText = RepositoryHandler.getInstance().getDisplayText(repositoryRecord, model);
						if(!TextSearchHelper.matchesText(repoDisplayText, repository, fuzzy, wholeWord, FUZZY_THRESHOLD)){
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

		final String authorVal = FLEFRecordHelper.getChildValue(record, TAG_AUTHOR + DOT + TAG_VALUE);
		final String mediumVal = FLEFRecordHelper.getChildValue(record, TAG_MEDIUM);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(authorVal)){
			details.add("by " + authorVal);
		}
		if(StringUtils.isNotEmpty(mediumVal)){
			details.add(mediumVal);
		}

		return baseDisplayText + details;
	}

}
