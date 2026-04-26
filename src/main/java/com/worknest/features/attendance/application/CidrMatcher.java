package com.worknest.features.attendance.application;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.stereotype.Component;

@Component
public class CidrMatcher {

    public boolean matches(String ipAddress, String cidr) {
        if (ipAddress == null || ipAddress.isBlank() || cidr == null || cidr.isBlank()) {
            return false;
        }

        try {
            String[] parts = cidr.trim().split("/", 2);
            if (parts.length != 2) {
                return false;
            }
            byte[] addressBytes = InetAddress.getByName(ipAddress.trim()).getAddress();
            byte[] networkBytes = InetAddress.getByName(parts[0].trim()).getAddress();
            int prefixLength = Integer.parseInt(parts[1].trim());

            if (addressBytes.length != networkBytes.length) {
                return false;
            }

            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (addressBytes[i] != networkBytes[i]) {
                    return false;
                }
            }

            if (remainingBits == 0) {
                return true;
            }

            int mask = 0xFF << (8 - remainingBits);
            return (addressBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
        } catch (UnknownHostException | NumberFormatException exception) {
            return false;
        }
    }
}
