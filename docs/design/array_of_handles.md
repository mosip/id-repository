## What is Array of Handles?

Array of Handles is an enhancement to the existing [Handle support](./handle_support.md) in MOSIP that allows a single identity field to hold **multiple values**, each of which can independently be designated as a handle, a notification target, both, or neither.

Previously, handle fields were restricted to a single string value (e.g., one phone number, one email). With this enhancement, handle-eligible fields can be declared as arrays in the ID schema, enabling residents to register multiple values for a single field (e.g., multiple email addresses) and selectively choose which ones act as handles for authentication.

## Principles

* A field marked `handle: true` in the ID schema may be either a `string` (single value) or an `array` (multiple values).
* Each element in an array-type handle field carries a `value` and an optional `tags` list.
* Supported tag values are `"handle"` and `"notification"`.
* **Default handle behaviour** — if **no element** in the array has a `"handle"` tag (i.e., `tags` is absent or empty on every element), then **all values** in the array are treated as handles.
* **Selective handle behaviour** — if **at least one element** explicitly carries the `"handle"` tag, the system switches to selective mode: **only those elements that have the `"handle"` tag** are registered as handles. Elements that have no `"handle"` tag are stored in the identity but are **not** registered as handles in this case — this exclusion applies only because another element in the same array opted in via the tag.
* The `"notification"` tag is independent of the `"handle"` tag and governs which values are used for OTP/notification delivery.
* `selectedHandles` continues to hold the list of fieldIds (e.g., `["email", "phone"]`); within an array field, tag-based selection determines which individual values within that field become handles.
* All handle uniqueness and postfix rules described in [handle_support.md](./handle_support.md) continue to apply for every value that is resolved as a handle.

## Schema Definition

Array-type handle fields define their structure **inline** rather than referencing a shared `$ref` definition. This is intentional — if the array structure were pulled from a shared definition via `$ref`, field-specific regex validators on the `value` property (e.g., email format validation) would not be applicable. Defining it inline allows each field to carry its own validators directly on `value`.

> Note: A `simpleListType` definition exists in the shared schema but is not referenced by any handle field for this reason.

A field declared as an array-type handle looks like:

```json
"email": {
  "bioAttributes": [],
  "fieldCategory": "pvt",
  "format": "none",
  "fieldType": "default",
  "handle": true,
  "type": "array",
  "uniqueItems": true,
  "additionalItems": false,
  "minItems": 1,
  "items": {
    "additionalProperties": false,
    "type": "object",
    "required": ["value"],
    "properties": {
      "value": {
        "type": "string",
        "validators": [{
          "validator": "^[A-Za-z0-9_\\-]+(\\.[A-Za-z0-9_]+)*@[A-Za-z0-9_-]+(\\.[A-Za-z0-9_]+)*(\\.[a-zA-Z]{2,})$",
          "arguments": [],
          "type": "regex"
        }]
      },
      "tags": {
        "uniqueItems": true,
        "type": "array",
        "items": {
          "type": "string",
          "enum": ["handle", "notification"]
        }
      }
    }
  }
}
```

## Tag Resolution Logic

Given an array field `email` with `handle: true` and the field is listed in `selectedHandles`:

| Scenario | Array values | Resolved handles |
|----------|-------------|-----------------|
| No tags on any element | `[{value: "a@x.com"}, {value: "b@x.com"}]` | Both `a@x.com` and `b@x.com` |
| Only some elements tagged `"handle"` | `[{value: "a@x.com", tags: ["handle"]}, {value: "b@x.com"}]` | Only `a@x.com` |
| Multiple elements tagged `"handle"` | `[{value: "a@x.com", tags: ["handle"]}, {value: "b@x.com", tags: ["handle"]}, {value: "c@x.com"}]` | `a@x.com` and `b@x.com` |
| Element tagged `"notification"` only | `[{value: "a@x.com", tags: ["notification"]}, {value: "b@x.com"}]` | No element has `"handle"` tag → both are handles; `a@x.com` also receives notifications |
| Element tagged both `"handle"` and `"notification"` | `[{value: "a@x.com", tags: ["handle", "notification"]}, {value: "b@x.com"}]` | Only `a@x.com` (handle + notification); `b@x.com` is neither |

## Changes in `add_identity` API

The following steps extend the existing `add_identity` handle flow for array-type handle fields:

1. Identify the fields listed in `selectedHandles`.
2. For each selected field, check if its schema type is `string` or `array`.
3. **If `string`** — existing single-value handle flow applies (no change).
4. **If `array`** — apply tag resolution logic to identify which values in the array are handles.
   * Inspect each element for the `"handle"` tag.
   * If no element carries a `"handle"` tag, treat all elements as handles.
   * If at least one element carries a `"handle"` tag, treat only those elements as handles.
5. For each resolved handle value, append the configured postfix (or `@<fieldId>` by default) and check for uniqueness in the `mosip_idrepo.handle` table.
6. Fail the request if a collision is detected for any resolved handle value.
7. On success, create one entry in `mosip_idrepo.handle` per resolved handle value (with status `ACTIVATED`).
8. Issue a credential for each resolved handle value.

## Changes in `update_identity` API

The tag resolution step replaces the simple field-level value extraction for array-type handle fields.

* When `selectedHandles` includes an array-type handle field:
  1. Re-run tag resolution on the **incoming** array values to find the new set of handles.
  2. Compare against the previously registered handles for that field.
  3. Handles no longer resolved (removed or untagged) are marked `DELETE` in the `mosip_idrepo.handle` table.
  4. Newly resolved handles are checked for uniqueness and inserted as `ACTIVATED`.
  5. Unchanged handles (same value, still resolved) are left untouched.

* `selectedHandles` absent or `null` in the request — the saved `selectedHandles` list is used; re-run tag resolution against the incoming values for any array-type handle field that is being updated.

* `selectedHandles` set to empty list `[]` — all previously registered handles for all handle fields are marked `DELETE`.

## Sample `add_identity` Request

```json
{
  "id": "mosip.id.create",
  "version": "v1",
  "requesttime": "2025-07-01T10:00:00.000Z",
  "request": {
    "registrationId": "278474680010922202404180640",
    "identity": {
      "IDSchemaVersion": 0.1,
      "fullName": [{ "language": "eng", "value": "Jane Doe" }],
      "dateOfBirth": "1990/01/15",
      "gender": [{ "language": "eng", "value": "Female" }],
      "phone": "+919876543210",
      "email": [
        { "value": "jane.work@example.com", "tags": ["handle", "notification"] },
        { "value": "jane.personal@example.com", "tags": ["handle"] },
        { "value": "jane.old@example.com" }
      ],
      "selectedHandles": ["email", "phone"],
      "individualBiometrics": {
        "format": "cbeff",
        "version": 1.0,
        "value": "<biometric-data>"
      }
    }
  }
}
```

In the above request:
* `phone` — a `string`-type handle; `+919876543210@phone` is registered as a handle.
* `email` — an `array`-type handle; since two elements carry `"handle"` tag, only `jane.work@example.com@email` and `jane.personal@example.com@email` are registered as handles. `jane.old@example.com` is stored in the identity but is **not** a handle.
* `jane.work@example.com` additionally receives notifications (`"notification"` tag).

## How to Set Up Array of Handles in the ID Schema

1. Define `simpleListType` (or an inline equivalent) in the schema `definitions`.
2. Set the field `type` to `array` and add `"handle": true`.
3. Include `value` (required) and `tags` (optional, enum `["handle","notification"]`) in the item properties.
4. Publish the schema and configure the postfix mapping:
   ```
   mosip.identity.fieldid.handle-postfix.mapping={"email": "@email", "phone": "@phone"}
   ```
5. Configure the IDA regex for each handle postfix in `id-authentication-default.properties`:
   ```
   mosip.ida.handle-types.regex={ '@email': '^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[a-zA-Z]{2,}@email$', '@phone': '^[+][0-9]{10,12}@phone$' }
   ```

## Backward Compatibility

* Fields declared as `"type": "string"` with `"handle": true` continue to work without any change.
* If an array-type handle field is present in `selectedHandles` but all elements lack a `tags` property, all values are treated as handles — this means existing data that omits tags is handled gracefully without requiring migration.
* Fields in `selectedHandles` that are not present in the schema as handle-eligible are silently ignored (same as before).

## Questions

1. **Maximum handles per field** — should there be a configurable limit on how many values in an array can be simultaneously registered as handles for a single field?
   Recommendation: Introduce a configurable property `mosip.identity.fieldid.handle-max-count.mapping` (e.g., `{"email": 3}`) so that countries can enforce a cap. Default: unlimited.

2. **Partial update of array values** — when an update request includes the email array with fewer elements than saved, are the missing elements removed or preserved?
   Decision: The array in the update request fully replaces the saved array for that field. Handle registrations are recalculated from the new array. This is consistent with how `selectedHandles` replacement works.

3. **Notification channel resolution** — should `"notification"` tag selection follow the same default-all logic as `"handle"` tag?
   Decision: No. The `"notification"` tag is opt-in only. If no element has `"notification"` tag, no notification-specific channel is designated from this field. The existing notification channel configuration in the system continues to govern OTP delivery fallback.