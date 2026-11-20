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
open module io.github.mtrevisan.familylegacy.assistant {
	requires io.github.mtrevisan.familylegacy.domain;
	requires io.github.mtrevisan.calendars;
	requires org.apache.commons.lang3;
	requires org.slf4j;

	exports io.github.mtrevisan.familylegacy.assistant;
	exports io.github.mtrevisan.familylegacy.assistant.chronology;
	exports io.github.mtrevisan.familylegacy.assistant.citation;
	exports io.github.mtrevisan.familylegacy.assistant.conclusion;
	exports io.github.mtrevisan.familylegacy.assistant.consanguinity;
	exports io.github.mtrevisan.familylegacy.assistant.context;
	exports io.github.mtrevisan.familylegacy.assistant.fan;
	exports io.github.mtrevisan.familylegacy.assistant.gap;
	exports io.github.mtrevisan.familylegacy.assistant.identity;
	exports io.github.mtrevisan.familylegacy.assistant.place;
	exports io.github.mtrevisan.familylegacy.assistant.planner;
	exports io.github.mtrevisan.familylegacy.assistant.relationship;
	exports io.github.mtrevisan.familylegacy.assistant.source;

	// The Conflicts report finds the conflicts of the research here
	provides io.github.mtrevisan.familylegacy.reports.ConflictFinder
		with io.github.mtrevisan.familylegacy.assistant.conclusion.ConclusionConflictFinder;
}
