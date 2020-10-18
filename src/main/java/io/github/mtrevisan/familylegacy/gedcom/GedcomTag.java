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

	ABBR, ADDR, ADOP, ADR1, ADR2, ADR3, AGE, AGNC, _AKA, ALIA, ANCI, ASSO, AUTH,
	BIRT, BLOB, BURI,
	CALN, CAUS, CENS, CHAN, CHAR, CHIL, CITY, CONC, CONT, COPR, CORP, CREM, CTRY, _CUT, _CUTD,
	DATA, DATE, _DATE, DEAT, DESC, DESI, DEST, DIV, DSCR,
	EDUC, EMAIL, _EMAIL, EMIG, _EML, ENGA, EVEN,
	FAM, FAMC, FAMS, FAX, _FILE, FILE, FONE, FORM, _FREL,
	GED, GEDC, GIVN, GRAD,
	HEAD, HUSB,
	IMMI, INDI, _ITALIC,
	LANG, LATI, LONG,
	MAP, MARB, MARL, MARR, _MARRNM, _MAR, _MARNM, MEDI, _MREL,
	NAME, _NAME, NAME_TYPE, NATU, NCHI, NICK, NOTE, NPFX, NSFX,
	OBJE, OCCU, ORDI, ORDN,
	PAGE, _PAREN, PEDI, PHON, POST, PLAC, _PREF, _PRIM, _PRIMARY, PROP, PUBL, _PUBL,
	QUAY,
	REFN, RELA, RELI, REPO, RESI, RETI, RFN, RIN, ROLE, ROMN,
	_SCBK, SEX, SOUR, SPFX, _SSHOW, SSN, STAE, STAT, SUBM, SUBN, SURN,
	TEMP, TEXT, TIME, TITL, TRLR, TYPE, _TYPE,
	UID, _UID, _URL,
	VERS,
	WIFE, _WEB, WWW, _WWW,

	//personal LDS ordinances:
	//family LDS ordinances:
	BAPL, CONL, WAC, ENDL, SLGC,
	SLGS

}
