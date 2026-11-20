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

import java.util.List;
import java.util.ServiceLoader;


/**
 * A service that finds the conflicts of the research, settled or not, for the Conflicts report (the logic that knows them is in the
 * research assistant, that depends on this module, not the other way round).
 */
public interface ConflictFinder{

	/**
	 * @param model	The model to look into.
	 * @return	The conflicts, each with the conclusion that settles it, if any.
	 */
	List<ReportConflict> find(FLEFModel model);

	/** @return	The finder the application provides, {@code null} if there is none. */
	static ConflictFinder provided(){
		return Holder.INSTANCE;
	}


	final class Holder{
		private static final ConflictFinder INSTANCE = ServiceLoader.load(ConflictFinder.class).findFirst().orElse(null);

		private Holder(){}
	}

}
