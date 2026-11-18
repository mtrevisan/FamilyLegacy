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

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;


/**
 * Extracts the XML of a Gramps file.
 * <p>
 * A {@code .gramps} file is an XML document, usually compressed with gzip; a {@code .gpkg} file is a tar archive (itself usually
 * compressed with gzip) that holds the XML as {@code data.gramps}, together with the media files.
 */
public final class GrampsPackageReader{

	private static final int GZIP_MAGIC_FIRST = 0x1F;
	private static final int GZIP_MAGIC_SECOND = 0x8B;
	private static final int TAR_BLOCK = 512;
	private static final int TAR_NAME_LENGTH = 100;
	private static final int TAR_SIZE_OFFSET = 124;
	private static final int TAR_SIZE_LENGTH = 12;
	private static final int TAR_TYPE_OFFSET = 156;
	private static final int TAR_MAGIC_OFFSET = 257;
	private static final String TAR_MAGIC = "ustar";
	private static final int TAR_PREFIX_OFFSET = 345;
	private static final int TAR_PREFIX_LENGTH = 155;
	private static final byte TYPE_GNU_LONG_NAME = 'L';
	private static final byte TYPE_PAX_HEADER = 'x';
	private static final String PACKAGE_DATA_NAME = "data.gramps";
	/** The biggest content that is read, once decompressed (2 GiB less a bit, the limit of an array). */
	private static final int MAX_SIZE = Integer.MAX_VALUE - 8;


	private GrampsPackageReader(){}


	/**
	 * @param in	The content of a {@code .gramps} or {@code .gpkg} file.
	 * @return	The XML document.
	 * @throws IOException	If the content cannot be read, or the package has no data.
	 */
	public static byte[] readXml(final InputStream in) throws IOException{
		return readXml(in, null);
	}

	/**
	 * @param in	The content of a {@code .gramps} or {@code .gpkg} file.
	 * @param mediaDirectory	Where the media files of a package are extracted ({@code null} to not extract them). Existing files of
	 * 	the same size are left alone; the entries that would end outside the directory are skipped.
	 * @return	The XML document.
	 * @throws IOException	If the content cannot be read, or the package has no data.
	 */
	public static byte[] readXml(final InputStream in, final Path mediaDirectory) throws IOException{
		final byte[] content = gunzip(in.readAllBytes());
		if(!isTar(content))
			return content;

		final byte[] data = findEntry(content, PACKAGE_DATA_NAME);
		if(data == null)
			throw new IOException("The package has no " + PACKAGE_DATA_NAME);

		final byte[] xml = gunzip(data);
		//what is not an XML document leaves no file behind
		if(!looksLikeXml(xml))
			throw new IOException("The package has no valid " + PACKAGE_DATA_NAME);

		if(mediaDirectory != null)
			extractMedia(content, mediaDirectory);
		return xml;
	}

	private static boolean looksLikeXml(final byte[] content){
		int i = 0;
		//the byte order mark of UTF-8
		if(content.length >= 3 && (content[0] & 0xFF) == 0xEF && (content[1] & 0xFF) == 0xBB && (content[2] & 0xFF) == 0xBF)
			i = 3;
		while(i < content.length && Character.isWhitespace(content[i]))
			i ++;
		return (i < content.length && content[i] == '<');
	}

	/** An entry of the archive: its name, the type, and where the content is. */
	private record Entry(String name, byte type, int start, int size){}

	/** Walks the entries of the archive, with the names that the long-name extensions (GNU and pax) and the ustar prefix give. */
	private static List<Entry> entries(final byte[] archive) throws IOException{
		final List<Entry> entries = new ArrayList<>();
		String longName = null;
		int position = 0;
		while(position + TAR_BLOCK <= archive.length){
			final String shortName = cString(archive, position, TAR_NAME_LENGTH);
			if(shortName.isEmpty())
				break;

			final long declared = octal(cString(archive, position + TAR_SIZE_OFFSET, TAR_SIZE_LENGTH));
			final int start = position + TAR_BLOCK;
			if(declared > archive.length - (long)start)
				throw new IOException("The package is truncated");

			final int size = (int)declared;
			final byte type = archive[position + TAR_TYPE_OFFSET];
			if(type == TYPE_GNU_LONG_NAME)
				longName = cString(archive, start, size);
			else if(type == TYPE_PAX_HEADER){
				final String path = paxPath(archive, start, size);
				if(path != null)
					longName = path;
			}
			else{
				String name = shortName;
				if(longName != null)
					name = longName;
				else{
					final String prefix = cString(archive, position + TAR_PREFIX_OFFSET, TAR_PREFIX_LENGTH);
					if(!prefix.isEmpty() && TAR_MAGIC.equals(new String(archive, position + TAR_MAGIC_OFFSET, TAR_MAGIC.length(),
							StandardCharsets.US_ASCII)))
						name = prefix + "/" + shortName;
				}
				entries.add(new Entry(name, type, start, size));
				longName = null;
			}

			//the content is padded to a whole block
			final long next = start + ((size + (long)TAR_BLOCK - 1) / TAR_BLOCK) * TAR_BLOCK;
			if(next > archive.length)
				break;

			position = (int)next;
		}
		return entries;
	}

	/** @return	The {@code path} record of a pax header ({@code length key=value\n}), {@code null} if it has none. */
	private static String paxPath(final byte[] archive, final int start, final int size){
		final String text = new String(archive, start, size, StandardCharsets.UTF_8);
		for(final String line : StringUtils.split(text, '\n')){
			final int space = line.indexOf(' ');
			if(space > 0 && line.startsWith("path=", space + 1))
				return line.substring(space + 1 + "path=".length());
		}
		return null;
	}

	private static boolean isData(final String entryName){
		return (PACKAGE_DATA_NAME.equals(entryName) || entryName.endsWith("/" + PACKAGE_DATA_NAME));
	}

	/** Extracts every regular file of the archive, but the data, into the directory. */
	private static void extractMedia(final byte[] archive, final Path directory) throws IOException{
		final Path root = directory.toAbsolutePath()
			.normalize();
		for(final Entry entry : entries(archive)){
			if((entry.type() != '0' && entry.type() != 0) || isData(entry.name()))
				continue;

			final Path target;
			try{
				target = root.resolve(entry.name())
					.normalize();
			}
			catch(final InvalidPathException ignored){
				//a name that this system cannot have
				continue;
			}
			if(target.startsWith(root) && !target.equals(root)
					&& !(Files.isRegularFile(target) && Files.size(target) == entry.size())){
				Files.createDirectories(target.getParent());
				Files.write(target, Arrays.copyOfRange(archive, entry.start(), entry.start() + entry.size()));
			}
		}
	}

	/** Decompresses the content while it is compressed (a file can be compressed twice, as the package and its data). */
	private static byte[] gunzip(final byte[] content) throws IOException{
		byte[] current = content;
		while(current.length > 2 && (current[0] & 0xFF) == GZIP_MAGIC_FIRST && (current[1] & 0xFF) == GZIP_MAGIC_SECOND)
			try(final InputStream gzip = new GZIPInputStream(new ByteArrayInputStream(current))){
				//a bounded read, so that a bomb does not fill the memory
				current = gzip.readNBytes(MAX_SIZE + 1);
				if(current.length > MAX_SIZE)
					throw new IOException("The file is too big");
			}
		return current;
	}

	private static boolean isTar(final byte[] content){
		return content.length >= TAR_BLOCK && TAR_MAGIC.equals(new String(content, TAR_MAGIC_OFFSET, TAR_MAGIC.length(),
			StandardCharsets.US_ASCII));
	}

	/** @return	The content of the entry of the archive with the name (in any directory), or {@code null} if there is none. */
	private static byte[] findEntry(final byte[] archive, final String name) throws IOException{
		for(final Entry entry : entries(archive))
			if(entry.name().equals(name) || entry.name().endsWith("/" + name))
				return Arrays.copyOfRange(archive, entry.start(), entry.start() + entry.size());
		return null;
	}

	private static String cString(final byte[] content, final int offset, final int length){
		int end = offset;
		while(end < offset + length && content[end] != 0)
			end ++;
		return new String(content, offset, end - offset, StandardCharsets.UTF_8).trim();
	}

	private static long octal(final String text) throws IOException{
		try{
			return (text.isEmpty()? 0: Long.parseLong(text, 8));
		}
		catch(final NumberFormatException nfe){
			throw new IOException("The package is not valid", nfe);
		}
	}

}
