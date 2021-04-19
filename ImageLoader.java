package com.coderzf1.colordropper;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class ImageLoader {
    public interface ImageLoaderListener{
        void onImageSaved(String filename, Bitmap bmp);
        void onImageLoaded(Bitmap bmp);
        void onError(String error);

    }
    Bitmap loadedImage = null;
    int error = 0;
    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0){
                listener.onImageLoaded(loadedImage);
            } else if (msg.what == 1){
                listener.onError("Error downloading Image");
            }

        }
    };
    Handler handler2 = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg){
            if (msg.what==0){
                Bundle bundle = msg.getData();
                String filename = bundle.getString("filename");
                listener.onImageSaved(filename ,loadedImage);
            }else if(msg.what==1){
                listener.onError("Error downloading Image");
            }
        }
    };
    private ImageLoaderListener listener;

    public ImageLoader(){
        listener = null;
    }

    public void setImageLoaderListener(ImageLoaderListener listener){
        this.listener = listener;
    }

    private void saveImageToFile(Context context, Bitmap bmp, String filename){
        try {
            saveImage(context, bmp, getApplicationName(context), filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

    //The following code was found online+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    //Slightly modified by me
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    private Uri saveImage(Context context, Bitmap bitmap, @NonNull String folderName, @NonNull String fileName) throws IOException {
        OutputStream fos = null;
        File imageFile = null;
        Uri imageUri = null;
        Bitmap.CompressFormat format = null;

        String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
        String mimeType =  MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();

                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);

                contentValues.put(MediaStore.MediaColumns.MIME_TYPE,mimeType) ;
                contentValues.put(
                        MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + folderName);
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

                if (imageUri == null)
                    throw new IOException("Failed to create new MediaStore record.");

                fos = resolver.openOutputStream(imageUri);
            } else {
                File imagesDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString() + File.separator + folderName);

                String compressionName = mimeType.toLowerCase().replace("image/", "").toUpperCase();
                Log.println(Log.DEBUG,"compressFormat: ", compressionName);
                //try to determine compression format from original file
                try {
                    format = Bitmap.CompressFormat.valueOf(compressionName);
                }catch (Exception e){
                    format = Bitmap.CompressFormat.PNG;
                    extension = "png";
                }

                if (!imagesDir.exists())
                    imagesDir.mkdir();

                imageFile = new File(imagesDir, fileName + "." + extension);
                fos = new FileOutputStream(imageFile);
            }

            if (!bitmap.compress(format, 100, fos))
                throw new IOException("Failed to save bitmap.");
            fos.flush();
        } finally {
            if (fos != null)
                fos.close();
        }

        if (imageFile != null) {//pre Q
            MediaScannerConnection.scanFile(context, new String[]{imageFile.toString()}, null, null);
            imageUri = Uri.fromFile(imageFile);
        }
        return imageUri;
    }
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    public void downloadAndSaveImageFromUrl(Context context, String url){
        Uri uri = Uri.parse(url);
        final String filename = uri.getLastPathSegment();
        new Thread(() -> {
            Log.println(Log.DEBUG, "Start","Start Download from '" + url + "'");
            int counter = 0;
            Bitmap bmp = null;
            Message msg = new Message();
            try {
                Log.println(Log.DEBUG,"Download","Attempting to download image");
                bmp = BitmapFactory.decodeStream((InputStream) new URL(url).getContent());


                msg.what = 0;
                Bundle bundle = new Bundle();
                bundle.putString("filename",filename);
                msg.setData(bundle);
            } catch (IOException e) {
                e.printStackTrace();
                //bmp = BitmapFactory.decodeResource(activity.getResources(),R.drawable.imgnotfound);
                msg.what = 1;
                Log.println(Log.ERROR,"ERROR:","Image not found. Setting to Default image.");
            }
            while (counter < 10){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                counter +=1;
                if (bmp != null || error ==1){
                    break;
                }
            }
            Log.println(Log.DEBUG,"DEBUG","Calling Listener");
            loadedImage = bmp;
            if (loadedImage != null){
                saveImageToFile(context,loadedImage,filename);
            }
            handler2.sendMessage(msg);
        }).start();
    }

    public void loadImageFromUrl(String url){
        new Thread(() -> {
            Log.println(Log.DEBUG, "Start","Start Download from '" + url + "'");
            int counter = 0;
            Bitmap bmp = null;
            try {
                Log.println(Log.DEBUG,"Download","Attempting to download image");
                bmp = BitmapFactory.decodeStream((InputStream) new URL(url).getContent());
                 error = 0;
            } catch (IOException e) {
                e.printStackTrace();
                //bmp = BitmapFactory.decodeResource(activity.getResources(),R.drawable.imgnotfound);
                 error = 1;
                Log.println(Log.ERROR,"ERROR:","Image not found. Setting to Default image.");
            }
            while (counter < 10){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                counter +=1;
                if (bmp != null || error ==1){
                    break;
                }
            }
            Log.println(Log.DEBUG,"DEBUG","Calling Listener");
            loadedImage = bmp;

            handler.sendEmptyMessage(error);
        }).start();
    }
}

