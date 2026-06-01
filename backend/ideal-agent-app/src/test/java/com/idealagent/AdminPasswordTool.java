package com.idealagent;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class AdminPasswordTool {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private AdminPasswordTool() {
    }

    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        return ENCODER.matches(rawPassword, encodedPassword);
    }

    public static void main(String[] args) {
        if (args.length == 1) {
            String encoded = encode(args[0]);
            System.out.println(encoded);
            System.out.println("UPDATE ai_user SET password = '" + encoded + "' WHERE user_name = 'admin';");
            return;
        }

        if (args.length == 2) {
            System.out.println(matches(args[0], args[1]));
            return;
        }

        System.err.println("Usage: AdminPasswordTool <rawPassword> OR AdminPasswordTool <rawPassword> <bcryptHash>");
        System.exit(1);
    }
}
