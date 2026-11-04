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
package io.github.mtrevisan.familylegacy.importer.gedcom;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.importer.FileImporter;
import io.github.mtrevisan.familylegacy.importer.gedcom.converters.GEDCOMToFLEFConverter;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;


/** The importer of GEDCOM files (5.5.x and 7.0): the character set is detected, the file is parsed and converted into a FLEF model. */
public final class GedcomFileImporter implements FileImporter{

	@Override
	public String id(){
		return "gedcom";
	}

	@Override
	public String menuLabel(){
		return I18N.t("import.format.gedcom");
	}

	@Override
	public String dialogTitle(){
		return I18N.t("dialog.import.gedcom.title");
	}

	@Override
	public String fileDescription(){
		return I18N.t("import.files.gedcom");
	}

	@Override
	public List<String> fileExtensions(){
		return List.of("ged");
	}

	@Override
	public String invalidFileMessage(){
		return I18N.l("error.import.gedcom.invalid");
	}

	@Override
	public FLEFModel read(final Path file, final ProgressListener progress) throws IOException{
		progress.onProgress(-1, I18N.t("progress.reading_file"));
		final String gedcomContent;
		try(final InputStream in = Files.newInputStream(file);
				final BufferedReader br = GEDCOMHelper.getBufferedReader(in)){
			gedcomContent = br.lines()
				.collect(Collectors.joining(System.lineSeparator()));
		}

		progress.onProgress(-1, I18N.t("progress.parsing_gedcom"));
		final List<GEDCOMNode> roots;
		try(final StringReader reader = new StringReader(gedcomContent)){
			roots = new GEDCOMParser().parse(reader);
		}

		return new GEDCOMToFLEFConverter()
			.convert(roots, progress::onProgress);
	}

}
