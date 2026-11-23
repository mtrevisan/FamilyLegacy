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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.util.List;
import java.util.ServiceLoader;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;


/**
 * A service that gives the "To Verify" report the findings of the research assistant (research gaps, chronology, identities, the
 * identifiers of the sources): the logic that knows them is in the research assistant, that depends on this module, not the other way
 * round. The texts of the findings are in the language of the application, not in the one of the report.
 */
public interface ToVerifyAssistance{

	/**
	 * It can be slow (the comparison of the individuals takes minutes on a big file).
	 *
	 * @param model	The model to look into.
	 * @param visible	Whether a record can be shown: a finding about a record that is hidden must not be given.
	 * @param progress	Told, from time to time, the share of the work done (0 to 1).
	 * @return	The findings, of the kinds {@link ToVerifyFinder.Kind#RESEARCH_GAP}, {@link ToVerifyFinder.Kind#CHRONOLOGY},
	 * 	{@link ToVerifyFinder.Kind#SOURCE_IDENTIFIER} and {@link ToVerifyFinder.Kind#IDENTITY}.
	 */
	List<ToVerifyFinder.Finding> findings(FLEFModel model, Predicate<FLEFRecord> visible, DoubleConsumer progress);

	/** @return	The assistance the application provides, {@code null} if there is none. */
	static ToVerifyAssistance provided(){
		return Holder.INSTANCE;
	}


	final class Holder{
		private static final ToVerifyAssistance INSTANCE = ServiceLoader.load(ToVerifyAssistance.class).findFirst().orElse(null);

		private Holder(){}
	}

}
