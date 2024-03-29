package net.helix.pendulum.controllers;

import net.helix.pendulum.storage.rocksdb.RocksDBPersistenceProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.helix.pendulum.conf.MainnetConfig;
import net.helix.pendulum.crypto.SpongeFactory;
import net.helix.pendulum.model.TransactionHash;
import net.helix.pendulum.service.snapshot.SnapshotProvider;
import net.helix.pendulum.service.snapshot.impl.SnapshotProviderImpl;
import net.helix.pendulum.storage.Tangle;
import static net.helix.pendulum.TransactionTestUtils.getTransactionBytes;


public class BundleViewModelTest {

    private static final TemporaryFolder dbFolder = new TemporaryFolder();
    private static final TemporaryFolder logFolder = new TemporaryFolder();
    private static final Tangle tangle = new Tangle();
    private static SnapshotProvider snapshotProvider;

    @Before
    public void setUp() throws Exception {
        dbFolder.create();
        logFolder.create();
        RocksDBPersistenceProvider rocksDBPersistenceProvider;
        rocksDBPersistenceProvider =  new RocksDBPersistenceProvider(
                dbFolder.getRoot().getAbsolutePath(), logFolder.getRoot().getAbsolutePath(),
                1000, Tangle.COLUMN_FAMILIES, Tangle.METADATA_COLUMN_FAMILY);
        tangle.addPersistenceProvider(rocksDBPersistenceProvider);
        tangle.init();
        snapshotProvider = new SnapshotProviderImpl().init(new MainnetConfig());
    }

    @After
    public void shutdown() throws Exception {
        tangle.shutdown();
        snapshotProvider.shutdown();
        dbFolder.delete();
        logFolder.delete();
    }

    @Test
    public void quietFromHash() throws Exception {

    }

    @Test
    public void fromHash() throws Exception {

    }

    @Test
    public void getTransactionViewModels() throws Exception {

    }

    @Test
    public void quietGetTail() throws Exception {

    }

    @Test
    public void getTail() throws Exception {

    }

    @Test
    public void firstShouldFindTxTest() throws Exception {
        byte[] bytes = getTransactionBytes();
        TransactionViewModel transactionViewModel = new TransactionViewModel(bytes, TransactionHash.calculate(SpongeFactory.Mode.S256, bytes));
        transactionViewModel.store(tangle, snapshotProvider.getInitialSnapshot());

        BundleViewModel result = BundleViewModel.first(tangle);
        Assert.assertTrue(result.getHashes().contains(transactionViewModel.getHash()));
    }

}
