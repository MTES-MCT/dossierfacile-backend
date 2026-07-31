package fr.dossierfacile.common.application.projection;

import java.util.Collection;
import java.util.List;

/**
 * Contract of a read-model projection (Notion §8.C): transforms a source — an aggregate,
 * or a composed read view when the DTO needs data from several aggregates or derived
 * values — into an API DTO. Projections are plain components, unit-testable without Spring.
 *
 * @param <S> the projected source (aggregate or read view)
 * @param <D> the produced DTO
 */
public abstract class BaseProjection<S, D> {

    public abstract D project(S source);

    public List<D> projectAll(Collection<S> sources) {
        if (sources == null) {
            return List.of();
        }
        return sources.stream().map(this::project).toList();
    }
}
