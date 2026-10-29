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
package io.github.mtrevisan.familylegacy.ui.assistant.citation;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RepositoryReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/** Where the assistant looks for clues about a citation: the media of the cited source and the repositories of the model. */
final class CitationHints{

	/**
	 * A locator suggested by the name or the description of a media of the source.
	 *
	 * @param origin  where the clue was found
	 * @param locator the locator read from it
	 */
	record Hint(String origin, String locator){}


	private CitationHints(){}


	/**
	 * Reads the file names and the descriptions of the media of a source as archival references
	 * (<em>film1234567_frame245.jpg</em> gives <em>Film: 1234567, Frame: 245</em>).
	 *
	 * @param model  the model
	 * @param source the cited source (can be {@code null})
	 * @return the clues found, without duplicates
	 */
	static List<Hint> mediaHints(final FLEFModel model, final FLEFRecord source){
		final List<Hint> hints = new ArrayList<>(0);
		if(model == null || source == null)
			return hints;

		final List<FLEFRecord> media = FLEFRecordHelper.extractRecordsFromReference(source, SourceReader.TAG_MEDIA, model);
		for(int i = 0, size = media.size(); i < size; i ++){
			final FLEFRecord medium = media.get(i);

			add(hints, fileStem(FLEFRecordHelper.getChildValue(medium, MediaReader.TAG_URI)));
			add(hints, FLEFRecordHelper.getChildValue(medium, MediaReader.TAG_DESCRIPTION));
		}
		return hints;
	}

	private static void add(final List<Hint> hints, final String text){
		if(StringUtils.isBlank(text))
			return;

		final String locator = ArchivalReferenceParser.parse(separate(text)).locator();
		if(!locator.isEmpty() && hints.stream().noneMatch(hint -> hint.locator().equals(locator)))
			hints.add(new Hint(text.strip(), locator));
	}

	private static String fileStem(final String uri){
		if(StringUtils.isBlank(uri))
			return null;

		String name = uri.replace('\\', '/');
		name = name.substring(name.lastIndexOf('/') + 1);
		final int dot = name.lastIndexOf('.');
		return (dot > 0? name.substring(0, dot): name);
	}

	/** Separates what a file name glues together: underscores, hyphens and the boundaries between letters and digits. */
	private static String separate(final String text){
		return text.replace('_', ' ')
			.replaceAll("(?<=\\p{L})(?=\\d)|(?<=\\d)(?=\\p{L})", " ")
			.replaceAll("\\s+", " ")
			.strip();
	}


	/**
	 * The repositories of the model that are named by one of the fragments, by name or by acronym.
	 *
	 * @param model     the model
	 * @param fragments the text of the reference that was not understood
	 * @return the repositories that match, each once
	 */
	static List<FLEFRecord> repositoryMatches(final FLEFModel model, final List<String> fragments){
		final List<FLEFRecord> matches = new ArrayList<>(0);
		if(model == null || fragments.isEmpty())
			return matches;

		final List<FLEFRecord> repositories = model.getRecordsByType(RepositoryHandler.TYPE);
		for(int i = 0, size = repositories.size(); i < size; i ++){
			final FLEFRecord repository = repositories.get(i);

			final List<String> names = RepositoryReader.extractNames(repository);
			if(fragments.stream().anyMatch(fragment -> names.stream().anyMatch(name -> matches(fragment, name))))
				matches.add(repository);
		}
		return matches;
	}

	private static boolean matches(final String fragment, final String name){
		final String text = fragment.strip().toLowerCase(Locale.ROOT);
		if(text.length() < 2)
			return false;

		return (text.equals(name.strip().toLowerCase(Locale.ROOT)) || text.equals(acronym(name)));
	}

	/** The initials of the words of the name, from the ones that begin with an upper case letter: <em>Archivio di Stato di Venezia</em> is <em>asv</em>. */
	static String acronym(final String name){
		final StringBuilder sb = new StringBuilder();
		for(final String word : name.strip().split("[\\s,.'-]+"))
			if(!word.isEmpty() && Character.isUpperCase(word.charAt(0)))
				sb.append(Character.toLowerCase(word.charAt(0)));
		return sb.toString();
	}

}
