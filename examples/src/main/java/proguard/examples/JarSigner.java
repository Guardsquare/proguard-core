package proguard.examples;

import proguard.io.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

/**
 * This sample application illustrates how to sign jars with the ProGuardCORE API.
 * <p>
 * Usage:
 *     java proguard.examples.JarSigner -ks <keystore> -kspass <password> -key <alias> -keypass <password> input.jar output.jar
 */
public class JarSigner
{
    private static final String KEYSTORE          = "-ks";
    private static final String KEYSTORE_PASSWORD = "-kspass";
    private static final String KEY_ALIAS         = "-key";
    private static final String KEY_PASSWORD      = "-keypass";


    private static void usage() {
        System.err.println("usage: java proguard.examples.JarSigner " +
                "-ks <keystore> -kspass <password> -key <alias> " +
                "-keypass <password> input.jar output.jar");
        System.exit(1);
    }

    private static KeyStore.PrivateKeyEntry getPrivateKeyEntry(String keyStoreFileName, String keyStorePassword, String keyAlias, String keyPassword) {
        try (FileInputStream keyStoreInputStream = new FileInputStream(keyStoreFileName)) {
            // Get the private key from the key store.
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(keyStoreInputStream, keyStorePassword.toCharArray());

            KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(keyPassword.toCharArray());

            return (KeyStore.PrivateKeyEntry)keyStore.getEntry(keyAlias, protectionParameter);
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableEntryException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args)
    {
        if (args.length != 10) {
            usage();
        }

        String keyStoreFileName = null;
        String keyStorePassword = null;
        String keyAlias         = null;
        String keyPassword      = null;

        String inputJarFileName  = null;
        String outputJarFileName = null;

        int argIndex = 0;
        while (argIndex < args.length) {
            switch (args[argIndex]) {
                case KEYSTORE:
                    keyStoreFileName = args[++argIndex];
                    break;

                case KEYSTORE_PASSWORD:
                    keyStorePassword = args[++argIndex];
                    break;

                case KEY_ALIAS:
                    keyAlias = args[++argIndex];
                    break;

                case KEY_PASSWORD:
                    keyPassword = args[++argIndex];
                    break;

                default:
                    if (inputJarFileName == null) {
                        inputJarFileName = args[argIndex];
                    } else {
                        outputJarFileName = args[argIndex];
                    }
                    break;
            }

            argIndex++;
        }

        if (keyStoreFileName  == null ||
            keyStorePassword  == null ||
            keyAlias          == null ||
            keyPassword       == null ||
            inputJarFileName  == null ||
            outputJarFileName == null) {
            usage();
        }

        try
        {
            KeyStore.PrivateKeyEntry privateKeyEntry =
                getPrivateKeyEntry(keyStoreFileName, keyStorePassword, keyAlias, keyPassword);

            // We'll write the output to a jar file.
            JarWriter jarWriter =
                new SignedJarWriter(privateKeyEntry,
                new ZipWriter(
                new FixedFileWriter(
                new File(outputJarFileName))));

            // Parse and push all classes from the input jar.
            DataEntrySource source =
                new FileSource(
                new File(inputJarFileName));

            source.pumpDataEntries(
                new JarReader(
                new DataEntryCopier(jarWriter)
                ));

            jarWriter.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
