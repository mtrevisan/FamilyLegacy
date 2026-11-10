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
package io.github.mtrevisan.familylegacy.importer.gedcom.converters;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Merges the repositories that are the same repository: the ones with the same name (a GEDCOM file repeats a repository, once for each
 * program that exported it or for each source that cites it) whose place and contacts do not contradict each other.
 * <p>
 * The first repository survives, completed with what the others say more; the references to the others follow the survivor.
 */
final class RepositoryMerger{

	private RepositoryMerger(){}


	/**
	 * @param repositoryMap	The repositories by id: the merged ones are removed from it.
	 */
	static void merge(final FLEFModel model, final Map<String, FLEFRecord> repositoryMap){
		final Map<String, List<FLEFRecord>> survivorsByName = new HashMap<>();
		final Map<String, String> merged = new HashMap<>();
		for(final FLEFRecord repository : new ArrayList<>(model.getRecordsByType(RepositoryReader.TYPE))){
			final String name = nameOf(repository);
			if(StringUtils.isEmpty(name))
				continue;

			final List<FLEFRecord> candidates = survivorsByName.computeIfAbsent(name, k -> new ArrayList<>(1));
			FLEFRecord survivor = null;
			for(final FLEFRecord candidate : candidates)
				if(compatible(candidate, repository)){
					survivor = candidate;
					break;
				}
			if(survivor == null){
				candidates.add(repository);
				continue;
			}

			mergeInto(survivor, repository);
			merged.put(repository.getId(), survivor.getId());
		}
		if(merged.isEmpty())
			return;

		for(final FLEFRecord record : model.getRecords())
			remap(record, merged);
		for(final String duplicateId : merged.keySet()){
			model.removeRecord(duplicateId);
			repositoryMap.remove(duplicateId);
		}
		for(final FLEFRecord source : model.getRecords())
			removeRepeatedCitations(source);
	}

	/** The first name of the repository, without capitals and with the spaces normalized. */
	private static String nameOf(final FLEFRecord repository){
		final List<FLEFRecord> names = FLEFRecordHelper.findChildren(repository, RepositoryReader.TAG_NAME);
		return StringUtils.lowerCase(StringUtils.normalizeSpace(names.isEmpty()? null: FLEFRecordHelper.getChildValue(names.getFirst(),
			NameReader.TAG_VALUE)));
	}

	/** Two repositories with the same name are the same one unless they both say a place (or contacts) and these are different. */
	private static boolean compatible(final FLEFRecord first, final FLEFRecord second){
		return (compatible(signatures(first, RepositoryReader.TAG_PLACE), signatures(second, RepositoryReader.TAG_PLACE))
			&& compatible(signatures(first, RepositoryReader.TAG_CONTACT), signatures(second, RepositoryReader.TAG_CONTACT)));
	}

	private static boolean compatible(final Set<String> first, final Set<String> second){
		return (first.isEmpty() || second.isEmpty() || first.containsAll(second) || second.containsAll(first));
	}

	private static Set<String> signatures(final FLEFRecord repository, final String tag){
		final Set<String> signatures = new HashSet<>();
		for(final FLEFRecord child : FLEFRecordHelper.findChildren(repository, tag))
			signatures.add(Deduplicator.computeSignature(child));
		return signatures;
	}

	/** What the duplicate says that the survivor does not (a place, a contact, a note) is not lost: it is added to the survivor. */
	private static void mergeInto(final FLEFRecord survivor, final FLEFRecord duplicate){
		final Set<String> known = new HashSet<>();
		final Set<String> knownTags = new HashSet<>();
		for(final FLEFRecord child : survivor.getChildren()){
			known.add(Deduplicator.computeSignature(child));
			knownTags.add(child.getTag());
		}
		for(final FLEFRecord child : new ArrayList<>(duplicate.getChildren())){
			final String tag = child.getTag();
			if(AuditReader.TAG_AUDIT.equals(tag) || known.contains(Deduplicator.computeSignature(child)))
				continue;
			//a field that can be only one stays the one of the survivor
			final boolean repeatable = (RepositoryReader.TAG_NAME.equals(tag) || RepositoryReader.TAG_CONTACT.equals(tag)
				|| RepositoryReader.TAG_NOTE.equals(tag));
			if(!repeatable && knownTags.contains(tag))
				continue;

			//the audit is the last field of a record
			final List<FLEFRecord> children = survivor.getChildren();
			int index = children.size();
			for(int i = 0; i < children.size(); i ++)
				if(AuditReader.TAG_AUDIT.equals(children.get(i).getTag())){
					index = i;
					break;
				}
			children.add(index, child);
		}
	}

	/** Makes the references to a merged repository refer to the survivor. */
	private static void remap(final FLEFRecord node, final Map<String, String> merged){
		if(RepositoryCitationReader.TAG_REPOSITORY.equals(node.getTag()) && node.getValue() != null && merged.containsKey(node.getValue()))
			node.setValue(merged.get(node.getValue()));
		for(final FLEFRecord child : node.getChildren())
			remap(child, merged);
	}

	/** A source that cited two repositories now merged, in the same way, cites the survivor once. */
	private static void removeRepeatedCitations(final FLEFRecord record){
		if(!SourceReader.TYPE.equalsIgnoreCase(record.getTag()))
			return;

		final Set<String> seen = new HashSet<>();
		record.getChildren().removeIf(child -> SourceReader.TAG_REPOSITORY.equals(child.getTag())
			&& !seen.add(Deduplicator.computeSignature(child)));
	}

}
