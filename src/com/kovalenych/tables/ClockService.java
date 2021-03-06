package com.kovalenych.tables;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.RemoteViews;
import com.kovalenych.Const;
import com.kovalenych.R;
import com.kovalenych.Table;
import com.kovalenych.Utils;
import com.kovalenych.stats.StatsDAO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * this class was made
 * by insomniac and angryded
 * for their purposes
 */
public class ClockService extends Service implements Soundable, Const {

    Table table;
    Vibrator v;
    PendingIntent pi;

    final String LOG_TAG = "CO2 ClockService";

    private boolean vibrationEnabled;
    private ArrayList<Integer> voices;
    String name;
    HashMap<Integer, Object> soundPool = new HashMap<Integer, Object>();
    StatsDAO dao;

    public boolean showTray = false;

    MediaPlayer mediaPlayer;
    private int volume;
    private String cachePath;


    public void onCreate() {
        super.onCreate();

        v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        Log.d(LOG_TAG, "ClockService onCreate");
        mediaPlayer = new MediaPlayer();

        SharedPreferences _preferedSettings = getSharedPreferences("sharedSettings", MODE_PRIVATE);
        int bla = _preferedSettings.getInt("volume", 15);
        Log.d("zzzzzzbla", "" + bla);

        fillPool();
        File externalCacheDir = getExternalCacheDir();
        if (externalCacheDir == null)
            cachePath = null;
        else
            cachePath = externalCacheDir.getPath() + "/";
    }

    private void fillPool() {
        soundPool.put(TO_START_2_MIN, R.raw.to2min);
        soundPool.put(TO_START_1_MIN, R.raw.to1min);
        soundPool.put(TO_START_30_SEC, R.raw.to30sec);
        soundPool.put(TO_START_10_SEC, R.raw.to10sec);
        soundPool.put(TO_START_5_SEC, R.raw.to5sec);
        soundPool.put(START, R.raw.start);
        soundPool.put(AFTER_START_1, R.raw.after1min);
        soundPool.put(AFTER_START_2, R.raw.after2min);
        soundPool.put(AFTER_START_3, R.raw.after3min);
        soundPool.put(AFTER_START_4, R.raw.after4min);
        soundPool.put(AFTER_START_5, R.raw.after5min);
        soundPool.put(BREATHE, R.raw.breathe);

        SharedPreferences voiceFileSettings = getSharedPreferences("voice_files", MODE_PRIVATE);
        Map<String, String> savedSounds = (Map<String, String>) voiceFileSettings.getAll();
        for (String key : savedSounds.keySet()) {
            soundPool.put(Integer.parseInt(key), savedSounds.get(key));
        }
    }

    private void showProgressInTray(int progress, int max, boolean breathing) {
        Log.d("showProgressInTray", "zzzzzzzzz");
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE); // Создаем экземпляр менеджера уведомлений
        int icon = R.drawable.tray_icon; // Иконка для уведомления, я решил воспользоваться стандартной иконкой для Email
        long when = System.currentTimeMillis(); // Выясним системное время
        Intent notificationIntent = new Intent(this, ClockActivity.class); // Создаем экземпляр Intent
        Notification notification = new Notification(icon, null, when); // Создаем экземпляр уведомления, и передаем ему наши параметры
        PendingIntent contentIntent = PendingIntent.getActivity(this, 5, notificationIntent, 0); // Подробное описание смотреть в UPD к статье
        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notif); // Создаем экземпляр RemoteViews указывая использовать разметку нашего уведомления
        contentView.setProgressBar(R.id.tray_progress, max, progress, false);
        contentView.setTextViewText(R.id.tray_text, (breathing ? "breath  " : "hold  ") + Utils.timeToString(progress)); // Привязываем текст к TextView в нашей разметке
        notification.contentIntent = contentIntent; // Присваиваем contentIntent нашему уведомлению
        notification.contentView = contentView; // Присваиваем contentView нашему уведомлению
        mNotificationManager.notify(NOTIFY_ID, notification); // Выводим уведомление в строку
    }


    public void onDestroy() {
        super.onDestroy();
        NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancel(NOTIFY_ID);
        if (task != null)
            task.cancel(true);
        if (mediaPlayer != null)
            mediaPlayer.release();
        mediaPlayer = null;
        Log.d(LOG_TAG, "ClockService onDestroy");
        if (dao != null) {
            dao.onEndSession();
            dao.onDestroy();
        }
    }

    public int subscriber = SUBSCRIBER_CLOCK;

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "ClockService onStartCommand" + flags);

        String destination = intent.getStringExtra(FLAG);
        if (destination == null)
            return super.onStartCommand(intent, flags, startId);
        if (destination.equals(FLAG_CREATE)) {
            Log.d(LOG_TAG, FLAG_CREATE);
            Bundle cyclesBundle = intent.getBundleExtra(ClockActivity.PARAM_CYCLES);
            pi = intent.getParcelableExtra(ClockActivity.PARAM_PINTENT);
            name = intent.getStringExtra(ClockActivity.PARAM_TABLE);
            Log.d(LOG_TAG, "tableName  " + name);
            volume = intent.getIntExtra(PARAM_VOLUME, 0);
            Log.d(LOG_TAG, "set volume " + volume);
            int size = cyclesBundle.getInt("tablesize");
            table = new Table();
            ArrayList<Cycle> cycles = table.getCycles();
            for (int i = 0; i < size; i++) {
                cycles.add(
                        new Cycle(cyclesBundle.getInt("breathe" + Integer.toString(i)), cyclesBundle.getInt("hold" + Integer.toString(i)))
                );
                Log.d(LOG_TAG, "new cycle" + cycles.get(i).convertToString());
            }
            int position = cyclesBundle.getInt("number");
            vibrationEnabled = cyclesBundle.getBoolean("vibro");
            voices = cyclesBundle.getIntegerArrayList("voices");
            task = new ClockTask(table, true);
            task.execute(position);
            dao = new StatsDAO(this, name, true);
            dao.onStartSession();
            wholeTimeStarts = System.currentTimeMillis();
            for (Cycle cycle : cycles) {
                wholeTableTime += cycle.breathe * 1000;
                wholeTableTime += cycle.hold * 1000;    // todo start from position
            }

        } else if (destination.equals(FLAG_SHOW_TRAY)) {
            Log.d(LOG_TAG, FLAG_SHOW_TRAY);
            showTray = true;
        } else if (destination.equals(FLAG_HIDE_TRAY)) {
            showTray = false;
            pi = intent.getParcelableExtra(PARAM_PINTENT);
            NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nMgr.cancel(NOTIFY_ID);
            subscriber = SUBSCRIBER_CLOCK;
        } else if (destination.equals(FLAG_SUBSCRIBE_TABLE)) {
            pi = intent.getParcelableExtra(PARAM_PINTENT);
            subscriber = SUBSCRIBER_TABLE;
            Intent newIntent = new Intent()
                    .putExtra(ClockActivity.PARAM_TABLE, name)
                    .putExtra(ClockActivity.PARAM_M_CYCLE, curCycle);
            try {
                pi.send(ClockService.this, 0, newIntent);
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        } else if (destination.equals(FLAG_SUBSCRIBE_CYCLES)) {
            pi = intent.getParcelableExtra(PARAM_PINTENT);
            subscriber = SUBSCRIBER_CYCLE;
            volume = intent.getIntExtra(PARAM_VOLUME, 0);
            Log.d(LOG_TAG, "set volume " + volume);
            Intent newIntent = new Intent()
                    .putExtra(ClockActivity.PARAM_TABLE, name)
                    .putExtra(ClockActivity.PARAM_M_CYCLE, curCycle);
            try {
                pi.send(ClockService.this, 0, newIntent);
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }

        } else if (destination.equals(FLAG_SETVOLUME)) {
            Log.d(LOG_TAG, "set volume " + volume);
            volume = intent.getIntExtra(PARAM_VOLUME, 0);
        } else if (destination.equals(FLAG_CONTRACTION)) {
            Log.d(LOG_TAG, "set volume " + volume);
            if (!isBreathing)
                contrConsumed = false;
        } else if (destination.equals(FLAG_IMMEDIATE_BREATH)) {
            Log.d(LOG_TAG, "set volume " + volume);
            if (!isBreathing)
                immediateConsumed = false;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    boolean contrConsumed = true;
    boolean immediateConsumed = true;
    boolean isBreathing;
    ClockTask task;
    int curCycle = -1;
    long wholeTableTime;
    long wholeTimeStarts;


    private void onTic(int time, int cycle, boolean breathing) {
        isBreathing = breathing;
        //evaluate percent progress
        curCycle = cycle;
        int breathe = table.getCycles().get(cycle).breathe;
        int hold = table.getCycles().get(cycle).hold;
        int all = breathing ? breathe : hold;
        Log.d(LOG_TAG, "zzzzonTic  time: " + time);
        long elapsed = System.currentTimeMillis() - wholeTimeStarts;
        Intent intent = new Intent()
                .putExtra(ClockActivity.PARAM_TIME, time)
                .putExtra(ClockActivity.PARAM_PROGRESS, all)
                .putExtra(ClockActivity.PARAM_BREATHING, breathing)
                .putExtra(ClockActivity.PARAM_TABLE, name)
                .putExtra(ClockActivity.PARAM_WHOLE_TABLE_ELAPSED, elapsed)
                .putExtra(ClockActivity.PARAM_WHOLE_TABLE_REMAINS, wholeTableTime - elapsed)
                .putExtra(ClockActivity.PARAM_M_CYCLE, cycle);
        try {
            int stat = breathing ? STATUS_BREATH : STATUS_HOLD;
            if (subscriber == SUBSCRIBER_CLOCK ||
                    (breathing && (time == 0) && subscriber == SUBSCRIBER_CYCLE)) {
                pi.send(ClockService.this, stat, intent);
                Log.d(LOG_TAG, "sendtotable");
            }
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
        //vibrate  or sound

        if (time == 0 && vibrationEnabled)
            v.vibrate(200);
        if (breathing) {
            if (showTray)
                showProgressInTray(time, breathe, breathing);
            //breath
            int relatTime = time - breathe;
            if (time == 0 && voices.contains(BREATHE)) {
                playSound(BREATHE);
            } else if (voices.contains(relatTime))
                playSound(relatTime);
            if (time == breathe - 1)
                dao.onCycleLife(breathing, cycle, time + 1);
        } else {
            if (showTray)
                showProgressInTray(time, hold, breathing);
            if (time == 0 && voices.contains(START)) {
                playSound(START);
            } else if (voices.contains(time))
                playSound(time);
            if (time == hold - 1)
                dao.onCycleLife(breathing, cycle, time + 1);
            if (!contrConsumed) {
                contrConsumed = true;
                dao.onContraction(cycle, time + 1);
            }
        }
    }

    public static final int MAX_VOLUME = 30;

    private void playSound(int key) {
        Object obj = soundPool.get(key);
        if (obj instanceof Integer) {      //if it is my sound
            mediaPlayer = MediaPlayer.create(getApplicationContext(), (Integer) obj);
            float log1 = (float) (Math.log(MAX_VOLUME - volume) / Math.log(MAX_VOLUME));
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(1 - log1, 1 - log1);
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                        mp.start();
                    }
                });
            }

        } else                             //if it is external sound
            try {
                if (cachePath == null)
                    return;
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(cachePath + soundPool.get(key));
                float log1 = (float) (Math.log(MAX_VOLUME - volume) / Math.log(MAX_VOLUME));
                mediaPlayer.setVolume(1 - log1, 1 - log1);
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                        mp.start();
                    }
                });
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }


    public void onTableFinish() {

        if (voices.contains(BREATHE)) {
            playSound(BREATHE);
        }
        if (vibrationEnabled)
            v.vibrate(200);

        Intent intent = new Intent()
                .putExtra(ClockActivity.PARAM_TIME, 0)
                .putExtra(ClockActivity.PARAM_PROGRESS, 0)
                .putExtra(ClockActivity.PARAM_BREATHING, false);
        try {
            pi.send(ClockService.this, STATUS_FINISH, intent);
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
        stopSelf();
    }

    public IBinder onBind(Intent arg0) {
        return null;
    }


    class ClockTask extends AsyncTask<Integer, Integer, Void> {

        Table table;
        volatile boolean breathing;
        public static final int IMMEDIATE = 0;
        public static final int NOT_IMMEDIATE = 1;

        public ClockTask(Table table, boolean breathing) {
            this.table = table;
            this.breathing = breathing;
        }

        @Override
        protected Void doInBackground(Integer... params) {

            long tableStart = System.currentTimeMillis();
            for (int i = params[0]; i < table.getCycles().size(); i++) {
                long cycleStart = System.currentTimeMillis();
                Cycle cycle = table.getCycles().get(i);
                if (breathing)
                    for (int t = 0; t < cycle.breathe; t++) {
                        if (isCancelled())
                            return null;
                        publishProgress(t, i, NOT_IMMEDIATE);
                        try {
                            long adjust = System.currentTimeMillis() - cycleStart - 1000 * t;
                            long sleeptime = 1000 - adjust;
                            if (sleeptime > 0)
                                Thread.sleep(sleeptime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                breathing = false;
                cycleStart = System.currentTimeMillis();
                for (int t = 0; t < cycle.hold; t++) {
                    if (isCancelled())
                        return null;
                    if (immediateConsumed)
                        publishProgress(t, i, NOT_IMMEDIATE);
                    else {
                        immediateConsumed = true;
                        publishProgress(cycle.hold - 1, i, IMMEDIATE);
                        break;
                    }
                    try {
                        long adjust = System.currentTimeMillis() - cycleStart - 1000 * t;
                        long sleeptime = 1000 - adjust;
                        if (sleeptime > 0)
                            Thread.sleep(sleeptime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
                breathing = true;
            }
            Log.d("table made in ", "" + (System.currentTimeMillis() - tableStart));
            return null;
        }


        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (values[2] == NOT_IMMEDIATE)
                onTic(values[0], values[1], breathing);
            else
                onImmediateBreath(values[0], values[1]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            onTableFinish();
        }
    }

    private void onImmediateBreath(Integer cycle, Integer time) {
        dao.onCycleLife(false, cycle, time + 1);
    }

}
