package hc.manager.datapp.service;

import android.os.AsyncTask;
import android.util.Log;

public class ResentDataToServerService extends AsyncTask<Void, Void, Boolean> {
    //    private TextToSpeech textToSpeech;
    public ResentDataToServerService() {
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {


            return true;
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
