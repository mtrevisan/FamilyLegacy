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
package io.github.mtrevisan.familylegacy.quality;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;


/**
 * The media whose file is not where the record says. It reads the filesystem, so it is slow on a big file and on a network drive: it is
 * called away from the event dispatch thread, with the rest of the validation.
 * <p>
 * Only what can be said is said: a remote location (a web address) is not looked for, a relative path is looked for under the base
 * directory (the one the user chose or the folder of the open file) and, with no base directory, it is not judged. A missing file is a
 * warning: the record is right, it is the disk that may have changed.
 * </p>
 */
public final class MediaFiles{

	private static final String URL_SEPARATOR = "://";
	/** The files looked for between two reports of the progress. */
	private static final int PROGRESS_FILES = 20;
	private static final String FILE_SCHEME = "file";


	private MediaFiles(){}


	/**
	 * @param model	The model.
	 * @param baseDirectory	The directory that a relative path is relative to ({@code null} if there is none: the relative paths are then
	 * 	not judged).
	 * @return	A warning for each media whose file was not found, in the order of the model.
	 */
	public static List<ConsistencyService.Issue> missing(final FLEFModel model, final String baseDirectory){
		return missing(model, baseDirectory, fraction -> {});
	}

	/** The same, telling how far it got through the media (0 to 1): looking for a file on a network drive can take long. */
	public static List<ConsistencyService.Issue> missing(final FLEFModel model, final String baseDirectory,
			final DoubleConsumer progress){
		final List<ConsistencyService.Issue> issues = new ArrayList<>();
		final List<FLEFRecord> records = model.getRecordsByType(MediaReader.TYPE);
		int done = 0;
		for(final FLEFRecord media : records){
			if(Thread.currentThread().isInterrupted())
				break;

			done ++;
			if(done % PROGRESS_FILES == 0)
				progress.accept((double)done / records.size());

			final String location = MediaReader.extractUri(media);
			final Path path = (StringUtils.isBlank(location)? null: localPath(location.strip(), baseDirectory));
			if(path != null && !Files.exists(path))
				issues.add(new ConsistencyService.Issue(ConsistencyService.Severity.WARNING, media.getId(),
					I18N.tf("text.consistency.issue.media_missing", media.getId(), path.toString()), "media_missing"));
		}
		return issues;
	}

	/** @return	Where the file should be, {@code null} if it cannot be said (remote, malformed, or relative with no base directory). */
	public static Path localPath(final String location, final String baseDirectory){
		try{
			final Path path;
			if(Strings.CI.startsWith(location, FILE_SCHEME + ":"))
				path = Path.of(URI.create(location));
			else if(location.contains(URL_SEPARATOR))
				return null;
			else
				path = Path.of(location);

			if(path.isAbsolute())
				return path;

			return (StringUtils.isBlank(baseDirectory)? null: Path.of(baseDirectory).resolve(path).normalize());
		}
		catch(final IllegalArgumentException | FileSystemNotFoundException ignored){
			//an InvalidPathException is an IllegalArgumentException
			//not a location that can be looked for
			return null;
		}
	}

}
