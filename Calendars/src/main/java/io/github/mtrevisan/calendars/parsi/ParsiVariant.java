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
package io.github.mtrevisan.calendars.parsi;


/**
 * Reckonings of the Parsi (Yazdegerdi) calendar.
 * <p>
 * Shahenshahi and Kadmi count 365-day years from the Yazdegerdi era (16 June 632 Julian), but the Shahenshahi community
 * intercalated one month once (sometime around the 12th century), so its dates fall 30 days after the Kadmi ones with the
 * same year number. The Fasli reckoning keeps the year aligned with the seasons instead.
 * </p>
 */
public enum ParsiVariant{

	/** Shahenshahi (Shenshai), followed by most Parsis in India. */
	SHAHENSHAHI(30),
	/** Kadmi (Qadimi), the reckoning without intercalation, adopted by part of the Parsis of Surat in 1745. */
	KADMI(0),
	/**
	 * Fasli (Bastani), proposed in 1906 by the Zarathushti Fasili Sal Mandal: the year starts on 21 March (Jamshedi
	 * Navroz), the YZ year being the Gregorian year − 630, and the year containing a Gregorian 29 February has a sixth
	 * Gatha day, Avardad-sal-Gah, on 20 March (the offset is not used).
	 */
	FASLI(0);


	private final int offsetDays;


	ParsiVariant(final int offsetDays){
		this.offsetDays = offsetDays;
	}


	/**
	 * @return the days to add to the uninterrupted Yazdegerdi reckoning
	 */
	public int getOffsetDays(){
		return offsetDays;
	}

}
