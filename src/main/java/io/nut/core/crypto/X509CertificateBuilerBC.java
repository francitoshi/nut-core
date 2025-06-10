/*
 *  X509CertificateBuilerBC.java
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
 */
package io.nut.core.crypto;

import io.nut.base.crypto.Kripto;
import io.nut.base.crypto.X509CertificateBuiler;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * A Bouncy Castle-based implementation of the {@link X509CertificateBuiler}
 * interface. This class is responsible for creating self-signed X.509 v3
 * certificates.
 */
public class X509CertificateBuilerBC implements X509CertificateBuiler
{
    
    /**
     * The signature algorithm used to sign the certificate.
     */
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /**
     * Static initializer to ensure the Bouncy Castle security provider is
     * registered. This is necessary for the cryptographic operations performed
     * by this class.
     */
    static
    {
        Kripto.registerBouncyCastle();
    }

    /**
     * Builds a self-signed X.509 v3 certificate using the provided public and
     * private keys. The certificate is valid for one year from the time of
     * creation and uses the provided alias as its Common Name (CN) for both the
     * issuer and the subject.
     *
     * @param publicKey The public key to be included in the certificate.
     * @param privateKey The private key used to sign the certificate.
     * @param dnAlias The alias to be used as the Common Name (CN) in the
     * certificate's Distinguished Name (DN).
     * @return The newly created, self-signed {@link X509Certificate}.
     * @throws Exception if any error occurs during certificate generation, such
     * as a problem with the cryptographic provider or invalid key material.
     */
    @Override
    public X509Certificate buildCertificate(PublicKey publicKey, PrivateKey privateKey, String dnAlias) throws Exception
    {

        // 1. Create the X.500 Name for the certificate's owner (issuer and subject).
        X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        nameBuilder.addRDN(BCStyle.CN, dnAlias); // Common Name
        X500Name owner = nameBuilder.build();

        // 2. Set the certificate's validity period (1 year).
        Instant now = Instant.now();
        Date notBefore = Date.from(now);
        Date notAfter = Date.from(now.plus(365, ChronoUnit.DAYS));

        // 3. Generate a random serial number for the certificate.
        BigInteger serialNumber = new BigInteger(64, new SecureRandom());

        // 4. Assemble the certificate details using JcaX509v3CertificateBuilder.
        // The issuer and subject are the same, making it a self-signed certificate.
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                owner, // Issuer
                serialNumber,
                notBefore,
                notAfter,
                owner, // Subject
                publicKey);

        // 5. Create a content signer to sign the certificate with the private key.
        ContentSigner contentSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(privateKey);

        // 6. Build the certificate and convert it to the standard Java (JCA) format.
        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certBuilder.build(contentSigner));
    }

}
