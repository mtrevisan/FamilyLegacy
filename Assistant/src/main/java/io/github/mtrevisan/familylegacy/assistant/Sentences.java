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

import io.github.mtrevisan.familylegacy.i18n.I18N;

import java.util.Collection;


/**
 * The few words that join the pieces of a text of the assistant: the space between two sentences, the separator of a list. They are
 * messages and not characters of the code, so that a language can write them its own way (a list of the Japanese is not separated by a
 * comma and a space, and a Thai sentence is not followed by a space).
 * <p>
 * Whatever is not written in a message must be neutral to the language: an identifier in square brackets is, a sentence is not.
 * </p>
 */
public final class Sentences{

	private Sentences(){}


	/** @return two sentences, one after the other */
	public static String join(final String first, final String second){
		return I18N.tf("assistant.sentences.join", first, second);
	}

	/** @return a title and the text that goes with it */
	public static String titled(final String title, final String text){
		return I18N.tf("assistant.sentences.titled", title, text);
	}

	/** @return the items of a list, written in a line (names, types, identifiers) */
	public static String list(final Collection<String> items){
		return String.join(I18N.t("assistant.list.separator"), items);
	}

	/** @return the items of a list whose items are long or have a comma of their own (what is alike, what differs) */
	public static String items(final Collection<String> items){
		return String.join(I18N.t("assistant.list.separator.long"), items);
	}

	/** @return some parts of a line of status, one after the other */
	public static String status(final Collection<String> parts){
		return String.join(I18N.t("assistant.list.separator.status"), parts);
	}

}
