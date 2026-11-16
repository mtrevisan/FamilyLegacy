# Optional components

See [RELEASES.md](RELEASES.md) for how the files of a release must be made.

`catalog.json` is the list shown by **Help > Optional Components...** in the application. The application reads it from
`https://raw.githubusercontent.com/mtrevisan/FamilyLegacy/main/components/catalog.json`, so a component is added by adding its file to a
GitHub release and a line to this catalog: **the application does not change**.

## Adding a component

1. Upload the file as an asset of a release. A release that is kept only to hold components (for instance with the tag `components`) is
   better than the release of a version, because the files stay there when new versions are published. GitHub publishes the SHA-256 of
   every asset, and the application checks the download against it.
2. Add an entry to `catalog.json`:

```json
{
	"id": "fonts-cjk",
	"name": "Chinese, Japanese, Korean fonts",
	"description": "For the PDF reports",
	"target": "fonts",
	"asset": "fonts-cjk.zip",
	"release": "components",
	"files": ["NotoSansSC-Regular.otf"],
	"minVersion": "1.2.0"
}
```

| Field | Meaning |
|-------|---------|
| `id` | Letters, digits, `.`, `_`, `-`; unique for the target. |
| `name`, `description` | The text in English (it may also be an object by language tag). It is the text shown unless the application has it in its own translations: it looks first for the keys `component.<target>.<id>.name` and `component.<target>.<id>.description` in `messages.properties`, so the components that the application knows are translated with the rest of its texts, and a new one still appears, in English, without changing the application. |
| `target` | `fonts` (the `fonts` folder, accepts `.ttf .otf .ttc .txt`) or `plugins` (the `plugins` folder, accepts `.jar .txt`). |
| `asset` | The name of the file in the release. A `.zip` is unpacked (plain files only, no folders), any other file is copied as it is. |
| `release` | The tag of the release that holds the asset; `latest` (the default) is the latest published release. |
| `url` + `sha256` | Instead of `asset`: a file served over HTTPS, with its SHA-256 written here. |
| `files` | Optional. The files that tell that the component is installed, even if the application did not install it. |
| `size` | Optional, in bytes, shown for a `url` file (an asset has it from GitHub). |
| `minVersion` | Optional. A program older than this does not list the component. |

An entry that is not understood is skipped. A plugin is loaded at startup, so it needs a restart of the application.
