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
package io.github.mtrevisan.familylegacy.privacy;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PrivacyReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.ui.components.PreferredImagePanel;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.MediaHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.JulianFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Computes the <em>effective</em> privacy level of the records of a model, as defined by the FLEF protocol.
 * <p>
 * The effective level of a record is the most restrictive of:
 * <ul>
 *   <li>its own {@code privacy} block, unless the {@code expires} date has passed;</li>
 *   <li>the effective level of every individual or group it references (for an event, of the participants of the
 *       participations that reference it; for a media, of the individuals and groups that use it as preferred image);</li>
 * </ul>
 * and, when no {@code privacy} block applies, it is public, except:
 * <ul>
 *   <li>an individual that may be living is {@link PrivacyLevel#RESTRICTED}: an individual may be living unless it is the
 *       principal of a death event, or the principal of a birth event dated more than 110 years before the evaluation date
 *       (an unknown date, or one that cannot be converted, does not exclude that the individual is living);</li>
 *   <li>an individual attribute of type {@code national_id} is {@link PrivacyLevel#CONFIDENTIAL}.</li>
 * </ul>
 * An individual or a group does not inherit from the other individuals and groups it mentions, to avoid cycles.
 * <p>
 * The resolver is a snapshot: it indexes the model on first use and caches the results, so it must be discarded when the
 * model changes (see {@link PrivacyService}).
 */
public final class PrivacyResolver{

	/** Where the effective level of a record comes from. */
	public enum Origin{
		/** No restriction applies. */
		NONE,
		/** The record declares it in its own {@code privacy} block. */
		DECLARED,
		/** The record references an individual or a group that is restricted. */
		INHERITED,
		/** The individual may be living. */
		LIVING_PERSON,
		/** The record is a national identification number. */
		NATIONAL_ID
	}


	/**
	 * The effective privacy of a record.
	 *
	 * @param level	The effective level.
	 * @param origin	Where the level comes from.
	 * @param reason	The reason of the restriction, if any.
	 * @param expires	The date the restriction expires, as written in the record, if any.
	 */
	public record Effective(PrivacyLevel level, Origin origin, String reason, String expires){

		static final Effective PUBLIC = new Effective(PrivacyLevel.PUBLIC, Origin.NONE, null, null);


		public boolean isRestricted(){
			return (level != PrivacyLevel.PUBLIC);
		}

	}


	private static final int LIVING_YEARS = 110;
	private static final String TAG_PRIVACY = "privacy";
	private static final String TAG_DATE = "date";
	private static final String ROLE_PRINCIPAL = "principal";
	private static final String TYPE_NATIONAL_ID = "national_id";


	private final FLEFModel model;
	private final LocalDate evaluationDate;

	private final Map<String, Effective> cache = new HashMap<>();
	private final Set<String> inProgress = new HashSet<>();

	private boolean indexed;
	/** Event id -> ids of the individuals and groups that participate in it. */
	private final Map<String, List<String>> participantsByEvent = new HashMap<>();
	/** Media id -> ids of the individuals and groups that use it as preferred image. */
	private final Map<String, List<String>> ownersByMedia = new HashMap<>();
	/** Individuals that are certainly not living. */
	private final Set<String> notLiving = new HashSet<>();


	public PrivacyResolver(final FLEFModel model){
		this(model, LocalDate.now());
	}

	public PrivacyResolver(final FLEFModel model, final LocalDate evaluationDate){
		this.model = model;
		this.evaluationDate = evaluationDate;
	}


	/** The effective privacy level of the record; {@link PrivacyLevel#PUBLIC} for a {@code null} record. */
	public PrivacyLevel effectiveLevel(final FLEFRecord record){
		return describe(record).level();
	}

	/**
	 * The effective level of a structure nested in a record (for example a source citation, which may carry its own
	 * {@code privacy} block): the most restrictive of the level of the record and the one declared by the structure.
	 *
	 * @param container	The record that contains the structure.
	 * @param nested	The nested structure.
	 * @return	The effective level.
	 */
	public PrivacyLevel effectiveLevel(final FLEFRecord container, final FLEFRecord nested){
		final PrivacyLevel inherited = effectiveLevel(container);
		final Effective declared = declared(nested);
		return (declared != null? PrivacyLevel.mostRestrictive(inherited, declared.level()): inherited);
	}

	/** The effective privacy of the record, with its origin. */
	public synchronized Effective describe(final FLEFRecord record){
		if(record == null)
			return Effective.PUBLIC;
		//a nested structure has no id: only its own block counts
		if(record.getId() == null){
			final Effective own = declared(record);
			return (own != null? own: Effective.PUBLIC);
		}

		final Effective cached = cache.get(record.getId());
		if(cached != null)
			return cached;

		//a cycle of references contributes nothing more than what has been found so far
		if(!inProgress.add(record.getId()))
			return Effective.PUBLIC;
		try{
			final Effective effective = compute(record);
			cache.put(record.getId(), effective);
			return effective;
		}
		finally{
			inProgress.remove(record.getId());
		}
	}


	private Effective compute(final FLEFRecord record){
		ensureIndexed();

		Effective result = declared(record);
		if(result == null)
			result = defaultFor(record);

		for(final String sourceId : inheritanceSources(record)){
			final FLEFRecord source = model.getRecordById(sourceId);
			if(source == null || source == record)
				continue;

			final Effective inherited = describe(source);
			if(inherited.level().compareTo(result.level()) > 0)
				result = new Effective(inherited.level(), Origin.INHERITED, inherited.reason(), inherited.expires());
		}
		return result;
	}

	/** The restriction declared by the {@code privacy} block of the node, {@code null} if absent or expired. */
	private Effective declared(final FLEFRecord node){
		if(node == null)
			return null;

		final List<FLEFRecord> blocks = FLEFRecordHelper.findChildren(node, TAG_PRIVACY);
		if(blocks.isEmpty())
			return null;

		final FLEFRecord privacy = blocks.getFirst();
		final String expires = PrivacyReader.extractExpires(privacy);
		if(isExpired(expires))
			return null;

		final PrivacyLevel level = PrivacyLevel.of(FLEFRecordHelper.getChildValue(privacy, PrivacyReader.TAG_LEVEL));
		return new Effective(level, Origin.DECLARED, PrivacyReader.extractReason(privacy), expires);
	}

	private boolean isExpired(final String expires){
		if(StringUtils.isBlank(expires))
			return false;

		final LocalDate lastDay = lastDayOf(expires.trim());
		return (lastDay != null && lastDay.isBefore(evaluationDate));
	}

	private static LocalDate lastDayOf(final String text){
		try{
			return LocalDate.parse(text);
		}
		catch(final RuntimeException ignored){
			//try a less precise date
		}
		try{
			return YearMonth.parse(text)
				.atEndOfMonth();
		}
		catch(final RuntimeException ignored){
			//try a year
		}
		try{
			return Year.parse(text)
				.atMonth(12)
				.atEndOfMonth();
		}
		catch(final RuntimeException ignored){
			return null;
		}
	}

	/** The level that applies when the record declares no (non-expired) privacy. */
	private Effective defaultFor(final FLEFRecord record){
		if(IndividualHandler.isOfType(record)){
			if(!notLiving.contains(record.getId()))
				return new Effective(PrivacyLevel.RESTRICTED, Origin.LIVING_PERSON, "living_person", null);
		}
		else if(IndividualAttributeHandler.isOfType(record)
				&& Strings.CI.equals(FLEFRecordHelper.getChildValue(record, IndividualAttributeReader.TAG_TYPE), TYPE_NATIONAL_ID))
			return new Effective(PrivacyLevel.CONFIDENTIAL, Origin.NATIONAL_ID, "sensitive_information", null);
		return Effective.PUBLIC;
	}

	/** The ids of the records whose effective level the record inherits. */
	private List<String> inheritanceSources(final FLEFRecord record){
		final List<String> sources = new ArrayList<>();
		if(IndividualHandler.isOfType(record) || GroupHandler.isOfType(record))
			return sources;

		if(EventHandler.isOfType(record))
			sources.addAll(participantsByEvent.getOrDefault(record.getId(), List.of()));
		else if(MediaHandler.isOfType(record))
			sources.addAll(ownersByMedia.getOrDefault(record.getId(), List.of()));
		collectReferencedPeople(record, sources);
		return sources;
	}

	/** Adds the ids of the individuals and groups referenced by the descendants of the node. */
	private void collectReferencedPeople(final FLEFRecord node, final List<String> sources){
		for(final FLEFRecord child : node.getChildren()){
			final String tag = child.getTag();
			if((Strings.CI.equals(tag, IndividualHandler.TYPE) || Strings.CI.equals(tag, GroupHandler.TYPE))
					&& StringUtils.isNotBlank(child.getValue()))
				sources.add(child.getValue());
			collectReferencedPeople(child, sources);
		}
	}


	/* ======================================================================
	 *                          Indexes
	 * ====================================================================== */

	private void ensureIndexed(){
		if(indexed)
			return;
		indexed = true;

		final Map<String, FLEFRecord> events = new HashMap<>();
		for(final FLEFRecord event : model.getRecordsByType(EventHandler.TYPE))
			events.put(event.getId(), event);

		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationHandler.TYPE)){
			final Participant participant = participantOf(participation);
			final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			if(participant == null || eventId == null)
				continue;

			final String participantId = participant.id();
			participantsByEvent.computeIfAbsent(eventId, k -> new ArrayList<>())
				.add(participantId);

			if(participant.individual() && isPrincipal(participation)
					&& isCertainlyDead(events.get(eventId)))
				notLiving.add(participantId);
		}

		for(final FLEFRecord owner : model.getRecordsByType(IndividualHandler.TYPE))
			indexPreferredImage(owner);
		for(final FLEFRecord owner : model.getRecordsByType(GroupHandler.TYPE))
			indexPreferredImage(owner);
	}

	private void indexPreferredImage(final FLEFRecord owner){
		final String mediaId = FLEFRecordHelper.getChildValue(owner,
			FLEFRecordHelper.composePath(IndividualReader.TAG_PREFERRED_IMAGE, PreferredImagePanel.TAG_MEDIA));
		if(StringUtils.isNotBlank(mediaId))
			ownersByMedia.computeIfAbsent(mediaId, k -> new ArrayList<>())
				.add(owner.getId());
	}

	/** The individual or group that participates, {@code null} if none. */
	private static Participant participantOf(final FLEFRecord participation){
		final FLEFRecord block = FLEFRecordHelper.findChild(participation, EventParticipationReader.TAG_PARTICIPANT);
		final FLEFRecord oneOf = (block != null? block.getTheOnlyChild(): null);
		if(oneOf == null || !(IndividualHandler.isOfType(oneOf) || GroupHandler.isOfType(oneOf)))
			return null;

		final FLEFRecord reference = oneOf.getTheOnlyChild();
		final String id = (reference != null? reference.getValue(): oneOf.getValue());
		return (StringUtils.isNotBlank(id)? new Participant(id, IndividualHandler.isOfType(oneOf)): null);
	}

	private record Participant(String id, boolean individual){}

	private static boolean isPrincipal(final FLEFRecord participation){
		for(final FLEFRecord role : FLEFRecordHelper.findChildren(participation, EventParticipationReader.TAG_ROLE))
			if(Strings.CI.equals(role.getValue(), ROLE_PRINCIPAL))
				return true;
		return false;
	}

	/** Whether the event proves that its principal is not living: a death, or a birth more than 110 years ago. */
	private boolean isCertainlyDead(final FLEFRecord event){
		if(event == null)
			return false;

		final String type = FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE);
		if(Strings.CI.equals(type, EventReader.ENUM_TYPE_DEATH))
			return true;
		if(!Strings.CI.equals(type, EventReader.ENUM_TYPE_BIRTH))
			return false;

		final FLEFRecord date = FLEFRecordHelper.findChildren(event, TAG_DATE)
			.stream()
			.findFirst()
			.orElse(null);
		if(date == null)
			return false;

		try{
			final TemporalSpan span = DateNormalizer.normalize(date);
			final long limit = evaluationDate.minusYears(LIVING_YEARS)
				.getLong(JulianFields.JULIAN_DAY);
			return (span != null && span.maxJdn() < limit);
		}
		catch(final RuntimeException ignored){
			//a date that cannot be converted does not exclude that the individual is living
			return false;
		}
	}

}
