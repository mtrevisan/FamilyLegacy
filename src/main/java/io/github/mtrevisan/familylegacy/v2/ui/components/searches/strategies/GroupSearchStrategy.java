package io.github.mtrevisan.familylegacy.v2.ui.components.searches.strategies;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchCriteria;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.SearchStrategy;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.TextSearchHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Predicate;


/* TODO */
/**
 * Search strategy for Group records.
 * Supports filtering by group type and member individual.
 */
public class GroupSearchStrategy implements SearchStrategy{

	private static final String DOT = ".";

	private static final String TAG_TYPE = "type";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_GROUP = "group";
	private static final String TAG_INDIVIDUAL = "individual";

	private static final String TAG_SUBJECT_INDIVIDUAL = TAG_SUBJECT + DOT + TAG_INDIVIDUAL;
	private static final String TAG_TARGET_GROUP = TAG_TARGET + DOT + TAG_GROUP;

	private static final double FUZZY_THRESHOLD = 0.05;

	private static final GroupHandler HANDLER = GroupHandler.getInstance();

	private String groupType;
	private String memberName;
	private boolean fuzzy;
	private boolean wholeWord;

	private FLEFModel model;

	// Cache group ID -> list of member individual IDs
	private final Map<String, List<String>> groupMembersMap = new HashMap<>();


	@Override
	public Predicate<FLEFRecord> buildPredicate(final SearchCriteria criteria, final FLEFModel model){
		groupType = criteria.getFilterFor("groupType");
		memberName = criteria.getFilterFor("memberName");
		fuzzy = criteria.isFuzzy();
		wholeWord = criteria.isWholeWord();

		this.model = model;

		if(StringUtils.isNotEmpty(memberName)){
			precomputeGroupMemberships();
		}

		return group -> {
			// Group type filter
			if(StringUtils.isNotEmpty(groupType)){
				final String recordType = FLEFRecordHelper.getChildValue(group, TAG_TYPE);
				if(!groupType.equalsIgnoreCase(recordType)){
					return false;
				}
			}

			// Member name filter
			if(StringUtils.isNotEmpty(memberName)){
				if(!matchesMemberName(group.getId())){
					return false;
				}
			}

			return true;
		};
	}

	private void precomputeGroupMemberships(){
		groupMembersMap.clear();

		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord rel : relationships){
			final String memberId = rel.extractReferencedId(TAG_SUBJECT_INDIVIDUAL, IndividualHandler.TYPE);
			final String groupId = rel.extractReferencedId(TAG_TARGET_GROUP, GroupHandler.TYPE);

			if(memberId != null && groupId != null){
				groupMembersMap.computeIfAbsent(groupId, k -> new ArrayList<>())
					.add(memberId);
			}
		}
	}

	private boolean matchesMemberName(final String groupId){
		final List<String> memberIds = groupMembersMap.get(groupId);
		if(memberIds == null || memberIds.isEmpty()){
			return false;
		}

		for(final String memberId : memberIds){
			final FLEFRecord memberRecord = model.getRecordById(memberId);
			if(memberRecord != null){
				final String memberDisplayText = IndividualHandler.getInstance().getDisplayText(memberRecord, model);
				if(TextSearchHelper.matchesText(memberDisplayText, memberName, fuzzy, wholeWord, FUZZY_THRESHOLD)){
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String getDisplayText(final FLEFRecord record, final FLEFModel model){
		final String baseDisplayText = HANDLER.getDisplayText(record, model);
		final String type = FLEFRecordHelper.getChildValue(record, TAG_TYPE);

		final StringJoiner details = new StringJoiner(", ", " (", ")");
		details.setEmptyValue(StringUtils.EMPTY);

		if(StringUtils.isNotEmpty(type)){
			details.add(type);
		}

		return baseDisplayText + details;
	}

}
