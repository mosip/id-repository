# Postman — ID Repository Local APIs

Use the included Postman collection to explore the local ID Repository APIs.

## Prerequisites

Start the dependencies and ID Repository application using the
[local setup guide](../README.md). Keep the application running while you
send requests.

## Import the collection

Import these files into Postman:

1. [ID Repository collection](Id-Repository-Local.postman_collection.json)
2. [Local environment](Id-Repository-Local.postman_environment.json)

Select the **Id-Repository-Local** environment.

## Suggested order

1. Open **00 - Auth (WireMock)** and send **Client token (idrepo)**.
   Copy the returned token into the environment variable `accessToken`.
2. Open **01 - Dependencies (WireMock)** and send **Generate UIN**.
   Copy the returned UIN into the environment variable `uin`.
3. Open **02 - Identity** and review the **Add Identity** request body.
   Replace masked biometric data and document placeholders with suitable
   values before sending it.
4. Explore the draft, VID, and credential requests as needed.

The token and UIN are not saved to the environment automatically.

## Environment variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `idrepoBaseUrl` | `http://localhost:8090` | ID Repository application |
| `wiremockBaseUrl` | `http://localhost:8082` | Local mock dependencies |
| `accessToken` | Empty | Authentication token |
| `uin`, `vid`, `registrationId`, `requestId` | Placeholders | Request identifiers |

## Before sending requests

Review each request's URL, headers, and body. Some sample bodies contain
masked data, document placeholders, or values such as `${utcTime}` and
`${uin}` that may need to be replaced for the request you are sending.
