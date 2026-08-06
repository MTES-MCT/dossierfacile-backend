package fr.dossierfacile.common.application.usecase;

// C = Command type, R = return type

import lombok.AllArgsConstructor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

@AllArgsConstructor
public abstract class BaseUseCase<C, R> {

    protected final TransactionTemplate transactionTemplate;

    protected BaseUseCase(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // Call this method if you want a simple transaction management
    public R executeWithTransaction(C command) {
        checkTransaction();
        return transactionTemplate.execute(status -> execute(command));
    }

    // Ce wrapper de l'outil de transaction permet de gérer @Nullable de la méthode avec les Optional de java de manière propre
    @SuppressWarnings("unchecked")
    protected <T> Optional<T> executeInTransaction(TransactionCallback<?> action) {
        Object result = transactionTemplate.execute(action);
        if (result == null) {
            return Optional.empty();
        }
        if (result instanceof Optional<?>) {
            return (Optional<T>) result;
        }
        return Optional.of((T) result);
    }

    public abstract R execute(C command);

    protected void checkTransaction() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("A useCase can not be executed inside a transaction");
        }
    }
}
