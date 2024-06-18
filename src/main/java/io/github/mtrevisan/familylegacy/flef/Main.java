/**
 * Copyright (c) 2021 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef;

import io.github.mtrevisan.familylegacy.flef.gedcom.GedcomDataException;
import io.github.mtrevisan.familylegacy.flef.gedcom.GedcomFileParser;
import io.github.mtrevisan.familylegacy.flef.gedcom.GedcomGrammarException;
import io.github.mtrevisan.familylegacy.flef.sql.SQLDataException;
import io.github.mtrevisan.familylegacy.flef.sql.SQLFileParser;
import io.github.mtrevisan.familylegacy.flef.sql.SQLGrammarException;


public class Main{

	public static void main(final String[] args) throws SQLGrammarException, SQLDataException, GedcomGrammarException, GedcomDataException{
		final SQLFileParser sqlParser = new SQLFileParser();
		sqlParser.load("src/main/resources/gedg/treebard/FLeF.sql", "src/main/resources/gedg/treebard/FLeF.data");

		final GedcomFileParser gedcomParser = new GedcomFileParser();
		gedcomParser.load("/gedg/gedcom_5.5.1.tcgb.gedg", "src/main/resources/ged/large.ged");
	}

}
