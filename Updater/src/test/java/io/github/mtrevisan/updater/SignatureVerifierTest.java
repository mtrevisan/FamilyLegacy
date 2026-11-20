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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;


/** The check of the signature of the installer: the key must be the published one; Windows is replaced by a canned answer. */
class SignatureVerifierTest{

	/** A self-signed test certificate (EC P-256), made with keytool. */
	private static final byte[] CERTIFICATE = Base64.getDecoder().decode("MIIBNjCB3aADAgECAghpv9+2HONUjjAKBggqhkjOPQQDAzAPMQ0wCwYDVQQDEwRUZXN0MCAXDTI2MTEyMDA4MTA0M1oYDzIxMjYxMDI3"
		+ "MDgxMDQzWjAPMQ0wCwYDVQQDEwRUZXN0MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1qpnfJYSywao5krpXt8s4oIZxZeEx14JCNIUqhRX5QKemOet+WYkwQXYOeSH"
		+ "9sNnwCQLlAVMTsb0qksCD52VGaMhMB8wHQYDVR0OBBYEFMMCm1YXjXa74jLvayqc0gjGii4lMAoGCCqGSM49BAMDA0gAMEUCIDIigzizfZHVkqnrPz7viBi92KNsGKMg"
		+ "NSUu9baqx7PVAiEAnx4pvvKubYIQAu2hVjnvvMekzKngPGqupR+jOC3wvUw=");
	/** The SHA-256 of its public key, computed by openssl (not by the code under test). */
	private static final String FINGERPRINT = "9a8d68e4dc88b69a17571218cce0105862ed4c0a0aed694e77bf218543e617d7";
	private static final String OTHER_FINGERPRINT = "0".repeat(64);

	private static final byte[] INSTALLER = "an installer, more or less".getBytes(StandardCharsets.UTF_8);


	private static SignatureVerifier verifier(final Set<String> trusted, final String status, final byte[] certificate){
		return new SignatureVerifier(trusted, file -> new SignatureVerifier.Signature(status, certificate));
	}

	private static String sha256(final byte[] bytes) throws Exception{
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
	}


	@Test
	void theFingerprintIsTheHashOfThePublicKey() throws Exception{
		Assertions.assertEquals(FINGERPRINT, SignatureVerifier.fingerprint(CERTIFICATE));
	}

	@Test
	void aGarbageCertificateIsAnIoError(){
		Assertions.assertThrows(IOException.class, () -> SignatureVerifier.fingerprint(new byte[]{1, 2, 3}));
	}

	@Test
	void theKnownKeyWithAValidSignatureIsAccepted(@TempDir final Path directory) throws Exception{
		final Path file = Files.write(directory.resolve("a.exe"), INSTALLER);

		verifier(Set.of(FINGERPRINT.toUpperCase()), "Valid", CERTIFICATE).verify(file);
	}

	@Test
	void anotherKeyIsRefused(@TempDir final Path directory) throws Exception{
		final Path file = Files.write(directory.resolve("a.exe"), INSTALLER);

		final IOException e = Assertions.assertThrows(IOException.class,
			() -> verifier(Set.of(OTHER_FINGERPRINT), "Valid", CERTIFICATE).verify(file));
		Assertions.assertTrue(e.getMessage().contains(FINGERPRINT));
	}

	@Test
	void aSignatureThatIsNotValidIsRefusedEvenWithTheKnownKey(@TempDir final Path directory) throws Exception{
		final Path file = Files.write(directory.resolve("a.exe"), INSTALLER);

		for(final String status : List.of("NotSigned", "HashMismatch", "NotTrusted", "UnknownError", "valid", ""))
			Assertions.assertThrows(IOException.class, () -> verifier(Set.of(FINGERPRINT), status, CERTIFICATE).verify(file), status);
		Assertions.assertThrows(IOException.class, () -> verifier(Set.of(FINGERPRINT), "Valid", null).verify(file));
	}

	@Test
	void withoutAPublishedKeyNothingIsEnforced(@TempDir final Path directory) throws Exception{
		final SignatureVerifier none = new SignatureVerifier(Set.of(), file -> {
			throw new AssertionError("must not look at the file");
		});

		Assertions.assertFalse(none.isEnforced());
		none.verify(Files.write(directory.resolve("a.exe"), INSTALLER));
	}

	@Test
	void theListIsParsed(){
		final Set<String> parsed = SignatureVerifier.parseTrusted(List.of("# comment", "", "   ", FINGERPRINT.toUpperCase() + "  main key 2026",
			OTHER_FINGERPRINT));

		Assertions.assertEquals(Set.of(FINGERPRINT, OTHER_FINGERPRINT), parsed);
		Assertions.assertThrows(IllegalStateException.class, () -> SignatureVerifier.parseTrusted(List.of("abc123")));
		Assertions.assertThrows(IllegalStateException.class, () -> SignatureVerifier.parseTrusted(List.of("z".repeat(64))));
	}

	@Test
	void theBundledListIsReadable(){
		//(whatever is published there must at least be well formed: a typo would otherwise reject every installer)
		Assertions.assertDoesNotThrow(SignatureVerifier::application);
	}


	@Test
	void anInstallerWithTheWrongSignatureIsDeletedAfterTheDownload(@TempDir final Path directory) throws Exception{
		final ReleaseInfo release = release(sha256(INSTALLER));
		final InstallerDownloader downloader = new InstallerDownloader(uri -> new ByteArrayInputStream(INSTALLER),
			verifier(Set.of(OTHER_FINGERPRINT), "Valid", CERTIFICATE));

		Assertions.assertThrows(IOException.class, () -> downloader.download(release, directory, null, null));
		try(final var files = Files.list(directory)){
			Assertions.assertEquals(List.of(), files.toList());
		}
	}

	@Test
	void anInstallerWithTheRightSignatureIsKept(@TempDir final Path directory) throws Exception{
		final InstallerDownloader downloader = new InstallerDownloader(uri -> new ByteArrayInputStream(INSTALLER),
			verifier(Set.of(FINGERPRINT), "Valid", CERTIFICATE));

		final Path file = downloader.download(release(sha256(INSTALLER)), directory, null, null);

		Assertions.assertArrayEquals(INSTALLER, Files.readAllBytes(file));
	}

	private static ReleaseInfo release(final String sha256){
		return new ReleaseInfo(Version.parse("1.2.0").orElseThrow(), "v1.2.0", "r", "", URI.create("https://example.org"),
			"FamilyLegacy-1.2.0.exe", URI.create("https://github.com/mtrevisan/FamilyLegacy/releases/download/v1.2.0/FamilyLegacy-1.2.0.exe"),
			INSTALLER.length, sha256);
	}

}
