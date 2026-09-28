package engine;

import utils.AESEncryption;
import java.awt.image.BufferedImage;
import java.util.List;

public class ImageSteganography {

    // -------------------------------------------------------
    // convertToBits()
    // Converts encrypted message string into a binary string
    // Stores 32-bit length header first then 8-bit characters
    // -------------------------------------------------------
    private static String convertToBits(String encryptedMessage) {
        StringBuilder allBits = new StringBuilder();

        // Store message length as a 32-bit binary number
        // So decoder knows exactly how many bits to read
        allBits.append(
                String.format("%32s",
                        Integer.toBinaryString(encryptedMessage.length())
                ).replace(' ', '0')
        );

        // Store each character as an 8-bit binary number
        for (char c : encryptedMessage.toCharArray()) {
            allBits.append(
                    String.format("%8s",
                            Integer.toBinaryString(c)
                    ).replace(' ', '0')
            );
        }

        return allBits.toString();
    }

    // -------------------------------------------------------
    // extractBitsFromPixel()
    // FIXED: extracted repeated 8-line pixel reading code
    // into single helper — fixes duplicate warnings :181, :227
    //
    // Reads LSB from each R, G, B channel of a pixel
    // and appends bits to the provided StringBuilder
    // Returns how many bits were actually appended
    // -------------------------------------------------------
    private static int extractBitsFromPixel(
            int rgb,
            StringBuilder bits,
            int bitsStillNeeded) {

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8)  & 0xFF;
        int b =  rgb        & 0xFF;

        int added = 0;

        if (bitsStillNeeded > 0) { bits.append(r & 1); added++; bitsStillNeeded--; }
        if (bitsStillNeeded > 0) { bits.append(g & 1); added++; bitsStillNeeded--; }
        if (bitsStillNeeded > 0) { bits.append(b & 1); added++;                    }

        return added;
    }

    // -------------------------------------------------------
    // hidePixelBits()
    // FIXED: extracted repeated 13-line pixel writing code
    // into single helper — fixes duplicate warning :101
    //
    // Hides up to 3 bits into the R, G, B channels of a pixel
    // Returns modified RGB int value
    // -------------------------------------------------------
    private static int hidePixelBits(
            int rgb,
            String allBits,
            int bitIndex) {

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8)  & 0xFF;
        int b =  rgb        & 0xFF;

        int len = allBits.length();

        if (bitIndex     < len) r = (r & 0xFE) | (allBits.charAt(bitIndex)     - '0');
        if (bitIndex + 1 < len) g = (g & 0xFE) | (allBits.charAt(bitIndex + 1) - '0');
        if (bitIndex + 2 < len) b = (b & 0xFE) | (allBits.charAt(bitIndex + 2) - '0');

        return (r << 16) | (g << 8) | b;
    }

    // -------------------------------------------------------
    // readLengthBits()
    // FIXED: extracted from decode() to fix
    // "extract method returning allBits" warning :54
    //
    // Reads the first 32 bits from scattered pixel positions
    // Returns the binary string representing message length
    // -------------------------------------------------------
    private static String readLengthBits(
            BufferedImage image,
            List<Integer> positions,
            int width) {

        StringBuilder lengthBits = new StringBuilder();
        int bitsNeeded = 32;

        // FIXED: enhanced for-each loop — fixes warning :178
        for (int pixelIndex : positions) {
            if (lengthBits.length() >= 32) break;
            int x = pixelIndex % width;
            int y = pixelIndex / width;
            int rgb = image.getRGB(x, y);
            // FIXED: no always-true condition — bitsNeeded tracks remaining
            int added = extractBitsFromPixel(rgb, lengthBits, bitsNeeded);
            bitsNeeded -= added;
        }

        return lengthBits.toString();
    }

    // -------------------------------------------------------
    // ENCODE METHOD
    // Hides an encrypted message inside an image
    // -------------------------------------------------------
    public static BufferedImage encode(
            BufferedImage originalImage,
            String message,
            String password) throws Exception {

        int width       = originalImage.getWidth();
        int height      = originalImage.getHeight();
        int totalPixels = width * height;

        // Encrypt message using AES-256
        String encryptedMessage = AESEncryption.encrypt(message, password);

        // Check if the image is large enough
        if (!PixelScatter.isCapacityEnough(totalPixels, encryptedMessage)) {
            throw new Exception(
                    "Image is too small to hide this message!\n"
                            + "Please use a larger image or a shorter message."
            );
        }

        // Convert encrypted message to binary string
        String allBits      = convertToBits(encryptedMessage);
        int pixelsNeeded    = (int) Math.ceil(allBits.length() / 3.0);

        // Get scattered pixel positions using password as seed
        List<Integer> positions = PixelScatter.generateScatteredPositions(
                totalPixels, password, pixelsNeeded
        );

        // Copy original image — never modify the original
        BufferedImage encodedImage = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_RGB
        );

        // FIXED: enhanced for-each loop — fixes warning :95
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                encodedImage.setRGB(x, y, originalImage.getRGB(x, y));
            }
        }

        // Hide bits at scattered pixel positions
        int bitIndex = 0;

        // FIXED: enhanced for-each loop — fixes warning :95
        for (int pixelIndex : positions) {
            if (bitIndex >= allBits.length()) break;

            int x   = pixelIndex % width;
            int y   = pixelIndex / width;
            int rgb = encodedImage.getRGB(x, y);

            // FIXED: uses hidePixelBits() helper
            // fixes duplicated code fragment warning :101
            int newRgb = hidePixelBits(rgb, allBits, bitIndex);
            encodedImage.setRGB(x, y, newRgb);

            // Move forward by up to 3 bits per pixel
            bitIndex += Math.min(3, allBits.length() - bitIndex);
        }

        return encodedImage;
    }

    // -------------------------------------------------------
    // DECODE METHOD
    // Extracts and decrypts a hidden message from an image
    // -------------------------------------------------------
    public static String decode(
            BufferedImage encodedImage,
            String password) throws Exception {

        int width       = encodedImage.getWidth();
        int height      = encodedImage.getHeight();
        int totalPixels = width * height;

        // Step 1: Read the first 32 bits to get message length
        int lengthPixels = (int) Math.ceil(32.0 / 3.0);
        List<Integer> lengthPositions = PixelScatter.generateScatteredPositions(
                totalPixels, password, lengthPixels
        );

        // FIXED: extracted into readLengthBits() helper
        // fixes "extract method returning allBits" warning :54
        String lengthBinary = readLengthBits(
                encodedImage, lengthPositions, width
        );

        // Convert 32-bit binary string to integer
        int messageLength = Integer.parseInt(lengthBinary, 2);

        // Validate the message length
        if (messageLength <= 0 || messageLength > 100000) {
            throw new Exception(
                    "No hidden message found!\n"
                            + "Wrong password or image has no hidden data."
            );
        }

        // Step 2: Read all message bits
        int totalBits    = 32 + (messageLength * 8);
        int pixelsNeeded = (int) Math.ceil(totalBits / 3.0);

        List<Integer> allPositions = PixelScatter.generateScatteredPositions(
                totalPixels, password, pixelsNeeded
        );

        // FIXED: bitsNeeded tracks remaining count
        // fixes "condition always true" warnings :191, :237
        StringBuilder allBits   = new StringBuilder();
        int bitsNeeded          = totalBits;

        // FIXED: enhanced for-each loop — fixes warning :224
        for (int pixelIndex : allPositions) {
            if (bitsNeeded <= 0) break;

            int x   = pixelIndex % width;
            int y   = pixelIndex / width;
            int rgb = encodedImage.getRGB(x, y);

            // FIXED: uses extractBitsFromPixel() helper
            // fixes duplicated code fragment warnings :181, :227
            int added = extractBitsFromPixel(rgb, allBits, bitsNeeded);
            bitsNeeded -= added;
        }

        // Step 3: Skip first 32 bits and convert to characters
        StringBuilder extractedMessage = new StringBuilder();

        for (int i = 0; i < messageLength; i++) {
            int start    = 32 + (i * 8);
            int end      = start + 8;
            String charBits  = allBits.substring(start, end);
            int charValue    = Integer.parseInt(charBits, 2);
            extractedMessage.append((char) charValue);
        }

        // Step 4: Decrypt and return the original message
        return AESEncryption.decrypt(extractedMessage.toString(), password);
    }
}
