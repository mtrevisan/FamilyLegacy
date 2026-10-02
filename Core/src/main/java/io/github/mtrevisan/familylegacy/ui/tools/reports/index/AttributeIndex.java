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
package io.github.mtrevisan.familylegacy.ui.tools.reports.index;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;


public final class AttributeIndex{

	private final Map<String, List<FLEFRecord>> personToAttrsMap = new HashMap<>();

	public AttributeIndex(final FLEFModel model, final Predicate<FLEFRecord> filter){
		if(model == null)
			return;

		final List<FLEFRecord> attributes = model.getRecordsByType(IndividualAttributeHandler.TYPE);
		for(int i = 0, size = attributes.size(); i < size; i ++){
			final FLEFRecord attr = attributes.get(i);
			if(!filter.test(attr))
				continue;

			final String indId = FLEFRecordHelper.getChildValue(attr, IndividualHandler.TYPE);
			if(indId != null){
				personToAttrsMap.computeIfAbsent(indId, k -> new ArrayList<>()).add(attr);
			}
		}
	}

	public List<FLEFRecord> attributesOf(final FLEFRecord person){
		if(person == null || person.getId() == null)
			return Collections.emptyList();
		return personToAttrsMap.getOrDefault(person.getId(), Collections.emptyList());
	}

}
