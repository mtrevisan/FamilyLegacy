package io.github.mtrevisan.familylegacy.v2.ui.tools.files;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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


	/** Returns the list of recent files, most recent first. */
	public List<File> list(){
		return List.copyOf(files);
	}


	/**
	 * Adds a file to the front of the list. If the file is already
	 * present, it is moved to the front. If the list is full, the oldest
	 * entry is dropped.
	 */
	public void add(final File file){
		if(file == null)
			return;

		final Set<File> set = new LinkedHashSet<>();
		set.add(file);
		for(final File existing : files)
			if(!existing.equals(file))
				set.add(existing);

		files.clear();
		files.addAll(set);

		while(files.size() > MAX_ENTRIES)
			files.remove(files.size() - 1);

		save();
	}

	/** Removes the given file from the list, if present. */
	public void remove(final File file){
		if(file != null && files.remove(file))
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
			for(int i = 0; i < count; i++){
				final String path = preferences.get(KEY_PREFIX + i, null);
				if(path != null && !path.isBlank())
					files.add(new File(path));
			}
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
			for(int i = 0; i < files.size(); i++)
				preferences.put(KEY_PREFIX + i, files.get(i).getAbsolutePath());
			preferences.flush();
		}
		catch(final BackingStoreException | SecurityException ignored){
			// The store is not writable: the in-memory list remains valid
			// for the current session.
		}
	}

}
