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
package io.github.mtrevisan.familylegacy.io.merger;

import io.github.mtrevisan.familylegacy.io.grammar.XrefPathIndex;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.StructType;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Finds and rewrites the references between records.
 * <p>
 * References are located through the Xref paths of the grammar (see {@link XrefPathIndex}), never by comparing arbitrary values with
 * record ids, so that a text that happens to equal an id is left untouched. Links written as {@code flef:<Id>} inside notes are
 * rewritten too, as the protocol requires on every rename.
 * <p>
 * References inside fields unknown to the grammar are not maintained, as stated by the protocol.
 */
final class ReferenceRewriter{

	private static final String TAG_NOTE = "note";
	private static final String TAG_TEXT = "text";
	private static final String TAG_TRANSLATION = "translation";
	private static final String WILDCARD = "[*]";

	/** A {@code flef:} link; the id stops at the first character that cannot belong to an id, so {@code flef:I1} never matches {@code flef:I12}. */
	private static final Pattern FLEF_LINK = Pattern.compile(Pattern.quote(FLEFModel.FLEF_LINK_PREFIX) + "([A-Za-z][A-Za-z0-9_-]*)");


	/**
	 * A path, relative to a record of some kind, that holds a reference to a record of {@code targetKind}.
	 */
	record XrefPath(String path, String targetKind){}

	@FunctionalInterface
	interface ReferenceVisitor{
		/**
		 * @param owner      the top-level record that contains the reference
		 * @param parent     the node that holds the reference (e.g. the citation holding a {@code source} reference)
		 * @param node       the node whose value is the referenced id
		 * @param path       the path of the node, relative to the owner
		 * @param targetKind the record kind the reference must point to
		 */
		void visit(FLEFRecord owner, FLEFRecord parent, FLEFRecord node, String path, String targetKind);
	}


	private final Map<String, List<XrefPath>> pathsByKind = new HashMap<>();


	ReferenceRewriter(final GrammarSupport grammar){
		for(final String targetKind : grammar.recordKinds()){
			final StructType targetType = grammar.recordType(targetKind);
			if(targetType == null)
				continue;

			for(final String fullPath : XrefPathIndex.getPaths(targetType.getName())){
				final int separator = fullPath.indexOf('.');
				if(separator < 0)
					continue;

				final String ownerKind = fullPath.substring(0, separator);
				pathsByKind.computeIfAbsent(ownerKind, k -> new ArrayList<>())
					.add(new XrefPath(fullPath.substring(separator + 1), targetKind));
			}
		}
	}


	List<XrefPath> pathsOf(final String ownerKind){
		return pathsByKind.getOrDefault(ownerKind, List.of());
	}

	void forEachReference(final FLEFRecord owner, final ReferenceVisitor visitor){
		for(final XrefPath xrefPath : pathsOf(owner.getTag())){
			final String path = xrefPath.path();
			final int separator = path.lastIndexOf('.');
			final List<FLEFRecord> parents = (separator < 0
				? List.of(owner)
				: FLEFRecordHelper.findChildren(owner, path.substring(0, separator)));
			final String tag = StringUtils.removeEnd(path.substring(separator + 1), WILDCARD);
			for(final FLEFRecord parent : parents)
				for(final FLEFRecord node : parent.getChildren())
					if(tag.equals(node.getTag()) && node.getValue() != null)
						visitor.visit(owner, parent, node, path, xrefPath.targetKind());
		}
	}

	void forEachReference(final Collection<FLEFRecord> owners, final ReferenceVisitor visitor){
		for(final FLEFRecord owner : owners)
			forEachReference(owner, visitor);
	}

	/**
	 * Redirects every reference, and every {@code flef:} link in notes, whose id is a key of {@code mapping} to the mapped id.
	 *
	 * @param records the records to update in place
	 * @param mapping old id to new id
	 * @return the number of rewritten references and links
	 */
	int rewrite(final Collection<FLEFRecord> records, final Map<String, String> mapping){
		if(mapping.isEmpty())
			return 0;

		final int[] count = {0};
		forEachReference(records, (owner, parent, node, path, targetKind) -> {
			final String newId = mapping.get(node.getValue());
			if(newId != null){
				node.setValue(newId);
				count[0] ++;
			}
		});
		for(final FLEFRecord record : records)
			count[0] += rewriteNoteLinks(record, mapping);
		return count[0];
	}

	private static int rewriteNoteLinks(final FLEFRecord node, final Map<String, String> mapping){
		int count = 0;
		if(TAG_NOTE.equals(node.getTag())){
			count += rewriteText(FLEFRecordHelper.findChildren(node, TAG_TEXT), mapping);
			for(final FLEFRecord translation : FLEFRecordHelper.findChildren(node, TAG_TRANSLATION))
				count += rewriteText(FLEFRecordHelper.findChildren(translation, TAG_TEXT), mapping);
		}
		for(final FLEFRecord child : node.getChildren())
			count += rewriteNoteLinks(child, mapping);
		return count;
	}

	private static int rewriteText(final List<FLEFRecord> textNodes, final Map<String, String> mapping){
		int count = 0;
		for(final FLEFRecord textNode : textNodes){
			final String text = textNode.getValue();
			if(text == null || !text.contains(FLEFModel.FLEF_LINK_PREFIX))
				continue;

			final Matcher matcher = FLEF_LINK.matcher(text);
			final StringBuilder sb = new StringBuilder(text.length());
			boolean changed = false;
			while(matcher.find()){
				final String newId = mapping.get(matcher.group(1));
				if(newId != null){
					matcher.appendReplacement(sb, Matcher.quoteReplacement(FLEFModel.FLEF_LINK_PREFIX + newId));
					changed = true;
					count ++;
				}
			}
			if(changed){
				matcher.appendTail(sb);
				textNode.setValue(sb.toString());
			}
		}
		return count;
	}

}
