# ID Repository API Test Rig

## Overview

The **ID Repository API Test Rig** is designed for the execution of module-wise automation API tests for the ID repository services. This test rig utilizes **Java REST Assured** and **TestNG** frameworks to automate testing of the ID repository API functionalities. The key focus is to validate the Identity creation, VID creation, Identity updation and related functionalities provided by the ID repository module.

---

## Test Categories

- **Smoke**: Contains only positive test scenarios for quick verification.
- **Regression**: Includes all test scenarios, covering both positive and negative cases.

---

## Coverage

This test rig covers only **external API endpoints** exposed by the ID repository services module.

---

## Pre-requisites

Before running the automation tests, ensure the following software is installed on the machine:

- **Java 21** ([download here](https://jdk.java.net/))
- **Maven 3.9.6** or higher ([installation guide](https://maven.apache.org/install.html))
- **Lombok** (Refer to [Lombok Project](https://projectlombok.org/))
- **setting.xml** ([download here](https://github.com/mosip/mosip-functional-tests/blob/master/settings.xml))

### For Windows

- **Git Bash 2.18.0** or higher
- Ensure the `settings.xml` file is present in the `.m2` folder.

### For Linux

- The `settings.xml` file should be present in two places:
  - In the regular Maven configuration folder (`/conf`)
  - Under `/usr/local/maven/conf/`

---

## Access Test Automation Code

You can access the test automation code using either of the following methods:

### From Browser

1. Clone or download the repository as a zip file from [GitHub](https://github.com/mosip/id-repository).
2. Unzip the contents to your local machine.
3. Open a terminal (Linux) or command prompt (Windows) and continue with the following steps.

### From Git Bash

1. Copy the Git repository URL: `https://github.com/mosip/id-repository`
2. Open **Git Bash** on your local machine.
3. Run the following command to clone the repository:
   ```sh
   git clone https://github.com/mosip/id-repository
   ```
---

## Update the property file (server / QA)

For **server or QA** runs only:

1. Open `api-test/src/main/resources/config/Idrepo.properties`
2. Update client secrets and host URLs for that environment

**Do not edit `Idrepo.properties` for local docker-compose.** Local runs use `Idrepo-local.properties` via `-Didrepo.propertiesFile=Idrepo-local.properties`. Secrets are passed as environment variables (see below).

---

## Run against localhost (docker-compose)

Use this section to run the api-test rig against the **id-repository local-dev-setup** stack on your machine (Cursor / VS Code or scripts). Server/QA `Idrepo.properties` stays unchanged.

### What talks to what

| Service | Host URL | Notes |
|---------|----------|--------|
| WireMock gateway (`env.endpoint`) | `http://localhost:8082` | All id-repo / keymanager / masterdata / idgenerator calls go here |
| ID-Repository | `http://localhost:8090` (via WireMock) | Compose service `id-repository-service` |
| Local Keycloak | `http://localhost:8081` | Admin UI `/auth` — user `admin` / password `admin` |
| PostgreSQL | `localhost:5455` | Databases `mosip_idrepo`, `mosip_idmap`, `mosip_credential`, `mosip_keymgr` |

Local properties file: `src/main/resources/config/Idrepo-local.properties`  
Certs folder: `api-test/target/local-authcerts` (created automatically)

### 1. Start the local stack

From the Maven parent (not `api-test/`):

```bash
cd id-repository
mvn -pl id-repository-service -am package -DskipTests -Dgpg.skip=true

cd local-dev-setup
./run-local-stack.sh build
./run-local-stack.sh up
./run-local-stack.sh smoke
```

Windows (PowerShell), equivalent compose path:

```powershell
cd id-repository\local-dev-setup\docker-compose
docker compose up -d
.\restart-idrepo.bat
```

Reload WireMock + local IAM before the first api-test run (and after mapping changes):

```powershell
cd id-repository\local-dev-setup\docker-compose
docker compose up -d --force-recreate --no-deps mock-service auth-token-bridge
python keycloak\bootstrap_and_mint_token.py
```

Smoke that the stack is up:

```powershell
curl.exe -s http://localhost:8090/actuator/health
curl.exe -s http://localhost:8082/v1/idgenerator/uin
```

Full stack notes: [`id-repository/local-dev-setup/LOCAL-DEV-SETUP.md`](../id-repository/local-dev-setup/LOCAL-DEV-SETUP.md).

### 2. Local JVM flags (required)

Always pass these so the runner does **not** fall back to `Idrepo.properties` / QA hosts:

```
-Didrepo.propertiesFile=Idrepo-local.properties
-Didrepo.skipPartnerSetup=true
-Dmodules=idrepo
-Denv.user=api-internal.local
-Denv.endpoint=http://localhost:8082
-Denv.keycloak=http://localhost:8081
-Denv.testLevel=smoke
```

| Flag | Local value | Why |
|------|-------------|-----|
| `idrepo.propertiesFile` | `Idrepo-local.properties` | Local URLs, ports, `authCertsPath`; leaves QA properties alone |
| `idrepo.skipPartnerSetup` | `true` | Skip partner/device cert generation. Avoids Windows `InvalidPathException` on `http://localhost:8082` folder names |
| `env.user` | `api-internal.local` | Run-context prefix for reports / Keycloak users |
| `env.endpoint` | `http://localhost:8082` | WireMock gateway (not `:8090`) |
| `env.keycloak` | `http://localhost:8081` | Host-mapped local Keycloak |
| `env.testLevel` | `smoke` or `smokeAndRegression` | Smoke = positives only. Regression cases are **skipped** (not failed) on smoke |

### 3. Local secrets (environment variables)

`Idrepo-local.properties` leaves secrets blank. `ConfigManager` reads `System.getenv` first. Match local-dev-setup / Keycloak bootstrap.

Copy `run-local.env.example` to gitignored `.env.local` and fill these values (or export them in the shell). Scripts and IDE launch fail if any required value is missing:

| Env var | Local value |
|---------|-------------|
| `postgres-password` | `mosip123` |
| `keycloak_Password` | `admin` |
| `mosip_idrepo_client_secret` | `QTGizTYN4US0XHOU` |
| `mosip_testrig_client_secret` | `local-dev-testrig-secret` |
| `mosip_admin_client_secret` | `local-dev-secret` |
| `mosip_partner_client_secret` | `local-dev-secret` |
| `mosip_pms_client_secret` | `local-dev-secret` |
| `mosip_resident_client_secret` | `local-dev-secret` |
| `mosip_reg_client_secret` | `local-dev-secret` |
| `mosip_hotlist_client_secret` | `local-dev-secret` |
| `mosip_regproc_client_secret` | `local-dev-secret` |
| `mpartner_default_mobile_secret` | `local-dev-secret` |
| `AuthClientSecret` | `local-dev-secret` |
| `mosip_crvs1_client_secret` | `local-dev-secret` |

These are **local-compose defaults only**. Do not commit real server/QA secrets.

### 4. Run from scripts (no IDE)

Does not modify `Idrepo.properties`. Console output is also teed to `api-test/logs/run-local-<testLevel>-<timestamp>.log`.

```bash
cp run-local.env.example .env.local   # then fill local-dev values from the table above
```

Windows: `copy run-local.env.example .env.local`

```bash
cd api-test
./run-local-smoke.sh          # smokeAndRegression
./run-local-smoke.sh smoke    # smoke only
```

Windows:

```bat
cd api-test
run-local-smoke.bat
run-local-smoke.bat smoke
```

### 5. Run from Cursor / VS Code

Checked-in configs:

| File | Role |
|------|------|
| [`.vscode/launch.json`](.vscode/launch.json) | Debug/Run configurations |
| [`.vscode/tasks.json`](.vscode/tasks.json) | Optional Maven tasks (compile api-test; install commons only when you change it) |

Launch configs read secrets from `.env.local` (`envFile`). Copy `run-local.env.example` first.

**Use the named launch config.** Do **not** click the green ▶ on `MosipTestRunner.java` — that skips `vmArgs` / env and will hit QA properties or crash on Windows cert paths.

`apitest-commons` is a **Maven dependency** (`1.7.0-SNAPSHOT` from `.m2`). F5 does **not** rebuild it. Install it yourself when you change commons source:

```text
Terminal → Run Task → maven: install apitest-commons
```

Optional task `maven: install apitest-commons` uses env `APITEST_COMMONS_HOME` (your local `mosip-functional-tests/apitest-commons` clone). To edit commons next to api-test: **File → Add Folder to Workspace**.

1. Open the `api-test` folder as the workspace (or select this folder in a multi-root workspace).
2. Install the **Extension Pack for Java**.
3. Start the local stack (step 1).
4. **Run and Debug** → pick one of:
   - **MosipTestRunner - IDE (local smoke)** — classpath run, `testLevel=smoke`
   - **MosipTestRunner - IDE (local full)** — classpath run, `smokeAndRegression`
   - **MosipTestRunner - jar (local smoke)** — fat jar (packages first)
   - **MosipTestRunner - jar (local full)** — fat jar, full suite
5. Wait for `maven: compile api-test` (or package, for jar configs), then `MosipTestRunner` starts in the integrated terminal.

Each launch config already sets the JVM flags from step 2 and the env secrets from step 3.

#### Maven tasks on Windows PowerShell

Do **not** pass `-Dgpg.skip=true` unquoted in a VS Code/Cursor **shell** task. PowerShell splits `-Dgpg.skip=true` into `-Dgpg` and `.skip=true`, and Maven fails with:

```text
Unknown lifecycle phase ".skip=true"
```

`.vscode/tasks.json` uses Maven’s long form instead (works in PowerShell and cmd):

```text
mvn install --define gpg.skip=true --define maven.gitcommitid.skip=true --define maven.javadoc.skip=true --define skipTests=true
```

From **cmd.exe** / Git Bash you can still use quoted `-D`:

```bat
mvn install "-Dgpg.skip=true" "-Dmaven.gitcommitid.skip=true" "-DskipTests=true"
```

### 6. Reports and expected smoke counts

- HTML reports: `api-test/testng-report/`
- Script logs: `api-test/logs/`

On `testLevel=smoke`, most YAML cases are **skipped** (regression / not smoke). A run such as `165 tests, 1 pass, 164 skips` is normal if the smoke Add Identity case passed. Use `smokeAndRegression` (IDE **local full** or `run-local-smoke` with no arg) for positives + negatives.

### 7. Local troubleshooting

| Symptom | Cause | Fix |
|---------|--------|-----|
| PreLaunch: `Unknown lifecycle phase ".skip=true"` | PowerShell ate `-D…skip=true` | Use the checked-in `tasks.json` (`--define …`) |
| `InvalidPathException: Illegal char <:>` under `Temp\AUTHCERTS\…http:\localhost:8082` | Partner cert cleanup used the URL as a Windows folder | Use the named launch config (skip partner setup). Re-install local `apitest-commons` if you still hit it. |
| Runner talks to `dev-int` / `ad1_idrepo` | Green ▶ on `MosipTestRunner` (no local `vmArgs`) | Run **MosipTestRunner - IDE (local smoke)** from Run and Debug |
| `IDR-IDC-002` Invalid UIN | Random / non-Verhoeff UIN | UIN must come from `GET http://localhost:8082/v1/idgenerator/uin` |
| Auth / token failures | Stale WireMock or missing testrig client | Recreate `mock-service` + `auth-token-bridge`; run `bootstrap_and_mint_token.py` |
| Empty tables in pgAdmin | Wrong DB/schema | Open database `mosip_idrepo` → schema `idrepo` (not `postgres` / `public`) |
| `mosip_ida` / `mosip_master` connection errors | Those DBs are not in local compose | Ignored by `DBManager`; not a blocker |
| Slack / `/home/mosip/testrig/report` copy errors | Cluster-only report mount | Harmless on a laptop |

---

## Build Test Automation Code

Once the repository is cloned or downloaded, follow these steps to build and install the test automation code:

1. Navigate to the project directory:
   ```sh
   cd api-test
   ```

2. Build the project using Maven:
   ```sh
   mvn clean install -Dgpg.skip=true -Dmaven.gitcommitid.skip=true
   ```

This will download the required dependencies and prepare the test suite for execution.

---

## Execute Test Automation Suite

**Local docker-compose (localhost):** see [Run against localhost (docker-compose)](#run-against-localhost-docker-compose) — use `run-local-smoke.*` or the Cursor/VS Code launch configs. Do not use the commands below against `:8082` without `Idrepo-local.properties`.

You can execute the test automation code against a **server / QA** environment using either of the following methods:

### Using Jar

To execute the tests using Jar, use the following steps:

1. Navigate to the `target` directory where the JAR file is generated:
   ```sh
   cd target/
   ```

2. Run the automation test suite JAR file:
   ```
   java -jar -Dmodules=idrepo -Denv.user=api-internal.<env_name> -Denv.endpoint=<base_env> -Denv.testLevel=smokeAndRegression -jar apitest-idrepo-1.2.1-jar-with-dependencies.jar
   ```
   
# Using Eclipse IDE

To execute the tests using Eclipse IDE, use the following steps:

## 1. **Install Eclipse (Latest Version)**
   - Download and install the latest version of Eclipse IDE from the [Eclipse Downloads](https://www.eclipse.org/downloads/).

## 2. **Import the Maven Project**

   After Eclipse is installed, follow these steps to import the Maven project:

   - Open Eclipse IDE.
   - Go to `File` > `Import`.
   - In the **Import** wizard, select `Maven` > `Existing Maven Projects`, then click **Next**.
   - Browse to the location where the `api-test` folder is saved (either from the cloned Git repository or downloaded zip).
   - Select the folder, and Eclipse will automatically detect the Maven project. Click **Finish** to import the project.

## 3. **Build the Project**

   - Right-click on the project in the **Project Explorer** and select `Maven` > `Update Project`.
   - This will download the required dependencies as defined in the `pom.xml` and ensure everything is correctly set up.

## 4. **Run the Tests**

   To execute the test automation suite, you need to configure the run parameters in Eclipse:

   - Go to `Run` > `Run Configurations`.
   - In the **Run Configurations** window, create a new configuration for your tests:
     - Right-click on **Java Application** and select **New**.
     - In the **Main** tab, select the project by browsing the location where the `api-test` folder is saved, and select the **Main class** as `io.mosip.testrig.apirig.idrepo.testrunner.MosipTestRunner`.
   - In the **Arguments** tab, add the necessary **VM arguments**.

     Server / QA:
		```
		-Dmodules=idrepo -Denv.user=api-internal.<env_name> -Denv.endpoint=<base_env> -Denv.testLevel=smokeAndRegression
		```

     Localhost (also set the env vars from [Local secrets](#3-local-secrets-environment-variables)):
		```
		-Didrepo.propertiesFile=Idrepo-local.properties -Didrepo.skipPartnerSetup=true -Dmodules=idrepo -Denv.user=api-internal.local -Denv.endpoint=http://localhost:8082 -Denv.keycloak=http://localhost:8081 -Denv.testLevel=smoke
		```

     Prefer Cursor/VS Code [named launch configs](#5-run-from-cursor--vs-code) over a one-off Eclipse run on Windows.

## 5. **Run the Configuration**

   - Once the configuration is set up, click **Run** to execute the test suite.
   - The tests will run, and the results will be shown in the **Console** tab of Eclipse.

   **Note**: You can also run in **Debug Mode** to troubleshoot issues by setting breakpoints in your code and choosing `Debug` instead of `Run`.

---

## 6. **View Test Results**

   - After the tests are executed, you can view the detailed results in the `api-test\testng-report` directory.
   - The report will have two sections:
       - One section for pre-requisite APIs test cases.
       - Another section for core test cases.

---

## Test Report Column Definitions
This section describes the meaning of each column in the test report:
- **Total (T)**
  The total number of test cases considered in the report.
- **Passed (P)**
  Indicates the number of test cases that executed successfully with the expected results.
- **Failed (F)**
  Indicates the number of test cases that failed due to issues such as output validation mismatches or unexpected errors during execution.
- **Skipped (S)**
  Represents test cases that were not executed due to missing prerequisites or data dependencies.
- **Ignored (I)**
  Represents test cases that were intentionally not executed due to limitations such as unsupported features, incompatibilities, or undeployed services.
- **Known Issues (KI)**
  Indicates test cases that failed but are already acknowledged as known issues for the current release, typically linked with a bug or defect ID.

## Details of Arguments Used

- **env.user**: Replace `<env_name>` with the appropriate environment name (e.g., `dev`, `qa`). For local compose use `api-internal.local`.
- **env.endpoint**: Base URL of the application under test (e.g. `https://api-internal.<env_name>.mosip.net`). For local compose use `http://localhost:8082` (WireMock gateway).
- **env.keycloak**: Keycloak base URL. For local compose use `http://localhost:8081`.
- **env.testLevel**: `smoke` (positives only) or `smokeAndRegression` (positives + negatives).
- **idrepo.propertiesFile**: Optional. Set to `Idrepo-local.properties` for localhost; omit for server/QA (`Idrepo.properties`).
- **idrepo.skipPartnerSetup**: `true` for local compose on Windows (skip partner/device cert folders).
- **jar**: Name of the fat JAR (version follows the POM), e.g. `apitest-idrepo-1.4.0-SNAPSHOT-jar-with-dependencies.jar`.

### Build and Run Info

To run the tests for both **Smoke** and **Regression**:

1. Ensure the correct environment and test level parameters are set.
2. Execute the tests as shown in the command above to validate ID repository API functionalities.

---

## License

This project is licensed under the terms of the [Mozilla Public License 2.0](https://github.com/mosip/mosip-platform/blob/master/LICENSE)