package fr.dossierfacile.api.front.register;

public interface RegisterFactory {
    <T> SaveStep<T> get(String step);
}
