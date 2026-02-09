package hc.manager.datapp.app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import org.apache.commons.beanutils.BeanUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SyntheticUserList {

    private static SyntheticUserList instance;
    public List<UserItem> usersList = new ArrayList<UserItem>();
    private SQLiteDatabase db;

    public static SyntheticUserList getInstance() {
        if (null == instance) {
            instance = new SyntheticUserList();
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
        usersList.clear();

        if (IsFileExists(Environment.getExternalStorageDirectory() + "/HC_DAT/users.db")) {
            db = SQLiteDatabase.openOrCreateDatabase(Environment.getExternalStorageDirectory() + "/HC_DAT/users.db", null);
        } else {
            db = SQLiteDatabase.openOrCreateDatabase(Environment.getExternalStorageDirectory() + "/HC_DAT/users.db", null);
            String sql = "CREATE TABLE TB_USERS(userid TEXT PRIMARY KEY,"
                    + "username TEXT,"
                    + "enlcon1 BLOB,"
                    + "enlcon2 BLOB,"
                    + "fp1 BLOB,"
                    + "fp2 BLOB,"
                    + "email TEXT,"
                    + "gender TEXT,"
                    + "name TEXT,"
                    + "phoneNumber TEXT,"
                    + "address TEXT,"
                    + "userType TEXT,"
                    + "code TEXT,"
                    + "trainingCenterId TEXT,"
                    + "avatarId TEXT,"
                    + "birthDay TEXT);";

            db.execSQL(sql);
        }
        ///*
        Cursor cursor = db.query("TB_USERS", null, null, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    //cursor.move(i);
                    UserItem ui = new UserItem();
                    ui.userid = cursor.getString(0);
                    ui.username = cursor.getString(1);
                    ui.enlcon1 = cursor.getBlob(2);
                    ui.enlcon2 = cursor.getBlob(3);
                    ui.fp1 = cursor.getBlob(4);
                    ui.fp2 = cursor.getBlob(5);
                    ui.email = cursor.getString(6);
//					ui.fp2=cursor.getString(5);
                    ui.name = cursor.getString(8);
                    ui.phoneNumber = cursor.getString(9);
                    ui.address = cursor.getString(10);
                    ui.userType = cursor.getString(11);
                    ui.code = cursor.getString(12);
                    ui.trainingCenterId = cursor.getString(13);
                    ui.avatarId = cursor.getString(14);
                    ui.birthDay = cursor.getString(15);
                    usersList.add(ui);
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

    public void ClearUsers() {
        usersList.clear();
        String sql = "delete from TB_USERS";
        db.execSQL(sql);
    }

    public void DeleteUser(String userid) {
        String sql = "delete from TB_USERS where userid=\"" + String.valueOf(userid) + "\"";
        db.execSQL(sql);
    }

    public void AppendUser(UserItem ui) {
        if (ui.fingerPrintId1 != null) {
            ui.fp1 = Base64.decode(ui.fingerPrintId1, 0);
            ui.fp2 = Base64.decode(ui.fingerPrintId1, 0);
        }
        ui.enlcon2[0] = 1;
        ui.enlcon1[0] = 1;
        usersList.add(ui);
        String sql = "insert into TB_USERS(userid,username,enlcon1,enlcon2,fp1,fp2,email,gender,name,phoneNumber,address,userType,code,trainingCenterId,avatarId,birthDay) "
                + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        Object[] args = new Object[]{ui.id, ui.username,
                ui.enlcon1, ui.enlcon2,
                ui.fp1, ui.fp2, ui.email, ui.gender, ui.name, ui.phoneNumber, ui.address, ui.userType, ui.code, ui.trainingCenterId, ui.avatarId, ui.birthDay};
        db.execSQL(sql, args);
    }

    public void AppendUser(Object obj) {
        try {
            Log.d("Info user 2", ": " + obj);
//			Map<String, String> properties = BeanUtils.describe(obj);
            UserItem ui = new UserItem();
            ui.fp1 = Base64.decode(BeanUtils.getProperty(obj, "fingerPrintId1"), 0);
            ui.fp2 = Base64.decode(BeanUtils.getProperty(obj, "fingerPrintId2"), 0);
            ui.enlcon2[0] = 1;
            ui.enlcon1[0] = 1;
            ui.username = BeanUtils.getProperty(obj, "username");
            ui.userid = BeanUtils.getProperty(obj, "id");
            ui.email = BeanUtils.getProperty(obj, "email");
            ui.gender = BeanUtils.getProperty(obj, "gender");
            ui.name = BeanUtils.getProperty(obj, "name");
            ui.phoneNumber = BeanUtils.getProperty(obj, "phoneNumber");
            ui.address = BeanUtils.getProperty(obj, "address");
            ui.userType = BeanUtils.getProperty(obj, "userType");
            ui.code = BeanUtils.getProperty(obj, "code");
            ui.trainingCenterId = BeanUtils.getProperty(obj, "trainingCenterId");
            ui.avatarId = BeanUtils.getProperty(obj, "avatarId");
            ui.birthDay = BeanUtils.getProperty(obj, "birthDay");
            usersList.add(ui);
            String sql = "insert into TB_USERS(userid,username,enlcon1,enlcon2,fp1,fp2,email,gender,name,phoneNumber,address,userType,code,trainingCenterId,avatarId,birthDay) "
                    + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            Object[] args = new Object[]{ui.userid, ui.username,
                    ui.enlcon1, ui.enlcon2,
                    ui.fp1, ui.fp2, ui.email, ui.gender, ui.name, ui.phoneNumber, ui.address, ui.userType, ui.code, ui.trainingCenterId, ui.avatarId, ui.birthDay};
            db.execSQL(sql, args);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("SQLite save error", ": " + e.toString());
        }

    }

    public void AppendUser(byte[] tempInfo, byte[] tempFP) {
//		UserItem ui=new UserItem();
//
//		ui.userid=get(tempInfo[1],tempInfo[0]);
//		ui.usertype=tempInfo[2];
//		ui.groupid=tempInfo[3];
//		ui.username=new String();
//		try {
//			ui.username=new String(tempInfo, 4, 16,"gb2312");
//		} catch (UnsupportedEncodingException e) {
//		}
//		ui.username=ui.username.replaceAll("\\s","");
//		System.arraycopy(tempInfo, 20, ui.expdate,0, 3);
//		System.arraycopy(tempInfo, 23, ui.enlcon1,0, 5);
//		System.arraycopy(tempInfo, 28, ui.enlcon2,0, 5);
//		System.arraycopy(tempInfo, 33, ui.enlcon3,0, 5);
//
//		int fpcount=0;
//		if(ui.enlcon1[0]==1){
//			System.arraycopy(tempFP, 512*fpcount, ui.fp1,0, 512);
//			fpcount++;
//		}
//		if(ui.enlcon2[0]==1){
//			System.arraycopy(tempFP, 512*fpcount, ui.fp2,0, 512);
//			fpcount++;
//		}
//		if(ui.enlcon3[0]==1){
//			System.arraycopy(tempFP, 512*fpcount, ui.fp3,0, 512);
//			fpcount++;
//		}
//		usersList.add(ui);
//
//		String sql="insert into TB_USERS(userid,usertype,groupid,username,expdate,enlcon1,enlcon2,enlcon3,fp1,fp2,fp3) "
//				+ "values(?,?,?,?,?,?,?,?,?,?,?)";
//		Object[] args = new Object[]{ui.userid,ui.usertype,ui.groupid,ui.username,
//							ui.expdate,
//							ui.enlcon1,ui.enlcon2,ui.enlcon3,
//							ui.fp1,ui.fp2,ui.fp3};
//		db.execSQL(sql,args);
    }


    public UserItem FindUserItemByFP1(String id) {
        for (int i = 0; i < usersList.size(); i++) {
            if (usersList.get(i).userid == id) {
                return usersList.get(i);
            }
        }
        return null;
    }

    public boolean UserExist(String id) {
        for (int i = 0; i < usersList.size(); i++) {
            if (id != null) {
                if (usersList.get(i).userid.equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean bytesEquals(byte[] src, int spos, byte[] dst, int dpos, int size) {
        for (int i = 0; i < size; i++) {
            if (src[spos + i] != dst[dpos + i])
                return false;
        }
        return true;
    }

    public UserItem FindUserItemByCard(byte[] cardsn) {
        for (int i = 0; i < usersList.size(); i++) {
            if (usersList.get(i).enlcon1[0] == 2) {
                if (bytesEquals(cardsn, 0, usersList.get(i).enlcon1, 1, 4)) {
                    return usersList.get(i);
                }
            }
            if (usersList.get(i).enlcon2[0] == 2) {
                if (bytesEquals(cardsn, 0, usersList.get(i).enlcon2, 1, 4)) {
                    return usersList.get(i);
                }
            }
        }
        return null;
    }

    //	public UserItem FindUserItemByCardex(byte[] cardsn){
//		for(int i=0;i<usersList.size();i++){
//				if(bytesEquals(cardsn,0,usersList.get(i).enllNO,0,4)){
//					return usersList.get(i);
//				}
//		}
//		return null;
//	}
//
    public boolean UserIsExists(String id) {
        for (int i = 0; i < usersList.size(); i++) {
            if (usersList.get(i).userid == id)
                return true;
        }
        return false;
    }
}
