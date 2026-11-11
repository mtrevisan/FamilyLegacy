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
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


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
	private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "svg", "webp");
	private static final String STYLE = """
		:root{--ink:#222;--muted:#666;--line:#d8d8d8;--link:#1f3864;--nav:#f5f6f8;}
		*{box-sizing:border-box;}
		body{margin:0;font-family:Georgia,serif;line-height:1.6;color:var(--ink);}
		a{color:var(--link);text-decoration:none;}a:hover{text-decoration:underline;}
		.site-header{padding:.6em 1.2em;border-bottom:1px solid var(--line);font-weight:bold;}
		.layout{display:grid;grid-template-columns:18em minmax(0,1fr);min-height:calc(100vh - 3em);}
		.site-nav{background:var(--nav);border-inline-end:1px solid var(--line);padding:1em 1.2em;font-size:.95em;}
		.site-nav h2{margin:.2em 0 .6em;font-size:1em;text-transform:uppercase;letter-spacing:.05em;color:var(--muted);border:0;}
		.site-nav ol{margin:0;padding:0;list-style:none;}
		.site-nav li{margin:.25em 0;}
		.site-nav a[aria-current]{font-weight:bold;color:var(--ink);}
		main{padding:1.5em 2.5em;max-width:56em;}
		h1,h2,h3{border-bottom:1px solid var(--line);padding-bottom:.2em;line-height:1.25;}
		.subtitle{font-style:italic;color:var(--muted);}
		table{border-collapse:collapse;width:100%;margin:.8em 0;display:block;overflow-x:auto;}
		td,th{border:1px solid var(--line);padding:3px 8px;text-align:start;}
		pre{white-space:pre-wrap;font-family:ui-monospace,monospace;}
		figure{margin:1em 0;}img{max-width:100%;height:auto;}figcaption{font-size:.85em;color:var(--muted);}
		.pager{display:flex;justify-content:space-between;gap:1em;margin-top:2.5em;padding-top:1em;border-top:1px solid var(--line);}
		@media (max-width:50em){.layout{grid-template-columns:1fr;}.site-nav{border-inline-end:0;border-bottom:1px solid var(--line);}main{padding:1em;}}
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

		Files.createDirectories(target);
		clean(target);
		final Map<Path, String> images = new LinkedHashMap<>();

		for(int i = 0; i < chapters.size(); i ++){
			final String body = body(doc, chapters, i, anchors, target, images);
			Files.writeString(target.resolve(pageFile(i)), page(doc, chapters, i, body), StandardCharsets.UTF_8);
		}
		Files.writeString(target.resolve(STYLE_FILE), STYLE, StandardCharsets.UTF_8);
	}


	/* ======================================================================
	 *                          Pages
	 * ====================================================================== */

	/** The home page is the title page. */
	private static String pageFile(final int index){
		return (index == 0? HOME: String.format(Locale.ROOT, "chapter-%03d.html", index));
	}

	/** Removes what an earlier rendering left (pages and images), not what else is in the directory. */
	private static void clean(final Path directory) throws IOException{
		try(final DirectoryStream<Path> pages = Files.newDirectoryStream(directory, "chapter-*.html")){
			for(final Path page : pages)
				Files.delete(page);
		}
		final Path images = directory.resolve(IMAGES_DIR);
		if(Files.isDirectory(images))
			try(final DirectoryStream<Path> files = Files.newDirectoryStream(images, "image-*")){
				for(final Path file : files)
					Files.delete(file);
			}
	}

	private static String body(final ReportDocument doc, final List<ReportChapter> chapters, final int index,
			final Map<String, Integer> anchors, final Path target, final Map<Path, String> images){
		final ReportChapter chapter = chapters.get(index);
		final StringBuilder sb = new StringBuilder();
		if(chapter.titlePage()){
			sb.append("<h1>").append(ReportMarkup.escape(chapter.title())).append("</h1>\n");
			if(doc.subtitle() != null && !doc.subtitle().isBlank())
				sb.append("<p class=\"subtitle\">").append(ReportMarkup.escape(ReportMarkup.plain(doc.subtitle()))).append("</p>\n");
		}
		final Set<String> writtenAnchors = new HashSet<>();
		chapter.sections().forEach(section -> ReportXhtml.writeSection(sb, section,
			//a link to a record that has no heading in the site is only its text
			id -> (anchors.containsKey(id)? pageFile(anchors.get(id)) + "#" + id: null),
			file -> copyImage(file, target, images), writtenAnchors));

		if(chapter.titlePage()){
			sb.append("<h2>").append(ReportMarkup.escape(doc.contentsTitle())).append("</h2>\n<ol>\n");
			for(int i = 1; i < chapters.size(); i ++)
				sb.append("<li><a href=\"").append(pageFile(i)).append("\">").append(ReportMarkup.escape(label(chapters.get(i))))
					.append("</a></li>\n");
			sb.append("</ol>\n");
		}
		return sb.toString();
	}

	private static String label(final ReportChapter chapter){
		return (chapter.title().isBlank()? "-": chapter.title());
	}

	private static String page(final ReportDocument doc, final List<ReportChapter> chapters, final int index, final String body){
		final String siteTitle = ReportMarkup.escape(label(chapters.getFirst()));
		final StringBuilder sb = new StringBuilder("<!DOCTYPE html>\n<html lang=\"").append(ReportMarkup.escape(doc.language())).append('"')
			.append(doc.isRightToLeft()? " dir=\"rtl\"": "").append(">\n<head><meta charset=\"utf-8\"/>")
			.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>")
			.append("<title>").append(ReportMarkup.escape(index == 0? label(chapters.getFirst()): label(chapters.get(index)) + " - "
				+ label(chapters.getFirst()))).append("</title>");
		if(doc.subtitle() != null && !doc.subtitle().isBlank())
			sb.append("<meta name=\"description\" content=\"").append(ReportMarkup.escape(ReportMarkup.plain(doc.subtitle()))).append("\"/>");
		sb.append("<link rel=\"stylesheet\" href=\"").append(STYLE_FILE).append("\"/></head>\n<body>\n");
		sb.append("<header class=\"site-header\"><a href=\"").append(HOME).append("\">").append(siteTitle).append("</a></header>\n");
		sb.append("<div class=\"layout\">\n<nav class=\"site-nav\" aria-label=\"").append(ReportMarkup.escape(doc.contentsTitle()))
			.append("\"><h2>").append(ReportMarkup.escape(doc.contentsTitle())).append("</h2>\n<ol>\n");
		for(int i = 0; i < chapters.size(); i ++)
			sb.append("<li><a href=\"").append(pageFile(i)).append('"').append(i == index? " aria-current=\"page\"": "").append('>')
				.append(ReportMarkup.escape(label(chapters.get(i)))).append("</a></li>\n");
		sb.append("</ol></nav>\n<main>\n").append(body);

		sb.append("<div class=\"pager\"><span>");
		if(index > 0)
			sb.append("<a rel=\"prev\" href=\"").append(pageFile(index - 1)).append("\">‹ ")
				.append(ReportMarkup.escape(label(chapters.get(index - 1)))).append("</a>");
		sb.append("</span><span>");
		if(index < chapters.size() - 1)
			sb.append("<a rel=\"next\" href=\"").append(pageFile(index + 1)).append("\">")
				.append(ReportMarkup.escape(label(chapters.get(index + 1)))).append(" ›</a>");
		sb.append("</span></div>\n</main>\n</div>\n</body>\n</html>\n");
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

		final String href = IMAGES_DIR + "/image-" + (images.size() + 1) + name.substring(dot).toLowerCase(Locale.ROOT);
		try{
			Files.createDirectories(target.resolve(IMAGES_DIR));
			Files.copy(key, target.resolve(href), StandardCopyOption.REPLACE_EXISTING);
			images.put(key, href);
			return href;
		}
		catch(final IOException ignored){
			return null;
		}
	}

}
