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
package io.github.mtrevisan.updater;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToIntFunction;


/** The releases of a GitHub repository. */
public final class GitHubReleases{

	/** The repository where the releases of the application are published. */
	public static final String REPOSITORY = "mtrevisan/FamilyLegacy";

	private static final String API = "https://api.github.com/repos/";
	private static final String SHA256_PREFIX = "sha256:";
	private static final int SHA256_HEX_LENGTH = 64;
	/** The answer of the API is small; anything bigger is not what is expected. */
	private static final int MAX_ANSWER_BYTES = 4 * 1024 * 1024;


	private final String repository;
	private final Transport transport;
	private final ToIntFunction<String> installerRank;


	/** @return	The releases of the application, read from the network, with the installers of this system. */
	public static GitHubReleases application(){
		return new GitHubReleases(REPOSITORY, Transport.http(), InstallerLauncher::installerRank);
	}

	/**
	 * @param repository	The repository, as {@code owner/name}.
	 * @param transport	How to read from the network.
	 * @param installerRank	How much an asset is preferred as the installer (0 is the best), -1 if it is not an installer.
	 */
	public GitHubReleases(final String repository, final Transport transport, final ToIntFunction<String> installerRank){
		this.repository = repository;
		this.transport = transport;
		this.installerRank = installerRank;
	}

	/** @return	The page where all the releases are listed. */
	public URI releasesPage(){
		return URI.create("https://github.com/" + repository + "/releases");
	}

	/**
	 * @return	The latest published release (neither a draft nor a pre-release), or empty if there is none yet.
	 * @throws IOException	If GitHub cannot be reached or its answer cannot be understood.
	 */
	public Optional<ReleaseInfo> latest() throws IOException{
		final Optional<Map<String, Object>> release = latestRelease();
		return (release.isPresent()? parse(release.get()): Optional.empty());
	}

	/**
	 * @param name	The file name of the asset.
	 * @return	The asset of the latest published release with that name, or empty if there is no release, or it has no such asset.
	 * @throws IOException	If GitHub cannot be reached or its answer cannot be understood.
	 */
	public Optional<ReleaseAsset> latestAsset(final String name) throws IOException{
		final Optional<Map<String, Object>> release = latestRelease();
		if(release.isEmpty() || Boolean.TRUE.equals(release.get().get("draft")))
			return Optional.empty();

		final List<Object> assets = Json.asArray(release.get().get("assets"));
		for(final Object asset : assets){
			final Map<String, Object> candidate = Json.asObject(asset);
			final String url = Json.asString(candidate.get("browser_download_url"));
			if(!name.equals(Json.asString(candidate.get("name"))) || !isHttps(url))
				continue;

			String sha256 = digestOf(candidate);
			if(sha256 == null)
				sha256 = checksumFile(assets, name);
			final long size = (candidate.get("size") instanceof final Double value? value.longValue(): -1);
			return Optional.of(new ReleaseAsset(name, URI.create(url), size, sha256));
		}
		return Optional.empty();
	}

	private Optional<Map<String, Object>> latestRelease() throws IOException{
		final URI uri = URI.create(API + repository + "/releases/latest");
		final String body;
		try{
			body = readText(uri);
		}
		catch(final Transport.HttpStatusException e){
			//(also what GitHub answers for a repository that is private or renamed, or with pre-releases only)
			if(e.status() == 404)
				return Optional.empty();
			if(e.status() == 403 || e.status() == 429)
				throw new IOException("GitHub refuses the request (too many requests?): " + e.getMessage(), e);
			throw e;
		}

		try{
			return Optional.of(Json.asObject(Json.parse(body)));
		}
		catch(final IllegalArgumentException e){
			throw new IOException("Unreadable answer from GitHub: " + e.getMessage(), e);
		}
	}

	private Optional<ReleaseInfo> parse(final Map<String, Object> release) throws IOException{
		final String tag = Json.asString(release.get("tag_name"));
		if(Boolean.TRUE.equals(release.get("draft")))
			return Optional.empty();

		//(a release exists but cannot be compared: this is not "nothing published")
		final Optional<Version> version = Version.parse(tag);
		if(version.isEmpty())
			throw new IOException("The latest release has a tag that is not a version: " + tag);

		final String name = Json.asString(release.get("name"));
		final String notes = Json.asString(release.get("body"));
		final String page = Json.asString(release.get("html_url"));

		final List<Object> assets = Json.asArray(release.get("assets"));
		Map<String, Object> installer = null;
		int bestRank = Integer.MAX_VALUE;
		for(final Object asset : assets){
			final Map<String, Object> candidate = Json.asObject(asset);
			final String assetName = Json.asString(candidate.get("name"));
			if(assetName == null)
				continue;

			final int rank = installerRank.applyAsInt(assetName);
			if(rank >= 0 && rank < bestRank && isHttps(Json.asString(candidate.get("browser_download_url")))){
				bestRank = rank;
				installer = candidate;
			}
		}

		String installerName = null;
		URI installerUrl = null;
		long installerSize = -1;
		String sha256 = null;
		if(installer != null){
			installerName = Json.asString(installer.get("name"));
			installerUrl = URI.create(Json.asString(installer.get("browser_download_url")));
			installerSize = (installer.get("size") instanceof final Double size? size.longValue(): -1);
			sha256 = digestOf(installer);
			if(sha256 == null)
				sha256 = checksumFile(assets, installerName);
		}

		return Optional.of(new ReleaseInfo(version.get(), tag, (name == null || name.isBlank()? tag: name), (notes == null? "": notes),
			(isHttps(page)? URI.create(page): releasesPage()), installerName, installerUrl, installerSize, sha256));
	}

	private static boolean isHttps(final String url){
		if(url == null)
			return false;
		try{
			return "https".equalsIgnoreCase(URI.create(url).getScheme());
		}
		catch(final IllegalArgumentException e){
			return false;
		}
	}

	/** GitHub publishes the digest of an asset as {@code sha256:<hex>}. */
	private static String digestOf(final Map<String, Object> asset){
		final String digest = Json.asString(asset.get("digest"));
		if(digest == null || !digest.toLowerCase(Locale.ROOT).startsWith(SHA256_PREFIX))
			return null;
		return normalizeSha256(digest.substring(SHA256_PREFIX.length()));
	}

	/** A release may carry, beside the installer, a {@code <installer>.sha256} file (the output of {@code sha256sum}). */
	private String checksumFile(final List<Object> assets, final String installerName){
		for(final Object asset : assets){
			final Map<String, Object> candidate = Json.asObject(asset);
			final String assetName = Json.asString(candidate.get("name"));
			final String url = Json.asString(candidate.get("browser_download_url"));
			if(assetName == null || !assetName.equalsIgnoreCase(installerName + ".sha256") || !isHttps(url))
				continue;

			try{
				final String content = readText(URI.create(url)).strip();
				final int end = indexOfWhitespace(content);
				return normalizeSha256(end < 0? content: content.substring(0, end));
			}
			catch(final IOException ignored){
				//the checksum is a safeguard, not a requirement
				return null;
			}
		}
		return null;
	}

	private static int indexOfWhitespace(final String text){
		for(int i = 0; i < text.length(); i ++)
			if(Character.isWhitespace(text.charAt(i)))
				return i;
		return -1;
	}

	private static String normalizeSha256(final String hex){
		final String lower = hex.strip().toLowerCase(Locale.ROOT);
		if(lower.length() != SHA256_HEX_LENGTH)
			return null;
		for(int i = 0; i < lower.length(); i ++)
			if(Character.digit(lower.charAt(i), 16) < 0)
				return null;
		return lower;
	}

	private String readText(final URI uri) throws IOException{
		try(final InputStream in = transport.open(uri)){
			final byte[] bytes = in.readNBytes(MAX_ANSWER_BYTES + 1);
			if(bytes.length > MAX_ANSWER_BYTES)
				throw new IOException("The answer of " + uri + " is too big");
			return new String(bytes, StandardCharsets.UTF_8);
		}
	}

}
