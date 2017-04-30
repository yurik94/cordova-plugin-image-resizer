package info.protonet.imageresizer;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.camera.FileHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class ImageResizer extends CordovaPlugin {
    private static final int ARGUMENT_NUMBER = 1;
    public CallbackContext callbackContext;

    private String uri;
    private String folderName;
    private String fileName;
    private int quality;
    private int width;
    private int height;

    public static final String FORMAT_JPG = "jpg";
    public static final String FORMAT_PNG = "png";
    public static final String DEFAULT_FORMAT = "jpg";

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        try {
            this.callbackContext = callbackContext;

            if (action.equals("resize")) {
                checkParameters(args);

                // get the arguments
                JSONObject jsonObject = args.getJSONObject(0);
                uri = jsonObject.getString("uri");
                folderName = null;
                if (jsonObject.has("folderName")) {
                    folderName = jsonObject.getString("folderName");
                }
                fileName = null;
                if (jsonObject.has("fileName")) {
                    fileName = jsonObject.getString("fileName");
                }
                quality = jsonObject.getInt("quality");
                width = jsonObject.getInt("width");
                height = jsonObject.getInt("height");

                // load the image from uri
                Bitmap bitmap = loadScaledBitmapFromUri(uri, width, height);

                // save the image as jpeg on the device
                Uri scaledFile = saveFile(bitmap);

                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, scaledFile.toString()));
                return true;
            } else if (action.equals("size")) {
                JSONObject params = args.getJSONObject(0);
                GetImageSize imageSize = new GetImageSize(params, callbackContext);
                cordova.getThreadPool().execute(imageSize);
                return true;
            } else {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
                return false;
            }
        } catch (JSONException e) {
            Log.e("Protonet", "JSON Exception during the Image Resizer Plugin... :(");
        }
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
        return false;
    }

    /**
     * Loads a Bitmap of the given android uri path
     *
     * @params uri the URI who points to the image
     **/

    private Bitmap loadScaledBitmapFromUri(String uriString, int width, int height) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(FileHelper.getInputStreamFromUriString(uriString, cordova), null, options);

            //calc aspect ratio
            int[] retval = calculateAspectRatio(options.outWidth, options.outHeight);

            options.inJustDecodeBounds = false;
            options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, width, height);
            Bitmap unscaledBitmap = BitmapFactory.decodeStream(FileHelper.getInputStreamFromUriString(uriString, cordova), null, options);
            return Bitmap.createScaledBitmap(unscaledBitmap, retval[0], retval[1], true);
        } catch (FileNotFoundException e) {
            Log.e("Protonet", "File not found. :(");
        } catch (IOException e) {
            Log.e("Protonet", "IO Exception :(");
        } catch (Exception e) {
            Log.e("Protonet", e.toString());
        }
        return null;
    }

    private Uri saveFile(Bitmap bitmap) {
        File folder = null;
        if (folderName == null) {
            folder = new File(this.getTempDirectoryPath());
        } else {
            if (folderName.contains("/")) {
                folder = new File(folderName.replace("file://", ""));
            } else {
                folder = new File(Environment.getExternalStorageDirectory() + "/" + folderName);
            }
        }
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }

        if (success) {
            if (fileName == null) {
                fileName = System.currentTimeMillis() + ".jpg";
            }
            File file = new File(folder, fileName);
            if (file.exists()) file.delete();
            try {
                FileOutputStream out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                Log.e("Protonet", e.toString());
            }
            return Uri.fromFile(file);
        }
        return null;
    }

    /**
     * Figure out what ratio we can load our image into memory at while still being bigger than
     * our desired width and height
     *
     * @param srcWidth
     * @param srcHeight
     * @param dstWidth
     * @param dstHeight
     * @return
     */
    private int calculateSampleSize(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
        final float srcAspect = (float) srcWidth / (float) srcHeight;
        final float dstAspect = (float) dstWidth / (float) dstHeight;

        if (srcAspect > dstAspect) {
            return srcWidth / dstWidth;
        } else {
            return srcHeight / dstHeight;
        }
    }

    /**
     * Maintain the aspect ratio so the resulting image does not look smooshed
     *
     * @param origWidth
     * @param origHeight
     * @return
     */
    private int[] calculateAspectRatio(int origWidth, int origHeight) {
        int newWidth = width;
        int newHeight = height;

        // If no new width or height were specified return the original bitmap
        if (newWidth <= 0 && newHeight <= 0) {
            newWidth = origWidth;
            newHeight = origHeight;
        }
        // Only the width was specified
        else if (newWidth > 0 && newHeight <= 0) {
            newHeight = (newWidth * origHeight) / origWidth;
        }
        // only the height was specified
        else if (newWidth <= 0 && newHeight > 0) {
            newWidth = (newHeight * origWidth) / origHeight;
        }
        // If the user specified both a positive width and height
        // (potentially different aspect ratio) then the width or height is
        // scaled so that the image fits while maintaining aspect ratio.
        // Alternatively, the specified width and height could have been
        // kept and Bitmap.SCALE_TO_FIT specified when scaling, but this
        // would result in whitespace in the new image.
        else {
            double newRatio = newWidth / (double) newHeight;
            double origRatio = origWidth / (double) origHeight;

            if (origRatio > newRatio) {
                newHeight = (newWidth * origHeight) / origWidth;
            } else if (origRatio < newRatio) {
                newWidth = (newHeight * origWidth) / origHeight;
            }
        }

        int[] retval = new int[2];
        retval[0] = newWidth;
        retval[1] = newHeight;
        return retval;
    }

    private boolean checkParameters(JSONArray args) {
        if (args.length() != ARGUMENT_NUMBER) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
            return false;
        }
        return true;
    }

    private String getTempDirectoryPath() {
        File cache = null;

        // SD Card Mounted
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            cache = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/Android/data/" + cordova.getActivity().getPackageName() + "/cache/");
        } else {
            // Use internal storage
            cache = cordova.getActivity().getCacheDir();
        }

        // Create the cache directory if it doesn't exist
        cache.mkdirs();
        return cache.getAbsolutePath();
    }


    private class ImageTools {
        protected JSONObject params;
        protected CallbackContext callbackContext;
        protected String format;
        protected String imageData;
        protected String imageDataType;

        public ImageTools(JSONObject params, CallbackContext callbackContext) throws JSONException {
            this.params = params;
            this.callbackContext = callbackContext;
            imageData = params.getString("data");
            imageDataType = "urlImage";
            if (params.has("imageDataType")) {
                imageDataType = params.getString("imageDataType");
            }
            format = DEFAULT_FORMAT;
            if (params.has("format")) {
                format = params.getString("format");
            }
        }

        protected Bitmap getBitmap(String imageData, String imageDataType, BitmapFactory.Options options) throws IOException, URISyntaxException {
            Bitmap bmp;
            URI uri = new URI(imageData);
            File imageFile = new File(uri);
            bmp = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            return bmp;
        }

        protected void storeImage(JSONObject params, String format, Bitmap bmp, CallbackContext callbackContext) throws JSONException, IOException, URISyntaxException {
            int quality = params.getInt("quality");
            String filename = params.getString("filename");
            URI folderUri = new URI(params.getString("directory"));
            URI pictureUri = new URI(params.getString("directory") + "/" + filename);
            File folder = new File(folderUri);
            folder.mkdirs();
            File file = new File(pictureUri);
            OutputStream outStream = new FileOutputStream(file);
            if (format.equals(FORMAT_PNG)) {
                bmp.compress(Bitmap.CompressFormat.PNG, quality,
                        outStream);
            } else {
                bmp.compress(Bitmap.CompressFormat.JPEG, quality,
                        outStream);
            }
            outStream.flush();
            outStream.close();
            JSONObject res = new JSONObject();
            res.put("filename", filename);
            res.put("width", bmp.getWidth());
            res.put("height", bmp.getHeight());
            callbackContext.success(res);
        }
    }

    private class GetImageSize extends ImageTools implements Runnable {
        public GetImageSize(JSONObject params, CallbackContext callbackContext) throws JSONException {
            super(params, callbackContext);
        }

        @Override
        public void run() {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                Bitmap bmp = getBitmap(imageData, imageDataType, options);
                JSONObject res = new JSONObject();
                res.put("width", options.outWidth);
                res.put("height", options.outHeight);
                callbackContext.success(res);
            } catch (JSONException e) {
                callbackContext.error(e.getMessage());
            } catch (IOException e) {
                Log.d("PLUGIN", e.getMessage());
                callbackContext.error(e.getMessage());
            } catch (URISyntaxException e) {
                Log.d("PLUGIN", e.getMessage());
                callbackContext.error(e.getMessage());
            }
        }
    }
}
