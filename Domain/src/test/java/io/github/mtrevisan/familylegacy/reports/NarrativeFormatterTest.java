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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.reports.i18n.NarrativeFormatter;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLabels;
import io.github.mtrevisan.familylegacy.reports.i18n.ReportLanguage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** A missing date, place or cause leaves no gap in the sentence. */
class NarrativeFormatterTest{

	private static final NarrativeFormatter FORMATTER = new NarrativeFormatter(new ReportLabels(ReportLanguage.ENGLISH));


	@Test
	void everyFragmentIsThere(){
		Assertions.assertEquals("Anna was born on 1 JAN 1900 in Treviso.", FORMATTER.birth("Anna", "1 JAN 1900", "Treviso"));
	}

	@Test
	void aMissingDate(){
		Assertions.assertEquals("Anna was born in Treviso.", FORMATTER.birth("Anna", null, "Treviso"));
		Assertions.assertEquals("Anna was buried in Treviso.", FORMATTER.burial("Anna", null, "Treviso"));
	}

	@Test
	void aMissingPlace(){
		Assertions.assertEquals("Anna was born on 1 JAN 1900.", FORMATTER.birth("Anna", "1 JAN 1900", null));
	}

	@Test
	void everythingMissing(){
		Assertions.assertEquals("Anna was born.", FORMATTER.birth("Anna", null, null));
		Assertions.assertEquals("Anna died.", FORMATTER.death("Anna", null, null, null));
	}

	@Test
	void aMissingCause(){
		Assertions.assertEquals("Anna died in Treviso.", FORMATTER.death("Anna", null, "Treviso", null));
	}

	@Test
	void aDivorceWithAndWithoutDate(){
		Assertions.assertEquals("Anna and Luigi divorced on 1 JAN 1900.", FORMATTER.divorce("Anna", "Luigi", "1 JAN 1900"));
		Assertions.assertEquals("Anna and Luigi divorced.", FORMATTER.divorce("Anna", "Luigi", null));
	}

}
