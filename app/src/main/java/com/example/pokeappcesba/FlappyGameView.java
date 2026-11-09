package com.example.pokeappcesba;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FlappyGameView extends View {

    public interface GameListener {
        void onScoreChanged(int score);
        void onGameOver(int score);
    }

    private GameListener listener;

    private Paint paint;
    private Bitmap pokemonBitmap;

    private boolean isPlaying = false;
    private int score = 0;
    private int screenWidth, screenHeight;
    private float groundY;

    // Pokemon State
    private float pokemonX, pokemonY;
    private float velocityY = 0f;
    private float gravity = 0.8f;
    private float jumpVelocity = -15f;
    private int pokemonSize = 120;

    // Obstacles
    private List<Pipe> pipes = new ArrayList<>();
    private int pipeWidth = 140;
    private float pipeSpeed = 5.5f;
    private int pipeGap = 600;
    private Random random = new Random();
    private ToneGenerator toneGen;

    public FlappyGameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
    }

    public void setGameListener(GameListener listener) {
        this.listener = listener;
    }

    public void setPokemonBitmap(Bitmap bitmap) {
        this.pokemonBitmap = bitmap;
        if (bitmap != null) {
            pokemonSize = Math.max(100, Math.min(bitmap.getWidth(), 150));
        }
        invalidate();
    }

    public void startGame() {
        pokemonX = screenWidth / 3f;
        pokemonY = screenHeight / 2f;
        velocityY = 0f;
        score = 0;
        pipeSpeed = 5.5f;
        pipes.clear();
        isPlaying = true;
        if (listener != null) listener.onScoreChanged(score);
        postInvalidateOnAnimation();
    }

    public void stopGame() {
        isPlaying = false;
        if (listener != null) listener.onGameOver(score);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        groundY = h - 100;
        pokemonX = w / 3f;
        pokemonY = h / 2f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (isPlaying) {
                velocityY = jumpVelocity;
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 50);
            }
            performClick();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDraw(@androidx.annotation.NonNull Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw background (Night sky)
        paint.setColor(Color.parseColor("#0F172A"));
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        
        // Draw stars (Simple static)
        paint.setColor(Color.WHITE);
        for(int i=0; i<20; i++) {
            canvas.drawCircle((i*97) % screenWidth, (i*131) % (screenHeight-100), 2, paint);
        }

        if (isPlaying) {
            updatePhysics();
        }

        // Draw Pipes
        for (Pipe pipe : pipes) {
            // Draw top pipe
            drawRetroPipe(canvas, pipe.x, 0, pipe.x + pipeWidth, pipe.topHeight, true);
            // Draw bottom pipe
            drawRetroPipe(canvas, pipe.x, pipe.topHeight + pipeGap, pipe.x + pipeWidth, groundY, false);
        }

        // Draw ground
        paint.setColor(Color.parseColor("#1E293B"));
        canvas.drawRect(0, groundY, screenWidth, screenHeight, paint);
        paint.setColor(Color.parseColor("#94A3B8"));
        canvas.drawLine(0, groundY, screenWidth, groundY, paint);

        // Draw Pokemon
        if (pokemonBitmap != null) {
            Matrix matrix = new Matrix();
            // Scale
            float scale = (float) pokemonSize / pokemonBitmap.getWidth();
            matrix.postScale(scale, scale);
            // Rotate based on velocity
            float rotation = Math.max(-25, Math.min(25, velocityY * 2));
            matrix.postRotate(rotation, (pokemonBitmap.getWidth()*scale)/2, (pokemonBitmap.getHeight()*scale)/2);
            // Translate
            matrix.postTranslate(pokemonX - (pokemonSize/2f), pokemonY - (pokemonSize/2f));
            
            canvas.drawBitmap(pokemonBitmap, matrix, null);
        } else {
            // Fallback draw
            paint.setColor(Color.RED);
            canvas.drawCircle(pokemonX, pokemonY, pokemonSize/2f, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(pokemonX, pokemonY, pokemonSize/4f, paint);
            paint.setColor(Color.BLACK);
            canvas.drawLine(pokemonX - pokemonSize/2f, pokemonY, pokemonX + pokemonSize/2f, pokemonY, paint);
        }

        if (isPlaying) {
            postInvalidateOnAnimation();
        }
    }

    private void drawRetroPipe(Canvas canvas, float left, float top, float right, float bottom, boolean isTopPipe) {
        // Body
        paint.setColor(Color.parseColor("#334155"));
        canvas.drawRect(left, top, right, bottom, paint);
        // Borders
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(Color.parseColor("#94A3B8"));
        canvas.drawRect(left, top, right, bottom, paint);
        paint.setStyle(Paint.Style.FILL);
        
        // Cap
        float capHeight = 30;
        paint.setColor(Color.parseColor("#FACC15"));
        if (isTopPipe) {
            canvas.drawRect(left - 10, bottom - capHeight, right + 10, bottom, paint);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(left - 10, bottom - capHeight, right + 10, bottom, paint);
            paint.setStyle(Paint.Style.FILL);
        } else {
            canvas.drawRect(left - 10, top, right + 10, top + capHeight, paint);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(left - 10, top, right + 10, top + capHeight, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void updatePhysics() {
        // Pokemon Gravity
        velocityY += gravity;
        pokemonY += velocityY;

        // Pipe Generation
        if (pipes.isEmpty() || pipes.get(pipes.size() - 1).x < screenWidth - 550) {
            float minPipeHeight = 100;
            float maxPipeHeight = Math.max(150, groundY - pipeGap - 100);
            float topHeight = minPipeHeight + random.nextFloat() * (maxPipeHeight - minPipeHeight);
            
            // Retrasar (alejar) la aparición de la primera tubería para dar más tiempo de reacción
            float spawnX = pipes.isEmpty() ? screenWidth + 800 : screenWidth;
            pipes.add(new Pipe(spawnX, topHeight));
        }

        // Pipe Movement & Collision
        RectF pokemonRect = new RectF(pokemonX - pokemonSize/2f + 15, pokemonY - pokemonSize/2f + 15, 
                                      pokemonX + pokemonSize/2f - 15, pokemonY + pokemonSize/2f - 15);

        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            pipe.x -= pipeSpeed;

            // Scoring
            if (!pipe.passed && pokemonX > pipe.x + pipeWidth) {
                pipe.passed = true;
                score++;
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 50);
                pipeSpeed += 0.1f; // Aumentar dificultad de manera más suave
                if (listener != null) listener.onScoreChanged(score);
            }

            // Collision with pipes
            RectF topPipeRect = new RectF(pipe.x, 0, pipe.x + pipeWidth, pipe.topHeight);
            RectF bottomPipeRect = new RectF(pipe.x, pipe.topHeight + pipeGap, pipe.x + pipeWidth, groundY);

            if (RectF.intersects(pokemonRect, topPipeRect) || RectF.intersects(pokemonRect, bottomPipeRect)) {
                toneGen.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200);
                stopGame();
                return;
            }
        }

        // Remove off-screen pipes
        if (!pipes.isEmpty() && pipes.get(0).x + pipeWidth < 0) {
            pipes.remove(0);
        }

        // Floor/Ceiling Collision
        if (pokemonY + pokemonSize/2f >= groundY || pokemonY - pokemonSize/2f <= 0) {
            toneGen.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200);
            stopGame();
        }
    }

    private class Pipe {
        float x;
        float topHeight;
        boolean passed = false;

        Pipe(float x, float topHeight) {
            this.x = x;
            this.topHeight = topHeight;
        }
    }
}
