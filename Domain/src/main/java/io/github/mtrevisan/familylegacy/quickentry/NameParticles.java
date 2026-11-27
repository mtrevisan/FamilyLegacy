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
package io.github.mtrevisan.familylegacy.quickentry;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Set;


/**
 * The words that stand in front of a family name without being part of it: the particles (<i>de</i>, <i>van der</i>, <i>von</i>...), the
 * connectors between two family names (<i>y</i>, <i>e</i>, <i>i</i>), the honorifics that open a name (<i>Dr.</i>) and the generation
 * marks that close it (<i>Jr.</i>, <i>III</i>).
 * <p>
 * The words are compared in lower case and without accents, so that <i>Del</i>, <i>DEL</i> and <i>D&eacute;l</i> are the same word. The
 * lists cover the languages of the genealogical sources most used, in the modern forms and in the historical ones (<i>de la</i>,
 * <i>de li</i>, <i>de'</i> in Italian; <i>van den</i>, <i>vander</i> in Dutch; <i>vom</i>, <i>zur</i> in German...). A word that is also
 * a common given name or family name in a language (<i>Li</i>, <i>An</i>, <i>Don</i>) is either left out, or is a particle only after a
 * given name, when a mistake would hide or misplace a part of the name: the result of the parser is only a proposal, to be checked by
 * the user.
 */
final class NameParticles{

	/** The apostrophes (straight and typographic) that may close a particle written apart from its name ({@code de'}, {@code dell'}). */
	private static final String APOSTROPHES = "'’ʼ‘`";

	/** The particles, as a single word each (a particle of many words, like <i>de la</i> or <i>van der</i>, is a run of them). */
	private static final Set<String> PARTICLES = Set.of(
		//Italian, Latin and the historical Italian forms (de la, de li, de i, de')
		"de", "di", "da", "del", "della", "dello", "dei", "degli", "delle", "deli", "delli", "dal", "dalla", "dallo", "dai", "dagli", "dalle",
		"dela", "lo", "la", "le", "li", "gli", "ab",
		//Spanish, Portuguese, Catalan, Galician
		"los", "las", "el", "do", "dos", "das", "dels", "des",
		//French and Walloon
		"du", "les",
		//Dutch, Flemish, Afrikaans
		"van", "vander", "vanden", "vande", "ver", "ter", "ten", "te", "het", "op", "in", "aan", "bij", "uit", "den", "der",
		//German, Austrian, Swiss
		"von", "vom", "zu", "zum", "zur", "und", "auf", "aus", "am", "im", "vor", "bei", "unter", "dem",
		//Scandinavian
		"af", "av",
		//English and Scots
		"of", "atte", "at", "fitz",
		//Gaelic (Irish and Scottish), Welsh
		"mac", "ni", "nic", "ui", "ua", "mhic", "ap",
		//Arabic, Persian, Hebrew
		"al", "ul", "bin", "ibn", "bint", "ben", "bat", "bar", "ha"
	);

	/**
	 * The particles that are elided, so that they are particles only when they close with an apostrophe (<i>d'</i> <i>Angelo</i>,
	 * <i>dell'</i> <i>Orto</i>, <i>de'</i> <i>Medici</i>): without it, <i>D</i> is the initial of a given name.
	 */
	private static final Set<String> ELIDED_PARTICLES = Set.of("d", "l", "de", "dei", "dell", "dall", "nell", "sull", "all");

	/**
	 * The particles that are also given names or family names (<i>Al Pacino</i>, <i>Li Wei</i>, <i>Mac Miller</i>): they are particles
	 * only if a given name comes before them.
	 */
	private static final Set<String> WEAK_PARTICLES = Set.of(
		"al", "el", "ul", "ben", "bar", "bat", "bin", "ibn", "bint", "ha", "mac", "ni", "nic", "ui", "ua", "mhic", "ap", "ab", "fitz", "of", "at",
		"in", "op", "am", "im", "li", "ten", "te"
	);

	/**
	 * The words that join two family names ({@code Ortega y Gasset}, {@code Pujol i Soley}, {@code Teixeira e Silva}); they are
	 * recognized only in lower case, since an upper case one is the initial of a given name ({@code John E Smith}).
	 */
	private static final Set<String> CONNECTORS = Set.of("y", "e", "i");

	/** The honorifics that open a name (without the final dot). The ones that are also given names (Don, Dom, Lord) are not here. */
	private static final Set<String> HONORIFICS = Set.of(
		"dr", "dott", "dottssa", "prof", "profssa", "ing", "avv", "arch", "geom", "rag", "cav", "sig", "sigra", "signa", "mr", "mrs", "ms",
		"miss", "sir", "rev", "herr", "frau", "mme", "mlle", "mons", "hon"
	);

	/** The generation marks that close a name (case-insensitive, without the final dot). */
	private static final Set<String> GENERATIONS = Set.of("jr", "sr", "junior", "senior", "filho", "neto", "sobrinho");
	/** The generation marks written in Roman numerals (upper case only: {@code Vi} is a name). */
	private static final Set<String> ROMAN_GENERATIONS = Set.of("II", "III", "IV", "VI", "VII", "VIII", "IX");


	private NameParticles(){}


	private static String fold(final String word){
		return StringUtils.stripAccents(word.toLowerCase(Locale.ROOT));
	}

	/** @return	Whether the word is a particle ({@code de}, {@code van}, {@code von}, {@code al}...). */
	static boolean isParticle(final String word){
		final String folded = fold(word);
		//the Dutch article 't, written with its apostrophe
		if(folded.length() == 2 && APOSTROPHES.indexOf(folded.charAt(0)) >= 0 && folded.charAt(1) == 't')
			return true;

		final String bare = StringUtils.stripEnd(folded, APOSTROPHES);
		if(bare.length() != folded.length())
			return (ELIDED_PARTICLES.contains(bare) || PARTICLES.contains(bare));
		return PARTICLES.contains(folded);
	}

	/** @return	Whether the particle is also a given name or a family name, so that it is a particle only after a given name. */
	static boolean isWeakParticle(final String word){
		return WEAK_PARTICLES.contains(fold(word));
	}

	/** @return	Whether the word joins two family names ({@code y}, {@code e}, {@code i}, as written, in lower case). */
	static boolean isConnector(final String word){
		return CONNECTORS.contains(word);
	}

	/** @return	Whether the word is an honorific that opens a name ({@code Dr.}, {@code Prof.}, {@code Sig.}). */
	static boolean isHonorific(final String word){
		return HONORIFICS.contains(StringUtils.stripEnd(fold(word), "."));
	}

	/** @return	Whether the word is a generation mark ({@code Jr.}, {@code Sr.}, {@code III}). */
	static boolean isGeneration(final String word){
		final String stripped = StringUtils.stripEnd(word, ".,");
		return (GENERATIONS.contains(fold(stripped)) || ROMAN_GENERATIONS.contains(stripped));
	}

}
