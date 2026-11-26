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
package io.github.mtrevisan.familylegacy.backup;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.io.ProgressListener;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.preferences.ProjectMeta;
import io.github.mtrevisan.familylegacy.preferences.TagStore;
import io.github.mtrevisan.familylegacy.util.AtomicFileWriter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;


/**
 * A backup of a project as a single ZIP file: the FLEF file, the folder of the data the application keeps for it (see
 * {@link TagStore#metaDirectory(String)}: tags, bookmarks, ignored findings) and, if asked, the media files.
 * <p>
 * Layout of the archive:
 * <pre>
 * manifest.properties          what the archive is (format, date, name of the FLEF file, whether it has media)
 * name.flef                    the model, as it is in memory (compact, not validated: its errors are kept)
 * name.flef.meta/...           the application data
 * media/N-file.ext             the local media files, if included
 * media-map.properties         the media record id and the entry that holds its file
 * </pre>
 * The FLEF file is written from the model, not copied from the disk, so it is consistent even while the autosave is pending. The media
 * records keep their original location in the archive: they are linked to the files of the archive when it is extracted.
 */
public final class ProjectArchive{

	/** The extension of an archive. */
	public static final String EXTENSION = "zip";

	private static final String MANIFEST = "manifest.properties";
	private static final String MEDIA_MAP = "media-map.properties";
	private static final String MEDIA_FOLDER = "media/";
	private static final String KEY_FLEF = "flef";
	private static final String DEFAULT_NAME = "project";
	private static final String FILE_SCHEME = "file";
	private static final String URL_SEPARATOR = "://";


	/**
	 * @param mediaIncluded	How many media files were put in the archive.
	 * @param mediaMissing	How many media records point to a local file that does not exist (or to no file): they are not in the archive.
	 * @param mediaRemote	How many media records point to a remote address: they are not in the archive either.
	 */
	public record WriteResult(int mediaIncluded, int mediaMissing, int mediaRemote){}

	/**
	 * @param flef	The extracted FLEF file.
	 * @param mediaRestored	How many media records were linked to a file extracted from the archive.
	 */
	public record Extracted(Path flef, int mediaRestored){}


	private ProjectArchive(){}


	/** @return	Whether the file is an archive, by its extension. */
	public static boolean isArchive(final Path file){
		return (file != null && Strings.CI.endsWith(file.getFileName().toString(), "." + EXTENSION));
	}

	/**
	 * Writes the archive, replacing a file of that name only when it is complete (it is written through a temporary file).
	 *
	 * @param zip	The archive to write.
	 * @param model	The model to save.
	 * @param documentPath	The path of the FLEF file of the document, {@code null} if it has none (there is no data folder then).
	 * @param includeMedia	Whether to put the local media files in the archive.
	 */
	public static WriteResult write(final Path zip, final FLEFModel model, final String documentPath, final boolean includeMedia,
			final ProgressListener listener) throws IOException{
		final String name = (documentPath != null? FilenameUtils.getBaseName(documentPath): DEFAULT_NAME);
		final String flefName = name + ".flef";
		final Path temporary = zip.resolveSibling(zip.getFileName() + ".tmp");

		int included = 0;
		int missing = 0;
		int remote = 0;
		try{
			try(final ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(temporary), StandardCharsets.UTF_8)){
				final Properties manifest = new Properties();
				manifest.setProperty("format", "1");
				manifest.setProperty("created", Instant.now().toString());
				manifest.setProperty(KEY_FLEF, flefName);
				manifest.setProperty("media", Boolean.toString(includeMedia));
				put(out, MANIFEST, ProjectMeta.toBytes(manifest, "archive"));

				out.putNextEntry(new ZipEntry(flefName));
				final Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
				FLEFWriter.createCompact().write(model, writer, listener);
				writer.flush();
				out.closeEntry();

				addMetaDirectory(out, documentPath, flefName + TagStore.META_SUFFIX);
				final byte[] tags = TagStore.tagsContent(model.tags());
				if(tags != null)
					put(out, flefName + TagStore.META_SUFFIX + "/" + TagStore.tagsFileName(), tags);

				if(includeMedia){
					final Properties map = new Properties();
					final Map<Path, String> entries = new HashMap<>();
					for(final FLEFRecord media : model.getRecordsByType(MediaReader.TYPE)){
						final String uri = MediaReader.extractUri(media);
						if(isRemote(uri)){
							remote ++;
							continue;
						}
						final Path file = localFile(uri);
						if(file == null || !Files.isRegularFile(file)){
							missing ++;
							continue;
						}

						final Path key = file.toAbsolutePath().normalize();
						String entry = entries.get(key);
						if(entry == null){
							entry = MEDIA_FOLDER + (entries.size() + 1) + "-" + safeName(file.getFileName().toString());
							entries.put(key, entry);
							out.putNextEntry(new ZipEntry(entry));
							Files.copy(file, out);
							out.closeEntry();
							included ++;
						}
						map.setProperty(media.getId(), entry);
					}
					if(!map.isEmpty())
						put(out, MEDIA_MAP, ProjectMeta.toBytes(map, "archive media"));
				}
			}
			Files.move(temporary, zip, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		}
		finally{
			Files.deleteIfExists(temporary);
		}
		return new WriteResult(included, missing, remote);
	}

	/**
	 * Extracts the archive into a folder that does not exist yet (or is empty), and links the media records to the extracted media files.
	 * Nothing is written outside the folder, whatever the names in the archive.
	 *
	 * @return	The extracted FLEF file.
	 * @throws IOException	If the archive is not one of the application, or the folder is not free.
	 */
	public static Extracted extract(final Path zip, final Path folder) throws IOException{
		if(Files.exists(folder)){
			try(final Stream<Path> content = Files.list(folder)){
				if(!Files.isDirectory(folder) || content.findAny().isPresent())
					throw new IOException("The folder is not empty: " + folder);
			}
		}

		final Path root = folder.toAbsolutePath().normalize();
		String flefName;
		try(final ZipFile file = new ZipFile(zip.toFile(), StandardCharsets.UTF_8)){
			final ZipEntry manifestEntry = file.getEntry(MANIFEST);
			if(manifestEntry == null)
				throw new IOException("Not an archive of FamilyLegacy: " + zip.getFileName());
			final Properties manifest = new Properties();
			try(final InputStream in = file.getInputStream(manifestEntry)){
				manifest.load(in);
			}
			flefName = manifest.getProperty(KEY_FLEF, StringUtils.EMPTY);
			if(flefName.isEmpty() || !flefName.equals(FilenameUtils.getName(flefName)) || file.getEntry(flefName) == null)
				throw new IOException("The archive has no FLEF file: " + zip.getFileName());

			Files.createDirectories(root);
			for(final ZipEntry entry : file.stream().toList()){
				if(entry.isDirectory())
					continue;
				final Path target = root.resolve(entry.getName()).normalize();
				if(!target.startsWith(root))
					throw new IOException("Unsafe name in the archive: " + entry.getName());

				Files.createDirectories(target.getParent());
				try(final InputStream in = file.getInputStream(entry)){
					Files.copy(in, target);
				}
			}
		}

		Files.deleteIfExists(root.resolve(MANIFEST));
		final Path flef = root.resolve(flefName);
		return new Extracted(flef, linkMedia(root, flef));
	}


	/** Points the media records to the files extracted next to the FLEF file; the map of the archive is then not needed. */
	private static int linkMedia(final Path root, final Path flef) throws IOException{
		final Path mapFile = root.resolve(MEDIA_MAP);
		if(!Files.exists(mapFile))
			return 0;

		final Properties map = ProjectMeta.read(mapFile);
		final FLEFModel model = new FLEFParser().parse(flef);
		int linked = 0;
		for(final String id : map.stringPropertyNames()){
			final FLEFRecord media = model.getRecordById(id);
			final Path file = root.resolve(map.getProperty(id)).normalize();
			if(media == null || !file.startsWith(root) || !Files.isRegularFile(file))
				continue;

			FLEFRecordHelper.updateChildValue(media, MediaReader.TAG_URI, file.toString());
			linked ++;
		}
		if(linked > 0)
			AtomicFileWriter.write(flef, StandardCharsets.UTF_8, writer -> FLEFWriter.createCompact().write(model, writer));
		Files.delete(mapFile);
		return linked;
	}

	private static void addMetaDirectory(final ZipOutputStream out, final String documentPath, final String entryFolder) throws IOException{
		if(documentPath == null)
			return;

		final Path meta = TagStore.metaDirectory(documentPath);
		if(!Files.isDirectory(meta))
			return;

		final List<Path> files;
		try(final Stream<Path> list = Files.list(meta)){
			files = list.filter(Files::isRegularFile)
				//the tags are written from the model
				.filter(path -> !path.getFileName().toString().equals(TagStore.tagsFileName()))
				.sorted()
				.toList();
		}
		for(final Path file : files){
			out.putNextEntry(new ZipEntry(entryFolder + "/" + file.getFileName()));
			Files.copy(file, out);
			out.closeEntry();
		}
	}

	private static void put(final ZipOutputStream out, final String name, final byte[] content) throws IOException{
		out.putNextEntry(new ZipEntry(name));
		out.write(content);
		out.closeEntry();
	}

	private static boolean isRemote(final String uri){
		return (!StringUtils.isBlank(uri) && uri.contains(URL_SEPARATOR) && !Strings.CI.startsWith(uri, FILE_SCHEME + ":"));
	}

	/** @return	The local file of a media location (a path or a {@code file:} URI), {@code null} if it is not one. */
	private static Path localFile(final String uri){
		if(StringUtils.isBlank(uri))
			return null;

		try{
			return (Strings.CI.startsWith(uri, FILE_SCHEME + ":")? Path.of(URI.create(uri)): Path.of(uri));
		}
		catch(final IllegalArgumentException | FileSystemNotFoundException ignored){
			//not a location of a local file (InvalidPathException is an IllegalArgumentException)
			return null;
		}
	}

	private static String safeName(final String name){
		final StringBuilder sb = new StringBuilder(name.length());
		for(int i = 0; i < name.length(); i ++){
			final char c = name.charAt(i);
			sb.append(c == '/' || c == '\\' || c == ':' || Character.isISOControl(c)? '_': c);
		}
		return sb.toString();
	}

}
