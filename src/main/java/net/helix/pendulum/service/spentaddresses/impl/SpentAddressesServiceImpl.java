package net.helix.pendulum.service.spentaddresses.impl;

import net.helix.pendulum.BundleValidator;
import net.helix.pendulum.controllers.AddressViewModel;
import net.helix.pendulum.controllers.RoundViewModel;
import net.helix.pendulum.controllers.TransactionViewModel;
import net.helix.pendulum.model.Hash;
import net.helix.pendulum.service.snapshot.SnapshotProvider;
import net.helix.pendulum.service.spentaddresses.SpentAddressesException;
import net.helix.pendulum.service.spentaddresses.SpentAddressesProvider;
import net.helix.pendulum.service.spentaddresses.SpentAddressesService;
import net.helix.pendulum.service.tipselection.TailFinder;
import net.helix.pendulum.service.tipselection.impl.TailFinderImpl;
import net.helix.pendulum.storage.Tangle;
import net.helix.pendulum.utils.dag.DAGHelper;
import org.apache.commons.collections4.CollectionUtils;
import pl.touk.throwing.ThrowingPredicate;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * Implementation of <tt>SpentAddressesService</tt> that calculates and checks spent addresses using the {@link Tangle}
 *
 */
public class SpentAddressesServiceImpl implements SpentAddressesService {
    private Tangle tangle;

    private SnapshotProvider snapshotProvider;

    private SpentAddressesProvider spentAddressesProvider;

    private TailFinder tailFinder;

    /**
     * Creates a Spent address service using the Tangle
     *
     * @param tangle Tangle object which is used to load models of addresses
     * @param snapshotProvider {@link SnapshotProvider} to find the genesis, used to verify tails
     * @param spentAddressesProvider Provider for loading/saving addresses to a database.
     * @return this instance
     */
    public SpentAddressesServiceImpl init(Tangle tangle, SnapshotProvider snapshotProvider, SpentAddressesProvider spentAddressesProvider) {
        this.tangle = tangle;
        this.snapshotProvider = snapshotProvider;
        this.spentAddressesProvider = spentAddressesProvider;
        this.tailFinder = new TailFinderImpl(tangle);

        return this;
    }

    @Override
    public boolean wasAddressSpentFrom(Hash addressHash) throws SpentAddressesException {
        if (spentAddressesProvider.containsAddress(addressHash)) {
            return true;
        }

        try {
            Set<Hash> hashes = AddressViewModel.load(tangle, addressHash).getHashes();
            for (Hash hash : hashes) {
                TransactionViewModel tx = TransactionViewModel.fromHash(tangle, hash);
                // Check for spending transactions
                if (wasTransactionSpentFrom(tx)) {
                    return true;
                }
            }
        } catch (Exception e) {
            throw new SpentAddressesException(e);
        }

        return false;
    }

    @Override
    public void persistSpentAddresses(int fromMilestoneIndex, int toMilestoneIndex) throws SpentAddressesException {
        Set<Hash> addressesToCheck = new HashSet<>();
        try {
            for (int i = fromMilestoneIndex; i < toMilestoneIndex; i++) {
                RoundViewModel currentMilestone = RoundViewModel.get(tangle, i);
                if (currentMilestone != null) {
                    for (Hash confirmedTip : currentMilestone.getConfirmedTips(tangle, 1)) {
                        DAGHelper.get(tangle).traverseApprovees(
                                confirmedTip,
                                transactionViewModel -> transactionViewModel.snapshotIndex() >= currentMilestone.index(),
                                transactionViewModel -> addressesToCheck.add(transactionViewModel.getAddressHash())
                        );
                    }
                }
            }
        } catch (Exception e) {
            throw new SpentAddressesException(e);
        }

        //Can only throw runtime exceptions in streams
        try {
            spentAddressesProvider.saveAddressesBatch(addressesToCheck.stream()
                    .filter(ThrowingPredicate.unchecked(this::wasAddressSpentFrom))
                    .collect(Collectors.toList()));
        } catch (RuntimeException e) {
            if (e.getCause() instanceof SpentAddressesException) {
                throw (SpentAddressesException) e.getCause();
            } else {
                throw e;
            }
        }
    }

    @Override
    public void persistSpentAddresses(Collection<TransactionViewModel> transactions) throws SpentAddressesException {
        try {
            Collection<Hash> spentAddresses = transactions.stream()
                    .filter(ThrowingPredicate.unchecked(this::wasTransactionSpentFrom))
                    .map(TransactionViewModel::getAddressHash).collect(Collectors.toSet());

            spentAddressesProvider.saveAddressesBatch(spentAddresses);
        } catch (RuntimeException e) {
            throw new SpentAddressesException("Exception while persisting spent addresses", e);
        }
    }

    private boolean wasTransactionSpentFrom(TransactionViewModel tx) throws Exception {
        Optional<Hash> tailFromTx = tailFinder.findTailFromTx(tx);
        if (tailFromTx.isPresent() && tx.value() < 0) {
            // Transaction is confirmed
            if (tx.snapshotIndex() != 0) {
                return true;
            }

            // transaction is pending
            Hash tailHash = tailFromTx.get();
            return isBundleValid(tailHash);
        }

        return false;
    }

    private boolean isBundleValid(Hash tailHash) throws Exception {
        List<List<TransactionViewModel>> validation =
                BundleValidator.validate(tangle, snapshotProvider.getInitialSnapshot(), tailHash);
        return (CollectionUtils.isNotEmpty(validation) && validation.get(0).get(0).getValidity() == 1);
    }
}
