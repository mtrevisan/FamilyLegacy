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
package io.github.mtrevisan.familylegacy.io;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditStamp;
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Migrates the citations written when the extracts were structures inside them: the {@code extract} of a SourceCitation
 * <pre>
 * individual I1 {
 *   source {
 *     source S1
 *     extract {
 *       text "Born on the 3rd"
 *       type verbatim
 *     }
 *     stance supports
 *   }
 * }
 * </pre>
 * becomes an ExtractRecord of the source cited, referenced by the citation
 * <pre>
 * individual I1 {
 *   source {
 *     source S1
 *     extract X1
 *     stance supports
 *   }
 * }
 * extract X1 {
 *   source S1
 *   text "Born on the 3rd"
 *   type verbatim
 *   audit { creation { date ... comment "Migrated from the extract of a citation" } }
 * }
 * </pre>
 * so that every extract belongs to the source cited ({@code require every e in extract: e.source == source}).
 * <p>
 * The same extract of the same source, written in several citations, becomes one record that all of them reference. The extract is
 * dated as the record that held the citation. Migrating a model that has already been migrated does nothing.
 * <p>
 * The importers build the extracts as structures too, and call this migration when they are done (after the sources have been
 * merged, so that each extract belongs to the source that is left).
 */
public final class ExtractMigration{

	/** The comment of the creation of the extracts made by the migration. */
	public static final String MIGRATION_COMMENT = "Migrated from the extract of a citation";

	private static final String TAG_AUDIT_CREATION_DATE = FLEFRecordHelper.composePath(AuditReader.TAG_AUDIT, AuditReader.TAG_CREATION_DATE);


	private ExtractMigration(){}


	/**
	 * @param model	The model to migrate (it is changed in place).
	 * @return	The number of the extracts written as structures that now reference a record.
	 */
	public static int migrate(final FLEFModel model){
		final Migration migration = new Migration(model);
		for(final FLEFRecord record : new ArrayList<>(model.getRecords()))
			//an extract record holds no citation
			if(!ExtractReader.isOfType(record))
				migration.migrateRecord(record);
		return migration.migrated;
	}


	private static final class Migration{

		private final FLEFModel model;
		private final IdSequence sequence;
		/** The content of an extract (its source and its fields) to the id of the record made for it. */
		private final Map<FLEFRecord, String> idsByContent = new HashMap<>();
		private int migrated;


		private Migration(final FLEFModel model){
			this.model = model;
			sequence = new IdSequence(model);
		}


		private void migrateRecord(final FLEFRecord record){
			final Deque<FLEFRecord> stack = new ArrayDeque<>();
			stack.push(record);
			while(!stack.isEmpty()){
				final FLEFRecord node = stack.pop();
				final List<FLEFRecord> children = node.getChildren();
				if(hasStructuredExtract(node))
					migrateCitation(record, node);

				for(int i = children.size() - 1; i >= 0; i --)
					stack.push(children.get(i));
			}
		}

		/** A citation that holds an extract as a structure (with fields and no value). */
		private static boolean hasStructuredExtract(final FLEFRecord node){
			boolean extract = false;
			boolean source = false;
			for(final FLEFRecord child : node.getChildren()){
				final String tag = child.getTag();
				if(Strings.CI.equals(SourceCitationReader.TAG_EXTRACT, tag) && StringUtils.isEmpty(child.getValue()) && child.hasChildren())
					extract = true;
				else if(Strings.CI.equals(SourceCitationReader.TAG_SOURCE, tag) && StringUtils.isNotEmpty(child.getValue()))
					source = true;
			}
			return (extract && source);
		}

		private void migrateCitation(final FLEFRecord owner, final FLEFRecord citation){
			final String sourceId = SourceCitationReader.extractSource(citation);
			final List<FLEFRecord> children = citation.getChildren();
			for(int i = 0; i < children.size(); i ++){
				final FLEFRecord child = children.get(i);
				if(!Strings.CI.equals(SourceCitationReader.TAG_EXTRACT, child.getTag()) || StringUtils.isNotEmpty(child.getValue())
						|| !child.hasChildren())
					continue;

				final String extractId = extractIdOf(owner, sourceId, child);
				children.set(i, FLEFRecord.createChildWithTagAndValue(child.getTag(), extractId));
				migrated ++;
			}
		}

		private String extractIdOf(final FLEFRecord owner, final String sourceId, final FLEFRecord structure){
			final FLEFRecord content = FLEFRecord.createChildWithTag(ExtractReader.TYPE)
				.addChild(FLEFRecord.createChildWithTagAndValue(ExtractReader.TAG_SOURCE, sourceId));
			for(final FLEFRecord field : structure.getChildren()){
				final FLEFRecord copy = FLEFRecord.createEmpty();
				field.deepCopyTo(copy);
				content.addChild(copy);
			}

			final String existingId = idsByContent.get(content);
			if(existingId != null)
				return existingId;

			final String id = sequence.next();
			final FLEFRecord extract = FLEFRecord.createMainRecord(id, ExtractReader.TYPE);
			for(final FLEFRecord field : content.getChildren()){
				final FLEFRecord copy = FLEFRecord.createEmpty();
				field.deepCopyTo(copy);
				extract.addChild(copy);
			}
			extract.addChild(auditOf(owner));
			model.addRecord(extract);
			idsByContent.put(content, id);
			return id;
		}

		private static FLEFRecord auditOf(final FLEFRecord owner){
			final String date = FLEFRecordHelper.getChildValue(owner, TAG_AUDIT_CREATION_DATE);
			if(date == null){
				final FLEFRecord audit = AuditStamp.now();
				FLEFRecordHelper.findChild(audit, AuditReader.TAG_CREATION)
					.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_COMMENT, MIGRATION_COMMENT));
				return audit;
			}

			return FLEFRecord.createChildWithTag(AuditReader.TAG_AUDIT)
				.addChild(FLEFRecord.createChildWithTag(AuditReader.TAG_CREATION)
					.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_DATE, date))
					.addChild(FLEFRecord.createChildWithTagAndValue(AuditReader.TAG_COMMENT, MIGRATION_COMMENT)));
		}

	}


	/** Hands out the ids of the extracts that are free in the model (the ids are unique ignoring the case). */
	private static final class IdSequence{

		private final FLEFModel model;
		private long last;


		private IdSequence(final FLEFModel model){
			this.model = model;

			final String prefix = ExtractReader.ID_PREFIX;
			for(final FLEFRecord extract : model.getRecordsByType(ExtractReader.TYPE)){
				final String id = extract.getId();
				if(id != null && Strings.CI.startsWith(id, prefix) && StringUtils.isNumeric(id.substring(prefix.length()))
						&& id.length() - prefix.length() < 18)
					last = Math.max(last, Long.parseLong(id.substring(prefix.length())));
			}
		}

		private String next(){
			String id;
			do{
				last ++;
				id = ExtractReader.ID_PREFIX + last;
			}while(model.hasRecord(id));
			return id;
		}

	}

}
