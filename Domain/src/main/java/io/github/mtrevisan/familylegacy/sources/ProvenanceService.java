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
package io.github.mtrevisan.familylegacy.sources;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EvidenceQualifiersReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The provenance of an assertion: where it comes from, that is the chain
 * <pre>assertion -> citation (locator, extract, stance, evidence) -> source -> repository, document</pre>
 * It is read from the model and never changes it. The records hidden by the privacy policy are left out.
 * <p>
 * Three views of the same chain:
 * <ul>
 *   <li>{@link #ofRecord(FLEFModel, FLEFRecord)}: the citations of one record;</li>
 *   <li>{@link #ofIndividual(FLEFModel, String)}: the citations of an individual and of everything that says something about them (their
 *       events, attributes and relationships);</li>
 *   <li>{@link #ofSource(FLEFModel, String)}: the other way round, everything that depends on a source.</li>
 * </ul>
 */
public final class ProvenanceService{

	/** How the values of a repeated field (extracts, repositories, documents) are joined in a row. */
	private static final String SEPARATOR = " | ";


	/**
	 * One citation, with the record that carries it.
	 *
	 * @param ownerId	The identifier of the record that carries the citation (the assertion).
	 * @param ownerType	The type of that record (its tag).
	 * @param ownerText	The text that names that record to the user.
	 * @param sourceId	The identifier of the cited source ({@code null} if the citation does not say it).
	 * @param sourceTitle	The title of the source (its identifier if it has none, empty if it is not in the model).
	 * @param locator	The place in the source (empty if none).
	 * @param stance	{@code supports} or {@code contradicts}.
	 * @param extractText	The texts of the extracts, joined (empty if none).
	 * @param extractType	The way the first text relates to the source ({@code verbatim}, ...; empty if none).
	 * @param sourceType	Evidence qualifier: {@code original} or {@code derived} (empty if not assessed).
	 * @param informationType	Evidence qualifier: {@code primary}, {@code secondary} or {@code undetermined} (empty if not assessed).
	 * @param evidenceType	Evidence qualifier: {@code direct}, {@code indirect} or {@code negative} (empty if not assessed).
	 * @param repositories	The names of the repositories that hold the source, joined (empty if none).
	 * @param documents	The identifiers of the documents of the extracts, joined (empty if none).
	 */
	public record Row(String ownerId, String ownerType, String ownerText, String sourceId, String sourceTitle, String locator,
			String stance, String extractText, String extractType, String sourceType, String informationType, String evidenceType,
			String repositories, String documents){

		/** @return	Whether the citation contradicts the assertion. */
		public boolean contradicts(){
			return SourceCitationReader.STANCE_CONTRADICTS.equals(stance);
		}

		/** @return	Whether any evidence qualifier is given. */
		public boolean isAssessed(){
			return (!sourceType.isEmpty() || !informationType.isEmpty() || !evidenceType.isEmpty());
		}

		/** @return	The evidence qualifiers as one line ("original, primary, direct"), empty if none is given. */
		public String evidence(){
			final List<String> parts = new ArrayList<>(3);
			for(final String part : new String[]{sourceType, informationType, evidenceType})
				if(!part.isEmpty())
					parts.add(part);
			return String.join(", ", parts);
		}
	}


	private ProvenanceService(){}


	/**
	 * @param model	The model.
	 * @param record	The record.
	 * @return	The citations that the record carries, in the order of the file; none if the record is hidden by the privacy policy.
	 */
	public static List<Row> ofRecord(final FLEFModel model, final FLEFRecord record){
		final List<Row> rows = new ArrayList<>();
		if(record != null && !PrivacyService.isHidden(model, record))
			collect(model, record, rows);
		return rows;
	}

	/**
	 * The citations of an individual and of the records that assert something about them: the individual itself, its participations in
	 * events and the events, its attributes, the relationships in which it is subject or target.
	 *
	 * @param model	The model.
	 * @param individualId	The identifier of the individual.
	 * @return	The citations, the individual's own first.
	 */
	public static List<Row> ofIndividual(final FLEFModel model, final String individualId){
		final FLEFRecord individual = (individualId != null? model.getRecordById(individualId): null);
		final List<Row> rows = new ArrayList<>();
		if(individual == null || PrivacyService.isHidden(model, individual))
			return rows;

		final Set<String> done = new LinkedHashSet<>();
		visit(model, individual, rows, done);

		for(final FLEFRecord participation : PrivacyService.visible(model, model.getRecordsByType(EventParticipationReader.TYPE))){
			if(!individualId.equals(EventParticipationReader.extractParticipantRef(participation)))
				continue;

			visit(model, participation, rows, done);
			final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			final FLEFRecord event = (eventId != null? model.getRecordById(eventId): null);
			if(event != null && !PrivacyService.isHidden(model, event))
				visit(model, event, rows, done);
		}

		for(final FLEFRecord attribute : PrivacyService.visible(model, model.getRecordsByType(IndividualAttributeReader.TYPE)))
			if(individualId.equals(FLEFRecordHelper.getChildValue(attribute, IndividualReader.TYPE)))
				visit(model, attribute, rows, done);

		for(final FLEFRecord relationship : PrivacyService.visible(model, model.getRecordsByType(RelationshipReader.TYPE))){
			final String subjectId = EventParticipationReader.extractParticipantRef(relationship, RelationshipReader.TAG_SUBJECT);
			final String targetId = EventParticipationReader.extractParticipantRef(relationship, RelationshipReader.TAG_TARGET);
			if(individualId.equals(subjectId) || individualId.equals(targetId))
				visit(model, relationship, rows, done);
		}
		return rows;
	}

	/**
	 * Everything that cites the source: the other direction of the chain, to see what depends on it.
	 *
	 * @param model	The model.
	 * @param sourceId	The identifier of the source.
	 * @return	The citations of that source, grouped by the type of the records that carry them.
	 */
	public static List<Row> ofSource(final FLEFModel model, final String sourceId){
		final List<Row> rows = new ArrayList<>();
		if(sourceId == null)
			return rows;

		for(final String type : SourceHelper.allSourceCitingTypes())
			for(final FLEFRecord owner : PrivacyService.visible(model, model.getRecordsByType(type)))
				for(final FLEFRecord citation : citationsOf(owner))
					if(sourceId.equals(SourceHelper.extractReferencedId(citation)))
						rows.add(toRow(model, owner, citation));
		return rows;
	}


	private static void visit(final FLEFModel model, final FLEFRecord record, final List<Row> rows, final Set<String> done){
		if(done.add(record.getId()))
			collect(model, record, rows);
	}

	private static void collect(final FLEFModel model, final FLEFRecord owner, final List<Row> rows){
		for(final FLEFRecord citation : citationsOf(owner))
			rows.add(toRow(model, owner, citation));
	}

	private static List<FLEFRecord> citationsOf(final FLEFRecord owner){
		final List<FLEFRecord> citations = new ArrayList<>();
		for(final FLEFRecord child : owner.getChildren())
			//what a source is derived from is cited too: the chain from a transcription back to its original
			if(SourceReader.isOfType(child) || SourceHelper.isDerivedFrom(child))
				citations.add(child);
		return citations;
	}

	private static Row toRow(final FLEFModel model, final FLEFRecord owner, final FLEFRecord citation){
		final String sourceId = SourceHelper.extractReferencedId(citation);
		final FLEFRecord source = (sourceId != null? model.getRecordById(sourceId): null);

		final List<String> texts = new ArrayList<>();
		String extractType = StringUtils.EMPTY;
		final Map<String, String> documents = new LinkedHashMap<>();
		for(final FLEFRecord extract : FLEFRecordHelper.findChildren(citation, SourceCitationReader.TAG_EXTRACT)){
			final String text = FLEFRecordHelper.getChildValue(extract, SourceCitationReader.TAG_TEXT);
			if(StringUtils.isNotBlank(text)){
				if(texts.isEmpty())
					extractType = StringUtils.defaultString(FLEFRecordHelper.getChildValue(extract, SourceCitationReader.TAG_TYPE));
				texts.add(text.strip());
			}
			for(final FLEFRecord part : FLEFRecordHelper.findChildren(extract, SourceCitationReader.TAG_MEDIA_PART)){
				final String documentId = FLEFRecordHelper.getChildValue(part, SourceCitationReader.TAG_MEDIA);
				if(documentId != null)
					documents.put(documentId, documentId);
			}
		}

		final List<String> repositories = new ArrayList<>();
		if(source != null)
			for(final FLEFRecord child : source.getChildren())
				if(RepositoryReader.isOfType(child)){
					final String repositoryId = FLEFRecordHelper.getChildValue(child, RepositoryReader.TYPE);
					final FLEFRecord repository = (repositoryId != null? model.getRecordById(repositoryId): null);
					if(repository != null)
						repositories.add(SourceHelper.repositoryName(repository));
				}

		final FLEFRecord evidence = FLEFRecordHelper.findChild(citation, SourceCitationReader.TAG_EVIDENCE);
		final String stance = FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_STANCE);
		return new Row(owner.getId(), owner.getTag(), RecordDisplay.describe(owner, model), sourceId,
			(source != null? SourceHelper.sourceTitle(source): StringUtils.EMPTY),
			StringUtils.defaultString(FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_LOCATOR)),
			(stance != null? stance: SourceCitationReader.STANCE_SUPPORTS), String.join(SEPARATOR, texts), extractType,
			(evidence != null? StringUtils.defaultString(EvidenceQualifiersReader.extractSourceType(evidence)): StringUtils.EMPTY),
			(evidence != null? StringUtils.defaultString(EvidenceQualifiersReader.extractInformationType(evidence)): StringUtils.EMPTY),
			(evidence != null? StringUtils.defaultString(EvidenceQualifiersReader.extractEvidenceType(evidence)): StringUtils.EMPTY),
			String.join(SEPARATOR, repositories), String.join(SEPARATOR, documents.keySet()));
	}

}
