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
package io.github.mtrevisan.reports;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class ReportDocumentTest{

	private static boolean rightToLeft(final String language){
		return new ReportDocument("T", null, List.of(), language, null).isRightToLeft();
	}


	@Test
	void rightToLeftLanguages(){
		for(final String language : List.of("ar", "he", "fa", "ur", "yi", "ps", "sd", "ug", "dv", "ckb", "ar-EG", "pa-Arab"))
			Assertions.assertTrue(rightToLeft(language), language);
		for(final String language : List.of("en", "it", "zh-CN", "he-Latn", "pa"))
			Assertions.assertFalse(rightToLeft(language), language);
		Assertions.assertFalse(rightToLeft(null));
		Assertions.assertFalse(rightToLeft(" "));
	}

	@Test
	void defaults(){
		final ReportDocument doc = new ReportDocument("T", null, List.of(), null, " ");

		Assertions.assertEquals("en", doc.language());
		Assertions.assertEquals("Contents", doc.contentsTitle());
	}

}
