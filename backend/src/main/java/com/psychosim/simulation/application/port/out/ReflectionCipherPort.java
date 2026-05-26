package com.psychosim.simulation.application.port.out;

public interface ReflectionCipherPort {
    String encrypt(String plainText);

    String decrypt(String cipherText);
}
