# Customer Addresses

## Problem Statement

Today, clerks can only record a customer's name, email, phone number and date of birth. There is no way to store postal addresses. This removes the basis for sending invoices or delivering goods: it is unknown where an invoice should go and to which locations deliveries are allowed. In addition, the customer list does not show where a customer is located.

## Solution

Every customer gets a list of addresses. An address consists of street, house number, optional address addition, zip code, city and country, and can optionally be marked as a billing address (`BILLING`) or delivery address (`DELIVERY`). When creating and editing a customer, there must be exactly one billing address and at least one delivery address; in addition, any number of addresses without a type are allowed.

Addresses are maintained (add, change, remove) directly in the existing customer form in a dedicated "Adressen" section. Input is validated immediately in the form, including country-dependent zip code validation for Germany, Austria and Switzerland, and validated again by the server. The customer list shows each customer's billing address in a separate column.

Existing customers without addresses remain readable and deletable; the required addresses only have to be added the next time they are saved.

## User Stories

1. As a clerk, I want to assign multiple addresses to a customer, so that I have all relevant addresses of a customer in one place.
2. As a clerk, I want to mark an address as billing address, so that it is clear where invoices are sent.
3. As a clerk, I want to mark one or more addresses as delivery address, so that deliveries to different locations are possible.
4. As a clerk, I want to be able to record addresses without a type, so that I can keep track of other known addresses.
5. As a clerk, I want a customer not to be saved without a billing address, so that invoicing is always possible.
6. As a clerk, I want a customer not to be saved with two billing addresses, so that it is unambiguous which billing address applies.
7. As a clerk, I want a customer not to be saved without a delivery address, so that deliveries can always be made.
8. As a clerk, I want street, house number, zip code, city and country to be required, so that every address is postally complete.
9. As a clerk, I want to be able to enter an optional address addition (e.g. "c/o", "Hinterhaus", "3. OG"), so that shipments arrive even in complicated delivery situations.
10. As a clerk, I want to choose the country from a selection list with German country names, so that I don't need to know country codes and no invalid countries are recorded.
11. As a clerk, I want Germany, Austria and Switzerland at the top of the country selection, so that I can quickly find the most common countries.
12. As a clerk, I want Germany to be preselected for a new address, so that normally I don't have to change anything.
13. As a clerk, I want a German zip code to require exactly five digits, so that typos stand out immediately.
14. As a clerk, I want an Austrian or Swiss zip code to require exactly four digits, so that typos stand out immediately.
15. As a clerk, I want to be able to enter foreign zip codes in their respective format (e.g. "SW1A 1AA"), so that international customers can be maintained as well.
16. As a clerk, I want a house number to have to start with a digit (e.g. "12", "12a", "12-14"), so that swapped input is detected.
17. As a clerk, I want the city not to contain digits, so that zip code and city are not accidentally mixed up.
18. As a clerk, I want to see error messages directly at the affected address field, so that I know what to correct.
19. As a clerk, I want to see error messages about the billing/delivery address rule above the address section, so that I can tell which address is missing or duplicated.
20. As a clerk, I want to see all error messages in German, so that they match the rest of the UI.
21. As a clerk, I want to find an empty block for the billing address and one for the delivery address when creating a customer, so that I can fill in the normal case right away.
22. As a clerk, I want to add further addresses via "Adresse hinzufügen", so that I can record additional delivery addresses.
23. As a clerk, I want a newly added address to be preset as a delivery address, so that I don't have to change the most common case.
24. As a clerk, I want to delete any address via "Entfernen", so that outdated addresses disappear.
25. As a clerk, I want to see an error message after removing a required address instead of a disabled button, so that I can freely rearrange and correct.
26. As a clerk, I want to change a customer's existing addresses, so that moves can be maintained.
27. As a clerk, I want to change the type of an existing address, so that I can e.g. turn a delivery address into the billing address.
28. As a clerk, I want the order of my addresses to be preserved, so that the display looks the same after saving.
29. As a clerk, I want to see a "Rechnungsadresse" column in the customer list (e.g. "Musterstraße 12a, 26122 Oldenburg"), so that I can quickly identify customers.
30. As a clerk, I want to sort the customer list by billing address, so that I can group customers regionally.
31. As a clerk, I want to search the customer list by parts of the billing address, so that I can find customers by their city.
32. As a clerk, I want to see a "–" in the billing address column for existing customers without an address, so that I can recognize incomplete customers.
33. As a clerk, I want to still be able to open and view existing customers without addresses, so that the introduction of addresses does not disrupt operations.
34. As a clerk, I want to be able to delete existing customers without addresses, so that legacy data can be cleaned up.
35. As a clerk, I want to be notified when editing an existing customer that the billing and delivery address are missing, so that I complete the data the next time I save.
36. As a clerk, I want all of a customer's addresses to be removed when the customer is deleted, so that no orphaned data remains.
37. As an API user, I want to send and receive addresses as a list `addresses` in the customer JSON, so that I can maintain addresses without additional endpoints.
38. As an API user, I want to receive the addresses in the customer overview (`GET /api/customers`) as well, so that I don't need individual requests.
39. As an API user, I want to receive precise paths such as `addresses` or `addresses[0].zipCode` in `invalid-params` on validation errors, so that I can map errors programmatically.
40. As an API user, I want to receive a separate error entry for each rule violated at the same time, so that I can fix all problems in one pass.
41. As an API user, I want to receive error messages in German (default) or English depending on the language, so that they fit my client.
42. As an API user, I want invalid country codes to be rejected with a clear message, so that no faulty countries are stored.
43. As an API user, I want an unknown address type to be rejected, so that only `BILLING`, `DELIVERY` or no type is stored.
44. As an API user, I want values that are too long to be rejected with a validation message instead of a database error, so that errors remain understandable.
45. As an API user, I want the field lengths to be documented in the OpenAPI schema, so that I can build my clients correctly.
46. As a developer, I want entity, validation and database lengths to match, so that the architecture test stays green and no inconsistencies arise.

## Implementation Decisions

### Domain model (server)

- The customer record (at the same time embeddable and REST DTO) gets a new field `addresses` as a list of `Address`. Mapping as with `additionalEmails`/`additionalPhoneNumbers`: `@ElementCollection` (EAGER), a separate collection table, `@OrderColumn` on `C_POSITION`. The getter returns an unmodifiable list, or an empty list for `null`.
- `Address` is an `@Embeddable` record with the following fields, each as its own value object record in the project style (`@JsonValue`, Bean Validation, `public static final int MAX_LENGTH`):

| Field (JSON) | Value object | Required | Max. length | Rule | Message (de) |
|---|---|---|---|---|---|
| `type` | `AddressType` | no | 32 | `@OneOf({"BILLING","DELIVERY"})` | existing `OneOf.message` |
| `street` | `Street` | yes | 255 | `@NotBlank` | default |
| `houseNumber` | `HouseNumber` | yes | 16 | `@NotBlank`, pattern `^\d+.*$` | "muss mit einer Ziffer beginnen" |
| `addressAddition` | `AddressAddition` | no | 255 | – | – |
| `zipCode` | `ZipCode` | yes | 10 | `@NotBlank`; country-dependent format (see below) | "ist keine gültige Postleitzahl für das angegebene Land" |
| `city` | `City` | yes | 255 | `@NotBlank`, pattern `\D*` | "darf keine Ziffern enthalten" |
| `country` | `CountryCode` | yes | 2 | new constraint `@CountryCode` | "ist kein gültiger ISO-3166-Ländercode" |

- The address type follows the pattern of `ContactType` (string value object with `@OneOf`) and is optional.

### Validation

- **`@CountryCode`**: new constraint in the validation infrastructure package; valid values are exactly the ISO 3166 alpha-2 codes of the JDK (`Locale.getISOCountries()`). No additional library.
- **Country-dependent zip code validation**: class-level constraint on `Address`.
  - `DE`: exactly 5 digits
  - `AT`, `CH`: exactly 4 digits
  - all other countries: `^[A-Za-z0-9 -]{1,10}$`
  - The violation is reported on `zipCode` via `addPropertyNode` (path e.g. `addresses[0].zipCode`).
- **List rule**: class-level constraint on the customer record.
  - exactly one address with type `BILLING` → otherwise error on path `addresses`, "muss genau eine Rechnungsadresse enthalten"
  - at least one address with type `DELIVERY` → otherwise error on path `addresses`, "muss mindestens eine Lieferadresse enthalten"
  - If both rules are violated at the same time, they produce two separate entries.
  - Addresses without a type are allowed without limit.
- Validation applies via the existing `@Valid` on `POST`/`PUT`. No validation takes place on read and delete; existing customers without addresses therefore remain functional.
- All new messages are maintained in the existing ValidationMessages bundles (default, `_de`, `_en`); German remains the default language.
- Errors are delivered via the existing ValidationExceptionHandler as `invalid-params` with `name`/`reason` – no new error format.

### Database

- New Flyway migration V1.3.0 creates `tab_customer_address`:
  - `c_customer_id` (FK to `tab_customer`, `ON DELETE CASCADE`), `c_position`
  - `c_type VARCHAR(32)` nullable, `c_street VARCHAR(255)`, `c_house_number VARCHAR(16)`, `c_address_addition VARCHAR(255)` nullable, `c_zip_code VARCHAR(10)`, `c_city VARCHAR(255)`, `c_country VARCHAR(2)`
  - PK `(c_customer_id, c_position)`, naming conventions `pk_`/`fk_` as in V1.2.0
- No minimum-count constraints in the database; no data migration for existing customers.
- Column lengths match `MAX_LENGTH`/`@Size` exactly, so that `OpenApiConstraintArchitectureTest` stays green.
- The test data gets one billing and one delivery address for each of the two test customers (Max Mustermann, Erika Musterfrau).

### API contract

- No new endpoints. `addresses` is part of the customer JSON for `GET /api/customers/{id}`, `POST`, `PUT` and – via the existing `@JsonUnwrapped` in the customer overview – also for `GET /api/customers`.
- `PUT` replaces the entire address list.
- The order of the list is preserved.
- Example:

```json
"addresses": [
  {
    "type": "BILLING",
    "street": "Musterstraße",
    "houseNumber": "12a",
    "addressAddition": "c/o Firma XY",
    "zipCode": "26122",
    "city": "Oldenburg",
    "country": "DE"
  }
]
```

### Client

- **Domain schema**: The Zod customer schema gets `addresses` with an address schema that mirrors all server rules:
  - required fields, maximum lengths, house number pattern, city without digits
  - country-dependent zip code via `superRefine` at address level (error on field `zipCode`)
  - "exactly one billing address" / "at least one delivery address" via `superRefine` on the list (error on `addresses`)
  - type: `BILLING`, `DELIVERY` or empty
  - The country code is not validated separately against a list; the selection list only allows valid codes.
  - German error messages analogous to the existing ones.
- **Type**: The `Address` type is derived from the schema.
- **Customer form**: new "Adressen" section below the existing fields.
  - Array field (TanStack Form `mode="array"`), one block per address with the fields address type, street, house number, address addition, zip code, city, country.
  - Address type selection: "Rechnungsadresse", "Lieferadresse", "Ohne Typ".
  - Country selection: all ISO countries with German names via `Intl.DisplayNames(['de'], { type: 'region' })`; `DE`, `AT`, `CH` at the top, the rest alphabetically by German name. Preselected `DE`.
  - The "Adresse hinzufügen" button appends a block with type `DELIVERY`, country `DE` and otherwise empty fields.
  - "Entfernen" button per block, always enabled; missing required addresses are displayed as errors.
  - List errors are displayed above the address section, field errors at the respective field.
  - When creating a new customer, two blocks are prefilled: one with `BILLING`, one with `DELIVERY`.
  - When editing, the loaded addresses are adopted; existing customers without addresses start with an empty list and immediately see the list errors.
- **Customer table**: new "Rechnungsadresse" column in the format "Straße Hausnummer, PLZ Ort", sortable and searchable; "–" if no billing address exists.
- The API functions remain unchanged (they send the complete customer object); server errors continue to be displayed generically.

## Testing Decisions

### What makes a good test

- Tests exclusively check externally observable behavior via the highest available interface: HTTP responses of the REST API, the public Zod schema, rendered components from the user's perspective, or the running application in the browser.
- No tests for implementation details (individual validator classes, internal helper functions, entity mapping). Validators are tested via the API.
- Error cases check exact `name` paths and German `reason` texts.

### Test seams (all already exist, no new ones)

1. **REST API `/api/customers`** via the existing integration test (Spring Boot with random port, Postgres via Testcontainers, rollback after each test). Covers mapping, migration and validation together.
2. **Architecture test** for consistency of entity column lengths, `@Size` and Flyway migrations.
3. **Client domain and components** via Vitest and Testing Library: public customer schema, rendered customer form, rendered customer table.
4. **Playwright E2E** against the running application.

### Server (prior art: existing `CustomerControllerTest` with JSON text blocks, Given/When/Then, AssertJ on map bodies; e.g. `multipleAdditionalEmails`, `createCustomerWithInvalidAdditionalParametersFails`)

- Create and retrieve a customer with a billing, delivery and untyped address; order and optional fields (`addressAddition`, `type`) are preserved.
- Replace, add and remove addresses via `PUT`.
- Customer overview contains addresses.
- Error cases with exact `name`/`reason`:
  - no billing address; two billing addresses; no delivery address; both list rules at once (two entries)
  - wrong zip code for `DE`, `AT`, `CH`
  - valid foreign zip code is accepted (e.g. `GB` "SW1A 1AA")
  - invalid country code
  - house number without leading digit
  - city with digit
  - fields that are too long
  - unknown address type
  - missing required fields
- Existing customer without addresses is readable and deletable.
- Deleting a customer removes its addresses (no FK error).
- All existing `POST`/`PUT` test cases are extended with valid addresses.
- `OpenApiConstraintArchitectureTest` stays green unchanged.

### Client (prior art: existing form and FormField tests with Vitest globals, `userEvent`, label-based queries)

- Schema tests (new test file for the customer schema): all field rules, country-dependent zip code, list rules, optional fields.
- Form tests: initial values on creation (two blocks), "Adresse hinzufügen" (type delivery address, country DE), "Entfernen", display of field and list errors, country selection with DACH on top, passing the addresses to `onSubmit`.
- Table tests: "Rechnungsadresse" column including formatting, "–" for a missing address, sorting and search.
- The coverage threshold of 95% is maintained.

### E2E (prior art: existing Playwright suite with its own fixture, role-/label-based locators)

- Create a customer with billing and delivery address and find it again in the list.
- Change the address of an existing customer.
- Delete the address of an existing customer.
- Existing E2E tests for creating and editing are extended with required addresses.

## Out of Scope

- Address roles that allow an address to be billing and delivery address at the same time (the same address has to be entered twice).
- Further address types (e.g. private/business).
- Placeholder or data migration for existing customers.
- Database constraints for minimum count or uniqueness of address types.
- Zip code format validation for countries outside DE/AT/CH.
- Address validation against external services (existence check, geocoding, autocomplete).
- Evaluating the server's `invalid-params` in the client and displaying them at the form field.
- Upper limit for the number of addresses.
- Dedicated endpoints or subpages for address management.
- Adding `additionalEmails`/`additionalPhoneNumbers` to the client.

## Further Notes

- Since `PUT` from the client sends the complete customer object and the client does not know `additionalEmails`/`additionalPhoneNumbers`, these continue not to be sent when saving. This existing behavior is not changed by the feature, but should be reviewed separately.
- The list rules make every existing API call without addresses invalid for `POST`/`PUT`. External API users must be informed before the rollout.
- User-visible texts are German; identifiers, test names and commit messages are English – as in the rest of the project.
