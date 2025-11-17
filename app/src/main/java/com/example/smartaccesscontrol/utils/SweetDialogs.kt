package com.example.smartaccesscontrol.utils

import android.content.Context
import cn.pedant.SweetAlert.SweetAlertDialog

object SweetDialogs {

    fun success(context: Context, mensaje: String, onConfirm: (() -> Unit)? = null) {
        SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText("Éxito")
            .setContentText(mensaje)
            .setConfirmText("OK")
            .setConfirmClickListener {
                it.dismissWithAnimation()
                onConfirm?.invoke()
            }
            .show()
    }

    fun error(context: Context, mensaje: String, onConfirm: (() -> Unit)? = null) {
        SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE)
            .setTitleText("Error")
            .setContentText(mensaje)
            .setConfirmText("OK")
            .setConfirmClickListener {
                it.dismissWithAnimation()
                onConfirm?.invoke()
            }
            .show()
    }

    fun warning(context: Context, titulo: String, mensaje: String, onConfirm: (() -> Unit)? = null) {
        SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .setConfirmText("Aceptar")
            .setConfirmClickListener {
                it.dismissWithAnimation()
                onConfirm?.invoke()
            }
            .show()
    }
}