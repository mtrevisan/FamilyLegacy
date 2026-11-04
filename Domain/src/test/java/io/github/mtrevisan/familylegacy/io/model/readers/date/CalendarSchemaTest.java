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
package io.github.mtrevisan.familylegacy.io.model.readers.date;

import io.github.mtrevisan.calendars.CalendarType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Keeps the calendars of the Calendars module aligned with the FLEF schema and the user interface labels.
 */
class CalendarSchemaTest{

	@Test
	void schemaListsEveryCalendar() throws IOException{
		final String schema;
		try(final InputStream is = CalendarSchemaTest.class.getResourceAsStream("/gedg/flef_0.2.0.gedg")){
			Assertions.assertNotNull(is, "schema not found");
			schema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
		final Matcher matcher = Pattern.compile("enum CalendarType \\{([^}]*)}").matcher(schema);
		Assertions.assertTrue(matcher.find(), "CalendarType enum not found in schema");
		final Set<String> schemaCodes = Arrays.stream(matcher.group(1).split(","))
			.map(String::trim)
			.collect(Collectors.toCollection(TreeSet::new));

		Assertions.assertEquals(calendarCodes(), schemaCodes);
	}

	@Test
	void everyCalendarHasALabel() throws IOException{
		final Properties messages = new Properties();
		try(final InputStream is = CalendarSchemaTest.class.getResourceAsStream("/i18n/ui/messages.properties")){
			Assertions.assertNotNull(is, "messages not found");
			messages.load(new InputStreamReader(is, StandardCharsets.UTF_8));
		}

		Assertions.assertAll(calendarCodes().stream()
			.map(code -> () -> Assertions.assertTrue(messages.containsKey("enum.date.calendar." + code), code)));
	}

	private static Set<String> calendarCodes(){
		return Arrays.stream(CalendarType.values())
			.map(CalendarType::getCode)
			.collect(Collectors.toCollection(TreeSet::new));
	}

}
