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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.reports.ReportLayout;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/** The panel of the layout of the pages shows the layout it is given and gives back what the user chose. */
class ReportLayoutPanelTest{

	@Test
	void theLayoutIsShownAndGivenBack(){
		final ReportLayout layout = new ReportLayout(ReportLayout.PaperSize.A5, 25, false, "Draft", "Prepared for the family");

		final ReportLayoutPanel panel = new ReportLayoutPanel(layout);

		Assertions.assertEquals(layout, panel.toLayout());
	}

	@Test
	void resettingGoesBackToTheDefaultLayout(){
		final ReportLayoutPanel panel = new ReportLayoutPanel(new ReportLayout(ReportLayout.PaperSize.LETTER, 40, false, "x", "y"));

		panel.reset(ReportLayout.DEFAULT);

		Assertions.assertEquals(ReportLayout.DEFAULT, panel.toLayout());
	}

	@Test
	void aMarginOutsideTheLimitsIsKeptBetweenThem(){
		Assertions.assertEquals(ReportLayout.MIN_MARGIN_MM, new ReportLayout(ReportLayout.PaperSize.A4, 3, true, "", "").marginMm());
		Assertions.assertEquals(ReportLayout.MAX_MARGIN_MM, new ReportLayout(ReportLayout.PaperSize.A4, 300, true, "", "").marginMm());
		Assertions.assertEquals(0, new ReportLayout(ReportLayout.PaperSize.A4, -5, true, "", "").marginMm());
	}

}
