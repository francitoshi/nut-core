/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.core.utils;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HWDiskStore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HardwareFingerprint
{
    public static String getHardwareFingerprint()
    {
        SystemInfo si = new SystemInfo();

        // CPU info
        CentralProcessor cpu = si.getHardware().getProcessor();
        String cpuId = cpu.getProcessorIdentifier().getIdentifier();

        // Main hard drive (we take the first one available)
        String diskSerial = "";
        for (HWDiskStore disk : si.getHardware().getDiskStores())
        {
            if (disk.getSerial() != null && !disk.getSerial().isEmpty())
            {
                diskSerial = disk.getSerial();
                break;
            }
        }

        // Motherboard
        ComputerSystem cs = si.getHardware().getComputerSystem();
        String baseboardSerial = cs.getBaseboard().getSerialNumber();

        // Concatenar datos
        String rawData = cpuId + "|" + diskSerial + "|" + baseboardSerial;
        // hash
        return sha256(rawData);
    }

    private static String sha256(String input)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash)
            {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
