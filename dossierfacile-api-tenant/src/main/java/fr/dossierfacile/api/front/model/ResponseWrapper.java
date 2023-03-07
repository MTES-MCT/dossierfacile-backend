package fr.dossierfacile.api.front.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ResponseWrapper<T, M> {
    T data;
    M metadata;
}