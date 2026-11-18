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
package io.github.mtrevisan.calendars.indian;

import io.github.mtrevisan.calendars.text.TextNormalizer;

import java.util.HashSet;
import java.util.Set;


/**
 * The words that carry the grammar of a Vikram Samvat date (the fortnight, the leap month, the month scheme, the era and
 * the fillers), defined once as normalized sets. "Shuddha" is the non-leap (nija) month, not the bright fortnight.
 */
final class VikramSamvatGrammar{

	static final Set<String> SHUKLA = words("Shukla", "Sukla", "Sudi", "Sud", "Shudi", "Sudhi",
		"शुक्ल", "सुदी", "सुद");
	static final Set<String> KRISHNA = words("Krishna", "Krsna", "Vadi", "Vad", "Badi", "Bahula", "Bahul",
		"कृष्ण", "वदी", "बदी", "वद");
	static final Set<String> ADHIKA = words("Adhika", "Adhik", "Adhic", "Mala", "Purushottam", "Purushottama",
		"अधिक", "मल", "पुरुषोत्तम");
	static final Set<String> NIJA = words("Nija", "Nij", "Shuddha", "निज");
	static final Set<String> AMANTA = words("Amanta", "Amavasyant", "Amavasyanta", "अमान्त");
	static final Set<String> PURNIMANTA = words("Purnimanta", "Purnimant", "Pournimanta", "पूर्णिमान्त");
	static final Set<String> KARTIKADI = words("Kartikadi", "Gujarati", "कार्तिकादि");
	static final Set<String> FILLERS = words("Vikram", "Vikrami", "Vikrama", "Samvat", "Sambat", "Samvata", "VS",
		"V", "S", "Paksha", "Paksh", "Tithi", "Masa", "Maas", "Mas", "Month", "of", "the", "विक्रम", "संवत्", "संवत", "वि",
		"सं", "पक्ष", "तिथि", "मास");

	private static Set<String> words(final String... words){
		final Set<String> set = new HashSet<>();
		for(final String word : words)
			set.add(TextNormalizer.normalize(word));
		return set;
	}


	private VikramSamvatGrammar(){}

}
