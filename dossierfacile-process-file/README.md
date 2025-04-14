# Asynchronous File and Document Processing Service (dossierfacile-process-file)

## Description
Service responsible for the asynchronous processing of documents and files.

## Main Features
- File miniaturization
- Data extraction (OCR, 2D Doc reading, ...)
- Document analysis

## Configuration
Create a file `application-dev.properties` in `dossierfacile-process-file/src/main/resources`

```properties
# Storage path for the MockStorage
mock.storage.path=../mock-storage
# Storage provider list
storage.provider.list=LOCAL
# Port of this service
server.port=8088

#URL of the database
spring.datasource.url=
#Username of the database
spring.datasource.username=
#Password of the database
spring.datasource.password=

# Time before the document will be picked base on the createAt timestamp.
document.analysis.delay.ms=10000
# Timeout for the document analysis.
document.analysis.timeout.ms=90000

#Url to payfit api
payfit.api.url=

#Url to verification document api of france identite (default :https://dossierfacile-france-identite-numerique-api.osc-secnum-fr1.scalingo.io/api/validation/v1/check-doc-valid?all-attributes=true)
france.identite.api.url=

# Brevo
brevo.apikey=
```

# LogStash :

For the dev environment the appender Logstash is disabled by default.

# Database :
⚠️ The database is managed by this project. When you start it, liquibase will update the scheme according to the code.

# Run the application

```shell
mvn spring-boot:run -D mvn spring-boot:run -D spring-boot.run.profiles=dev,mockOvh
```

## Running the File Processing Tests

These tests verify the correctness of the file processing functionalities. They are not executed by default.

The test data required for execution contains sensitive information and is securely stored in a private Object Storage provided by OVH.

In the Object Storage, each test has a dedicated folder structured as follows:

```
[testName]/valide/[fileName]
[testName]/invalide/[fileName]
```

Additionally, each test folder includes a `dataset.json` file containing data used for parameterized tests:

```json
{
  "testName": "guarantee_provider_file_analysis",
  "documentCategory": "GUARANTEE_PROVIDER_CERTIFICATE",
  "documentSubCategory": [
    "OTHER_GUARANTEE",
    "VISALE"
  ],
  "validDocuments": [
    {
      "bucketPath": "guarantee_provider_certificate/visale/valide/001.pdf",
      "fileDescription": {
        "tenantNames": [
          {
            "firstName": "",
            "lastName": ""
          },
          {
            "firstName": "",
            "lastName": ""
          }
        ]
      },
      "expectedResult": null
    }
  ],
  "invalidDocuments": [
    {
      "bucketPath": "guarantee_provider_certificate/visale/invalide/002.pdf",
      "fileDescription": null,
      "expectedError": {
        "errorType": "NotParsableDocument"
      }
    },
    {
      "bucketPath": "guarantee_provider_certificate/visale/invalide/008.pdf",
      "fileDescription": null,
      "expectedError": {
        "errorType": "NotParsableDocument"
      }
    },
    {
      "bucketPath": "guarantee_provider_certificate/visale/invalide/011.pdf",
      "fileDescription": {
        "tenantNames": [
          {
            "firstName": "",
            "lastName": ""
          }
        ]
      },
      "expectedError": {
        "errorType": "WrongNamesDocument"
      }
    },
    {
      "bucketPath": "guarantee_provider_certificate/visale/invalide/012.pdf",
      "fileDescription": {
        "tenantNames": [
          {
            "firstName": "",
            "lastName": ""
          }
        ]
      },
      "expectedError": {
        "errorType": "ExpiredDocument"
      }
    }
  ]
}
```

### Setup

You need to install the following libraries on your system to run the tests:

- `tesseract`
- `tesseract-lang`

On macOS, you can use Homebrew to install these dependencies:

```shell
brew install tesseract
brew install tesseract-lang
```

Then, configure the environment variables:

- Set `TESSDATA_PREFIX` to point to the directory containing the Tesseract language files.
- Use the `-Djna.library.path` argument to indicate the path to the Tesseract library.

Example command to run the tests:

```shell
ENABLE_TESTS_FILE_ANALYSIS=true \
TESSDATA_PREFIX=/opt/homebrew/share/tessdata/ \
mvn test -Dtest=GuaranteeProviderDocumentTest -Djna.library.path=/opt/homebrew/lib
```

Finally, provide the S3 configuration required by the tests by creating a file named `document_analysis.properties` under the `src/test/resources` directory, with the following content:

```properties
s3.endpoint=https://s3.sbg.io.cloud.ovh.net/
s3.account.id=[[ovh_account_id]]
s3.access.key=[[ovh_access_key]]
s3.secret.key=[[ovh_secret_key]]
s3.region=sbg
s3.bucket.name=documents-analysis-test
```