package de.oliverwetterau.neo4j.websockets.server.aspects;

import de.oliverwetterau.neo4j.websockets.server.annotations.Propagation;
import de.oliverwetterau.neo4j.websockets.server.annotations.Transactional;
import de.oliverwetterau.neo4j.websockets.core.data.Result;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.neo4j.graphdb.GraphDatabaseService;
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
    private static GraphDatabaseService graphDatabaseService;

    /**
     * Holds a list of nested transactions and their statuses (success / failure) for the current thread.
     */
    public class TransactionHolder {
        public class CurrentTransaction {
            Transaction transaction;
            boolean success;
        }

        /** FiLo-List of all transactions */
        private ArrayDeque<CurrentTransaction> transactions = new ArrayDeque<>();

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
                currentTransaction.transaction = graphDatabaseService.beginTx();
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
            logger.debug("[failure]");
            transactions.getLast().success = false;
        }

        /**
         * Ends and closes a transaction.
         */
        public void close() {
            CurrentTransaction currentTransaction = transactions.removeLast();
            Transaction transaction = currentTransaction.transaction;

            if (currentTransaction.success) {
                logger.debug("[close] Transaction success");
                transaction.success();
            }
            else {
                logger.debug("[close] Transaction failure");
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
    private ThreadLocal<TransactionHolder> transactionHolderThreadLocal = new ThreadLocal<>();

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

                    if (isNewTransaction) {
                        // create json value of result to prevent another access to Neo4j when requesting json value of
                        // result as result.toJsonString() will otherwise access to entities will thrown an exception
                        // when not in a transaction - without a change on Result toString will cache the json value
                        // created in toString()
                        result.close();
                    }

                    if (!result.isOk()) {
                        transactionHolder.failure();
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error("[databaseCallAroundAdvice] exception", e);
            throwable = e;
            transactionHolder.failure();
        }
        finally {
            if (isNewTransaction) {
                transactionHolder.close();
            }
        }

        if (throwable != null) {
            throw throwable;
        }

        logger.debug("[databaseCallAroundAdvice] After @Transaction {}", proceedingJoinPoint.toString());

        return value;
    }

    public static void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
        DatabaseCallAspect.graphDatabaseService = graphDatabaseService;
    }
}
