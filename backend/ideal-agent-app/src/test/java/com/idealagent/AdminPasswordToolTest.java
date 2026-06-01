package com.idealagent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminPasswordToolTest {

    @Test
    void encodeCreatesBCryptHashThatMatchesRawPassword() {
        String encoded = AdminPasswordTool.encode("Lx13839781056");

        assertTrue(encoded.startsWith("$2"));
        assertTrue(AdminPasswordTool.matches("Lx13839781056", encoded));
        assertFalse(AdminPasswordTool.matches("wrong-password", encoded));
        System.out.println(encoded);
    }
}
