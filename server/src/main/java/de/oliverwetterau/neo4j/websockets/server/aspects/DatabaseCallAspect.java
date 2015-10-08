package de.oliverwetterau.neo4j.websockets.server.aspects;

import de.oliverwetterau.neo4j.websockets.core.data.Result;
import de.oliverwetterau.neo4j.websockets.server.annotations.Propagation;
import de.oliverwetterau.neo4j.websockets.server.annotations.Transactional;
import de.oliverwetterau.neo4j.websockets.server.neo4j.EmbeddedNeo4j;
import de.oliverwetterau.neo4j.websockets.server.neo4j.EmbeddedNeo4jBuilder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;

/**
 * Aspect to surround method with transactions needed by Neo4j.
 *
 * @author Oliver Wetterau
 */
@Aspect
public class DatabaseCallAspect {
    private static Logger logger = LoggerFactory.getLogger(DatabaseCallAspect.class);
    protected static EmbeddedNeo4j embeddedNeo4j;

    /**
     * Holds a list of nested transactions and their statuses (success / failure) for the current thread.
     */
    public class TransactionHolder {
        public class CurrentTransaction {
            Transaction transaction;
            boolean success;
        }

        /** FiLo-List of all transactions */
        protected ArrayDeque<CurrentTransaction> transactions = new ArrayDeque<>();

        /**
         * Begins a transaction
         * @param propagation defines how to handle new transactions
         * @return true, if a new transaction was created; false, if current transaction is being reused
         */
        public boolean begin(Propagation propagation) {
            CurrentTransaction currentTransaction;

            logger.debug("[begin] begin");

            if (transactions.isEmpty() || propagation.equals(Propagation.REQUIRES_NEW)) {
                currentTransaction = new CurrentTransaction();
                currentTransaction.transaction = getEmbeddedNeo4j().getDatabase().beginTx();
                currentTransaction.success = true;
                transactions.addLast(currentTransaction);

                logger.debug("[begin] Transaction created");

                return true;
            }

            logger.debug("[begin] end");

            return false;
        }

        /**
         * Marks a transaction as failed.
         */
        public void failure() {
            transactions.getLast().success = false;
        }

        /**
         * Ends and closes a transaction.
         */
        public void close() {
            CurrentTransaction currentTransaction = transactions.removeLast();
            Transaction transaction = currentTransaction.transaction;

            if (currentTransaction.success) {
                transaction.success();
            }
            else {
                transaction.failure();
            }

            transaction.close();

            logger.debug("[close] Transaction closed");
        }

        @Override
        public void finalize() throws Throwable {
            while (!transactions.isEmpty()) {
                close();
            }

            super.finalize();
        }
    }

    /** List of transactions linked to current thread */
    protected ThreadLocal<TransactionHolder> transactionHolderThreadLocal = new ThreadLocal<>();

    /**
     * Wraps transactional logic around methods annotated with "@Transaction"
     * @param proceedingJoinPoint wrapped method call
     * @param transactional annotation with parameters
     * @return return value of wrapped method call
     * @throws Throwable rethrown exception of wrapped method call
     */
    //@Around("@annotation(transactional)")
    @Around(value = "execution(@Transactional * *(..)) && @annotation(transactional)")
    public Object databaseCallAroundAdvice(ProceedingJoinPoint proceedingJoinPoint, Transactional transactional)
            throws Throwable
    {
        logger.debug("[databaseCallAroundAdvice] Before @Transaction {}", proceedingJoinPoint.toString());

        boolean isNewTransaction;
        Throwable throwable = null;
        Object value = null;

        TransactionHolder transactionHolder = transactionHolderThreadLocal.get();

        // if no TransactionHolder is associated with the current thread, create a new one and link it to this thread.
        if (transactionHolder == null) {
            transactionHolder = new TransactionHolder();
            transactionHolderThreadLocal.set(transactionHolder);
        }
        // begin transaction
        isNewTransaction = transactionHolder.begin(transactional.value());

        Result result = null;

        try {
            value = proceedingJoinPoint.proceed();

            if (value != null) {
                if (Result.class.isAssignableFrom(value.getClass())) {
                    result = (Result) value;
                    if (!result.isOk()) {
                        transactionHolder.failure();
                    }
                }
            }
        }
        catch (Throwable e) {
            throwable = e;
            transactionHolder.failure();

            logger.error("[databaseCallAroundAdvice]", e);
        }

        if (isNewTransaction) {
            if (result != null) {
                // create json value of result to prevent another access to Neo4j when requesting json value of result
                // as result.toJsonString() will otherwise access to entities will thrown an exception when not in a
                // transaction - without a change on Result toString will cache the json value created in toString()
                result.close();
            }
            transactionHolder.close();
        }

        if (throwable != null) {
            throw throwable;
        }

        logger.debug("[databaseCallAroundAdvice] After @Transaction {}", proceedingJoinPoint.toString());

        return value;
    }

    protected EmbeddedNeo4j getEmbeddedNeo4j() {
        if (embeddedNeo4j == null) {
            embeddedNeo4j = EmbeddedNeo4jBuilder.getNeo4j();
        }

        return embeddedNeo4j;
    }
}
