/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.dialogs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;


public class ModifiedRecords{

	private final Map<Integer, Map<String, Object>> upsertedRecords = new LinkedHashMap<>(1);
	private final List<Integer> removedIDs = new ArrayList<>(0);
	private final Set<Integer> ids = new HashSet<>(0);


	public void addModifiedRecord(final Map<String, Object> record){
		if(record != null){
			final Integer recordID = extractRecordID(record);
			upsertedRecords.put(recordID, record);

			//remove every record in `upsertedRecords` that will be deleted
			removedIDs.remove(recordID);
		}
	}

	public void addIDCollection(final Set<Integer> ids){
		this.ids.addAll(ids);
	}

	public void addRemovedRecordID(final int recordID){
		removedIDs.add(recordID);
	}

	public Collection<Map<String, Object>> getUpsertedRecords(){
		return upsertedRecords.values();
	}

	public List<Integer> getRemovedIDs(){
		return removedIDs;
	}

	public Set<Integer> getCollectionIDs(){
		return ids;
	}

	public void clear(){
		upsertedRecords.clear();
		removedIDs.clear();
		ids.clear();
	}

}
