package fr.dossierfacile.fileAnalysis

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request
import aws.smithy.kotlin.runtime.content.writeToFile
import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.CompletableFuture


data class S3Configuration(
    val endpoint: String,
    val accountId: String,
    val accessKey: String,
    val secretKey: String,
    val region: String,
    val bucketName: String
)

class TestOvhFileStorageServiceImpl(
    private val config: S3Configuration
) {

    private var s3client: S3Client = S3Client {
        region = config.region
        endpointUrl = Url.parse(config.endpoint)
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = config.accessKey
            secretAccessKey = config.secretKey
            accountId = config.accountId
            httpClient = CrtHttpEngine()
        }
    }

    private suspend fun listFiles() {
        s3client.listObjectsV2(ListObjectsV2Request {
            bucket = config.bucketName
        }).contents?.forEach {
            println("File: ${it.key}")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun listFileAsync(): CompletableFuture<Unit> =
        GlobalScope.future {
            listFiles()
        }

    private suspend fun download(path: String): File {
        return s3client.getObject(
            GetObjectRequest {
                bucket = config.bucketName
                key = path
            }
        ) {
            val file = File.createTempFile("temp", getExtensionFromPath(path))
            it.body?.writeToFile(file) ?: throw FileNotFoundException("File not found")
            return@getObject file
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun downloadAsync(path: String): CompletableFuture<File> =
        GlobalScope.future {
            download(path)
        }

    private fun getExtensionFromPath(path: String): String {
        return ".${path.substringAfterLast('.', "")}"
    }
}