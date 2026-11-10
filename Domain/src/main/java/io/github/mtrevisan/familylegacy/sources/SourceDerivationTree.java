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
package io.github.mtrevisan.familylegacy.sources;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The tree of the derivations of a source: the sources it is made from (and those they are made from, up to the originals), and the
 * sources that are made from it (and those made from them).
 * <p>
 * It follows the {@code derived_from} citations as saved in the model, and stops at a circle already in the file (the circle is
 * shown as a leaf, the consistency check reports it as an error). Only the sources the user can see are in the tree.
 * </p>
 */
public final class SourceDerivationTree{

	/** How many related sources make the tree worth showing. */
	public static final int MIN_RELATED_SOURCES = 3;


	/**
	 * A source in the tree.
	 *
	 * @param id	The identifier of the source.
	 * @param title	The title of the source (the identifier if it has none or is not in the model).
	 * @param locator	The locator of the citation that links it to its parent in the tree, {@code null} if none.
	 * @param contradicts	Whether the citation that links it to its parent says that the derived source does not agree with the original.
	 * @param cycle	Whether the source is already above it in the tree (the derivations are a circle): it has no children.
	 * @param children	The sources that follow it in the tree.
	 */
	public record Node(String id, String title, String locator, boolean contradicts, boolean cycle, List<Node> children){}


	private final Map<String, FLEFRecord> sources;
	/** The sources that are derived from an original, in the order of the model: original id to the derived source and its citation. */
	private final Map<String, List<Link>> derivatives = new LinkedHashMap<>();


	private record Link(String sourceId, FLEFRecord citation){}


	/** A tree of the sources the application shows (the privacy policy of the application hides the others). */
	public SourceDerivationTree(final FLEFModel model){
		this(SourceHelper.listAllSources(model));
	}

	/** A tree of the given sources only (a report has a privacy policy of its own). */
	public SourceDerivationTree(final List<FLEFRecord> visibleSources){
		sources = SourceHelper.indexById(visibleSources);
		for(final FLEFRecord source : sources.values())
			for(final FLEFRecord citation : FLEFRecordHelper.findChildren(source, SourceReader.TAG_DERIVED_FROM)){
				final String originalId = citedId(citation);
				if(originalId != null)
					derivatives.computeIfAbsent(originalId, k -> new ArrayList<>(1))
						.add(new Link(source.getId(), citation));
			}
	}


	/** @return	The sources the source is made from, each with the sources it is made from in turn; empty for an original. */
	public List<Node> ancestors(final String sourceId){
		return expand(sourceId, new LinkedHashSet<>(List.of(sourceId)), true);
	}

	/** @return	The sources that are made from the source, each with the sources made from it in turn; empty if there are none. */
	public List<Node> descendants(final String sourceId){
		return expand(sourceId, new LinkedHashSet<>(List.of(sourceId)), false);
	}

	/** @return	Every source above the source, from the nearest to the originals, without repetitions (the source itself is not in it). */
	public List<String> ancestorIds(final String sourceId){
		final Set<String> ids = new LinkedHashSet<>();
		collect(ancestors(sourceId), ids);
		ids.remove(sourceId);
		return List.copyOf(ids);
	}

	/** @return	The originals of the source: the sources at the top of its chains (no {@code derived_from}), in order, without repetitions. */
	public List<String> originals(final String sourceId){
		final Set<String> ids = new LinkedHashSet<>();
		collectOriginals(ancestors(sourceId), ids);
		return List.copyOf(ids);
	}

	/** @return	How many different sources, other than this one, are in its tree, above or below. */
	public int relatedCount(final String sourceId){
		final Set<String> ids = new HashSet<>();
		collect(ancestors(sourceId), ids);
		collect(descendants(sourceId), ids);
		ids.remove(sourceId);
		return ids.size();
	}

	/** @return	Whether the tree of the source has enough sources to be worth showing ({@link #MIN_RELATED_SOURCES}). */
	public boolean isWorthShowing(final String sourceId){
		return (relatedCount(sourceId) >= MIN_RELATED_SOURCES);
	}


	private List<Node> expand(final String sourceId, final Set<String> path, final boolean up){
		final List<Node> nodes = new ArrayList<>(0);
		final List<Link> links = new ArrayList<>(0);
		if(up){
			final FLEFRecord source = sources.get(sourceId);
			if(source != null)
				for(final FLEFRecord citation : FLEFRecordHelper.findChildren(source, SourceReader.TAG_DERIVED_FROM)){
					final String originalId = citedId(citation);
					if(originalId != null)
						links.add(new Link(originalId, citation));
				}
		}
		else
			links.addAll(derivatives.getOrDefault(sourceId, List.of()));

		for(final Link link : links){
			final boolean cycle = path.contains(link.sourceId());
			List<Node> children = List.of();
			if(!cycle){
				path.add(link.sourceId());
				children = expand(link.sourceId(), path, up);
				path.remove(link.sourceId());
			}
			nodes.add(new Node(link.sourceId(), titleOf(link.sourceId()), locatorOf(link.citation()), contradicts(link.citation()), cycle,
				children));
		}
		return nodes;
	}

	private String titleOf(final String sourceId){
		final FLEFRecord source = sources.get(sourceId);
		final String title = (source != null? SourceHelper.sourceTitle(source): null);
		return (StringUtils.isNotBlank(title)? title: sourceId);
	}

	private static String citedId(final FLEFRecord citation){
		final String id = FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_SOURCE);
		return (StringUtils.isNotBlank(id)? id: null);
	}

	private static String locatorOf(final FLEFRecord citation){
		final String locator = FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_LOCATOR);
		return (StringUtils.isNotBlank(locator)? locator: null);
	}

	private static boolean contradicts(final FLEFRecord citation){
		return SourceCitationReader.STANCE_CONTRADICTS.equalsIgnoreCase(
			FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_STANCE));
	}

	private static void collectOriginals(final List<Node> nodes, final Set<String> ids){
		for(final Node node : nodes)
			if(node.children().isEmpty() && !node.cycle())
				ids.add(node.id());
			else
				collectOriginals(node.children(), ids);
	}

	private static void collect(final List<Node> nodes, final Set<String> ids){
		for(final Node node : nodes){
			ids.add(node.id());
			collect(node.children(), ids);
		}
	}

}
