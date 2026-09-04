package com.hc.dat.model

/**
 * Mốc giờ đêm, giờ xe số tự động và ngưỡng tối đa của phiên đang chạy.
 *
 * Phải giữ riêng nhóm giá trị này vì không nguồn nào có đủ: chỉ start-rider-session trả về
 * ngưỡng Max, còn bảng rider_session trong máy không lưu mốc giờ đêm nên phiên dựng lại từ
 * DB (app khởi động lại giữa phiên) sẽ mất khung giờ đêm và cảnh báo sai.
 */
data class SessionTimeLimitInfo(
    /** Số giây đã học trong khung đêm trước phiên hiện tại, do server tổng hợp. */
    val nightTime: Double = 0.0,
    /** Giờ bắt đầu khung đêm trong ngày, 0..23. */
    val nightFromHour: Int = 0,
    /** Giờ kết thúc khung đêm trong ngày, 0..23; bằng nightFromHour là khung rỗng. */
    val nightToHour: Int = 0,
    /** Ngưỡng giờ đêm tối đa theo giây, 0 nghĩa là server không giới hạn. */
    val nightTimeMax: Double = 0.0,
    /** Số giây đã học trên xe số tự động trước phiên hiện tại. */
    val automaticTransmissionTime: Double = 0.0,
    /** Ngưỡng giờ xe số tự động tối đa theo giây, 0 nghĩa là không giới hạn. */
    val autoTimeMax: Double = 0.0,
    /** Xe của phiên hiện tại có phải xe số tự động hay không. */
    val isAutomaticTransmission: Boolean = false
)
