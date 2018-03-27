/*
 * Copyright (C) 2008 ZXing authors
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

package com.truiton.mobile.vision.qrcode;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;


import java.util.Collection;


public final class CaptureActivityHandler extends Handler {

    private final QRCodeScanner activity;
    private final DecodeThread decodeThread;
    private State state;
    private final CameraManager cameraManager;

    private enum State {
        PREVIEW, SUCCESS, DONE
    }

    CaptureActivityHandler(QRCodeScanner activity, Collection<BarcodeFormat> decodeFormats, String characterSet,
                           CameraManager cameraManager) {
        this.activity = activity;
        decodeThread = new DecodeThread(activity, decodeFormats, characterSet);
        decodeThread.start();
        state = State.SUCCESS;

        // Start ourselves capturing previews and decoding.
        this.cameraManager = cameraManager;
        cameraManager.startPreview();
        restartPreviewAndDecode();
    }


    @Override
    public void handleMessage(Message message) {
        if (message.what == R.id.auto_focus) {
            if (state == State.PREVIEW) {
                cameraManager.requestAutoFocus(this, R.id.auto_focus);
            }

        } else if (message.what == R.id.restart_preview) {

            restartPreviewAndDecode();

        } else if (message.what == R.id.decode_succeeded) {
            state = State.SUCCESS;
            Bundle bundle = message.getData();
            Bitmap barcode = bundle == null ? null : (Bitmap) bundle.getParcelable(DecodeThread.BARCODE_BITMAP);

            if (message.obj instanceof Result) {
                activity.handleDecode(((Result) message.obj).getText(), barcode);
            } else if (message.obj instanceof String) {
                activity.handleDecode((String) message.obj, barcode);
            }

        }
        else if (message.what == R.id.decode_failed) {
            state = State.PREVIEW;
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);

        }
        else if (message.what == R.id.return_scan_result) {
            activity.setResult(Activity.RESULT_OK, (Intent) message.obj);
            activity.finish();

        } else if (message.what == R.id.launch_product_query) {
            String url = (String) message.obj;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            activity.startActivity(intent);

        } else {
        }
    }
    
//    public void quitSynchronously() {
//        state = State.DONE;
//        cameraManager.stopPreview();
//        Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
//        quit.sendToTarget();
//        try {
//            // Wait at most half a second; should be enough time, and onPause() will timeout quickly
//            decodeThread.join(500L);
//        } catch (InterruptedException e) {
//            // continue
//        }
//
//        // Be absolutely sure we don't send any queued up messages
//        removeMessages(R.id.decode_succeeded);
//        removeMessages(R.id.decode_failed);
//    }

    private void restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
            cameraManager.requestAutoFocus(this, R.id.auto_focus);

        }
    }

}
