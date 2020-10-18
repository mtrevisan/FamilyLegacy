/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.gedcom;


public enum GedcomTag{

	ABBR, ADDR, ADR1, ADR2, ADR3, _AKA, ALIA, ANCI, ASSO, AUTH,
	BLOB, BURI,
	CALN, CAUS, CHAN, CHAR, CHIL, CITY, CONC, CONT, COPR, CORP, CTRY,
	DATA, DATE, DESC, DESI, DEST,
	EMAIL, _EMAIL, _EML,
	FAM, FAMC, FAMS, FAX, _FILE, FILE, FONE, FORM, _FREL,
	GED, GEDC, GIVN,
	HEAD, HUSB,
	INDI, _ITALIC,
	LANG,
	_MARRNM, _MAR, _MARNM, MEDI, _MREL,
	NAME, _NAME, NICK, NOTE, NPFX, NSFX,
	OBJE, ORDI,
	PAGE, _PAREN, PEDI, PHON, POST, PLAC, _PREF, _PRIM, _PRIMARY, PUBL,
	QUAY,
	REFN, RELA, REPO, RFN, RIN, ROMN,
	_SCBK, SOUR, SPFX, _SSHOW, STAE, STAT, SUBM, SUBN, SURN,
	TEMP, TEXT, TIME, TITL, TRLR, TYPE, _TYPE,
	UID, _UID, _URL,
	VERS,
	WIFE, _WEB, WWW, _WWW,

	//personal LDS ordinances:
	//family LDS ordinances:
	BAPL, CONL, WAC, ENDL, SLGC,
	SLGS

}
