FamilyLegacy

## TODO

- **PDF report font.** The PDF renderer (`PdfReportRenderer` in the `ReportPdf` module, OpenPDF) uses the standard Helvetica and Courier fonts, which are not embedded and cover only Latin text. A report in Arabic, Hebrew, Persian, Cyrillic, Greek, an Indic script, Thai, Chinese, Japanese or Korean may show missing letters in the PDF (Word and HTML are not affected). Embed a Unicode font with a licence that allows it (for example a Noto family) and choose it by the language of the report, then check each language of `Domain/src/main/resources/i18n/report/`.
