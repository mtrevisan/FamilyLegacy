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
import io.github.mtrevisan.familylegacy.preferences.MediaPathDisplay;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
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
 * <p>
 * A file that is there is also read for its first bytes, to see that it is what the record says: an image that is a PDF (or a PDF that
 * is an image, whatever its extension) is a warning too. Only the formats that the first bytes tell with certainty are judged; a file of
 * another format, or one that cannot be read, is left alone. The time the looking took is logged (at info level when it took 2 s or
 * more, as on a network drive), so that the cost is known and not guessed.
 * </p>
 */
public final class MediaFiles{

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaFiles.class);

	/** The time from which the looking for the files is worth an info message in the log, in milliseconds. */
	private static final long SLOW_MILLIS = 2_000L;
	/** The bytes of the beginning of a file that tell its format. */
	private static final int SIGNATURE_BYTES = 16;

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
		final long start = System.nanoTime();
		int done = 0;
		for(final FLEFRecord media : records){
			if(Thread.currentThread().isInterrupted())
				break;

			done ++;
			if(done % PROGRESS_FILES == 0)
				progress.accept((double)done / records.size());

			final String location = MediaReader.extractUri(media);
			final Path path = (StringUtils.isBlank(location)? null: localPath(location.strip(), baseDirectory));
			if(path == null)
				continue;

			if(!Files.exists(path))
				issues.add(new ConsistencyService.Issue(ConsistencyService.Severity.WARNING, media.getId(),
					I18N.tf("text.consistency.issue.media_missing", media.getId(), MediaPathDisplay.display(path.toString())), "media_missing"));
			else{
				final String actual = detectKind(path);
				final String declared = declaredKind(media, location);
				if(actual != null && !actual.equals(declared))
					issues.add(new ConsistencyService.Issue(ConsistencyService.Severity.WARNING, media.getId(),
						I18N.tf("text.consistency.issue.media_wrong_type", media.getId(), actual, declared,
						MediaPathDisplay.display(path.toString())), "media_wrong_type"));
			}
		}
		final long elapsed = (System.nanoTime() - start) / 1_000_000L;
		if(elapsed >= SLOW_MILLIS)
			LOGGER.info("Looked for the files of {} media in {} ms", records.size(), elapsed);
		else
			LOGGER.debug("Looked for the files of {} media in {} ms", records.size(), elapsed);
		return issues;
	}

	/** @return	The kind that the record declares: the one it says, or, if it says none, the one of the extension of its location. */
	private static String declaredKind(final FLEFRecord media, final String location){
		final String kind = MediaReader.extractKind(media);
		return (StringUtils.isNotBlank(kind)? kind: MediaReader.inferKind(location));
	}

	/**
	 * The kind of a file by its first bytes: {@link MediaReader#ENUM_KIND_IMAGE} for PNG, JPEG, GIF, TIFF and WebP,
	 * {@link MediaReader#ENUM_KIND_DOCUMENT} for PDF, {@link MediaReader#ENUM_KIND_AUDIO} for WAVE, FLAC and MP3 with a tag,
	 * {@link MediaReader#ENUM_KIND_VIDEO} for AVI, Matroska/WebM and QuickTime. The formats that the beginning does not tell with
	 * certainty (an Ogg or an MP4 file can be audio or video) are not judged.
	 *
	 * @param path	The file.
	 * @return	The kind, {@code null} if the file is not of a format known here, is not a regular file or cannot be read.
	 */
	static String detectKind(final Path path){
		final byte[] head = new byte[SIGNATURE_BYTES];
		final int read;
		try{
			if(!Files.isRegularFile(path))
				return null;

			try(final InputStream in = Files.newInputStream(path)){
				read = in.readNBytes(head, 0, head.length);
			}
		}
		catch(final IOException | RuntimeException ignored){
			return null;
		}
		return kindOfSignature(head, read);
	}

	/** @return	The kind that the first bytes of a file tell, {@code null} if they do not tell it with certainty. */
	static String kindOfSignature(final byte[] head, final int length){
		if(startsWith(head, length, 0, "%PDF"))
			return MediaReader.ENUM_KIND_DOCUMENT;
		if(startsWith(head, length, 0, 0x89, 'P', 'N', 'G') || startsWith(head, length, 0, 0xFF, 0xD8, 0xFF)
				|| startsWith(head, length, 0, "GIF8") || startsWith(head, length, 0, 'I', 'I', '*', 0x00)
				|| startsWith(head, length, 0, 'M', 'M', 0x00, '*'))
			return MediaReader.ENUM_KIND_IMAGE;
		if(startsWith(head, length, 0, "RIFF")){
			if(startsWith(head, length, 8, "WEBP"))
				return MediaReader.ENUM_KIND_IMAGE;
			if(startsWith(head, length, 8, "WAVE"))
				return MediaReader.ENUM_KIND_AUDIO;
			if(startsWith(head, length, 8, "AVI "))
				return MediaReader.ENUM_KIND_VIDEO;
			return null;
		}
		if(startsWith(head, length, 0, "ID3") || startsWith(head, length, 0, "fLaC"))
			return MediaReader.ENUM_KIND_AUDIO;
		if(startsWith(head, length, 0, 0x1A, 0x45, 0xDF, 0xA3))
			return MediaReader.ENUM_KIND_VIDEO;
		if(startsWith(head, length, 4, "ftyp")){
			if(startsWith(head, length, 8, "M4A "))
				return MediaReader.ENUM_KIND_AUDIO;
			if(startsWith(head, length, 8, "qt  ") || startsWith(head, length, 8, "M4V "))
				return MediaReader.ENUM_KIND_VIDEO;
		}
		return null;
	}

	private static boolean startsWith(final byte[] head, final int length, final int offset, final String text){
		final int[] expected = new int[text.length()];
		for(int i = 0; i < expected.length; i ++)
			expected[i] = text.charAt(i);
		return startsWith(head, length, offset, expected);
	}

	private static boolean startsWith(final byte[] head, final int length, final int offset, final int... expected){
		if(length < offset + expected.length)
			return false;

		for(int i = 0; i < expected.length; i ++)
			if((head[offset + i] & 0xFF) != expected[i])
				return false;
		return true;
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
