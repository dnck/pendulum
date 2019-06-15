package net.helix.hlx.storage;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.helix.hlx.conf.MainnetConfig;
import net.helix.hlx.crypto.SpongeFactory;
import net.helix.hlx.model.TransactionHash;
import net.helix.hlx.model.persistables.Tag;
import net.helix.hlx.controllers.TransactionViewModel;
import net.helix.hlx.storage.rocksDB.RocksDBPersistenceProvider;
import net.helix.hlx.service.snapshot.SnapshotProvider;
import net.helix.hlx.service.snapshot.impl.SnapshotProviderImpl;


public class TangleTest {
    
    private static final Logger log = LoggerFactory.getLogger(TangleTest.class);
    private static final Random RND = new Random();

    private static final TemporaryFolder dbFolder = new TemporaryFolder();
    private static final TemporaryFolder logFolder = new TemporaryFolder();
    private static SnapshotProvider snapshotProvider;
    private final Tangle tangle = new Tangle();

    
    @Before
    public void setup() throws Exception {
        dbFolder.create();
        logFolder.create();
        RocksDBPersistenceProvider rocksDBPersistenceProvider =  new RocksDBPersistenceProvider(
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
    }

    @Test
    public void saveTest() throws Exception {
    }

    @Test
    public void getKeysStartingWithValueTest() throws Exception {
        byte[] bytes = new byte[TransactionViewModel.SIZE];
        RND.nextBytes(bytes);
        TransactionViewModel transactionViewModel = new TransactionViewModel(bytes,
                TransactionHash.calculate(SpongeFactory.Mode.S256, bytes));
        transactionViewModel.store(tangle, snapshotProvider.getInitialSnapshot());
        Set<Indexable> tag = tangle.keysStartingWith(Tag.class,
                Arrays.copyOf(transactionViewModel.getTagValue().bytes(), 15));
        Assert.assertNotEquals(tag.size(), 0);
    }

}