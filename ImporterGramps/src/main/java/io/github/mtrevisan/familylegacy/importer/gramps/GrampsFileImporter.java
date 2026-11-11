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
package io.github.mtrevisan.familylegacy.importer.gramps;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.importer.FileImporter;
import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;


/** The importer of Gramps XML files, also when packed in a package ({@code .gpkg}): the XML is read and converted into a FLEF model. */
public final class GrampsFileImporter implements FileImporter{

	@Override
	public String id(){
		return "gramps";
	}

	@Override
	public String menuLabel(){
		return I18N.t("import.format.gramps");
	}

	@Override
	public String dialogTitle(){
		return I18N.t("dialog.import.gramps.title");
	}

	@Override
	public String fileDescription(){
		return I18N.t("import.files.gramps");
	}

	@Override
	public List<String> fileExtensions(){
		return List.of("gramps", "gpkg");
	}

	@Override
	public String invalidFileMessage(){
		return I18N.l("error.import.gramps.invalid");
	}

	@Override
	public FLEFModel read(final Path file, final ProgressListener progress) throws IOException{
		return read(file, progress, new ImportReport());
	}

	@Override
	public FLEFModel read(final Path file, final ProgressListener progress, final ImportReport report) throws IOException{
		progress.onProgress(-1, I18N.t("progress.reading_file"));
		//the media of a package are extracted next to it, in a directory named after it (without the extension)
		final String name = file.getFileName()
			.toString();
		final Path mediaDirectory = (name.toLowerCase(Locale.ROOT).endsWith(".gpkg")
			? file.toAbsolutePath().resolveSibling(name.substring(0, name.length() - ".gpkg".length()) + "_media")
			: null);
		final byte[] grampsXml;
		try(final InputStream in = Files.newInputStream(file)){
			grampsXml = GrampsPackageReader.readXml(in, mediaDirectory);
		}

		progress.onProgress(-1, I18N.t("progress.parsing_gramps"));
		return GrampsXMLToFLEFConverter.convert(grampsXml, progress::onProgress, report, mediaDirectory);
	}

}
