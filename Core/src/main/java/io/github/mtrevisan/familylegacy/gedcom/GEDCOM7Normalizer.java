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
package io.github.mtrevisan.familylegacy.gedcom;

import io.github.mtrevisan.familylegacy.gedcom.utils.GEDCOMMapper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Rewrites a GEDCOM 7 forest into the syntax read by the converters, that is the one of GEDCOM 5.5.1.
 * <p>
 * Only the syntactic differences are rewritten here; the structures that GEDCOM 7 adds (translations, media types and languages of
 * notes, crops, roles of associates, non-events, creation dates, ...) are read by the converters directly, as a GEDCOM 5.5.1 file never
 * contains them.
 * <ul>
 *   <li>SNOTE (the shared note record, and the pointer to it) → NOTE;</li>
 *   <li>the calendar of a date (e.g. {@code JULIAN 1 JAN 1700}) → calendar escape ({@code @#DJULIAN@ 1 JAN 1700});</li>
 *   <li>DATE with PHRASE → interpreted date ({@code INT <date> (<phrase>)}), or date phrase ({@code (<phrase>)}) when there is no
 *   date;</li>
 *   <li>a missing value, or the enumerated value {@code OTHER}, with PHRASE → the phrase;</li>
 *   <li>a pointer to nothing ({@code @VOID@}): a source citation becomes the citation of an unrecorded source, a repository citation
 *   and an association become notes, any other link is dropped.</li>
 * </ul>
 * The normalization can be repeated on the same forest.
 */
public final class GEDCOM7Normalizer{

	private static final String VOID_POINTER = "@VOID@";
	private static final String ENUM_OTHER = "OTHER";

	/** A calendar of a GEDCOM 7 date, and the name of its GEDCOM 5.5.1 escape. */
	private static final Pattern CALENDAR_KEYWORD = Pattern.compile("(?<=^|\s)(GREGORIAN|JULIAN|HEBREW|FRENCH_R)(?=\s)");


	private GEDCOM7Normalizer(){}


	/** Whether the forest is a GEDCOM 7 one, as declared by HEAD.GEDC.VERS. */
	public static boolean isGEDCOM7(final List<GEDCOMNode> roots){
		for(final GEDCOMNode root : roots)
			if("HEAD".equals(root.getTag())){
				final String version = GEDCOMHelper.findFirstValue(GEDCOMHelper.findFirstChild(root, "GEDC"), "VERS");
				return (version != null && version.startsWith("7"));
			}
		return false;
	}

	public static void normalize(final List<GEDCOMNode> roots){
		for(final GEDCOMNode root : roots)
			normalizeNode(root);
	}

	private static void normalizeNode(final GEDCOMNode node){
		if("SNOTE".equals(node.getTag()))
			node.setTag("NOTE");
		if("DATE".equals(node.getTag()))
			normalizeDate(node);
		else
			applyPhrase(node);

		for(final GEDCOMNode child : node.getChildren())
			normalizeNode(child);

		// once the children are normalized (e.g. the ROLE of an ASSO), resolve the pointers to nothing
		final ListIterator<GEDCOMNode> itr = node.getChildren().listIterator();
		while(itr.hasNext()){
			final GEDCOMNode child = itr.next();
			if(!VOID_POINTER.equals(StringUtils.trim(child.getValue())))
				continue;

			final GEDCOMNode replacement = replaceVoidPointer(child);
			if(replacement != null)
				itr.set(replacement);
			else
				itr.remove();
		}
	}

	/**
	 * Rewrites the calendars of a date as escapes, and its PHRASE as an interpreted date (the PHRASE is removed, to make the
	 * normalization repeatable).
	 */
	private static void normalizeDate(final GEDCOMNode dateNode){
		String date = StringUtils.trimToNull(dateNode.getValue());
		if(date != null){
			final Matcher matcher = CALENDAR_KEYWORD.matcher(date);
			final StringBuilder sb = new StringBuilder();
			while(matcher.find())
				matcher.appendReplacement(sb, "@#D" + matcher.group(1).replace('_', ' ') + "@");
			matcher.appendTail(sb);
			date = sb.toString();
		}

		final GEDCOMNode phraseNode = GEDCOMHelper.findFirstChild(dateNode, "PHRASE");
		final String phrase = (phraseNode != null? StringUtils.trimToNull(phraseNode.getValue()): null);
		if(phrase != null){
			date = (date != null? "INT " + date + " (" + phrase + ")": "(" + phrase + ")");
			dateNode.getChildren().remove(phraseNode);
		}
		dateNode.setValue(date);
	}

	/** Replaces a missing value, or the value {@code OTHER}, with the PHRASE of the node. */
	private static void applyPhrase(final GEDCOMNode node){
		final String phrase = GEDCOMHelper.findFirstValue(node, "PHRASE");
		if(phrase != null && (StringUtils.isBlank(node.getValue()) || ENUM_OTHER.equalsIgnoreCase(node.getValue().trim())))
			node.setValue(phrase);
	}

	/**
	 * Returns what replaces a pointer to nothing.
	 *
	 * @return	The replacement, or {@code null} if the node is to be removed.
	 */
	private static GEDCOMNode replaceVoidPointer(final GEDCOMNode node){
		return switch(node.getTag()){
			// the citation of an unrecorded source
			case "SOUR" -> {
				node.setValue(null);
				yield node;
			}
			case "REPO" -> {
				final List<String> callNumbers = new ArrayList<>();
				for(final GEDCOMNode calnNode : GEDCOMHelper.findChildren(node, "CALN"))
					if(StringUtils.isNotBlank(calnNode.getValue()))
						callNumbers.add(calnNode.getValue().trim());
				yield (!callNumbers.isEmpty()
					? new GEDCOMNode(node.getLevel(), "NOTE", "Call number in an unrecorded repository: "
						+ String.join("; ", callNumbers))
					: null);
			}
			case "ASSO" -> {
				final String associate = GEDCOMHelper.findFirstValue(node, "PHRASE");
				final String role = GEDCOMMapper.mapAssociationRoleLabel(GEDCOMHelper.findFirstValue(node, "ROLE"));
				yield (associate != null || role != null
					? new GEDCOMNode(node.getLevel(), "NOTE", "Associate: " + StringUtils.defaultString(associate, "unknown")
						+ (role != null? " (" + role + ")": StringUtils.EMPTY))
					: null);
			}
			default -> null;
		};
	}

}
