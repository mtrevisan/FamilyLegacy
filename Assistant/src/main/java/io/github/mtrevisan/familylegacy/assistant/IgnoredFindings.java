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
package io.github.mtrevisan.familylegacy.assistant;

import io.github.mtrevisan.familylegacy.preferences.AppPreferences;
import io.github.mtrevisan.familylegacy.preferences.ProjectMeta;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;


/**
 * The findings the user chose to ignore, because after examining them they decided there is nothing to do (e.g. a date that is
 * really inconsistent in the source).
 * <p>
 * Ignoring a finding is a presentation choice of the user, like the order of the children: it is kept next to each file (in its
 * {@code .meta} folder, so that it travels with the file), and <em>never</em> in the genealogy records, so it creates, modifies or
 * deletes no record and is not exported.
 * </p>
 * <p>
 * A finding is ignored through its {@link Diagnostic#key() key}, which changes when the data it reports changes: if the date is
 * corrected and the finding is still there, or a record is added to the conflict, it is a new finding and it comes back. The keys
 * are stored as a digest, not as text: the folder does not keep names or dates of the people. A file that was never saved has no
 * identity, so its ignored findings are kept for the session only.
 * </p>
 * <p>
 * Besides a single finding, the user can ignore every finding of a kind, every finding about a record, or put a finding aside until a
 * date. What is ignored can be exported and added to another file (a renamed or moved file is another file for the preferences).
 * </p>
 */
public final class IgnoredFindings{

	/** Where the ignored findings are kept. */
	public interface Storage{

		/**
		 * @param scope the digest of the file
		 * @return the digests of the keys ignored for the file
		 */
		Set<String> load(String scope);

		/**
		 * @param scope the digest of the file
		 * @param keys  the digests of the keys ignored for the file
		 */
		void save(String scope, Set<String> keys);

		/**
		 * @param file	the file the findings are about (its path)
		 * @param scope	the digest of the file
		 * @return the digests of the keys ignored for the file; by default, the ones kept by the digest of the file
		 */
		default Set<String> load(final String file, final String scope){
			return load(scope);
		}

		/**
		 * @param file	the file the findings are about (its path)
		 * @param scope	the digest of the file
		 * @param keys	the digests of the keys ignored for the file
		 */
		default void save(final String file, final String scope, final Set<String> keys){
			save(scope, keys);
		}

	}

	/** The findings split in the ones to show and the ones ignored. */
	public record Split(List<Diagnostic> active, List<Diagnostic> ignored){}


	private static final String IGNORED_FILE_NAME = "ignored-findings.properties";
	private static final String IGNORED_KEY = "findings";
	private static final int DIGEST_CHARS = 16;
	/** Before the digest of a kind of finding: everything of that kind is ignored. */
	private static final String KIND_PREFIX = "k:";
	/** Before the digest of a record: every finding about that record is ignored. */
	private static final String RECORD_PREFIX = "r:";
	/** Before the date until which a finding is put aside, then the digest of its key ({@code z:2027-01-31:0123456789abcdef}). */
	private static final String SNOOZE_PREFIX = "z:";
	private static final char SNOOZE_SEPARATOR = ':';
	/** The first line of a file of ignored findings. */
	public static final String EXPORT_HEADER = "FamilyLegacy ignored findings 1";

	private final Storage storage;
	private final Supplier<LocalDate> today;
	private final Map<String, Set<String>> cache = new HashMap<>();
	/** The findings ignored for a file with no identity (never saved), for the session only. */
	private final Set<String> session = new HashSet<>();
	private final List<Runnable> listeners = new CopyOnWriteArrayList<>();


	public IgnoredFindings(final Storage storage){
		this(storage, LocalDate::now);
	}

	/** @param today gives the date of today (the tests give their own), to know when a finding put aside comes back */
	public IgnoredFindings(final Storage storage, final Supplier<LocalDate> today){
		this.storage = storage;
		this.today = today;
	}

	/**
	 * @return the ignored findings kept next to each file ({@code <file>.meta/ignored-findings.properties}), so that they travel with it;
	 * 	the ones that the older versions kept in the user preferences are taken from there the first time a file is opened
	 */
	public static IgnoredFindings usingPreferences(){
		return new IgnoredFindings(new Storage(){
			@Override
			public Set<String> load(final String scope){
				return AppPreferences.getInstance().ignoredFindings(scope);
			}

			@Override
			public void save(final String scope, final Set<String> keys){
				AppPreferences.getInstance().setIgnoredFindings(scope, keys);
			}

			@Override
			public Set<String> load(final String file, final String scope){
				final Path path = ProjectMeta.file(file, IGNORED_FILE_NAME);
				if(Files.exists(path))
					return decode(ProjectMeta.read(path));

				final Set<String> legacy = load(scope);
				if(!legacy.isEmpty())
					write(path, legacy);
				return legacy;
			}

			@Override
			public void save(final String file, final String scope, final Set<String> keys){
				final Path path = ProjectMeta.file(file, IGNORED_FILE_NAME);
				//nothing to say and no folder: none is created; once the file exists it is kept (empty), so that the findings of the older
				//versions are not read again
				if(!keys.isEmpty() || Files.exists(path))
					write(path, keys);
			}

			private static Set<String> decode(final Properties props){
				final Set<String> keys = new TreeSet<>();
				for(final String key : StringUtils.split(props.getProperty(IGNORED_KEY, StringUtils.EMPTY), '\n'))
					if(!key.isBlank())
						keys.add(key);
				return keys;
			}

			private static void write(final Path path, final Set<String> keys){
				final Properties props = new Properties();
				props.setProperty(IGNORED_KEY, String.join("\n", new TreeSet<>(keys)));
				ProjectMeta.write(path, props, "ignored findings");
			}
		});
	}

	/** @return a store that keeps nothing outside of memory */
	public static IgnoredFindings inMemory(){
		final Map<String, Set<String>> kept = new HashMap<>();
		return new IgnoredFindings(new Storage(){
			@Override
			public Set<String> load(final String scope){
				return kept.getOrDefault(scope, Set.of());
			}

			@Override
			public void save(final String scope, final Set<String> keys){
				kept.put(scope, Set.copyOf(keys));
			}
		});
	}


	/**
	 * @param scope      the file the findings are about ({@code null} if it has no identity yet)
	 * @param diagnostic the finding
	 * @return whether the user ignored the finding (a finding without a key cannot be ignored)
	 */
	public synchronized boolean isIgnored(final String scope, final Diagnostic diagnostic){
		if(!diagnostic.isIgnorable())
			return false;

		final Set<String> keys = keys(scope);
		if(keys.isEmpty())
			return false;

		final String own = digest(diagnostic.key());
		if(keys.contains(own) || keys.contains(KIND_PREFIX + digest(diagnostic.kind())))
			return true;
		for(final String recordId : diagnostic.recordIds())
			if(keys.contains(RECORD_PREFIX + digest(recordId)))
				return true;

		final LocalDate now = today.get();
		final String suffix = SNOOZE_SEPARATOR + own;
		for(final String key : keys)
			if(key.startsWith(SNOOZE_PREFIX) && key.endsWith(suffix) && snoozedUntil(key).isAfter(now))
				return true;
		return false;
	}

	/** Ignores a finding; nothing happens if it has no key. */
	public synchronized void ignore(final String scope, final Diagnostic diagnostic){
		if(diagnostic.isIgnorable())
			add(scope, digest(diagnostic.key()));
	}

	/** Ignores every finding of the kind of this one (the kind is the same in every language); nothing happens if it has no key. */
	public synchronized void ignoreKind(final String scope, final Diagnostic diagnostic){
		if(diagnostic.isIgnorable())
			add(scope, KIND_PREFIX + digest(diagnostic.kind()));
	}

	/** Ignores every finding about a record (it is not an identity: a finding that names it among others is ignored too). */
	public synchronized void ignoreRecord(final String scope, final String recordId){
		if(recordId != null)
			add(scope, RECORD_PREFIX + digest(recordId));
	}

	/**
	 * Puts a finding aside until a date: it comes back by itself that day, if it is still there. Nothing happens if it has no key.
	 *
	 * @param until the first day the finding is shown again
	 */
	public synchronized void snooze(final String scope, final Diagnostic diagnostic, final LocalDate until){
		if(diagnostic.isIgnorable())
			add(scope, SNOOZE_PREFIX + until + SNOOZE_SEPARATOR + digest(diagnostic.key()));
	}

	/**
	 * Shows a finding again, whatever made it ignored: its own key, the putting aside, and the rules that cover it (its kind, the records
	 * it is about): these rules cover other findings too, which are shown again with it.
	 */
	public synchronized void restore(final String scope, final Diagnostic diagnostic){
		if(!diagnostic.isIgnorable())
			return;

		final Set<String> keys = new HashSet<>(keys(scope));
		boolean changed = keys.remove(digest(diagnostic.key())) | keys.remove(KIND_PREFIX + digest(diagnostic.kind()));
		for(final String recordId : diagnostic.recordIds())
			changed |= keys.remove(RECORD_PREFIX + digest(recordId));
		final String suffix = SNOOZE_SEPARATOR + digest(diagnostic.key());
		changed |= keys.removeIf(key -> key.startsWith(SNOOZE_PREFIX) && key.endsWith(suffix));
		if(changed)
			store(scope, keys);
	}

	/** @return whether the finding is ignored by a rule that covers more than it (its kind, a record), and so shown again with others */
	public synchronized boolean isIgnoredByRule(final String scope, final Diagnostic diagnostic){
		if(!diagnostic.isIgnorable())
			return false;

		final Set<String> keys = keys(scope);
		if(keys.contains(KIND_PREFIX + digest(diagnostic.kind())))
			return true;
		for(final String recordId : diagnostic.recordIds())
			if(keys.contains(RECORD_PREFIX + digest(recordId)))
				return true;
		return false;
	}

	/**
	 * What is ignored in the file, to keep it or to give it to another file: a file that is renamed or moved is another file for the
	 * preferences (they know it by its path), and so is the same file on another machine. Only digests, never names or dates.
	 *
	 * @return the text to save: a header, then one entry for each line
	 */
	public synchronized String export(final String scope){
		final List<String> lines = new ArrayList<>();
		lines.add(EXPORT_HEADER);
		lines.addAll(new TreeSet<>(keys(scope)));
		return String.join("\n", lines) + "\n";
	}

	/**
	 * Adds to what is ignored in the file the entries of a text written by {@link #export(String)} (what is ignored already stays so).
	 * The lines that are not entries are skipped.
	 *
	 * @return how many entries are new
	 * @throws IllegalArgumentException if the text is not a file of ignored findings
	 */
	public synchronized int importFrom(final String scope, final String text){
		final String[] lines = StringUtils.split(text, "\r\n");
		if(lines == null || lines.length == 0 || !EXPORT_HEADER.equals(lines[0].trim()))
			throw new IllegalArgumentException("Not a file of ignored findings");

		final Set<String> keys = new HashSet<>(keys(scope));
		int added = 0;
		for(int i = 1; i < lines.length; i ++){
			final String entry = lines[i].trim();
			if(isEntry(entry) && keys.add(entry))
				added ++;
		}
		if(added > 0)
			store(scope, keys);
		return added;
	}

	/** @return how many entries are kept for the file: single findings, rules and findings put aside (to tell the user what an export holds) */
	public synchronized int count(final String scope){
		return keys(scope).size();
	}

	private void add(final String scope, final String entry){
		final Set<String> keys = new HashSet<>(keys(scope));
		if(keys.add(entry))
			store(scope, keys);
	}

	/** @return the day a finding put aside comes back (the entry is {@code z:<date>:<digest>}) */
	private static LocalDate snoozedUntil(final String entry){
		final int end = entry.indexOf(SNOOZE_SEPARATOR, SNOOZE_PREFIX.length());
		try{
			return LocalDate.parse(entry.substring(SNOOZE_PREFIX.length(), end));
		}
		catch(final RuntimeException re){
			//a date that cannot be read puts nothing aside
			return LocalDate.MIN;
		}
	}

	/** @return whether the text is what this class keeps: the digest of a key, of a kind or of a record, or a finding put aside */
	private static boolean isEntry(final String entry){
		if(entry.startsWith(SNOOZE_PREFIX)){
			final int end = entry.indexOf(SNOOZE_SEPARATOR, SNOOZE_PREFIX.length());
			return (end > 0 && snoozedUntil(entry) != LocalDate.MIN && isDigest(entry.substring(end + 1)));
		}
		if(entry.startsWith(KIND_PREFIX))
			return isDigest(entry.substring(KIND_PREFIX.length()));
		if(entry.startsWith(RECORD_PREFIX))
			return isDigest(entry.substring(RECORD_PREFIX.length()));
		return isDigest(entry);
	}

	private static boolean isDigest(final String text){
		if(text.length() != DIGEST_CHARS)
			return false;

		for(int i = 0; i < text.length(); i ++)
			if(Character.digit(text.charAt(i), 16) < 0)
				return false;
		return true;
	}

	/** Splits the findings in the ones to show and the ones the user ignored, keeping their order. */
	public synchronized Split split(final String scope, final List<Diagnostic> diagnostics){
		final List<Diagnostic> active = new ArrayList<>();
		final List<Diagnostic> ignored = new ArrayList<>();
		for(final Diagnostic diagnostic : diagnostics)
			(isIgnored(scope, diagnostic)? ignored: active).add(diagnostic);
		return new Split(active, ignored);
	}


	private Set<String> keys(final String scope){
		if(scope == null)
			return session;

		return cache.computeIfAbsent(digest(scope), scopeDigest -> storage.load(scope, scopeDigest));
	}

	private void store(final String scope, final Set<String> keys){
		if(scope == null){
			session.clear();
			session.addAll(keys);
		}
		else{
			final String scopeDigest = digest(scope);
			cache.put(scopeDigest, keys);
			storage.save(scope, scopeDigest, keys);
		}
		listeners.forEach(Runnable::run);
	}

	/** @param listener told every time a finding is ignored or shown again (it must be quick) */
	public void addListener(final Runnable listener){
		listeners.add(listener);
	}

	/** @return the first characters of the SHA-256 of a text */
	static String digest(final String text){
		try{
			final byte[] hash = MessageDigest.getInstance("SHA-256")
				.digest(text.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash, 0, DIGEST_CHARS / 2);
		}
		catch(final NoSuchAlgorithmException nsae){
			//every Java platform has SHA-256
			throw new IllegalStateException(nsae);
		}
	}

}
