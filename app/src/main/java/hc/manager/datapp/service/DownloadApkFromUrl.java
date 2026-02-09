package hc.manager.datapp.service;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import hc.manager.datapp.models.InOutModel;
import hc.manager.datapp.utils.Constant;

public class DownloadApkFromUrl extends AsyncTask<Void, Void, Boolean> {
    private InOutModel model;

    public DownloadApkFromUrl(InOutModel _model) {
        model = _model;
    }

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
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean aVoid) {
        super.onPostExecute(aVoid);
    }
}
