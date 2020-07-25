package com.example.smispeed;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    TextView textView;
    Button button;
    EditText fromText, toText;
    SharedPreferences sharedPreferences;
    int count;
    long fromRatio, toRatio;
    Float calcRatio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.smi_files);
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
        fromText = findViewById(R.id.from_ratio);
        toText = findViewById(R.id.to_ratio);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        fromRatio = sharedPreferences.getLong("fromRatio", 2565158);
        toRatio = sharedPreferences.getLong("toRatio", 2580641);
        calcRatio = (float) toRatio / (float) fromRatio;
        fromText.setText("" + fromRatio);
        fromText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                String ratioS = fromText.getText().toString();
                fromRatio = Long.parseLong(ratioS);
                editor.putFloat("fromRatio", fromRatio).apply();
                calcRatio = (float) toRatio / (float) fromRatio;
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        toText.setText("" + toRatio);
        fromText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                String ratioS = fromText.getText().toString();
                toRatio = Long.parseLong(ratioS);
                editor.putLong("toRatio", toRatio).apply();
                calcRatio = (float) toRatio / (float) fromRatio;
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
    }

    private void begin_convert() throws IOException {
        count = 0;
        StringBuilder sb = new StringBuilder();
        File srcDir = new File(Environment.getExternalStorageDirectory(), "Download");
        File dstDir = new File(Environment.getExternalStorageDirectory(), "Music");

//        File[] files = srcDir.listFiles();
        File[] files = srcDir.listFiles(new FileFilter() {
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
                textView.setText(sb);
                textView.invalidate();
            });

            Log.w("" + count, inpName);
            File outFile = new File(dstDir, inpName);
            outFile.delete();
            BufferedReader reader = null;
            String line = null;
            try {
                File smiFile = new File(srcDir, inpName);
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
                            num = (long) ((float) num * calcRatio);
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