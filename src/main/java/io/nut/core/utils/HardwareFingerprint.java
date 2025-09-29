/*
 *  HardwareFingerprint.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 *
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

    public static void main(String[] args)
    {
        System.out.println("Fingerprint del equipo: " + getHardwareFingerprint());
    }

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
        System.out.println("rawData: " + rawData);
        
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
