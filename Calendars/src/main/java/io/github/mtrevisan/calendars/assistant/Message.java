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
package io.github.mtrevisan.calendars.assistant;

import java.util.List;
import java.util.Objects;


/**
 * A text of the assistant, as a key with its arguments: the tools never produce words for the user, the user interface
 * translates the key (and the arguments) into the language of the user.
 * <p>
 * An argument is a {@link String} or a number (shown as they are), a {@link Calendar} (the name of a calendar), a
 * {@link java.time.DayOfWeek} or a {@link java.time.Month} (their names in the language of the user), or another message.
 * </p>
 *
 * @param key  the key of the text, or {@code null} for a text that must be shown as it is (the first and only argument)
 * @param args the arguments of the text
 */
public record Message(String key, List<Object> args){

	/** A calendar, to be shown with its name in the language of the user. */
	public record Calendar(String code){}


	public Message{
		args = List.copyOf(args);
	}


	/** @return the message with the given key and arguments */
	public static Message of(final String key, final Object... args){
		Objects.requireNonNull(key);
		return new Message(key, List.of(args));
	}

	/** @return a message that is shown as it is (a date, a name written by the user, ...) */
	public static Message text(final String text){
		return new Message(null, List.of(text));
	}

	/** @return a calendar to be used as an argument */
	public static Calendar calendar(final String code){
		return new Calendar(code);
	}

	/** @return {@code true} if the message is a text to be shown as it is */
	public boolean isLiteral(){
		return (key == null);
	}

}
