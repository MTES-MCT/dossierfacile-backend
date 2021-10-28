package fr.gouv.bo.dto;

public interface OperatorFlowDTO {
    double getAvg();

    long getInvalid();

    long getSame();

    long getTenant();

    long getTotal();

    long getValid();

    int getWeek();

    int getYear();
}
