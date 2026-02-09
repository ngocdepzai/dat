package hc.manager.datapp.service;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import hc.manager.datapp.models.InOutModel;
import hc.manager.datapp.utils.Constant;

public class SenderInOut extends AsyncTask<Void, Void, Boolean> {
    private InOutModel model;

    //    private TextToSpeech textToSpeech;
    public SenderInOut(InOutModel _model) {
        model = _model;
//        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
//            @Override
//            public void onInit(int status) {
//                setTextToSpeechLanguage();
//            }
//        });
    }

    //    private void printOutSupportedLanguages() {
//        Set<Locale> supportedLanguages = textToSpeech.getAvailableLanguages();
//        if(supportedLanguages != null){
//            for(Locale lag: supportedLanguages){
//
//            }
//        }
//    }
//    private void setTextToSpeechLanguage(){
//        Locale language = Locale.ENGLISH;
//        int result = textToSpeech.setLanguage(language);
//        Locale currentLanguage = textToSpeech.getVoice().getLocale();
//        CustomToast.makeText(_context, currentLanguage + "", 1000,1).show();
//    }
    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(Constant.RabbitServer);
            factory.setUsername(Constant.RabbitUser);
            factory.setPort(Constant.RabbitPort);
            factory.setAutomaticRecoveryEnabled(false);
            factory.setPassword(Constant.RabbitPwd);
            try {
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();
                // channel.queueDeclare("hello", false, false, false, null);
                Gson gson = new Gson();
                String json = gson.toJson(model);
                Log.w("RabbitMQ ", json);
                channel.basicPublish(Constant.RabbitExchange, Constant.RabbitQueueInOut, false, null, json.getBytes());
                Log.w("RabbitMQ ", "Success");
                String utteranceId = UUID.randomUUID().toString();
//                textToSpeech.speak("Bạn đã đăng nhập thành công học viên "+model.Name, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                channel.close();
                connection.close();
                return true;
            } catch (IOException ex) {
                Log.e("RabbitMQ IOException", ex.getMessage());
                return false;
            } catch (TimeoutException ex) {
                Log.e("RabbitMQ Exception", ex.getMessage());
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("RabbitMQ ", e.getMessage());
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean aVoid) {
        super.onPostExecute(aVoid);
    }
}
