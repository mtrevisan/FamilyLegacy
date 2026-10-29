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
package io.github.mtrevisan.familylegacy.ui.assistant.context;

import io.github.mtrevisan.familylegacy.ui.assistant.chronology.DateRange;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * What the Context Assistant needs to know: the events of the tree, the historic events and the cultural norms of the model, the
 * impacts that already link them, and where the places are.
 *
 * @param events       the events of the tree
 * @param contexts     the historic events and the cultural norms
 * @param impacts      the impacts that already exist
 * @param ancestors    for each place, the places that contain it (by the relationships of territory)
 * @param placeLabels  the name of each place
 */
record ContextData(List<Event> events, List<Context> contexts, List<Impact> impacts, Map<String, Set<String>> ancestors,
		Map<String, String> placeLabels){

	/**
	 * An event of the tree.
	 *
	 * @param id         the identifier
	 * @param type       the type
	 * @param range      the days it may have happened in ({@code null} if it has no date, or no bounded one)
	 * @param placeId    the place ({@code null} if none)
	 * @param label      what to call it ("birth of Anna Rossi (1850)")
	 * @param principals the individuals it is about
	 */
	record Event(String id, String type, DateRange range, String placeId, String label, List<String> principals){}

	/**
	 * A historic event or a cultural norm.
	 *
	 * @param id       the identifier
	 * @param historic whether it is a historic event (otherwise it is a cultural norm)
	 * @param type     the type ({@code null} if none)
	 * @param title    the title, or the identifier
	 * @param range    the days it was in effect ({@code null} if it has no date; unbounded on a side the norm has no limit on)
	 * @param placeId  the place ({@code null} if none)
	 */
	record Context(String id, boolean historic, String type, String title, DateRange range, String placeId){}

	/**
	 * An impact that already exists.
	 *
	 * @param contextId the historic event or the cultural norm
	 * @param targetId  the record it influences
	 */
	record Impact(String contextId, String targetId){}

}
