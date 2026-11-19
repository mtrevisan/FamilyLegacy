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


/**
 * How the pages of a report are laid out, for the formats that have pages (PDF, Word); the other formats ignore it.
 *
 * @param paper	The size of the paper.
 * @param marginMm	The margin of the page, in millimetres; 0 for the one the format uses (otherwise it is kept between
 * 	{@link #MIN_MARGIN_MM} and {@link #MAX_MARGIN_MM}).
 * @param pageHeadings	Whether the title of the report is repeated at the top of every page but the first.
 * @param footnote	A line of text at the bottom of every page, blank for none.
 * @param coverText	Lines of text under the title of the first page (who prepared the report, for whom...), blank for none.
 */
public record ReportLayout(PaperSize paper, int marginMm, boolean pageHeadings, String footnote, String coverText){

	/** The smallest margin: the page number and the footnote must fit under the text. */
	public static final int MIN_MARGIN_MM = 15;
	public static final int MAX_MARGIN_MM = 40;

	/** What a format does without a layout: A4, its own margins, the title at the top of the pages, no footnote and no cover text. */
	public static final ReportLayout DEFAULT = new ReportLayout(PaperSize.A4, 0, true, "", "");


	/** The sizes of paper. */
	public enum PaperSize{

		A4(210, 297),
		A5(148, 210),
		LETTER(215.9, 279.4);


		private final double widthMm;
		private final double heightMm;


		PaperSize(final double widthMm, final double heightMm){
			this.widthMm = widthMm;
			this.heightMm = heightMm;
		}

		public double widthMm(){
			return widthMm;
		}

		public double heightMm(){
			return heightMm;
		}

		/** @return	The width, in points (1/72 inch). */
		public float widthPoints(){
			return (float)(widthMm / 25.4 * 72);
		}

		public float heightPoints(){
			return (float)(heightMm / 25.4 * 72);
		}

		/** @return	The width, in twips (1/1440 inch). */
		public int widthTwips(){
			return (int)Math.round(widthMm / 25.4 * 1440);
		}

		public int heightTwips(){
			return (int)Math.round(heightMm / 25.4 * 1440);
		}

	}


	public ReportLayout{
		paper = (paper != null? paper: PaperSize.A4);
		marginMm = (marginMm <= 0? 0: Math.clamp(marginMm, MIN_MARGIN_MM, MAX_MARGIN_MM));
		footnote = (footnote != null? footnote.strip(): "");
		coverText = (coverText != null? coverText.strip(): "");
	}

}
