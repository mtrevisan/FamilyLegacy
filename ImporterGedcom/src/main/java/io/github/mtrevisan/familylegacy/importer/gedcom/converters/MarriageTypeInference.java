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

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMHelper;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * The kind of a marriage (civil or religious) when the GEDCOM file says it without the {@code MARR.TYPE}: only what is explicit counts,
 * never a guess.
 * <ul>
 *   <li>a note of the family or of its marriage that says it in words ("religious marriage", "matrimonio civile"...);</li>
 *   <li>a source cited by <em>both</em> spouses for their marriage ({@code EVEN MARR}) whose title ends with the word
 *   {@code religious} or {@code civil} (the way some programs name the record: "marriage Rossi Mario + Bianchi Anna 18500112
 *   religious").</li>
 * </ul>
 * If the evidence is contradictory, or there is none, the kind stays unknown.
 */
final class MarriageTypeInference{

	static final String CIVIL = "civil";
	static final String RELIGIOUS = "religious";

	private static final String[] RELIGIOUS_PHRASES = {"religious marriage", "religious wedding", "church marriage", "church wedding",
		"matrimonio religioso", "matrimonio in chiesa", "rito religioso", "mariage religieux"};
	private static final String[] CIVIL_PHRASES = {"civil marriage", "civil wedding", "civil ceremony", "matrimonio civile", "rito civile",
		"mariage civil"};


	private MarriageTypeInference(){}


	/**
	 * @param famNode      The GEDCOM family.
	 * @param husbandNode  The GEDCOM record of the husband ({@code null} if unknown).
	 * @param wifeNode     The GEDCOM record of the wife ({@code null} if unknown).
	 * @param recordsById  The GEDCOM records, by id.
	 * @return	{@code civil} or {@code religious} if the file says so, {@code null} otherwise.
	 */
	static String infer(final GEDCOMNode famNode, final GEDCOMNode husbandNode, final GEDCOMNode wifeNode,
			final Map<String, GEDCOMNode> recordsById){
		final String fromNotes = fromNotes(famNode, recordsById);
		if(fromNotes != null)
			return ("both".equals(fromNotes)? null: fromNotes);

		return fromSources(husbandNode, wifeNode, recordsById);
	}


	/** @return {@code civil}, {@code religious}, {@code both} (contradictory) or {@code null} */
	private static String fromNotes(final GEDCOMNode famNode, final Map<String, GEDCOMNode> recordsById){
		boolean religious = false;
		boolean civil = false;
		final Deque<GEDCOMNode> nodes = new ArrayDeque<>(famNode.getChildren());
		//the notes of the family and of its events (MARR, MARB, ...)
		while(!nodes.isEmpty()){
			final GEDCOMNode node = nodes.pop();
			if("NOTE".equals(node.getTag())){
				final String text = noteText(node, recordsById).toLowerCase(Locale.ROOT);
				religious |= containsAny(text, RELIGIOUS_PHRASES);
				civil |= containsAny(text, CIVIL_PHRASES);
			}
			else if(node.getLevel() < 2)
				nodes.addAll(node.getChildren());
		}
		return (religious && civil? "both": (religious? RELIGIOUS: (civil? CIVIL: null)));
	}

	private static String noteText(final GEDCOMNode noteNode, final Map<String, GEDCOMNode> recordsById){
		final String pointer = GEDCOMHelper.pointer(noteNode.getValue());
		final GEDCOMNode record = (pointer != null? recordsById.get(pointer): noteNode);
		return (record != null && record.getValue() != null? record.getValue(): "");
	}

	private static boolean containsAny(final String text, final String[] phrases){
		for(final String phrase : phrases)
			if(text.contains(phrase))
				return true;
		return false;
	}


	private static String fromSources(final GEDCOMNode husbandNode, final GEDCOMNode wifeNode, final Map<String, GEDCOMNode> recordsById){
		if(husbandNode == null || wifeNode == null)
			return null;

		final Set<String> shared = marriageSources(husbandNode);
		shared.retainAll(marriageSources(wifeNode));
		boolean religious = false;
		boolean civil = false;
		for(final String sourceId : shared){
			final GEDCOMNode source = recordsById.get(sourceId);
			final String title = (source != null? GEDCOMHelper.findFirstValue(source, "TITL"): null);
			if(title == null)
				continue;

			final String lower = title.trim()
				.toLowerCase(Locale.ROOT);
			religious |= lower.endsWith(StringUtils.SPACE + RELIGIOUS);
			civil |= lower.endsWith(StringUtils.SPACE + CIVIL);
		}
		return (religious == civil? null: (religious? RELIGIOUS: CIVIL));
	}

	/** @return the sources an individual cites for a marriage ({@code EVEN MARR} under the citation), wherever the citation is */
	private static Set<String> marriageSources(final GEDCOMNode individualNode){
		final Set<String> sources = new LinkedHashSet<>();
		final Deque<GEDCOMNode> nodes = new ArrayDeque<>(individualNode.getChildren());
		while(!nodes.isEmpty()){
			final GEDCOMNode node = nodes.pop();
			if("SOUR".equals(node.getTag())){
				final String pointer = GEDCOMHelper.pointer(node.getValue());
				if(pointer != null && "MARR".equalsIgnoreCase(valueOf(GEDCOMHelper.findFirstChild(node, "EVEN"))))
					sources.add(pointer);
				continue;
			}
			nodes.addAll(node.getChildren());
		}
		return sources;
	}

	private static String valueOf(final GEDCOMNode node){
		return (node != null && node.getValue() != null? node.getValue().trim(): "");
	}

}
