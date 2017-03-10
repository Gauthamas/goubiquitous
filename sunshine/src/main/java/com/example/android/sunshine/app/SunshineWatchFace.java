/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.example.android.sunshine.app.R;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mTextPaint;
        boolean mAmbient;
        Time mTime;

        Bitmap mBackgroundClearBitmap;
        Bitmap mBackgroundCloudsBitmap;
        Bitmap mBackgroundFogBitmap;
        Bitmap mBackgroundLightCloudsBitmap;
        Bitmap mBackgroundLightRainBitmap;
        Bitmap mBackgroundRainBitmap;
        Bitmap mBackgroundSnowBitmap;
        Bitmap mBackgroundStormBitmap;


        String formatHigh = "", formatLow = "", desc = "";
        int weatherId=800;
        long watchTime;


        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        int mTapCount;

        float mXOffset;
        float mYOffset, mYOffsetDate, mYOffesetTemp;

        float mScale = 0.25f;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
            Resources resources = SunshineWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mYOffsetDate = resources.getDimension(R.dimen.digital_y_offset_2);
            mYOffesetTemp = resources.getDimension(R.dimen.digital_y_offset_3);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mTextPaint = new Paint();
            mTextPaint = createTextPaint(resources.getColor(R.color.digital_text));

            mBackgroundClearBitmap = BitmapFactory
                    .decodeResource(getResources(), R.drawable.art_clear);
            mBackgroundCloudsBitmap = BitmapFactory
                    .decodeResource(getResources(), R.drawable.art_clouds);
            mBackgroundFogBitmap = BitmapFactory
                    .decodeResource(getResources(), R.drawable.art_fog);
            mBackgroundLightCloudsBitmap = BitmapFactory
                    .decodeResource(getResources(), R.drawable.art_light_clouds);
            mBackgroundLightRainBitmap = BitmapFactory
                    .decodeResource(getResources(), R.drawable.art_light_rain);
            mBackgroundRainBitmap = BitmapFactory
                    .decodeResource(getResources(), R.drawable.art_rain);
            mBackgroundSnowBitmap = BitmapFactory
                    .decodeResource(getResources(), R.drawable.art_snow);
            mBackgroundStormBitmap = BitmapFactory
                    .decodeResource(getResources(), R.drawable.art_storm);

            mTime = new Time();

            mBackgroundClearBitmap = Bitmap.createScaledBitmap
                    (mBackgroundClearBitmap, (int) (mBackgroundClearBitmap.getWidth() * mScale),
                            (int) (mBackgroundClearBitmap.getHeight() * mScale), true);
            mBackgroundCloudsBitmap = Bitmap.createScaledBitmap
                    (mBackgroundCloudsBitmap, (int) (mBackgroundCloudsBitmap.getWidth() * mScale),
                            (int) (mBackgroundCloudsBitmap.getHeight() * mScale), true);
            mBackgroundFogBitmap = Bitmap.createScaledBitmap
                    (mBackgroundFogBitmap, (int) (mBackgroundFogBitmap.getWidth() * mScale),
                            (int) (mBackgroundFogBitmap.getHeight() * mScale), true);
            mBackgroundLightCloudsBitmap = Bitmap.createScaledBitmap
                    (mBackgroundLightCloudsBitmap, (int) (mBackgroundLightCloudsBitmap.getWidth() * mScale),
                            (int) (mBackgroundLightCloudsBitmap.getHeight() * mScale), true);
            mBackgroundLightRainBitmap = Bitmap.createScaledBitmap
                    (mBackgroundLightRainBitmap, (int) (mBackgroundLightRainBitmap.getWidth() * mScale),
                            (int) (mBackgroundLightRainBitmap.getHeight() * mScale), true);
            mBackgroundRainBitmap = Bitmap.createScaledBitmap
                    (mBackgroundRainBitmap, (int) (mBackgroundRainBitmap.getWidth() * mScale),
                            (int) (mBackgroundRainBitmap.getHeight() * mScale), true);
            mBackgroundSnowBitmap = Bitmap.createScaledBitmap
                    (mBackgroundSnowBitmap, (int) (mBackgroundSnowBitmap.getWidth() * mScale),
                            (int) (mBackgroundSnowBitmap.getHeight() * mScale), true);
            mBackgroundStormBitmap = Bitmap.createScaledBitmap
                    (mBackgroundStormBitmap, (int) (mBackgroundStormBitmap.getWidth() * mScale),
                            (int) (mBackgroundStormBitmap.getHeight() * mScale), true);

            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    formatHigh=intent.getStringExtra(ListenPhoneService.WEATHER_HIGH);
                    formatLow=intent.getStringExtra(ListenPhoneService.WEATHER_LOW);
                    desc=intent.getStringExtra(ListenPhoneService.WEATHER_DESC);
                    weatherId = intent.getIntExtra(ListenPhoneService.WEATHER_ID, 0);
                    watchTime = intent.getLongExtra(ListenPhoneService.WATCH_TIME, 0);
                }
            }, new IntentFilter(ListenPhoneService.LOCAL_DATA));
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextPaint.setTextSize(textSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = SunshineWatchFace.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background : R.color.background2));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
                if (weatherId >= 200 && weatherId <= 232) {
                    canvas.drawBitmap(mBackgroundStormBitmap, mXOffset+60, mYOffesetTemp, mBackgroundPaint);
                } else if (weatherId >= 300 && weatherId <= 321) {
                    canvas.drawBitmap(mBackgroundLightRainBitmap, mXOffset+60, mYOffesetTemp, mBackgroundPaint);
                } else if (weatherId >= 500 && weatherId <= 504) {
                    canvas.drawBitmap(mBackgroundRainBitmap, mXOffset+60, mYOffesetTemp, mBackgroundPaint);
                } else if (weatherId == 511) {
                    canvas.drawBitmap(mBackgroundSnowBitmap, mXOffset+60, mYOffesetTemp, mBackgroundPaint);
                } else if (weatherId >= 520 && weatherId <= 531) {
                    canvas.drawBitmap(mBackgroundRainBitmap, mXOffset+60, mYOffesetTemp, mBackgroundPaint);
                } else if (weatherId >= 600 && weatherId <= 622) {
                    canvas.drawBitmap(mBackgroundSnowBitmap, mXOffset+60, mYOffesetTemp, mBackgroundPaint);
                } else if (weatherId >= 701 && weatherId <= 761) {
                    canvas.drawBitmap(mBackgroundFogBitmap, mXOffset+60, mYOffesetTemp, mBackgroundPaint);
                } else if (weatherId == 761 || weatherId == 781) {
                    canvas.drawBitmap(mBackgroundStormBitmap, mXOffset+60, mYOffesetTemp, mBackgroundPaint);
                } else if (weatherId == 800) {
                    canvas.drawBitmap(mBackgroundClearBitmap, mXOffset+60, mYOffesetTemp, mBackgroundPaint);
                } else if (weatherId == 801) {
                    canvas.drawBitmap(mBackgroundLightCloudsBitmap, mXOffset+60, mYOffesetTemp, mBackgroundPaint);
                } else if (weatherId >= 802 && weatherId <= 804) {
                    canvas.drawBitmap(mBackgroundCloudsBitmap, mXOffset+60, mYOffesetTemp, mBackgroundPaint);
                }

            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();
            String text = mAmbient
                    ? String.format("%d:%02d", mTime.hour, mTime.minute)
                    : String.format("%d:%02d:%02d", mTime.hour, mTime.minute, mTime.second);
            canvas.drawText(text, mXOffset, mYOffset, mTextPaint);

            canvas.drawText(formatHigh+"  " +formatLow, mXOffset+40, mYOffsetDate, mTextPaint);


        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }



}
