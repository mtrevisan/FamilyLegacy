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
import io.github.mtrevisan.updater.Transport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/** The download of the fonts of the PDF reports, against a canned GitHub answer. */
class FontPackTest{

	private static final String API = "https://api.github.com/repos/mtrevisan/FamilyLegacy/releases/latest";
	private static final String URL = "https://github.com/mtrevisan/FamilyLegacy/releases/download/v1.0.0/fonts.zip";


	private static byte[] zip(final String... namesAndContents) throws IOException{
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try(final ZipOutputStream zos = new ZipOutputStream(bytes)){
			for(int i = 0; i < namesAndContents.length; i += 2){
				zos.putNextEntry(new ZipEntry(namesAndContents[i]));
				zos.write(namesAndContents[i + 1].getBytes(StandardCharsets.UTF_8));
				zos.closeEntry();
			}
		}
		return bytes.toByteArray();
	}

	private static Transport publishing(final byte[] zip, final boolean withDigest) throws Exception{
		final String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(zip));
		final String json = "{\"tag_name\":\"v1.0.0\",\"draft\":false,\"assets\":[{\"name\":\"fonts.zip\",\"browser_download_url\":\"" + URL
			+ "\",\"size\":" + zip.length + (withDigest? ",\"digest\":\"sha256:" + digest + "\"": "") + "}]}";
		final Map<String, byte[]> content = Map.of(API, json.getBytes(StandardCharsets.UTF_8), URL, zip);
		return uri -> {
			final byte[] bytes = content.get(uri.toString());
			if(bytes == null)
				throw new Transport.HttpStatusException(404, uri);
			return new ByteArrayInputStream(bytes);
		};
	}

	private static GitHubReleases releases(final Transport transport){
		return new GitHubReleases("mtrevisan/FamilyLegacy", transport, name -> -1);
	}


	@Test
	void theFontsAreDownloadedAndUnpackedBesideThePlugins(@TempDir final Path directory) throws Exception{
		final Path fonts = directory.resolve("fonts");
		Assertions.assertFalse(FontPack.isInstalled(fonts));

		final Transport transport = publishing(zip("NotoSans-Regular.ttf", "a font", "OFL.txt", "the licence"), true);
		FontPack.download(releases(transport), transport, fonts, null, null);

		Assertions.assertTrue(FontPack.isInstalled(fonts));
		Assertions.assertEquals("a font", Files.readString(fonts.resolve("NotoSans-Regular.ttf")));
		Assertions.assertEquals("the licence", Files.readString(fonts.resolve("OFL.txt")));
		try(final var files = Files.list(fonts)){
			Assertions.assertEquals(2, files.count(), "no leftover of the unpacking");
		}
	}

	@Test
	void aPackWithAFileOutsideTheDirectoryIsRefusedWhole(@TempDir final Path directory) throws Exception{
		final Path fonts = directory.resolve("fonts");
		final Transport transport = publishing(zip("NotoSans-Regular.ttf", "a font", "../evil.ttf", "x"), true);

		Assertions.assertThrows(IOException.class, () -> FontPack.download(releases(transport), transport, fonts, null, null));

		Assertions.assertFalse(FontPack.isInstalled(fonts), "nothing is installed");
		Assertions.assertFalse(Files.exists(directory.resolve("evil.ttf")));
	}

	@Test
	void aPackWithoutAPublishedChecksumIsNotDownloaded(@TempDir final Path directory) throws Exception{
		final Transport transport = publishing(zip("NotoSans-Regular.ttf", "a font"), false);

		Assertions.assertThrows(IOException.class, () -> FontPack.download(releases(transport), transport, directory.resolve("fonts"), null, null));
	}

	@Test
	void aReleaseWithoutTheFontsSaysSo(@TempDir final Path directory){
		final Transport nothing = uri -> {
			throw new Transport.HttpStatusException(404, uri);
		};

		Assertions.assertThrows(FontPack.NotPublishedException.class,
			() -> FontPack.download(releases(nothing), nothing, directory.resolve("fonts"), null, null));
	}

	@Test
	void aMissingDirectoryIsNotInstalled(@TempDir final Path directory){
		Assertions.assertFalse(FontPack.isInstalled(null));
		Assertions.assertFalse(FontPack.isInstalled(directory.resolve("nothing")));
	}

}
