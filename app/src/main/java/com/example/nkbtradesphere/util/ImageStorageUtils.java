package com.example.nkbtradesphere.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Utility class for saving and loading images locally
 */
public class ImageStorageUtils {

    private static final String IMAGES_FOLDER = "tradesphere_images";
    private static final int QUALITY = 80;

    /**
     * Save a Base64 encoded image to local storage
     * @param context Android context
     * @param base64Image Base64 encoded image string
     * @return File path where image was saved, or null if failed
     */
    public static String saveBase64Image(Context context, String base64Image) {
        try {
            // Decode base64 to bitmap
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            if (decodedBitmap == null) {
                return null;
            }

            // Get images directory
            File imagesDir = getImagesDirectory(context);
            if (!imagesDir.exists()) {
                imagesDir.mkdirs();
            }

            // Create file with unique name
            String fileName = UUID.randomUUID().toString() + ".jpg";
            File imageFile = new File(imagesDir, fileName);

            // Save bitmap to file
            FileOutputStream fos = new FileOutputStream(imageFile);
            decodedBitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, fos);
            fos.close();

            return imageFile.getAbsolutePath();
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Save a bitmap to local storage
     * @param context Android context
     * @param bitmap Bitmap to save
     * @return File path where image was saved, or null if failed
     */
    public static String saveBitmap(Context context, Bitmap bitmap) {
        try {
            File imagesDir = getImagesDirectory(context);
            if (!imagesDir.exists()) {
                imagesDir.mkdirs();
            }

            String fileName = UUID.randomUUID().toString() + ".jpg";
            File imageFile = new File(imagesDir, fileName);

            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, fos);
            fos.close();

            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Load bitmap from file path
     * @param filePath Path to image file
     * @return Bitmap, or null if failed
     */
    public static Bitmap loadBitmap(String filePath) {
        try {
            return BitmapFactory.decodeFile(filePath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Delete image file
     * @param filePath Path to image file
     * @return true if deleted successfully
     */
    public static boolean deleteImage(String filePath) {
        try {
            File file = new File(filePath);
            return file.delete();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get images directory
     */
    private static File getImagesDirectory(Context context) {
        return new File(context.getFilesDir(), IMAGES_FOLDER);
    }

    /**
     * Get total images folder size
     */
    public static long getImagesFolderSize(Context context) {
        File imagesDir = getImagesDirectory(context);
        if (!imagesDir.exists()) {
            return 0;
        }

        long size = 0;
        for (File file : imagesDir.listFiles()) {
            if (file.isFile()) {
                size += file.length();
            }
        }
        return size;
    }

    /**
     * Clear all images (use carefully!)
     */
    public static void clearAllImages(Context context) {
        File imagesDir = getImagesDirectory(context);
        if (imagesDir.exists()) {
            for (File file : imagesDir.listFiles()) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        }
    }
}
