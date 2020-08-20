package com.example.smispeed;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    TextView smiText;
    Button button;
    EditText fromText, toText, gapText;
    SharedPreferences sharedPreferences;
    int count;
    long fromDuration, toDuration, gapDuration;
    Float ratioFloat;
    boolean updatedFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        smiText = findViewById(R.id.smi_files);
        fromText = findViewById(R.id.from_duration);
        toText = findViewById(R.id.to_duration);
        gapText = findViewById(R.id.gap_duration);
        button = findViewById(R.id.convert);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Timer().schedule(new TimerTask() {
                    public void run() {
                        try {
                            begin_convert();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 10);
            }
        });
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        fromDuration = sharedPreferences.getLong("fromDuration", 2565158);
        toDuration = sharedPreferences.getLong("toDuration", 2580641);
        gapDuration = fromDuration - toDuration;
        ratioFloat = (float) toDuration / (float) fromDuration;
        SharedPreferences.Editor editor = sharedPreferences.edit();

        fromText.setText("" + fromDuration);
        fromText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            public void afterTextChanged(Editable s) {
                String str = fromText.getText().toString();
                fromDuration = Long.parseLong(str);
                editor.putLong("fromDuration", fromDuration).apply();
                toDuration = fromDuration - gapDuration;
                ratioFloat = (float) toDuration / (float) fromDuration;
                toText.setText("" + toDuration);
            }
        });
        gapText.setText("" + gapDuration);
        gapText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            public void afterTextChanged(Editable s) {
                String str = gapText.getText().toString();
                gapDuration = Long.parseLong(str);
                toDuration = fromDuration - gapDuration;
                editor.putLong("toDuration", toDuration).apply();
                toText.setText("" + toDuration);
                ratioFloat = (float) toDuration / (float) fromDuration;
            }
        });
        toText.setText("" + toDuration);
//        toText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
//            public void afterTextChanged(Editable s) {
//                String ratioS = toText.getText().toString();
//                toDuration = Long.parseLong(ratioS);
//                editor.putLong("toDuration", toDuration).apply();
//                ratioFloat = (float) toDuration / (float) fromDuration;
//                gapDuration = fromDuration - toDuration;
//                gapText.setText(""+gapDuration);
//            }
//        });
    }

    private void begin_convert() throws IOException {
        count = 0;
        StringBuilder sb = new StringBuilder();
        File srcPath = new File(Environment.getExternalStorageDirectory(), "Download");
        File dstPath = new File(Environment.getExternalStorageDirectory(), "Music");

//        File[] files = srcPath.listFiles();
        File[] files = srcPath.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
//                return true;
                return (file.getPath().endsWith("smi"));
            }
        });
        Log.w("file count", "= " + files.length);
        for (File file : files) {
            String inpName = file.getName();
            count++;
            sb.append(count).append(":").append(inpName).append(", ");
            this.runOnUiThread(() -> {
                smiText.setText(sb);
                smiText.invalidate();
            });

            Log.w("" + count, inpName);
            File outFile = new File(dstPath, inpName);
            outFile.delete();
            BufferedReader reader = null;
            String line = null;
            try {
                File smiFile = new File(srcPath, inpName);
                if (smiFile.exists() && smiFile.isFile() && smiFile.length() > 0) {
                    reader = new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(smiFile),
                                    "MS949"
                            )
                    );
                    int lineCnt = 0;
                    while ((line = reader.readLine()) != null) {
                        int pos = line.indexOf("Start=");
                        if (pos < 0)
                            append2file(outFile, line);
                        else {
                            int ending = line.indexOf(">");
                            String leftLine = line.substring(0,pos+6);
                            String number = line.substring(pos+6,ending);
                            String rightLine = line.substring(ending);
                            Log.w(""+lineCnt,leftLine+"~"+number+"~"+rightLine);
                            long num = Long.parseLong(number);
                            num = (long) ((float) num * ratioFloat);
                            append2file(outFile, leftLine+num+rightLine);
                        }
                    }
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    void append2file (File file, String text) {
        BufferedWriter bw = null;
        FileWriter fw = null;
        try {
            if (!file.exists()) {
                if(!file.createNewFile()) {
                    Log.e("createFile"," Error");
                }
            }
            fw = new FileWriter(file.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);
            bw.write("\n" + text);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) bw.close();
                if (fw != null) fw.close();
            } catch (IOException e) {
                Log.e("appendIOExcept2",  e.getMessage());
            }
        }
    }

}