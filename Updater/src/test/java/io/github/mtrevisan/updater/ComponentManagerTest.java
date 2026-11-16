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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/** The catalog of the optional components and their installation, against canned answers of GitHub. */
class ComponentManagerTest{

	private static final String REPOSITORY = "mtrevisan/FamilyLegacy";
	private static final String DOWNLOADS = "https://github.com/" + REPOSITORY + "/releases/download/";
	private static final String LATEST_API = "https://api.github.com/repos/" + REPOSITORY + "/releases/latest";
	private static final String TAG_API = "https://api.github.com/repos/" + REPOSITORY + "/releases/tags/components";


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

	private static String sha256(final byte[] bytes) throws Exception{
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
	}

	/** A release with these files (name to content), each published with its digest. */
	private static String release(final String tag, final Map<String, byte[]> files) throws Exception{
		final StringBuilder assets = new StringBuilder();
		for(final Map.Entry<String, byte[]> file : files.entrySet()){
			if(!assets.isEmpty())
				assets.append(',');
			assets.append("{\"name\":\"").append(file.getKey()).append("\",\"browser_download_url\":\"").append(DOWNLOADS).append(tag).append('/')
				.append(file.getKey()).append("\",\"size\":").append(file.getValue().length).append(",\"digest\":\"sha256:")
				.append(sha256(file.getValue())).append("\"}");
		}
		return "{\"tag_name\":\"" + tag + "\",\"draft\":false,\"assets\":[" + assets + "]}";
	}

	private static Transport serving(final Map<String, byte[]> content){
		return uri -> {
			final byte[] bytes = content.get(uri.toString());
			if(bytes == null)
				throw new Transport.HttpStatusException(404, uri);
			return new ByteArrayInputStream(bytes);
		};
	}

	private static ComponentManager manager(final Transport transport, final Path fonts, final Path plugins, final String catalogUrl){
		return new ComponentManager(new GitHubReleases(REPOSITORY, transport, name -> -1), transport, Map.of("fonts", fonts, "plugins", plugins),
			URI.create(catalogUrl));
	}

	private static byte[] text(final String text){
		return text.getBytes(StandardCharsets.UTF_8);
	}


	@Test
	void theCatalogTakesPlainAndLocalizedTextsAndSkipsWhatItDoesNotUnderstand() throws Exception{
		final ComponentCatalog catalog = ComponentCatalog.parse("""
			{"components":[
			 {"id":"a","name":"Plain","target":"fonts","asset":"a.zip"},
			 {"id":"b","name":{"en":"English","it":"Italiano"},"description":{"it":"Una descrizione"},"target":"plugins","asset":"b.jar",
			  "release":"components","files":["b.jar"],"minVersion":"2.0.0"},
			 {"id":"c","name":"No source","target":"fonts"},
			 {"id":"d","name":"Unknown target","target":"elsewhere","asset":"d.zip"},
			 {"id":"../e","name":"Bad id","target":"fonts","asset":"e.zip"},
			 {"id":"f","name":"Bad file","target":"fonts","asset":"../f.zip"},
			 {"id":"g","name":"Http","target":"fonts","url":"http://example.org/g.zip","sha256":"%s"},
			 {"id":"h","name":"No checksum","target":"fonts","url":"https://example.org/h.zip"},
			 {"id":"i","name":"By url","target":"fonts","url":"https://example.org/i.zip","sha256":"%s"},
			 {"id":"a","name":"Duplicate","target":"fonts","asset":"a2.zip"}
			]}""".formatted("0".repeat(64), "A".repeat(64)));

		final List<Component> components = catalog.components();
		Assertions.assertEquals(List.of("a", "b", "i"), components.stream().map(Component::id).toList());
		Assertions.assertEquals("Plain", components.get(0).name(Locale.ITALIAN));
		Assertions.assertEquals("Italiano", components.get(1).name(Locale.ITALIAN));
		Assertions.assertEquals("English", components.get(1).name(Locale.GERMAN));
		Assertions.assertEquals("Una descrizione", components.get(1).description(Locale.ITALIAN));
		Assertions.assertEquals("", components.get(0).description(Locale.ITALIAN));
		Assertions.assertEquals("components", components.get(1).release());
		Assertions.assertEquals(Component.LATEST, components.get(0).release());
		Assertions.assertEquals("A".repeat(64).toLowerCase(Locale.ROOT), components.get(2).sha256());
	}

	@Test
	void anInvalidCatalogIsAnError(){
		Assertions.assertThrows(IOException.class, () -> ComponentCatalog.parse("{not json"));
	}

	@Test
	void withoutACatalogTheFontsOfTheLatestReleaseAreOffered() throws Exception{
		final Transport nothing = serving(Map.of());

		final ComponentCatalog catalog = ComponentCatalog.read(nothing, URI.create("https://example.org/catalog.json"));

		Assertions.assertEquals(1, catalog.components().size());
		Assertions.assertEquals("fonts.zip", catalog.components().getFirst().asset());
	}

	@Test
	void aComponentIsInstalledThenKnownAsInstalledThenOutdatedWhenTheFileChanges(@TempDir final Path directory) throws Exception{
		final Path fonts = directory.resolve("app/fonts");
		final Path plugins = directory.resolve("app/plugins");
		final String catalogUrl = "https://example.org/catalog.json";
		final Map<String, byte[]> content = new HashMap<>();
		content.put(catalogUrl, text("""
			{"components":[
			 {"id":"cjk","name":"CJK","target":"fonts","asset":"fonts-cjk.zip","release":"components","files":["NotoSansSC-Regular.otf"]},
			 {"id":"foo","name":"Foo","target":"plugins","asset":"report-foo.jar","release":"components"}
			]}"""));
		final byte[] fontsZip = zip("NotoSansSC-Regular.otf", "a font", "OFL.txt", "the licence");
		final byte[] jar = text("a jar");
		content.put(TAG_API, text(release("components", Map.of("fonts-cjk.zip", fontsZip, "report-foo.jar", jar))));
		content.put(DOWNLOADS + "components/fonts-cjk.zip", fontsZip);
		content.put(DOWNLOADS + "components/report-foo.jar", jar);
		final ComponentManager manager = manager(serving(content), fonts, plugins, catalogUrl);

		List<ComponentManager.Entry> entries = manager.list("1.0.0");
		Assertions.assertEquals(List.of(ComponentManager.State.AVAILABLE, ComponentManager.State.AVAILABLE),
			entries.stream().map(ComponentManager.Entry::state).toList());

		for(final ComponentManager.Entry entry : entries)
			manager.install(entry, null, null);

		Assertions.assertEquals("a font", Files.readString(fonts.resolve("NotoSansSC-Regular.otf")));
		Assertions.assertEquals("the licence", Files.readString(fonts.resolve("OFL.txt")));
		Assertions.assertEquals("a jar", Files.readString(plugins.resolve("report-foo.jar")));
		try(final var files = Files.list(fonts)){
			Assertions.assertEquals(2, files.count(), "no leftover of the unpacking");
		}
		entries = manager.list("1.0.0");
		Assertions.assertEquals(List.of(ComponentManager.State.INSTALLED, ComponentManager.State.INSTALLED),
			entries.stream().map(ComponentManager.Entry::state).toList());

		//a new file under the same name in the release
		final byte[] newJar = text("a newer jar");
		content.put(TAG_API, text(release("components", Map.of("fonts-cjk.zip", fontsZip, "report-foo.jar", newJar))));
		content.put(DOWNLOADS + "components/report-foo.jar", newJar);
		entries = manager.list("1.0.0");
		Assertions.assertEquals(ComponentManager.State.INSTALLED, entries.get(0).state());
		Assertions.assertEquals(ComponentManager.State.OUTDATED, entries.get(1).state());
		manager.install(entries.get(1), null, null);
		Assertions.assertEquals("a newer jar", Files.readString(plugins.resolve("report-foo.jar")));
		Assertions.assertEquals(ComponentManager.State.INSTALLED, manager.list("1.0.0").get(1).state());

		//the user deletes the fonts: they can be installed again
		Files.delete(fonts.resolve("NotoSansSC-Regular.otf"));
		Assertions.assertEquals(ComponentManager.State.AVAILABLE, manager.list("1.0.0").get(0).state());
	}

	@Test
	void fontsThatAreThereWithoutTheApplicationHavingInstalledThemCountAsInstalled(@TempDir final Path directory) throws Exception{
		final Path fonts = directory.resolve("app/fonts");
		Files.createDirectories(fonts);
		Files.writeString(fonts.resolve("NotoSans-Regular.ttf"), "a font");
		final Transport nothing = serving(Map.of());

		final List<ComponentManager.Entry> entries = manager(nothing, fonts, directory.resolve("app/plugins"), "https://example.org/c.json")
			.list(null);

		Assertions.assertEquals(ComponentManager.State.INSTALLED, entries.getFirst().state());
	}

	@Test
	void aComponentThatNeedsANewerApplicationIsLeftOut(@TempDir final Path directory) throws Exception{
		final String catalogUrl = "https://example.org/catalog.json";
		final Transport transport = serving(Map.of(catalogUrl, text("""
			{"components":[{"id":"x","name":"X","target":"fonts","asset":"x.zip","minVersion":"2.0.0"}]}""")));

		final ComponentManager manager = manager(transport, directory.resolve("fonts"), directory.resolve("plugins"), catalogUrl);

		Assertions.assertTrue(manager.list("1.9.9").isEmpty());
		Assertions.assertEquals(1, manager.list("2.0.0").size());
	}

	@Test
	void aFileOfAnotherKindOrOutsideTheFolderIsRefusedWhole(@TempDir final Path directory) throws Exception{
		final Path fonts = directory.resolve("app/fonts");
		final String catalogUrl = "https://example.org/catalog.json";
		final Map<String, byte[]> content = new HashMap<>();
		content.put(catalogUrl, text("""
			{"components":[
			 {"id":"slip","name":"Slip","target":"fonts","asset":"slip.zip"},
			 {"id":"exe","name":"Exe","target":"fonts","asset":"exe.zip"}
			]}"""));
		final byte[] slip = zip("good.ttf", "a font", "../evil.ttf", "x");
		final byte[] exe = zip("good.ttf", "a font", "run.exe", "x");
		content.put(LATEST_API, text(release("v1.0.0", Map.of("slip.zip", slip, "exe.zip", exe))));
		content.put(DOWNLOADS + "v1.0.0/slip.zip", slip);
		content.put(DOWNLOADS + "v1.0.0/exe.zip", exe);
		final ComponentManager manager = manager(serving(content), fonts, directory.resolve("app/plugins"), catalogUrl);

		for(final ComponentManager.Entry entry : manager.list("1.0.0"))
			Assertions.assertThrows(IOException.class, () -> manager.install(entry, null, null));

		Assertions.assertFalse(Files.exists(fonts.resolve("good.ttf")), "nothing is installed");
		Assertions.assertFalse(Files.exists(directory.resolve("app/evil.ttf")));
		Assertions.assertFalse(Files.exists(directory.resolve("app").resolve(ComponentManager.REGISTRY)));
	}

	@Test
	void aFileWithoutAPublishedChecksumCannotBeInstalled(@TempDir final Path directory) throws Exception{
		final String catalogUrl = "https://example.org/catalog.json";
		final Map<String, byte[]> content = new HashMap<>();
		content.put(catalogUrl, text("""
			{"components":[{"id":"x","name":"X","target":"fonts","asset":"x.zip"}]}"""));
		content.put(LATEST_API, text("{\"tag_name\":\"v1.0.0\",\"draft\":false,\"assets\":[{\"name\":\"x.zip\",\"browser_download_url\":\""
			+ DOWNLOADS + "v1.0.0/x.zip\",\"size\":3}]}"));

		final List<ComponentManager.Entry> entries = manager(serving(content), directory.resolve("fonts"), directory.resolve("plugins"),
			catalogUrl).list("1.0.0");

		Assertions.assertEquals(ComponentManager.State.UNAVAILABLE, entries.getFirst().state());
		Assertions.assertFalse(entries.getFirst().canInstall());
	}

	@Test
	void aFileFromAnAddressIsCheckedAgainstTheChecksumOfTheCatalog(@TempDir final Path directory) throws Exception{
		final byte[] jar = text("a jar");
		final String catalogUrl = "https://example.org/catalog.json";
		final Map<String, byte[]> content = new HashMap<>();
		content.put(catalogUrl, text("{\"components\":[{\"id\":\"x\",\"name\":\"X\",\"target\":\"plugins\",\"url\":\"https://example.org/report-x.jar\","
			+ "\"sha256\":\"" + sha256(jar) + "\"}]}"));
		content.put("https://example.org/report-x.jar", jar);
		final Path plugins = directory.resolve("app/plugins");
		final ComponentManager manager = manager(serving(content), directory.resolve("app/fonts"), plugins, catalogUrl);

		manager.install(manager.list("1.0.0").getFirst(), null, null);
		Assertions.assertEquals("a jar", Files.readString(plugins.resolve("report-x.jar")));

		content.put("https://example.org/report-x.jar", text("tampered"));
		Files.delete(plugins.resolve("report-x.jar"));
		Files.delete(directory.resolve("app").resolve(ComponentManager.REGISTRY));
		final ComponentManager.Entry entry = manager.list("1.0.0").getFirst();
		Assertions.assertThrows(IOException.class, () -> manager.install(entry, null, null));
		Assertions.assertFalse(Files.exists(plugins.resolve("report-x.jar")));
	}

}
