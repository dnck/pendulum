package net.helix.pendulum.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import net.helix.pendulum.conf.MainnetConfig;
import net.helix.pendulum.model.Hash;
import net.helix.pendulum.service.snapshot.SnapshotProvider;
import net.helix.pendulum.service.snapshot.impl.SnapshotProviderImpl;
import net.helix.pendulum.storage.Tangle;
import static net.helix.pendulum.TransactionTestUtils.getTransactionHash;

public class TransactionRequesterTest {


    private static final Tangle tangle = new Tangle();
    private static SnapshotProvider snapshotProvider;

    @Before
    public void setUp() throws Exception {
        snapshotProvider = new SnapshotProviderImpl().init(new MainnetConfig());
    }

    @After
    public void shutdown() throws Exception {
        snapshotProvider.shutdown();
    }

//    @Test
    public void init() throws Exception {
        // TODO implementation needed
    }

//    @Test
    public void rescanTransactionsToRequest() throws Exception {
        // TODO implementation needed
    }

//    @Test
    public void getRequestedTransactions() throws Exception {
        // TODO implementation needed
    }

//    @Test
    public void numberOfTransactionsToRequest() throws Exception {
        // TODO implementation needed
    }

//    @Test
    public void clearTransactionRequest() throws Exception {
        // TODO implementation needed
    }

//    @Test
    public void requestTransaction() throws Exception {
        // TODO implementation needed
    }

//    @Test
    public void transactionToRequest() throws Exception {
        // TODO implementation needed
    }

//    @Test
    public void checkSolidity() throws Exception {
        // TODO implementation needed
    }

//    @Test
    public void instance() throws Exception {
        // TODO implementation needed
    }

    @Test
    public void popEldestTransactionToRequest() throws Exception {
        TransactionRequester txReq = new TransactionRequester(tangle, snapshotProvider);
        // Add some Txs to the pool and see if the method pops the eldest one
        Hash eldest = getTransactionHash();
        txReq.requestTransaction(eldest, false);
        txReq.requestTransaction(getTransactionHash(), false);
        txReq.requestTransaction(getTransactionHash(), false);
        txReq.requestTransaction(getTransactionHash(), false);

        txReq.popEldestTransactionToRequest();
        // Check that the transaction is there no more
        Assert.assertFalse(txReq.isTransactionRequested(eldest, false));
    }

    @Test
    public void transactionRequestedFreshness() throws Exception {
        // Add some Txs to the pool and see if the method pops the eldest one
        List<Hash> eldest = new ArrayList<>(Arrays.asList(
                getTransactionHash(),
                getTransactionHash(),
                getTransactionHash()
        ));
        TransactionRequester txReq = new TransactionRequester(tangle, snapshotProvider);
        int capacity = TransactionRequester.MAX_TX_REQ_QUEUE_SIZE;
        //fill tips list
        for (int i = 0; i < 3; i++) {
            txReq.requestTransaction(eldest.get(i), false);
        }
        for (int i = 0; i < capacity; i++) {
            Hash hash = getTransactionHash();
            txReq.requestTransaction(hash, false);
        }

        //check that limit wasn't breached
        Assert.assertEquals("Queue capacity breached!!", capacity, txReq.numberOfTransactionsToRequest());
        // None of the eldest transactions should be in the pool
        for (int i = 0; i < 3; i++) {
            Assert.assertFalse("Old transaction has been requested", txReq.isTransactionRequested(eldest.get(i), false));
        }
    }

    @Test
    public void nonMilestoneCapacityLimited() throws Exception {
        TransactionRequester txReq = new TransactionRequester(tangle, snapshotProvider);
        int capacity = TransactionRequester.MAX_TX_REQ_QUEUE_SIZE;
        //fill tips list
        for (int i = 0; i < capacity * 2 ; i++) {
            Hash hash = getTransactionHash();
            txReq.requestTransaction(hash,false);
        }
        //check that limit wasn't breached
        Assert.assertEquals(capacity, txReq.numberOfTransactionsToRequest());
    }

    @Test
    public void milestoneCapacityNotLimited() throws Exception {
        TransactionRequester txReq = new TransactionRequester(tangle, snapshotProvider);
        int capacity = TransactionRequester.MAX_TX_REQ_QUEUE_SIZE;
        //fill tips list
        for (int i = 0; i < capacity * 2 ; i++) {
            Hash hash = getTransactionHash();
            txReq.requestTransaction(hash,true);
        }
        //check that limit was surpassed
        Assert.assertEquals(capacity * 2, txReq.numberOfTransactionsToRequest());
    }

    @Test
    public void mixedCapacityLimited() throws Exception {
        TransactionRequester txReq = new TransactionRequester(tangle, snapshotProvider);
        int capacity = TransactionRequester.MAX_TX_REQ_QUEUE_SIZE;
        //fill tips list
        for (int i = 0; i < capacity * 4 ; i++) {
            Hash hash = getTransactionHash();
            txReq.requestTransaction(hash, (i % 2 == 1));

        }
        //check that limit wasn't breached
        Assert.assertEquals(capacity + capacity * 2, txReq.numberOfTransactionsToRequest());
    }
 
}
