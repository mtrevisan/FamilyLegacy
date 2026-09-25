package io.github.mtrevisan.familylegacy.v2.ui.tools.reports.renderers;

import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.ReportDocument;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.ReportSection;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


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
		else if(s instanceof ReportSection.Paragraph p){
			w.write(stripInline(p.text()));
			w.newLine();
		}
		else if(s instanceof ReportSection.Spacer){
			w.newLine();
		}
		else if(s instanceof ReportSection.BulletList b){
			for(final String item : b.items()){
				w.write("  - ");
				w.write(stripInline(item));
				w.newLine();
			}
		}
		else if(s instanceof ReportSection.Table t){
			w.write(String.join(" | ", t.headers()));
			w.newLine();
			w.write("-".repeat(60));
			w.newLine();
			for(final var row : t.rows()){
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
		return s.replace("**", "").replace("`", "").replaceAll("(?<!\\*)\\*(?!\\*)", "");
	}
}
