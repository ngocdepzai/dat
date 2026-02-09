package hc.manager.datapp.service;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class GetBitmapFromUrl extends AsyncTask<String, Void, Bitmap> {
    private String _urlResouce;
    private Exception exception;

    public GetBitmapFromUrl(String urlResouce) {
        _urlResouce = urlResouce;
    }

    protected Bitmap doInBackground(String... urls) {
        URL url = null;
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            url = new URL(_urlResouce);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            input = connection.getInputStream();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return BitmapFactory.decodeStream(input);
    }

    protected void onPostExecute(Bitmap feed) {
        // TODO: check this.exception
        // TODO: do something with the feed
    }
}
