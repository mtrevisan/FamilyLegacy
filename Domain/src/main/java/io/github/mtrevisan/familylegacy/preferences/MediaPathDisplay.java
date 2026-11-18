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
package io.github.mtrevisan.familylegacy.preferences;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;


/**
 * How the location of a media file is shown (a view preference: the stored value is never changed, and the field being
 * edited always shows it whole).
 */
public enum MediaPathDisplay{

	/** The whole location, as stored. */
	FULL,
	/** Only the name of the file. */
	FILE_NAME,
	/**
	 * The path relative to the base directory (the whole path if the file is not under it): the one chosen by the user or,
	 * by default, the directory of the open file.
	 */
	RELATIVE_TO_BASE;


	private static final String URL_SEPARATOR = "://";
	private static final String FILE_SCHEME = "file";


	/** The directory of the open {@code .flef} file, the base when the user did not choose one ({@code null} for an unsaved document). */
	private static volatile String documentDirectory;


	/** Tells which file is open (or {@code null} for an unsaved document): its directory is the default base directory. */
	public static void setDocumentFile(final File file){
		final File parent = (file != null? file.getAbsoluteFile().getParentFile(): null);
		documentDirectory = (parent != null? parent.getPath(): null);
	}

	/** @return	the base directory in use: the one chosen by the user, else the one of the open file, else {@code null}. */
	public static String baseDirectory(){
		return baseDirectory(AppPreferences.getInstance().mediaBaseDirectory());
	}

	public static String baseDirectory(final String chosen){
		return (chosen != null? chosen: documentDirectory);
	}

	/** Formats a media location following the saved preferences. */
	public static String display(final String location){
		return AppPreferences.getInstance().mediaPathDisplay().format(location, baseDirectory());
	}

	/**
	 * @param location	the location of the media, as stored (a path, a {@code file:} URI or a remote URL)
	 * @param baseDirectory	the base directory ({@code null} if not set), used by {@link #RELATIVE_TO_BASE}
	 * @return	the text to show; remote locations are never shortened to a relative path, and anything that cannot be
	 * 	interpreted is returned unchanged
	 */
	public String format(final String location, final String baseDirectory){
		if(this == FULL || StringUtils.isBlank(location))
			return location;

		final boolean remote = (location.contains(URL_SEPARATOR) && !Strings.CI.startsWith(location, FILE_SCHEME + ":"));
		if(this == FILE_NAME){
			final String name = FilenameUtils.getName(localPath(location, remote));
			return (StringUtils.isEmpty(name)? location: name);
		}

		//RELATIVE_TO_BASE
		if(remote || StringUtils.isBlank(baseDirectory))
			return location;
		try{
			final Path path = Path.of(localPath(location, false)).toAbsolutePath().normalize();
			final Path base = Path.of(baseDirectory).toAbsolutePath().normalize();
			if(path.startsWith(base) && !path.equals(base))
				return base.relativize(path).toString();
		}
		catch(final InvalidPathException ignored){
			//not a path: show it as it is
		}
		return location;
	}

	/** Turns a {@code file:} URI into a path, leaving anything else as it is. */
	private static String localPath(final String location, final boolean remote){
		if(!remote && Strings.CI.startsWith(location, FILE_SCHEME + ":")){
			try{
				return Path.of(URI.create(location)).toString();
			}
			catch(final IllegalArgumentException | FileSystemNotFoundException ignored){
				//malformed URI: use it as it is
			}
		}
		return location;
	}

}
