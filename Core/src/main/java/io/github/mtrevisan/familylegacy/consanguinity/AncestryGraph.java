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
package io.github.mtrevisan.familylegacy.consanguinity;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Who is the biological parent of whom: the only links that count for the consanguinity.
 * <p>
 * Adoption, fostering, guardianship and step-parenthood are left out: they say nothing about shared blood. The generic {@code child}
 * link (what an import from GEDCOM gives) is taken as biological, like the tree of the application does. An individual has at most two
 * parents here: with more (a conflict that the Conclusion Assistant reports) the first two in the order of the model are kept, so
 * that the result does not depend on a choice that only the researcher can make.
 * </p>
 * The individuals hidden by the privacy settings are left out, as they are from the views, and so are their links.
 */
public final class AncestryGraph{

	/** The most parents an individual has: a biological child has two. */
	private static final int MAX_PARENTS = 2;

	private final Map<String, List<String>> parents;
	private final Map<String, List<String>> children;


	/**
	 * @param parentsByChild the parents of each child, by the identifier of the child (the individuals that appear only as parents are
	 *                       founders, with no parents of their own)
	 */
	public AncestryGraph(final Map<String, ? extends Collection<String>> parentsByChild){
		final Map<String, List<String>> parentsCopy = new LinkedHashMap<>();
		final Map<String, List<String>> childrenCopy = new LinkedHashMap<>();
		for(final Map.Entry<String, ? extends Collection<String>> entry : parentsByChild.entrySet()){
			final List<String> distinct = new ArrayList<>(new LinkedHashSet<>(entry.getValue()));
			if(distinct.size() > MAX_PARENTS)
				distinct.subList(MAX_PARENTS, distinct.size())
					.clear();
			if(distinct.isEmpty())
				continue;

			parentsCopy.put(entry.getKey(), List.copyOf(distinct));
			for(final String parent : distinct)
				childrenCopy.computeIfAbsent(parent, k -> new ArrayList<>())
					.add(entry.getKey());
		}
		parents = parentsCopy;
		children = childrenCopy;
	}

	/**
	 * Reads the biological links from the model.
	 *
	 * @param model	The model.
	 * @return	The graph, without the individuals that the privacy settings hide.
	 */
	public static AncestryGraph of(final FLEFModel model){
		final Set<String> visible = visibleIndividuals(model);
		final Map<String, List<String>> parentsByChild = new LinkedHashMap<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipReader.TYPE);
		for(final FLEFRecord relationship : relationships){
			if(!isBiologicalChildLink(RelationshipReader.extractType(relationship)))
				continue;

			//the subject is the child and the target the parent
			final String child = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE);
			final String parent = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualReader.TYPE);
			if(child != null && parent != null && visible.contains(child) && visible.contains(parent))
				parentsByChild.computeIfAbsent(child, k -> new ArrayList<>())
					.add(parent);
		}
		return new AncestryGraph(parentsByChild);
	}

	/** @return	The identifiers of the individuals of the model that the privacy settings do not hide. */
	public static Set<String> visibleIndividuals(final FLEFModel model){
		final Set<String> visible = new LinkedHashSet<>();
		final List<FLEFRecord> individuals = model.getRecordsByType(IndividualReader.TYPE);
		for(final FLEFRecord individual : individuals)
			if(individual.getId() != null && !PrivacyService.isHidden(model, individual))
				visible.add(individual.getId());
		return visible;
	}

	/** @return	Whether the type of a child-parent relationship says that the parent is a biological one. */
	public static boolean isBiologicalChildLink(final String type){
		return (RelationshipReader.ENUM_TYPE_CHILD.equals(type) || RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD.equals(type));
	}


	/** @return	The biological parents of the individual (none, one or two), in the order of the model. */
	public List<String> parentsOf(final String id){
		return parents.getOrDefault(id, List.of());
	}

	/** @return	The individuals that have the given one as a biological parent. */
	public List<String> childrenOf(final String id){
		return children.getOrDefault(id, List.of());
	}

	/** @return	The individuals that have two known parents (the only ones that can be inbred). */
	public Set<String> childrenWithTwoParents(){
		final Set<String> result = new LinkedHashSet<>();
		for(final Map.Entry<String, List<String>> entry : parents.entrySet())
			if(entry.getValue().size() == MAX_PARENTS)
				result.add(entry.getKey());
		return Collections.unmodifiableSet(result);
	}

}
