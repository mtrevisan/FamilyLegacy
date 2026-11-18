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
package io.github.mtrevisan.familylegacy.ui.components.dates;

import io.github.mtrevisan.calendars.assistant.DateSuggestion;
import io.github.mtrevisan.calendars.assistant.Message;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;


/** Translates the messages of the date assistant (the tools only produce keys and arguments) and writes the explanations. */
final class AssistantMessages{

	private static final String CALENDAR_PREFIX = "enum.date.calendar.";


	private AssistantMessages(){}


	/** @return the text of a message in the language of the user */
	static String translate(final Message message){
		if(message.isLiteral())
			return String.valueOf(message.args().getFirst());

		return I18N.tf(message.key(), message.args().stream()
			.map(AssistantMessages::argument)
			.toArray());
	}

	private static Object argument(final Object argument){
		return switch(argument){
			case Message message -> translate(message);
			case Message.Calendar calendar -> calendarName(calendar.code());
			case DayOfWeek day -> day.getDisplayName(TextStyle.FULL, Locale.getDefault());
			case Month month -> month.getDisplayName(TextStyle.FULL, Locale.getDefault());
			default -> argument;
		};
	}

	/** @return the name of a calendar, capitalized as a proper adjective (Julian, Gregorian, Hebrew…) */
	static String calendarName(final String code){
		return StringUtils.capitalize(I18N.t(CALENDAR_PREFIX + code, code));
	}

	/** @return the explanation of a suggestion as plain text, suitable to be copied into notes */
	static String explanation(final DateSuggestion suggestion){
		final StringBuilder sb = new StringBuilder();
		sb.append(translate(suggestion.title())).append("\n\n");
		sb.append(I18N.tf("date_assistant.explanation.meaning", translate(suggestion.meaning()))).append('\n');
		appendList(sb, "date_assistant.explanation.assumptions", suggestion.assumptions(), false);
		appendList(sb, "date_assistant.explanation.steps", suggestion.steps(), true);

		//the result closes the explanation, in the calendar it is written in
		final String calendar = (suggestion.calendarCode() != null? calendarName(suggestion.calendarCode()): null);
		sb.append('\n');
		if(suggestion.normalizedDate() != null)
			sb.append(calendar != null
				? I18N.tf("date_assistant.explanation.normalized_calendar", suggestion.normalizedDate(), calendar)
				: I18N.tf("date_assistant.explanation.normalized", suggestion.normalizedDate())).append('\n');
		else if(calendar != null)
			sb.append(I18N.tf("date_assistant.explanation.calendar", calendar)).append('\n');
		if(suggestion.notBefore() != null)
			sb.append(I18N.tf("date_assistant.explanation.not_before", suggestion.notBefore())).append('\n');
		if(suggestion.notAfter() != null)
			sb.append(I18N.tf("date_assistant.explanation.not_after", suggestion.notAfter())).append('\n');
		return sb.toString().stripTrailing();
	}

	private static void appendList(final StringBuilder sb, final String headerKey, final List<Message> messages,
			final boolean numbered){
		if(messages.isEmpty())
			return;

		sb.append('\n').append(I18N.t(headerKey)).append('\n');
		for(int i = 0; i < messages.size(); i ++){
			sb.append("  ");
			sb.append(numbered? (i + 1) + ". ": "- ");
			sb.append(translate(messages.get(i))).append('\n');
		}
	}

}
