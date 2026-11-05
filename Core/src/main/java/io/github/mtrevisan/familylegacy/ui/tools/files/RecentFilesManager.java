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
package io.github.mtrevisan.familylegacy.ui.tools.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


/**
 * Manages the list of recently opened files, persisted between
 * sessions through {@link Preferences}.
 * <p>
 * The list is kept in insertion order (most recent first) and is
 * bounded to a fixed size: adding a file already in the list moves it
 * to the front instead of creating a duplicate, and adding a file when
 * the list is full drops the oldest entry.
 * <p>
 * The persistence is best-effort: on a headless system, or when the
 * preferences store is not writable, the manager falls back to an
 * in-memory list and continues to work for the current session. A
 * failure to persist is not a failure of the operation that triggered
 * it.
 */
public final class RecentFilesManager{

	private static final int MAX_ENTRIES = 10;
	private static final String KEY_COUNT = "count";
	private static final String KEY_PREFIX = "file.";


	private final Preferences preferences;
	private final List<File> files = new ArrayList<>();


	public RecentFilesManager(){
		Preferences prefs = null;
		try{
			prefs = Preferences.userNodeForPackage(RecentFilesManager.class);
		}
		catch(final SecurityException ignored){
			// The security manager forbids access to the preferences store:
			// fall back to an in-memory list.
		}
		this.preferences = prefs;

		load();
	}

	/** @param preferences	Where the list is kept (a test gives its own). */
	RecentFilesManager(final Preferences preferences){
		this.preferences = preferences;

		load();
	}

	/**
	 * @return	The file as the list knows it: absolute, without {@code .} and {@code ..}, and, if it exists, as the file system writes it
	 * 	(links resolved, capitals as they are). The same file reached through different paths is a single entry of the list.
	 */
	public static File normalized(final File file){
		Path path = file.toPath().toAbsolutePath().normalize();
		try{
			if(Files.exists(path))
				path = path.toRealPath();
		}
		catch(final IOException | SecurityException ignored){
			//the path as it is
		}
		return path.toFile();
	}

	/**
	 * @param file	A file of the list.
	 * @param all	The files of the list.
	 * @return	The text of its menu entry: the name of the file, and the folder too when another file of the list has the same name
	 * 	(two different files can be called the same).
	 */
	public static String label(final File file, final List<File> all){
		final boolean ambiguous = all.stream()
			.anyMatch(other -> other != file && other.getName().equals(file.getName()) && !same(other, file));
		return (ambiguous && file.getParentFile() != null? file.getName() + " (" + file.getParent() + ")": file.getName());
	}

	private static boolean same(final File a, final File b){
		return normalized(a).toPath().equals(normalized(b).toPath());
	}


	/** Returns the list of recent files, most recent first, with no file twice. */
	public List<File> list(){
		return List.copyOf(files);
	}

	/** @return	Whether the file is in the list, whichever path it is written with. */
	public boolean contains(final File file){
		return file != null && files.stream().anyMatch(existing -> same(existing, file));
	}


	/**
	 * Adds a file to the front of the list. If the file is already
	 * present, it is moved to the front. If the list is full, the oldest
	 * entry is dropped.
	 */
	public void add(final File file){
		if(file == null)
			return;

		final File entry = normalized(file);
		files.removeIf(existing -> same(existing, entry));
		files.addFirst(entry);

		while(files.size() > MAX_ENTRIES)
			files.remove(files.size() - 1);

		save();
	}

	/** Removes the given file from the list, if present. */
	public void remove(final File file){
		if(file != null && files.removeIf(existing -> same(existing, file)))
			save();
	}

	/** Removes entries that no longer exist on disk. */
	public void pruneMissing(){
		final List<File> toRemove = new ArrayList<>();
		for(final File file : files)
			if(!file.exists())
				toRemove.add(file);
		if(files.removeAll(toRemove))
			save();
	}

	/** Empties the list. */
	public void clear(){
		files.clear();
		save();
	}


	/* ======================================================================
	 *                          Persistence
	 * ====================================================================== */

	private void load(){
		if(preferences == null)
			return;

		try{
			final int count = preferences.getInt(KEY_COUNT, 0);
			for(int i = 0; i < count; i ++){
				final String path = preferences.get(KEY_PREFIX + i, null);
				if(path != null && !path.isBlank()){
					final File file = new File(path);
					//a list written by an older version can have a file twice
					if(files.stream().noneMatch(existing -> same(existing, file)))
						files.add(normalized(file));
				}
			}
			if(files.size() != count)
				save();
		}
		catch(final SecurityException ignored){
			// The store is not readable: leave the in-memory list empty.
		}
	}

	private void save(){
		if(preferences == null)
			return;

		try{
			preferences.clear();
			preferences.putInt(KEY_COUNT, files.size());
			for(int i = 0; i < files.size(); i ++)
				preferences.put(KEY_PREFIX + i, files.get(i).getAbsolutePath());
			preferences.flush();
		}
		catch(final BackingStoreException | SecurityException ignored){
			// The store is not writable: the in-memory list remains valid
			// for the current session.
		}
	}

}
