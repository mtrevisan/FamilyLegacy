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
package io.github.mtrevisan.familylegacy.importer.gedcom;

import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Gives the cross-references of the records of a GEDCOM forest the form of the identifiers of FLEF (uppercase letters followed by
 * digits), and rewrites the pointers to them accordingly.
 * <p>
 * An xref that already has that form is kept, but for the one of a family, {@code F<number>}, that becomes {@code G<number>} (the
 * identifier of a group). Any other xref (a number, mixed case letters, an underscore, ...) is replaced by a new identifier, that never
 * collides with an existing one. Pointers and records are changed together, so that the links survive whatever the generator of the
 * file used as xref.
 * <p>
 * To be called after {@link IDGenerator#reset()}: the identifiers it assigns are registered there.
 */
public final class XrefNormalizer{

	private static final Pattern FAMILY_XREF = Pattern.compile("^F(\\d+)$");
	/** The prefixes of the identifiers of the records of FLEF (the {@code ID_PREFIX} of each reader). */
	private static final List<String> RECORD_PREFIXES = List.of("CC", "CI", "CN", "EP", "E", "GA", "G", "HE", "IH", "IA", "I", "M", "P",
		"PR", "RL", "R", "RA", "RS", "RT", "S");
	private static final String NOTE_PREFIX = "N";
	private static final String SUBMITTER_PREFIX = "SU";
	private static final String OTHER_PREFIX = "X";


	private XrefNormalizer(){}


	public static void normalize(final List<GEDCOMNode> roots){
		//the identifier of each record, in the order of the file
		final Map<GEDCOMNode, String> assigned = new HashMap<>();
		final Map<String, String> renames = new HashMap<>();
		final Set<String> used = new HashSet<>();
		final List<GEDCOMNode> toGenerate = new ArrayList<>();

		for(final GEDCOMNode root : roots){
			final String xref = root.getXrefId();
			if(xref == null)
				continue;

			final String id = keptId(root.getTag(), xref);
			if(id != null && used.add(id)){
				assigned.put(root, id);
				renames.putIfAbsent(xref, id);
				IDGenerator.registerExistingId(id);
			}
			else
				toGenerate.add(root);
		}
		//the pointers to records that are not in the file: no new identifier can be the one they point to
		final Set<String> dangling = new HashSet<>();
		for(final GEDCOMNode root : roots)
			collectPointers(root, dangling);
		dangling.removeAll(renames.keySet());
		for(final GEDCOMNode root : toGenerate){
			final String prefix = prefixOf(root.getTag());
			String id;
			do
				id = IDGenerator.nextId(prefix);
			while(!used.add(id) || dangling.contains(id));
			assigned.put(root, id);
			renames.putIfAbsent(root.getXrefId(), id);
		}

		for(final Map.Entry<GEDCOMNode, String> entry : assigned.entrySet())
			entry.getKey()
				.setXrefId(entry.getValue());
		for(final GEDCOMNode root : roots)
			rewritePointers(root, renames);
	}

	/**
	 * @return	The xref in the form of an identifier (uppercase letters followed by digits) that does not look like the identifier of
	 * 	another kind of record (the application tells the kind of a record from the prefix of its identifier), {@code null} if it has not
	 * 	it.
	 */
	private static String keptId(final String tag, final String xref){
		if(GEDCOMMapper.TAG_FAMILY.equals(tag)){
			final Matcher matcher = FAMILY_XREF.matcher(xref);
			if(matcher.matches())
				return GroupReader.ID_PREFIX + matcher.group(1);
		}
		if(!GEDCOMHelper.isUppercaseLettersThenNumbers(xref))
			return null;
		//a family is a group, and the groups are the records whose identifier starts with G
		if(GEDCOMMapper.TAG_FAMILY.equals(tag) && !xref.startsWith(GroupReader.ID_PREFIX))
			return null;

		int letters = 0;
		while(letters < xref.length() && Character.isLetter(xref.charAt(letters)))
			letters ++;
		final String ownPrefix = prefixOf(tag);
		if(xref.substring(0, letters).equals(ownPrefix))
			return xref;

		for(final String prefix : RECORD_PREFIXES)
			if(!prefix.equals(ownPrefix) && xref.startsWith(prefix))
				return null;
		return xref;
	}

	private static void collectPointers(final GEDCOMNode node, final Set<String> pointers){
		final String target = GEDCOMHelper.pointer(node.getValue());
		if(target != null)
			pointers.add(target);
		for(final GEDCOMNode child : node.getChildren())
			collectPointers(child, pointers);
	}

	private static String prefixOf(final String tag){
		return switch(tag){
			case GEDCOMMapper.TAG_INDIVIDUAL -> IndividualReader.ID_PREFIX;
			case GEDCOMMapper.TAG_FAMILY -> GroupReader.ID_PREFIX;
			case GEDCOMMapper.TAG_SOURCE -> SourceReader.ID_PREFIX;
			case GEDCOMMapper.TAG_REPOSITORY -> RepositoryReader.ID_PREFIX;
			case GEDCOMMapper.TAG_MULTIMEDIA -> MediaReader.ID_PREFIX;
			case GEDCOMMapper.TAG_NOTE, "SNOTE" -> NOTE_PREFIX;
			case GEDCOMMapper.TAG_SUBMITTER -> SUBMITTER_PREFIX;
			default -> OTHER_PREFIX;
		};
	}

	private static void rewritePointers(final GEDCOMNode node, final Map<String, String> renames){
		final String target = GEDCOMHelper.pointer(node.getValue());
		if(target != null){
			final String id = renames.get(target);
			if(id != null && !id.equals(target))
				node.setValue("@" + id + "@");
		}
		for(final GEDCOMNode child : node.getChildren())
			rewritePointers(child, renames);
	}

}
