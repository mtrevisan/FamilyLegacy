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
package io.github.mtrevisan.familylegacy.io.merger;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;


/**
 * The records the protocol rules of merge and deletion act upon: the session of an automatic merge, or a live model edited by the
 * user.
 */
interface RecordStore{

	Collection<FLEFRecord> records();

	FLEFRecord record(String id);

	/** Removes a record, without cascading. */
	void delete(String id);

	/**
	 * Removes the absorbed records and redirects every reference to them to their survivor.
	 *
	 * @param merges absorbed id to survivor id
	 * @return the number of redirected references
	 */
	int absorb(Map<String, String> merges);


	static RecordStore of(final FLEFModel model, final ReferenceRewriter references){
		return new RecordStore(){
			@Override
			public Collection<FLEFRecord> records(){
				return model.getRecords();
			}

			@Override
			public FLEFRecord record(final String id){
				return model.getRecordById(id);
			}

			@Override
			public void delete(final String id){
				model.removeRecord(id);
			}

			@Override
			public int absorb(final Map<String, String> merges){
				for(final String absorbedId : merges.keySet())
					model.removeRecord(absorbedId);
				return references.rewrite(new ArrayList<>(model.getRecords()), merges);
			}
		};
	}

}
