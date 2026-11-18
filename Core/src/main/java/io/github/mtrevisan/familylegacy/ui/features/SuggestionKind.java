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
package io.github.mtrevisan.familylegacy.ui.features;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.preferences.UxLevel;

import java.util.Locale;


/**
 * An advanced FLEF concept that the interface may suggest when it becomes useful. Every level receives the suggestion, worded for the
 * level (progressive disclosure): a basic user reads "Possible duplicate person detected", a standard user "Create Identity Hypothesis...",
 * an advanced user the full terminology. The record that is created is the same at every level.
 */
public enum SuggestionKind{

	/** Competing assertions of the same fact: a conclusion says which one the researcher prefers, and why. */
	CONCLUSION("conclusion"),
	/** Two individuals that may be the same person: an identity hypothesis records the reasoning, without merging them. */
	IDENTITY_HYPOTHESIS("identity_hypothesis"),
	/** A date estimated by a convention of the time and place: a cultural norm is what justifies the estimate. */
	CULTURAL_NORM("cultural_norm"),
	/** A place whose jurisdiction changed over time: its timeline shows when it belonged to each area, and a historic event may say why. */
	JURISDICTION_CHANGE("jurisdiction_change"),
	/** An age outside the usual range (a marriage or a parenthood too early or too late): a custom of the time may explain it. */
	AGE_NORM("age_norm"),
	/** A historic event or a norm that may have influenced an event of the tree: a context impact documents it. */
	CONTEXT_IMPACT("context_impact"),
	/** Several deaths in a place in a short time that nothing explains: a historic event (an epidemic, a famine...) may. */
	HISTORIC_EVENT("historic_event"),
	/** Something that is not known yet: a research question keeps track of it. */
	RESEARCH_GAP("research_gap"),
	/** Something to do next: a research task keeps track of it. */
	RESEARCH_TASK("research_task"),
	/** An identity hypothesis that is still open: a conclusion records the decision (same person, or different people). */
	OPEN_HYPOTHESIS("open_hypothesis");


	private final String key;


	SuggestionKind(final String key){
		this.key = key;
	}


	/** @return what was noticed, in the words of the level (localized, short). */
	public String message(final UxLevel level){
		return I18N.t("suggestion." + key + "." + suffix(level));
	}

	/** @return the text of the link that starts what is suggested, in the words of the level (e.g. "Create Conclusion..."). */
	public String actionLabel(final UxLevel level){
		return I18N.t("suggestion.action." + key + "." + suffix(level));
	}

	/** @return what the FLEF record is and does, for the tooltip of the advanced level (localized). */
	public String hint(){
		return I18N.t("suggestion." + key + ".hint");
	}

	private static String suffix(final UxLevel level){
		return level.name().toLowerCase(Locale.ROOT);
	}

}
