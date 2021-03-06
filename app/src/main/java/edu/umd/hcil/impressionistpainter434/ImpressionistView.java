package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 50;
    private Point _lastPoint = null;
    private long _lastPointTime = 0;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 25;
    private double speed = 0;
    private float size = _defaultRadius;
    private boolean tiltSensor = false;
    private int eraseColor = Color.rgb(245, 245, 245);

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DARKEN));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        //TODO
        //_offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
        //_offScreenCanvas = new Canvas(_offScreenBitmap);
        _offScreenCanvas.drawColor(eraseColor);
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        //TODO
        //Basically, the way this works is to liste for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        Bitmap imageMap = _imageView.getDrawingCache();
        int color;
        int xMax = imageMap.getWidth();
        int yMax = imageMap.getHeight();

        if (x >= 0 && x <= xMax && y > 0 && y < yMax) {
            color = imageMap.getPixel((int) x, (int) y);
            if (color != 0) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        size = _defaultRadius;
                        speed = 0;

                        _paint.setColor(color);
                        drawOnBitmap(x, y);
                        _lastPoint = new Point((int) x, (int) y);
                        _lastPointTime = System.currentTimeMillis();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (_useMotionSpeedForBrushStrokeSize) {
                            double rat = getRatio(x, y);
                            if (rat > 1) {
                                size = size + 10;
                            } else if (rat < 1) {
                                size = size - 10 < _minBrushRadius ? _minBrushRadius : size - 10;
                            }
                        }
                        //color = imageMap.getPixel((int) x, (int) y);
                        _paint.setColor(color);
                        drawOnBitmap(x, y);
                        break;
                }
            }
        }

        invalidate();
        return true;
    }

    private double getRatio(float x, float y){
        double dist = Math.sqrt(Math.pow((x - _lastPoint.x), 2) + Math.pow((y - _lastPoint.y), 2));
        long time = System.currentTimeMillis();
        long timeDif = time - _lastPointTime;
        _lastPoint = new Point((int)x, (int)y);
        _lastPointTime = time;
        double tmp = dist / timeDif;
        double ratio = speed == 0 ? 1 : tmp / speed;
        speed = tmp;
        //System.out.println(ratio);
        return ratio;
    }

    private void drawOnBitmap(float x, float y){
        float tmp = size / 2;
        float l = x - tmp, r = x + tmp, t = y - tmp, b = y +tmp;

        switch(_brushType){
            case Circle:
                _offScreenCanvas.drawCircle(x, y, size, _paint);
                break;
            case Square:
                _offScreenCanvas.drawRect(l, t, r, b, _paint);
                break;
            case Line:
                _offScreenCanvas.drawLine(l, t, r, b, _paint);
                break;
            case CircleSplatter:
                _offScreenCanvas.drawCircle(l, y, size/2, _paint);
                float xTmp = (float)((size*Math.cos(Math.toRadians(45))) - (0*Math.sin(Math.toRadians(45)))) + l;
                float yTmp = (float)((size*Math.sin(Math.toRadians(45))) + (0*Math.cos(Math.toRadians(45)))) + y;
                _offScreenCanvas.drawCircle(xTmp, yTmp, size/3, _paint);

                xTmp = (float)((size*Math.cos(Math.toRadians(315))) - (0*Math.sin(Math.toRadians(315)))) + l;
                yTmp = (float)((size*Math.sin(Math.toRadians(315))) + (0*Math.cos(Math.toRadians(315)))) + y;
                _offScreenCanvas.drawCircle(xTmp, yTmp, size/3, _paint);
                break;
            case LineSplatter:
                float[] pts = getCoords(x, y, size);
                _offScreenCanvas.drawLines(pts, _paint);
                break;
            case Eraser:
                _paint.setColor(eraseColor);
                _offScreenCanvas.drawCircle(x, y, _defaultRadius, _paint);
                break;

        }
    }

    private float[] getCoords(float x, float y, float dif){
        float[] list = new float[32];
        float xp = dif;
        float yp = 0;
        double sig = 0;

        for (int i = 2; i < 31; i = i+4){
            list[i-2] = x;
            list[i-1] = y;
            float xTmp = (float)((xp*Math.cos(sig)) - (yp*Math.sin(sig))) + x;
            float yTmp = (float)((xp*Math.sin(sig)) + (yp*Math.cos(sig))) + y;

            list[i] = xTmp;
            list[i+1] = yTmp;
            sig = sig + Math.toRadians(45);
        }

        return list;
    }




    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }

    public Bitmap get_offScreenBitmap() {
        return _offScreenBitmap;
    }

    public void flipToggle(){
        _useMotionSpeedForBrushStrokeSize = !_useMotionSpeedForBrushStrokeSize;
    }

    public void sensorMove(float r, float d) {
        r = r/2;
        d = d/2;
        if (_lastPoint != null) {
            Bitmap imageMap = _imageView.getDrawingCache();
            int color;
            int xMax = imageMap.getWidth();
            int yMax = imageMap.getHeight();
            float x = _lastPoint.x;
            float y = _lastPoint.y;


            x = x + r;
            y = y + d;
            if (x >= 0 && x <= xMax && y >= 0 && y <= yMax) {
                color = imageMap.getPixel((int) x, (int) y);

                if (color != 0) {
                    _paint.setColor(color);
                    drawOnBitmap(x, y);
                    _lastPoint = new Point((int) x, (int) y);
                }
            }
        }
    }

    public void flipSensor(){
        tiltSensor = !tiltSensor;
    }
}

