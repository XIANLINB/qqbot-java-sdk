package com.xuanji.qqbot.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntentsTest {
    @Test
    void orBits() {
        long v = Intents.of(Intents.GROUP_AND_C2C_EVENT, Intents.INTERACTION);
        assertEquals((1L << 25) | (1L << 26), v);
    }
}
