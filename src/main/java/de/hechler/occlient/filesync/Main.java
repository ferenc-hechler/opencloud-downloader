package de.hechler.occlient.filesync;

/**
 * Main dispatcher class that delegates to the appropriate sync main class
 * based on the first argument.
 *
 * Usage: java -jar oc-sync.jar <mode> [config-yaml]
 *   mode: upload | download | dbf-upload | dbf-download
 */
public class Main {

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            printUsageAndExit();
        }

        String mode = args[0];
        String[] remainingArgs = new String[args.length - 1];
        System.arraycopy(args, 1, remainingArgs, 0, remainingArgs.length);

        switch (mode) {
            case "upload":
                UploaderMain.main(remainingArgs);
                break;
            case "download":
                DownloaderMain.main(remainingArgs);
                break;
            case "dbf-upload":
                DBFUploaderMain.main(remainingArgs);
                break;
            case "dbf-download":
                DBFDownloaderMain.main(remainingArgs);
                break;
            default:
                System.err.println("Unknown mode: " + mode);
                printUsageAndExit();
        }
    }

    private static void printUsageAndExit() {
        System.err.println("Usage: java -jar oc-sync.jar <mode> [config-yaml]");
        System.err.println("  mode:");
        System.err.println("    upload       - Upload local files to OpenCloud");
        System.err.println("    download     - Download files from OpenCloud to local");
        System.err.println("    dbf-upload   - Upload DBF files to OpenCloud (with encryption support)");
        System.err.println("    dbf-download - Download DBF files from OpenCloud (with decryption support)");
        System.exit(1);
    }
}
