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

import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import java.util.List;


/**
 * How many findings of each severity the Research Assistant has, as the indicator of the main window shows them.
 *
 * @param errors   the errors
 * @param warnings the warnings
 * @param notes    the information notices
 */
public record FindingCounts(int errors, int warnings, int notes){

	/** @return the counts of the findings (the ones the user ignored should not be among them) */
	public static FindingCounts of(final List<Diagnostic> findings){
		int errors = 0;
		int warnings = 0;
		int notes = 0;
		for(final Diagnostic finding : findings)
			switch(finding.severity()){
				case ERROR -> errors ++;
				case WARNING -> warnings ++;
				case INFORMATION -> notes ++;
			}
		return new FindingCounts(errors, warnings, notes);
	}


	public int total(){
		return errors + warnings + notes;
	}

	public boolean isClean(){
		return (total() == 0);
	}

	/** @return the short text of the indicator: the counts that are not zero, each with the symbol of its severity, or a tick */
	public String label(){
		if(isClean())
			return "✓";

		final StringBuilder sb = new StringBuilder();
		append(sb, "✖", errors);
		append(sb, "⚠", warnings);
		append(sb, "ℹ", notes);
		return sb.toString();
	}

	/** @return the counts in words, for the tooltip */
	public String summary(){
		return (isClean()? I18N.t("assistant.indicator.clean"): I18N.tf("assistant.indicator.summary", errors, warnings, notes));
	}


	private static void append(final StringBuilder sb, final String symbol, final int count){
		if(count == 0)
			return;

		if(!sb.isEmpty())
			sb.append("  ");
		sb.append(symbol).append(' ').append(count);
	}

}
