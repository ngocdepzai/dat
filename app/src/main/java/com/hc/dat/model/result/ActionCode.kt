package com.hc.dat.model.result

object ActionCode {
    /**
     * App module using range of number from 100 to 199 for definition error code.
     * Outside range used for other module
     */

    const val REGISTER_SUCCESSFUL = 101
    const val TEMPORARY_SUCCESSFUL = 102

    const val PRINT_SUCCESSFUL = 103
    const val DELETE_SUCCESSFUL = 104
    const val UPDATE_SUCCESSFUL = 105
}
