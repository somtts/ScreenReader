package com.example.vinay.screenreader;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

import static android.support.v4.app.ActivityCompat.startActivityForResult;


public class MyScreenReaderService extends AccessibilityService implements TextToSpeech.OnInitListener {
    private final static String TAG = "MyScreenReaderService";
    private final int  ENG = 0;
    private final int  TEL = 1;
    private static MediaPlayer mediaPlayer ;
    private TextToSpeech tts;
    boolean mTtsInitialized = false;
    int reading_type = 0; //default mode
    String last;
    public String[] lines ;
    public String[] paras ;
    int nParas=0,nLines = 0;
    int curr_line = 0, curr_para=0;
    int curr_lang = ENG;
    boolean stopSpeechFlag = false; // flag will stop speech generation
    public static boolean mp_free = true; // flag for media player
    private int tel_curr_line = 0;
    private int tel_curr_line2 = 0;

    //Configure the Accessibility Service
    @Override
    protected void onServiceConnected() {
        Toast.makeText(getApplication(), "ServiceConnected", Toast.LENGTH_SHORT).show();
        Log.v(TAG,"ServiceConnected");
        //Init TextToSpeech
        tts = new TextToSpeech(getApplicationContext(),this);
        mediaPlayer = new MediaPlayer();
    }

    @Override
    /*
        obtain the text from various types of Accessibility events
        triggered by the application upon interaction by the user
     */
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();
        String eventText = "";
        String curr;
        Boolean speakFlag = false;
        Log.v(TAG,String.format(
                "onAccessibilityEvent: [type] %s [class] %s  [package]  %s [time] %s \n [text] %s \n [description] %s"
                ,getEventType(event),event.getClassName(),event.getPackageName(),event.getEventTime()
                ,event.getText().toString().trim(),event.getContentDescription()));
        switch (event.getEventType()){
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                speakFlag = true;
                String tempo ;
                tempo = event.getText().toString();
                if(tempo.equals("[]")){

                }
                else
                    eventText = eventText + tempo;
                tts.speak(eventText, TextToSpeech.QUEUE_FLUSH, null);
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                speakFlag = false;
                break;
            case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER:
                speakFlag = true;
                if(event.getText().toString().equals("[]")){
                    if(event.getContentDescription() != null) {
                        String temp;
                        byte[] bytes;
                        temp = event.getContentDescription().toString();
                        if(curr_lang == ENG)
                            last = preProcess(temp);
                        else{

                            last = preProcess(temp);

                        }
                        curr_line = 0;
                        curr_para = 0;
                        split_lines(new String(last));
                        split_paras(new String(last));
                        Log.v("test", lines[0]);
                        Log.v("test", paras[0]);
                        curr_line = 0;
                        curr_para = 0;
                        eventText+= last;
                    }
                    else{
                        switch(reading_type){
                            case 0:
                                eventText += last;
                                break;
                            case 1:
                                eventText += lines[curr_line];
                                break;
                            case 2:
                                eventText += paras[curr_para];
                                break;
                        }
                    }
                    if(curr_lang == ENG)
                        tts.speak(eventText, TextToSpeech.QUEUE_FLUSH, null);
                    else if(curr_lang == TEL)
                        speak_telugu(eventText);
                }
                else{
                    String temp,temp0 ;
                    temp0 = event.getText().toString();
                    temp = preProcessTelugu(temp0);
                    //eventText = eventText + temp;

                    Log.v(TAG,"check text0-"+ temp0);
                    Log.v(TAG,"check text "+ temp);
                    //read line by line
                    nLines = 0;
                    tel_curr_line2=0;
                    split_lines(temp);
                    if(curr_lang ==ENG) split_paras(temp0);
                    for (int i =0;i<nLines;i++){
                        //if(stopSpeechFlag)  break;
                        //speakAnyLanguage(lines[i]);
                        //loop till mp is busy
                        Log.i(TAG,String.format("line - %d = %s ",i,lines[i]));
                        //speakAnyLanguage(lines[i]);

                    }
                    stopSpeechFlag = false;
                    speakAnyLanguage(temp);
                }
                break;
            }

    }

    @Override
    public void onInterrupt() {
        Log.v(TAG, "onInterrupt ");
        //
    }
    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
        if (mTtsInitialized) {
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    /*
        Initialise TTS and copy assets for flite HTS
     */
    public void onInit(int status) {
        Log.v(TAG, "TTsinit successfully");
        if(status == TextToSpeech.SUCCESS){
            mTtsInitialized = true;
            tts.setLanguage(Locale.ENGLISH);
        }
        if (!PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext())
                .getBoolean("installed", false)) {
            PreferenceManager.getDefaultSharedPreferences(
                    getApplicationContext())
                    .edit().putBoolean("installed", true).commit();
            copyAssests();
        }
    }

    @Override
    /*
        input-  gesture id indicating the type of gesture on screen
        description - Perform various actions based on the type of the gesture
     */
    protected boolean onGesture(int gestureId) {
        Log.v(TAG, String.format("Gesture id - %d\n", gestureId));
        switch(gestureId){
            case AccessibilityService.GESTURE_SWIPE_UP :
                tts.stop();
                reading_type = (reading_type+1)%3;
                switch (reading_type){
                    case 0:
                        tts.speak("default", TextToSpeech.QUEUE_FLUSH, null);
                        break;
                    case 1:
                        tts.speak("lines", TextToSpeech.QUEUE_FLUSH, null);
                        break;
                    case 2:
                        tts.speak("paras", TextToSpeech.QUEUE_FLUSH, null);
                        break;
                }
                break;
            case AccessibilityService.GESTURE_SWIPE_LEFT :
                switch(reading_type){
                    case 0:
                        tts.stop();
                        break;
                    //go to previous line
                    case 1:
                        if(curr_lang == ENG){
                            if(curr_line == 0)  curr_line = nLines-1;
                            else curr_line--;
                            tts.speak(lines[(curr_line)%nLines],TextToSpeech.QUEUE_FLUSH, null);
                            break;
                        }else{
                            if(tel_curr_line2 == 0)  tel_curr_line2 = nLines-1;
                            else tel_curr_line2--;
                            try {
                                synthesisWavInBackground2(tel_curr_line2);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    //goto previous para
                    case 2:
                        if(curr_para == 0)  curr_para = nParas-1;
                        else curr_para--;
                        tts.speak(paras[(curr_para)%nParas],TextToSpeech.QUEUE_FLUSH, null);
                        break;
                }
                break;
            case AccessibilityService.GESTURE_SWIPE_RIGHT :
                switch(reading_type){
                    case 0:
                        //tts.stop();
                        stopAnyTTS();
                        break;
                    case 1:
                        //tts.speak(lines[(curr_line++)%nLines],TextToSpeech.QUEUE_FLUSH, null);
                        if(curr_lang == ENG)
                        speakAnyLanguage(lines[(curr_line++)%nLines]);
                        else
                            try {
                                synthesisWavInBackground2((tel_curr_line2++)%nLines);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        break;
                    case 2:
                        //tts.speak(paras[(curr_para++)%nParas],TextToSpeech.QUEUE_FLUSH, null);
                        speakAnyLanguage(paras[(curr_para++)%nParas]);
                        break;
                }
                break;
            case AccessibilityService.GESTURE_SWIPE_DOWN :
                stopAnyTTS();
                break;
            // to pop up settings menu
            case AccessibilityService.GESTURE_SWIPE_DOWN_AND_LEFT:

//                Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
//                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(i);
                MyScreenReaderService.this.stopSelf();


                break;
            // to change language
            case AccessibilityService.GESTURE_SWIPE_DOWN_AND_RIGHT:
                if(curr_lang == ENG)    curr_lang = TEL;
                else if(curr_lang == TEL) curr_lang = ENG;
                break;
            case AccessibilityService.GESTURE_SWIPE_UP_AND_RIGHT:
                //speak_telugu(getResources().getString(R.string.tel));
                speak_telugu("హైదరాబాద్"); // debug text
                break;

        }
        return super.onGesture(gestureId);
    }

    /**
     * utilities for myAccessibilityService
     */
    private String getEventType(AccessibilityEvent event){
      return AccessibilityEvent.eventTypeToString(event.getEventType());
    }

    /**
     * @param event
     * @return string description of event
     */
    private String getEventText(AccessibilityEvent event){
        StringBuilder sb = new StringBuilder();
        for(CharSequence a: event.getText()){
            sb.append(a);
        }
        return sb.toString();
    }
    /*
        Split text into lines
     */
    private void split_lines(String text) {
        // split the text into lines
        lines = text.split("[.!?]");
        nLines = lines.length;

    }
    /*
       Split text into paras
    */
    private void split_paras(String text) {
        // split the text into paras
        paras = text.split("[\n]");
        nParas = paras.length;
    }
    /*
        Preprocess telugu text before passing to TTS
     */
    private String preProcessTelugu(String text){
        String inputtext = text.trim();
        inputtext=inputtext.replace("|",".");
        inputtext=inputtext.replace(" . ", " .");
        inputtext=inputtext.replaceAll("\\s+", " ");
        inputtext=inputtext.trim();
        if(inputtext.endsWith( " ." )){
            inputtext = inputtext.substring(0, inputtext.length() - 2);
        }else if(inputtext.endsWith( " . " )){
            inputtext = inputtext.substring(0, inputtext.length() - 3);
        }
        inputtext = inputtext.trim();
        return inputtext;

    }
    /*
        Preprocess before TTS for english text
     */
    private String preProcess(String text) {
        String ans;
        StringBuilder sb = new StringBuilder();
        //1. preprocess to create proper paras
        for (int i=0;i<text.length()-1;i++){
            char curr,next;
            curr = text.charAt(i);
            next = text.charAt(i+1);
            sb.append(curr);

            if(curr=='.'||curr=='!'||curr=='?'){
                if(next != ' '){
                    sb.append("\n");
                }
            }

            if(i==text.length()-2)  sb.append(next);
        }

        ans = sb.toString();
        //2. remove the front label if present
        String label = "Page Content: ";
        if(ans.length()>label.length()) {
            boolean flag = label.equals(ans.substring(0, label.length()));
            if (flag) ans = ans.substring(label.length());
        }

        return ans;
    }
    /*
        Stop English or Telugu TTS service
     */
    void stopAnyTTS(){
        stopSpeechFlag = true;
        if(curr_lang==ENG)  tts.stop();
        else stop_telugu_tts();
    }
    void speakAnyLanguage(String eventText){
        if(curr_lang == ENG)    tts.speak(eventText, TextToSpeech.QUEUE_FLUSH, null);
        else  speak_telugu(eventText);
    }
    /*
        Telugu Text to Speech for String msg
     */
    private void speak_telugu(String msg)  {
        mp_free = false;
        Log.i(TAG,"mp_free= false");
        if (msg != null) {
            displayText(msg);
            try {

                synthesisWavInBackground(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
    private void stop_telugu_tts() {
        mediaPlayer.reset();
        mediaPlayer.stop();
    }
    /*
        Generating the Waveform and playing using media player for Telugu text
     */
    private void synthesisWavInBackground(String msg) throws IOException {

        setData(lines[tel_curr_line]);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
                mp_free = true;
                Log.i(TAG, "mp_free= true1");
                if(!stopSpeechFlag)
                {
                    tel_curr_line++;
                    if (tel_curr_line < nLines) {
                        setData(lines[tel_curr_line]);

                        mediaPlayer.start();
                    } else {
                        tel_curr_line = 0;

                    }
                }
                else
                    tel_curr_line = 0;

            }
        });
        mediaPlayer.start();
        Log.i(TAG, "mp_free= true2");
    }
    private void synthesisWavInBackground2(int line_no) throws IOException {
        setData(lines[line_no]);
        //tel_curr_line2 = line_no;
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
            }
        });
        mediaPlayer.start();
    }
    /*
        Set data source for media player after making C call to the flite-hts
     */
    public void setData(String msg){
        String inputtext = msg.trim();
        inputtext=inputtext.replace("|",".");
        inputtext=inputtext.replace(" . ", " .");
        inputtext=inputtext.replaceAll("\\s+", " ");
        //inputtext=inputtext.replace("[","");
        inputtext=inputtext.trim();
        if(inputtext.endsWith( " ." )){
            inputtext = inputtext.substring(0, inputtext.length() - 2);
        }else if(inputtext.endsWith( " . " )){
            inputtext = inputtext.substring(0, inputtext.length() - 3);
        }
        inputtext = inputtext.trim();
        Log.i(TAG,String.format("in setData-line = %s",inputtext));
        String speaker_name = "iitm_telugu_old";
        //
        String foldername = Environment.getExternalStorageDirectory().getPath()+"/Android/data/"+getPackageName().toString()+"/";
        String filename = foldername+speaker_name+".htsvoice";
        String wavname = foldername+"1.wav";
        //
        Log.v(TAG,"inputtext= "+inputtext+"\nfilename= "+filename+"\nwavename= "+wavname);
        //mainfn(inputtext,filename,wavname);
        Toast.makeText(MyScreenReaderService.this,mainfn(inputtext,filename,wavname),Toast.LENGTH_SHORT).show();
        /////////////////////////////////////////////////////////////////////////////////
        //mediaPlayer = new MediaPlayer();

        Log.i("test", "media player reached hurray! ");
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(wavname);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

    }

    public native String mainfn(String s, String inputtext, String wavname);
    static
    {
        System.loadLibrary("mainfn");
    }
    /*
     * utilities for flite-HTS
     */
    public void copyAssests()
    {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String filename : files)
        {
            Log.i(TAG,"In CopyAssets "+filename);
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open(filename);
                String foldername= Environment.getExternalStorageDirectory().getPath()+"/Android/data/"+getPackageName().toString()+"/";
                File folder = new File(foldername);
                folder.mkdirs();
                File outfile = new File(foldername+filename);
                out = new FileOutputStream(outfile);
                copyFile(in, out);
                Log.i(TAG,"In copyAssets Entire Path"+foldername+filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }
    private void displayText(String msg) {
//        TextView inputTextView = (TextView) findViewById(
//                R.id.inputText);
//        inputTextView.setText("" + msg);
        // create toast
        Log.v("test",String.format("dude- %s\n",msg) );
    }

}
