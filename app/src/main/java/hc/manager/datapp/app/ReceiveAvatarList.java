package hc.manager.datapp.app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.beanutils.BeanUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReceiveAvatarList {

    private static ReceiveAvatarList instance;
    public List<ReceiveAvatarItem> receiveAvatarList = new ArrayList<ReceiveAvatarItem>();
    private SQLiteDatabase db;

    public static ReceiveAvatarList getInstance() {
        if (null == instance) {
            instance = new ReceiveAvatarList();
        }
        return instance;
    }

    public static boolean IsFileExists(String filename) {
        File f = new File(filename);
        if (f.exists()) {
            return true;
        }
        return false;
    }

    public void LoadAll() {
        receiveAvatarList.clear();

        if (IsFileExists(Environment.getExternalStorageDirectory() + "/HC_DAT/receiveAvatar.db")) {
            db = SQLiteDatabase.openOrCreateDatabase(Environment.getExternalStorageDirectory() + "/HC_DAT/receiveAvatar.db", null);
        } else {
            db = SQLiteDatabase.openOrCreateDatabase(Environment.getExternalStorageDirectory() + "/HC_DAT/receiveAvatar.db", null);
            String sql = "CREATE TABLE TB_RECEIVES(avatarId TEXT PRIMARY KEY,"
                    + "userCode TEXT);";

            db.execSQL(sql);
        }
        ///*
        Cursor cursor = db.query("TB_RECEIVES", null, null, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    //cursor.move(i);
                    ReceiveAvatarItem ui = new ReceiveAvatarItem();
                    ui.avatarId = cursor.getString(0);
                    ui.userCode = cursor.getString(1);
                    receiveAvatarList.add(ui);
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
        ///*/
    }

    private short getShort(byte b1, byte b2) {
        short temp = 0;
        temp |= (b1 & 0xff);
        temp <<= 8;
        temp |= (b2 & 0xff);
        return temp;
    }

    public void ClearReceive() {
        receiveAvatarList.clear();
        String sql = "delete from TB_RECEIVES";
        db.execSQL(sql);
    }

    public void DeleteReceiver(String avatarId) {
//        String sql = "delete from TB_RECEIVES where avatarId=" + String.valueOf(avatarId);
        db.delete("TB_RECEIVES", "avatarId=?", new String[]{avatarId});
        // db.execSQL(sql);
    }

    public void AppendReceiver(ReceiveAvatarItem ui) {
        receiveAvatarList.add(ui);
        String sql = "insert into TB_RECEIVES(avatarId,userCode) "
                + "values(?,?)";
        Object[] args = new Object[]{ui.avatarId, ui.userCode};
        db.execSQL(sql, args);
    }

    public void AppendReceiver(Object obj) {
        try {
            Log.d("Info user 2", ": " + obj);
//			Map<String, String> properties = BeanUtils.describe(obj);
            ReceiveAvatarItem ui = new ReceiveAvatarItem();
            ui.avatarId = BeanUtils.getProperty(obj, "avatarId");
            ui.userCode = BeanUtils.getProperty(obj, "userCode");
            receiveAvatarList.add(ui);
            String sql = "insert into TB_RECEIVES(avatarId,userCode) "
                    + "values(?,?)";
            Object[] args = new Object[]{ui.avatarId, ui.userCode};
            db.execSQL(sql, args);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("SQLite save error", ": " + e.toString());
        }

    }

    private boolean bytesEquals(byte[] src, int spos, byte[] dst, int dpos, int size) {
        for (int i = 0; i < size; i++) {
            if (src[spos + i] != dst[dpos + i])
                return false;
        }
        return true;
    }
}
