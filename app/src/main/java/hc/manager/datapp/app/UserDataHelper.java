package hc.manager.datapp.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Base64;

import androidx.annotation.RequiresApi;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hc.manager.datapp.models.AuthModel;
import hc.manager.datapp.models.GpsModel;
import hc.manager.datapp.models.InOutModel;
import hc.manager.datapp.utils.DateUtil;

public class UserDataHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "HC_DAT_DATABASE";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_USER = "TB_USERS";
    private static final String TABLE_AUTH = "TB_AUTHS_NEW";
    private static final String TABLE_ATTENDANCE = "TABLE_ATTENDANCES_NEW";
    private static final String TABLE_GPS = "TABLE_GPSES_NEW";
    public List<InOutModel> attendanceList = new ArrayList<InOutModel>();
    public List<AuthModel> authList = new ArrayList<AuthModel>();
    public List<AuthModel> authListResent = new ArrayList<AuthModel>();
    public List<UserItem> usersList = new ArrayList<UserItem>();
    public List<UserItem> usersListHaveFace = new ArrayList<UserItem>();
    public List<UserItem> usersListHaveCard = new ArrayList<UserItem>();
    public List<UserItem> usersListHaveFinger = new ArrayList<UserItem>();
    public List<GpsModel> gpsList = new ArrayList<GpsModel>();
    public String imeiDevice = "";
    android.text.format.DateFormat df = new android.text.format.DateFormat();
    private Context _context;
    private Date dateNow = new Date();
    private String sDateNow = (String) df.format("dd-MM-yyyy", dateNow);

    public UserDataHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        _context = context;
    }

    public static boolean IsFileExists(String filename) {
        File f = new File(filename);
        if (f.exists()) {
            return true;
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String getImeiDevice() {
        if (isFP()) {
            imeiDevice = android.os.Build.getSerial();
        } else {
            TelephonyManager telephonyManager = (TelephonyManager) _context.getSystemService(Context.TELEPHONY_SERVICE);
            imeiDevice = telephonyManager.getDeviceId();
        }
        return imeiDevice;
    }

    public boolean isFP() {
        String modelName = android.os.Build.MODEL;
        if (modelName.contains("FP")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Drop table
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUTH);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTENDANCE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GPS);
        if (!isTableExists(TABLE_AUTH, db)) {
            String sql4 = "CREATE TABLE " + TABLE_AUTH + "(Time INTEGER PRIMARY KEY,"
                    + "Seri TEXT,"
                    + "UserCode TEXT,"
                    + "Status TEXT,"
                    + "FilePath TEXT,"
                    + "Lat TEXT,"
                    + "Lng TEXT,"
                    + "Dis INTEGER,"
                    + "Sent INTEGER,"
                    + "SessionId TEXT,"
                    + "TeacherCode TEXT)";

            db.execSQL(sql4);
        }
        if (!isTableExists(TABLE_USER, db)) {
            String sql3 = "CREATE TABLE " + TABLE_USER + "(userid TEXT PRIMARY KEY,"
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
                    + "birthDay TEXT,"
                    + "faceToken TEXT,"
                    + "totalTime INTEGER,"
                    + "totalDis INTEGER,"
                    + "idCard TEXT,"
                    + "courseCode TEXT,"
                    + "driverNo TEXT,"
                    + "idNo TEXT,"
                    + "totalCourseTime INTEGER,"
                    + "totalCourseDis INTEGER);";

            db.execSQL(sql3);
        }
        if (!isTableExists(TABLE_ATTENDANCE, db)) {
// Recreate
            String sql = "CREATE TABLE " + TABLE_ATTENDANCE + "(Time INTEGER PRIMARY KEY,"
                    + "Type INTEGER,"
                    + "Seri TEXT,"
                    + "UserCode TEXT,"
                    + "UserId TEXT,"
                    + "Name TEXT,"
                    + "Lat TEXT,"
                    + "Lng TEXT,"
                    + "LoginType INTEGER,"
                    + "Sent INTEGER,"
                    + "UserType INTEGER,"
                    + "FilePath TEXT,"
                    + "FilePathLocal TEXT)";
            db.execSQL(sql);
        }
        if (!isTableExists(TABLE_GPS, db)) {
            String sql2 = "CREATE TABLE " + TABLE_GPS + "(Time INTEGER PRIMARY KEY,"
                    + "Status INTEGER,"
                    + "UserCode TEXT,"
                    + "Dir TEXT,"
                    + "Dis INTEGER,"
                    + "Lat TEXT,"
                    + "Lng TEXT,"
                    + "Vel INTEGER,"
                    + "GpsStatus INTEGER,"
                    + "GsmStatus INTEGER,"
                    + "Seri TEXT,"
                    + "Sent INTEGER,"
                    + "SessionId TEXT,"
                    + "TeacherCode TEXT)";
            db.execSQL(sql2);
        }
        onCreate(db);
    }

    public ArrayList<GpsModel> LoadDataResentGps() {
        ArrayList<GpsModel> listResent = new ArrayList<GpsModel>();
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ///*
            if (isTableExists(TABLE_ATTENDANCE, db)) {
                Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_GPS + " where Sent = 0", null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        for (int i = 0; i < cursor.getCount(); i++) {
                            //cursor.move(i);
                            GpsModel gpsModel = new GpsModel();
                            gpsModel.Time = cursor.getLong(0);
                            gpsModel.Status = cursor.getString(1);
                            gpsModel.UserCode = cursor.getString(2);
                            gpsModel.Dir = cursor.getString(3);
                            gpsModel.Dis = cursor.getLong(4);
                            gpsModel.Lat = cursor.getDouble(5);
                            gpsModel.Lng = cursor.getDouble(6);
                            gpsModel.Vel = cursor.getLong(7);
                            gpsModel.GpsStatus = cursor.getInt(8);
                            gpsModel.GsmStatus = cursor.getInt(9);
                            gpsModel.Seri = cursor.getString(10);
                            gpsModel.Sent = cursor.getInt(11);
                            gpsModel.SessionId = cursor.getString(12);
                            gpsModel.TeacherCode = cursor.getString(13);
                            listResent.add(gpsModel);
                            cursor.moveToNext();
                        }
                    }
                    cursor.close();
                }
            }
            return listResent;
        } catch (Exception e) {
            e.printStackTrace();
            return listResent;
        }
    }

    public void LoadAllGps() {
        gpsList.clear();

        SQLiteDatabase db = this.getWritableDatabase();
        ///*
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_GPS, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    //cursor.move(i);
                    GpsModel gpsModel = new GpsModel();
                    gpsModel.Time = cursor.getLong(0);
                    gpsModel.Status = cursor.getString(1);
                    gpsModel.UserCode = cursor.getString(2);
                    gpsModel.Dir = cursor.getString(3);
                    gpsModel.Dis = cursor.getLong(4);
                    gpsModel.Lat = cursor.getDouble(5);
                    gpsModel.Lng = cursor.getDouble(6);
                    gpsModel.Vel = cursor.getLong(7);
                    gpsModel.GpsStatus = cursor.getInt(8);
                    gpsModel.GsmStatus = cursor.getInt(9);
                    gpsModel.Seri = cursor.getString(10);
                    gpsModel.Sent = cursor.getInt(11);
                    gpsModel.SessionId = cursor.getString(12);
                    gpsModel.TeacherCode = cursor.getString(13);
                    gpsList.add(gpsModel);
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }

    }

    public void UpdateResentGps(long time) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            String sql = "UPDATE " + TABLE_GPS + " set Sent = 1 where Time=" + time + "";
            db.execSQL(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void AppendGps(GpsModel gpsModel) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            gpsList.add(gpsModel);
            String sql = "insert into " + TABLE_GPS + "(Time,Status,UserCode,Dir,Dis,Lat,Lng,Vel,GpsStatus,GsmStatus,Seri,Sent,SessionId,TeacherCode) "
                    + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            Object[] args = new Object[]{gpsModel.Time, gpsModel.Status,
                    gpsModel.UserCode, gpsModel.Dir, gpsModel.Dis, gpsModel.Lat, gpsModel.Lng, gpsModel.Vel, gpsModel.GpsStatus, gpsModel.GsmStatus, gpsModel.Seri, gpsModel.Sent, gpsModel.SessionId, gpsModel.TeacherCode
            };
            db.execSQL(sql, args);
        } catch (Exception ex) {
            CreateTabelGps();
            AppendGps(gpsModel);
            ex.printStackTrace();
        }

    }

    public void BackUpGps() {
        try {

            ArrayList<GpsModel> listResentBackup = new ArrayList<GpsModel>();

            SQLiteDatabase db = this.getWritableDatabase();
            Date dateNotTime = new Date(dateNow.getYear(), dateNow.getMonth(), dateNow.getDate());
            long timeCheck = dateNotTime.getTime() / 1000;
            ///*
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_GPS + " where Time > " + timeCheck, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    for (int i = 0; i < cursor.getCount(); i++) {
                        //cursor.move(i);
                        GpsModel gpsModel = new GpsModel();
                        gpsModel.Time = cursor.getLong(0);
                        gpsModel.Status = cursor.getString(1);
                        gpsModel.UserCode = cursor.getString(2);
                        gpsModel.Dir = cursor.getString(3);
                        gpsModel.Dis = cursor.getLong(4);
                        gpsModel.Lat = cursor.getDouble(5);
                        gpsModel.Lng = cursor.getDouble(6);
                        gpsModel.Vel = cursor.getLong(7);
                        gpsModel.GpsStatus = cursor.getInt(8);
                        gpsModel.GsmStatus = cursor.getInt(9);
                        gpsModel.Seri = cursor.getString(10);
                        gpsModel.Sent = cursor.getInt(11);
                        gpsModel.SessionId = cursor.getString(12);
                        gpsModel.TeacherCode = cursor.getString(13);
                        listResentBackup.add(gpsModel);
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
            Workbook wb = new HSSFWorkbook();
            Cell cell = null;
            Sheet sheet = null;
            sheet = wb.createSheet(sDateNow);
            Row row = sheet.createRow(0);
            cell = row.createCell(0);
            cell.setCellValue("Mã học viên");
            cell = row.createCell(1);
            cell.setCellValue("Thời điểm");

            cell = row.createCell(2);
            cell.setCellValue("Lat");

            cell = row.createCell(3);
            cell.setCellValue("Lng");

            cell = row.createCell(4);
            cell.setCellValue("Seri thiết bị");

            cell = row.createCell(5);
            cell.setCellValue("Vận tốc");

            cell = row.createCell(6);
            cell.setCellValue("Quãng đường");

            cell = row.createCell(7);
            cell.setCellValue("Trạng thái gsm");

            cell = row.createCell(8);
            cell.setCellValue("Trạng thái gps");
            sheet.setColumnWidth(0, (30 * 200));
            sheet.setColumnWidth(1, (30 * 200));
            sheet.setColumnWidth(2, (30 * 200));
            sheet.setColumnWidth(3, (30 * 200));
            sheet.setColumnWidth(4, (30 * 200));
            sheet.setColumnWidth(5, (30 * 200));
            sheet.setColumnWidth(6, (30 * 200));
            sheet.setColumnWidth(7, (30 * 200));
            sheet.setColumnWidth(8, (30 * 200));
            for (int i = 0; i < listResentBackup.size(); i++) {
                Row row1 = sheet.createRow(i + 1);
                GpsModel gpsModel = listResentBackup.get(i);
                cell = row1.createCell(0);
                cell.setCellValue(gpsModel.getUserCode());
                cell = row1.createCell(1);
                Date dateTIme = new Date(gpsModel.getTime() * 1000 + 5 * 60 * 60 * 1000);
                SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
                String dateFormat = format.format(dateTIme);
                cell.setCellValue(dateFormat);

                cell = row1.createCell(2);
                cell.setCellValue(gpsModel.getLat());

                cell = row1.createCell(3);
                cell.setCellValue(gpsModel.getLng());

                cell = row1.createCell(4);
                cell.setCellValue(gpsModel.getSeri());

                cell = row1.createCell(5);
                cell.setCellValue(gpsModel.getVel());

                cell = row1.createCell(6);
                cell.setCellValue(gpsModel.Dis);

                cell = row1.createCell(7);
                cell.setCellValue(gpsModel.getGsmStatus());

                cell = row1.createCell(8);
                cell.setCellValue(gpsModel.getGpsStatus());
                sheet.setColumnWidth(0, (30 * 200));
                sheet.setColumnWidth(1, (30 * 200));
                sheet.setColumnWidth(2, (30 * 200));
                sheet.setColumnWidth(3, (30 * 200));
                sheet.setColumnWidth(4, (30 * 200));
                sheet.setColumnWidth(5, (30 * 200));
                sheet.setColumnWidth(6, (30 * 200));
                sheet.setColumnWidth(7, (30 * 200));
                sheet.setColumnWidth(8, (30 * 200));
            }
            File filepath = Environment.getExternalStorageDirectory();
            File dir = new File(filepath.getAbsolutePath()
                    + "/HC_DAT_BACKUP/GPS/");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String fileName = sDateNow + ".csv";
            File fileXacthuc = new File(dir, fileName);
            if (fileXacthuc.exists()) {
                fileXacthuc.delete();
            }
            FileOutputStream outputStream = null;
            outputStream = new FileOutputStream(fileXacthuc.getPath());
            wb.write(outputStream);
        } catch (IOException ex) {
            CreateTabelGps();
            ex.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void BackupTeacher() {
        Date dateNotTime = new Date(dateNow.getYear(), dateNow.getMonth(), dateNow.getDate());
        long timeCheck = dateNotTime.getTime() / 1000;
        ArrayList<InOutModel> listBackup = new ArrayList<InOutModel>();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ATTENDANCE + " where UserType = 2 and Time > " + timeCheck, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    //cursor.move(i);
                    InOutModel inOutModel = new InOutModel();
                    inOutModel.Time = cursor.getLong(0);
                    inOutModel.Type = cursor.getInt(1);
                    inOutModel.Seri = cursor.getString(2);
                    inOutModel.UserCode = cursor.getString(3);
                    inOutModel.UserId = cursor.getString(4);
                    inOutModel.Name = cursor.getString(5);
                    inOutModel.Lat = cursor.getDouble(6);
                    inOutModel.Lng = cursor.getDouble(7);
                    inOutModel.LoginType = cursor.getInt(8);
                    inOutModel.Sent = cursor.getInt(9);
                    inOutModel.UserType = cursor.getInt(10);
                    inOutModel.FilePath = cursor.getString(11);
                    inOutModel.FilePathLocal = cursor.getString(12);
                    listBackup.add(inOutModel);
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
        GenerateFileBackup(listBackup, 2);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void BackupStudent() {
        Date dateNotTime = new Date(dateNow.getYear(), dateNow.getMonth(), dateNow.getDate());
        long timeCheck = dateNotTime.getTime() / 1000;
        ArrayList<InOutModel> listBackup = new ArrayList<InOutModel>();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ATTENDANCE + " where UserType = 1 and Time > " + timeCheck, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    //cursor.move(i);
                    InOutModel inOutModel = new InOutModel();
                    inOutModel.Time = cursor.getLong(0);
                    inOutModel.Type = cursor.getInt(1);
                    inOutModel.Seri = cursor.getString(2);
                    inOutModel.UserCode = cursor.getString(3);
                    inOutModel.UserId = cursor.getString(4);
                    inOutModel.Name = cursor.getString(5);
                    inOutModel.Lat = cursor.getDouble(6);
                    inOutModel.Lng = cursor.getDouble(7);
                    inOutModel.LoginType = cursor.getInt(8);
                    inOutModel.Sent = cursor.getInt(9);
                    inOutModel.UserType = cursor.getInt(10);
                    inOutModel.FilePath = cursor.getString(11);
                    inOutModel.FilePathLocal = cursor.getString(12);
                    listBackup.add(inOutModel);
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
        GenerateFileBackup(listBackup, 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void GenerateFileBackup(ArrayList<InOutModel> listBackup, int type) {
        Workbook wb = new HSSFWorkbook();
        Cell cell = null;
        Sheet sheet = null;
        sheet = wb.createSheet(sDateNow);
        Row row = sheet.createRow(0);
        cell = row.createCell(0);
        if (type == 1) {
            cell.setCellValue("Mã học viên");
        } else {
            cell.setCellValue("Mã giảng viên");
        }
        cell = row.createCell(1);
        if (type == 1) {
            cell.setCellValue("Tên học viên");
        } else {
            cell.setCellValue("Tên giảng viên");
        }
        cell = row.createCell(2);
        cell.setCellValue("Thời điểm");

        cell = row.createCell(3);
        cell.setCellValue("Lat");

        cell = row.createCell(4);
        cell.setCellValue("Lng");

        cell = row.createCell(5);
        cell.setCellValue("Seri thiết bị");

        cell = row.createCell(6);
        cell.setCellValue("Kiểu đăng nhập");

        cell = row.createCell(7);
        cell.setCellValue("Đăng nhập/ Đăng xuất");

        cell = row.createCell(8);
        cell.setCellValue("Hình ảnh");

        sheet.setColumnWidth(0, (30 * 200));
        sheet.setColumnWidth(1, (30 * 200));
        sheet.setColumnWidth(2, (30 * 200));
        sheet.setColumnWidth(3, (30 * 200));
        sheet.setColumnWidth(4, (30 * 200));
        sheet.setColumnWidth(5, (30 * 200));
        sheet.setColumnWidth(6, (30 * 200));
        sheet.setColumnWidth(7, (30 * 200));
        sheet.setColumnWidth(8, (30 * 200));
        for (int i = 0; i < listBackup.size(); i++) {
            Row row1 = sheet.createRow(i + 1);
            row1.setHeight((short) 1500);
            InOutModel inOutModel = listBackup.get(i);
            cell = row1.createCell(0);
            cell.setCellValue(inOutModel.getUserCode());
            cell = row1.createCell(1);
            cell.setCellValue(inOutModel.getName());
            cell = row1.createCell(2);
            Date dateTIme = new Date(inOutModel.getTime() * 1000 + 5 * 60 * 60 * 1000);
            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
            String dateFormat = format.format(dateTIme);
            cell.setCellValue(dateFormat);

            cell = row1.createCell(3);
            cell.setCellValue(inOutModel.getLat());

            cell = row1.createCell(4);
            cell.setCellValue(inOutModel.getLng());

            cell = row1.createCell(5);
            cell.setCellValue(inOutModel.getSeri());

            cell = row1.createCell(6);

            String loginTypeString = "Khuôn mặt";
            if (inOutModel.getType() == 2) {
                loginTypeString = "";
            } else {
                if (inOutModel.getLoginType() == 2) {
                    loginTypeString = "Vân tay";
                }
                if (inOutModel.getLoginType() == 3) {
                    loginTypeString = "Thẻ";
                }
            }
            cell.setCellValue(loginTypeString);

            String typeString = "Đăng nhập";
            if (inOutModel.getType() == 2) {
                typeString = "Đăng xuất";
            }
            cell = row1.createCell(7);
            cell.setCellValue(typeString);

            try {
                CreationHelper factory = sheet.getWorkbook().getCreationHelper();
                Path path = Paths.get(inOutModel.getFilePathLocal());
                byte[] raw = java.nio.file.Files.readAllBytes(path);
                int my_picture_id = wb.addPicture(raw, Workbook.PICTURE_TYPE_PNG);
                Drawing drawing = sheet.createDrawingPatriarch();
                ClientAnchor my_anchor = factory.createClientAnchor();
                my_anchor.setCol1(8); //Column B
                my_anchor.setRow1(i + 1); //Row 4
                my_anchor.setCol2(9); //Column C
                my_anchor.setRow2(i + 2); //Row 4
                Picture my_picture = drawing.createPicture(my_anchor, my_picture_id);
            } catch (IOException e) {
                e.printStackTrace();
            }

            sheet.setColumnWidth(0, (30 * 200));
            sheet.setColumnWidth(1, (30 * 200));
            sheet.setColumnWidth(2, (30 * 200));
            sheet.setColumnWidth(3, (30 * 200));
            sheet.setColumnWidth(4, (30 * 200));
            sheet.setColumnWidth(5, (30 * 200));
            sheet.setColumnWidth(6, (30 * 200));
            sheet.setColumnWidth(7, (30 * 200));
            sheet.setColumnWidth(8, (30 * 200));
        }
        File filepath = Environment.getExternalStorageDirectory();
        String pathFolder = "";
        if (type == 1) {
            pathFolder = "/HC_DAT_BACKUP/DIEM_DANH/HOC_VIEN/";
        } else {
            pathFolder = "/HC_DAT_BACKUP/DIEM_DANH/GIANG_VIEN/";
        }
        File dir = new File(filepath.getAbsolutePath()
                + pathFolder);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = sDateNow + ".csv";
        File fileXacthuc = new File(dir, fileName);
        if (fileXacthuc.exists()) {
            fileXacthuc.delete();
        }
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(fileXacthuc.getPath());
            wb.write(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<InOutModel> LoadResentDataAttendance() {

        ArrayList<InOutModel> resentData = new ArrayList<InOutModel>();
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ///*
            if (isTableExists(TABLE_ATTENDANCE, db)) {
                Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ATTENDANCE + " where Sent = 0", null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        for (int i = 0; i < cursor.getCount(); i++) {
                            //cursor.move(i);
                            InOutModel inOutModel = new InOutModel();
                            inOutModel.Time = cursor.getLong(0);
                            inOutModel.Type = cursor.getInt(1);
                            inOutModel.Seri = cursor.getString(2);
                            inOutModel.UserCode = cursor.getString(3);
                            inOutModel.UserId = cursor.getString(4);
                            inOutModel.Name = cursor.getString(5);
                            inOutModel.Lat = cursor.getDouble(6);
                            inOutModel.Lng = cursor.getDouble(7);
                            inOutModel.LoginType = cursor.getInt(8);
                            inOutModel.Sent = cursor.getInt(9);
                            inOutModel.UserType = cursor.getInt(10);
                            inOutModel.FilePath = cursor.getString(11);
                            inOutModel.FilePathLocal = cursor.getString(12);
                            resentData.add(inOutModel);
                            cursor.moveToNext();
                        }
                    }
                    cursor.close();
                }
            }
            return resentData;
        } catch (Exception e) {
            e.printStackTrace();
            return resentData;
        }
    }

    public void CreateTabelAtten() {
        SQLiteDatabase db = this.getWritableDatabase();
        if (!isTableExists(TABLE_ATTENDANCE, db)) {
            String sql = "CREATE TABLE " + TABLE_ATTENDANCE + "(Time INTEGER PRIMARY KEY,"
                    + "Type INTEGER,"
                    + "Seri TEXT,"
                    + "UserCode TEXT,"
                    + "UserId TEXT,"
                    + "Name TEXT,"
                    + "Lat TEXT,"
                    + "Lng TEXT,"
                    + "LoginType INTEGER,"
                    + "Sent INTEGER,"
                    + "UserType INTEGER,"
                    + "FilePath TEXT,"
                    + "FilePathLocal TEXT)";


            db.execSQL(sql);
        }


    }

    public void CreateTabelGps() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            if (!isTableExists(TABLE_GPS, db)) {

                String sql = "CREATE TABLE " + TABLE_GPS + "(Time INTEGER PRIMARY KEY,"
                        + "Status INTEGER,"
                        + "UserCode TEXT,"
                        + "Dir TEXT,"
                        + "Dis INTEGER,"
                        + "Lat TEXT,"
                        + "Lng TEXT,"
                        + "Vel INTEGER,"
                        + "GpsStatus INTEGER,"
                        + "GsmStatus INTEGER,"
                        + "Seri TEXT,"
                        + "Sent INTEGER,"
                        + "SessionId TEXT,"
                        + "TeacherCode TEXT)";
                db.execSQL(sql);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public boolean isTableExists(String tableName, SQLiteDatabase db) {
        String query = "select DISTINCT tbl_name from sqlite_master where tbl_name = '" + tableName + "'";
        try (Cursor cursor = db.rawQuery(query, null)) {
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    return true;
                }
            }
            return false;
        }
    }

    public void UpdateResentAttendance(long time) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            String sql = "UPDATE " + TABLE_ATTENDANCE + " set Sent = 1 where Time=" + time + "";
            db.execSQL(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void AppendAttendance(InOutModel inOutModel) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            String sql = "insert into " + TABLE_ATTENDANCE + "(Time,Type,Seri,UserCode,UserId,Name,Lat,Lng,LoginType,Sent,UserType,FilePath,FilePathLocal) "
                    + "values(?,?,?,?,?,?,?,?,?,?,?,?,?)";
            Object[] args = new Object[]{inOutModel.Time, inOutModel.Type,
                    inOutModel.Seri, inOutModel.UserCode, inOutModel.UserId, inOutModel.Name, inOutModel.Lat, inOutModel.Lng, inOutModel.LoginType, inOutModel.Sent, inOutModel.UserType, inOutModel.FilePath, inOutModel.FilePathLocal
            };
            db.execSQL(sql, args);
        } catch (Exception ex) {
            CreateTabelAtten();
            AppendAttendance(inOutModel);
            ex.printStackTrace();
        }
    }

    public ArrayList<AuthModel> LoadDataResentAuth() {
        ArrayList<AuthModel> resentList = new ArrayList<AuthModel>();
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            if (isTableExists(TABLE_ATTENDANCE, db)) {
                Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_AUTH + " where Sent = 0", null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        for (int i = 0; i < cursor.getCount(); i++) {
                            //cursor.move(i);
                            AuthModel authModel = new AuthModel();
                            authModel.Time = cursor.getLong(0);
                            authModel.Seri = cursor.getString(1);
                            authModel.UserCode = cursor.getString(2);
                            authModel.Status = cursor.getString(3);
                            authModel.FilePath = cursor.getString(4);
                            authModel.Lat = cursor.getDouble(5);
                            authModel.Lng = cursor.getDouble(6);
                            authModel.Dis = cursor.getLong(7);
                            authModel.Sent = cursor.getInt(8);
                            authModel.TeacherCode = cursor.getString(9);
                            authModel.SessionId = cursor.getString(10);
                            authModel.FilePathLocal = cursor.getString(11);
                            resentList.add(authModel);
                            cursor.moveToNext();

                        }
                    }
                    cursor.close();
                }
            }
            return resentList;
        } catch (Exception e) {
            e.printStackTrace();
            return resentList;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void BackUpAuth() {
        ArrayList<AuthModel> listBackup = new ArrayList<AuthModel>();

        SQLiteDatabase db = this.getWritableDatabase();
        Date dateNotTime = new Date(dateNow.getYear(), dateNow.getMonth(), dateNow.getDate());
        long timeCheck = dateNotTime.getTime() / 1000;
        ///*
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_AUTH + " where Time > " + timeCheck, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    //cursor.move(i);
                    AuthModel authModel = new AuthModel();
                    authModel.Time = cursor.getLong(0);
                    authModel.Seri = cursor.getString(1);
                    authModel.UserCode = cursor.getString(2);
                    authModel.Status = cursor.getString(3);
                    authModel.FilePath = cursor.getString(4);
                    authModel.Lat = cursor.getDouble(5);
                    authModel.Lng = cursor.getDouble(6);
                    authModel.Dis = cursor.getLong(7);
                    authModel.Sent = cursor.getInt(8);
                    authModel.TeacherCode = cursor.getString(9);
                    authModel.SessionId = cursor.getString(10);
                    authModel.FilePathLocal = cursor.getString(11);
                    listBackup.add(authModel);
                    cursor.moveToNext();

                }
            }
            cursor.close();
        }
        Workbook wb = new HSSFWorkbook();
        Cell cell = null;
        Sheet sheet = null;
        sheet = wb.createSheet(sDateNow);
        Row row = sheet.createRow(0);
        cell = row.createCell(0);
        cell.setCellValue("Mã học viên");
        cell = row.createCell(1);
        cell.setCellValue("Thời điểm");

        cell = row.createCell(2);
        cell.setCellValue("Lat");

        cell = row.createCell(3);
        cell.setCellValue("Lng");

        cell = row.createCell(4);
        cell.setCellValue("Seri thiết bị");

        cell = row.createCell(5);
        cell.setCellValue("Trạng thái xác thực");

        cell = row.createCell(6);
        cell.setCellValue("Mã giảng viên");

        cell = row.createCell(7);
        cell.setCellValue("Hình ảnh");

        sheet.setColumnWidth(0, (30 * 200));
        sheet.setColumnWidth(1, (30 * 200));
        sheet.setColumnWidth(2, (30 * 200));
        sheet.setColumnWidth(3, (30 * 200));
        sheet.setColumnWidth(4, (30 * 200));
        sheet.setColumnWidth(5, (30 * 200));
        sheet.setColumnWidth(6, (30 * 200));
        sheet.setColumnWidth(7, (30 * 200));
        for (int i = 0; i < listBackup.size(); i++) {

            AuthModel authModel = listBackup.get(i);

            Row row1 = sheet.createRow(i + 1);
            row1.setHeight((short) 1500);
            cell = row1.createCell(0);
            cell.setCellValue(authModel.getUserCode());
            cell = row1.createCell(1);
            Date dateTIme = new Date(authModel.getTime() * 1000 + 5 * 60 * 60 * 1000);
            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
            String dateFormat = format.format(dateTIme);
            cell.setCellValue(dateFormat);

            cell = row1.createCell(2);
            cell.setCellValue(authModel.getLat());

            cell = row1.createCell(3);
            cell.setCellValue(authModel.getLng());

            cell = row1.createCell(4);
            cell.setCellValue(authModel.getSeri());

            cell = row1.createCell(5);
            String statusString = "Thành công";
            if (authModel.getStatus().equals("2")) {
                statusString = "Thất bại";
            }
            cell.setCellValue(statusString);

            cell = row1.createCell(6);
            cell.setCellValue(authModel.TeacherCode);
            try {
                CreationHelper factory = sheet.getWorkbook().getCreationHelper();
                Path path = Paths.get(authModel.getFilePathLocal());
                byte[] raw = java.nio.file.Files.readAllBytes(path);
                int my_picture_id = wb.addPicture(raw, Workbook.PICTURE_TYPE_PNG);
                Drawing drawing = sheet.createDrawingPatriarch();
                ClientAnchor my_anchor = factory.createClientAnchor();
                my_anchor.setCol1(7); //Column B
                my_anchor.setRow1(i + 1); //Row 4
                my_anchor.setCol2(8); //Column C
                my_anchor.setRow2(i + 2); //Row 4
                Picture my_picture = drawing.createPicture(my_anchor, my_picture_id);
            } catch (IOException e) {
                e.printStackTrace();
            }
            sheet.setColumnWidth(0, (30 * 200));
            sheet.setColumnWidth(1, (30 * 200));
            sheet.setColumnWidth(2, (30 * 200));
            sheet.setColumnWidth(3, (30 * 200));
            sheet.setColumnWidth(4, (30 * 200));
            sheet.setColumnWidth(5, (30 * 200));
            sheet.setColumnWidth(6, (30 * 200));
            sheet.setColumnWidth(7, (30 * 200));
        }
        File filepath = Environment.getExternalStorageDirectory();
        File dir = new File(filepath.getAbsolutePath()
                + "/HC_DAT_BACKUP/XAC_THUC/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = sDateNow + ".csv";
        File fileXacthuc = new File(dir, fileName);
        if (fileXacthuc.exists()) {
            fileXacthuc.delete();
        }
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(fileXacthuc.getPath());
            wb.write(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void UpdateResentAuth(long time) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            String sql = "UPDATE " + TABLE_AUTH + " set Sent = 1 where Time=" + time + "";
            db.execSQL(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void DeleteAuth(long time) {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "delete from " + TABLE_AUTH + " where Time=" + time + "";
        db.execSQL(sql);
    }

    public void AppendAuth(AuthModel authModel) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            String sql = "insert into " + TABLE_AUTH + "(Time,Seri,UserCode,Status,FilePath,Lat,Lng,Dis,Sent,TeacherCode,SessionId,FilePathLocal) "
                    + "values(?,?,?,?,?,?,?,?,?,?,?,?)";
            Object[] args = new Object[]{authModel.Time, authModel.Seri,
                    authModel.UserCode, authModel.Status, authModel.FilePath, authModel.Lat, authModel.Lng, authModel.Dis, authModel.Sent, authModel.TeacherCode, authModel.SessionId, authModel.FilePathLocal
            };
            db.execSQL(sql, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Create table
    @Override
    public void onCreate(SQLiteDatabase db) {
        if (!isTableExists(TABLE_USER, db)) {
            String sql = "CREATE TABLE " + TABLE_USER + "(userid TEXT PRIMARY KEY,"
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
                    + "birthDay TEXT,"
                    + "faceToken TEXT,"
                    + "totalTime INTEGER,"
                    + "totalDis INTEGER,"
                    + "idCard TEXT,"
                    + "courseCode TEXT,"
                    + "driverNo TEXT,"
                    + "idNo TEXT,"
                    + "totalCourseTime INTEGER,"
                    + "totalCourseDis INTEGER);";
            db.execSQL(sql);
        }
        if (!isTableExists(TABLE_AUTH, db)) {
// Execute Script.
            String sqlAuth = "CREATE TABLE " + TABLE_AUTH + "(Time INTEGER PRIMARY KEY,"
                    + "Seri TEXT,"
                    + "UserCode TEXT,"
                    + "Status TEXT,"
                    + "FilePath TEXT,"
                    + "Lat TEXT,"
                    + "Lng TEXT,"
                    + "Dis INTEGER,"
                    + "Sent INTEGER,"
                    + "TeacherCode TEXT,"
                    + "SessionId TEXT,"
                    + "FilePathLocal TEXT)";

            db.execSQL(sqlAuth);
        }


    }

    public void AppendUser(UserItem ui) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (ui.fingerPrintId1 != null) {
            ui.fp1 = Base64.decode(ui.fingerPrintId1, 0);
            ui.fp2 = Base64.decode(ui.fingerPrintId1, 0);
        }
        ui.enlcon2[0] = 1;
        ui.enlcon1[0] = 1;
        usersList.add(ui);
        String sql = "insert into TB_USERS(userid,username,enlcon1,enlcon2,fp1,fp2,email,gender,name,phoneNumber,address,userType,code,trainingCenterId,avatarId,birthDay,faceToken,totalTime,totalDis,idCard,courseCode,driverNo,idNo,totalCourseTime,totalCourseDis) "
                + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        Object[] args = new Object[]{ui.id, ui.username,
                ui.enlcon1, ui.enlcon2,
                ui.fp1, ui.fp2, ui.email, ui.gender, ui.name, ui.phoneNumber, ui.address, ui.userType, ui.code, ui.trainingCenterId, ui.avatarId, ui.birthDay, ui.faceToken, ui.totalTime, ui.totalDis, ui.idCard, ui.courseCode, ui.driverNo, ui.idNo, ui.totalCourseTime, ui.totalCourseDis};
        db.execSQL(sql, args);
    }

    public void UpdateUser(UserItem ui) {
        if (ui != null && ui.userid != null) {
            if (ui.fingerPrintId1 != null) {
                ui.fp1 = Base64.decode(ui.fingerPrintId1, 0);
                ui.fp2 = Base64.decode(ui.fingerPrintId1, 0);
            }
            SQLiteDatabase dbUser = this.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("username", ui.username);
            cv.put("email", ui.email);
            cv.put("enlcon1", ui.enlcon1);
            cv.put("enlcon2", ui.enlcon2);
            cv.put("fp1", ui.fp1);
            cv.put("fp2", ui.fp2);
            cv.put("gender", ui.gender);
            cv.put("name", ui.name);
            cv.put("phoneNumber", ui.phoneNumber);
            cv.put("address", ui.address);
            cv.put("userType", ui.userType);
            cv.put("code", ui.code);
            cv.put("trainingCenterId", ui.trainingCenterId);
            cv.put("avatarId", ui.avatarId);
            cv.put("birthDay", ui.birthDay);
            cv.put("faceToken", ui.faceToken);
            cv.put("totalTime", ui.totalTime);
            cv.put("totalDis", ui.totalDis);
            cv.put("idCard", ui.idCard);
            cv.put("courseCode", ui.courseCode);
            cv.put("driverNo", ui.driverNo);
            cv.put("idNo", ui.idNo);
            cv.put("totalCourseTime", ui.totalCourseTime);
            cv.put("totalCourseDis", ui.totalCourseDis);
            dbUser.update(TABLE_USER, cv, "userid = ?", new String[]{ui.userid});
            dbUser.close();
        }
    }

    public void DeleteUserHaveFinger() {
        SQLiteDatabase db = this.getWritableDatabase();
        for (int i = 0; i < usersListHaveFinger.size(); i++) {
            String sql = "delete from " + TABLE_USER + " where userid=\"" + String.valueOf(usersListHaveFinger.get(i).userid) + "\"";
            db.execSQL(sql);
        }
    }

    public void DeleteUserHaveFace() {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "delete from " + TABLE_USER + " where faceToken IS NOT NULL";
        db.execSQL(sql);
    }

    public void DeleteUser(String userid) {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "delete from " + TABLE_USER + " where userid=\"" + String.valueOf(userid) + "\"";
        db.execSQL(sql);
    }

    public void UpdateFaceToken(String faceTokenS, String userId) {
        if (faceTokenS != null && !faceTokenS.equals("")) {
            SQLiteDatabase dbUser = this.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("faceToken", faceTokenS);
            dbUser.update(TABLE_USER, cv, "userid = ?", new String[]{userId});
            dbUser.close();
        }
    }

    public void UpdateTotalTime(String userId, double totalTime) {
        SQLiteDatabase dbUser = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("totalTime", totalTime);
        dbUser.update(TABLE_USER, cv, "userid = ?", new String[]{userId});
        dbUser.close();
    }

    public void UpdateTotalDis(String userId, float totalDis) {
        SQLiteDatabase dbUser = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("totalDis", totalDis);
        dbUser.update(TABLE_USER, cv, "userid = ?", new String[]{userId});
        dbUser.close();
    }

    public void LoadHaveFace() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USER + " where faceToken IS NOT NULL", null);
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
                    ui.faceToken = cursor.getString(16);
                    ui.totalTime = cursor.getDouble(17);
                    ui.totalDis = cursor.getFloat(18);
                    ui.idCard = cursor.getString(19);
                    ui.courseCode = cursor.getString(20);
                    ui.driverNo = cursor.getString(21);
                    ui.idNo = cursor.getString(22);
                    ui.totalCourseTime = cursor.getDouble(23);
                    ui.totalCourseDis = cursor.getFloat(24);
                    usersListHaveFace.add(ui);
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
    }

    public void LoadHaveFinger() {
        usersListHaveFinger.clear();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USER + " where fp1 IS NOT NULL", null);
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
                    ui.faceToken = cursor.getString(16);
                    ui.totalTime = cursor.getDouble(17);
                    ui.totalDis = cursor.getFloat(18);
                    ui.idCard = cursor.getString(19);
                    ui.courseCode = cursor.getString(20);
                    ui.driverNo = cursor.getString(21);
                    ui.idNo = cursor.getString(22);
                    ui.totalCourseTime = cursor.getDouble(23);
                    ui.totalCourseDis = cursor.getFloat(24);
                    if (ui.fp1.length > 0) {
                        usersListHaveFinger.add(ui);
                    }

                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
    }

    public void LoadHaveCard() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USER + " where idCard IS NOT NULL", null);
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
                    ui.faceToken = cursor.getString(16);
                    ui.totalTime = cursor.getDouble(17);
                    ui.totalDis = cursor.getFloat(18);
                    ui.idCard = cursor.getString(19);
                    ui.courseCode = cursor.getString(20);
                    ui.driverNo = cursor.getString(21);
                    ui.idNo = cursor.getString(22);
                    ui.totalCourseTime = cursor.getDouble(23);
                    ui.totalCourseDis = cursor.getFloat(24);
                    usersListHaveCard.add(ui);
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
    }

    public void BackUp() {
        ArrayList<UserItem> listResentBackup = new ArrayList<UserItem>();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USER + " where userType = 'STUDENT'", null);
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
                    ui.faceToken = cursor.getString(16);
                    ui.totalTime = cursor.getDouble(17);
                    ui.totalDis = cursor.getFloat(18);
                    ui.idCard = cursor.getString(19);
                    ui.courseCode = cursor.getString(20);
                    ui.driverNo = cursor.getString(21);
                    ui.idNo = cursor.getString(22);
                    ui.totalCourseTime = cursor.getDouble(23);
                    ui.totalCourseDis = cursor.getFloat(24);
                    listResentBackup.add(ui);
                    cursor.moveToNext();
                }
            }
            cursor.close();
        }
        Workbook wb = new HSSFWorkbook();
        Cell cell = null;
        Sheet sheet = null;
        sheet = wb.createSheet("Danhsachsinhvien");
        Row row = sheet.createRow(0);
        cell = row.createCell(0);
        cell.setCellValue("Mã học viên");
        cell = row.createCell(1);
        cell.setCellValue("Tên học viên");

        cell = row.createCell(2);
        cell.setCellValue("Địa chỉ");

        cell = row.createCell(3);
        cell.setCellValue("Số điện thoại");

        cell = row.createCell(4);
        cell.setCellValue("Mã khóa học");

        cell = row.createCell(5);
        cell.setCellValue("Tổng quãng đường phải học");

        cell = row.createCell(6);
        cell.setCellValue("Tổng thời gian phải học");

        cell = row.createCell(7);
        cell.setCellValue("Tổng quãng đường tích lũy");

        cell = row.createCell(8);
        cell.setCellValue("Tổng thời gian tích lũy");

        sheet.setColumnWidth(0, (30 * 200));
        sheet.setColumnWidth(1, (30 * 200));
        sheet.setColumnWidth(2, (30 * 200));
        sheet.setColumnWidth(3, (30 * 200));
        sheet.setColumnWidth(4, (30 * 200));
        sheet.setColumnWidth(5, (30 * 200));
        sheet.setColumnWidth(6, (30 * 200));
        sheet.setColumnWidth(7, (30 * 220));
        sheet.setColumnWidth(8, (30 * 220));
        for (int i = 0; i < listResentBackup.size(); i++) {
            Row row1 = sheet.createRow(i + 1);
            UserItem userItem = listResentBackup.get(i);
            cell = row1.createCell(0);
            cell.setCellValue(userItem.code);
            cell = row1.createCell(1);
            cell.setCellValue(userItem.name);

            cell = row1.createCell(2);
            cell.setCellValue(userItem.address);

            cell = row1.createCell(3);
            cell.setCellValue(userItem.phoneNumber);

            cell = row1.createCell(4);
            cell.setCellValue(userItem.courseCode);

            cell = row1.createCell(5);
            cell.setCellValue(String.valueOf(Math.ceil((userItem.totalCourseDis) / 1000 * 100.0) / 100.0) + " km");

            cell = row1.createCell(6);
            cell.setCellValue(DateUtil.ConvertHms(userItem.totalCourseTime * 60));

            cell = row1.createCell(7);
            cell.setCellValue(String.valueOf(Math.ceil((userItem.totalDis) / 1000 * 100.0) / 100.0) + " km");

            cell = row1.createCell(8);
            cell.setCellValue(DateUtil.ConvertHms(userItem.totalTime * 60));
            sheet.setColumnWidth(0, (30 * 200));
            sheet.setColumnWidth(1, (30 * 200));
            sheet.setColumnWidth(2, (30 * 200));
            sheet.setColumnWidth(3, (30 * 200));
            sheet.setColumnWidth(4, (30 * 200));
            sheet.setColumnWidth(5, (30 * 220));
            sheet.setColumnWidth(6, (30 * 220));
            sheet.setColumnWidth(7, (30 * 220));
            sheet.setColumnWidth(8, (30 * 220));
        }
        File filepath = Environment.getExternalStorageDirectory();
        File dir = new File(filepath.getAbsolutePath()
                + "/HC_DAT_BACKUP/THONG_TIN_HOC_VIEN/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = "danh_sach_hoc_vien_backup_" + sDateNow + ".csv";
        File fileXacthuc = new File(dir, fileName);
        if (fileXacthuc.exists()) {
            fileXacthuc.delete();
        }
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(fileXacthuc.getPath());
            wb.write(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void LoadAll() {
        try {
            usersList.clear();
            SQLiteDatabase db = this.getWritableDatabase();
            ///*
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USER, null);
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
                        ui.faceToken = cursor.getString(16);
                        ui.totalTime = cursor.getDouble(17);
                        ui.totalDis = cursor.getFloat(18);
                        ui.idCard = cursor.getString(19);
                        ui.courseCode = cursor.getString(20);
                        ui.driverNo = cursor.getString(21);
                        ui.idNo = cursor.getString(22);
                        ui.totalCourseTime = cursor.getDouble(23);
                        ui.totalCourseDis = cursor.getFloat(24);
                        usersList.add(ui);
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ///*/
    }

    public void ClearUsers() {
        usersList.clear();
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "delete from TB_USERS";
        db.execSQL(sql);
    }

    public UserItem FindByFaceToken(String facetoken) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USER + " where faceToken IS NOT NULL", null);
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
                        ui.faceToken = cursor.getString(16);
                        ui.totalTime = cursor.getDouble(17);
                        ui.totalDis = cursor.getFloat(18);
                        ui.idCard = cursor.getString(19);
                        ui.courseCode = cursor.getString(20);
                        ui.driverNo = cursor.getString(21);
                        ui.idNo = cursor.getString(22);
                        ui.totalCourseTime = cursor.getDouble(23);
                        ui.totalCourseDis = cursor.getFloat(24);
                        usersListHaveFace.add(ui);
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
            for (int i = 0; i < usersListHaveFace.size(); i++) {
                if (usersListHaveFace.get(i).faceToken != null && usersListHaveFace.get(i).faceToken.equals(facetoken)) {
                    return usersListHaveFace.get(i);
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public UserItem FindUserItemByFP1(String id) {
        LoadAll();
        for (int i = 0; i < usersList.size(); i++) {
            if (usersList.get(i).userid == id) {
                return usersList.get(i);
            }
        }
        return null;
    }

    public boolean UserExist(String id) {
        LoadAll();
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

    public UserItem FindUserItemByCard(String cardsn) {
        LoadHaveCard();
        for (int i = 0; i < usersListHaveCard.size(); i++) {
            if (usersListHaveCard.get(i).idCard.equals(cardsn)) {
                return usersListHaveCard.get(i);
            }
        }
        return null;
    }

    public UserItem FindUserById(String id) {
        LoadHaveCard();
        for (int i = 0; i < usersList.size(); i++) {
            if (usersList.get(i).userid.equals(id)) {
                return usersList.get(i);
            }
        }
        return null;
    }

    public boolean UserIsExists(String id) {
        for (int i = 0; i < usersList.size(); i++) {
            if (usersList.get(i).userid == id)
                return true;
        }
        return false;
    }
}
