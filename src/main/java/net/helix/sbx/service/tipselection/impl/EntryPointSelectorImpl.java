package net.helix.sbx.service.tipselection.impl;

import net.helix.sbx.controllers.MilestoneViewModel;
import net.helix.sbx.model.Hash;
import net.helix.sbx.service.milestone.LatestMilestoneTracker;
import net.helix.sbx.service.snapshot.SnapshotProvider;
import net.helix.sbx.service.tipselection.EntryPointSelector;
import net.helix.sbx.storage.Tangle;

/**
 * Implementation of {@link EntryPointSelector} that given a depth {@code N}, returns a N-deep milestone.
 * Meaning <code>milestone(latestSolid - depth)</code>
 * Used as a starting point for the random walk.
 */
public class EntryPointSelectorImpl implements EntryPointSelector {

    private final Tangle tangle;
    private final SnapshotProvider snapshotProvider;
    private final LatestMilestoneTracker latestMilestoneTracker;

    /**
     * Constructor for Entry Point Selector
     * @param tangle Tangle object which acts as a database interface.
     * @param snapshotProvider accesses snapshots of the ledger state
     * @param latestMilestoneTracker  used to get latest milestone.
     */
    public EntryPointSelectorImpl(Tangle tangle, SnapshotProvider snapshotProvider,
                                  LatestMilestoneTracker latestMilestoneTracker) {
        this.tangle = tangle;
        this.snapshotProvider = snapshotProvider;
        this.latestMilestoneTracker = latestMilestoneTracker;
    }

    @Override
    public Hash getEntryPoint(int depth) throws Exception {
        int milestoneIndex = Math.max(snapshotProvider.getLatestSnapshot().getIndex() - depth - 1,
                snapshotProvider.getInitialSnapshot().getIndex());
        MilestoneViewModel milestoneViewModel = MilestoneViewModel.findClosestNextMilestone(tangle, milestoneIndex,
                latestMilestoneTracker.getLatestMilestoneIndex());
        if (milestoneViewModel != null && milestoneViewModel.getHash() != null) {
            return milestoneViewModel.getHash();
        }

        return snapshotProvider.getLatestSnapshot().getHash();
    }
}