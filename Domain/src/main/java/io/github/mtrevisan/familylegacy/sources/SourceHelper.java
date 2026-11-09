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
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.CulturalNormReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.HistoricEventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.preferences.MediaPathDisplay;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Shared extraction utilities for the source-related records:
 * {@code SourceRecord}, {@code RepositoryRecord}, and
 * {@code MediaRecord}.
 * <p>
 * The methods here are the single point where source fields are read
 * from the model. Every tool in the {@code sources} package goes through
 * this class, so a change to the FLEF structure only needs to be
 * reflected here.
 */
public final class SourceHelper{

	private SourceHelper(){}


	public static List<FLEFRecord> listAllSources(final FLEFModel model){
		return PrivacyService.visible(model, model.getRecordsByType(SourceReader.TYPE));
	}

	public static List<FLEFRecord> listAllRepositories(final FLEFModel model){
		return PrivacyService.visible(model, model.getRecordsByType(RepositoryReader.TYPE));
	}

	public static List<FLEFRecord> listAllDocuments(final FLEFModel model){
		return PrivacyService.visible(model, model.getRecordsByType(MediaReader.TYPE));
	}

	public static Map<String, FLEFRecord> indexById(final List<FLEFRecord> records){
		final Map<String, FLEFRecord> result = new LinkedHashMap<>();
		for(final FLEFRecord record : records)
			if(record.getId() != null)
				result.put(record.getId(), record);
		return result;
	}

	/**
	 * Returns the first non-blank text value found among the direct
	 * children of the record with the given tag. When the child has a
	 * single nested child, its value is used as a fallback.
	 */
	public static String firstTextValue(final FLEFRecord record, final String tag){
		if(record == null)
			return null;
		final String direct = FLEFRecordHelper.getChildValue(record, tag);
		if(direct != null && !direct.isBlank())
			return direct;
		final FLEFRecord child = FLEFRecordHelper.findChild(record, tag);
		if(child == null)
			return null;
		final FLEFRecord onlyChild = child.getTheOnlyChild();
		return (onlyChild != null? onlyChild.getValue(): null);
	}

	/**
	 * Returns the title of a source. {@code SourceRecord.title} is a list
	 * of {@code NameStructure}; the first one with a non-blank value is
	 * used. Falls back to the record id.
	 */
	public static String sourceTitle(final FLEFRecord source){
		if(source == null)
			return StringUtils.EMPTY;

		final String title = SourceReader.extractPrimaryTitle(source);
		if(StringUtils.isNotEmpty(title))
			return title;

		for(final FLEFRecord child : source.getChildren()){
			if(!SourceReader.TAG_TITLE.equalsIgnoreCase(child.getTag()))
				continue;

			final FLEFRecord onlyChild = child.getTheOnlyChild();
			if(onlyChild != null && onlyChild.getValue() != null && !onlyChild.getValue().isBlank())
				return onlyChild.getValue();
		}
		return (source.getId() != null? source.getId(): StringUtils.EMPTY);
	}

	/**
	 * Returns the name of a repository. {@code RepositoryRecord.name} is
	 * a list of {@code NameStructure}; the first one with a non-blank
	 * value is used.
	 */
	public static String repositoryName(final FLEFRecord repository){
		if(repository == null)
			return StringUtils.EMPTY;

		final String name = RepositoryReader.extractPrimaryName(repository);
		if(StringUtils.isNotEmpty(name))
			return name;
		return (repository.getId() != null? repository.getId(): StringUtils.EMPTY);
	}

	public static String documentUri(final FLEFRecord document){
		return firstTextValue(document, MediaReader.TAG_URI);
	}

	/** The location of the media as the user wants to see it (see {@link MediaPathDisplay}): never use it to open the file. */
	public static String documentDisplayUri(final FLEFRecord document){
		return MediaPathDisplay.display(documentUri(document));
	}

	public static String documentDescription(final FLEFRecord document){
		return firstTextValue(document, MediaReader.TAG_DESCRIPTION);
	}

	public static String sourceAuthor(final FLEFRecord source){
		return firstTextValue(source, SourceReader.TAG_AUTHOR);
	}

	public static String sourcePublisher(final FLEFRecord source){
		return firstTextValue(source, SourceReader.TAG_PUBLISHER);
	}

	public static String sourceMediaType(final FLEFRecord source){
		return firstTextValue(source, SourceReader.TAG_MEDIA_TYPE);
	}


	/* ======================================================================
	 *                          Usage indexing
	 * ====================================================================== */

	/**
	 * Counts how many times a source is referenced by a citation. The
	 * result is keyed by source id. Sources that never appear in the
	 * map are unused.
	 */
	public static Map<String, Integer> countCitationsPerSource(final FLEFModel model){
		final Map<String, Integer> counts = new LinkedHashMap<>();
		for(final String type : allSourceCitingTypes()){
			final List<FLEFRecord> records = PrivacyService.visible(model, model.getRecordsByType(type));
			for(final FLEFRecord record : records)
				incrementCitations(record, counts);
		}
		return counts;
	}

	/**
	 * Counts how many times a document is referenced. Every reference to a media record is a {@code media} field, wherever it is: in a
	 * source, in the extract of a citation, in the preferred image of an individual or a group, in a conclusion, an identity
	 * hypothesis, a research question... so the whole model is read, not only the sources and their citations (which used to leave
	 * the documents of the other places looking unused). A hidden record counts too: that it is not shown does not make the document
	 * unused.
	 */
	public static Map<String, Integer> countDocumentReferences(final FLEFModel model){
		final Map<String, Integer> counts = new LinkedHashMap<>();
		for(final FLEFRecord record : model.getRecords())
			if(!MediaReader.isOfType(record))
				countMediaReferences(record, counts);
		return counts;
	}

	private static void countMediaReferences(final FLEFRecord node, final Map<String, Integer> counts){
		for(final FLEFRecord child : node.getChildren()){
			if(MediaReader.isOfType(child)){
				//the reference is the value of the field, or (if the field is a typed union) of its only child
				String docId = child.getValue();
				if(StringUtils.isBlank(docId) && child.getChildren().size() == 1)
					docId = child.getChildren().getFirst().getValue();
				if(StringUtils.isNotBlank(docId))
					counts.merge(docId.strip(), 1, Integer::sum);
			}
			countMediaReferences(child, counts);
		}
	}

	private static void incrementCitations(final FLEFRecord record,
		final Map<String, Integer> counts){
		for(final FLEFRecord source : record.getChildren()){
			if(!SourceReader.isOfType(source))
				continue;

			final String sourceId = extractReferencedId(source);
			if(sourceId != null)
				counts.merge(sourceId, 1, Integer::sum);
		}
	}

	public static String extractReferencedId(final FLEFRecord sourceBlock){
		final String direct = FLEFRecordHelper.getChildValue(sourceBlock, SourceReader.TYPE);
		if(direct != null)
			return direct;

		final FLEFRecord inner = FLEFRecordHelper.findChild(sourceBlock, SourceReader.TYPE);
		if(inner != null){
			final FLEFRecord only = inner.getTheOnlyChild();
			if(only != null)
				return only.getValue();
		}
		return null;
	}

	/**
	 * Returns every record type that can carry a {@code source} child,
	 * i.e. every type that can cite a source.
	 */
	public static List<String> allSourceCitingTypes(){
		return List.of(
			IndividualReader.TYPE, GroupReader.TYPE, EventReader.TYPE, IndividualAttributeReader.TYPE,
			EventParticipationReader.TYPE, GroupAttributeReader.TYPE, RelationshipReader.TYPE,
			PlaceReader.TYPE, PlaceRelationshipReader.TYPE, SourceReader.TYPE, CulturalNormReader.TYPE,
			HistoricEventReader.TYPE, ContextImpactReader.TYPE, IdentityHypothesisReader.TYPE,
			ResearchQuestionReader.TYPE, ResearchActivityReader.TYPE, ConclusionReader.TYPE
		);
	}


	/* ======================================================================
	 *                          Row models
	 * ====================================================================== */

	/** Row for the source management table. */
	public record SourceRow(String id, String title, String author,
									String publisher, String mediaType, int citationCount){}

	/** Row for the repository management table. */
	public record RepositoryRow(String id, String name, String place,
										 int sourceCount){}

	/** Row for the document management table. */
	public record DocumentRow(String id, String uri, String description, String mapping, int referenceCount){}


	/** Builds a {@link SourceRow} with the usage count. */
	public static SourceRow toSourceRow(final FLEFRecord source,
		final Map<String, Integer> citationCounts){
		final String id = source.getId();
		return new SourceRow(
			id,
			sourceTitle(source),
			sourceAuthor(source),
			sourcePublisher(source),
			sourceMediaType(source),
			citationCounts.getOrDefault(id, 0)
		);
	}

	/** Builds a {@link RepositoryRow} with the usage count. */
	public static RepositoryRow toRepositoryRow(final FLEFRecord repository,
		final FLEFModel model){
		final String id = repository.getId();
		int sourceCount = 0;
		for(final FLEFRecord source : listAllSources(model))
			for(final FLEFRecord child : source.getChildren())
				if(RepositoryReader.isOfType(child)){
					final String repoId = FLEFRecordHelper.getChildValue(child, RepositoryReader.TYPE);
					if(id != null && id.equals(repoId)){
						sourceCount ++;
						break;
					}
				}
		return new RepositoryRow(
			id,
			repositoryName(repository),
			firstTextValue(repository, RepositoryReader.TAG_PLACE),
			sourceCount
		);
	}

	/** Builds a {@link DocumentRow} with the reference count. */
	public static DocumentRow toDocumentRow(final FLEFRecord document,
		final Map<String, Integer> referenceCounts){
		final String id = document.getId();
		return new DocumentRow(
			id,
			documentDisplayUri(document),
			documentDescription(document),
			firstTextValue(document, MediaReader.TAG_MAPPING),
			referenceCounts.getOrDefault(id, 0)
		);
	}

}
