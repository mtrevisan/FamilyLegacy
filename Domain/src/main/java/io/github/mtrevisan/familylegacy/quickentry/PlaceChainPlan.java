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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.RecordLookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * What a chain of place names means for a file ({@code San Martino, Treviso, Veneto}): for each name, from the largest place to the
 * smallest, whether it is a place of the file (and which, if several have the name) or a new one. It is the preview that tells the user
 * what is going to be created, and the creation of it.
 * <p>
 * The largest names are settled first, each one inside the place chosen for the one before it: once a name is a new place, the ones below
 * it are new too (a new place has nothing inside yet).
 */
public final class PlaceChainPlan{

	/**
	 * @param name	The name, as typed.
	 * @param candidates	The places of the file with that name, inside the place chosen for the name above (empty if there is none, or
	 * 	if a place above is new).
	 * @param choice	The index of the place of the file that is meant, {@code -1} if the name is a new place.
	 */
	public record Level(String name, List<FLEFRecord> candidates, int choice){

		public boolean isNew(){
			return (choice < 0);
		}

		/** @return	The place of the file that is meant, {@code null} if the name is a new place. */
		public FLEFRecord chosen(){
			return (choice >= 0? candidates.get(choice): null);
		}

	}


	private PlaceChainPlan(){}


	/**
	 * @param model	The model.
	 * @param enclosingId	The place that contains the whole chain, {@code null} if the largest name is not inside any.
	 * @param names	The names of the chain, the place first and the largest place last (see {@link PlaceChain#names(String)}).
	 * @param choices	What the user chose for each level, from the largest ({@code -1}: a new place, {@code null} or out of range: the
	 * 	first place of the file with the name, if there is one).
	 * @return	The levels, from the largest to the smallest.
	 */
	public static List<Level> plan(final RecordLookup model, final String enclosingId, final List<String> names,
			final List<Integer> choices){
		final List<Level> levels = new ArrayList<>(names.size());
		String parentId = enclosingId;
		boolean resolving = true;
		for(int i = names.size() - 1; i >= 0; i --){
			final String name = names.get(i);
			final List<FLEFRecord> candidates = (resolving? PlaceChain.candidates(model, name, parentId): List.of());
			int choice = -1;
			if(!candidates.isEmpty()){
				final int position = names.size() - 1 - i;
				final Integer wanted = (position < choices.size()? choices.get(position): null);
				choice = (wanted != null && wanted >= -1 && wanted < candidates.size()? wanted: 0);
			}
			levels.add(new Level(name, candidates, choice));

			if(choice >= 0)
				parentId = candidates.get(choice).getId();
			else
				resolving = false;
		}
		return levels;
	}

	/**
	 * @param enclosingId	The place that contains the whole chain, {@code null} if none.
	 * @param levels	The levels of {@link #plan(RecordLookup, String, List, List)}.
	 * @return	The place the chain means: a place of the file if every name is one, otherwise the new place with the new places that contain
	 * 	it ({@link QuickEntryService.PlaceRef#NONE} if there is no name).
	 */
	public static QuickEntryService.PlaceRef reference(final String enclosingId, final List<Level> levels){
		if(levels.isEmpty())
			return QuickEntryService.PlaceRef.NONE;

		final Level smallest = levels.getLast();
		if(!smallest.isNew())
			return QuickEntryService.PlaceRef.existing(smallest.chosen().getId());

		int firstNew = 0;
		while(!levels.get(firstNew).isNew())
			firstNew ++;
		final String containerId = (firstNew > 0? levels.get(firstNew - 1).chosen().getId(): enclosingId);
		//the new places that contain the smallest, from the nearest to the largest
		final List<String> enclosing = new ArrayList<>();
		for(int i = levels.size() - 2; i >= firstNew; i --)
			enclosing.add(levels.get(i).name());
		return QuickEntryService.PlaceRef.inside(smallest.name(), enclosing, containerId);
	}

	/**
	 * Creates the places that the chain means and are not in the file yet, each part of the next one (an administrative jurisdiction).
	 *
	 * @param model	The model (the new places and their relationships are added to it).
	 * @param reference	The place, see {@link #reference(String, List)}.
	 * @return	The id of the place the chain means, {@code null} if there is none.
	 */
	public static String create(final FLEFModel model, final QuickEntryService.PlaceRef reference){
		final List<FLEFRecord> records = new ArrayList<>();
		final String id = QuickEntryService.resolvePlaceId(model, records, reference, new HashMap<>());
		for(final FLEFRecord record : records)
			model.addRecord(record);
		return id;
	}

}
