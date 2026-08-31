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

## Update the property file
1. Navigate to the Idrepo.properties file located at:
    id-repository\api-test\src\main\resources\config\Idrepo.properties
2. Open the file in your preferred editor
3. Update the client secret values and other required credentials as per your environment

Do **not** commit local secrets or localhost URLs in `Idrepo.properties`.

---

## Run against localhost

Use this when ID Repository is running on your machine (service on port `8090`, typically with a local gateway on `8082`).

### 1. Point `Idrepo.properties` at localhost

Edit `api-test/src/main/resources/config/Idrepo.properties` locally (do not push these values):

```properties
keycloak-external-url = http://localhost:8082
audit_url = jdbc:postgresql://localhost:5455/mosip_idrepo
partner_url = jdbc:postgresql://localhost:5455/mosip_idrepo
db-server = localhost
db-port = 5455
postgres-password = <local postgres password>
authCertsPath = target/local-authcerts
mosip_components_base_urls = idrepository=localhost:8082;authmanager=localhost:8082;masterdata=localhost:8082;idgenerator=localhost:8082;keymanager=localhost:8082;auditmanager=localhost:8082;datashare=localhost:8082;partnermanager=localhost:8082;policymanager=localhost:8082;credentialservice=localhost:8082;credentialrequest=localhost:8082
```

Fill client secrets and `keycloak_Password` for your local IAM. `ConfigManager` also reads matching environment variables, so you can leave secrets blank in the file and export them in the shell instead.

When `env.endpoint` is `localhost` or `127.0.0.1`, the runner skips Keycloak admin user setup and partner/device cert generation, and it sanitizes Windows cert folder names that contain `:`.

### 2. Build and run

```sh
cd api-test
mvn clean install -Dgpg.skip=true -Dmaven.gitcommitid.skip=true
```

```sh
java -Dmodules=idrepo \
  -Denv.user=api-internal.local \
  -Denv.endpoint=http://localhost:8082 \
  -Denv.testLevel=smoke \
  -jar target/apitest-idrepo-*-jar-with-dependencies.jar
```

Eclipse / VS Code VM arguments:

```
-Dmodules=idrepo -Denv.user=api-internal.local -Denv.endpoint=http://localhost:8082 -Denv.testLevel=smoke
```

Use `env.testLevel=smokeAndRegression` for the full suite. `env.endpoint` should be the local gateway (`http://localhost:8082`), not the ID Repository process on `:8090`.

---

## Run against qa11new (or other remote env)

1. Put Keycloak / DB URLs and secrets in `config/Idrepo.properties` (do **not** commit real secrets).
2. Ensure `apitest-commons` includes the Face Subtype / CBEFF sanitize fix (see Biometric BioValue section below).
3. Run with a non-localhost endpoint, for example:

```sh
java -Dmodules=idrepo \
  -Denv.user=api-internal.qa11new \
  -Denv.endpoint=https://api-internal.qa11new.mosip.net \
  -Denv.testLevel=smoke \
  -jar target/apitest-idrepo-*-jar-with-dependencies.jar
```

---

## Biometric BioValue (local vs env)

AddIdentity needs a CBEFF payload in `documents[0].value`. The runner chooses the source from `env.endpoint`:

| Target | Bio source | What you need |
|--------|------------|---------------|
| **Localhost** | Bundled `config/bioValue.properties` | File present under `api-test/src/main/resources/config/` (no Mock SBI required) |
| **Remote env** | Mock SBI / MDS generation | Complete `api-test/src/main/resources/mds/` (devices + `resource/Profile/.../Face.iso`) |

### Local

- Uses `bioValue.properties` only (partner/device setup is skipped on localhost).
- Confirm in the log: `Local mode: loading bundled bioValue.properties`.

### Remote / QA

- Always generates BioValue via Mock SBI; it does **not** fall back to `bioValue.properties`.
- Runner sets `user.dir` to `src/main/resources/mds` while generating so Mock SBI finds `application.properties`, `Biometric Devices/`, and `resource/Profile/`.
- Confirm in the log: `Env mode: generating BioValue via Mock SBI` and a large `BioValue ready for AddIdentity (len=...)`.
- `apitest-commons` must keep empty Face `<Subtype></Subtype>` and sanitize MDS CBEFF for idrepo XSD (strip JWT `<SB>` / `<others>` that known-good BioValue does not have). Install the fixed commons SNAPSHOT before the env run if your IDE still has an older jar.

If env fails with `IDR-IDC-002` / `Invalid Input Parameter - documents/0/value`, the CBEFF failed `validateXML` — check mds Face ISO, Face Subtype, and commons sanitize — not the local property file.

### Optional: scope test cases

In `Idrepo.properties` (or `Idrepo-local.properties` when using that flow):

```properties
# Empty = full suite. Comma-separated uniqueIdentifier values to run a subset.
testCasesToExecute=
```

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

You can execute the test automation code using either of the following methods:

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
   - In the **Arguments** tab, add the necessary **VM arguments**:
     - **VM Arguments**:
		```
		-Dmodules=idrepo -Denv.user=api-internal.<env_name> -Denv.endpoint=<base_env> -Denv.testLevel=smokeAndRegression
		```

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

- **env.user**: Replace `<env_name>` with the appropriate environment name (e.g., `dev`, `qa`, etc.).
- **env.endpoint**: The environment where the application under test is deployed. Replace `<base_env>` with the correct base URL for the environment (e.g., `https://api-internal.<env_name>.mosip.net`).
- **env.testLevel**: Set this to `smoke` to run only smoke test cases, or `smokeAndRegression` to run both smoke and regression tests.
- **jar**: Specify the name of the JAR file to execute. The version will change according to the development code version. For example, the current version may look like `apitest-idrepo-1.2.1-jar-with-dependencies.jar`.

### Build and Run Info

To run the tests for both **Smoke** and **Regression**:

1. Ensure the correct environment and test level parameters are set.
2. Execute the tests as shown in the command above to validate ID repository API functionalities.

---

## License

This project is licensed under the terms of the [Mozilla Public License 2.0](https://github.com/mosip/mosip-platform/blob/master/LICENSE)