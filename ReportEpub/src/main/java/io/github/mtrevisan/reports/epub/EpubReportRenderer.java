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
package io.github.mtrevisan.reports.epub;

import io.github.mtrevisan.reports.ReportChapter;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportMarkup;
import io.github.mtrevisan.reports.ReportRenderer;
import io.github.mtrevisan.reports.ReportXhtml;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * Renders a {@link ReportDocument} as an EPUB 3 book (a ZIP of XHTML files), with no library: a chapter for each heading of the first
 * level, a table of contents, the images inside the book.
 * <p>
 * The protocol links {@code [text](@id@)} become links between chapters, to the heading that carries the record reference
 * ({@code [I12]}); inline Markdown ({@code **bold**}, {@code *italic*}, {@code `code`}) is converted as in the HTML renderer. A page
 * break has no meaning in a reflowable book and is ignored. The language of the report is the one of the book.
 */
public final class EpubReportRenderer implements ReportRenderer{

	private static final String MIME_TYPE = "application/epub+zip";
	private static final String STYLE = "body{font-family:Georgia,serif;line-height:1.5;margin:1em;}"
		+ "h1,h2,h3{border-bottom:1px solid #ccc;padding-bottom:.2em;}"
		+ "table{border-collapse:collapse;width:100%;margin:.5em 0;}"
		+ "td,th{border:1px solid #ccc;padding:2px 6px;text-align:left;}"
		+ "pre{white-space:pre-wrap;font-family:monospace;}"
		+ "figure{margin:1em 0;}img{max-width:100%;}figcaption{font-size:.85em;color:#555;}"
		+ "a{color:#1f3864;text-decoration:none;}";

	private static final Map<String, String> IMAGE_TYPES = Map.of(
		"png", "image/png",
		"jpg", "image/jpeg",
		"jpeg", "image/jpeg",
		"gif", "image/gif",
		"svg", "image/svg+xml",
		"webp", "image/webp");

	private static final String CONTAINER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
		+ "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">"
		+ "<rootfiles><rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/></rootfiles></container>\n";


	@Override
	public String id(){
		return "epub";
	}

	@Override
	public String displayName(){
		return "EPUB";
	}

	@Override
	public String extension(){
		return ".epub";
	}

	@Override
	public void render(final ReportDocument doc, final Path target) throws IOException{
		final List<ReportChapter> chapters = ReportChapter.split(doc);
		final Map<String, Integer> anchors = ReportChapter.anchors(chapters);
		final Map<Path, Image> images = new LinkedHashMap<>();

		final List<String> pages = chapters.stream()
			.map(chapter -> writeChapter(doc, chapter, anchors, images))
			.toList();

		try(final OutputStream out = Files.newOutputStream(target);
				final ZipOutputStream zip = new ZipOutputStream(out)){
			writeMimeType(zip);
			writeEntry(zip, "META-INF/container.xml", CONTAINER);
			writeEntry(zip, "OEBPS/style.css", STYLE);
			for(int i = 0; i < chapters.size(); i ++)
				writeEntry(zip, "OEBPS/" + chapterFile(i), pages.get(i));
			for(final Image image : images.values())
				writeBytes(zip, "OEBPS/" + image.href(), image.data());
			writeEntry(zip, "OEBPS/nav.xhtml", navigation(doc, chapters));
			writeEntry(zip, "OEBPS/content.opf", packageDocument(doc, chapters, images));
		}
	}


	/* ======================================================================
	 *                          Chapters
	 * ====================================================================== */

	private record Image(String href, String mediaType, byte[] data){}


	private static String chapterFile(final int index){
		return String.format(Locale.ROOT, "chapter-%03d.xhtml", index + 1);
	}

	private static String writeChapter(final ReportDocument doc, final ReportChapter chapter, final Map<String, Integer> anchors,
			final Map<Path, Image> images){
		final StringBuilder sb = new StringBuilder();
		if(chapter.titlePage())
			sb.append("<h1>").append(ReportMarkup.escape(chapter.title())).append("</h1>\n");
		//a link to a record that has no heading in the book is only its text
		chapter.sections().forEach(section -> ReportXhtml.writeSection(sb, section,
			id -> (anchors.containsKey(id)? chapterFile(anchors.get(id)) + "#" + id: null),
			file -> {
				final Image image = register(file, images);
				return (image != null? image.href(): null);
			}));
		return xhtml(doc, chapter.title(), sb.toString());
	}

	private static String xhtml(final ReportDocument doc, final String title, final String body){
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\" lang=\""
			+ ReportMarkup.escape(doc.language()) + "\" xml:lang=\"" + ReportMarkup.escape(doc.language()) + "\""
			+ (doc.isRightToLeft()? " dir=\"rtl\"": "")
			+ ">\n<head><meta charset=\"utf-8\"/><title>" + ReportMarkup.escape(title.isBlank()? "-": title)
			+ "</title><link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\"/></head>\n<body>\n" + body + "</body>\n</html>\n";
	}

	private static Image register(final Path file, final Map<Path, Image> images){
		if(file == null)
			return null;

		final Path key = file.toAbsolutePath().normalize();
		if(images.containsKey(key))
			return images.get(key);

		final String name = key.getFileName().toString();
		final int dot = name.lastIndexOf('.');
		final String mediaType = (dot >= 0? IMAGE_TYPES.get(name.substring(dot + 1).toLowerCase(Locale.ROOT)): null);
		if(mediaType == null)
			return null;

		try{
			final Image image = new Image("images/image-" + (images.size() + 1) + name.substring(dot).toLowerCase(Locale.ROOT),
				mediaType, Files.readAllBytes(key));
			images.put(key, image);
			return image;
		}
		catch(final IOException ignored){
			return null;
		}
	}


	/* ======================================================================
	 *                          Package
	 * ====================================================================== */

	private static String navigation(final ReportDocument doc, final List<ReportChapter> chapters){
		final StringBuilder sb = new StringBuilder("<nav epub:type=\"toc\" id=\"toc\"><h1>")
			.append(ReportMarkup.escape(doc.contentsTitle())).append("</h1><ol>");
		for(int i = 0; i < chapters.size(); i ++){
			final String title = chapters.get(i).title();
			sb.append("<li><a href=\"").append(chapterFile(i)).append("\">")
				.append(ReportMarkup.escape(title.isBlank()? "-": title)).append("</a></li>");
		}
		sb.append("</ol></nav>\n");
		return xhtml(doc, ReportMarkup.plain(doc.title()), sb.toString());
	}

	private static String packageDocument(final ReportDocument doc, final List<ReportChapter> chapters, final Map<Path, Image> images){
		final String title = ReportMarkup.plain(doc.title());
		final StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" unique-identifier=\"book-id\" xml:lang=\""
			+ ReportMarkup.escape(doc.language()) + "\">\n<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n");
		sb.append("<dc:identifier id=\"book-id\">urn:uuid:").append(UUID.randomUUID()).append("</dc:identifier>\n");
		sb.append("<dc:title>").append(ReportMarkup.escape(title.isBlank()? "-": title)).append("</dc:title>\n");
		sb.append("<dc:language>").append(ReportMarkup.escape(doc.language())).append("</dc:language>\n");
		if(doc.subtitle() != null && !doc.subtitle().isBlank())
			sb.append("<dc:description>").append(ReportMarkup.escape(ReportMarkup.plain(doc.subtitle()))).append("</dc:description>\n");
		sb.append("<meta property=\"dcterms:modified\">").append(Instant.now().truncatedTo(ChronoUnit.SECONDS)).append("</meta>\n");
		sb.append("</metadata>\n<manifest>\n");
		sb.append("<item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>\n");
		sb.append("<item id=\"style\" href=\"style.css\" media-type=\"text/css\"/>\n");
		for(int i = 0; i < chapters.size(); i ++)
			sb.append("<item id=\"chapter-").append(i + 1).append("\" href=\"").append(chapterFile(i))
				.append("\" media-type=\"application/xhtml+xml\"/>\n");
		int index = 0;
		for(final Image image : images.values())
			sb.append("<item id=\"image-").append(++ index).append("\" href=\"").append(ReportMarkup.escape(image.href()))
				.append("\" media-type=\"").append(image.mediaType()).append("\"/>\n");
		sb.append("</manifest>\n<spine").append(doc.isRightToLeft()? " page-progression-direction=\"rtl\"": "").append(">\n");
		for(int i = 0; i < chapters.size(); i ++)
			sb.append("<itemref idref=\"chapter-").append(i + 1).append("\"/>\n");
		sb.append("</spine>\n</package>\n");
		return sb.toString();
	}


	/* ======================================================================
	 *                          ZIP
	 * ====================================================================== */

	/** The first entry of an EPUB is the stored (not compressed) {@code mimetype}, so that the type of the file can be told by its bytes. */
	private static void writeMimeType(final ZipOutputStream zip) throws IOException{
		final byte[] data = MIME_TYPE.getBytes(StandardCharsets.US_ASCII);
		final CRC32 crc = new CRC32();
		crc.update(data);

		final ZipEntry entry = new ZipEntry("mimetype");
		entry.setMethod(ZipEntry.STORED);
		entry.setSize(data.length);
		entry.setCompressedSize(data.length);
		entry.setCrc(crc.getValue());
		zip.putNextEntry(entry);
		zip.write(data);
		zip.closeEntry();
	}

	private static void writeEntry(final ZipOutputStream zip, final String name, final String content) throws IOException{
		writeBytes(zip, name, content.getBytes(StandardCharsets.UTF_8));
	}

	private static void writeBytes(final ZipOutputStream zip, final String name, final byte[] data) throws IOException{
		zip.putNextEntry(new ZipEntry(name));
		zip.write(data);
		zip.closeEntry();
	}

}
