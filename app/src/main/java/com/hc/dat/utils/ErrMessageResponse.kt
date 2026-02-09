package com.hc.dat.utils

enum class ErrMessageResponse(
    val errorCode: String = "",
    val messageErr: String? = ""
) {
    ER0104("ER0104", "ログインIDまたは車番が間違っています。"),
    ER0106("ER0106", "入力された車番は存在しません。再入力してください。"),
    ER0107("ER0107", "車番は有効ではありません。"),
    ER0101("ER0101", "ログインIDまたはパスワードが間違っています。\n連続で5回ログイン失敗する場合、アカウントがロックされますので。ご注意ください。"),
    ER0102("ER0102", "アカウントがロックされています。管理者に連絡してください。"),
    ER0103("ER0103", "連続で５回ログイン失敗ため。アカウントがロックされました。管理者に連絡してください。"),
    ER0105("ER0105", "このアカウントは無効化されました。"),
    ER0111("ER0111", "ログインできない。"),
    ER0053("ER0053", "通信エラーが発生しました。"),
    ERR_UNKNOWN(messageErr = "Unknown message error");

    companion object {
        fun findError(errorCode: String?): ErrMessageResponse {
            return values().find {
                errorCode?.contains(it.errorCode, true) ?: false
            } ?: return ERR_UNKNOWN
        }
    }
}
