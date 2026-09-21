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
package io.github.mtrevisan.familylegacy.v2.ui.tools.sources;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Shared extraction utilities for the source-related records:
 * {@code SourceRecord}, {@code RepositoryRecord}, and
 * {@code DocumentRecord}.
 * <p>
 * The methods here are the single point where source fields are read
 * from the model. Every tool in the {@code sources} package goes through
 * this class, so a change to the FLEF structure only needs to be
 * reflected here.
 */
public final class SourceHelper{

	public static final String TYPE_SOURCE = "source";
	public static final String TYPE_REPOSITORY = "repository";
	public static final String TYPE_DOCUMENT = "document";

	public static final String TAG_TITLE = "title";
	public static final String TAG_NAME = "name";
	public static final String TAG_VALUE = "value";
	public static final String TAG_AUTHOR = "author";
	public static final String TAG_PUBLISHER = "publisher";
	public static final String TAG_URI = "uri";
	public static final String TAG_DESCRIPTION = "description";
	public static final String TAG_MAPPING = "mapping";
	public static final String TAG_MEDIA_TYPE = "media_type";
	public static final String TAG_REPOSITORY = "repository";
	public static final String TAG_DOCUMENT = "document";
	public static final String TAG_CUSTODIAN = "custodian";
	public static final String TAG_PLACE = "place";
	public static final String TAG_SOURCE = "source";
	public static final String TAG_LOCATOR = "locator";
	public static final String TAG_NOTE = "note";
	public static final String TAG_EXTRACT = "extract";


	private SourceHelper(){
	}


	public static List<FLEFRecord> listAllSources(final FLEFModel model){
		return model.getRecordsByType(SourceHandler.TYPE);
	}

	public static List<FLEFRecord> listAllRepositories(final FLEFModel model){
		return model.getRecordsByType(RepositoryHandler.TYPE);
	}

	public static List<FLEFRecord> listAllDocuments(final FLEFModel model){
		return model.getRecordsByType(DocumentHandler.TYPE);
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
			return "";
		for(final FLEFRecord child : source.getChildren()){
			if(!TAG_TITLE.equalsIgnoreCase(child.getTag()))
				continue;
			final String value = FLEFRecordHelper.getChildValue(child, TAG_VALUE);
			if(value != null && !value.isBlank())
				return value;
			final FLEFRecord onlyChild = child.getTheOnlyChild();
			if(onlyChild != null && onlyChild.getValue() != null && !onlyChild.getValue().isBlank())
				return onlyChild.getValue();
		}
		return source.getId() != null? source.getId(): "";
	}

	/**
	 * Returns the name of a repository. {@code RepositoryRecord.name} is
	 * a list of {@code NameStructure}; the first one with a non-blank
	 * value is used.
	 */
	public static String repositoryName(final FLEFRecord repository){
		if(repository == null)
			return "";
		for(final FLEFRecord child : repository.getChildren()){
			if(!TAG_NAME.equalsIgnoreCase(child.getTag()))
				continue;
			final String value = FLEFRecordHelper.getChildValue(child, TAG_VALUE);
			if(value != null && !value.isBlank())
				return value;
		}
		return repository.getId() != null? repository.getId(): "";
	}

	public static String documentUri(final FLEFRecord document){
		return firstTextValue(document, TAG_URI);
	}

	public static String documentDescription(final FLEFRecord document){
		return firstTextValue(document, TAG_DESCRIPTION);
	}

	public static String sourceAuthor(final FLEFRecord source){
		return firstTextValue(source, TAG_AUTHOR);
	}

	public static String sourcePublisher(final FLEFRecord source){
		return firstTextValue(source, TAG_PUBLISHER);
	}

	public static String sourceMediaType(final FLEFRecord source){
		return firstTextValue(source, TAG_MEDIA_TYPE);
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
		for(final String type : allSourceCitingTypes())
			for(final FLEFRecord record : model.getRecordsByType(type))
				incrementCitations(record, counts);
		return counts;
	}

	/**
	 * Counts how many times a document is referenced by a source or by
	 * an extract.
	 */
	public static Map<String, Integer> countDocumentReferences(final FLEFModel model){
		final Map<String, Integer> counts = new LinkedHashMap<>();

		// Documents referenced directly by a source.
		for(final FLEFRecord source : listAllSources(model))
			for(final FLEFRecord child : source.getChildren())
				if(TAG_DOCUMENT.equalsIgnoreCase(child.getTag())){
					final String docId = child.getTheOnlyChild() != null
						? child.getTheOnlyChild().getValue(): null;
					if(docId != null)
						counts.merge(docId, 1, Integer::sum);
				}

		// Documents referenced by an extract inside a citation.
		for(final String type : allSourceCitingTypes())
			for(final FLEFRecord record : model.getRecordsByType(type))
				countDocumentsInCitations(record, counts);

		return counts;
	}

	private static void incrementCitations(final FLEFRecord record,
		final Map<String, Integer> counts){
		for(final FLEFRecord source : record.getChildren()){
			if(!TAG_SOURCE.equalsIgnoreCase(source.getTag()))
				continue;
			final String sourceId = extractReferencedId(source);
			if(sourceId != null)
				counts.merge(sourceId, 1, Integer::sum);
		}
	}

	private static void countDocumentsInCitations(final FLEFRecord record,
		final Map<String, Integer> counts){
		for(final FLEFRecord source : record.getChildren()){
			if(!TAG_SOURCE.equalsIgnoreCase(source.getTag()))
				continue;
			for(final FLEFRecord extract : source.getChildren()){
				if(!TAG_EXTRACT.equalsIgnoreCase(extract.getTag()))
					continue;
				for(final FLEFRecord part : extract.getChildren()){
					if(!"document_part".equalsIgnoreCase(part.getTag()))
						continue;
					final String docId = FLEFRecordHelper.getChildValue(part, TAG_DOCUMENT);
					if(docId != null)
						counts.merge(docId, 1, Integer::sum);
				}
			}
		}
	}

	private static String extractReferencedId(final FLEFRecord sourceBlock){
		final String direct = FLEFRecordHelper.getChildValue(sourceBlock, TAG_SOURCE);
		if(direct != null)
			return direct;
		final FLEFRecord inner = FLEFRecordHelper.findChild(sourceBlock, TAG_SOURCE);
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
	private static List<String> allSourceCitingTypes(){
		return List.of(
			"individual", "group", "event", "individual_attribute",
			"event_participation", "group_attribute", "relationship",
			"place", "place_relationship", "source", "cultural_norm",
			"historic_event", "context_impact", "identity_hypothesis",
			"research_question", "research_activity", "conclusion"
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
	public record DocumentRow(String id, String uri, String description,
									  String mapping, int referenceCount){}

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
				if(TAG_REPOSITORY.equalsIgnoreCase(child.getTag())){
					final String repoId = FLEFRecordHelper.getChildValue(child, TAG_REPOSITORY);
					if(id != null && id.equals(repoId)){
						sourceCount++;
						break;
					}
				}
		return new RepositoryRow(
			id,
			repositoryName(repository),
			firstTextValue(repository, TAG_PLACE),
			sourceCount
		);
	}

	/** Builds a {@link DocumentRow} with the reference count. */
	public static DocumentRow toDocumentRow(final FLEFRecord document,
		final Map<String, Integer> referenceCounts){
		final String id = document.getId();
		return new DocumentRow(
			id,
			documentUri(document),
			documentDescription(document),
			firstTextValue(document, TAG_MAPPING),
			referenceCounts.getOrDefault(id, 0)
		);
	}

}
