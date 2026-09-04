# Luồng kết thúc phiên học (finish-rider-session)

Ghi chú cho lần sau khi gặp lỗi kiểu **"app báo kết thúc phiên thành công nhưng trên
server phiên vẫn đang chạy"**.

## Quy ước bắt buộc nhớ

| Điểm | Giá trị |
|---|---|
| API | `POST api/Session/v2024/v335/finish-rider-session` (`ServiceDefinition.FINISH_RIDER_SESSION_V335_URL`) |
| `status = 1` | Thành công — **chỉ** giá trị này nghĩa là server đã đóng phiên |
| `status = 0` | Thất bại (đã xác nhận với backend) |
| `status = 2` | Server nhận phiên nhưng đẩy lên Tổng Cục thất bại |
| Timeout OkHttp | 120s (`ServiceDefinition.CONNECTING_TIMEOUTS_DEFAULT`), không có retry |
| `coverReSend = true` | Cờ để server ghi đè khi thiết bị gửi lại, dùng chống trùng |
| Đơn vị `logoutTime` | Giây + 25200 (GMT+7 đã cộng sẵn). `Utils.getTimeStamp()` trả sẵn giá trị này |

## Bốn bất biến dễ vi phạm

1. **`state = START_ONLINE_FINISH_OFFLINE (3)` là state duy nhất được gửi lại.**
   `recoverSendOfflineData()` bước 4.3 chỉ xử lý state 3, và `getListSessionNotFinishOnline()`
   query `WHERE state != 4`. Vì vậy **mọi đường thất bại đều phải lưu phiên với state 3**
   (hoặc state 2 nếu phiên cũng chưa mở online). Bỏ bước lưu = phiên treo vĩnh viễn trên
   server, không bao giờ được gửi lại. Đây chính là nguyên nhân gốc của lỗi ban đầu.

2. **Đánh dấu `state = 4` là không thể quay lại.** Session bị loại khỏi hàng đợi gửi lại
   mãi mãi. Chỉ set khi server xác nhận `status = 1`.

3. **`recoverSendOfflineData()` chỉ được kích hoạt bởi sự kiện mạng** (`NETWORK_INIT` khi
   khởi tạo ViewModel, `NETWORK_AVAILABLE` khi mạng trở lại) — **không** có bộ định thời
   định kỳ. Lỗi online xảy ra khi mạng vẫn còn nên sẽ không có sự kiện nào; phải gọi
   `triggerRecoverSendOfflineData()` bằng tay sau khi lưu.

4. **`resetDataSession()` KHÔNG xoá bản ghi trong DB** — chỉ clear `localRiderSession`,
   `inProgressSession` và shared-pref. Nên lưu DB trước rồi reset là an toàn.

## Các chỗ đã sửa

Repository — `RepositoryImpl.finishRiderSession`:
- `status = 0` trước đây map thành `SUCCESS_WITH_ERROR` mà **không set `isError`** (default
  `false`) ⇒ phía gọi coi là thành công. Nay trả `isError = true`, `FINISH_SESSION_FAIL`.
- `status` lạ không còn map `errorCode = status` (có thể trùng mã lỗi nội bộ, ví dụ 101 là
  `PUSH_SESSION_TO_TC_FAIL`), nay cũng là `FINISH_SESSION_FAIL`.
- Nhánh HTTP != 200 đọc `response.errorBody()`; `response.body()` luôn null khi HTTP lỗi nên
  trước đây `errorMessage` rỗng và Sentry không có thông tin gì. Trả
  `FINISH_SESSION_RESULT_UNKNOWN` vì không biết server đã xử lý hay chưa.
- `catch (UnknownHostException)` → `catch (Exception)` (rethrow `CancellationException`).
  Trước đây timeout / SSL / EOF / parse lỗi lọt ra ngoài repository.

ViewModel — `RiderSessionViewModel`:
- `saveFinishSessionForReSend()` tách từ khối lưu offline, dùng chung cho **ba** đường thất
  bại: không có mạng, upload ảnh logout lỗi, API trả lỗi. Hai đường sau trước đây không lưu
  gì cả.
- `PUSH_SESSION_TO_TC_FAIL` là ngoại lệ: **không** lưu, **không** đóng phiên, vì người dùng
  còn được chọn "kết thúc không gửi Tổng Cục" (`handleCallFinishRiderSession(true)`).
- `CoroutineExceptionHandler` tách thành action `FINISH_RIDER_SESSION_FAIL_UNKNOWN`: nó fire
  cho exception ở bất kỳ đâu trong coroutine, kể cả **sau** khi API đã thành công, nên không
  được khẳng định "đã lưu, sẽ gửi lại".
- `triggerRecoverSendOfflineData()` gộp cờ `recoverUploadInProgress`, gọi ngay sau khi lưu ở
  các đường lỗi online.

UI — `TrainingSessionScreen`:
- Nhánh lỗi **không còn** toast `finish_session_success` ("Phiên học đã kết thúc") — đây là
  lý do người dùng đọc thành "thành công" dù API thất bại.
- `FINISH_RIDER_SESSION_FAIL_BY_LOCATION` tách khỏi `FINISH_RIDER_SESSION_FAIL`: thiếu toạ độ
  kết thúc thì không lưu được gì để gửi lại, nên **giữ phiên đang chạy** và cho "Thử lại"
  (`openCamera()` để resume, giống nhánh `CONTINUES_SESSION`).
- Dialog lỗi nối thêm nguyên nhân cụ thể (`finish_session_fail_reason`) để hỗ trợ đối soát.
- Xoá `FINISH_RIDER_SESSION_SUCCESS_WITH_ERROR` (cả enum, nhánh VM và nhánh UI) cùng hằng
  `ErrorCode.SUCCESS_WITH_ERROR`: sau khi `status = 0` thành lỗi thì không còn đường nào trả
  `errorCode = 0` nữa nên nhánh này không bao giờ chạy tới.

## Tồn đọng — chưa sửa

1. **`pushReportFile()` không được gọi ở đường gửi lại.** Chỉ có ở nhánh online-success
   (`RiderSessionViewModel:1286`) và admin logout (`:1349`). Phiên kết thúc offline hoặc
   kết thúc qua `recoverSendOfflineData` không đẩy file Excel báo cáo lên server. Lỗ hổng có
   từ trước của luồng offline; sửa là thêm 1 lời gọi ở bước 4.3 của `recoverSendOfflineData`,
   nhưng cần xác nhận không đẩy trùng với cơ chế nào khác.
2. **`recoverUploadInProgress` có thể kẹt `true`** nếu coroutine bị cancel —
   `CoroutineExceptionHandler` không nhận `CancellationException` nên cờ không được reset,
   trong phiên process đó không còn gửi lại nữa (phải khởi động lại app). Thực tế scope
   không có parent job nên khó bị cancel.
3. **GPS chết thì không thể kết thúc phiên** (dialog "Thử lại" mãi). Đúng về mặt dữ liệu vì
   không có toạ độ thì không ghi nhận được kết thúc, nhưng nếu module GPS lỗi cứng thì học
   viên bị kẹt. Cần quyết định: cho phép kết thúc sau N lần thử với toạ độ điểm xác thực gần
   nhất, hay giữ nguyên.
4. **`PUSH_SESSION_TO_TC_FAIL` có thể lặp vô hạn** nếu server vẫn trả `status = 2` kể cả khi
   `isSendTC = false`. Có từ trước.
5. **`adminLogoutHandler` truyền `isSendTC` lệch nhau**: `true` cho entity trong RAM
   (`:1329`) nhưng `false` cho DB (`:1338`) ⇒ file báo cáo và DB không khớp. Có từ trước,
   không liên quan luồng này. (Việc `adminLogoutHandler` không gọi API finish là **đúng** —
   nó chỉ chạy khi server ra lệnh `FORCE_LOGOUT_CURRENT_SESSION_BY_ADMIN`, server đã đóng
   phiên bên nó rồi.)

## Ghi chú khi debug

- Sentry tag: `SESSION_FINISH_SUCCESS`, `SESSION_FINISH_FAIL`, `SESSION_FINISH_EXCEPTION`.
  Nhánh HTTP lỗi có thêm extras `errorBody`.
- Log file trên máy phân biệt nguyên nhân qua tiêu đề: `Kết thúc phiên thành công - OFFLINE`,
  `Kết thúc phiên chờ gửi lại - UPLOAD ẢNH LỖI`, `Kết thúc phiên chờ gửi lại - LỖI ONLINE`.
- `Logger` dùng `android.util.Log` nên không viết được unit test JVM cho các hàm có Logger
  khi project chưa bật `testOptions.unitTests.returnDefaultValues`.
