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

import io.github.mtrevisan.familylegacy.ui.preferences.AppPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * The findings the user chose to ignore, because after examining them they decided there is nothing to do (e.g. a date that is
 * really inconsistent in the source).
 * <p>
 * Ignoring a finding is a presentation choice of the user, like the order of the children: it is kept in the user preferences, for
 * each file, and <em>never</em> in the genealogy records, so it creates, modifies or deletes no record and is not exported.
 * </p>
 * <p>
 * A finding is ignored through its {@link Diagnostic#key() key}, which changes when the data it reports changes: if the date is
 * corrected and the finding is still there, or a record is added to the conflict, it is a new finding and it comes back. The keys
 * are stored as a digest, not as text: the preferences do not keep names or dates of the people. A file that was never saved has no
 * identity, so its ignored findings are kept for the session only.
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

	}

	/** The findings split in the ones to show and the ones ignored. */
	public record Split(List<Diagnostic> active, List<Diagnostic> ignored){}


	private static final int DIGEST_CHARS = 16;

	private final Storage storage;
	private final Map<String, Set<String>> cache = new HashMap<>();
	/** The findings ignored for a file with no identity (never saved), for the session only. */
	private final Set<String> session = new HashSet<>();
	private final List<Runnable> listeners = new CopyOnWriteArrayList<>();


	public IgnoredFindings(final Storage storage){
		this.storage = storage;
	}

	/** @return the ignored findings kept in the user preferences */
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
		return diagnostic.isIgnorable() && keys(scope).contains(digest(diagnostic.key()));
	}

	/** Ignores a finding; nothing happens if it has no key. */
	public synchronized void ignore(final String scope, final Diagnostic diagnostic){
		if(!diagnostic.isIgnorable())
			return;

		final Set<String> keys = new HashSet<>(keys(scope));
		if(keys.add(digest(diagnostic.key())))
			store(scope, keys);
	}

	/** Shows an ignored finding again. */
	public synchronized void restore(final String scope, final Diagnostic diagnostic){
		if(!diagnostic.isIgnorable())
			return;

		final Set<String> keys = new HashSet<>(keys(scope));
		if(keys.remove(digest(diagnostic.key())))
			store(scope, keys);
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

		return cache.computeIfAbsent(digest(scope), storage::load);
	}

	private void store(final String scope, final Set<String> keys){
		if(scope == null){
			session.clear();
			session.addAll(keys);
		}
		else{
			final String scopeDigest = digest(scope);
			cache.put(scopeDigest, keys);
			storage.save(scopeDigest, keys);
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
			final StringBuilder sb = new StringBuilder(DIGEST_CHARS);
			for(int i = 0; sb.length() < DIGEST_CHARS; i ++)
				sb.append(String.format("%02x", hash[i]));
			return sb.toString();
		}
		catch(final NoSuchAlgorithmException nsae){
			//every Java platform has SHA-256
			throw new IllegalStateException(nsae);
		}
	}

}
