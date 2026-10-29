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
package io.github.mtrevisan.familylegacy.ui.assistant;

import java.util.ArrayList;
import java.util.List;


/** What the user can do about a finding, in the order of the keys of the palette. */
public final class DiagnosticActions{

	private DiagnosticActions(){}


	/**
	 * The actions of a finding: the one it carries (e.g. start the draft of a conclusion), the other ones it carries (e.g. compare
	 * two records), then the opening of its records, which every finding about records has. The first one is run with Enter, the
	 * second one with Ctrl+Enter, the third one with Ctrl+Shift+Enter.
	 *
	 * @param diagnostic the finding
	 * @return the actions, the most relevant first (empty if there is nothing to do)
	 */
	public static List<DiagnosticAction> of(final Diagnostic diagnostic){
		final List<DiagnosticAction> actions = new ArrayList<>(3);
		if(diagnostic.action() != null)
			actions.add(diagnostic.action());
		actions.addAll(diagnostic.alternatives());
		if(!diagnostic.recordIds().isEmpty())
			actions.add(new OpenRecordsAction(diagnostic.recordIds()));
		return actions;
	}

}
