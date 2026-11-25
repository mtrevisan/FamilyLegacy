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
package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.RecordLookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Two biological parents of the same child are of different sexes: this finds who would break that.
 * <p>
 * Only {@code biological_child} links are considered: a plain {@code child} link does not say whether the parentage is biological, so nothing
 * can be said about it and every sex is accepted. Other kinds of parentage (adoptive, foster, step, ...) and the unions
 * (marriage, civil union, ...) are never considered.
 */
public final class CoParentSex{

	/**
	 * Two parents of the same sex of the same child.
	 *
	 * @param individualId	The individual that was asked about (or the first parent, when scanning a whole model).
	 * @param coParentId	The other parent.
	 * @param childId	The common child.
	 * @param certain	Whether both links are {@code biological_child} (otherwise at least one is a plain {@code child}).
	 */
	public record Clash(String individualId, String coParentId, String childId, boolean certain){}

	private record Link(String parentId, boolean certain){}


	private CoParentSex(){}


	/**
	 * @param individualId	The identifier of the individual whose sex is going to be {@code sex}.
	 * @param sex	The sex (only {@link SexType#MALE} and {@link SexType#FEMALE} can clash).
	 * @param model	Where the relationships and the sexes are looked up.
	 * @return	A biological co-parent (of a common child) that already has the same sex, a certain clash before a possible one,
	 * {@code null} if there is none.
	 */
	public static Clash findClashingCoParent(final String individualId, final SexType sex, final RecordLookup model){
		if(individualId == null || model == null || sex == null || sex == SexType.UNKNOWN)
			return null;

		Clash possible = null;
		for(final Map.Entry<String, List<Link>> entry : linksByChild(model, null).entrySet()){
			final List<Link> links = entry.getValue();
			Link own = null;
			for(final Link link : links)
				if(individualId.equals(link.parentId)){
					own = link;
					break;
				}
			if(own == null)
				continue;

			for(final Link other : links){
				if(other.parentId.equals(individualId) || PreferredSex.of(model.getRecordById(other.parentId), model) != sex)
					continue;

				final Clash clash = new Clash(individualId, other.parentId, entry.getKey(), (own.certain && other.certain));
				if(clash.certain)
					return clash;
				possible = clash;
			}
		}
		return possible;
	}

	/**
	 * @param childId	The child.
	 * @param parentId	The parent of the link that is going to be created.
	 * @param type	The type of the link (only {@code child} and {@code biological_child} are considered).
	 * @param excludedRelationshipId	The relationship being edited, whose current state does not count (may be {@code null}).
	 * @param model	Where the relationships and the sexes are looked up.
	 * @return	Another biological parent of the child with the same sex as the new parent, a certain clash before a possible one,
	 * {@code null} if there is none.
	 */
	public static Clash findClashingParent(final String childId, final String parentId, final String type,
			final String excludedRelationshipId, final RecordLookup model){
		if(childId == null || parentId == null || model == null || !isBiological(type))
			return null;

		final SexType sex = PreferredSex.of(model.getRecordById(parentId), model);
		if(sex == SexType.UNKNOWN)
			return null;

		final boolean certain = RelationshipReader.isTypeBiologicalChild(type);
		Clash possible = null;
		final List<Link> links = linksByChild(model, excludedRelationshipId).get(childId);
		if(links != null)
			for(final Link other : links){
				if(other.parentId.equals(parentId) || PreferredSex.of(model.getRecordById(other.parentId), model) != sex)
					continue;

				final Clash clash = new Clash(parentId, other.parentId, childId, (certain && other.certain));
				if(clash.certain)
					return clash;
				possible = clash;
			}
		return possible;
	}

	/**
	 * For two links that are going to be created together (a new child with both parents).
	 *
	 * @return	The clash if the two parents have the same sex and both links are biological (certain or not), {@code null} otherwise.
	 */
	public static Clash findClashBetween(final String childId, final String parentId, final String parentType,
			final String otherParentId, final String otherParentType, final RecordLookup model){
		if(parentId == null || otherParentId == null || parentId.equals(otherParentId) || model == null
				|| !isBiological(parentType) || !isBiological(otherParentType))
			return null;

		final SexType sex = PreferredSex.of(model.getRecordById(parentId), model);
		if(sex == SexType.UNKNOWN || sex != PreferredSex.of(model.getRecordById(otherParentId), model))
			return null;

		return new Clash(parentId, otherParentId, childId,
			(RelationshipReader.isTypeBiologicalChild(parentType) && RelationshipReader.isTypeBiologicalChild(otherParentType)));
	}

	/** @return	All the pairs of biological parents of the same sex, one entry per pair and child. */
	public static List<Clash> findAll(final RecordLookup model){
		final List<Clash> clashes = new ArrayList<>();
		if(model == null)
			return clashes;

		final Map<String, List<FLEFRecord>> sexes = PreferredSex.indexByIndividual(model);
		final Map<String, SexType> sexOf = new HashMap<>();
		for(final Map.Entry<String, List<Link>> entry : linksByChild(model, null).entrySet()){
			final List<Link> links = entry.getValue();
			for(int i = 0, size = links.size(); i < size; i ++){
				final Link first = links.get(i);
				final SexType firstSex = sexOf.computeIfAbsent(first.parentId,
					id -> PreferredSex.of(model.getRecordById(id), sexes, model));
				if(firstSex == SexType.UNKNOWN)
					continue;

				for(int j = i + 1; j < size; j ++){
					final Link second = links.get(j);
					if(!first.parentId.equals(second.parentId) && firstSex == sexOf.computeIfAbsent(second.parentId,
							id -> PreferredSex.of(model.getRecordById(id), sexes, model)))
						clashes.add(new Clash(first.parentId, second.parentId, entry.getKey(), (first.certain && second.certain)));
				}
			}
		}
		return clashes;
	}


	/** @return	The biological links (certain or not) of the model, by child. */
	private static Map<String, List<Link>> linksByChild(final RecordLookup model, final String excludedRelationshipId){
		final Map<String, List<Link>> links = new HashMap<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipReader.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			if(!isBiological(type) || relationship.getId().equals(excludedRelationshipId))
				continue;

			final String childId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE);
			final String parentId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualReader.TYPE);
			if(childId != null && parentId != null)
				links.computeIfAbsent(childId, k -> new ArrayList<>(2))
					.add(new Link(parentId, RelationshipReader.isTypeBiologicalChild(type)));
		}
		return links;
	}

	private static boolean isBiological(final String type){
		return RelationshipReader.isTypeBiologicalChild(type);
	}

}
