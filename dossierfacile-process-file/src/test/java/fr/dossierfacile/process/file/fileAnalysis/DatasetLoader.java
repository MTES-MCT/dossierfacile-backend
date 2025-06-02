package fr.dossierfacile.process.file.fileAnalysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.dossierfacile.fileAnalysis.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
public class DatasetLoader {

    @Autowired
    private TestOvhFileStorageServiceImpl ovhFileStorageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public <V, I, F> FileAnalysisTestData<V, I, F> loadDataset(
            String datasetPath,
            Class<F> fileDescriptionClass,
            Class<V> expectedResultClass,
            Class<I> exepectedErrorClass
    ) throws ExecutionException, InterruptedException, IOException {

        var file = ovhFileStorageService.downloadAsync(datasetPath).get();

        var dataset = objectMapper.readValue(file, JsonFileAnalysisTestData.class);

        return new FileAnalysisTestData<>(
                dataset.getTestName(),
                dataset.getDocumentCategory(),
                dataset.getDocumentSubCategory(),
                getValidDocumentData(dataset.getValidDocuments(), fileDescriptionClass, expectedResultClass),
                getInvalidDocumentData(dataset.getInvalidDocuments(), fileDescriptionClass, exepectedErrorClass)
        );

    }

    private <T, F> List<ValidDocumentData<T, F>> getValidDocumentData(
            List<JsonValidDocumentData> jsonData,
            Class<F> fileDescriptionClass,
            Class<T> expectedResultClass
    ) {
        return jsonData.stream().map(item -> {
            try {
                return new ValidDocumentData<>(
                        item.getBucketPath(),
                        item.getFileDescription() != null ?
                                objectMapper.readValue(item.getFileDescription().toString(), fileDescriptionClass) :
                                null,
                        item.getExpectedResult() != null ?
                                objectMapper.readValue(item.getExpectedResult().toString(), expectedResultClass) :
                                null
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

    private <I, F> List<InvalidDocumentData<I, F>> getInvalidDocumentData(
            List<JsonInvalidDocumentData> jsonData,
            Class<F> fileDescriptionClass,
            Class<I> expectedErrorClass
    ) {
        return jsonData.stream().map(item -> {
            try {
                return new InvalidDocumentData<>(
                        item.getBucketPath(),
                        item.getFileDescription() != null ?
                                objectMapper.readValue(item.getFileDescription().toString(), fileDescriptionClass) :
                                null,
                        item.getExpectedError() != null ?
                                objectMapper.readValue(item.getExpectedError().toString(), expectedErrorClass) :
                                null
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }
}
