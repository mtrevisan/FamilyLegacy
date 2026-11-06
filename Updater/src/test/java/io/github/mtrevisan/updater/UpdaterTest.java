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
package io.github.mtrevisan.updater;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;


/** The check against a canned GitHub answer, and the download of the installer. */
class UpdaterTest{

	private static final byte[] INSTALLER = "an installer, more or less".getBytes(StandardCharsets.UTF_8);
	private static final String INSTALLER_URL = "https://github.com/mtrevisan/FamilyLegacy/releases/download/v1.2.0/FamilyLegacy-1.2.0.exe";


	private static String sha256(final byte[] bytes) throws Exception{
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
	}

	private static String releaseJson(final String assetsJson){
		return "{\"tag_name\":\"v1.2.0\",\"name\":\"Family Legacy 1.2.0\",\"draft\":false,\"prerelease\":false,"
			+ "\"html_url\":\"https://github.com/mtrevisan/FamilyLegacy/releases/tag/v1.2.0\","
			+ "\"body\":\"Fixes \\u00e8 new things\\r\\n- one\\n- two\",\"assets\":[" + assetsJson + "]}";
	}

	private static String asset(final String name, final String url, final long size, final String digest){
		return "{\"name\":\"" + name + "\",\"browser_download_url\":\"" + url + "\",\"size\":" + size
			+ (digest == null? "": ",\"digest\":\"sha256:" + digest + "\"") + "}";
	}

	private static Transport answering(final Map<String, byte[]> content){
		return uri -> {
			final byte[] bytes = content.get(uri.toString());
			if(bytes == null)
				throw new Transport.HttpStatusException(404, uri);
			return new ByteArrayInputStream(bytes);
		};
	}

	private static GitHubReleases releases(final Transport transport){
		return new GitHubReleases("mtrevisan/FamilyLegacy", transport, name -> InstallerLauncher.installerRank(name, true));
	}

	private static Transport withLatest(final String json){
		return answering(Map.of("https://api.github.com/repos/mtrevisan/FamilyLegacy/releases/latest",
			json.getBytes(StandardCharsets.UTF_8)));
	}


	@Test
	void readsTheLatestRelease() throws Exception{
		final String json = releaseJson(asset("FamilyLegacy-1.2.0.msi", "https://example.org/x.msi", 5, null) + ","
			+ asset("notes.txt", "https://example.org/notes.txt", 1, null) + ","
			+ asset("FamilyLegacy-1.2.0.exe", INSTALLER_URL, INSTALLER.length, sha256(INSTALLER).toUpperCase()));

		final ReleaseInfo release = releases(withLatest(json)).latest().orElseThrow();

		Assertions.assertEquals("1.2.0", release.version().toString());
		Assertions.assertEquals("Family Legacy 1.2.0", release.name());
		Assertions.assertTrue(release.notes().startsWith("Fixes è new things"));
		//the exe is preferred to the msi
		Assertions.assertEquals("FamilyLegacy-1.2.0.exe", release.installerName());
		Assertions.assertEquals(URI.create(INSTALLER_URL), release.installerUrl());
		Assertions.assertEquals(INSTALLER.length, release.installerSize());
		Assertions.assertEquals(sha256(INSTALLER), release.sha256());
	}

	@Test
	void readsTheChecksumFromASiblingFile() throws Exception{
		final String checksumUrl = INSTALLER_URL + ".sha256";
		final String json = releaseJson(asset("FamilyLegacy-1.2.0.exe", INSTALLER_URL, INSTALLER.length, null) + ","
			+ asset("FamilyLegacy-1.2.0.exe.sha256", checksumUrl, 100, null));
		final Transport transport = answering(Map.of(
			"https://api.github.com/repos/mtrevisan/FamilyLegacy/releases/latest", json.getBytes(StandardCharsets.UTF_8),
			checksumUrl, (sha256(INSTALLER) + "  FamilyLegacy-1.2.0.exe\n").getBytes(StandardCharsets.UTF_8)));

		Assertions.assertEquals(sha256(INSTALLER), releases(transport).latest().orElseThrow().sha256());
	}

	@Test
	void aReleaseWithoutInstallerIsStillReported() throws Exception{
		final ReleaseInfo release = releases(withLatest(releaseJson(asset("source.zip", "https://example.org/s.zip", 1, null))))
			.latest().orElseThrow();

		Assertions.assertFalse(release.hasInstaller());
		Assertions.assertNull(release.sha256());
	}

	@Test
	void aDownloadThatIsNotHttpsIsNotOffered() throws Exception{
		final ReleaseInfo release = releases(withLatest(releaseJson(asset("a.exe", "http://example.org/a.exe", 1, null))))
			.latest().orElseThrow();

		Assertions.assertFalse(release.hasInstaller());
	}

	@Test
	void noReleaseYet() throws Exception{
		Assertions.assertEquals(Optional.empty(), releases(answering(Map.of())).latest());
	}

	@Test
	void aDraftOrAnUnknownTagIsIgnored() throws Exception{
		Assertions.assertTrue(releases(withLatest(releaseJson("").replace("\"draft\":false", "\"draft\":true"))).latest().isEmpty());
		Assertions.assertTrue(releases(withLatest(releaseJson("").replace("v1.2.0", "nightly"))).latest().isEmpty());
	}

	@Test
	void garbageIsAnIoError(){
		Assertions.assertThrows(IOException.class, () -> releases(withLatest("<html>rate limited</html>")).latest());
	}

	@Test
	void anyOtherHttpErrorIsAnIoError(){
		final Transport failing = uri -> {
			throw new Transport.HttpStatusException(500, uri);
		};
		Assertions.assertThrows(IOException.class, () -> releases(failing).latest());
	}

	@Test
	void comparesWithTheRunningVersion() throws Exception{
		final UpdateChecker checker = new UpdateChecker(releases(withLatest(releaseJson(""))));

		Assertions.assertEquals(UpdateCheck.Status.UPDATE_AVAILABLE, checker.check("1.1.9").status());
		Assertions.assertEquals(UpdateCheck.Status.UPDATE_AVAILABLE, checker.check("1.2.0-rc1").status());
		Assertions.assertEquals(UpdateCheck.Status.UP_TO_DATE, checker.check("1.2.0").status());
		Assertions.assertEquals(UpdateCheck.Status.UP_TO_DATE, checker.check("2.0.0 (development)").status());
		Assertions.assertEquals(UpdateCheck.Status.UNKNOWN_CURRENT_VERSION, checker.check("unknown").status());
		Assertions.assertNotNull(checker.check("unknown").release());
	}

	@Test
	void noReleaseMeansNothingToCompare() throws Exception{
		final UpdateCheck check = new UpdateChecker(releases(answering(Map.of()))).check("1.0.0");

		Assertions.assertEquals(UpdateCheck.Status.NO_RELEASE, check.status());
		Assertions.assertNull(check.release());
	}


	private static ReleaseInfo release(final String name, final long size, final String sha256){
		return new ReleaseInfo(Version.parse("1.2.0").orElseThrow(), "v1.2.0", "r", "", URI.create("https://example.org"), name,
			URI.create(INSTALLER_URL), size, sha256);
	}

	private static Transport serving(final byte[] bytes){
		return uri -> new ByteArrayInputStream(bytes);
	}


	@Test
	void downloadsAndVerifies(@TempDir final Path directory) throws Exception{
		final AtomicInteger last = new AtomicInteger();

		final Path file = new InstallerDownloader(serving(INSTALLER))
			.download(release("FamilyLegacy-1.2.0.exe", INSTALLER.length, sha256(INSTALLER)), directory,
				(done, total) -> last.set((int)done), null);

		Assertions.assertArrayEquals(INSTALLER, Files.readAllBytes(file));
		Assertions.assertEquals(INSTALLER.length, last.get());
		Assertions.assertFalse(Files.exists(directory.resolve("FamilyLegacy-1.2.0.exe.part")));
	}

	@Test
	void aWrongChecksumLeavesNothing(@TempDir final Path directory) throws Exception{
		final ReleaseInfo release = release("a.exe", INSTALLER.length, sha256("something else".getBytes(StandardCharsets.UTF_8)));

		Assertions.assertThrows(IOException.class, () -> new InstallerDownloader(serving(INSTALLER)).download(release, directory, null, null));
		try(final var files = Files.list(directory)){
			Assertions.assertEquals(List.of(), files.toList());
		}
	}

	@Test
	void aShortDownloadLeavesNothing(@TempDir final Path directory) throws Exception{
		final ReleaseInfo release = release("a.exe", INSTALLER.length + 10, null);

		Assertions.assertThrows(IOException.class, () -> new InstallerDownloader(serving(INSTALLER)).download(release, directory, null, null));
		try(final var files = Files.list(directory)){
			Assertions.assertEquals(List.of(), files.toList());
		}
	}

	@Test
	void canBeCancelled(@TempDir final Path directory) throws Exception{
		final ReleaseInfo release = release("a.exe", INSTALLER.length, null);

		Assertions.assertThrows(InstallerDownloader.DownloadCancelledException.class,
			() -> new InstallerDownloader(serving(INSTALLER)).download(release, directory, null, () -> true));
		try(final var files = Files.list(directory)){
			Assertions.assertEquals(List.of(), files.toList());
		}
	}

	@Test
	void aNameThatLeavesTheDirectoryIsRefused(@TempDir final Path directory){
		final InstallerDownloader downloader = new InstallerDownloader(serving(INSTALLER));

		Assertions.assertThrows(IOException.class,
			() -> downloader.download(release("..\\..\\evil.exe", INSTALLER.length, null), directory, null, null));
		Assertions.assertThrows(IOException.class,
			() -> downloader.download(release("../evil.exe", INSTALLER.length, null), directory, null, null));
	}

	@Test
	void aReleaseWithoutInstallerCannotBeDownloaded(@TempDir final Path directory){
		final ReleaseInfo none = new ReleaseInfo(Version.parse("1.2.0").orElseThrow(), "v1.2.0", "r", "", URI.create("https://example.org"),
			null, null, -1, null);

		Assertions.assertThrows(IOException.class, () -> new InstallerDownloader(serving(INSTALLER)).download(none, directory, null, null));
	}


	@Test
	void theLauncherKnowsTheInstallers(){
		Assertions.assertEquals(0, InstallerLauncher.installerRank("FamilyLegacy-1.0.0.EXE", true));
		Assertions.assertEquals(1, InstallerLauncher.installerRank("FamilyLegacy-1.0.0.msi", true));
		Assertions.assertEquals(-1, InstallerLauncher.installerRank("FamilyLegacy-1.0.0.zip", true));
		Assertions.assertEquals(-1, InstallerLauncher.installerRank("FamilyLegacy-1.0.0.exe", false));
		Assertions.assertTrue(InstallerLauncher.isWindows("Windows 11"));
		Assertions.assertFalse(InstallerLauncher.isWindows("Linux"));
	}

	@Test
	void anMsiRunsThroughMsiexec(){
		final Path msi = Path.of("x.msi").toAbsolutePath();

		Assertions.assertEquals(List.of("msiexec", "/i", msi.toString()), InstallerLauncher.command(msi));
		Assertions.assertEquals(List.of(Path.of("x.exe").toAbsolutePath().toString()), InstallerLauncher.command(Path.of("x.exe")));
	}

}
