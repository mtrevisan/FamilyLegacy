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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * Checks the Authenticode signature of a downloaded installer: it must be valid for Windows <em>and</em> made with a key whose
 * fingerprint the application knows.
 * <p>
 * The fingerprint is the SHA-256 of the public key of the signing certificate (its {@code SubjectPublicKeyInfo}), so it survives the
 * renewal of the certificate as long as the key is the same. The accepted fingerprints are in {@code trusted-signers.txt}, beside this
 * class, and are published in {@code installer/SIGNING.md}. While that list is empty nothing is enforced (no key exists yet); once it
 * has a line, an installer that is unsigned, badly signed or signed by another key is refused.
 * </p>
 * <p>
 * Run as a program ({@code java -cp Updater/target/classes io.github.mtrevisan.updater.SignatureVerifier <file>}) it prints the
 * status and the fingerprint of a signed file, so that the one to publish is computed by the same code that checks it.
 * </p>
 */
public final class SignatureVerifier{

	static final String TRUSTED_RESOURCE = "trusted-signers.txt";
	private static final String VALID = "Valid";
	private static final long INSPECT_TIMEOUT_SECONDS = 60;
	/** The path travels in the environment, never inside the script: no quoting problem, nothing to inject. */
	private static final String FILE_VARIABLE = "FAMILYLEGACY_SIGNED_FILE";
	private static final String SCRIPT = "$s = Get-AuthenticodeSignature -LiteralPath $env:" + FILE_VARIABLE + "; "
		+ "[Console]::Out.WriteLine([string]$s.Status); "
		+ "if($s.SignerCertificate){ [Console]::Out.WriteLine([Convert]::ToBase64String($s.SignerCertificate.RawData)) }";


	/** What Windows says about the signature of a file. */
	public record Signature(String status, byte[] signerCertificate){}


	/** Looks at the signature of a file. */
	@FunctionalInterface
	public interface Inspector{

		/**
		 * @param file	The file.
		 * @return	The status of the signature (as Authenticode reports it) and the DER of the signer certificate, if any.
		 * @throws IOException	If the file cannot be inspected.
		 */
		Signature inspect(Path file) throws IOException;

	}


	private final Set<String> trusted;
	private final Inspector inspector;


	/**
	 * @param trusted	The accepted fingerprints (hexadecimal SHA-256 of the public key); empty to enforce nothing.
	 * @param inspector	How to read the signature of a file.
	 */
	public SignatureVerifier(final Set<String> trusted, final Inspector inspector){
		final Set<String> normalized = new LinkedHashSet<>();
		for(final String fingerprint : trusted)
			normalized.add(fingerprint.toLowerCase(Locale.ROOT));
		this.trusted = Set.copyOf(normalized);
		this.inspector = inspector;
	}

	/** @return	The verifier of the application: the published fingerprints, Windows to read the signature. */
	public static SignatureVerifier application(){
		return new SignatureVerifier(readTrusted(), SignatureVerifier::inspectWithWindows);
	}


	/** @return	Whether a file is refused when it is not signed by a known key (that is, whether a fingerprint is published). */
	public boolean isEnforced(){
		return !trusted.isEmpty();
	}

	/**
	 * @param file	The downloaded installer.
	 * @throws IOException	If the installer is unsigned, its signature is not valid or the key is not a known one.
	 */
	public void verify(final Path file) throws IOException{
		if(!isEnforced())
			return;

		final Signature signature = inspector.inspect(file);
		if(!VALID.equals(signature.status()))
			throw new IOException("The installer is not signed correctly (signature status: " + signature.status() + ")");
		if(signature.signerCertificate() == null)
			throw new IOException("The installer has no signer certificate");

		final String fingerprint = fingerprint(signature.signerCertificate());
		if(!trusted.contains(fingerprint))
			throw new IOException("The installer is signed with a key that is not the published one (" + fingerprint + ")");
	}

	/**
	 * @param certificate	A certificate, DER encoded.
	 * @return	The SHA-256 of its public key ({@code SubjectPublicKeyInfo}), as lower-case hexadecimal.
	 * @throws IOException	If it is not a certificate.
	 */
	public static String fingerprint(final byte[] certificate) throws IOException{
		try{
			final byte[] key = CertificateFactory.getInstance("X.509")
				.generateCertificate(new ByteArrayInputStream(certificate))
				.getPublicKey()
				.getEncoded();
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key));
		}
		catch(final CertificateException e){
			throw new IOException("The signer certificate cannot be read: " + e.getMessage(), e);
		}
		catch(final NoSuchAlgorithmException e){
			//(every Java platform has it)
			throw new IllegalStateException(e);
		}
	}


	/**
	 * @param content	The text of the list: one fingerprint per line, then optionally a comment; blank lines and lines starting with
	 * 	{@code #} are ignored.
	 * @return	The fingerprints.
	 */
	static Set<String> parseTrusted(final List<String> content){
		final Set<String> fingerprints = new LinkedHashSet<>();
		for(final String line : content){
			final String text = line.strip();
			if(text.isEmpty() || text.charAt(0) == '#')
				continue;

			final int end = indexOfWhitespace(text);
			final String fingerprint = (end < 0? text: text.substring(0, end)).toLowerCase(Locale.ROOT);
			if(fingerprint.length() != 64 || !isHex(fingerprint))
				//(a mistyped line must stop the verification, not shrink the list silently: a wrong list would reject every installer)
				throw new IllegalStateException("Not a SHA-256 fingerprint in " + TRUSTED_RESOURCE + ": " + text);
			fingerprints.add(fingerprint);
		}
		return fingerprints;
	}

	private static int indexOfWhitespace(final String text){
		for(int i = 0; i < text.length(); i ++)
			if(Character.isWhitespace(text.charAt(i)))
				return i;
		return -1;
	}

	private static boolean isHex(final String text){
		for(int i = 0; i < text.length(); i ++)
			if(Character.digit(text.charAt(i), 16) < 0)
				return false;
		return true;
	}

	private static Set<String> readTrusted(){
		try(final InputStream in = SignatureVerifier.class.getResourceAsStream(TRUSTED_RESOURCE)){
			if(in == null)
				return Set.of();

			final List<String> lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().toList();
			return parseTrusted(lines);
		}
		catch(final IOException e){
			throw new IllegalStateException("Cannot read " + TRUSTED_RESOURCE, e);
		}
	}

	/** Asks Windows (the same check Explorer does, through PowerShell) for the signature of a file. */
	private static Signature inspectWithWindows(final Path file) throws IOException{
		if(!InstallerLauncher.isSupportedPlatform())
			throw new IOException("The signature of the installer can be checked only on Windows");

		final String root = System.getenv("SystemRoot");
		//(the full path: a "powershell" found through the PATH could be anything)
		final Path powershell = Path.of(root != null? root: "C:\\Windows", "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
		final ProcessBuilder builder = new ProcessBuilder(powershell.toString(), "-NoProfile", "-NonInteractive", "-ExecutionPolicy",
			"Bypass", "-Command", SCRIPT)
			.redirectErrorStream(false)
			.redirectError(ProcessBuilder.Redirect.DISCARD);
		builder.environment().put(FILE_VARIABLE, file.toAbsolutePath().toString());

		final Process process = builder.start();
		try{
			process.getOutputStream().close();
			final List<String> lines;
			try(final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))){
				lines = reader.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
			}
			if(!process.waitFor(INSPECT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
				throw new IOException("Windows did not answer about the signature of the installer");
			if(lines.isEmpty())
				throw new IOException("Windows did not tell the signature of the installer");

			return new Signature(lines.get(0), (lines.size() > 1? Base64.getDecoder().decode(lines.get(1)): null));
		}
		catch(final InterruptedException e){
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while checking the signature of the installer", e);
		}
		catch(final IllegalArgumentException e){
			throw new IOException("Windows gave an unreadable signer certificate", e);
		}
		finally{
			process.destroyForcibly();
		}
	}


	/** Prints the signature status and the fingerprint of the key that signed the file given as argument. */
	public static void main(final String[] args) throws IOException{
		if(args.length != 1){
			System.err.println("usage: SignatureVerifier <signed file>");
			System.exit(2);
		}

		final Path file = Path.of(args[0]);
		if(!Files.isRegularFile(file)){
			System.err.println("Not a file: " + file);
			System.exit(2);
		}
		final Signature signature = inspectWithWindows(file);
		System.out.println("status: " + signature.status());
		if(signature.signerCertificate() != null)
			System.out.println("fingerprint: " + fingerprint(signature.signerCertificate()));
		System.exit(VALID.equals(signature.status())? 0: 1);
	}

}
