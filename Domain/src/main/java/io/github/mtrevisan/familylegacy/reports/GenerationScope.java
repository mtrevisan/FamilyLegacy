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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/** Which generations of an ancestor or descendant report have a chapter, and in which order. */
final class GenerationScope{

	private GenerationScope(){}


	/**
	 * @param levels	The generations, the first with the subject of the report.
	 * @param config	The options of the report: the first generation that has a chapter and the order of the generations.
	 * @return	The persons of the generations that have a chapter, in the order of the chapters.
	 */
	static List<FLEFRecord> select(final List<List<FLEFRecord>> levels, final ReportConfig config){
		final List<List<FLEFRecord>> chosen = new ArrayList<>(levels.subList(Math.min(config.firstGeneration(), levels.size()), levels.size()));
		if(config.generationOrder() == GenerationOrder.FARTHEST_FIRST)
			Collections.reverse(chosen);

		final List<FLEFRecord> out = new ArrayList<>();
		for(final List<FLEFRecord> level : chosen)
			out.addAll(level);
		return out;
	}

}
