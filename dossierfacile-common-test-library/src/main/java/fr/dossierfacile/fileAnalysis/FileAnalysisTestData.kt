package fr.dossierfacile.fileAnalysis

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import fr.dossierfacile.common.enums.DocumentCategory
import fr.dossierfacile.common.enums.DocumentSubCategory


data class JsonFileAnalysisTestData @JsonCreator constructor(
    @JsonProperty("testName") val testName: String,
    @JsonProperty("documentCategory") val documentCategory: DocumentCategory,
    @JsonProperty("documentSubCategory") val documentSubCategory: List<DocumentSubCategory>,
    @JsonProperty("validDocuments") val validDocuments: List<JsonValidDocumentData>,
    @JsonProperty("invalidDocuments") val invalidDocuments: List<JsonInvalidDocumentData>
)

data class JsonValidDocumentData @JsonCreator constructor(
    @JsonProperty("bucketPath") val bucketPath: String,
    @JsonProperty("fileDescription") val fileDescription: JsonNode?,
    @JsonProperty("expectedResult") val expectedResult: JsonNode?
)

data class JsonInvalidDocumentData @JsonCreator constructor(
    @JsonProperty("bucketPath") val bucketPath: String,
    @JsonProperty("fileDescription") val fileDescription: JsonNode?,
    @JsonProperty("expectedError") val expectedError: JsonNode?
)

data class FileAnalysisTestData<V, I, F>(
    val testName: String,
    val documentCategory: DocumentCategory,
    val documentSubCategory: List<DocumentSubCategory>,
    val validDocuments: List<ValidDocumentData<V, F>>,
    val invalidDocuments: List<InvalidDocumentData<I, F>>
)

abstract class DocumentData<F>(open val bucketPath: String, open val fileDescription: F)

data class ValidDocumentData<T, F>(
    override val bucketPath: String,
    override val fileDescription: F,
    val expectedResult: T
) : DocumentData<F>(bucketPath, fileDescription)

data class InvalidDocumentData<T, F>(
    override val bucketPath: String,
    override val fileDescription: F,
    val expectedError: T
) : DocumentData<F>(bucketPath, fileDescription)