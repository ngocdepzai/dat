# Giờ đêm và giờ xe số tự động

Ghi chú cho lần sau khi sửa phần cảnh báo quá giờ đêm / quá giờ xe số tự động.

## Nguồn dữ liệu — khảo sát thực tế bằng API, không phải suy đoán

Khảo sát ngày 2026-09-04 với tài khoản test `hchv2003` (HV) / `hcgv2003` (GV), thiết bị
`HF20220112002192` (FP-08).

| Field | `start-rider-session` | `check-user-in-session` | `get_by_seri_v3` | Đơn vị |
|---|---|---|---|---|
| `nightTime` | ✅ | ✅ | ❌ | giây |
| `fromNightTime` / `toNightTime` | ✅ | ✅ | ❌ | **giờ trong ngày** (18 và 5) |
| `automaticTransmissionTime` | ✅ | ✅ | ❌ | giây |
| `nightTimeMin` / `nightTimeMax` | ✅ | ❌ | ❌ | giây (7200 / 14400) |
| `autoTimeMin` / `autoTimeMax` | ✅ | ❌ | ❌ | giây (10800 / 21600) |
| `isAutomaticTransmission` | ✅ | ❌ | ✅ (node `vehicle`) | boolean |

**Điểm dễ mắc bẫy nhất: `check-user-in-session` KHÔNG trả các ngưỡng Min/Max.** Node
`session` của nó chỉ có 24 field. Đây không phải do server ẩn null — cùng response đó
`code`, `value`, `oldValue` đều được serialize là `null`. Đã kiểm tra trên cả hai host
`api.hcsky.vn` và `webapi.hcsky.vn`, giống nhau.

`fetch-current-session` (chạy định kỳ sau mỗi lần đẩy GPS) chỉ trả `totalDis`, `totalTime`,
`totalTimeIn24h` — cũng không có gì liên quan.

Ngoài ra `session` trả thêm 3 field model chưa khai báo: `time24hStudent`, `time24hSeri`,
`time24hPlate`.

## Server chỉ cộng dồn SAU KHI phiên kết thúc

Đo bằng thực nghiệm trên phiên đang chạy thật, lấy mẫu 5 lần trong 3 phút, phiên nằm hoàn
toàn trong khung đêm:

```
18:38:05 totalTime=688 nightTime=39600 timeIn24H=0
18:38:50 totalTime=733 nightTime=39600 timeIn24H=0
...
18:41:05 totalTime=868 nightTime=39600 timeIn24H=0
```

`totalTime` chạy đúng thực tế nhưng `nightTime` đứng im. Sau khi phiên đóng (chạy 18:26 →
19:09 = 2514.75s, trọn trong khung đêm) thì `nightTime` nhảy từ `39600` lên `42114.75`,
đúng bằng `39600 + 2514.75`.

⇒ **App phải tự cộng phần phiên đang chạy**, giống hệt cách `timeIn24H` đang làm
(`totalTime + timeIn24H`). Không có rủi ro cộng trùng.

## Công thức

- **Giờ đêm** = `nightTime` (mốc server lúc mở phiên) + phần `[giờ mở phiên, hiện tại]` giao
  với khung `[fromNightTime, toNightTime]`.
- **Giờ tự động** = `automaticTransmissionTime` + `totalTime` của phiên, chỉ khi
  `isAutomaticTransmission = true`.

Khung đêm vắt qua nửa đêm (18h → 5h) nên **không được trừ giờ trực tiếp**. Hàm nhận giờ
thập phân nên khung lẻ kiểu 18.5 (18h30) cũng đúng.
`Utils.nightTimeSecondsBetween()` dựng khung của từng ngày rồi lấy tổng phần giao. Quét từ
**ngày trước** ngày bắt đầu, vì khung mở từ tối hôm trước phủ sang sáng hôm sau. Số ngày quét
tính từ chính độ dài khoảng cần tính, **không chốt cứng theo độ dài phiên tối đa** — chốt
cứng thì phiên treo nhiều ngày sẽ bị tính thiếu giờ đêm mà không báo lỗi gì (ví dụ phiên treo
30 ngày: chốt 3 ngày ra 27h, đúng phải 330h). Có chặn trên 366 ngày đề phòng mốc thời gian
rác làm vòng lặp chạy vô tận.

Kiểm chứng bằng cách **đối chiếu với một bản đếm thô từng phút** chứ không so với số tính
tay — số tính tay đã sai một lần khi tôi đếm nhầm số khung trong ca 7 ngày. 14 ca khớp tuyệt
đối: ca thực tế đo từ server, mở 17h tới 19h, rạng sáng 3h–6h, ban ngày 6h–8h, vắt nửa đêm,
khung không vắt 1h–5h, hai ca khung giờ lẻ 18.5 và 5.5, treo 3 / 7 / 30 ngày, qua tháng, qua
năm. Thêm 6 ca `isInNightWindow` gồm cả hai biên đầu và cuối khung, và ca `from == to`.

## Múi giờ và mốc thời gian

Có hai loại mốc thời gian trong app, dùng lẫn là sai 7 tiếng:

- `Utils.getTimeStamp()` = giờ thật **+ 25200**. Đây là **quy ước gửi lên server**, không phải
  bù múi giờ cho máy: server đọc mốc như UTC rồi hiển thị nguyên nên client cộng sẵn 7 tiếng.
  Chỉ dùng ở chỗ gửi `loginTime` / `logoutTime` cho API.
- `Utils.getRealTimeStamp()` = giờ thật. Dùng cho mọi phép tính trong máy, **bắt buộc** dùng
  cho giờ đêm: nếu dùng bản cộng lệch thì thời lượng vẫn đúng nhưng vị trí trên trục ngày bị
  dịch 7 tiếng, phiên 19h bị coi là 2h sáng hôm sau nên rơi sai phía trong khung đêm.

Khung đêm dựng theo múi giờ máy. An toàn vì app **từ chối chạy** khi máy không ở GMT+7
(`Asia/Ho_Chi_Minh` hoặc `Asia/Saigon`) hoặc đồng hồ lệch server quá 5 phút — xem phần kiểm
tra giờ thiết bị trong `ApplicationViewModel`, sai thì báo `CHECK_DEVICE_DATE_TIME_FAIL` và
không cho vào. Vì thế không hardcode GMT+7 trong hàm tính: làm vậy là nhân đôi chỗ quy định
múi giờ, mà chính sách đó đã có chủ ở màn khởi động.

Lưu ý ngược lại: trong máy, `logoutTime` của bảng phiên **có** lưu giá trị đã cộng lệch, để
phần gửi lại dùng trực tiếp. Đừng đem trường đó đi tính toán cục bộ.

## Vì sao ngưỡng lưu shared-pref chứ không lưu Room

Hai chỗ đều thiếu dữ liệu nếu chỉ dựa vào nguồn có sẵn:

1. `check-user-in-session` không trả Max (xem trên).
2. **`RiderSessionEntity` không lưu `nightTime`, `fromNightTime`, `toNightTime`,
   `automaticTransmissionTime`** — chỉ có `timeIn24H` và `time24hTeacher`. Nên khi app khởi
   động lại giữa phiên, `InProgressSession` dựng từ DB qua `convertToModel()` sẽ có 4 field
   này = null → `fromHour = 0, toHour = 0` → khung đêm rỗng → **giờ đêm về 0**.

Giải pháp: gom mốc + ngưỡng vào `SessionTimeLimitInfo`, lưu shared-pref khi
`start-rider-session` thành công, xoá trong `removeSessionCode()`.

Chọn shared-pref thay vì thêm cột Room vì `AppDatabase` đang ở version 6 và chính comment
trong đó cảnh báo bảng `rider_session` chứa phiên offline chưa upload; thêm cột buộc phải
viết `MIGRATION_6_7`. Nhóm giá trị này chỉ dùng cho cảnh báo trực tiếp, không đi vào báo cáo
Excel hay payload gửi lại, nên không đáng đánh đổi rủi ro migration.

**Phải xoá khi kết thúc phiên**, nếu không phiên sau mở offline (chưa có phản hồi
`start-rider-session`) sẽ cảnh báo theo ngưỡng của học viên trước.

## Cảnh báo

Theo chốt với người dùng: **Toast + TTS + nhấp nháy đỏ ô số**, và **không tự động đăng xuất**
(khác với mốc 24h có `handleAutoLogout`).

- `checkNightTimeOverBlock` / `checkAutoTimeOverBlock` gắn vào `logicBlockChecking` trong
  `startTimeCounter`.
- Còn dưới 15 phút → `showWarning`; đã quá (`<= 0`) → `showError`. Cả hai lặp **5 phút/lần**
  bằng cách đặt duration = `TIME_WARNING_OVER`, nhàn thì về `FREQUENCY_CHECK_LEARNING_TIME`.
- Phải kiểm tra `<= 0` **trước** `< 15 phút`, vì `<= 0` cũng thoả `< 15 phút`.
- `Max = 0` nghĩa là server không giới hạn → thoát sớm, không cảnh báo, không nhấp nháy
  (`blinkThresholdOf` trả `Int.MAX_VALUE`).
- **Chỉ cảnh báo khi con số đang tăng được**: giờ đêm chỉ cảnh báo khi hiện tại nằm trong
  khung đêm (`Utils.isInNightWindow`), giờ xe số tự động chỉ cảnh báo khi
  `isAutomaticTransmission = true`. Không có hai chốt này thì học viên đã vượt ngưỡng từ
  trước sẽ bị kêu 5 phút/lần suốt phiên ban ngày, hoặc bị kêu "quá giờ xe số tự động" trong
  khi đang lái xe số sàn — con số đứng im mà vẫn kêu.
- **Nhấp nháy phải chịu đúng hai điều kiện trên và cả công tắc cài đặt.** Nhấp nháy là một
  hình thức cảnh báo, không phải trang trí: nếu chỉ chặn phần toast/TTS mà để ô vẫn nháy đỏ
  thì xe số sàn sẽ có ô "Giờ AT" nháy suốt phiên, và tắt công tắc rồi vẫn thấy ô nháy.
- Mọi khối cảnh báo phải `if (!isAdded) return@launch` trước khi gọi `getString`: kết thúc
  phiên thì fragment bị detach ngay trong lúc một tick còn đang chờ trên Main, mà
  `clockLogicBlock` không có `CoroutineExceptionHandler` nên sẽ crash. Riêng khối hiển thị
  chạy mỗi giây nên khả năng gặp rất cao.
- Hai ô "Giờ đêm" / "Giờ AT" cập nhật **mỗi giây** trong `clockLogicBlock`, không còn gán
  một lần lúc mở phiên như trước.

Hai switch bật/tắt cảnh báo nằm trong màn Cài đặt thiết bị, **mặc định ON**
(`getBoolean(key, true)`).

## Tồn đọng

1. **Ngưỡng là của cả khoá học, không reset theo ngày.** Học viên đã vượt thì mọi phiên về
   sau đều cảnh báo. Nếu quy định thật là theo ngày hoặc theo tuần thì công thức phải đổi —
   cần chốt lại với nghiệp vụ.
2. **Phiên mở offline không có ngưỡng và không biết xe số tự động** (`Max = 0`,
   `isAutomaticTransmission = false`) cho tới khi có mạng và `recoverSendOfflineData` đẩy
   phiên lên server thành công — lúc đó `handleRecoverUploadSession` mới lưu được ngưỡng, và
   chỉ lưu nếu đúng phiên đang chạy để không lấy ngưỡng của phiên cũ. Muốn chạy được cả khi
   offline thì phải lấy `isAutomaticTransmission` từ node `vehicle` của `get_by_seri_v3`
   (`VehicleInfo` hiện chưa parse field này) và ngưỡng thì không có nguồn nào khác.
3. **`nightTimeMin` / `autoTimeMin` chưa dùng.** Đã khai báo trong
   `StartRiderSessionResponse` để giữ đủ contract. Nghi là mức tối thiểu bắt buộc của khoá
   học (giờ đêm/giờ số tự động phải học đủ) — cần xác nhận với backend trước khi làm gì với
   chúng.
4. Mốc server chỉ được làm mới lúc mở phiên. Trong phiên app tự cộng dồn nên không lệch,
   nhưng nếu backend sửa `nightTime` giữa phiên thì app không thấy.
