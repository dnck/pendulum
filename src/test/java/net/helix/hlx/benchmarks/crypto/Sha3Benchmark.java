package net.helix.hlx.benchmarks.crypto;

import net.helix.hlx.crypto.Sha3;
import net.helix.hlx.crypto.SpongeFactory;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.openjdk.jmh.annotations.Benchmark;



public class Sha3Benchmark {

    private final static String txHex = "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a3fcb75bbfc68db05a5207c2afc97fc496ec86e7ecdd6a933be4d1bad8f74c3400000000000000000000000000000000000000000000000000000000000000000000000000000004000000005bdf1138000000000000000000000000000000022806d634614f758a0558610043329f310ad5d227ab5bf6f4c24b2fbf7d8de7500000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
    private final static String txHash = "d12a1accea363b6233077e7bb7fc1352f8384e29c47af635373cb57eb07a5c47";

    /**
     * Benchmark absorb and squeeze methods of Sha3 hash function.
     */
    @Benchmark
    public void sha3() {
        int size = 768;
        byte[] in_bytes = Hex.decode(txHex);
        byte[] hash_bytes = new byte[Sha3.HASH_LENGTH];

        Sha3 sha3 = (Sha3) SpongeFactory.create(SpongeFactory.Mode.S256);

        sha3.absorb(in_bytes, 0, in_bytes.length);
        sha3.squeeze(hash_bytes, 0, Sha3.HASH_LENGTH);

        Assert.assertEquals(txHash, Hex.toHexString(hash_bytes));
    }

}
