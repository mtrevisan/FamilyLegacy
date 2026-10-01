/**
 * Copyright (c) 2026 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */


module io.github.mtrevisan.familylegacy.core {
	requires java.base; requires org.apache.commons.lang3;
	requires java.desktop;
	requires com.miglayout.swing;
	requires org.slf4j;
	requires org.apache.commons.text;
	requires com.ibm.icu;
	requires org.apache.commons.io;
	requires org.apache.tika.core;
	requires java.net.http;
	requires java.prefs;
	requires org.jxmapviewer.jxmapviewer2;
	requires com.github.librepdf.openpdf;
	requires net.coobird.thumbnailator;
	requires org.apache.pdfbox;
	requires org.threeten.extra;
	requires org.apache.poi.poi;
	requires org.apache.poi.ooxml;

	// Export packages needed by external modules/plugins
	exports io.github.mtrevisan.familylegacy.v2.services;

	// Declare SPI consumption
	uses io.github.mtrevisan.familylegacy.v2.services.AstronomicalEngine;
}