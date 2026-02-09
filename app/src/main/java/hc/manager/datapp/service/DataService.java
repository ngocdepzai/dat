package hc.manager.datapp.service;

import hc.manager.datapp.models.request.AddFaceTokenRequest;
import hc.manager.datapp.models.request.ChangeFingerPrintRequest;
import hc.manager.datapp.models.request.CheckUserLoginRequest;
import hc.manager.datapp.models.request.CreateUpdateUserDeviceRequest;
import hc.manager.datapp.models.request.DeleteResetDeviceRequest;
import hc.manager.datapp.models.request.DeleteUserDeviceRequest;
import hc.manager.datapp.models.request.GetByFaceTokenRequest;
import hc.manager.datapp.models.request.GetDeviceBySeriRequest;
import hc.manager.datapp.models.request.GetLastVersionRequest;
import hc.manager.datapp.models.request.GetListSessionRequest;
import hc.manager.datapp.models.request.GetResetDeviceRequest;
import hc.manager.datapp.models.request.GetUpdateUserBySeriRequest;
import hc.manager.datapp.models.request.GetUserByIdCardRequest;
import hc.manager.datapp.models.request.GetUserByIdRequest;
import hc.manager.datapp.models.request.ResentSessionRequest;
import hc.manager.datapp.models.request.SessionTotalRequest;
import hc.manager.datapp.models.request.StudentLoginRequest;
import hc.manager.datapp.models.request.StudentLogoutRequest;
import hc.manager.datapp.models.request.TeacherLoginRequest;
import hc.manager.datapp.models.request.TeacherLogoutRequest;
import hc.manager.datapp.models.response.AddFaceTokenResponse;
import hc.manager.datapp.models.response.ChangeFingerPrintResponse;
import hc.manager.datapp.models.response.CheckUserLoginResponse;
import hc.manager.datapp.models.response.CreateUpdateUserDeviceResponse;
import hc.manager.datapp.models.response.DeleteResetDeviceResponse;
import hc.manager.datapp.models.response.DeleteUserDeviceResponse;
import hc.manager.datapp.models.response.GetByFaceTokenResponse;
import hc.manager.datapp.models.response.GetDeviceBySeriResponse;
import hc.manager.datapp.models.response.GetLastVersionResponse;
import hc.manager.datapp.models.response.GetListSessionResponse;
import hc.manager.datapp.models.response.GetResetDeviceResponse;
import hc.manager.datapp.models.response.GetUpdateUserBySeriResponse;
import hc.manager.datapp.models.response.GetUserByIdCardResponse;
import hc.manager.datapp.models.response.GetUserByIdResponse;
import hc.manager.datapp.models.response.GetVehicleBySeriCourseIdResponse;
import hc.manager.datapp.models.response.ResentSessionResponse;
import hc.manager.datapp.models.response.SessionTotalResponse;
import hc.manager.datapp.models.response.StudentLoginResponse;
import hc.manager.datapp.models.response.StudentLogoutResponse;
import hc.manager.datapp.models.response.TeacherLoginResponse;
import hc.manager.datapp.models.response.TeacherLogoutResponse;
import hc.manager.datapp.models.response.UpdateAvatarResponse;
import hc.manager.datapp.models.response.UploadAuthResponse;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface DataService {
    @POST("api/User/get_by_IdCard")
    Call<GetUserByIdCardResponse> GetUserByIdCard(@Body GetUserByIdCardRequest getUserByIdCardRequest);

    @POST("api/User/change_fingerPrint")
    Call<ChangeFingerPrintResponse> ChangeFingerPrint(@Body ChangeFingerPrintRequest changeFingerPrintRequest);

    @POST("api/User/add_face_token")
    Call<AddFaceTokenResponse> AddFaceToken(@Body AddFaceTokenRequest addFaceTokenRequest);

    @POST("api/User/get_by_face_token")
    Call<GetByFaceTokenResponse> GetByFaceToken(@Body GetByFaceTokenRequest getByFaceTokenRequest);

    @POST("api/device/get_update_user_by_seri")
    Call<GetUpdateUserBySeriResponse> GetUpdateUserBySeri(@Body GetUpdateUserBySeriRequest getUpdateUserBySeriRequest);

    @POST("api/device/get_by_seri_v3")
    Call<GetDeviceBySeriResponse> GetDeviceBySeri(@Body GetDeviceBySeriRequest getDeviceBySeriRequest);

    @POST("api/device/get-reset-device")
    Call<GetResetDeviceResponse> GetResetDevice(@Body GetResetDeviceRequest getResetDeviceRequest);

    @POST("api/device/update_user_device")
    Call<CreateUpdateUserDeviceResponse> CreateUpdateUserDevice(@Body CreateUpdateUserDeviceRequest createUpdateUserDeviceRequest);

    @POST("api/User/get_by_Id")
    Call<GetUserByIdResponse> GetUserById(@Body GetUserByIdRequest getUserByIdRequest);

    @POST("api/User/check_login")
    Call<CheckUserLoginResponse> CheckUserLogin(@Body CheckUserLoginRequest checkUserLoginRequest);

    @POST("api/Device/delete_user_device")
    Call<DeleteUserDeviceResponse> DeleteUserDevice(@Body DeleteUserDeviceRequest request);

    @POST("api/Device/get-last-version-dat")
    Call<GetLastVersionResponse> GetLastVersion(@Body GetLastVersionRequest request);

    @POST("api/Device/delete_reset_device")
    Call<DeleteResetDeviceResponse> DeleteResetDevice(@Body DeleteResetDeviceRequest checkUserLoginRequest);

    @POST("api/Session/student-login")
    Call<StudentLoginResponse> StudentLogin(@Body StudentLoginRequest request);

    @POST("api/Session/teacher-login")
    Call<TeacherLoginResponse> TeacherLogin(@Body TeacherLoginRequest request);

    @POST("api/Session/student-logout")
    Call<StudentLogoutResponse> StudentLogout(@Body StudentLogoutRequest request);

    @POST("api/Session/teacher-logout")
    Call<TeacherLogoutResponse> TeacherLogout(@Body TeacherLogoutRequest request);

    @POST("api/Session/session-total")
    Call<SessionTotalResponse> SessionTotal(@Body SessionTotalRequest request);

    @POST("api/Session/dat-get-list-session")
    Call<GetListSessionResponse> GetListSession(@Body GetListSessionRequest request);

    @POST("api/Session/resent-session-tc")
    Call<ResentSessionResponse> ResentSession(@Body ResentSessionRequest request);

    @Multipart
    @POST("api/User/update_avatar_v2")
    Call<UpdateAvatarResponse> UpdateAvatar(@Part MultipartBody.Part file, @Part("userCode") RequestBody userCode, @Part("seri") RequestBody seri);

    @Multipart
    @POST("api/Resource/upload_auth")
    Call<UploadAuthResponse> UploadAuth(@Part MultipartBody.Part file, @Part("userCode") RequestBody userCode, @Part("seri") RequestBody seri);

    @GET("api/Vehicle/get")
    Call<GetVehicleBySeriCourseIdResponse> GetVehicleBySeriCourseId(@Query("deviceSeri") String deviceSeri, @Query("courseId") String courseId);

    @GET("api/Vehicle/get")
    Call<GetVehicleBySeriCourseIdResponse> GetVehicleBySeriTrainingCenterId(@Query("deviceSeri") String deviceSeri, @Query("trainingCenterId") String trainingCenterId);

//    @GET("api/Vehicle/get-by-courseId")
//    Call<GetVehicleBySeriCourseIdResponse> GetByUserTest(@Query("courseId") String courseId);
}
