# ImageLoaderJavaClass
Class that can be used to download an image and display into an ImageView or to save to a file.

There are 2 public functions and a Listener for callbacks

The first function downloadAndSaveFromUrl takes 2 parameters: a context and a url.
The second function loadImageFromUrl takes 1 parameter: a url.

Implement the listener before calling either of these 2 functions.
Example:

      ImageLoader loader = new Loader();
      loader.setImageLoaderListener(new ImageLoader.ImageLoaderListener() {
          @Override
          public void onImageSaved(String filename, Bitmap bmp) {
              Toast.makeText(getContext(),"File Saved:" + filename,Toast.LENGTH_LONG).show();
          }

          @Override
          public void onImageLoaded(Bitmap bmp) {

          }

          @Override
          public void onError(String error) {

          }
      });
      loader.loadImageFromUrl(url);
      loader.downloadAndSaveFromUrl(getContext(), url);
     
The downloadAndSaveFromUrl will attempt to determine what the filetype is and automatically save using the appropiate compression format, otherwise, it will save the image as a PNG file.

ImageLoader.ImageLoaderListener is used for callbacks to notify the app that the download process has been completed. onImageSaved is fired when the Image has been successfully saved to the device storage. onImageLoaded is fired when the image is loaded into virtual memory into a bitmap object, which can then be set to be displayed in an ImageView or View of your choice. onError occurs if there was an error of any kind.
