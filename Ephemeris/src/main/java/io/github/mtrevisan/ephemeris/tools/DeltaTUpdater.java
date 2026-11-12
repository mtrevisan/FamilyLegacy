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
package io.github.mtrevisan.ephemeris.tools;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * Updates the IERS ΔT table ({@code deltat.data}) from the U.S. Naval Observatory.
 * <p>
 * Usage: {@code DeltaTUpdater [--strict] [file ...]}. The files are the copies to write (by default
 * {@code src/main/resources/deltat.data}). The downloaded table is validated and it replaces a file only if it is
 * well-formed and does not end earlier than the one it replaces; the first line of the file is the source URL, as a
 * comment.
 * </p>
 * <p>
 * A failure (no network, malformed data) leaves the files untouched and is only reported, so that it never breaks a
 * build, unless {@code --strict} is given.
 * </p>
 */
public final class DeltaTUpdater{

	public static final String SOURCE_URL = "https://maia.usno.navy.mil/ser7/deltat.data";

	private static final String DEFAULT_TARGET = "src/main/resources/deltat.data";
	private static final int TIMEOUT_MILLIS = 20_000;
	// the table starts in 1973 and has one row per month
	private static final int MINIMUM_ROWS = 500;


	private DeltaTUpdater(){}


	static void main(final String[] args) throws IOException{
		final List<Path> targets = new ArrayList<>();
		boolean strict = false;
		for(final String arg : args)
			if("--strict".equals(arg))
				strict = true;
			else
				targets.add(Path.of(arg));
		if(targets.isEmpty())
			targets.add(Path.of(DEFAULT_TARGET));

		try{
			final List<String> rows = parse(download());
			for(final Path target : targets)
				update(target, rows);
		}
		catch(final IOException | IllegalArgumentException e){
			if(strict)
				throw (e instanceof IOException ioe? ioe: new IOException(e));

			System.err.println("deltat.data not updated: " + e.getMessage());
		}
	}

	private static String download() throws IOException{
		final HttpURLConnection connection = (HttpURLConnection)URI.create(SOURCE_URL)
			.toURL()
			.openConnection();
		connection.setConnectTimeout(TIMEOUT_MILLIS);
		connection.setReadTimeout(TIMEOUT_MILLIS);
		try{
			if(connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException("HTTP " + connection.getResponseCode() + " from " + SOURCE_URL);

			try(final InputStream in = connection.getInputStream()){
				return new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
		}
		finally{
			connection.disconnect();
		}
	}

	/**
	 * Validates a table and returns its data rows.
	 *
	 * @param text the content of the table
	 * @return the data rows, trimmed of the line terminator
	 * @throws IllegalArgumentException if a row is malformed, the dates are not strictly increasing, or the table is too
	 *                                  short to be the real one
	 */
	static List<String> parse(final String text){
		final List<String> rows = new ArrayList<>();
		int previous = 0;
		for(final String line : StringUtils.split(text, "\r\n")){
			final String trimmed = line.trim();
			if(trimmed.isEmpty() || trimmed.startsWith("#"))
				continue;

			final String[] tokens = StringUtils.split(trimmed);
			if(tokens.length < 4)
				throw new IllegalArgumentException("malformed row: " + line);

			final int year;
			final int month;
			final int day;
			try{
				year = Integer.parseInt(tokens[0]);
				month = Integer.parseInt(tokens[1]);
				day = Integer.parseInt(tokens[2]);
				Double.parseDouble(tokens[3]);
			}
			catch(final NumberFormatException e){
				throw new IllegalArgumentException("malformed row: " + line, e);
			}
			if(month < 1 || month > 12 || day < 1 || day > 31)
				throw new IllegalArgumentException("invalid date in row: " + line);

			final int date = (year * 12 + month) * 32 + day;
			if(date <= previous)
				throw new IllegalArgumentException("dates not increasing at row: " + line);
			previous = date;

			rows.add(line.stripTrailing());
		}
		if(rows.size() < MINIMUM_ROWS)
			throw new IllegalArgumentException("only " + rows.size() + " rows, expected at least " + MINIMUM_ROWS);
		return rows;
	}

	private static void update(final Path target, final List<String> rows) throws IOException{
		final String updated = "# " + SOURCE_URL + "\n" + String.join("\n", rows) + "\n";

		if(Files.exists(target)){
			final String current = Files.readString(target, StandardCharsets.UTF_8);
			if(current.replace("\r\n", "\n").equals(updated)){
				System.out.println(target + " is up to date");
				return;
			}

			final List<String> currentRows = parse(current);
			if(rows.size() < currentRows.size())
				throw new IllegalArgumentException("the downloaded table has fewer rows (" + rows.size() + ") than "
					+ target + " (" + currentRows.size() + ")");
		}
		else if(target.getParent() != null && !Files.isDirectory(target.getParent()))
			//a copy in a directory that does not exist yet (nothing to update): skip it
			return;

		Files.writeString(target, updated, StandardCharsets.UTF_8);
		System.out.println(target + " updated: " + rows.size() + " rows, up to " + StringUtils.normalizeSpace(rows.getLast()));
	}

}
