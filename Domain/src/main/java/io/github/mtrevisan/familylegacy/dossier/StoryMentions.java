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
package io.github.mtrevisan.familylegacy.dossier;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.NoteReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The stories (the notes of kind story) that name a record: a story is about the record it is written in, and links to the records it
 * speaks of ({@code [Giovanni](flef:I12)}); this is the other direction, to find the stories that speak of a record.
 * <p>
 * The index of the model is made once and kept until the model changes (see {@link FLEFModel#derived}).
 */
public final class StoryMentions{

	/** A story, and the record it is written in. */
	public record Mention(FLEFRecord owner, FLEFRecord story){}


	private static final Object KEY = new Object();
	/** What a link to a record of the file is: {@code flef:<Id>} (the one that marks confidential text is not a record). */
	private static final Pattern LINK = Pattern.compile("flef:([^)\\s\"'<>\\]]+)");
	private static final String CONFIDENTIAL = "confidential";


	private StoryMentions(){}


	/**
	 * @param model	The model.
	 * @param recordId	The identifier of a record.
	 * @return	The stories that link to the record, in the order of the file; the ones written in the record itself are not among them.
	 */
	public static List<Mention> of(final FLEFModel model, final String recordId){
		if(model == null || recordId == null)
			return List.of();

		final Map<String, List<Mention>> index = model.derived(KEY, () -> build(model));
		final List<Mention> mentions = new ArrayList<>(index.getOrDefault(recordId, List.of()));
		mentions.removeIf(mention -> recordId.equals(mention.owner().getId()));
		return mentions;
	}

	private static Map<String, List<Mention>> build(final FLEFModel model){
		final Map<String, List<Mention>> index = new HashMap<>();
		for(final FLEFRecord record : model.getRecords())
			collect(record, record, index);
		return index;
	}

	private static void collect(final FLEFRecord node, final FLEFRecord owner, final Map<String, List<Mention>> index){
		for(final FLEFRecord child : node.getChildren()){
			if(NoteReader.TYPE.equalsIgnoreCase(child.getTag()) && NoteReader.isStory(child)){
				final String text = NoteReader.extractText(child);
				if(text != null && text.contains("flef:")){
					final Set<String> seen = new HashSet<>();
					final Matcher matcher = LINK.matcher(text);
					while(matcher.find()){
						final String id = matcher.group(1);
						if(!CONFIDENTIAL.equalsIgnoreCase(id) && seen.add(id))
							index.computeIfAbsent(id, k -> new ArrayList<>())
								.add(new Mention(owner, child));
					}
				}
			}
			collect(child, owner, index);
		}
	}

}
