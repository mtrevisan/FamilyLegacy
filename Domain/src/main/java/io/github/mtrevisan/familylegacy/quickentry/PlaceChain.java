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
package io.github.mtrevisan.familylegacy.quickentry;

import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.record.RecordLookup;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * A place typed as a chain of names, the way genealogical places are written: the place, then the places that contain it, from the
 * nearest to the largest ({@code Padova, Veneto, Italy}). Each name is a place of its own, part of the next one (an administrative
 * jurisdiction), so that the places are not one name that nothing links to the others.
 * <p>
 * The chain is read here; which places of the file are the ones meant is for the user to say (see {@link QuickEntryService.PlaceRef}).
 */
public final class PlaceChain{

	private static final char SEPARATOR = ',';


	private PlaceChain(){}


	/**
	 * @param text	The place, as typed.
	 * @return	The names of the chain, the place first and the largest place last (blank names are left out; empty if there are none).
	 */
	public static List<String> names(final String text){
		final List<String> names = new ArrayList<>();
		if(text != null)
			for(final String name : StringUtils.split(text, SEPARATOR)){
				final String normalized = StringUtils.normalizeSpace(name);
				if(!normalized.isEmpty())
					names.add(normalized);
			}
		return names;
	}

	/**
	 * Finds the places of the file with exactly the name (ignoring case, accents and extra spaces), for a name of a chain.
	 *
	 * @param model	The model.
	 * @param name	The name.
	 * @param parentId	The place that has to contain them (a place is part of another through a place relationship, whatever its type),
	 * 	{@code null} for the places that are not required to be part of any (the largest place of a chain).
	 * @return	The places, in the order of the file.
	 */
	public static List<FLEFRecord> candidates(final RecordLookup model, final String name, final String parentId){
		final String key = NameLookup.fold(name);
		final List<FLEFRecord> found = new ArrayList<>();
		if(key.isEmpty())
			return found;

		final Set<String> parts = (parentId != null? partsOf(model, parentId): Set.of());
		for(final FLEFRecord place : model.getRecordsByType(PlaceReader.TYPE)){
			if(parentId != null && !parts.contains(place.getId()))
				continue;

			for(final FLEFRecord placeName : FLEFRecordHelper.findChildren(place, PlaceReader.TAG_NAME))
				if(key.equals(NameLookup.fold(FLEFRecordHelper.getChildValue(placeName, PlaceReader.TAG_NAME_VALUE)))){
					found.add(place);

					break;
				}
		}
		return found;
	}

	/** @return	The ids of the places that are part of the place (directly). */
	private static Set<String> partsOf(final RecordLookup model, final String parentId){
		final Set<String> parts = new HashSet<>();
		for(final FLEFRecord relationship : model.getRecordsByType(PlaceRelationshipReader.TYPE))
			if(parentId.equals(relationship.extractReferencedId(PlaceRelationshipReader.TAG_TARGET, PlaceReader.TYPE))){
				final String subject = relationship.extractReferencedId(PlaceRelationshipReader.TAG_SUBJECT, PlaceReader.TYPE);
				if(subject != null)
					parts.add(subject);
			}
		return parts;
	}

}
