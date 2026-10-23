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
package io.github.mtrevisan.calendars;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class DangiCalendarConverterTest{

	@Test
	void hangulLayout(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 March 1957"),
			CalendarType.DANGI.parseToJdn("단기 4290년 3월 1일"));
	}

	@Test
	void hanjaLayout(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("15 August 1948"),
			CalendarType.DANGI.parseToJdn("檀紀4281年8月15日"));
	}

	@Test
	void romanizedYearAlone(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 January 1961"), CalendarType.DANGI.parseToJdn("Dangi 4294"));
	}

}
