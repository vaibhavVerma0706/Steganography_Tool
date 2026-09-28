// This file is inside the "engine" folder/package
package engine;

// java.util.ArrayList = a resizable list to store pixel positions
import java.util.ArrayList;

// java.util.Collections = used to shuffle the list randomly
import java.util.Collections;

// java.util.List = standard list interface
import java.util.List;

// java.util.Random = generates random numbers from a seed
// Same seed = same random numbers every time (predictable randomness)
import java.util.Random;

public class PixelScatter {

    // -------------------------------------------------------
    // generateScatteredPositions()
    //
    // This method creates a shuffled list of pixel positions
    // based on the password (used as a seed for randomness)
    //
    // Input:
    //   totalPixels → total number of pixels in the image
    //                 (width × height)
    //   password    → used as seed so same password gives
    //                 same positions every time
    //   bitsNeeded  → how many pixel positions we need
    //                 (each pixel holds 3 bits — R, G, B)
    //
    // Output:
    //   A list of pixel index positions in random order
    // -------------------------------------------------------
    public static List<Integer> generateScatteredPositions(
            int totalPixels,
            String password,
            int bitsNeeded) {

        // Step 1: Convert password to a numeric seed
        // hashCode() converts any string to a unique number
        // Example: "my password" → 1784913424
        long seed = password.hashCode();

        // Step 2: Create a Random object with our seed
        // Same seed = same sequence of random numbers always
        Random random = new Random(seed);

        // Step 3: Create a list of ALL pixel positions
        // Example: if image has 1000 pixels
        // list = [0, 1, 2, 3, 4, ... 999]
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < totalPixels; i++) {
            positions.add(i);
        }

        // Step 4: Shuffle the list using our seeded random
        // Example: list becomes [482, 7, 293, 841, 156, ...]
        // Same password = same shuffle every time ✅
        Collections.shuffle(positions, random);

        // Step 5: Return only as many positions as we need
        // We don't need all pixels — just enough for our message
        return positions.subList(0, bitsNeeded);
    }

    // -------------------------------------------------------
    // calculateBitsNeeded()
    //
    // Calculates how many pixel positions we need to hide
    // the full message inside the image
    //
    // How it works:
    //   Each character = 8 bits
    //   Each pixel has 3 channels (R, G, B)
    //   We hide 1 bit per channel = 3 bits per pixel
    //   We also add 32 bits at start to store message length
    //
    // Input:  message text
    // Output: number of pixel positions needed
    // -------------------------------------------------------
    public static int calculateBitsNeeded(String message) {

        // Each character needs 8 bits
        // Plus 32 bits at the beginning to store message length
        // (so we know when to stop reading on decode)
        int totalBits = (message.length() * 8) + 32;

        // Each pixel can hold 3 bits (one per R, G, B channel)
        // We divide by 3 and round up to get pixel count needed
        return (int) Math.ceil(totalBits / 3.0);
    }

    // -------------------------------------------------------
    // isCapacityEnough()
    //
    // Checks if the image is big enough to hold the message
    //
    // Input:
    //   totalPixels → image width × height
    //   message     → the text you want to hide
    //
    // Output:
    //   true  → image is big enough ✅
    //   false → image is too small ❌
    // -------------------------------------------------------
    public static boolean isCapacityEnough(int totalPixels, String message) {

        // Get how many pixels we need
        int pixelsNeeded = calculateBitsNeeded(message);

        // Compare with total pixels available
        // Return true if we have enough pixels
        return totalPixels >= pixelsNeeded;
    }
}

