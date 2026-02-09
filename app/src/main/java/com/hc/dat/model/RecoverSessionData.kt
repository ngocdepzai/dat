package com.hc.dat.model

import com.hc.dat.model.database.entity.GPSSignalEntity
import com.hc.dat.model.database.entity.RiderSessionEntity
import com.hc.dat.model.database.entity.StudentAuthenticationEntity

/**
 * Created by Duc Bui on 2023/09.
 * Author: Duc Bui
 * Email: ducbui1890@gmail.com
 * Hanoi, VN.
 */
data class RecoverSessionData(
    val riderSessionEntity: RiderSessionEntity,
    val listAuthenData: List<StudentAuthenticationEntity>,
    val listGpsSignal: List<GPSSignalEntity>
)
