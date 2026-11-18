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
package io.github.mtrevisan.reports.site;

import io.github.mtrevisan.reports.ReportChapter;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportMarkup;
import io.github.mtrevisan.reports.ReportRenderer;
import io.github.mtrevisan.reports.ReportXhtml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;


/**
 * Renders a {@link ReportDocument} as a static website, in a directory, with no library: a page for each heading of the first level
 * and a home page (the title page with the contents), a navigation that lists the pages, links to the previous and the next page, the
 * images in a folder of their own. The pages need no server and no script: the site can be opened from the disk or published as it is.
 * <p>
 * The protocol links {@code [text](@id@)} become links between pages, to the heading that carries the record reference
 * ({@code [I12]}). The language of the report is the one of the site (and its direction, for the right-to-left languages).
 */
public final class SiteReportRenderer implements ReportRenderer{

	private static final String HOME = "index.html";
	private static final String STYLE_FILE = "style.css";
	private static final String IMAGES_DIR = "images";
	/** The list of the files written by the last rendering: only these are removed by the next one. */
	private static final String MANIFEST_FILE = ".site-manifest";
	private static final String STAGING_PREFIX = ".render-";
	private static final int MAX_SLUG_LENGTH = 60;
	/** Above this number of chapters the navigation lists only the ones around the current page (the home page has all of them). */
	private static final int NAV_LIMIT = 25;
	private static final int NAV_RADIUS = 10;
	private static final System.Logger LOGGER = System.getLogger(SiteReportRenderer.class.getName());
	private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "svg", "webp");
	private static final String STYLE = """
		:root{--bg:#fff;--ink:#222;--muted:#595959;--line:#d8d8d8;--link:#1f3864;--nav:#f5f6f8;}
			@media (prefers-color-scheme:dark){:root{--bg:#16181d;--ink:#e6e6e6;--muted:#a8a8a8;--line:#3a3d44;--link:#9db7ee;--nav:#1d2026;}}
		*{box-sizing:border-box;}
		body{margin:0;min-height:100vh;display:flex;flex-direction:column;font-family:Georgia,serif;line-height:1.6;color:var(--ink);background:var(--bg);}
		a{color:var(--link);text-decoration:none;}a:hover{text-decoration:underline;}a:focus-visible{outline:2px solid var(--link);outline-offset:2px;}
		.site-header{padding:.6em 1.2em;border-bottom:1px solid var(--line);font-weight:bold;}
		.layout{display:grid;grid-template-columns:18em minmax(0,1fr);grid-template-areas:"nav main";flex:1;}
			main{grid-area:main;}.site-nav{grid-area:nav;}
		.site-nav{background:var(--nav);border-inline-end:1px solid var(--line);padding:1em 1.2em;font-size:.95em;}
		.site-nav h2{margin:.2em 0 .6em;font-size:1em;text-transform:uppercase;letter-spacing:.05em;color:var(--muted);border:0;}
		.site-nav ol{margin:0;padding:0;list-style:none;}
		.site-nav li{margin:.25em 0;}
		.site-nav a[aria-current]{font-weight:bold;color:var(--ink);}
		main{padding:1.5em 2.5em;max-width:56em;}
		h1,h2,h3{border-bottom:1px solid var(--line);padding-bottom:.2em;line-height:1.25;}
		.subtitle{font-style:italic;color:var(--muted);}
		table{border-collapse:collapse;width:100%;margin:.8em 0;}
		td,th{border:1px solid var(--line);padding:3px 8px;text-align:start;overflow-wrap:anywhere;}
		pre{white-space:pre-wrap;font-family:ui-monospace,monospace;}
		figure{margin:1em 0;}img{max-width:100%;height:auto;}figcaption{font-size:.85em;color:var(--muted);}
		.pager{display:flex;justify-content:space-between;gap:1em;margin-top:2.5em;padding-top:1em;border-top:1px solid var(--line);}
		@media (max-width:50em){.layout{grid-template-columns:minmax(0,1fr);grid-template-areas:"main" "nav";}.site-nav{border-inline-end:0;border-top:1px solid var(--line);}main{padding:1em;}}
		@media print{.site-nav,.pager,.site-header{display:none;}.layout{display:block;}}
		""";


	@Override
	public String id(){
		return "site";
	}

	@Override
	public String displayName(){
		return "Static Website";
	}

	/** The site is a directory: the suffix of its name, not an extension. */
	@Override
	public String extension(){
		return "_site";
	}

	@Override
	public void render(final ReportDocument doc, final Path target) throws IOException{
		final List<ReportChapter> chapters = ReportChapter.split(doc);
		final Map<String, Integer> anchors = ReportChapter.anchors(chapters);
		final Site site = new Site(doc, chapters, pageFiles(chapters), labels(chapters, fallbackTitle(target)));

		Files.createDirectories(target);
		//the site is written aside and moved in only when complete: a failure leaves the previous site as it was
		final Path staging = Files.createTempDirectory(target, STAGING_PREFIX);
		try{
			final Map<Path, String> images = new LinkedHashMap<>();
			for(int i = 0; i < chapters.size(); i ++){
				final String body = body(site, i, anchors, staging, images);
				Files.writeString(staging.resolve(site.files().get(i)), page(site, i, body), StandardCharsets.UTF_8);
			}
			Files.writeString(staging.resolve(STYLE_FILE), STYLE, StandardCharsets.UTF_8);

			publish(staging, target);
		}
		finally{
			deleteTree(staging);
		}
	}


	/* ======================================================================
	 *                          Pages
	 * ====================================================================== */

	/** What is needed to write the pages: the report, its chapters, and the file name and label of each one. */
	private record Site(ReportDocument doc, List<ReportChapter> chapters, List<String> files, List<String> labels){}


	/**
	 * The home page is the title page; the others are named after their title (so that the addresses do not change when a chapter is
	 * added or removed), or by their position when the title gives no usable name or is already taken.
	 */
	private static List<String> pageFiles(final List<ReportChapter> chapters){
		final List<String> files = new ArrayList<>(chapters.size());
		files.add(HOME);
		final Set<String> reserved = Set.of("index", "style");
		final Set<String> used = new HashSet<>();
		for(int i = 1; i < chapters.size(); i ++){
			final String slug = slug(chapters.get(i).title());
			String name = (slug.isEmpty() || reserved.contains(slug)? String.format(Locale.ROOT, "chapter-%03d", i): slug);
			if(!used.add(name)){
				int suffix = 2;
				while(!used.add(name + "-" + suffix))
					suffix ++;
				name += "-" + suffix;
			}
			files.add(name + ".html");
		}
		return files;
	}

	/** Lower case ASCII letters and digits, separated by single dashes; no accents; empty if there is nothing usable. */
	static String slug(final String title){
		final String decomposed = Normalizer.normalize(title, Normalizer.Form.NFD);
		final StringBuilder sb = new StringBuilder();
		boolean separator = false;
		for(int i = 0; i < decomposed.length() && sb.length() < MAX_SLUG_LENGTH; i ++){
			final char c = decomposed.charAt(i);
			if(Character.getType(c) == Character.NON_SPACING_MARK)
				continue;

			if(c < 128 && Character.isLetterOrDigit(c)){
				if(separator && !sb.isEmpty())
					sb.append('-');
				separator = false;
				sb.append(Character.toLowerCase(c));
			}
			else
				separator = true;
		}
		return sb.toString();
	}

	private static List<String> labels(final List<ReportChapter> chapters, final String fallback){
		final List<String> labels = new ArrayList<>(chapters.size());
		for(int i = 0; i < chapters.size(); i ++){
			final String title = chapters.get(i).title();
			labels.add(title.isBlank()? (i == 0? fallback: "-"): title);
		}
		return labels;
	}

	/** The title of a report without one: the name of the folder, without the suffix of the site. */
	private static String fallbackTitle(final Path target){
		final Path fileName = target.getFileName();
		String name = (fileName == null? "": fileName.toString());
		if(name.endsWith("_site"))
			name = name.substring(0, name.length() - "_site".length());
		return (name.isBlank()? "-": name);
	}

	/**
	 * Replaces the files of the previous rendering (only the ones listed in the manifest, never what else is in the directory) with the
	 * ones just written, then records the new list.
	 */
	private static void publish(final Path staging, final Path target) throws IOException{
		final List<String> generated = new ArrayList<>();
		try(final Stream<Path> files = Files.walk(staging)){
			for(final Path file : (Iterable<Path>)files.filter(Files::isRegularFile)::iterator)
				generated.add(staging.relativize(file).toString().replace('\\', '/'));
		}
		generated.sort(null);

		final Path root = target.toAbsolutePath().normalize();
		for(final String previous : previousFiles(root)){
			final Path file = root.resolve(previous).normalize();
			//an entry that points outside the site is not trusted
			if(file.startsWith(root) && !file.equals(root.resolve(MANIFEST_FILE)))
				Files.deleteIfExists(file);
		}
		try{
			Files.delete(root.resolve(IMAGES_DIR));
		}
		catch(final IOException ignored){
			//not there, or it keeps files that are not ours
		}

		for(final String name : generated){
			final Path destination = root.resolve(name);
			Files.createDirectories(destination.getParent());
			Files.move(staging.resolve(name), destination, StandardCopyOption.REPLACE_EXISTING);
		}
		Files.writeString(root.resolve(MANIFEST_FILE), String.join("\n", generated) + "\n", StandardCharsets.UTF_8);
	}

	private static List<String> previousFiles(final Path root) throws IOException{
		final Path manifest = root.resolve(MANIFEST_FILE);
		if(Files.isRegularFile(manifest))
			return Files.readAllLines(manifest, StandardCharsets.UTF_8).stream()
				.filter(line -> !line.isBlank())
				.toList();
		return legacyFiles(root);
	}

	/**
	 * The files of a site written before the manifest existed: recognized by its home page (its navigation and its pager), so that
	 * the pages with the old names (and the images) do not stay in the folder; nothing is removed from any other directory.
	 */
	private static List<String> legacyFiles(final Path root) throws IOException{
		final Path home = root.resolve(HOME);
		if(!Files.isRegularFile(home))
			return List.of();

		final String content = Files.readString(home, StandardCharsets.UTF_8);
		if(!content.contains("class=\"site-nav\"") || !content.contains("class=\"pager\""))
			return List.of();

		final List<String> files = new ArrayList<>();
		collect(root, "chapter-*.html", "", files);
		collect(root.resolve(IMAGES_DIR), "image-*", IMAGES_DIR + "/", files);
		return files;
	}

	private static void collect(final Path directory, final String glob, final String prefix, final List<String> files) throws IOException{
		if(!Files.isDirectory(directory))
			return;

		try(final DirectoryStream<Path> stream = Files.newDirectoryStream(directory, glob)){
			for(final Path file : stream)
				if(Files.isRegularFile(file))
					files.add(prefix + file.getFileName());
		}
	}

	private static void deleteTree(final Path directory){
		try(final Stream<Path> paths = Files.walk(directory)){
			for(final Path path : (Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator)
				Files.deleteIfExists(path);
		}
		catch(final IOException | UncheckedIOException ignored){
			//a temporary directory that cannot be removed does no harm
		}
	}

	private static String body(final Site site, final int index, final Map<String, Integer> anchors, final Path target,
			final Map<Path, String> images){
		final ReportDocument doc = site.doc();
		final List<ReportChapter> chapters = site.chapters();
		final ReportChapter chapter = chapters.get(index);
		final StringBuilder sb = new StringBuilder();
		if(chapter.titlePage()){
			sb.append("<h1>").append(ReportMarkup.escape(site.labels().getFirst())).append("</h1>\n");
			if(doc.subtitle() != null && !doc.subtitle().isBlank())
				sb.append("<p class=\"subtitle\">").append(ReportMarkup.escape(ReportMarkup.plain(doc.subtitle()))).append("</p>\n");
		}
		final Set<String> writtenAnchors = new HashSet<>();
		chapter.sections().forEach(section -> ReportXhtml.writeSection(sb, section,
			//a link to a record that has no heading in the site is only its text
			id -> (anchors.containsKey(id)? site.files().get(anchors.get(id)) + "#" + id: null),
			file -> copyImage(file, target, images), writtenAnchors));

		if(chapter.titlePage()){
			sb.append("<h2>").append(ReportMarkup.escape(doc.contentsTitle())).append("</h2>\n<ol>\n");
			for(int i = 1; i < chapters.size(); i ++)
				sb.append("<li><a href=\"").append(site.files().get(i)).append("\">").append(ReportMarkup.escape(site.labels().get(i)))
					.append("</a></li>\n");
			sb.append("</ol>\n");
		}
		return sb.toString();
	}

	private static String page(final Site site, final int index, final String body){
		final ReportDocument doc = site.doc();
		final List<ReportChapter> chapters = site.chapters();
		final List<String> files = site.files();
		final List<String> labels = site.labels();
		final String siteTitle = ReportMarkup.escape(labels.getFirst());
		final StringBuilder sb = new StringBuilder("<!DOCTYPE html>\n<html lang=\"").append(ReportMarkup.escape(doc.language())).append('"')
			.append(doc.isRightToLeft()? " dir=\"rtl\"": "").append(">\n<head><meta charset=\"utf-8\"/>")
			.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>")
			.append("<title>").append(ReportMarkup.escape(index == 0? labels.getFirst(): labels.get(index) + " - " + labels.getFirst()))
			.append("</title>");
		if(doc.subtitle() != null && !doc.subtitle().isBlank())
			sb.append("<meta name=\"description\" content=\"").append(ReportMarkup.escape(ReportMarkup.plain(doc.subtitle()))).append("\"/>");
		if(index > 0)
			sb.append("<link rel=\"prev\" href=\"").append(files.get(index - 1)).append("\"/>");
		if(index < chapters.size() - 1)
			sb.append("<link rel=\"next\" href=\"").append(files.get(index + 1)).append("\"/>");
		sb.append("<link rel=\"stylesheet\" href=\"").append(STYLE_FILE).append("\"/></head>\n<body>\n");
		sb.append("<header class=\"site-header\"><a href=\"").append(HOME).append("\">").append(siteTitle).append("</a></header>\n");
		//the content comes first in the page (readers and keyboard reach it without crossing the navigation); the style puts the
		//navigation beside it
		sb.append("<div class=\"layout\">\n<main>\n").append(body);

		//the arrows point towards where the pages go: reversed in the right-to-left languages
		final String previousArrow = (doc.isRightToLeft()? "›": "‹");
		final String nextArrow = (doc.isRightToLeft()? "‹": "›");
		sb.append("<div class=\"pager\"><span>");
		if(index > 0)
			sb.append("<a rel=\"prev\" href=\"").append(files.get(index - 1)).append("\">").append(previousArrow).append(' ')
				.append(ReportMarkup.escape(labels.get(index - 1))).append("</a>");
		sb.append("</span><span>");
		if(index < chapters.size() - 1)
			sb.append("<a rel=\"next\" href=\"").append(files.get(index + 1)).append("\">")
				.append(ReportMarkup.escape(labels.get(index + 1))).append(' ').append(nextArrow).append("</a>");
		sb.append("</span></div>\n</main>\n");

		sb.append("<nav class=\"site-nav\" aria-label=\"").append(ReportMarkup.escape(doc.contentsTitle()))
			.append("\"><h2>").append(ReportMarkup.escape(doc.contentsTitle())).append("</h2>\n<ol>\n");
		final boolean windowed = (chapters.size() > NAV_LIMIT);
		int previous = -1;
		for(int i = 0; i < chapters.size(); i ++){
			if(windowed && i != 0 && Math.abs(i - index) > NAV_RADIUS)
				continue;

			//a gap in the list
			if(i > previous + 1)
				sb.append("<li aria-hidden=\"true\">…</li>\n");
			previous = i;
			sb.append("<li><a href=\"").append(files.get(i)).append('"').append(i == index? " aria-current=\"page\"": "").append('>')
				.append(ReportMarkup.escape(labels.get(i))).append("</a></li>\n");
		}
		if(previous < chapters.size() - 1)
			sb.append("<li aria-hidden=\"true\">…</li>\n");
		sb.append("</ol></nav>\n</div>\n</body>\n</html>\n");
		return sb.toString();
	}


	/* ======================================================================
	 *                          Images
	 * ====================================================================== */

	/** Copies an image into the site; returns its address there, {@code null} if it is not an image of the web or cannot be read. */
	private static String copyImage(final Path file, final Path target, final Map<Path, String> images){
		if(file == null)
			return null;

		final Path key = file.toAbsolutePath().normalize();
		if(images.containsKey(key))
			return images.get(key);

		final String name = key.getFileName().toString();
		final int dot = name.lastIndexOf('.');
		if(dot < 0 || !IMAGE_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT)))
			return null;

		try{
			//an SVG can carry scripts, that would run if the file is opened by itself
			if("svg".equals(name.substring(dot + 1).toLowerCase(Locale.ROOT))
					&& Files.readString(key, StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT).contains("<script")){
				LOGGER.log(System.Logger.Level.WARNING, "SVG image with a script not copied into the site: {0}", key);
				return null;
			}
		}
		catch(final IOException ioe){
			LOGGER.log(System.Logger.Level.WARNING, "Image not copied into the site: {0} ({1})", key, ioe.toString());
			return null;
		}

		final String href = IMAGES_DIR + "/image-" + (images.size() + 1) + name.substring(dot).toLowerCase(Locale.ROOT);
		try{
			Files.createDirectories(target.resolve(IMAGES_DIR));
			Files.copy(key, target.resolve(href), StandardCopyOption.REPLACE_EXISTING);
			images.put(key, href);
			return href;
		}
		catch(final IOException ioe){
			LOGGER.log(System.Logger.Level.WARNING, "Image not copied into the site: {0} ({1})", key, ioe.toString());
			return null;
		}
	}

}
