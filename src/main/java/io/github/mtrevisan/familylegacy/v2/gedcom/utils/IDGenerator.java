package io.github.mtrevisan.familylegacy.v2.gedcom.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public final class IDGenerator{

	private static final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

	private IDGenerator(){
	}

	/**
	 * Registers an existing ID so that the counter starts after its number.
	 * For example, "I7" registers prefix "I" with value 7.
	 * If the ID does not match the pattern (letter + digits), it is ignored.
	 */
	public static void registerExistingId(String id){
		if(id == null || id.isEmpty()) return;
		// Remove '@' if present
		String clean = id.replace("@", "");
		// Extract prefix (one or more letters) and trailing digits
		int i = 0;
		while(i < clean.length() && Character.isLetter(clean.charAt(i))) i++;
		if(i == 0 || i == clean.length()) return; // must have both letters and digits
		String prefix = clean.substring(0, i);
		String numStr = clean.substring(i);
		try{
			int num = Integer.parseInt(numStr);
			counters.computeIfAbsent(prefix, k -> new AtomicInteger(0))
				.updateAndGet(current -> Math.max(current, num));
		}
		catch(NumberFormatException ignored){
			// ignore non‑numeric trailing part
		}
	}

	/**
	 * Generates a new unique ID for the given prefix.
	 * For example, nextId("I") returns "I1", "I2", etc.
	 */
	public static String nextId(String prefix){
		int next = counters.computeIfAbsent(prefix, k -> new AtomicInteger(0))
			.incrementAndGet();
		return prefix + next;
	}

	/**
	 * Convenience method to get the next ID for a prefix,
	 * but also registers the ID if it's an existing one.
	 * Used when we want to assign a specific ID and ensure the counter is updated.
	 */
	public static void registerAndUseId(String id){
		registerExistingId(id);
	}

	/**
	 * Returns the current maximum number for a prefix without incrementing.
	 */
	public static int getCurrentMax(String prefix){
		AtomicInteger counter = counters.get(prefix);
		return counter == null? 0: counter.get();
	}
}
