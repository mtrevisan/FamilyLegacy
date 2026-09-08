package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;


/**
 * Search strategy for Group records.
 * Supports filtering by group name and type.
 */
public class GroupSearchStrategy implements SearchStrategy{

	private static final String TAG_NAME = "name";
	private static final String TAG_VALUE = "value";
	private static final String TAG_TYPE = "type";

	private static final double FUZZY_THRESHOLD = 0.05;


	private static final GroupHandler HANDLER = GroupHandler.getInstance();


	private String name;
	private String type;
	private boolean fuzzy;
	private boolean wholeWord;


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		name = criteria.getFilterFor(GroupFilterPanel.FILTER_KEY_NAME);
		type = criteria.getFilterFor(GroupFilterPanel.FILTER_KEY_TYPE);
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		return group -> {
			// Name filter
			if(StringUtils.isNotEmpty(name)){
				final List<FLEFRecord> names = FLEFRecordHelper.findChildren(group, TAG_NAME);
				boolean matched = false;
				for(final FLEFRecord nameStruct : names){
					final String nameValue = FLEFRecordHelper.getChildValue(nameStruct, TAG_VALUE);
					if(TextSearchHelper.matchesText(nameValue, name, fuzzy, wholeWord, FUZZY_THRESHOLD)){
						matched = true;

						break;
					}
				}
				if(!matched)
					return false;
			}

			// Type filter
			if(StringUtils.isNotEmpty(type)){
				final String groupType = FLEFRecordHelper.getChildValue(group, TAG_TYPE);
				if(!type.equalsIgnoreCase(groupType))
					return false;
			}

			return true;
		};
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);

		final String typeVal = FLEFRecordHelper.getChildValue(record, TAG_TYPE);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(typeVal))
			details.add("Type: " + typeVal);

		return baseDisplayText + details;
	}

}
