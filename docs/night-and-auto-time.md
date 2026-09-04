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

Khung đêm vắt qua nửa đêm (18h → 5h) nên **không được trừ giờ trực tiếp**.
`Utils.nightTimeSecondsBetween()` dựng khung của 3 ngày liên tiếp (từ ngày trước ngày bắt
đầu, vì khung mở từ tối hôm trước phủ sang sáng hôm sau) rồi lấy tổng phần giao. Ba ngày là
đủ vì phiên dài nhất theo quy định chưa tới 4 giờ.

Đã kiểm chứng 8 ca: ca thực tế đo từ server (2514s), mở 17h giờ 19h (ra 1 tiếng), rạng sáng
3h–6h, ban ngày 6h–8h (ra 0), vắt nửa đêm 23h–1h, khung không vắt 1h–5h, phiên dài phủ 2
đoạn, và `from == to` (khung rỗng).

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
- Hai ô "Giờ đêm" / "Giờ AT" cập nhật **mỗi giây** trong `clockLogicBlock`, không còn gán
  một lần lúc mở phiên như trước.

Hai switch bật/tắt cảnh báo nằm trong màn Cài đặt thiết bị, **mặc định ON**
(`getBoolean(key, true)`).

## Tồn đọng

1. **Phiên mở offline không có ngưỡng và không biết xe số tự động** (`Max = 0`,
   `isAutomaticTransmission = false`) cho tới khi có mạng và `recoverSendOfflineData` đẩy
   phiên lên server thành công — lúc đó `handleRecoverUploadSession` mới lưu được ngưỡng, và
   chỉ lưu nếu đúng phiên đang chạy để không lấy ngưỡng của phiên cũ. Muốn chạy được cả khi
   offline thì phải lấy `isAutomaticTransmission` từ node `vehicle` của `get_by_seri_v3`
   (`VehicleInfo` hiện chưa parse field này) và ngưỡng thì không có nguồn nào khác.
2. **`nightTimeMin` / `autoTimeMin` chưa dùng.** Đã khai báo trong
   `StartRiderSessionResponse` để giữ đủ contract. Nghi là mức tối thiểu bắt buộc của khoá
   học (giờ đêm/giờ số tự động phải học đủ) — cần xác nhận với backend trước khi làm gì với
   chúng.
3. Mốc server chỉ được làm mới lúc mở phiên. Trong phiên app tự cộng dồn nên không lệch,
   nhưng nếu backend sửa `nightTime` giữa phiên thì app không thấy.
