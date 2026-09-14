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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.dossier.names;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


/**
 * A single name of an entity, decomposed into its parts.
 * <p>
 * For an individual, this corresponds to a FLEF {@code PersonalNameStructure}
 * and carries the ordered list of parts. For a group, place, source or
 * other entity using the generic {@code NameStructure}, the parts list is
 * empty and the whole name is in the {@code value} field.
 * <p>
 * The record is immutable. Empty fields are normalized to empty strings
 * and empty collections.
 *
 * @param type          the name type (official, birth, married, alias, …);
 *                      may be empty
 * @param locale        the locale code; may be empty
 * @param value         the whole textual value for generic names; empty
 *                      for personal names
 * @param parts         the ordered parts of a personal name; empty for
 *                      generic names
 * @param variants      variants attached to the whole name (generic names
 *                      only; personal names carry variants on their parts)
 * @param culturalNorms references to cultural norms governing the name
 * @param sourceRecord  the backing FLEF record, used for editing
 */
public record NameAnatomy(
	String type,
	String locale,
	String value,
	List<NamePart> parts,
	List<NameVariant> variants,
	List<String> culturalNorms,
	FLEFRecord sourceRecord
){

	public NameAnatomy{
		if(type == null)
			type = StringUtils.EMPTY;
		if(locale == null)
			locale = StringUtils.EMPTY;
		if(value == null)
			value = StringUtils.EMPTY;
		parts = (parts != null? List.copyOf(parts): List.of());
		variants = (variants != null? List.copyOf(variants): List.of());
		culturalNorms = (culturalNorms != null? List.copyOf(culturalNorms): List.of());
	}


	/**
	 * Returns whether this name has parts (i.e. it is a personal name).
	 *
	 * @return {@code true} if the parts list is not empty
	 */
	public boolean hasParts(){
		return !parts.isEmpty();
	}

	/**
	 * Returns whether this name carries variants on the whole name.
	 *
	 * @return {@code true} if the variants list is not empty
	 */
	public boolean hasVariants(){
		return !variants.isEmpty();
	}

	/**
	 * Returns whether this name has cultural norms attached.
	 *
	 * @return {@code true} if the cultural norms list is not empty
	 */
	public boolean hasCulturalNorms(){
		return !culturalNorms.isEmpty();
	}

	/**
	 * Returns the display label of the name type, with underscores replaced
	 * by spaces. Falls back to {@code "name"} when the type is empty.
	 *
	 * @return the display label, never {@code null}
	 */
	public String displayType(){
		return (type.isEmpty()? "name": type.replace('_', ' '));
	}

	/**
	 * Returns a compact textual representation obtained by joining the
	 * parts (or the whole value for generic names). Used for fallback
	 * display and for logging.
	 *
	 * @return the joined text, never {@code null}
	 */
	public String joinedText(){
		if(hasParts()){
			final StringBuilder sb = new StringBuilder();
			for(final NamePart part : parts){
				if(sb.length() > 0)
					sb.append(' ');
				sb.append(part.value());
			}
			return sb.toString();
		}
		return value;
	}

}
