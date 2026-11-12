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
package io.github.mtrevisan.familylegacy.gedcom;

import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMNode;
import io.github.mtrevisan.familylegacy.importer.gedcom.GEDCOMParser;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


/** A GEDCOM with quotes and backslashes becomes a FLEF file that reads back to the same values (the writer decides what is a text block). */
class GEDCOMQuotesRoundTripTest{

	private static void values(final FLEFRecord record, final List<String> found){
		if(record.getValue() != null)
			found.add(record.getValue());
		for(final FLEFRecord child : record.getChildren())
			values(child, found);
	}

	private static List<String> values(final FLEFModel model){
		final List<String> found = new ArrayList<>();
		for(final FLEFRecord record : model.getRecords())
			values(record, found);
		return found;
	}


	@Test
	void quotesAndBackslashesSurviveTheConversionAndTheWriting() throws IOException{
		final String gedcom = "0 HEAD\n1 CHAR UTF-8\n"
			+ "0 @I1@ INDI\n1 NAME Mario \"Gigi\" /Rossi/\n"
			+ "1 NOTE He said \"hi\" and \"\"\"bye\"\"\" in C:\\temp\\new\n"
			+ "2 CONT second \"line\"\n"
			+ "2 CONT \"\"\"\n"
			+ "1 OCCU \"Farmer\"\n"
			+ "0 TRLR\n";
		final List<GEDCOMNode> roots = new GEDCOMParser().parse(new StringReader(gedcom));
		final FLEFModel model = new GEDCOMToFLEFConverter().convert(roots);

		final String text = FLEFWriter.createCompact().writeToString(model);
		final FLEFModel reread = new FLEFParser().parse(text);

		final List<String> before = values(model);
		Assertions.assertTrue(before.stream().anyMatch(value -> value.contains("\"")), before.toString());
		Assertions.assertEquals(before, values(reread), text);
	}

}
