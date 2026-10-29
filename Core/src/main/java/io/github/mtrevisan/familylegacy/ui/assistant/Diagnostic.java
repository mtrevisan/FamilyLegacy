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

import java.util.List;


/**
 * A passive finding shown by the Research Assistant next to the actions (e.g. a child born before the parent).
 * <p>
 * A diagnostic only reports and explains: it never changes the data.
 * </p>
 *
 * @param severity     how serious the finding is
 * @param category     the group of the finding
 * @param title        a short (localized) description
 * @param explanation  the (localized) reasoning behind the finding
 * @param recordIds    the identifiers of the records the finding is about, to find them
 * @param action       what the user can do about the finding (can be {@code null}), run only when the user asks for it
 * @param key          what identifies the finding, the same in every language and until the data it is about changes: a finding
 *                     with a key can be ignored by the user, and it comes back if the key changes (can be {@code null})
 * @param alternatives other things the user can do about the finding, after {@code action} (e.g. compare two records side by side)
 * @param rank         how much the finding matters among the ones of the same severity: the higher, the earlier it is listed
 */
public record Diagnostic(Severity severity, AssistantCategory category, String title, String explanation,
		List<String> recordIds, DiagnosticAction action, String key, List<DiagnosticAction> alternatives, double rank){

	public Diagnostic{
		recordIds = List.copyOf(recordIds);
		alternatives = List.copyOf(alternatives);
	}

	/** A finding that can be ignored (it has a key), and acted on. */
	public Diagnostic(final Severity severity, final AssistantCategory category, final String title, final String explanation,
			final List<String> recordIds, final DiagnosticAction action, final String key){
		this(severity, category, title, explanation, recordIds, action, key, List.of(), 0.);
	}

	/** A finding that can be acted on, and cannot be ignored. */
	public Diagnostic(final Severity severity, final AssistantCategory category, final String title, final String explanation,
			final List<String> recordIds, final DiagnosticAction action){
		this(severity, category, title, explanation, recordIds, action, null);
	}

	/** A finding without an action. */
	public Diagnostic(final Severity severity, final AssistantCategory category, final String title, final String explanation,
			final List<String> recordIds){
		this(severity, category, title, explanation, recordIds, null, null);
	}

	/** A finding that is not about particular records. */
	public Diagnostic(final Severity severity, final AssistantCategory category, final String title, final String explanation){
		this(severity, category, title, explanation, List.of(), null, null);
	}

	/** @return the same finding with an action */
	public Diagnostic withAction(final DiagnosticAction newAction){
		return new Diagnostic(severity, category, title, explanation, recordIds, newAction, key, alternatives, rank);
	}

	/** @return the same finding with other actions, after the first one */
	public Diagnostic withAlternatives(final List<DiagnosticAction> newAlternatives){
		return new Diagnostic(severity, category, title, explanation, recordIds, action, key, newAlternatives, rank);
	}

	/** @return the same finding with another rank */
	public Diagnostic withRank(final double newRank){
		return new Diagnostic(severity, category, title, explanation, recordIds, action, key, alternatives, newRank);
	}

	/** @return whether the user can ignore the finding */
	public boolean isIgnorable(){
		return (key != null);
	}

	/** How serious a finding is, from the most to the least serious. */
	public enum Severity{
		ERROR,
		WARNING,
		INFORMATION
	}

}
