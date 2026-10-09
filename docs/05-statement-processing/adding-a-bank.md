# Adding a bank

Spendwise is a product for everyone, so statement support must scale to every Indian bank and wallet. Adding one is meant to be
writing a single class plus one sample file, with nothing else edited. This is the whole procedure.

## 1. Implement `StatementParser`
Create `infrastructure/pdf/<Bank>StatementParser.java` as a Spring `@Component` with a no-argument constructor. Spring finds it
and the detector (`StatementFormatDetector`) picks it up automatically; there is no registry, switch or bank list to edit.

| Method | What it must do |
|---|---|
| `bankName()` | Stable unique code, e.g. `HDFC_BANK`. It becomes the staged rows' source and the sample file name. |
| `displayName()` | The name users see, e.g. "HDFC Bank". The "supported banks" message is built from these. |
| `matches(text)` | True only for *this bank's own statements*. Require this bank's own structure (a row in its layout, its own section title or footer), never just its name: other statements mention other banks in narration and linked-account labels. |
| `identityPosition(text)` | Index of this bank's own name/title/footer in the text. Breaks a tie if two parsers match; the letterhead precedes narration that merely mentions another bank. |
| `parse(text)` | Rows with date, positive amount, debit/credit, raw narration and (when the bank prints one) the payment reference. |

Work from the text PDFBox actually extracts (`PDFTextStripper.getText`), never from how the PDF looks: real exports wrap
narration mid-token, put several columns on one line, or split one transaction across lines. Debit vs credit may have to be
inferred from the running balance when there are no separate columns (Bank of Baroda).

## 2. Optionally implement `DescriptionCleaner`
If the bank's narration is noisy (UPI references, timestamps, bank suffixes), add a cleaner with the same `bankName()`. It only
changes the display description; classification always uses the raw text.

## 3. Add a synthetic sample
Add `src/test/resources/statements/samples/<bankName>.txt`: a short, invented statement in the bank's layout with the bank named in
the header, a few rows, and narration that mentions *other* banks, Paytm and UPI handles. Never use real statement data in the repo.

## 4. Run the contract test
`StatementParserContractTest` finds every parser on the classpath and checks, with no edits, that each one: has a unique name,
recognises and parses its own sample, and is detected as itself, and that no other bank claims it, whatever order the parsers are
registered in. If your bank collides with another, this fails before anything ships.

## 5. Verify against a real statement
Synthetic samples prove the plumbing, not the layout. Before calling a bank supported, run one real statement through the running app
and check the row count and totals against the totals the statement itself prints (or opening/closing balance). Every bank so far
(Paytm, Bank of Baroda) only worked correctly after this step: a real file revealed assumptions no sample did.

## What the detector does for you
- No parser matches: "isn't recognized yet", listing the supported banks.
- One matches: used.
- Several match: the one whose own name appears earliest wins; a genuine tie is refused with an explanatory error rather than guessed.

## Current banks
| Code | Bank | Verified on a real statement |
|---|---|---|
| `PAYTM_WALLET` | Paytm Wallet | Yes |
| `BOB` | Bank of Baroda | Yes |
| `AXIS_BANK` | Axis Bank | Not yet (synthetic only) |
| `UJJIVAN` | Ujjivan Small Finance Bank | Not yet (synthetic only) |
| `HDFC_BANK` | HDFC Bank | Not yet (synthetic only) |
| `SBI` | State Bank of India | Not yet (synthetic only) |
