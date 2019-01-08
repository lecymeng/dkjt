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

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import com.f2prateek.dart.Dart;
import com.f2prateek.dart.InjectExtra;

import com.squareup.otto.Bus;
import com.sspai.dkjt.R;
import com.sspai.dkjt.ui.AppInfo;
import com.sspai.dkjt.model.Device;
import com.sspai.dkjt.ui.activity.MainActivity;

import javax.inject.Inject;

public abstract class AbstractGenerateFrameService extends IntentService implements DeviceFrameGenerator.Callback {
  static final int DFG_NOTIFICATION_ID = 1;
  public static final String KEY_EXTRA_DEVICE = "KEY_EXTRA_DEVICE";

  @Inject NotificationManager notificationManager;
  @Inject Bus bus;

  @InjectExtra(KEY_EXTRA_DEVICE) Device device;
  NotificationCompat.Builder notificationBuilder;

  public AbstractGenerateFrameService (String name) {
    super(name);
  }

  @Override
  public void onCreate () {
    super.onCreate();
    ((AppInfo) getApplication()).inject(this);
  }

  @Override
  protected void onHandleIntent (Intent intent) {
    Dart.inject(this, intent.getExtras());
  }

  /**
   * Notify the user of a error.
   *
   * @param title Title of the notification.
   * @param text Text of the notification.
   * @param extra Extra information to show to user.
   */
  @Override
  public void failedImage (String title, String text, String extra) {

    Notification notification = new NotificationCompat.Builder(this)
        .setTicker(title)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(new NotificationCompat.BigTextStyle()
            .setBigContentTitle(title)
            .bigText(text)
            .setSummaryText(extra))
        .setSmallIcon(R.drawable.ic_action_error)
        .setWhen(System.currentTimeMillis())
        .setAutoCancel(true)
        .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0))
        .build();
    notificationManager.notify(DFG_NOTIFICATION_ID, notification);
  }
}