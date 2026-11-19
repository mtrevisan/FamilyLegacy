# FamilyLegacy - Proposed Feature Roadmap

## General Requirements

Before implementing any feature:

- Analyze the existing architecture, Domain model, FLEF model, Importers, Reports, and UI layers.
- Reuse existing concepts whenever possible.
- Avoid duplication of functionality.
- Preserve backward compatibility with existing FLEF files.
- Prefer incremental evolution over large rewrites.
- Design all new features to support future AI-assisted genealogy workflows.
- Keep domain concepts independent from any specific AI vendor or OCR provider.
- Add appropriate tests for all new functionality.
- Integrate with existing Sources, Citations, Media, Notes, Reports, and Search mechanisms.

---

# 1. Research Log

## Goal

Provide a structured genealogy research workspace.

Researchers must be able to track:

- research objectives
- repositories consulted
- searches performed
- positive findings
- negative findings
- unresolved questions
- next actions

## Suggested Model

```text
ResearchTask
    id
    title
    objective
    status
    priority
    createdDate
    completedDate

ResearchEntry
    date
    repository
    description
    result

Finding
    type
    description
    sourceCitation

NextAction
    description
    dueDate
```

## Features

- Create research tasks.
- Link tasks to:
  - individuals
  - families
  - places
  - events
  - sources
- Record negative searches.
- Record repositories consulted.
- Record unresolved questions.
- Record future actions.

## UI

Create:

```text
Research Workspace
```

showing:

- Open Tasks
- Completed Tasks
- Recent Findings
- Next Actions

## Reports

Generate:

```text
Research Summary Report
```

---

# 2. Evidence Analysis View

## Goal

Introduce an evidence-centric genealogy workflow.

Users should be able to evaluate the quality of the evidence supporting a conclusion.

## Concepts

Introduce and integrate:

```text
Evidence
Assertion
Conflict
Conclusion
Confidence
```

## Features

For facts such as:

- Birth
- Marriage
- Death
- Residence
- Occupation
- Relationship

display:

- Supporting citations
- Contradicting citations
- Evidence quality
- Confidence level
- Open conflicts

## Example

```text
Birth

Evidence:
  Baptism Register

Conflicting Evidence:
  Census Record

Confidence:
  82%

Status:
  Conflict Present
```

## UI

Create:

```text
Evidence Panel
```

accessible from:

- persons
- families
- events
- places

---

# 3. Source-Centric Workspace

## Goal

Support archive-based genealogy workflows.

Many genealogists work primarily from sources rather than people.

## Features

For every source:

```text
Source
 ├ Individuals referenced
 ├ Families referenced
 ├ Events referenced
 ├ Citations
 ├ Media
 └ Extracts
```

Enable reverse navigation:

```text
Source
    → Citation
        → Person
```

## Additional Views

Provide:

```text
Sources by Repository
Sources by Archive
Sources by Collection
Sources by Record Group
```

---

# 4. Citation Coverage Report

## Goal

Identify poorly documented areas of the tree.

## Metrics

Calculate:

```text
Individuals without citations
Families without citations
Events without citations
Places without citations
Media without citations
```

## Dashboard

Create:

```text
Documentation Coverage
```

Highlight:

- percentages
- missing evidence hotspots
- undocumented branches

## Reports

Generate coverage reports.

---

# 5. Genealogical Proof Report

## Goal

Support the Genealogical Proof Standard (GPS).

## Structure

```text
Research Question

Evidence Summary

Conflicting Evidence

Analysis

Reasoning

Conclusion

Confidence
```

## Export

Support:

- PDF
- DOCX
- EPUB
- Site Export

Reuse existing Report infrastructure.

---

# 6. OCR → Extract → Citation Workflow

## Goal

Convert scanned documents into structured genealogy evidence.

## Workflow

```text
Image
    ↓
OCR
    ↓
Extract
    ↓
Review
    ↓
Source Citation
    ↓
Entity Linking
```

## Provider Architecture

Create pluggable OCR adapters:

```text
OCRProvider
```

Future providers may include:

- Azure AI Vision
- Google Document AI
- Transkribus
- GPT Vision
- Claude Vision
- local OCR engines

## Extract Types

Support:

```text
Verbatim
Transcription
Transliteration
Translation
Summary
```

## Entity Linking

Allow linking extracted content to:

- individuals
- families
- events
- places
- sources

---

# 7. CSV / Spreadsheet Import Framework

## Goal

Import genealogy data from arbitrary spreadsheets.

## Supported Input

- CSV
- TSV
- XLSX

## Workflow

```text
Column
       ↓
Field Mapping
       ↓
Validation
       ↓
Import
```

Example:

```text
Name       → Individual.name
BirthDate  → Birth.date
Village    → Birth.place
Occupation → Occupation.description
```

## Features

- Mapping wizard
- Profile saving
- Import previews
- Validation before import
- Reusable templates

---

# 8. Evidence Package Export

## Goal

Create portable genealogy research packages.

## Output Structure

```text
Package.zip

 ├ dataset.flef

 ├ sources/
 ├ media/
 ├ extracts/
 ├ citations/
 ├ reports/

 └ metadata.json
```

## Metadata

Include:

```text
Creation Date
FamilyLegacy Version
Author
Source Count
Citation Count
Extract Count
Media Count
Record Count
```

## Use Cases

- sharing with researchers
- archival backups
- collaboration
- publication packages

---

# 9. Research Timeline

## Goal

Track genealogy work over time.

This is different from an individual’s life timeline.

## Events

Track:

```text
Research task created
Repository visited
Source added
OCR completed
Extract created
Citation created
Conclusion updated
Conflict resolved
Research task completed
```

## Filters

Support:

- person
- family
- source
- date range
- repository

---

# 10. Relationship and Association Network View

## Goal

Visualize social and documentary connections.

## Include

Not only family links:

```text
Parents
Children
Siblings
Spouses

Witnesses
Godparents
Sponsors
Associates
Neighbors
Shared Sources
Shared Events
Shared Places
```

## Features

- Interactive graph
- Expand/Collapse nodes
- Highlight path
- Shortest relationship path
- Source-based connections
- Event-based connections

Reuse existing graph components where possible.

---

# 11. Inference Inspection View

## Goal

Prepare FamilyLegacy for explainable genealogy reasoning.

Every conclusion must be traceable.

## Display

```text
Conclusion

Evidence Used

Reasoning Chain

Confidence

Alternative Interpretations

Conflicting Evidence
```

## Requirement

No black-box reasoning.

Users must always be able to inspect:

- why
- how
- based on which evidence

a conclusion was reached.

---

# 12. AI Assistant Ready Architecture

## Goal

Prepare for future AI-assisted genealogy without coupling to a specific provider.

## Introduce Abstractions

```text
OCRProvider

ExtractionProvider

InferenceProvider

ResearchAssistant
```

## Initial Scope

Do not implement provider-specific integrations.

Instead provide architecture that can later support:

- GPT
- Claude
- Azure AI
- local models
- future providers

## UI Requirements

Support displaying:

- AI suggestions
- proposed citations
- possible record matches
- confidence scores
- explanation chains

All final decisions must remain under user control.

---

# Deliverables

Before implementation starts, produce:

## Architecture Analysis

- affected modules
- affected packages
- required refactorings

## Data Model Analysis

- schema changes
- entity changes
- compatibility considerations

## Migration Strategy

- FLEF compatibility
- database migration
- report migration
- import/export migration

## Risk Assessment

For every feature estimate:

- complexity
- maintenance cost
- architectural impact
- future extensibility

## Recommended Execution Order

Prioritize according to:

1. Architectural value
2. Benefit for genealogists
3. Long-term maintainability
4. Development effort
5. Future AI-readiness
