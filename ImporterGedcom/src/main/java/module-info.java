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

/** The importer of GEDCOM files (5.5.x and 7.0). */
open module io.github.mtrevisan.importers.gedcom {
	requires io.github.mtrevisan.familylegacy.domain;
	requires org.apache.commons.lang3;
	requires org.apache.commons.io;
	requires com.ibm.icu;
	requires org.slf4j;

	exports io.github.mtrevisan.familylegacy.importer.gedcom;
	exports io.github.mtrevisan.familylegacy.importer.gedcom.converters;
	exports io.github.mtrevisan.familylegacy.importer.gedcom.fileimporter;

	provides io.github.mtrevisan.familylegacy.importer.FileImporter
		with io.github.mtrevisan.familylegacy.importer.gedcom.fileimporter.GedcomFileImporter;
}
