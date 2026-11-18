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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.preferences.AppPreferences;
import io.github.mtrevisan.familylegacy.util.AtomicFileWriter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;


/**
 * What the user does with the findings of the assistant, as counts: how many findings of each kind were listed, and how many were acted
 * on, opened, shown in the main window, ignored (alone, by kind, by record), put aside or shown again. It is how the numbers that the
 * assistants use as conventions (the weights of the usefulness, the limits of the levels) can be calibrated on real research instead of
 * guessed.
 * <p>
 * It collects <b>nothing about the research</b>: no name, date, identifier or text of a finding, only its kind (the same in every language:
 * {@code gap.no_death_certain}), what was done, and the position it had in the list. It is off until the user turns it on, it is kept
 * in a plain text file that the user can read, copy and delete, and it is never sent anywhere: the user decides to give it to somebody.
 * </p>
 * <p>
 * The counts are cumulative: what the file holds is added to when the application starts, so they add up over the runs. They are kept
 * for each set of numbers the assistants had (a digest of {@link AssistantTuning}, with the numbers that are not the defaults), because
 * counts made with other weights say something else.
 * </p>
 */
public final class AssistantStatistics{

	private static final Logger LOGGER = LoggerFactory.getLogger(AssistantStatistics.class);

	/** What the user did with a finding. */
	public enum Event{
		/** The finding was in the list when the palette opened. */
		SHOWN,
		/** One of its actions was run (a draft, the comparison, the opening of the records...). */
		ACTED,
		/** A record of the finding was opened from its detail. */
		OPENED,
		/** The individual of the finding was shown in the main window. */
		VIEWED,
		/** The finding was ignored. */
		IGNORED,
		/** Every finding of its kind was ignored. */
		IGNORED_KIND,
		/** Every finding about its first record was ignored. */
		IGNORED_RECORD,
		/** The finding was put aside for a while. */
		SNOOZED,
		/** The finding was shown again. */
		RESTORED
	}

	/** Where the text is kept. */
	public interface Store{

		/** @return the text kept, or {@code null} if there is none */
		String read() throws IOException;

		void write(String text) throws IOException;

	}

	/** The counts of the findings of a kind, with the numbers the assistants had. */
	private static final class Counts{

		private final long[] counts = new long[Event.values().length];
		/** The sum of the positions in the list of the findings acted on, and how many were (to tell how high in the list they are). */
		private long positionSum;
		private long positionCount;

	}

	/** The first line of the file. */
	static final String HEADER = "FamilyLegacy assistant statistics 1";
	private static final String TUNING = "tuning";
	private static final String KIND = "kind";
	private static final String DEFAULTS = "defaults";
	private static final int DIGEST_CHARS = 8;
	/** How long the counts are kept in memory before they are written (milliseconds). */
	private static final long FLUSH_DELAY_MILLIS = 30_000L;

	private final Store store;
	private final BooleanSupplier enabled;
	private final Supplier<AssistantTuning> tuning;
	/** By the digest of the numbers, then by the kind of the finding (sorted: the file is always in the same order). */
	private final Map<String, Map<String, Counts>> counts = new TreeMap<>();
	/** The numbers (the ones that are not the defaults) of each digest. */
	private final Map<String, String> tunings = new TreeMap<>();
	private boolean loaded;
	private boolean dirty;
	private boolean scheduled;
	private final boolean timed;


	/**
	 * @param store   where the text is kept
	 * @param enabled whether the user wants the counts (checked at every count: it can be turned off meanwhile)
	 * @param tuning  gives the numbers the assistants have now
	 * @param timed   whether the counts are written by themselves a while after the last one (and when the application ends)
	 */
	public AssistantStatistics(final Store store, final BooleanSupplier enabled, final Supplier<AssistantTuning> tuning,
			final boolean timed){
		this.store = store;
		this.enabled = enabled;
		this.tuning = tuning;
		this.timed = timed;
		if(timed)
			Runtime.getRuntime().addShutdownHook(new Thread(this::flush, "assistant-statistics"));
	}

	/** @return the statistics kept in a file next to the preferences of the user */
	public static AssistantStatistics usingFile(final AssistantSettings settings){
		return new AssistantStatistics(new FileStore(file()), settings::isStatisticsEnabled, settings::tuning, true);
	}

	/** @return the statistics that keep nothing outside of memory (the tests must not write the files of the user) */
	public static AssistantStatistics inMemory(final BooleanSupplier enabled, final Supplier<AssistantTuning> tuning){
		return new AssistantStatistics(new Store(){
			private String kept;

			@Override
			public String read(){
				return kept;
			}

			@Override
			public void write(final String text){
				kept = text;
			}
		}, enabled, tuning, false);
	}

	/** @return the file of the statistics: {@code assistant-statistics.txt} in the folder of the preferences */
	public static Path file(){
		return AppPreferences.filePath().resolveSibling("assistant-statistics.txt");
	}


	/** @return whether the user wants the counts */
	public boolean isEnabled(){
		return enabled.getAsBoolean();
	}

	/**
	 * Counts something the user did with a finding.
	 *
	 * @param kind     the kind of the finding ({@link Diagnostic#kind()}: the same in every language, with no data in it)
	 * @param event    what was done
	 * @param position where the finding was in the list (0 for the first), or a negative number if it does not matter or is not known
	 */
	public void record(final String kind, final Event event, final int position){
		add(kind, event, 1L, position);
	}

	/** Counts many findings of the same kind at once (the findings of a kind that the palette lists). */
	public void add(final String kind, final Event event, final long amount, final int position){
		if(kind == null || amount <= 0L || !enabled.getAsBoolean())
			return;

		synchronized(this){
			load();
			final Counts counts = counts(kind);
			counts.counts[event.ordinal()] += amount;
			if(event == Event.ACTED && position >= 0){
				counts.positionSum += position;
				counts.positionCount ++;
			}
			dirty = true;
			schedule();
		}
	}

	private Counts counts(final String kind){
		final AssistantTuning current = tuning.get();
		final String digest = digest(current);
		tunings.computeIfAbsent(digest, key -> describe(current));
		return counts.computeIfAbsent(digest, key -> new TreeMap<>())
			.computeIfAbsent(kind, key -> new Counts());
	}

	/** @return how many times a kind of finding got something done, with the numbers the assistants have now (for the tests, and the page) */
	public synchronized long count(final String kind, final Event event){
		load();
		final Map<String, Counts> ofKinds = counts.get(digest(tuning.get()));
		final Counts found = (ofKinds != null? ofKinds.get(kind): null);
		return (found != null? found.counts[event.ordinal()]: 0L);
	}

	/** What was counted for a kind of finding. */
	public record Row(String kind, Map<Event, Long> counts, double averagePosition){

		/** @return how many times the kind of finding got this done */
		public long count(final Event event){
			return counts.getOrDefault(event, 0L);
		}

		/** @return how many findings were ignored, alone, by kind or by record */
		public long ignored(){
			return count(Event.IGNORED) + count(Event.IGNORED_KIND) + count(Event.IGNORED_RECORD);
		}

		/** @return the share of the findings listed that were acted on (0 to 1), or a negative number if none was listed */
		public double actedShare(){
			final long shown = count(Event.SHOWN);
			return (shown > 0L? (double)count(Event.ACTED) / shown: -1.);
		}

	}

	/** @return the counts of each kind of finding with the numbers the assistants have now, the kinds in order */
	public synchronized List<Row> rows(){
		load();
		final Map<String, Counts> ofKinds = counts.get(digest(tuning.get()));
		final List<Row> rows = new ArrayList<>();
		if(ofKinds != null)
			ofKinds.forEach((kind, found) -> {
				final Map<Event, Long> byEvent = new java.util.EnumMap<>(Event.class);
				for(final Event event : Event.values())
					byEvent.put(event, found.counts[event.ordinal()]);
				rows.add(new Row(kind, byEvent, (found.positionCount > 0L? (double)found.positionSum / found.positionCount: -1.)));
			});
		return rows;
	}

	/** @return how many sets of numbers of the assistants the counts were made with (the ones that are not the current ones are in the file too) */
	public synchronized int numberOfSets(){
		load();
		return counts.size();
	}

	/** @return the average position in the list of the findings of a kind that were acted on, or a negative number if none was */
	public synchronized double averagePosition(final String kind){
		load();
		final Map<String, Counts> ofKinds = counts.get(digest(tuning.get()));
		final Counts found = (ofKinds != null? ofKinds.get(kind): null);
		return (found != null && found.positionCount > 0L? (double)found.positionSum / found.positionCount: -1.);
	}


	/** Writes the counts, if they changed since they were written. It is called by itself a while after the last count and when the application ends. */
	public synchronized void flush(){
		if(!dirty)
			return;

		try{
			store.write(toText());
			dirty = false;
		}
		catch(final IOException ioe){
			LOGGER.warn("Cannot write the statistics of the assistant: {}", ioe.toString());
		}
	}

	/** @return the counts as text, the same that is in the file: to copy it and give it to somebody */
	public synchronized String export(){
		load();
		return toText();
	}

	/** Forgets every count, in memory and in the file. */
	public synchronized void reset(){
		counts.clear();
		tunings.clear();
		loaded = true;
		dirty = true;
		flush();
	}


	private void schedule(){
		if(!timed || scheduled)
			return;

		scheduled = true;
		final Thread thread = new Thread(() -> {
			try{
				Thread.sleep(FLUSH_DELAY_MILLIS);
			}
			catch(final InterruptedException ie){
				Thread.currentThread().interrupt();
			}
			synchronized(this){
				scheduled = false;
			}
			flush();
		}, "assistant-statistics-writer");
		thread.setDaemon(true);
		thread.start();
	}

	/** Reads what the file holds, the first time it is needed: the counts add up over the runs. */
	private void load(){
		if(loaded)
			return;

		loaded = true;
		try{
			final String text = store.read();
			if(text != null)
				parse(text);
		}
		catch(final IOException | RuntimeException e){
			//a file that cannot be read is started again, never an obstacle
			LOGGER.warn("Cannot read the statistics of the assistant: {}", e.toString());
		}
	}

	private void parse(final String text){
		final String[] lines = StringUtils.split(text, "\r\n");
		if(lines == null || lines.length == 0 || !HEADER.equals(lines[0].trim()))
			return;

		for(int i = 1; i < lines.length; i ++){
			final String[] parts = StringUtils.split(lines[i].trim(), ' ');
			if(parts == null || parts.length < 3)
				continue;

			if(TUNING.equals(parts[0]))
				tunings.putIfAbsent(parts[1], StringUtils.join(Arrays.copyOfRange(parts, 2, parts.length), ' '));
			else if(KIND.equals(parts[0]) && parts.length >= 4){
				final Counts target = counts.computeIfAbsent(parts[1], key -> new TreeMap<>())
					.computeIfAbsent(parts[2], key -> new Counts());
				for(int p = 3; p < parts.length; p ++)
					read(parts[p], target);
			}
		}
	}

	/** Adds a {@code name=count} (or {@code positions=sum:count}) of a line to the counts. */
	private static void read(final String part, final Counts target){
		final int equal = part.indexOf('=');
		if(equal <= 0)
			return;

		final String name = part.substring(0, equal);
		final String value = part.substring(equal + 1);
		try{
			if("positions".equals(name)){
				final int colon = value.indexOf(':');
				target.positionSum += Long.parseLong(value.substring(0, colon));
				target.positionCount += Long.parseLong(value.substring(colon + 1));
				return;
			}
			for(final Event event : Event.values())
				if(event.name().equals(name))
					target.counts[event.ordinal()] += Long.parseLong(value);
		}
		catch(final RuntimeException re){
			//a part that is not a number is skipped
		}
	}

	private String toText(){
		final List<String> lines = new ArrayList<>();
		lines.add(HEADER);
		for(final Map.Entry<String, Map<String, Counts>> byTuning : counts.entrySet()){
			lines.add(TUNING + ' ' + byTuning.getKey() + ' ' + tunings.getOrDefault(byTuning.getKey(), DEFAULTS));
			for(final Map.Entry<String, Counts> byKind : byTuning.getValue().entrySet()){
				final StringBuilder line = new StringBuilder(KIND).append(' ').append(byTuning.getKey()).append(' ').append(byKind.getKey());
				final Counts kind = byKind.getValue();
				for(final Event event : Event.values())
					if(kind.counts[event.ordinal()] > 0L)
						line.append(' ').append(event.name()).append('=').append(kind.counts[event.ordinal()]);
				if(kind.positionCount > 0L)
					line.append(" positions=").append(kind.positionSum).append(':').append(kind.positionCount);
				lines.add(line.toString());
			}
		}
		return String.join("\n", lines) + "\n";
	}

	/** @return a short digest of the numbers of the assistants (the same numbers, the same digest) */
	static String digest(final AssistantTuning tuning){
		try{
			final byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
				.digest(describe(tuning).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash, 0, DIGEST_CHARS / 2);
		}
		catch(final java.security.NoSuchAlgorithmException nsae){
			//every Java platform has SHA-256
			throw new IllegalStateException(nsae);
		}
	}

	/** @return the numbers that are not the defaults, as {@code name=value} (or {@code defaults}) */
	static String describe(final AssistantTuning tuning){
		final List<String> changed = new ArrayList<>();
		for(final AssistantTuning.Parameter parameter : AssistantTuning.Parameter.values())
			if(tuning.get(parameter) != parameter.defaultValue())
				changed.add(parameter.name().toLowerCase(java.util.Locale.ROOT) + '=' + tuning.get(parameter));
		return (changed.isEmpty()? DEFAULTS: String.join(" ", changed));
	}


	/** The text in a file, written all at once (a file that is half written is never left). */
	private record FileStore(Path file) implements Store{

		@Override
		public String read() throws IOException{
			return (Files.exists(file)? Files.readString(file, StandardCharsets.UTF_8): null);
		}

		@Override
		public void write(final String text) throws IOException{
			Files.createDirectories(file.getParent());
			AtomicFileWriter.write(file, text, StandardCharsets.UTF_8);
		}

	}

}
