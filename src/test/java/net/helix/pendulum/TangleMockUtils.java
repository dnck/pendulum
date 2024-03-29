package net.helix.pendulum;

import java.util.Map;

import net.helix.pendulum.controllers.TransactionViewModel;
import net.helix.pendulum.model.Hash;
import net.helix.pendulum.model.IntegerIndex;
import net.helix.pendulum.model.StateDiff;
import net.helix.pendulum.model.persistables.Round;
import net.helix.pendulum.model.persistables.Milestone;
import net.helix.pendulum.model.persistables.Transaction;
import net.helix.pendulum.storage.Tangle;
import net.helix.pendulum.utils.Pair;

import org.mockito.Mockito;


public class TangleMockUtils {

    public static Round mockRound(Tangle tangle, Hash hash, int index) {
        Round round = new Round();
        round.index = new IntegerIndex(index);
        round.set.add(hash);

        try {
            Mockito.when(tangle.load(Round.class, round.index)).thenReturn(round);
            Mockito.when(tangle.getLatest(Round.class, IntegerIndex.class)).
                    thenReturn(new Pair<>(round.index, round));
        } catch (Exception e) {
            // the exception can not be raised since we mock
        }

        return round;
    }

    /**
     * <p>
     * Registers a {@link Milestone} in the mocked tangle that can consequently be accessed by the tested classes.
     * </p>
     * <p>
     * It first creates the {@link Milestone} with the given details and then mocks the retrieval methods of the tangle
     * to return this object. In addition to mocking the specific retrieval method for the given hash, we also mock the
     * retrieval method for the "latest" entity so the mocked tangle returns the elements in the order that they were
     * mocked / created (which allows the mocked tangle to behave just like a normal one).
     * </p>
     * <p>
     * Note: We return the mocked object which allows us to set additional fields or modify it after "injecting" it into
     *       the mocked tangle.
     * </p>
     * 
     * @param tangle mocked tangle object that shall retrieve a milestone object when being queried for it
     * @param hash transaction hash of the milestone
     * @param index milestone index of the milestone
     * @return the Milestone object that be returned by the mocked tangle upon request
     */
    public static Milestone mockMilestone(Tangle tangle, Hash hash, int index) {
        Milestone milestone = new Milestone();
        milestone.hash = hash;
        milestone.index = new IntegerIndex(index);

        try {
            Mockito.when(tangle.load(Milestone.class, new IntegerIndex(index))).thenReturn(milestone);
            Mockito.when(tangle.getLatest(Milestone.class, IntegerIndex.class)).
                    thenReturn(new Pair<>(milestone.index, milestone));
        } catch (Exception e) {
            // the exception can not be raised since we mock
        }

        return milestone;
    }

    /**
     * Creates an empty transaction, which is marked filled and parsed.
     * This transaction is returned when the hash is asked to load in the tangle object
     * 
     * @param tangle mocked tangle object that shall retrieve a milestone object when being queried for it
     * @param hash transaction hash
     * @return The newly created (empty) transaction
     */
    public static Transaction mockTransaction(Tangle tangle, Hash hash) {
        Transaction transaction = new Transaction();
        transaction.bytes = new byte[0];
        transaction.type = TransactionViewModel.FILLED_SLOT;
        transaction.parsed = true;

        return mockTransaction(tangle, hash, transaction);
    }

    /**
     * Mocks the tangle object by checking for the hash and returning the transaction.
     * 
     * @param tangle mocked tangle object that shall retrieve a milestone object when being queried for it
     * @param hash transaction hash
     * @param transaction the transaction we send back
     * @return The transaction
     */
    public static Transaction mockTransaction(Tangle tangle, Hash hash, Transaction transaction) {
        try {
            Mockito.when(tangle.load(Transaction.class, hash)).thenReturn(transaction);
            Mockito.when(tangle.getLatest(Transaction.class, Hash.class)).thenReturn(new Pair<>(hash, transaction));
        } catch (Exception e) {
            // the exception can not be raised since we mock
        }

        return transaction;
    }

    public static StateDiff mockStateDiff(Tangle tangle, Hash hash, Map<Hash, Long> balanceDiff, int roundIndex) {
        StateDiff stateDiff = new StateDiff();
        stateDiff.state = balanceDiff;

        try {
            Mockito.when(tangle.load(StateDiff.class, hash)).thenReturn(stateDiff);
            Mockito.when(tangle.getLatest(StateDiff.class, Hash.class)).thenReturn(new Pair<>(hash, stateDiff));
            Mockito.when(tangle.load(StateDiff.class,  new IntegerIndex(roundIndex))).thenReturn(stateDiff);
        } catch (Exception e) {
            // the exception can not be raised since we mock
        }

        return stateDiff;
    }

    public static Round mockRound(Tangle tangle, int index, Hash hash) {
        Round round = new Round();
        round.index = new IntegerIndex(index);
        round.set.add(hash);
        return mockRound(tangle, index, round);
    }

    public static Round mockRound(Tangle tangle, int index, Round round) {

        try {
            Mockito.when(tangle.load(Round.class, new IntegerIndex(index))).thenReturn(round);
        } catch (Exception e) {
        }
        return round;
    }

}
