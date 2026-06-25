package fr.dossierfacile.api.front.application.usecase;

// C = Command type, R = return type

import lombok.AllArgsConstructor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@AllArgsConstructor
public abstract class BaseUseCase<C, R> {

    protected final TransactionTemplate transactionTemplate;

    public BaseUseCase(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // Call this method if you want a simple transaction management
    public R executeWithTransaction(C command) {
        checkTransaction();
        return transactionTemplate.execute(status -> execute(command));
    }

    public abstract R execute(C command);

    protected void checkTransaction() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("A useCase can not be executed inside a transaction");
        }
    }
}
