package hc.manager.datapp;

import android.app.Activity;
import android.app.Application;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {
    public Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            Toast.makeText(getApplicationContext(), "Xin lỗi", Toast.LENGTH_SHORT).show();
            String info = "";
            ByteArrayOutputStream baos = null;
            PrintStream printStream = null;
            try {
                baos = new ByteArrayOutputStream();
                printStream = new PrintStream(baos);
                ex.printStackTrace(printStream);
                byte[] data = baos.toByteArray();
                // android.os.Process.killProcess(android.os.Process.myPid());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (printStream != null) {
                        printStream.close();
                    }

                    if (baos != null) {
                        baos.close();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }
    };
    List<Activity> list = new ArrayList<Activity>();

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
