package fr.dossierfacile.fileAnalysis

import fr.dossierfacile.common.enums.DocumentCategory
import fr.dossierfacile.common.enums.DocumentSubCategory

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