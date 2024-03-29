package net.helix.pendulum.service.nominee.impl;

import net.helix.pendulum.conf.PendulumConfig;
import net.helix.pendulum.controllers.AddressViewModel;
import net.helix.pendulum.controllers.BundleViewModel;
import net.helix.pendulum.controllers.RoundViewModel;
import net.helix.pendulum.controllers.TransactionViewModel;
import net.helix.pendulum.crypto.SpongeFactory;
import net.helix.pendulum.model.Hash;
import net.helix.pendulum.model.HashFactory;
import net.helix.pendulum.service.nominee.NomineeService;
import net.helix.pendulum.service.nominee.NomineeSolidifier;
import net.helix.pendulum.service.nominee.NomineeTracker;
import net.helix.pendulum.service.snapshot.SnapshotProvider;
import net.helix.pendulum.service.utils.RoundIndexUtil;
import net.helix.pendulum.storage.Tangle;
import net.helix.pendulum.utils.log.interval.IntervalLogger;
import net.helix.pendulum.utils.thread.DedicatedScheduledExecutorService;
import net.helix.pendulum.utils.thread.SilentScheduledExecutorService;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class NomineeTrackerImpl implements NomineeTracker {

    private static final int MAX_CANDIDATES_TO_ANALYZE = 5000;
    private static final int RESCAN_INTERVAL = 1000;
    private Hash Curator_Address;

    private static final IntervalLogger log = new IntervalLogger(NomineeTrackerImpl.class);
    private final SilentScheduledExecutorService executorService = new DedicatedScheduledExecutorService(
            "Nominee Tracker", log.delegate());

    private Tangle tangle;
    private PendulumConfig config;
    private SnapshotProvider snapshotProvider;
    private NomineeService nomineeService;
    private NomineeSolidifier nomineeSolidifier;
    private Set<Hash> latestNominees;
    private Hash latestNomineeHash;
    private int startRound;
    private final Set<Hash> seenCuratorTransactions = new HashSet<>();
    private final Deque<Hash> curatorTransactionsToAnalyze = new ArrayDeque<>();

    public NomineeTrackerImpl init(Tangle tangle, SnapshotProvider snapshotProvider, NomineeService nomineeService, NomineeSolidifier nomineeSolidifier, PendulumConfig config) {

        this.tangle = tangle;
        this.config = config;
        this.Curator_Address = config.getCuratorAddress();
        this.snapshotProvider = snapshotProvider;
        this.nomineeService = nomineeService;
        this.nomineeSolidifier = nomineeSolidifier;
        this.latestNominees = config.getInitialNominees();

       // startRound = (int) (System.currentTimeMillis() - config.getGenesisTime()) / config.getRoundDuration() + 2 ; // start round of the initial nominees
        startRound = RoundIndexUtil.getRound(RoundIndexUtil.getCurrentTime(),  config.getGenesisTime(), config.getRoundDuration(), 2);
        latestNomineeHash = Hash.NULL_HASH;
        //bootstrapLatestNominees();

        return this;
    }

    @Override
    public Set<Hash> getLatestNominees() {
        return latestNominees;
    }

    @Override
    public Hash getLatestNomineeHash() {
        return latestNomineeHash;
    }

    @Override
    public int getStartRound() {
        return startRound;
    }


    // todo there is not an entry for each round, just for the start round from which the nominees apply, so this method has to be adjusted
    @Override
    public Set<Hash> getNomineesOfRound(int roundIndex) throws Exception {
        try {
            Set<Hash> validators = new HashSet<>();
            for (Hash hash : AddressViewModel.load(tangle, Curator_Address).getHashes()) {
                TransactionViewModel transaction = TransactionViewModel.fromHash(tangle, hash);
                if (RoundViewModel.getRoundIndex(transaction) == roundIndex) {
                    validators = getNomineeAddresses(hash);
                }
            }
            return validators;
        } catch (Exception e) {
            throw new Exception("unexpected error while getting Validators of round #{}" + roundIndex, e);
        }
    }


    @Override
    public boolean processNominees(Hash transactionHash) throws Exception {
        TransactionViewModel transaction = TransactionViewModel.fromHash(tangle, transactionHash);
        try {
            if (Curator_Address.equals(transaction.getAddressHash()) && transaction.getCurrentIndex() == 0) {

                int roundIndex = RoundViewModel.getRoundIndex(transaction);

                log.info("Process Nominee Transaction " + transaction.getHash() + ", start round: " + roundIndex);

                // if the trustee transaction is older than our ledger start point: we already processed it in the past
                if (roundIndex <= snapshotProvider.getInitialSnapshot().getIndex()) {
                    return true;
                }

                // validate
                switch (nomineeService.validateNominees(transaction, roundIndex, SpongeFactory.Mode.S256, config.getCuratorSecurity())) {
                    case VALID:
                        log.info("Nominee Transaction " + transaction.getHash() + " is VALID");
                        //System.out.println("round index: " + roundIndex);
                        //System.out.println("start round: " + startRound);
                        if (roundIndex > startRound) {
                            latestNominees = getNomineeAddresses(transaction.getHash());
                            latestNomineeHash = transaction.getHash();
                            startRound = roundIndex;
                            //System.out.println("New nominees:");
                            //latestNominees.forEach(n -> System.out.println(n));
                        }

                        if (!transaction.isSolid()) {
                            nomineeSolidifier.add(transaction.getHash(), roundIndex);
                        }

                        break;

                    case INCOMPLETE:
                        log.info("Nominee Transaction " + transaction.getHash() + " is INCOMPLETE");
                        nomineeSolidifier.add(transaction.getHash(), roundIndex);
                        return false;

                    default:
                }
            } else {
                return false;
            }
            return true;
        } catch (Exception e) {
            throw new Exception("unexpected error while processing trustee transaction " + transaction, e);
        }
    }

    @Override
    public Set<Hash> getNomineeAddresses(Hash transaction) throws Exception {
        TransactionViewModel tail = TransactionViewModel.fromHash(tangle, transaction);
        BundleViewModel bundle = BundleViewModel.load(tangle, tail.getBundleHash());
        int security = config.getNomineeSecurity();
        Set<Hash> validators = new HashSet<>();

        for (Hash txHash : bundle.getHashes()) {
            TransactionViewModel tx = TransactionViewModel.fromHash(tangle, txHash);
            // get transactions with validator addresses in signatureMessageFragment
            // 0 - security: tx with signature
            // security: tx with merkle path
            // security+1 - n: tx with validator addresses
            if ((tx.getCurrentIndex() > security)) {

                //System.out.println("Get Nominees");
                for (int i = 0; i < TransactionViewModel.SIGNATURE_MESSAGE_FRAGMENT_SIZE / Hash.SIZE_IN_BYTES; i++) {
                    Hash address = HashFactory.ADDRESS.create(tx.getSignature(), i * Hash.SIZE_IN_BYTES, Hash.SIZE_IN_BYTES);
                    Hash null_address = HashFactory.ADDRESS.create("0000000000000000000000000000000000000000000000000000000000000000");
                    // TODO: Do we send all validators or only adding and removing ones ?
                    if (address.equals(null_address)) {
                        return validators;
                    }
                    validators.add(address);
                }
            }
        }
        return validators;
    }

    @Override
    public void analyzeCuratorTransactions() throws Exception {
        int transactionsToAnalyze = Math.min(curatorTransactionsToAnalyze.size(), MAX_CANDIDATES_TO_ANALYZE);
        for (int i = 0; i < transactionsToAnalyze; i++) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            Hash trusteeTransactionHash = curatorTransactionsToAnalyze.pollFirst();
            if (!processNominees(trusteeTransactionHash)) {
                seenCuratorTransactions.remove(trusteeTransactionHash);
            }
        }
    }

    @Override
    public void collectNewCuratorTransactions() throws Exception {
        try {
            for (Hash hash : AddressViewModel.load(tangle, Curator_Address).getHashes()) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                if (seenCuratorTransactions.add(hash)) {
                    curatorTransactionsToAnalyze.addFirst(hash);
                }
            }
        } catch (Exception e) {
            throw new Exception("failed to collect the new trustee transactions", e);
        }
    }

    private void validatorTrackerThread() {
        try {
            collectNewCuratorTransactions();
            analyzeCuratorTransactions();
        } catch (Exception e) {
            log.error("error while scaning for trustee transactions", e);
        }
    }

    public void start() {
        executorService.silentScheduleWithFixedDelay(this::validatorTrackerThread, 0, RESCAN_INTERVAL,
                TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        executorService.shutdownNow();
    }
}