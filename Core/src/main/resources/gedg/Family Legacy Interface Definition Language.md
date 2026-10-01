# FLIDL — Family Legacy Interface Definition Language

A little pattern language.

---

## Basic grammar (meta-grammar of FLIDL itself)

- `record Name { ... }` — top-level entity, implicit xref.
- `struct Name { ... }` — reusable structure, no xref of its own.
- `Name = oneof { variant: Type ... }` — sum type, exactly one variant present.
- Cardinality as a field suffix: *(none)* = `{1:1}`, `?` = `{0:1}`, `*` = `{0:M}`, `+` = `{1:M}`.
- `require one_of(a, b)` — at least one of the two fields must be present (executable constraint).
- `require if field == value: other_field` — conditional requirement.
- `enum { A, B, C, ... }` — the trailing ellipsis marks that custom values ​​are permitted (equivalent to your `<CUSTOM_TYPE>` alongside a closed list).
- `Xref<Type>` — reference to a record of that type. `Xref<Id>` — polymorphic reference (INDIVIDUAL, GROUP, or any other supported record type).
- `XrefOrVoid<Type>` — as above, but also allows the "unidentified entity" marker (equivalent to your `@<XREF:ID>@|@VOID@`).

**Cardinality as field name suffix** (replaces `{n:m}`):

| Suffix | Meaning | GEDCOM-style equivalent |
|---|---|---|
| *(none)* | exactly one, mandatory | `{1:1}` |
| `?` | zero or one | `{0:1}` |
| `*` | zero or more | `{0:M}` |
| `+` | one or more | `{1:M}` |

**Executable cross-field constraints**, declared within the structure instead of in a comment:

```
require one_of(field_a, field_b) // at least one of the two present
require if basis == conventional: cultural_norm // mandatory conditional
```

**Types**: scalars (`Text`, `Int`, `Date`, `Duration`, `Uri`), references (`Xref<Type>`), open enums (`enum { A, B, C, ... }` — the final three dots indicate "custom values permitted", just like today you write `<EVENT_TYPE>` next to a closed list).

---

## 1. EVENT_PARTICIPATION_RECORD in FLIDL

```
record EventParticipation {
  event:        Xref<Event>                      // event in which you participate
  entity:       Xref<Id>                          // INDIVIDUAL | GROUP | ...
  role?:        enum { CHILD, PARENT, SPOUSE, POWER_OF_ATTORNEY, PRISONER, ... }
  note*:        Xref<Note>
  citation*:    SourceCitation
  evidence?:    EvidenceQualifiers
  restriction?: RestrictionStructure
  modification: ModificationStructure
}
```

Direct comparison to the original — 9 lines versus the current 9 lines, but with no level numbers to count, without having to remember what `{0:M}` means at a glance, and with `Xref<Event>` bringing the target type into the reference itself (in the original `@<XREF:EVENT>@` is already like this, so not much changes here — this record was already the cleanest).

---

## 2. DATE_STRUCTURE / DATE_VALUE in FLIDL — the case that really matters

```
struct DateStructure {
  value:      DateValue
  citation*:  SourceCitation
  evidence?:  EvidenceQualifiers
}

DateValue = oneof {
  point:     QualifiedDate
  bounded:   Bounded
  spanning:  Spanning
}

struct Bounded {
  not_before?: QualifiedDate
  not_after?:  QualifiedDate
  require one_of(not_before, not_after)
}

struct Spanning {
  from?: QualifiedDate
  to?:   QualifiedDate
  require one_of(from, to)
}

struct QualifiedDate {
  single_date:  SingleDate
  approximate?: Approximate
}

struct Approximate {
  basis?:         enum { stated, calculated, conventional, unspecified }
  cultural_norm?: Xref<CulturalNorm>
  margin?:        Duration
  require if basis == conventional: cultural_norm
}

SingleDate = oneof {
  full_date: { value: Date, calendar: CalendarType }
  decade:    { start_year: Int, calendar: CalendarType }
  century:   { ordinal: Int, part?: CenturyPart, calendar: CalendarType }
}
```
