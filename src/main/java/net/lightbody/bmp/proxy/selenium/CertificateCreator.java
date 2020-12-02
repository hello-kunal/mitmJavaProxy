package net.lightbody.bmp.proxy.selenium;

import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Methods for creating certificates.
 *
 * ***************************************************************************************
 * Copyright (c) 2007, Information Security Partners, LLC
 * All rights reserved.
 *
 * In a special exception, Selenium/OpenQA is allowed to use this code under the Apache License 2.0.
 *
 * @author Brad Hill
 *
 */
public class CertificateCreator {


	private static final HashSet<String> clientCertOidsNeverToCopy = new HashSet<String>();
	private static final HashSet<String> clientCertDefaultOidsNotToCopy = new HashSet<String>();

	/**
	 * The default key generation algorithm for this package is RSA.
	 */
	public static final String KEYGEN_ALGO = "RSA";

    /**
     * The default sign algorithm for this package is SHA1 with RSA.
     */
	public static final String SIGN_ALGO = "SHA1withRSA";


	/**
	 * X.509 OID for Subject Key Identifier Extension - Replaced when duplicating a cert.
	 */
	public static final String OID_SUBJECT_KEY_IDENTIFIER     = "2.5.29.14";

	/**
	 * X.509 OID for Subject Authority Key Identifier - Replaced when duplicating a cert.
	 */
	public static final String OID_AUTHORITY_KEY_IDENTIFIER   = "2.5.29.35";

	/**
	 * X.509 OID for Issuer Alternative Name - Omitted when duplicating a cert by default.
	 */
	public static final String OID_ISSUER_ALTERNATIVE_NAME    = "2.5.29.8";

	/**
	 * X.509 OID for Issuer Alternative Name 2 - Omitted when duplicating a cert by default.
	 */
	public static final String OID_ISSUER_ALTERNATIVE_NAME_2  = "2.5.29.18";

	/**
	 * X.509 OID for Certificate Revocation List Distribution Point - Omitted when duplicating a cert by default.
	 */
	public static final String OID_CRL_DISTRIBUTION_POINT     = "2.5.28.31";

	/**
	 * X.509 OID for Authority Information Access - Omitted when duplicating a cert by default.
	 */
	public static final String OID_AUTHORITY_INFO_ACCESS      = "1.3.6.1.5.5.7.1.1";

	/**
	 * X.509 OID for Additional CA Issuers for AIA - Omitted when duplicating a cert by default.
	 */
	public static final String OID_ID_AD_CAISSUERS            = "1.3.6.1.5.5.7.48.2";


	static
	{
		clientCertOidsNeverToCopy.add(OID_SUBJECT_KEY_IDENTIFIER);
		clientCertOidsNeverToCopy.add(OID_AUTHORITY_KEY_IDENTIFIER);

		clientCertDefaultOidsNotToCopy.add(OID_ISSUER_ALTERNATIVE_NAME);
		clientCertDefaultOidsNotToCopy.add(OID_ISSUER_ALTERNATIVE_NAME_2);
		clientCertDefaultOidsNotToCopy.add(OID_CRL_DISTRIBUTION_POINT);
		clientCertDefaultOidsNotToCopy.add(OID_AUTHORITY_INFO_ACCESS);
	}


	/**
	 * Utility method for generating a "standard" server certificate. Recognized by most
	 * browsers as valid for SSL/TLS.  These certificates are generated de novo, not from
	 * a template, so they will not retain the structure of the original certificate and may
	 * not be suitable for applications that require Extended Validation/High Assurance SSL
	 * or other distinct extensions or EKU.
	 *
	 * @param newPubKey
	 * @param caCert
	 * @param caPrivateKey
	 * @return
	 * @throws CertificateParsingException
	 * @throws SignatureException
	 * @throws InvalidKeyException
	 * @throws CertificateExpiredException
	 * @throws CertificateNotYetValidException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 */

	/**
	 * This method creates an X509v3 certificate based on an an existing certificate.
	 * It attempts to create as faithful a copy of the existing certificate as possible
	 * by duplicating all certificate extensions.
	 *
	 * If you are testing an application that makes use of additional certificate
	 * extensions (e.g. logotype, S/MIME capabilities) this method will preserve those
	 * fields.
	 *
	 * You may optionally include a set of OIDs not to copy from the original certificate.
	 * The most common reason to do this would be to remove fields that would cause inconsistency,
	 * such as Authority Info Access or Issuer Alternative Name where these are not defined for
	 * the MITM authority certificate.
	 *
	 * OIDs 2.5.29.14 : Subject Key Identifier and 2.5.29.35 : Authority Key Identifier,
	 * are never copied, but generated directly based on the input keys and certificates.
	 *
	 * You may also optionally include maps of custom extensions which will be added to or replace
	 * extensions with the same OID on the original certificate for the the MITM certificate.
	 *
	 * FUTURE WORK: JDK 1.5 is very strict in parsing extensions.  In particular, known extensions
	 * that include URIs must parse to valid URIs (including URL encoding all non-valid URI characters)
	 * or the extension will be rejected and not available to copy to the MITM certificate.  Will need
	 * to directly extract these as ASN.1 fields and re-insert (hopefully BouncyCastle will handle them)
	 *
	 *
	 * @param originalCert  The original certificate to duplicate.
	 * @param newPubKey     The new public key for the MITM certificate.
	 * @param caCert        The certificate of the signing authority fot the MITM certificate.
	 * @param caPrivateKey  The private key of the signing authority.
	 * @param extensionOidsNotToCopy  An optional list of certificate extension OIDs not to copy to the MITM certificate.
	 * @return The new MITM certificate.
	 * @throws CertificateParsingException
	 * @throws SignatureException
	 * @throws InvalidKeyException
	 * @throws CertificateExpiredException
	 * @throws CertificateNotYetValidException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 */
	public static X509Certificate mitmDuplicateCertificate(final X509Certificate originalCert,
			final PublicKey newPubKey,
			final X509Certificate caCert,
			final PrivateKey caPrivateKey,
			Set<String> extensionOidsNotToCopy)
	throws 	CertificateParsingException,
			SignatureException,
			InvalidKeyException,
			CertificateException,
			NoSuchAlgorithmException,
			NoSuchProviderException
	{
		if(extensionOidsNotToCopy == null)
		{
			extensionOidsNotToCopy = new HashSet<String>();
		}

		X509V3CertificateGenerator  v3CertGen = new X509V3CertificateGenerator();

		v3CertGen.setSubjectDN(originalCert.getSubjectX500Principal());
		v3CertGen.setSignatureAlgorithm(CertificateCreator.SIGN_ALGO); // needs to be the same as the signing cert, not the copied cert
		v3CertGen.setPublicKey(newPubKey);
		v3CertGen.setNotAfter(originalCert.getNotAfter());
		v3CertGen.setNotBefore(originalCert.getNotBefore());
		v3CertGen.setIssuerDN(caCert.getSubjectX500Principal());
		v3CertGen.setSerialNumber(originalCert.getSerialNumber());

		// copy other extensions:
		Set<String> critExts = originalCert.getCriticalExtensionOIDs();

		// get extensions returns null, not an empty set!
		if(critExts != null) {
			throw new RuntimeException("Ups has critical extensions..."); //TODO
		}
		Set<String> nonCritExs = originalCert.getNonCriticalExtensionOIDs();

		if(nonCritExs != null) {
			throw new RuntimeException("Ups has non-critical extensions..."); //TODO
		}

		/* TODO
		v3CertGen.addExtension(
				X509Extensions.SubjectKeyIdentifier,
				false,
				new SubjectKeyIdentifierStructure(newPubKey));
*/

		v3CertGen.addExtension(
				X509Extensions.AuthorityKeyIdentifier,
				false,
				new AuthorityKeyIdentifierStructure(caCert.getPublicKey()));

		X509Certificate cert = v3CertGen.generate(caPrivateKey, "BC");

		// For debugging purposes.
		//cert.checkValidity(new Date());
		//cert.verify(caCert.getPublicKey());

		return cert;
	}

	/**
	 * Convenience method for the most common case of certificate duplication.
	 *
	 *  This method will not add any custom extensions and won't copy the extensions 2.5.29.8 : Issuer Alternative Name,
	 * 	2.5.29.18 : Issuer Alternative Name 2, 2.5.29.31 : CRL Distribution Point or 1.3.6.1.5.5.7.1.1 : Authority Info Access, if they are present.
	 *
	 * @param originalCert
	 * @param newPubKey
	 * @param caCert
	 * @param caPrivateKey
	 * @return
	 * @throws CertificateParsingException
	 * @throws SignatureException
	 * @throws InvalidKeyException
	 * @throws CertificateExpiredException
	 * @throws CertificateNotYetValidException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 */
	public static X509Certificate mitmDuplicateCertificate(final X509Certificate originalCert,
			final PublicKey newPubKey,
			final X509Certificate caCert,
			final PrivateKey caPrivateKey)
	throws CertificateParsingException, SignatureException, InvalidKeyException, CertificateExpiredException, CertificateNotYetValidException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException
	{
		return mitmDuplicateCertificate(originalCert, newPubKey, caCert, caPrivateKey, clientCertDefaultOidsNotToCopy);
	}

	public static X509Certificate generateStdSSLServerCertificate(
			final PublicKey newPubKey,
			final X509Certificate caCert,
			final PrivateKey caPrivateKey,
			final String subject)
			throws 	CertificateParsingException,
			SignatureException,
			InvalidKeyException,
			CertificateExpiredException,
			CertificateNotYetValidException,
			CertificateException,
			NoSuchAlgorithmException,
			NoSuchProviderException
	{
		X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();

		v3CertGen.setSubjectDN(new X500Principal(subject));
		v3CertGen.setSignatureAlgorithm(CertificateCreator.SIGN_ALGO);
		v3CertGen.setPublicKey(newPubKey);
		v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + 30L * 60 * 60 * 24 * 30 * 12));
		v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30 *12));
		v3CertGen.setIssuerDN(caCert.getSubjectX500Principal());

		// Firefox actually tracks serial numbers within a CA and refuses to validate if it sees duplicates
		// This is not a secure serial number generator, (duh!) but it's good enough for our purposes.
		v3CertGen.setSerialNumber(new BigInteger(Long.toString(System.currentTimeMillis())));

		v3CertGen.addExtension(
				X509Extensions.BasicConstraints,
				true,
				new BasicConstraints(false) );

		//TODO
//		v3CertGen.addExtension(
//				X509Extensions.SubjectKeyIdentifier,
//				false,
//				new SubjectKeyIdentifierStructure(newPubKey));


		v3CertGen.addExtension(
				X509Extensions.AuthorityKeyIdentifier,
				false,
				new AuthorityKeyIdentifierStructure(caCert.getPublicKey()));

// 		Firefox 2 disallows these extensions in an SSL server cert.  IE7 doesn't care.
//		v3CertGen.addExtension(
//				X509Extensions.KeyUsage,
//				false,
//				new KeyUsage(KeyUsage.dataEncipherment | KeyUsage.digitalSignature ) );


		//TODO
//		DEREncodableVector typicalSSLServerExtendedKeyUsages = new DEREncodableVector();

//		typicalSSLServerExtendedKeyUsages.add(new DERObjectIdentifier(ExtendedKeyUsageConstants.serverAuth));
//		typicalSSLServerExtendedKeyUsages.add(new DERObjectIdentifier(ExtendedKeyUsageConstants.clientAuth));
//		typicalSSLServerExtendedKeyUsages.add(new DERObjectIdentifier(ExtendedKeyUsageConstants.netscapeServerGatedCrypto));
//		typicalSSLServerExtendedKeyUsages.add(new DERObjectIdentifier(ExtendedKeyUsageConstants.msServerGatedCrypto));

//		v3CertGen.addExtension(
//				X509Extensions.ExtendedKeyUsage,
//				false,
//				new DERSequence(typicalSSLServerExtendedKeyUsages));

//  Disabled by default.  Left in comments in case this is desired.
//
//		v3CertGen.addExtension(
//				X509Extensions.AuthorityInfoAccess,
//				false,
//				new AuthorityInformationAccess(new DERObjectIdentifier(OID_ID_AD_CAISSUERS),
//						new GeneralName(GeneralName.uniformResourceIdentifier, "http://" + subject + "/aia")));

//		v3CertGen.addExtension(
//				X509Extensions.CRLDistributionPoints,
//				false,
//				new CRLDistPoint(new DistributionPoint[] {}));



		X509Certificate cert = v3CertGen.generate(caPrivateKey, "BC");

		return cert;
	}

}
