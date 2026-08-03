# 📐 FLEF Architecture Specification v0.1.0

## Core modeling principles:

1. Individuals, groups, events, relationships, places, and sources are represented as independent records.
2. Events describe things that happened.
   Relationships describe enduring associations.
3. Assertions may be supported by evidence, challenged by evidence, or resolved through conclusions.
4. Sources describe evidence.
   Conclusions describe interpretations of evidence.
5. Historical uncertainty is modeled explicitly, not hidden.

The features that truly distinguish it from GEDCOM and many genealogical models are:
- explicit separation between source and interpretation
- explicit separation between evidence and conclusion
- preservation of conflicting claims
- management of identity hypotheses
- structured research tracking
- historical relationships between places
- support for normalization and transcription of original data

Architectural Evaluation

Looking at the format as a whole, I now see a very clear structure:

| Livello        | Record                              |
| -------------- | ----------------------------------- |
| Entità         | Individual, Group, Place            |
| Eventi         | Event, EventParticipation           |
| Relazioni      | Relationship, PlaceRelationship     |
| Attributi      | IndividualAttribute, GroupAttribute |
| Fonti          | Source, Repository                  |
| Contesto       | CulturalNorm, HistoricEvent         |
| Ricerca        | ResearchStatus, ResearchLog         |
| Incertezza     | IdentityHypothesis                  |
| Valutazione    | EvidenceQualifiers                  |
| Conclusione    | Conclusion                          |
| Documentazione | Note                                |

The separation is clean and, above all, does not mix data, evidence, and interpretation, which is GEDCOM's main flaw.
## Abstract Data Model

### 1. Core Entities

An Entity is any distinct, identifiable object in the genealogical domain that can exist independently and be referenced by other objects.

| Entity Type | Description | Key Attributes |
| --- | --- | --- |
| Individual | A historical person | Names, sex, cultural norms, notes, source citations, preferred image, restriction, conclusions, modification metadata |
| Group | A collection of entities (family, household, club, etc.) | Names, type, cultural norms, notes, source citations, preferred image, restriction, conclusions, modification metadata |
| Event | A historical occurrence | Type, date, place, agency, cause, cultural norms, notes, source citations, evidence qualifiers, restriction, conclusion, modification metadata |
| Place | A geographical or administrative location | Names, type, map coordinates, source citations, evidence qualifiers, restriction, conclusions, modification metadata |
| Source | A document, record, or artifact used as evidence | Titles, author, place of creation, date of creation, publisher, repository citations, media type, document structures, source citations, notes, restriction, conclusions, modification metadata |
| Note | A free-text annotation | Title, value, MIME type, locale, translations, source citations, restriction, modification metadata |
| Repository | A location where sources are held | Names, associated individual, place, contact methods, notes, restriction, modification metadata |

### 2. Relationships Between Entities

Relationships connect entities and are themselves first-class objects that can have their own properties (dates, evidence, conclusions).

| Relationship Type |Description | Direction |
| --- | --- |
| EventParticipation | An entity's role in an event | Entity ↔ Event |
| Relationship | An enduring association between two entities | Entity ↔ Entity |
| PlaceRelationship | An association between two places | Place ↔ Place |
| SourceCitation | A reference from any entity to a source | Any Entity → Source |
| RepositoryCitation | A reference from a source to a repository | Source → Repository |

### 3. Supporting Structures

These are reusable structures that appear within multiple entities.

| Structure | Purpose | Contained In |
| --- | --- |
| PersonalNameStructure | A structured personal name with parts (given, family, etc.) | Individual |
| NameStructure | A simple name with optional locale and validity dates | Group, Place, Repository |
| TextValue | A textual value with locale, validity dates, and variants | NameStructure, Source title |
| TextValueVariant | Alternative representations (phonetic, transliterated) | PersonalNameStructure, TextValue |
| EventStructure | Details of an event (date, place, agency, cause, etc.) | Event |
| PlaceStructure | A reference to a place with evidence qualifiers | Various |
| SourceCitation | Evidence linking an entity to a source | Various |
| DocumentStructure | File reference with metadata (extract, mapping, etc.) | Source |
| DateStructure | A historical date (point, bounded, spanning, approximate) | Various |
| ModificationStructure | Creation and update metadata | All entities |
| ConclusionStructure | A formal conclusion with proof status | Various |
| EvidenceQualifiers | Certainty and credibility assessments | Various |
| RestrictionStructure | Privacy and distribution controls | Various |
| ContactStructure | Phone, email, or web contact information | Submitter, Repository |
| IdentityHypothesis | A possible duplicate identity | Independent record |
| ResearchStatus | A research question with status | Independent record |
| ResearchLog | A chronological record of research activity | Independent record |

### 4. Entity-Relationship Diagram (Conceptual)

```text
┌──────────────┐     ┌─────────────────────┐     ┌──────────────┐
│  INDIVIDUAL  │────▶│ EVENT_PARTICIPATION │◀────│    EVENT     │
└──────────────┘     └─────────────────────┘     └──────────────┘
       │                       │                        │
       │                       │                        │
       ▼                       ▼                        ▼
┌──────────────┐     ┌─────────────────────┐     ┌──────────────┐
│ RELATIONSHIP │────▶│     RELATIONSHIP    │◀────│    PLACE     │
└──────────────┘     └─────────────────────┘     └──────────────┘
       │                       │                        │
       │                       │                        │
       ▼                       ▼                        ▼
┌──────────────┐     ┌─────────────────────┐     ┌──────────────┐
│    GROUP     │────▶│ PLACE_RELATIONSHIP  │◀────│   SOURCE     │
└──────────────┘     └─────────────────────┘     └──────────────┘
       │                                                │
       │                                                │
       ▼                                                ▼
┌──────────────┐                                 ┌──────────────┐
│ GROUP_ATTR   │                                 │  REPOSITORY  │
└──────────────┘                                 └──────────────┘
```

## 📄 Serialization Binding (Line-List Format)

### 1. Mapping Rules

| Model Element | Serialization Pattern |
| --- | --- |
| Entity Record | n @<XREF:TYPE>@ TYPE at level 0 |
| Attribute | +1 TAG <VALUE> at increasing levels |
| Nested Structure | +n <<STRUCTURE>> with children at +(n+1) |
| Reference | @<XREF:ID>@ |
| Union | [ OPTION1 | OPTION2 ] |
| Multiplicity | {min:max} where M = unlimited, 1 = required, 0 = optional |

### 2. Type Mapping

| Model Type | Serialization Representation |
| --- | --- |
| TEXT | Free-form string |
| DATE | ISO 8601 date string (YYYY-MM-DD) |
| RESOURCE_URI | RFC 3986 compliant URI |
| XREF | @<ID>@ where ID is alphanumeric |
| LOCALE_CODE | IETF BCP 47 language tag |
| MIME_TYPE | IETF RFC 2046 MIME type |
| DURATION | ISO 8601 duration (e.g., P2Y) |
| BOOLEAN | Y or N |
| ORDINAL | Integer (e.g., 15 for 15th century) |

### 3. Entity to Record Mapping

#### 3.1 Individual Entity → INDIVIDUAL_RECORD

```text
INDIVIDUAL_RECORD :=
n @<XREF:INDIVIDUAL>@ INDIVIDUAL    {1:1}
  +1 <<PERSONAL_NAME_STRUCTURE>>    {0:M}
  +1 SEX <SEX_VALUE>    {0:1}
  +1 CULTURAL_NORM @<XREF:CULTURAL_NORM>@    {0:M}
  +1 NOTE @<XREF:NOTE>@    {0:M}
  +1 <<SOURCE_CITATION>>    {0:M}
  +1 PREFERRED_IMAGE <RESOURCE_URI>    {0:1}
    +2 CROP <CROP_COORDINATES>    {0:1}
  +1 <<RESTRICTION_STRUCTURE>>    {0:1}
  +1 <<CONCLUSION_STRUCTURE>>    {0:M}
  +1 <<MODIFICATION_STRUCTURE>>    {1:1}
```

#### 3.2 Event Entity → EVENT_RECORD

```text
EVENT_RECORD :=
n @<XREF:EVENT>@ EVENT    {1:1}
  +1 TYPE <EVENT_TYPE>    {1:1}
  +1 <<EVENT_STRUCTURE>>    {0:1}
3.3 Participation Relationship → EVENT_PARTICIPATION_RECORD
text
EVENT_PARTICIPATION_RECORD :=
n @<XREF:EVENT_PARTICIPATION>@ EVENT_PARTICIPATION    {1:1}
  +1 EVENT @<XREF:EVENT>@    {1:1}
  +1 ENTITY @<XREF:ID>@    {1:1}
  +1 ROLE <ROLE_IN_EVENT>    {0:1}
  +1 <<SOURCE_CITATION>>    {0:M}
  +1 <<EVIDENCE_QUALIFIERS>>    {0:1}
  +1 NOTE @<XREF:NOTE>@    {0:M}
  +1 <<RESTRICTION_STRUCTURE>>    {0:1}
  +1 <<MODIFICATION_STRUCTURE>>    {1:1}
```

#### 3.4 Relationship Entity → RELATIONSHIP_RECORD

```text
RELATIONSHIP_RECORD :=
n @<XREF:RELATIONSHIP>@ RELATIONSHIP    {1:1}
  +1 SUBJECT @<XREF:ID>@|@VOID@    {1:1}
  +1 OBJECT @<XREF:ID>@|@VOID@    {1:1}
  +1 TYPE <RELATIONSHIP_TYPE>    {1:1}
  +1 ROLE <RELATIONSHIP_ROLE>    {0:1}
  +1 STATUS <RELATIONSHIP_STATUS>    {0:1}
  +1 <<DATE_STRUCTURE>>    {0:1}
  +1 VALID_FROM    {0:1}
    +2 <<DATE_STRUCTURE>>    {1:1}
  +1 VALID_TO    {0:1}
    +2 <<DATE_STRUCTURE>>    {1:1}
  +1 <<EVIDENCE_QUALIFIERS>>    {0:1}
  +1 NOTE @<XREF:NOTE>@    {0:M}
  +1 <<SOURCE_CITATION>>    {0:M}
  +1 <<RESTRICTION_STRUCTURE>>    {0:1}
  +1 <<CONCLUSION_STRUCTURE>>    {0:1}
  +1 <<MODIFICATION_STRUCTURE>>    {1:1}
```

## 🔧 Extensibility Pattern

### 1. Custom Extensions

Extensions are declared using a namespace prefix to avoid collisions.

Syntax: `+n <NAMESPACE>:<TAG>`

Examples:

- `+1 MYAPP:CUSTOM_ATTRIBUTE "value"`
- `+1 FAMILYSEARCH:LINK @UUID@`

### 2. Extension Declaration

Extensions SHOULD be declared in the HEADER to inform parsers of their presence and meaning.

```text
HEADER :=
n HEADER    {1:1}
  ...
  +1 EXTENSIONS    {0:1}	/* List of custom extensions used in this file. */
    +2 EXTENSION    {1:M}
      +3 NAMESPACE <TEXT>    {1:1}	/* e.g., 'DNAAPP' */
      +3 VERSION <VERSION_NUMBER>    {0:1}	/* Version of the extension. */
      +3 URI <RESOURCE_URI>    {0:1}	/* Documentation URI for the extension. */
      +3 DESCRIPTION <TEXT>    {1:1}	/* Brief description of the extension. */
      +3 LICENSE <TEXT>    {0:1}	/* License under which the extension is provided. */
      +3 CONTACT <TEXT>    {0:1}	/* Contact information for the extension developer. */
      +3 SCHEMA <RESOURCE_URI>    {0:1}	/* URI to a JSON Schema for validating the extension. */
```

### 3. Extension Usage Rules

1. All custom tags MUST use a namespace prefix followed by a colon (e.g., `MYAPP:MY_TAG`).
2. Parsers MAY ignore any tag they do not recognize, but SHOULD preserve it when round-tripping.
3. Extensions SHOULD NOT redefine existing FLEF tags or structures.

### 4. Example Extension Declaration

```text
0 HEADER
  1 PROTOCOL FLEF
    2 NAME "Family Legacy Format"
    2 VERSION "0.1.0"
  1 EXTENSIONS
    2 EXTENSION
      3 NAMESPACE "DNAAPP"
      3 URI "https://dnaapp.com/flef-extension"
      3 DESCRIPTION "DNA matching and segment data"
    2 EXTENSION
      3 NAMESPACE "MYCLAN"
      3 URI "https://myclan.org/flef-extension"
      3 DESCRIPTION "Clan membership and lineage data"
```

### 5. Using Extensions

```text
0 @I123@ INDIVIDUAL
  1 NAME
    2 PART
      3 TYPE given
      3 VALUE "John"
  1 DNAAPP_HAPLOGROUP "R1b1a2"
  1 DNAAPP_MATCH @DNA1@
  1 MYCLAN_CLAN_NAME "MacDonald"
  1 MYCLAN_TARTAN "Red Royal"
```

## 🔄 Versioning Strategy

### 1. Semantic Versioning

FLEF follows Semantic Versioning (MAJOR.MINOR.PATCH) as defined in the HEADER:

- **MAJOR**: Incompatible changes to the abstract model or core syntax.
- **MINOR**: Backward-compatible additions to the model.
- **PATCH**: Backward-compatible bug fixes or clarifications.

### 2. Extension Versioning

Extensions SHOULD declare their own version:

```text
+2 EXTENSION
  3 NAMESPACE "DNAAPP"
  3 VERSION "2.0.0"
  3 URI "https://dnaapp.com/flef-extension"
```

### 3. Forward Compatibility

Parsers SHOULD:

- Ignore unrecognized tags (but preserve them).
- Ignore unrecognized attributes.
- Continue parsing even if required fields are missing (graceful degradation).
- Report warnings but not errors for unrecognized content.

### 4. Schema Validation

A formal schema (e.g., XML Schema, JSON Schema, or a custom schema language) SHOULD be provided for validation:

- **Base Schema**: Validates core FLEF structures.
- **Extension Schemas**: Validates extension-specific structures (optional).

## 📊 Relationship Between Model and Serialization (Summary)

| Level | Purpose | Format | Who Defines |
| --- | --- | --- | --- |
| Conceptual Model | What to represent | UML / Entity-Relationship | Protocol Specification |
| Serialization Binding | How to represent it | Line-list syntax | Protocol Specification |
| Extension Model | How to extend | Namespace + Schema | Extensions Developers |
| Instance | Actual data | FLEF file | Data Producers |

## 🎯 Benefits of This Approach

1. **Separation of Concerns**: The conceptual model can evolve independently of the serialization format.
2. **Multiple Serializations**: In the future, FLEF could support JSON, XML, or binary formats without changing the core model.
3. **Clean Extensibility**: Extensions are first-class citizens with their own namespace, preventing collisions.
4. **Validation**: A formal model enables automated validation tools.
5. **Documentation**: The model serves as documentation for developers.
6. **Interoperability**: Clear relationships between entities enable better data integration.

## 📝 Implementation Notes

**For Developers Implementing FLEF**

1. **Start with the Abstract Model**: Understand the entities and their relationships first.
2. **Use the Serialization Mapping**: Follow the line-list syntax for file I/O.
3. **Respect Extensions**: Pass through unknown tags and handle extensions as documented.
4. **Validate Against the Model**: Ensure your implementation aligns with the conceptual model.

**For Extension Developers**

1. Choose a unique namespace (e.g., MYAPP).
2. Document your extension with URI and description.
3. Declare it in the HEADER.
4. Follow the naming convention: <NAMESPACE>_<TAG>.
5. Provide a schema (optional but recommended).


## FLEF 0.1.1 Example

avendo questo file di esempio, che segue il protocollo FLEF 0.1.1

```
header {
  protocol {
    name Family LEgacy Format
    version 0.1.1
  }
  source {
    system_id MyGenealogySoftware
    name My Genealogy Software
    version 1.0.0
  }
  date 2026-07-31
  submitter {
    name Mario Rossi
    contact {
      address mario.rossi@example.com
      type personal
    }
  }
  scope Example family
}
records {
  individual {
    id I1
      name {
        part {
          type given
          value Mario
        }
        part {
          type family
          value """
            Rossi
            Bianchi
          """
        }
      }
    sex MALE
    modification {
      creation {
        date 2026-07-31
      }
    }
  }
  note {
    id N1
    value Individuo presente nel registro di nascita.
    modification {
      creation {
        date 2026-07-31
      }
    }
  }
  event {
    id E1
    type BIRTH
    detail {
      date {
        value {
          point {
            single_date {
              full_date {
                value 1894-03-17
                calendar gregorian
              }
            }
          }
        }
      }
      modification {
        creation {
          date 2026-07-31
        }
      }
    }
  }
  event_participation {
    id EP1
    event {
      event E1
    }
    participant {
      individual I1
    }
    role CHILD
    modification {
      creation {
        date 2026-07-31
      }
    }
  }
}
```

con la convenzione che se il campo contiene ritorni a capo allora deve essere circondato da `""""`

implementa il parser, verifica che parsi correttamente un file di esempio con tutte le casistiche che possono capitare, fai la validazione del file passato in input contro il parsing della grammatica dato in una struttura FLEFGrammar

dopo il primo IDENT, tutto quello che segue prima del ritorno a capo, fa parte del valore, a meno che non cominci con tre doppie virgolette, e allora tutto quello che è chiuso tra triple virgolette doppie, ritorni a capo inclusi, fa parte del valore
