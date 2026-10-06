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
package io.github.mtrevisan.familylegacy.ui.tools.reports.renderers;

import io.github.mtrevisan.familylegacy.ui.tools.reports.ReportDocument;
import io.github.mtrevisan.familylegacy.ui.tools.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public final class TextReportRenderer implements ReportRenderer{

	@Override
	public void render(final ReportDocument doc, final Path target) throws IOException{
		try(BufferedWriter w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)){
			w.write(stripInline(doc.title()));
			w.newLine();
			w.write("=".repeat(Math.min(80, doc.title().length())));
			w.newLine();
			w.newLine();
			for(final ReportSection s : doc.sections())
				writeSection(w, s);
		}
	}

	private static void writeSection(final BufferedWriter w, final ReportSection s) throws IOException{
		if(s instanceof ReportSection.Heading h){
			w.newLine();
			w.write(stripInline(h.text()));
			w.newLine();
			w.write("-".repeat(Math.min(80, h.text().length())));
			w.newLine();
			w.newLine();
		}
		else if(s instanceof ReportSection.Paragraph(String text)){
			w.write(stripInline(text));
			w.newLine();
		}
		else if(s instanceof ReportSection.Spacer){
			w.newLine();
		}
		else if(s instanceof ReportSection.BulletList(java.util.List<String> items)){
			for(final String item : items){
				w.write("  - ");
				w.write(stripInline(item));
				w.newLine();
			}
		}
		else if(s instanceof ReportSection.Table(List<String> headers, List<List<String>> rows)){
			w.write(String.join(" | ", headers));
			w.newLine();
			w.write("-".repeat(60));
			w.newLine();
			for(final var row : rows){
				w.write(String.join(" | ", row));
				w.newLine();
			}
		}
		else if(s instanceof ReportSection.Image img){
			w.write("[image: ");
			w.write(img.file().toString());
			w.write("]");
			w.newLine();
		}
		else if(s instanceof ReportSection.HorizontalRule){
			w.write("-".repeat(60));
			w.newLine();
		}
		else if(s instanceof ReportSection.PageBreak){
			w.newLine();
		}
	}

	/** Rimuove **bold**, *italic*, `code`. */
	static String stripInline(final String s){
		return s.replace("**", StringUtils.EMPTY)
			.replace("`", StringUtils.EMPTY)
			.replaceAll("(?<!\\*)\\*(?!\\*)", StringUtils.EMPTY);
	}
}
