package hc.manager.datapp.service;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import hc.manager.datapp.models.AuthModel;
import hc.manager.datapp.utils.Constant;

public class SenderAuth extends AsyncTask<Void, Void, Boolean> {
    private AuthModel model;

    public SenderAuth(AuthModel _model) {
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
                Log.d("RabbitMQ ", json);
                channel.basicPublish(Constant.RabbitExchange, Constant.RabbitQueueAuth, false, null, json.getBytes());
                Log.w("RabbitMQ ", "Success");
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
