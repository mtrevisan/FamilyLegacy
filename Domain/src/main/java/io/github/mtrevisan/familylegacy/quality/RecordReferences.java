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
package io.github.mtrevisan.familylegacy.quality;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Where a record is used: the records that point to it, and by which field. A reference is a value, in the fields of another record,
 * that is the identifier of the record (a place of an event, the subject of a relationship, a source of a citation...), wherever it is.
 */
public final class RecordReferences{

	/**
	 * @param owner	The record that points to the other one.
	 * @param path	The field of the owner that holds the identifier, from the record down (e.g. {@code place.place}).
	 */
	public record Reference(FLEFRecord owner, String path){}


	private RecordReferences(){}


	/**
	 * @param model	The model.
	 * @param id	The identifier of a record.
	 * @return	The references to the record, in the order of the records (none if nobody points to it).
	 */
	public static List<Reference> to(final FLEFModel model, final String id){
		return to(model, List.of(id)).get(id);
	}

	/**
	 * The same, for several records at once (the file is read once).
	 *
	 * @param model	The model.
	 * @param ids	The identifiers of the records.
	 * @return	The references to each record, in the order of the identifiers (an empty list for the ones nobody points to).
	 */
	public static Map<String, List<Reference>> to(final FLEFModel model, final Collection<String> ids){
		final Map<String, List<Reference>> references = new LinkedHashMap<>();
		for(final String id : ids)
			references.put(id, new ArrayList<>());
		if(model == null || references.isEmpty())
			return references;

		for(final FLEFRecord record : model.getRecords())
			collect(record, references);
		return references;
	}

	/** Reads the fields of a record, without recursion, as the records can be deep. */
	private static void collect(final FLEFRecord owner, final Map<String, List<Reference>> references){
		final Deque<FLEFRecord> nodes = new ArrayDeque<>();
		final Deque<String> paths = new ArrayDeque<>();
		for(final FLEFRecord child : owner.getChildren()){
			nodes.push(child);
			paths.push(child.getTag());
		}
		while(!nodes.isEmpty()){
			final FLEFRecord node = nodes.pop();
			final String path = paths.pop();
			final String value = node.getValue();
			if(value != null){
				final List<Reference> list = references.get(value);
				//a record that points to itself is not a use of it by another record
				if(list != null && !value.equals(owner.getId()))
					list.add(new Reference(owner, path));
			}
			for(final FLEFRecord child : node.getChildren()){
				nodes.push(child);
				paths.push(path + "." + child.getTag());
			}
		}
	}

}
