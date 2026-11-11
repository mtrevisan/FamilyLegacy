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

import io.github.mtrevisan.familylegacy.preferences.UxLevel;

import java.util.Objects;


/**
 * A suggestion shown inside an editor: what was noticed, and the advanced concept that may help. It only suggests: the action opens the
 * usual editor on a draft, and nothing is created unless the user completes and saves it.
 *
 * @param kind        the advanced concept suggested
 * @param message     what was noticed, in the words of the level of the user (localized, short)
 * @param explanation why it matters (localized), for the tooltip; {@code null} where the level is not told FLEF terms
 * @param action      what to do when the user accepts, on the event dispatch thread
 * @param dismiss     what to do when the user asks not to be told again (it is remembered), or {@code null} if it cannot be remembered
 */
public record Suggestion(SuggestionKind kind, String message, String explanation, Runnable action, Runnable dismiss){

	public Suggestion{
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(action, "action");
	}

	/** A suggestion that cannot be dismissed for good. */
	public Suggestion(final SuggestionKind kind, final String message, final String explanation, final Runnable action){
		this(kind, message, explanation, action, null);
	}

	/** @return the suggestion in the words of the level, that cannot be dismissed for good. */
	public static Suggestion forLevel(final SuggestionKind kind, final UxLevel level, final String explanation, final Runnable action){
		return forLevel(kind, level, explanation, action, null);
	}

	/**
	 * @param kind        the advanced concept suggested
	 * @param level       the level of the user, that decides the words
	 * @param explanation the explanation of the finding, shown (as a tooltip) only to the levels that know the FLEF terms
	 * @param action      what to do when the user accepts
	 * @param dismiss     what to do when the user asks not to be told again
	 * @return the suggestion in the words of the level
	 */
	public static Suggestion forLevel(final SuggestionKind kind, final UxLevel level, final String explanation, final Runnable action,
			final Runnable dismiss){
		final String hint = (level == UxLevel.ADVANCED? kind.hint() + " ": "");
		return new Suggestion(kind, kind.message(level), (level == UxLevel.BASIC? null: hint + explanation), action, dismiss);
	}

}
