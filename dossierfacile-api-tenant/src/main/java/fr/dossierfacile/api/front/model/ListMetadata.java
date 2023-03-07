package fr.dossierfacile.api.front.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ListMetadata {
    long limit;
    long resultCount;
    String nextLink;
}

