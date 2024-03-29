package net.helix.pendulum.conf;

import net.helix.pendulum.model.Hash;

import java.util.Set;

/**
 * Configs that should be used for tracking milestones
 */
public interface MilestoneConfig extends Config {
    /**
     * @return {@value Descriptions#NOMINEE}
     */
    String getNominee();
    /**
     * @return Descriptions#INITIAL_NOMINEES
     */
    Set<Hash> getInitialNominees();
    /**
     * @return {@value Descriptions#DONT_VALIDATE_TESTNET_MILESTONE_SIG}
     */
    boolean isDontValidateTestnetMilestoneSig();
    /**
     * @return {@value Descriptions#GENESIS_TIME}
     */
    long getGenesisTime();
    /**
     * @return {@value Descriptions#ROUND_DURATION}
     */
    int getRoundDuration();
    /**
     * @return {@value Descriptions#ROUND_PAUSE}
     */
    int getRoundPause();
    /**
     * @return {@value Descriptions#NOMINEE_KEYFILE}
     */
    String getNomineeKeyfile();
    /**
     * @return {@value Descriptions#MILESTONE_KEY_DEPTH}
     */
    int getMilestoneKeyDepth();
    /**
     * @return {@value Descriptions#NOMINEE_SECURITY}
     */
    int getNomineeSecurity();

    interface Descriptions {
        String NOMINEE = "Flag that enables applying as a nominee in the network. A path to a file containing the seed has to be passed.";
        String INITIAL_NOMINEES = "The addresses of nominees the network starts with";
        String DONT_VALIDATE_TESTNET_MILESTONE_SIG = "Disable nominee validation on testnet";
        String GENESIS_TIME = "Time when the ledger started.";
        String ROUND_DURATION = "Duration of a round in milli secounds.";
        String ROUND_PAUSE = "Duration of time to finalize the round in milli secounds.";
        String NOMINEE_KEYFILE = "Filepath to nominee keyfile";
        String MILESTONE_KEY_DEPTH = "Depth of the merkle tree the milestones are signed with.";
        String NOMINEE_SECURITY = "Security level of transactions sent from a nominee (milestones, registrations)";
    }
}
