package fr.dossierfacile.common.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.common.entity.EncryptionKey;
import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.exceptions.OvhConnectionFailedException;
import fr.dossierfacile.common.exceptions.RetryableOperationException;
import fr.dossierfacile.common.exceptions.UnsupportedKeyException;
import fr.dossierfacile.common.model.S3Bucket;
import fr.dossierfacile.common.service.interfaces.FileStorageProviderService;
import fr.dossierfacile.common.service.model.BulkDeleteResult;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.api.exceptions.ConnectionException;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.Payload;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.model.storage.object.options.ObjectListOptions;
import org.openstack4j.openstack.OSFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.GCMParameterSpec;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service("ovhFileStorageProvider")
@Slf4j
@Profile("!mockOvh")
@Deprecated
public class OvhFileStorageServiceImpl implements FileStorageProviderService {
    @Value("${ovh.project.domain:default}")
    private String ovhProjectDomain;
    @Value("${ovh.auth.url:default}")
    private String ovhAuthUrl;
    @Value("${ovh.username:default}")
    private String ovhUsername;
    @Value("${ovh.password:default}")
    private String ovhPassword;
    @Value("${ovh.project.name:default}")
    private String ovhProjectName;
    @Value("${ovh.region:default}")
    private String ovhRegion;
    @Value("${ovh.container:default}")
    private String ovhContainerName;
    @Value("${ovh.connection.reattempts:3}")
    private Integer ovhConnectionReattempts;
    private final ThreadLocal<OSClient.OSClientV3> osClientThreadLocal = new ThreadLocal<>();

    private synchronized OSClient.OSClientV3 authenticate() {
        osClientThreadLocal.set(null);
        for (int i = 0; i <= ovhConnectionReattempts; i++) {
            try {
                osClientThreadLocal.set(OSFactory.builderV3()
                        .endpoint(ovhAuthUrl)
                        .credentials(ovhUsername, ovhPassword, Identifier.byId(ovhProjectDomain))
                        .scopeToProject(Identifier.byName(ovhProjectName), Identifier.byName(ovhProjectDomain))
                        .authenticate());
                osClientThreadLocal.get().useRegion(ovhRegion);

            } catch (AuthenticationException | ClientResponseException e) {
                log.error("ObjectStorage authentication failed.", e);
                break;
            } catch (ConnectionException e) {
                log.error("ObjectStorage failed. (" + i + "/" + ovhConnectionReattempts + ")", e);
            }
        }
        if (osClientThreadLocal.get() == null) {
            throw new OvhConnectionFailedException("ObjectStorage Max attempts reached ");
        }
        return osClientThreadLocal.get();
    }

    private OSClient.OSClientV3 getClient() {
        if (osClientThreadLocal.get() != null) return osClientThreadLocal.get();
        return authenticate();
    }

    /**
     * Extract the Object Storage endpoint URL from the authenticated client.
     * Retrieves the Swift/Object Storage endpoint from the service catalog.
     * 
     * NOTE: client.getEndpoint() returns the Keystone identity endpoint, not the Object Storage endpoint.
     * Since OpenStack4j doesn't provide a direct API to get service-specific endpoints, we construct
     * the endpoint URL based on OVH's standard pattern: https://storage.<region>.cloud.ovh.net/v1/AUTH_<project_id>
     */
    private String getObjectStorageEndpoint(OSClient.OSClientV3 client) {
        // Construct the endpoint URL manually based on OVH's standard pattern
        // OVH Object Storage endpoints follow: https://storage.<region>.cloud.ovh.net/v1/AUTH_<project_id>
        // We have the region from ovhRegion, and we can get the project ID from the token
        
        var token = client.getToken();
        if (token == null) {
            throw new OvhConnectionFailedException("Token is null");
        }
        
        var project = token.getProject();
        if (project == null) {
            throw new OvhConnectionFailedException("Project is null in token");
        }
        
        String projectId = project.getId();
        if (projectId == null || projectId.isEmpty()) {
            throw new OvhConnectionFailedException("Project ID is null or empty");
        }
        
        // Construct OVH Object Storage endpoint
        // Format: https://storage.<region>.cloud.ovh.net/v1/AUTH_<project_id>
        String endpoint = String.format("https://storage.%s.cloud.ovh.net/v1/AUTH_%s", 
                                        ovhRegion, projectId);
        
        log.debug("Constructed Object Storage endpoint: {}", endpoint);
        return endpoint;
    }

    // TODO will be put in common abstract class when key version 1 will be deprecated
    private static InputStream cipherInputStream(String path, EncryptionKey key, InputStream in) throws IOException {
        try {
            Cipher aes;
            byte[] iv = (key.getVersion() == 1) ? DigestUtils.md5(path) : DigestUtils.sha256(path);
            if (key.getVersion() == 1 || key.getVersion() == 2) {
                GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, iv);
                aes = Cipher.getInstance("AES/GCM/NoPadding");
                aes.init(Cipher.DECRYPT_MODE, key, gcmParamSpec);
            } else {
                throw new UnsupportedKeyException("Unsupported Key version " + key.getVersion());
            }
            in = new CipherInputStream(in, aes);
        } catch (Exception e) {
            throw new IOException(e);
        }
        return in;
    }

    @Override
    public ObjectStorageProvider getProvider() {
        return ObjectStorageProvider.OVH;
    }

    @Override
    public void delete(String path) {
        try {
            getClient().objectStorage().objects().delete(ovhContainerName, path);
        } catch (AuthenticationException e) {
            log.error("ObjectStorage authentication failed.", e);
            authenticate().objectStorage().objects().delete(ovhContainerName, path);
        }
    }

    /**
     * Bulk delete multiple objects using OpenStack Swift bulk-delete API.
     * This is much more efficient than deleting objects one by one.
     * Swift supports up to 10,000 objects per bulk delete request.
     *
     * @param paths List of object paths to delete
     * @return BulkDeleteResult with successful and failed deletions
     */
    @Override
    public BulkDeleteResult bulkDelete(List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return BulkDeleteResult.empty();
        }

        // Swift bulk delete limit is 10,000 objects per request
        if (paths.size() > 10000) {
            log.warn("Bulk delete request exceeds Swift limit of 10,000 objects. Processing first 10,000 only.");
            paths = paths.subList(0, 10000);
        }

        try {
            // Ensure we have a valid client/token
            OSClient.OSClientV3 client = getClient();
            String token = client.getToken().getId();
            String storageUrl = getObjectStorageEndpoint(client);

            return executeBulkDelete(paths, token, storageUrl);
        } catch (AuthenticationException e) {
            log.warn("Authentication failed during bulk delete, retrying with fresh token");
            try {
                OSClient.OSClientV3 client = authenticate();
                String token = client.getToken().getId();
                String storageUrl = getObjectStorageEndpoint(client);
                return executeBulkDelete(paths, token, storageUrl);
            } catch (Exception retryEx) {
                log.error("Bulk delete failed after retry: {}", retryEx.getMessage(), retryEx);
                return BulkDeleteResult.allFailed(paths, "Authentication failed: " + retryEx.getMessage());
            }
        } catch (Exception e) {
            log.error("Bulk delete failed: {}", e.getMessage(), e);
            return BulkDeleteResult.allFailed(paths, e.getMessage());
        }
    }

    /**
     * Execute bulk delete via direct HTTP call to Swift API.
     *
     * NOTE: OpenStack4j does not support Swift bulk-delete operation
     * (see https://github.com/ContainX/openstack4j/issues/1010).
     * We must use direct HTTP calls to the Swift bulk-delete endpoint.
     *
     * @see <a href="https://docs.openstack.org/swift/latest/api/bulk-delete.html">Swift Bulk Delete API</a>
     */
    private BulkDeleteResult executeBulkDelete(List<String> paths, String token, String storageUrl) {
        try {
            // Build request body: container/object per line
            // URL encode paths but preserve forward slashes in the container/object separator
            StringBuilder bodyBuilder = new StringBuilder();
            for (String path : paths) {
                // Encode path segments but keep structure intact
                String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8)
                        .replace("%2F", "/"); // Don't encode path separators
                bodyBuilder.append(ovhContainerName).append("/").append(encodedPath).append("\n");
            }
            String body = bodyBuilder.toString();

            // Build bulk delete URL
            URI bulkDeleteUri = URI.create(storageUrl + "?bulk-delete");

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(bulkDeleteUri)
                    .header("X-Auth-Token", token)
                    .header("Content-Type", "text/plain")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofMinutes(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return parseBulkDeleteResponse(response.body(), paths);
        } catch (IOException | InterruptedException e) {
            log.error("Error executing bulk delete request: {}", e.getMessage(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return BulkDeleteResult.allFailed(paths, "Request failed: " + e.getMessage());
        }
    }

    private BulkDeleteResult parseBulkDeleteResponse(String responseBody, List<String> requestedPaths) {
        Set<String> successfulPaths = new HashSet<>();
        Map<String, String> failedPaths = new HashMap<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            SwiftBulkDeleteResponse response = mapper.readValue(responseBody, SwiftBulkDeleteResponse.class);

            // Track all paths as potentially successful initially
            Set<String> remainingPaths = new HashSet<>(requestedPaths);

            // Process errors
            if (response.errors != null) {
                for (List<Object> error : response.errors) {
                    if (error.size() >= 2) {
                        String errorPath = extractPathFromSwiftResponse(String.valueOf(error.get(0)));
                        String errorMessage = String.valueOf(error.get(1));

                        // 404 errors are considered successful (object already deleted)
                        if (!"404 Not Found".equals(errorMessage)) {
                            failedPaths.put(errorPath, errorMessage);
                        }
                        remainingPaths.remove(errorPath);
                    }
                }
            }

            // All remaining paths are successful
            successfulPaths.addAll(remainingPaths);

            log.info("Bulk delete completed: {} successful, {} failed out of {} requested",
                    successfulPaths.size(), failedPaths.size(), requestedPaths.size());

            return new BulkDeleteResult(successfulPaths, failedPaths);
        } catch (Exception e) {
            log.error("Error parsing bulk delete response: {}. Response body: {}", e.getMessage(), responseBody);
            // If we can't parse the response, assume all failed
            return BulkDeleteResult.allFailed(requestedPaths, "Failed to parse response: " + e.getMessage());
        }
    }

    /**
     * Extract the original path from Swift response which includes container name.
     * Swift returns URL-encoded paths (as they were sent), so we need to decode them
     * to match the original unencoded paths used for comparison.
     */
    private String extractPathFromSwiftResponse(String swiftPath) {
        // Swift returns "container/path", we need just "path"
        String prefix = ovhContainerName + "/";
        String path;
        if (swiftPath.startsWith(prefix)) {
            path = swiftPath.substring(prefix.length());
        } else {
            path = swiftPath;
        }
        
        // Decode the path to match the original unencoded paths
        try {
            return URLDecoder.decode(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to decode path from Swift response: {}. Using as-is.", path, e);
            return path;
        }
    }

    /**
     * Response structure for Swift bulk delete operation.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SwiftBulkDeleteResponse {
        @JsonProperty("Number Not Found")
        public int numberNotFound;

        @JsonProperty("Number Deleted")
        public int numberDeleted;

        @JsonProperty("Errors")
        public List<List<Object>> errors;

        @JsonProperty("Response Status")
        public String responseStatus;

        @JsonProperty("Response Body")
        public String responseBody;
    }

    @Override
    public InputStream download(String path, EncryptionKey key) throws IOException {
        SwiftObject object;
        try {
            object = getClient().objectStorage().objects().get(ovhContainerName, path);
        } catch (AuthenticationException e) {
            log.error("ObjectStorage authentication failed.", e);
            object = authenticate().objectStorage().objects().get(ovhContainerName, path);
        }

        if (object == null) throw new FileNotFoundException("File " + path + " not found");

        InputStream in = object.download().getInputStream();
        if (key != null) {
            in = cipherInputStream(path, key, in);
        }
        return in;
    }

    @Override
    public void upload(String path, InputStream inputStream, EncryptionKey key, String contentType) throws RetryableOperationException, IOException {
        if (key != null) {
            if (key.getVersion() != 1 && key.getVersion() != 2){
                throw new UnsupportedKeyException("Unsupported key version " + key.getVersion());
            }
            try {
                byte[] iv = (key.getVersion() == 1) ? DigestUtils.md5(path) : DigestUtils.sha256(path);
                GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, iv);
                Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
                aes.init(Cipher.ENCRYPT_MODE, key, gcmParamSpec);

                inputStream = new CipherInputStream(inputStream, aes);

            } catch (Exception e) {
                log.error("Unable to encrypt file", e);
                throw new RetryableOperationException("Unable to encrypt file", e);
            }
        }

        String eTag;
        Payload<InputStream> payload = Payloads.create(inputStream);
        try {
            eTag = getClient().objectStorage().objects().put(ovhContainerName, path, payload);
            SwiftObject metaData = getClient().objectStorage().objects().get(ovhContainerName, path);
            if (metaData.getSizeInBytes() <= 0) {
                throw new IOException("File size is null - upload failed for: " + path);
            }

        } catch (AuthenticationException e) {
            log.error("ObjectStorage authentication failed.", e);
            eTag = authenticate().objectStorage().objects().put(ovhContainerName, path, payload);
        } catch (OvhConnectionFailedException e) {
            throw new RetryableOperationException("Ovh Connection Failed", e);
        }
        if (StringUtils.isEmpty(eTag)) {
            throw new IOException("ETag is empty - upload failed!" + path);
        }
    }

    @Override
    public List<String> listObjectNames(@Nullable String marker, int maxObjects) {
        ObjectListOptions options = ObjectListOptions.create().limit(maxObjects);
        if (marker != null) {
            options.marker(marker);
        }
        return getClient().objectStorage().objects()
                .list(ovhContainerName, options)
                .stream()
                .map(SwiftObject::getName)
                .toList();
    }

    @Override
    public void uploadV2(S3Bucket s3Bucket, String fileKey, InputStream inputStream, String contentType, EncryptionKey key) throws RetryableOperationException, IOException {
        throw new NotImplementedException("OVH does not support uploadV2 operation");
    }

    @Override
    public InputStream downloadV2(S3Bucket bucket, String path, EncryptionKey key) throws IOException {
        throw new NotImplementedException("OVH does not support downloadV2 operation");
    }

    @Override
    public void deleteV2(S3Bucket bucket, String path) {
        throw new NotImplementedException("OVH does not support deleteV2 operation");
    }

    @Override
    public BulkDeleteResult bulkDeleteV2(S3Bucket bucket, List<String> paths) {
        throw new NotImplementedException("OVH does not support bulkDeleteV2 operation");
    }

    @Override
    public List<String> listObjectNamesV2(S3Bucket s3Bucket, String prefix) {
        throw new NotImplementedException("OVH does not support listObjectNamesV2 operation");
    }

}