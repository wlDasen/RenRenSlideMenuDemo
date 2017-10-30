package net.sunniwell.renrenslidemenudemo;

import android.graphics.Point;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    private static final String TAG = "jpd-MA";
    private static int menuPadding = 80;
    private View menu;
    private View content;
    private LinearLayout.LayoutParams menuLayoutParams;
    private float xDown;
    private float xMove;
    private float xUp;
    /**
     * leftMargin左端极限值
     */
    private int leftEdge;
    /**
     * leftMargin右端极限值
     */
    private int rightEdge = 0;
    private boolean isMenuVisible = false;
    private static int screenWidth;
    private VelocityTracker mTracker;
    private static int VELOCITY_DEFAULT_DISTANCE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initLayoutparams();
        content.setOnTouchListener(this);
    }

    private void initLayoutparams() {
        WindowManager manager = (WindowManager)getSystemService(WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point outSize = new Point();
        display.getSize(outSize);
        screenWidth = outSize.x;

        menu = findViewById(R.id.menu);
        content = findViewById(R.id.content);
        menuLayoutParams = (LinearLayout.LayoutParams)menu.getLayoutParams();
        menuLayoutParams.width = screenWidth - menuPadding;
        leftEdge = -menuLayoutParams.width;
        menuLayoutParams.leftMargin = leftEdge;
        content.getLayoutParams().width = screenWidth;
        Log.d(TAG, "initLayoutparams: screenW:" + screenWidth);
    }

    private void createVelocity(MotionEvent motionEvent) {
        if (mTracker == null) {
            mTracker = VelocityTracker.obtain();
        }
        mTracker.addMovement(motionEvent);
    }

    private int getVelocity() {
        mTracker.computeCurrentVelocity(1000);
        int x = (int)mTracker.getXVelocity();
        return Math.abs(x);
    }

    private void recycleVelocity() {
        mTracker.recycle();
        mTracker = null;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        createVelocity(motionEvent);
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
//                Log.d(TAG, "onTouch: donw.");
                xDown = motionEvent.getRawX();
                break;
            case  MotionEvent.ACTION_MOVE:
//                Log.d(TAG, "onTouch: move.");
                xMove = motionEvent.getRawX();
                int distantX = (int)(xMove - xDown);
                if (isMenuVisible) {
                    menuLayoutParams.leftMargin = distantX;
                } else {
                    menuLayoutParams.leftMargin = leftEdge + distantX;
                }

                if (menuLayoutParams.leftMargin < leftEdge) {
                    menuLayoutParams.leftMargin = leftEdge;
                }
                if (menuLayoutParams.leftMargin > rightEdge) {
                    menuLayoutParams.leftMargin = rightEdge;
                }
                menu.setLayoutParams(menuLayoutParams);
                break;
            case MotionEvent.ACTION_UP:
//                Log.d(TAG, "onTouch: up.");
                xUp = motionEvent.getRawX();
                if (wantToScrollToMenu()) {
                    if (shouldToScrollToMenu()) {
                        scrollToMenu();
                    } else {
                        scrollToContent();
                    }
                } else if (wantToScrollToContent()) {
                    if (shouldToScrollToContent()) {
                        scrollToContent();
                    } else {
                        scrollToMenu();
                    }
                }
                Log.d(TAG, "onTouch: xUp:" + xUp + ",xDown:" + xDown + ",W:" + screenWidth);
                Log.d(TAG, "onTouch: velocity:" + getVelocity());
                recycleVelocity();
                break;
            default:
                break;
        }
        return true;
    }
    private boolean wantToScrollToMenu() {
        return xUp - xDown > 0 && !isMenuVisible;
    }
    private boolean wantToScrollToContent() {
        return xUp - xDown < 0 && isMenuVisible;
    }
    private boolean shouldToScrollToMenu() {
        return xUp - xDown > screenWidth / 2 || getVelocity() > VELOCITY_DEFAULT_DISTANCE;
    }
    private boolean shouldToScrollToContent() {
        return xDown - xUp + menuPadding > screenWidth / 2 || getVelocity() >VELOCITY_DEFAULT_DISTANCE;
    }
    private void scrollToMenu() {
        new ScrollTask().execute(30);
    }
    private void scrollToContent() {
        new ScrollTask().execute(-30);
    }

    public class ScrollTask extends AsyncTask<Integer, Integer, Integer> {
        private static final String TAG = "jpd-ScrollTask";
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "onPreExecute: thread:" + Thread.currentThread().getId());
        }

        @Override
        protected Integer doInBackground(Integer... integers) {
//            Log.d(TAG, "doInBackground: thread:" + Thread.currentThread().getId());
            int leftMargin;
            while (true) {
                leftMargin = menuLayoutParams.leftMargin + integers[0];
                if (leftMargin > rightEdge) {
                    leftMargin = rightEdge;
                    break;
                }
                if (leftMargin < leftEdge) {
                    leftMargin = leftEdge;
                    break;
                }
                publishProgress(leftMargin);
                sleep(20);
            }
            if (leftMargin == rightEdge) {
                isMenuVisible = true;
            } else {
                isMenuVisible = false;
            }
            return leftMargin;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
//            Log.d(TAG, "onProgressUpdate: thread:" + Thread.currentThread().getId());
            menuLayoutParams.leftMargin = values[0];
            menu.setLayoutParams(menuLayoutParams);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
//            Log.d(TAG, "onPostExecute: thread:" + Thread.currentThread().getId());
            menuLayoutParams.leftMargin = integer;
            menu.setLayoutParams(menuLayoutParams);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
