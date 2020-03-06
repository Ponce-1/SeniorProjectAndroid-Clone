package com.parkking491prototype.parkking;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;


public class PinchZoomPan extends View {

    private Bitmap mBitmap;
    private int mImageWidth;
    private int mImageHeight;

    private float mPositionX;
    private float mPositionY;
    private float mLastTouchX;
    private float mLastTouchY;

    private static final int INVALID_POINTER_ID = -1;
    private int mActivePointerID = INVALID_POINTER_ID;

    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.0f;
    private final static float mMinZoom = 1.0f;
    private final static float mMaxZoom = 5.0f;

    private Paint paint;

    private ParkingStatus parkingStatus;


    private int bitmapActualWidth =0;
    private int bitmapActualHeight = 0;

    public PinchZoomPan(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());



        //dot paint color
        paint = new Paint();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        //the scale gesture detector should inspect all the touch events
        mScaleDetector.onTouchEvent(event);

        final int action = event.getAction();

        switch (action & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN: {

                //get x and y cords of where we touch the screen
                final float x = event.getX();
                final float y = event.getY();

                //remember where touch event started
                mLastTouchX = x;
                mLastTouchY = y;

                //save the ID of this pointer
                mActivePointerID = event.getPointerId(0);

                break;
            }
            case MotionEvent.ACTION_MOVE: {

                //find the index of the active pointer and fetch its position
                final int pointerIndex = event.findPointerIndex(mActivePointerID);
                final float x = event.getX(pointerIndex);
                final float y = event.getY(pointerIndex);

                if (!mScaleDetector.isInProgress()) {

                    //calculate the distance in x and y directions
                    final float distanceX = x - mLastTouchX;
                    final float distanceY = y - mLastTouchY;

                    mPositionX += distanceX;
                    mPositionY += distanceY;

                    //redraw canvas call onDraw method
                    invalidate();

                }
                //remember this touch position for next move event
                mLastTouchX = x;
                mLastTouchY = y;


                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerID = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerID = INVALID_POINTER_ID;
                break;

            }

            case MotionEvent.ACTION_POINTER_UP: {
                //Extract the index of the pointer that left the screen
                final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == mActivePointerID) {
                    //Our active pointer is going up Choose another active pointer and adjust
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = event.getX(newPointerIndex);
                    mLastTouchY = event.getY(newPointerIndex);
                    mActivePointerID = event.getPointerId(newPointerIndex);
                }
                break;
            }
        }

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBitmap != null) {
            canvas.save();

            if ((mPositionX * -1) < 0) {
                mPositionX = 0;
            } else if ((mPositionX * -1) > mImageWidth * mScaleFactor - getWidth()) {
                mPositionX = (mImageWidth * mScaleFactor - getWidth()) * -1;
            }
            if ((mPositionY * -1) < 0) {
                mPositionY = 0;
            } else if ((mPositionY * -1) > mImageHeight * mScaleFactor - getHeight()) {
                mPositionY = (mImageHeight * mScaleFactor - getHeight()) * -1;
            }

            if ((mImageHeight * mScaleFactor) < getHeight()) {
                mPositionY = 0;
            }



            canvas.translate(mPositionX, mPositionY);
            canvas.scale(mScaleFactor, mScaleFactor);
            canvas.drawBitmap(mBitmap, 0, 0, null);
            //draw dots

            if(parkingStatus!=null && mBitmap!=null) {
                Map<String, StatusDot> statusDots = parkingStatus.getStatusDotList();
                for(StatusDot statusDot: statusDots.values()) {

                    //only normalize to the phone screen if it hasn't been done already.
                    if(!parkingStatus.isCanvasNormalizedFlag()){
                        int x = statusDot.getX();
                        int y = statusDot.getY();
                        //NORMALIZE  DOTS on pinchzoompan
                        // coordorig/imagesizeposorig = coordscreen/screensize
                        //I used proportions to normalize the coordinates on the image to the canvas on the screen.
                        final double tempX = x;
                        final double tempY = y;
                        final double canvasW = canvas.getWidth();
                        final double canvasH = (canvasW * bitmapActualHeight) / bitmapActualWidth;
                        //assume overlay is a square
                        x = (int) (canvasW * (tempX / bitmapActualWidth));
                        y = (int) (canvasH * (tempY / bitmapActualHeight));
                        statusDot.setCanvasX(x);
                        statusDot.setCanvasY(y);
                        //END NORMALIZE
                    }

                    boolean status = statusDot.getStatus();
                    if(parkingStatus.isUpdatedStatusFlag()) {
                        if (status) {
                            paint.setARGB(255, 0, 255, 0);
                        } else {
                            paint.setARGB(255, 255, 0, 0);
                        }
                    }else{
                        paint.setARGB(255, 150, 150, 150);
                    }
                    canvas.drawCircle(statusDot.getCanvasX(), statusDot.getCanvasY(), 30, paint);
                }

                //flip the flag so normalize doesn't run again.
                if(parkingStatus.isCanvasNormalizedFlag()){
                    parkingStatus.setCanvasNormalizedFlag(true);
                }
            }
            //end draw dots
            canvas.restore();


        }
    }

    public void loadImageOnCanvas(Bitmap myBitmap) {
        Bitmap bitmap = null;
        bitmapActualHeight = myBitmap.getHeight();
        bitmapActualWidth = myBitmap.getWidth();
        bitmap = myBitmap;

        float aspectRatio = (float) bitmap.getHeight() / (float) bitmap.getWidth();
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        mImageWidth = displayMetrics.widthPixels;
        mImageHeight = Math.round(mImageWidth * aspectRatio);
        mBitmap = bitmap.createScaledBitmap(bitmap, mImageWidth, mImageHeight, false);
        invalidate();
        //requestLayout();

    }

    public void setParkingStatus(ParkingStatus parkingStatus){
        this.parkingStatus =parkingStatus;
    }


    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {

            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            //don't to let the image get too large or small
            mScaleFactor = Math.max(mMinZoom, Math.min(mScaleFactor, mMaxZoom));

            invalidate();

            return true;
        }
    }
}