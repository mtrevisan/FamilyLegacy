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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.updater.GitHubReleases;
import io.github.mtrevisan.updater.InstallerDownloader;
import io.github.mtrevisan.updater.ReleaseAsset;
import io.github.mtrevisan.updater.Transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * The fonts of the PDF reports that the program does not carry in its jar: they are in the {@code fonts} directory beside the
 * {@code plugins} one, and when they are not there the application can download them from the latest release (the asset
 * {@value #ASSET_NAME}, checked against its published SHA-256). The PDF reports work without them, with fewer scripts.
 */
public final class FontPack{

	/** The name of the file of the release that holds the fonts. */
	public static final String ASSET_NAME = "fonts.zip";
	/** A font of the pack, whose presence tells that the pack is installed. */
	static final String SENTINEL = "NotoSans-Regular.ttf";

	private static final int MAX_FILES = 100;
	private static final long MAX_UNPACKED_BYTES = 256L * 1024 * 1024;


	/** The latest release does not publish the fonts. */
	public static final class NotPublishedException extends IOException{

		public NotPublishedException(){
			super("The latest release does not publish " + ASSET_NAME);
		}

	}


	private FontPack(){}


	/** @return	Whether the fonts are in the directory. */
	public static boolean isInstalled(final Path fontsDirectory){
		return (fontsDirectory != null && Files.isRegularFile(fontsDirectory.resolve(SENTINEL)));
	}

	/**
	 * Downloads the fonts of the latest release of the application into the directory.
	 *
	 * @param fontsDirectory	Where the fonts go (created if needed).
	 * @param progress	Told how far the download is; may be {@code null}.
	 * @param cancelled	Tells whether to give up; may be {@code null}.
	 * @throws NotPublishedException	If the latest release has no fonts (or there is no release).
	 * @throws InstallerDownloader.DownloadCancelledException	If {@code cancelled} says so.
	 * @throws IOException	If the download fails, the file is not the published one, or the fonts cannot be written.
	 */
	public static void download(final Path fontsDirectory, final InstallerDownloader.Progress progress, final BooleanSupplier cancelled)
			throws IOException{
		download(GitHubReleases.application(), Transport.http(), fontsDirectory, progress, cancelled);
	}

	static void download(final GitHubReleases releases, final Transport transport, final Path fontsDirectory,
			final InstallerDownloader.Progress progress, final BooleanSupplier cancelled) throws IOException{
		final ReleaseAsset asset = releases.latestAsset(ASSET_NAME)
			.orElseThrow(NotPublishedException::new);
		if(!asset.isDownloadable())
			throw new IOException("The release does not publish the SHA-256 of " + ASSET_NAME);

		final Path temporary = Files.createTempDirectory("familylegacy-fonts");
		try{
			final Path zip = new InstallerDownloader(transport).download(asset, temporary, progress, cancelled);
			unpack(zip, fontsDirectory);
		}
		finally{
			deleteTree(temporary);
		}
	}

	/**
	 * Unpacks the fonts: only plain files (fonts and licence texts) are accepted, and nothing is written outside the directory; the
	 * files are written all together, or none.
	 */
	static void unpack(final Path zip, final Path fontsDirectory) throws IOException{
		Files.createDirectories(fontsDirectory);
		final Path staging = Files.createTempDirectory(fontsDirectory, ".staging-");
		try{
			final List<Path> files = new ArrayList<>();
			long total = 0;
			try(final InputStream in = Files.newInputStream(zip);
					final ZipInputStream zis = new ZipInputStream(in)){
				ZipEntry entry;
				while((entry = zis.getNextEntry()) != null){
					if(entry.isDirectory())
						continue;

					final String name = entry.getName();
					//(the names come from the network: they must not lead outside the directory)
					if(!isAccepted(name) || files.size() >= MAX_FILES)
						throw new IOException("Unexpected file in " + ASSET_NAME + ": " + name);

					final Path file = staging.resolve(name);
					try(final var out = Files.newOutputStream(file)){
						total += copyLimited(zis, out, MAX_UNPACKED_BYTES - total);
					}
					files.add(file);
				}
			}
			if(files.isEmpty())
				throw new IOException(ASSET_NAME + " is empty");

			for(final Path file : files)
				Files.move(file, fontsDirectory.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
		}
		finally{
			deleteTree(staging);
		}
	}

	private static boolean isAccepted(final String name){
		if(name.isEmpty() || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || name.startsWith(".") || name.indexOf(':') >= 0)
			return false;

		final String lower = name.toLowerCase(Locale.ROOT);
		return (lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".txt"));
	}

	private static long copyLimited(final InputStream in, final OutputStream out, final long limit) throws IOException{
		final byte[] buffer = new byte[64 * 1024];
		long copied = 0;
		int read;
		while((read = in.read(buffer)) >= 0){
			copied += read;
			if(copied > limit)
				throw new IOException(ASSET_NAME + " is too big once unpacked");

			out.write(buffer, 0, read);
		}
		return copied;
	}

	private static void deleteTree(final Path directory){
		try(final Stream<Path> walk = Files.walk(directory)){
			walk.sorted(Comparator.reverseOrder())
				.forEach(path -> {
					try{
						Files.deleteIfExists(path);
					}
					catch(final IOException ignored){
						//a leftover in the temporary directory is not worth an error
					}
				});
		}
		catch(final IOException ignored){
			//the same
		}
	}

}
