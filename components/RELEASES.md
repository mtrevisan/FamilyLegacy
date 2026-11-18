# How the files of a release must be made

This explains what to upload to GitHub so that **Help > Optional Components...** can download and install it (the list itself is
`catalog.json`, see [README.md](README.md)).

## Where to put the files

| Release | Use it for | Notes |
|---------|-----------|-------|
| The release of a version (tag `v1.2.0`, ...) | The installer and, if you want, files that go with that version (`fonts.zip`) | A catalog entry with `"release": "latest"` (the default) reads the **latest published** release: not a draft, not a pre-release. |
| A release kept for components (suggested tag `components`) | Fonts and plugins that do not change with every version | Stays there when new versions are published. A catalog entry names it with `"release": "components"`. It may be a pre-release, but not a draft. |

The tag may contain letters, digits, `.`, `_` and `-` only.

## Rules for every file (asset)

- **Name**: a plain file name, with no `/`, `\` or `:` and not starting with a dot. The name in the catalog (`asset`) must be the same, letter
  for letter.
- **Checksum**: the application installs a file only if it knows its SHA-256. GitHub publishes it by itself for the assets you upload (the
  `digest` field). If it does not (old assets), upload beside the file a text file `<name>.sha256` (for example `fonts.zip.sha256`) with the
  output of `sha256sum` (the 64 hexadecimal digits, then the name). A file without a checksum is shown as *Not available*.
- **Replacing a file**: upload the new file with the same name. The users who installed the old one see *Update available* (the program
  compares the SHA-256 it remembered with the new one). Delete the old asset first, or GitHub refuses the upload.
- **Size**: a download is checked against the size GitHub gives, and a zip may hold at most 200 files and 512 MB once unpacked.

## A `.zip` file

It is **unpacked into the folder of the target**, so:

- the files go at the **top level of the zip, with no folders** (`NotoSans-Regular.ttf`, not `fonts/NotoSans-Regular.ttf`); an entry with a path
  makes the whole zip be refused;
- only the extensions the target accepts are allowed; any other file (for instance an `.exe` or a `.dll`) makes the whole zip be refused:

| Target | Folder | Accepted extensions |
|--------|--------|---------------------|
| `fonts` | `fonts`, beside `plugins` | `.ttf` `.otf` `.ttc` `.txt` |
| `plugins` | `plugins` | `.jar` `.txt` |

- put the licence of the fonts in a `.txt` file in the zip (the Noto fonts are under the SIL OFL: `OFL.txt`);
- nothing is written until the whole zip has been read and accepted; an existing file with the same name is replaced.

Any file that is **not** a zip is copied as it is, with its name, if its extension is accepted by the target (a single `report-foo.jar`, for
instance, with no need to zip it).

Make a zip without folders from the folder that holds the files:

```
cd fonts-cjk
zip ../fonts-cjk.zip *.otf *.txt
```

(on Windows: `Compress-Archive -Path .\fonts-cjk\* -DestinationPath fonts-cjk.zip`: check that the files are at the top level.)

## Fonts

- TrueType or OpenType (`.ttf`, `.otf`, `.ttc`). The PDF reports read every font in the folder.
- The `files` of the catalog entry should name a font that is surely in the zip (for `fonts.zip`, `NotoSans-Regular.ttf`): when it is in the
  folder the component counts as installed, even if it was copied by hand.
- One zip for each group of scripts (`fonts-cjk.zip`, `fonts-arabic.zip`, ...) lets the user choose what to download; a component is one
  line of the catalog each.

## Plugins

- The name decides what the plugin is: `report-*.jar` is a report format, `importer-*.jar` an importer, `face-detector.jar` the face detector. Any other name is not loaded.
- `face-detector.jar` (module `FaceDetector`, built into `Core/plugins/`; it is also copied into the installer by the Ephemeris profiles, like the other plugins) carries the native libraries of ONE platform: `mvn -pl FaceDetector package` makes the Windows x64 one, `-Djavacpp.platform=linux-x86_64` (or `macosx-arm64`, ...) another. Upload it to the `components` release (`gh release upload components Core/plugins/face-detector.jar`); a catalog entry is for one platform, so give another platform a jar with another name only if the loader is extended to it.
- The jar is built as the other plugins (see the `Report*` and `Importer*` modules): it carries only its own libraries, not the ones of the
  application, and lists its providers in `META-INF/services`.
- A plugin is loaded when the application starts: the user must restart it. On Windows a jar that is in use cannot be replaced, so an update of
  a plugin that is already loaded fails until the application is closed.
- A plugin built for a version of the application that changed the plugin API must not be offered to older ones: write the oldest compatible
  version in `minVersion`.

## What the installer carries, and what is downloaded

The installer (`-Pwindows-exe`, see the `Ephemeris` module) is light on purpose: it holds the core of the program and an **empty** `plugins` and
`fonts` folder (each with a `README.txt`, because the application looks for the folder to know where to install). Every other part is a
component of the catalog, uploaded to the `components` release:

| Catalog `id` | Asset | Built by |
|--------------|-------|----------|
| `fonts` | `fonts.zip` | `ReportPdf/target/fonts.zip` |
| `ephemeris` | `ephemeris.jar` | `Ephemeris/target/ephemeris-shaded.jar` (copied to `Core/plugins/ephemeris.jar`) |
| `importer-gedcom` | `importer-gedcom.jar` | `Core/plugins/importer-gedcom.jar` |
| `importer-gramps` | `importer-gramps.jar` (`"requires": ["importer-gedcom"]`) | `Core/plugins/importer-gramps.jar` |
| `report-pdf`, `report-docx`, `report-epub`, `report-site` | `report-*.jar` | `Core/plugins/report-*.jar` |
| `facedetector` | `face-detector.jar` | `Core/plugins/face-detector.jar` |

After a build, upload what changed (the users see *Update available* for a file whose SHA-256 changed; delete the old asset first):

```
gh release upload components Core/plugins/*.jar ReportPdf/target/fonts.zip --repo mtrevisan/FamilyLegacy --clobber
```

A plugin that is installed can be uninstalled from the application (a single file; a `.zip` such as the fonts cannot). `requires` in the catalog
lists the ids, of the same target, that must be installed with a component and that are removed after it.

## Checklist for a new component

1. Make the file (a zip without folders, or a single `.jar`) and check its content.
2. Upload it to the release (for instance with the GitHub CLI):

   ```
   gh release upload components fonts-cjk.zip --repo mtrevisan/FamilyLegacy
   ```

   (create the release once: `gh release create components --title "Components" --notes "Fonts and plugins"`).
3. Add the entry to `catalog.json` (`asset` = the file name, `release` = the tag) and publish it on the `main` branch.
4. Open **Help > Optional Components...**, press **Refresh**, check that the row appears with its size and *Not installed*, and install it.
5. For a translated name and description add to `messages.properties` the keys `component.<target>.<id>.name` and
   `component.<target>.<id>.description`.
