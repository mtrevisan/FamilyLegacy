# Signing the Windows installers

The installers of Family Legacy are meant to be signed with an Authenticode (code-signing) certificate. **Help > Check for Updates...**
checks the signature of the installer it downloads, besides its SHA-256, and discards the file if it is not right.

## The published key

The key is identified by the **SHA-256 of the public key of the signing certificate** (the `SubjectPublicKeyInfo`), not of the certificate:
a renewed certificate on the same key keeps the same fingerprint.

| Key | Fingerprint (SHA-256) | Used from |
|-----|-----------------------|-----------|
| *(not yet published: no certificate has been issued)* | | |

The same value is in the notes of every signed release and in
[`trusted-signers.txt`](../Updater/src/main/resources/io/github/mtrevisan/updater/trusted-signers.txt), which is what the application
compares with. To check an installer by hand, compare the fingerprint that this prints with the table above:

```
mvn -pl Updater compile
java -cp Updater/target/classes io.github.mtrevisan.updater.SignatureVerifier FamilyLegacy-X.Y.Z-setup.exe
```

## What the application does

- While `trusted-signers.txt` has no line (today) the signature is not checked: no key exists yet. The SHA-256 check still applies.
- Once a line is there, an installer is refused unless Windows says its signature is *Valid* (chain to a trusted authority, file not
  modified) **and** it was made with one of the listed keys. A refused file is deleted and the user sees *Unable to download the update*.
- Releases made before the first signed one are not signed: an installed version too old to carry the list does not check anything.

## Signing a release

1. Build the installer (`-Pwindows-inno`, see `Ephemeris/pom.xml`).
2. `.\installer\sign-installer.ps1 -File Core\target\windows-inno\FamilyLegacy-X.Y.Z-setup.exe -Thumbprint <certificate>` (or `-PfxFile`, with the
   password in `FAMILYLEGACY_PFX_PASSWORD`). It signs, timestamps, checks the result as the application does and prints the fingerprint.
3. The first time: add that fingerprint to `trusted-signers.txt` and to the table above, and **ship that version** (the list is part of
   the application: only versions carrying it check anything).
4. Upload the signed file to the release, and write the fingerprint in the release notes.

Notes:

- Sign **after** the build, on the final file. The installer made by `-Pwindows-exe` (WiX) is a bootstrapper that cannot be signed
  with a plain `signtool sign` without detaching its engine first (`insignia`); use `-Pwindows-inno` for signed releases.
- The uninstaller that Inno Setup embeds is not covered by this signature (it would need the `SignTool` directive of the script).
- A self-signed certificate is *not trusted* by Windows: the signature reads *NotTrusted* and the update is refused. Use a certificate
  issued by a certification authority.
- Changing key: add the new fingerprint, keep the old one until the releases signed with it are no longer offered, so the installed
  versions can still update.
- Lose or leak the private key: remove its line, publish a new key, and tell the users in the release notes.
