/*
 * Copyright 2014 Prateek Srivastava (@f2prateek)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sspai.dkjt.service;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.sspai.dkjt.prefs.ConfigString;
import com.sspai.dkjt.R;
import com.sspai.dkjt.model.Bounds;
import com.sspai.dkjt.model.Device;
import com.sspai.dkjt.model.Orientation;
import com.sspai.dkjt.model.Utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DeviceFrameGenerator {

  private final Context context;
  private final Callback callback;
  private final Device device;
  private final boolean withShadow;
  private final boolean withGlare;

  public DeviceFrameGenerator (Context context, Callback callback, Device device, boolean withShadow,
      boolean withGlare) {
    this.context = context;
    this.callback = callback;
    this.device = device;
    this.withShadow = withShadow;
    this.withGlare = withGlare;
  }

  /**
   * Generate the frame.
   *
   * @param screenshotUri Uri to the screenshot file.
   */
  public void generateFrame (Uri screenshotUri) {

    if (screenshotUri == null) {
      Resources r = context.getResources();
      callback.failedImage(r.getString(R.string.failed_open_screenshot_title), r.getString(R.string.no_image_received),
          null);
      return;
    }

    try {
      Bitmap screenshot = Utils.decodeUri(context.getContentResolver(), screenshotUri);
      if (screenshot != null) {
        generateFrame(screenshot);
      } else {
        failedToOpenScreenshot(screenshotUri);
      }
    } catch (IOException e) {
      failedToOpenScreenshot(screenshotUri);
    }
  }

  private void failedToOpenScreenshot (Uri screenshotUri) {
    Resources r = context.getResources();
    callback.failedImage(r.getString(R.string.failed_open_screenshot_title),
        r.getString(R.string.failed_open_screenshot_text, screenshotUri.toString()), null);
  }

  /**
   * Generate the frame.
   *
   * @param screenshot non-null screenshot to use.
   */
  void generateFrame (Bitmap screenshot) {
    callback.startingImage(screenshot);
    Orientation orientation;
    orientation = Orientation.calculate(screenshot, device);
    if (orientation == null || orientation == Orientation.LANDSCAPE) {
      Resources r = context.getResources();
      callback.failedImage(r.getString(R.string.failed_match_dimensions_title),
          r.getString(R.string.failed_match_dimensions_text, device.portSize().x(), device.portSize().y(),
              screenshot.getHeight(), screenshot.getWidth()), r.getString(R.string.device_chosen, device.name()));
      return;
    }

    final Bitmap background =
        Utils.decodeResource(context, device.getBackgroundStringResourceName(orientation.getId()));
    final Bitmap glare = Utils.decodeResource(context, device.getGlareStringResourceName(orientation.getId()));
    final Bitmap shadow = Utils.decodeResource(context, device.getShadowStringResourceName(orientation.getId()));

    Canvas frame;
    if (withShadow) {
      frame = new Canvas(shadow);
      frame.drawBitmap(background, 0f, 0f, null);
    } else {
      frame = new Canvas(background);
    }

    final Bounds offset;

    if (orientation == Orientation.PORTRAIT) {
      screenshot = Bitmap.createScaledBitmap(screenshot, device.portSize().x(), device.portSize().y(), false);
      offset = device.portOffset();
      frame.drawBitmap(screenshot, offset.x(), offset.y(), null);
    }

    if (withGlare) {
      frame.drawBitmap(glare, 0f, 0f, null);
    }

    ImageMetadata imageMetadata = prepareMetadata();
    // Save the screenshot to the MediaStore
    ContentValues values = new ContentValues();
    ContentResolver resolver = context.getContentResolver();
    values.put(MediaStore.Images.ImageColumns.DATA, imageMetadata.imageFilePath);
    values.put(MediaStore.Images.ImageColumns.TITLE, imageMetadata.imageFileName);
    values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, imageMetadata.imageFileName);
    values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, imageMetadata.imageTime);
    values.put(MediaStore.Images.ImageColumns.DATE_ADDED, imageMetadata.imageTime);
    values.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, imageMetadata.imageTime);
    values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/png");
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      values.put(MediaStore.Images.ImageColumns.WIDTH, background.getWidth());
      values.put(MediaStore.Images.ImageColumns.HEIGHT, background.getHeight());
    }
    Uri frameUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

    try {
      if (frameUri == null || TextUtils.getTrimmedLength(frame.toString()) == 0) {
        throw new IOException("Content Resolved could not save image");
      }
      OutputStream out = resolver.openOutputStream(frameUri);
      if (withShadow) {
        shadow.compress(Bitmap.CompressFormat.PNG, 100, out);
      } else {
        background.compress(Bitmap.CompressFormat.PNG, 100, out);
      }
      out.flush();
      out.close();
    } catch (IOException e) {
      Resources r = context.getResources();
      callback.failedImage(r.getString(R.string.unknown_error_title), r.getString(R.string.unknown_error_text), null);
      return;
    } finally {
      screenshot.recycle();
      background.recycle();
      glare.recycle();
      shadow.recycle();
    }

    // update file size in the database
    values.clear();
    values.put(MediaStore.Images.ImageColumns.SIZE, new File(imageMetadata.imageFilePath).length());
    resolver.update(frameUri, values, null, null);

    callback.doneImage(frameUri);
  }

  /**
   * Prepare the metadata for our image.
   *
   * @return {@link ImageMetadata} that will be used for the image.
   */
  ImageMetadata prepareMetadata () {
    ImageMetadata imageMetadata = new ImageMetadata();
    imageMetadata.imageTime = System.currentTimeMillis();
    String imageDate = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date(imageMetadata.imageTime));
    String imageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
    File dfgDir = new File(imageDir, ConfigString.DFG_DIR_NAME);
    dfgDir.mkdirs();
    imageMetadata.imageFileName = String.format(ConfigString.DFG_FILE_NAME_TEMPLATE, imageDate);
    imageMetadata.imageFilePath = new File(dfgDir, imageMetadata.imageFileName).getAbsolutePath();
    return imageMetadata;
  }

  // Views should have these methods to notify the user.
  public interface Callback {
    void startingImage (Bitmap screenshot);

    void failedImage (String title, String text, String extra);

    void doneImage (Uri imageUri);
  }

  public class ImageMetadata {
    String imageFileName;
    String imageFilePath;
    long imageTime;
  }
}