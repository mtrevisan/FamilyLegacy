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
import io.github.mtrevisan.reports.ReportSection;
import io.github.mtrevisan.reports.ReportXhtml;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * Renders a {@link ReportDocument} as an EPUB 3 book (a ZIP of XHTML files), with no library: a chapter for each heading of the first
 * level, a table of contents (with the headings of the second level nested in their chapter), the images inside the book.
 * <p>
 * The protocol links {@code [text](@id@)} become links between chapters, to the heading that carries the record reference
 * ({@code [I12]}); inline Markdown ({@code **bold**}, {@code *italic*}, {@code `code`}) is converted as in the HTML renderer. A page
 * break has no meaning in a reflowable book and is ignored. The language of the report is the one of the book.
 * <p>
 * The identifier of the book depends on the report (title, subtitle, language), not on the moment of the export, so that a reader
 * recognizes a regenerated book as the same one (and keeps its position). The file is written aside and then moved in place: a failure
 * never leaves a truncated book.
 */
public final class EpubReportRenderer implements ReportRenderer{

	private static final String MIME_TYPE = "application/epub+zip";
	private static final String PUBLISHER = "FamilyLegacy";
	private static final String STYLE = "body{font-family:Georgia,serif;line-height:1.5;}"
		+ "h1,h2,h3,h4,h5,h6{page-break-after:avoid;break-after:avoid;}"
		+ "h1,h2,h3{border-bottom:1px solid #ccc;padding-bottom:.2em;}"
		+ "h4,h5,h6{border-bottom:1px dotted #ccc;}"
		+ ".subtitle{font-style:italic;color:#555;}"
		+ "table{border-collapse:collapse;width:100%;table-layout:fixed;margin:.5em 0;}"
		+ "td,th{border:1px solid #ccc;padding:2px 6px;text-align:left;word-wrap:break-word;overflow-wrap:break-word;}"
		+ "pre{white-space:pre-wrap;font-family:monospace;}"
		+ "figure{margin:1em 0;}img{max-width:100%;}figcaption{font-size:.85em;color:#555;}"
		+ "a{color:#1f3864;}";

	//WebP is not a core media type of EPUB 3 (it needs a fallback): not accepted
	private static final Map<String, String> IMAGE_TYPES = Map.of(
		"png", "image/png",
		"jpg", "image/jpeg",
		"jpeg", "image/jpeg",
		"gif", "image/gif",
		"svg", "image/svg+xml");

	private static final String CONTAINER = """
		<?xml version="1.0" encoding="UTF-8"?>
		<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">\
		<rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>
		""";

	/** The headings of this level are listed, under their chapter, in the table of contents. */
	private static final int SECTION_LEVEL = ReportChapter.CHAPTER_LEVEL + 1;


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
		final String[] files = new String[chapters.size()];
		for(int i = 0; i < files.length; i ++)
			files[i] = chapterFile(i);
		final Images images = new Images();

		//the headings of the second level, that the table of contents links
		final Map<ReportSection, String> headingIds = new IdentityHashMap<>();
		final List<List<TocEntry>> sections = new ArrayList<>();
		for(int i = 0; i < chapters.size(); i ++){
			final List<TocEntry> entries = new ArrayList<>();
			for(final ReportSection section : chapters.get(i).sections())
				if(section instanceof ReportSection.Heading heading && heading.level() == SECTION_LEVEL){
					final String title = ReportMarkup.withoutRecordIds(ReportMarkup.plain(heading.text()));
					if(title.isBlank())
						continue;

					final String id = "section-" + (i + 1) + "-" + (entries.size() + 1);
					headingIds.put(heading, id);
					entries.add(new TocEntry(title, files[i] + "#" + id));
				}
			sections.add(entries);
		}

		final List<String> pages = new ArrayList<>(chapters.size());
		for(final ReportChapter chapter : chapters)
			pages.add(writeChapter(doc, chapter, anchors, files, headingIds, images));

		final Instant now = Instant.now()
			.truncatedTo(ChronoUnit.SECONDS);
		final Path folder = target.toAbsolutePath()
			.getParent();
		final Path temporary = Files.createTempFile(folder, ".epub-", ".tmp");
		try{
			try(final OutputStream out = Files.newOutputStream(temporary);
					final ZipOutputStream zip = new ZipOutputStream(out)){
				writeMimeType(zip);
				writeEntry(zip, "META-INF/container.xml", CONTAINER);
				writeEntry(zip, "OEBPS/style.css", STYLE);
				for(int i = 0; i < chapters.size(); i ++)
					writeEntry(zip, "OEBPS/" + files[i], pages.get(i));
				for(final Image image : images.all())
					writeBytes(zip, "OEBPS/" + image.href(), image.data());
				writeEntry(zip, "OEBPS/nav.xhtml", navigation(doc, chapters, files, sections));
				writeEntry(zip, "OEBPS/content.opf", packageDocument(doc, chapters, files, images.all(), now));
			}
			move(temporary, target);
		}
		finally{
			Files.deleteIfExists(temporary);
		}
	}

	private static void move(final Path source, final Path target) throws IOException{
		try{
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		}
		catch(final AtomicMoveNotSupportedException ignored){
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}


	/* ======================================================================
	 *                          Chapters
	 * ====================================================================== */

	private record Image(String href, String mediaType, byte[] data){}

	private record TocEntry(String title, String href){}

	/** The images of the book: the same file, or two files with the same bytes, are in the book once; an unreadable one is tried once. */
	private static final class Images{

		private final Map<Path, Image> byPath = new HashMap<>();
		private final Map<String, Image> byContent = new LinkedHashMap<>();
		private final Set<Path> unusable = new HashSet<>();


		Collection<Image> all(){
			return byContent.values();
		}

		/** @return	The image as it is in the book, {@code null} if the file cannot be used (missing, unreadable, of an unknown format). */
		Image register(final Path file){
			if(file == null)
				return null;

			final Path key = file.toAbsolutePath()
				.normalize();
			final Image known = byPath.get(key);
			if(known != null)
				return known;
			if(unusable.contains(key))
				return null;

			final String name = key.getFileName()
				.toString();
			final int dot = name.lastIndexOf('.');
			final String extension = (dot >= 0? name.substring(dot + 1).toLowerCase(Locale.ROOT): "");
			final String mediaType = IMAGE_TYPES.get(extension);
			if(mediaType == null){
				unusable.add(key);
				return null;
			}

			try{
				final byte[] data = Files.readAllBytes(key);
				final String digest = sha256(data);
				Image image = byContent.get(digest);
				if(image == null){
					image = new Image("images/image-" + (byContent.size() + 1) + "." + extension, mediaType, data);
					byContent.put(digest, image);
				}
				byPath.put(key, image);
				return image;
			}
			catch(final IOException ignored){
				unusable.add(key);
				return null;
			}
		}

		private static String sha256(final byte[] data){
			try{
				return HexFormat.of()
					.formatHex(MessageDigest.getInstance("SHA-256").digest(data));
			}
			catch(final NoSuchAlgorithmException nsae){
				//every Java platform has SHA-256
				throw new IllegalStateException(nsae);
			}
		}

	}


	private static String chapterFile(final int index){
		return String.format(Locale.ROOT, "chapter-%03d.xhtml", index + 1);
	}

	private static String writeChapter(final ReportDocument doc, final ReportChapter chapter, final Map<String, Integer> anchors,
			final String[] files, final Map<ReportSection, String> headingIds, final Images images){
		final StringBuilder sb = new StringBuilder();
		if(chapter.titlePage()){
			sb.append("<h1>").append(ReportMarkup.escape(chapter.title())).append("</h1>\n");
			if(doc.subtitle() != null && !doc.subtitle().isBlank())
				sb.append("<p class=\"subtitle\">").append(ReportMarkup.escape(ReportMarkup.plain(doc.subtitle()))).append("</p>\n");
		}
		//a link to a record that has no heading in the book is only its text
		final Set<String> writtenAnchors = new HashSet<>();
		chapter.sections().forEach(section -> ReportXhtml.writeSection(sb, section,
			id -> (anchors.containsKey(id)? files[anchors.get(id)] + "#" + id: null),
			file -> {
				final Image image = images.register(file);
				return (image != null? image.href(): null);
			}, writtenAnchors, headingIds));
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


	/* ======================================================================
	 *                          Package
	 * ====================================================================== */

	private static String navigation(final ReportDocument doc, final List<ReportChapter> chapters, final String[] files,
			final List<List<TocEntry>> sections){
		final StringBuilder sb = new StringBuilder("<nav epub:type=\"toc\" id=\"toc\"><h1>")
			.append(ReportMarkup.escape(doc.contentsTitle())).append("</h1><ol>");
		for(int i = 0; i < chapters.size(); i ++){
			final String title = chapters.get(i)
				.title();
			sb.append("<li><a href=\"").append(files[i]).append("\">")
				.append(ReportMarkup.escape(title.isBlank()? "-": title)).append("</a>");
			final List<TocEntry> entries = sections.get(i);
			if(!entries.isEmpty()){
				sb.append("<ol>");
				for(final TocEntry entry : entries)
					sb.append("<li><a href=\"").append(ReportMarkup.escape(entry.href())).append("\">")
						.append(ReportMarkup.escape(entry.title())).append("</a></li>");
				sb.append("</ol>");
			}
			sb.append("</li>");
		}
		sb.append("</ol></nav>\n");

		//the first chapter after the title page is where the book begins
		final int body = (chapters.size() > 1? 1: 0);
		final String bodyTitle = chapters.get(body)
			.title();
		sb.append("<nav epub:type=\"landmarks\" hidden=\"hidden\"><ol>")
			.append("<li><a epub:type=\"toc\" href=\"nav.xhtml#toc\">").append(ReportMarkup.escape(doc.contentsTitle())).append("</a></li>")
			.append("<li><a epub:type=\"bodymatter\" href=\"").append(files[body]).append("\">")
			.append(ReportMarkup.escape(bodyTitle.isBlank()? "-": bodyTitle)).append("</a></li>")
			.append("</ol></nav>\n");
		return xhtml(doc, ReportMarkup.plain(doc.title()), sb.toString());
	}

	private static String packageDocument(final ReportDocument doc, final List<ReportChapter> chapters, final String[] files,
			final Collection<Image> images, final Instant now){
		final String title = ReportMarkup.plain(doc.title());
		final String subtitle = (doc.subtitle() == null? "": doc.subtitle());
		final UUID identifier = UUID.nameUUIDFromBytes((title + "\n" + subtitle + "\n" + doc.language()).getBytes(StandardCharsets.UTF_8));
		final StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" unique-identifier=\"book-id\" xml:lang=\""
			+ ReportMarkup.escape(doc.language()) + "\">\n<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n");
		sb.append("<dc:identifier id=\"book-id\">urn:uuid:").append(identifier).append("</dc:identifier>\n");
		sb.append("<dc:title>").append(ReportMarkup.escape(title.isBlank()? "-": title)).append("</dc:title>\n");
		sb.append("<dc:language>").append(ReportMarkup.escape(doc.language())).append("</dc:language>\n");
		sb.append("<dc:publisher>").append(PUBLISHER).append("</dc:publisher>\n");
		sb.append("<dc:date>").append(LocalDate.ofInstant(now, ZoneOffset.UTC)).append("</dc:date>\n");
		if(!subtitle.isBlank())
			sb.append("<dc:description>").append(ReportMarkup.escape(ReportMarkup.plain(subtitle))).append("</dc:description>\n");
		sb.append("<meta property=\"dcterms:modified\">").append(now).append("</meta>\n");
		sb.append("</metadata>\n<manifest>\n");
		sb.append("<item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>\n");
		sb.append("<item id=\"style\" href=\"style.css\" media-type=\"text/css\"/>\n");
		for(int i = 0; i < chapters.size(); i ++)
			sb.append("<item id=\"chapter-").append(i + 1).append("\" href=\"").append(files[i])
				.append("\" media-type=\"application/xhtml+xml\"/>\n");
		int index = 0;
		for(final Image image : images)
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
