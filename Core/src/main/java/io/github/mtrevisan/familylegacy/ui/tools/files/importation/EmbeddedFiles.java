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
package io.github.mtrevisan.familylegacy.ui.tools.files.importation;

import io.github.mtrevisan.familylegacy.importer.ImportReasons;
import io.github.mtrevisan.familylegacy.importer.ImportReport;
import io.github.mtrevisan.familylegacy.importer.PendingFile;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


/**
 * The files that an import embeds in the imported data (the BLOB of a GEDCOM 5.5 multimedia record): they are written to a folder
 * chosen by the user, and the media records are linked to them. No UI here.
 */
final class EmbeddedFiles{

	private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedFiles.class);


	/** The item of the report entry of an embedded file that was not written. */
	static final String REPORT_ITEM = "BLOB";


	private EmbeddedFiles(){}


	/**
	 * Writes each pending file of the report to the folder, never overwriting a file (a numeric suffix is added to the name), and sets
	 * the uri of its media record to the file. A file that cannot be written is reported as discarded. The pending files are cleared.
	 *
	 * @param model	The imported model.
	 * @return	How many files were written.
	 */
	static int writeAndLink(final FLEFModel model, final ImportReport report, final File folder){
		int written = 0;
		for(final PendingFile pending : report.pendingFiles()){
			final FLEFRecord media = model.getRecordById(pending.mediaId());
			if(media == null){
				report.discard(ImportReasons.MEDIA_DETAIL, REPORT_ITEM, pending.mediaId());
				continue;
			}

			try{
				final File file = uniqueFile(folder, pending.suggestedName());
				Files.write(file.toPath(), pending.data());
				FLEFRecordHelper.updateChildValue(media, MediaReader.TAG_URI, file.getAbsolutePath());
				written ++;
			}
			catch(final IOException | RuntimeException e){
				LOGGER.error("Cannot write the embedded file {}", pending.suggestedName(), e);
				report.discard(ImportReasons.MEDIA_DETAIL, REPORT_ITEM + StringUtils.SPACE + pending.suggestedName(), pending.mediaId());
			}
		}
		report.clearPendingFiles();
		return written;
	}

	/** The user did not choose a folder: the media keep the placeholder uri, and each embedded file is reported as discarded. */
	static void decline(final ImportReport report){
		for(final PendingFile pending : report.pendingFiles())
			report.discard(ImportReasons.MEDIA_DETAIL, REPORT_ITEM + StringUtils.SPACE + pending.suggestedName(), pending.mediaId());
		report.clearPendingFiles();
	}

	/** The file of the given name in the folder, or with a numeric suffix ({@code name-2.ext}, {@code name-3.ext}, ...) if it exists. */
	static File uniqueFile(final File folder, final String name){
		File file = new File(folder, name);
		final String base = FilenameUtils.getBaseName(name);
		final String extension = FilenameUtils.getExtension(name);
		for(int i = 2; file.exists(); i ++)
			file = new File(folder, base + "-" + i + (extension.isEmpty()? "": "." + extension));
		return file;
	}

}
