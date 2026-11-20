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
package io.github.mtrevisan.familylegacy.assistant;

import java.util.List;
import java.util.Set;
import java.util.function.DoubleConsumer;


/**
 * A source of diagnostics. It can be slow (it may inspect the whole model), so the Research Assistant calls it away from the
 * event dispatch thread: an implementation must not touch Swing components.
 */
@FunctionalInterface
public interface DiagnosticProvider{

	/**
	 * @param context where the user is
	 * @return the findings relevant in the context
	 */
	List<Diagnostic> diagnose(AssistantContext context);

	/**
	 * The same, telling how far it got. A provider that is slow and can tell overrides it; the others do not, and are taken as done
	 * when they return.
	 *
	 * @param context where the user is
	 * @param progress told, from time to time, the share of the work done (0 to 1); it may be called from the thread of the provider
	 * @return the findings relevant in the context
	 */
	default List<Diagnostic> diagnose(final AssistantContext context, final DoubleConsumer progress){
		return diagnose(context);
	}

	/**
	 * The kinds of record (their tags, e.g. {@code individual}, {@code event}) the findings are computed from. A provider that says
	 * so promises that its findings depend <b>only</b> on the records of these kinds (and on {@link #environment()} and on the date of
	 * today): when none of them changed since the last time, the findings of the last time are given again without computing them.
	 * <p>
	 * It is only an optimization, and a wrong list gives stale findings, so the default is {@code null}: the findings may depend on
	 * anything (the records of other kinds, the header, the record selected by the user), and they are computed every time.
	 * </p>
	 *
	 * @return the tags of the records the findings depend on, or {@code null} if they cannot be told
	 */
	default Set<String> dependsOn(){
		return null;
	}

	/**
	 * What else the findings depend on, besides the records (the settings of the user): the findings of the last time are given again
	 * only if this is equal to what it was then.
	 *
	 * @return the settings the findings depend on ({@code null} if none)
	 */
	default Object environment(){
		return null;
	}

}
