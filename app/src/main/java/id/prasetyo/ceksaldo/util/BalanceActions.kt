package id.prasetyo.ceksaldo.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import id.prasetyo.ceksaldo.data.SimChoice

/** Semua cara membuka saldo: USSD, SMS banking, atau aplikasi resmi. */
object BalanceActions {

    fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** Menjalankan kode USSD. Tanpa izin telepon, kode dibuka di aplikasi Telepon untuk ditekan manual. */
    @SuppressLint("MissingPermission")
    fun dialUssd(context: Context, code: String, sim: SimChoice) {
        val uri = Uri.fromParts("tel", code, null)
        if (!hasPermission(context, Manifest.permission.CALL_PHONE)) {
            startSafely(context, Intent(Intent.ACTION_DIAL, uri))
            return
        }
        val telecom = context.getSystemService(TelecomManager::class.java)
        val extras = Bundle()
        phoneAccountFor(context, sim)?.let {
            extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, it)
        }
        try {
            telecom.placeCall(uri, extras)
        } catch (e: SecurityException) {
            startSafely(context, Intent(Intent.ACTION_DIAL, uri))
        }
    }

    /**
     * Menjalankan USSD satu langkah dan mengembalikan balasannya ke aplikasi (mis. cek pulsa).
     * USSD bertingkat (menu bank) tidak didukung Android untuk mode ini, gunakan [dialUssd].
     */
    @SuppressLint("MissingPermission")
    fun runUssdInline(
        context: Context,
        code: String,
        sim: SimChoice,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val base = context.getSystemService(TelephonyManager::class.java)
        if (base == null) {
            onError("Perangkat ini tidak mendukung jaringan seluler.")
            return
        }
        val telephony = subscriptionIdFor(context, sim)?.let { base.createForSubscriptionId(it) } ?: base
        try {
            telephony.sendUssdRequest(
                code,
                object : TelephonyManager.UssdResponseCallback() {
                    override fun onReceiveUssdResponse(
                        telephonyManager: TelephonyManager,
                        request: String,
                        response: CharSequence,
                    ) = onResult(response.toString())

                    override fun onReceiveUssdResponseFailed(
                        telephonyManager: TelephonyManager,
                        request: String,
                        failureCode: Int,
                    ) = onError("Operator tidak membalas (kode $failureCode). Coba jalankan lewat Telepon.")
                },
                Handler(Looper.getMainLooper()),
            )
        } catch (e: SecurityException) {
            onError("Izin telepon belum diberikan.")
        } catch (e: Exception) {
            onError(e.message ?: "Gagal menjalankan USSD.")
        }
    }

    fun openSms(context: Context, number: String, text: String?) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}"))
        if (!text.isNullOrBlank()) intent.putExtra("sms_body", text)
        startSafely(context, intent)
    }

    fun isInstalled(context: Context, packageName: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(packageName) != null

    /** Membuka aplikasi resmi; jika belum terpasang, buka halamannya di Play Store. */
    fun openApp(context: Context, packageName: String) {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            startSafely(context, launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        Toast.makeText(context, "Aplikasi belum terpasang, membuka Play Store…", Toast.LENGTH_SHORT).show()
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            startSafely(
                context,
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")),
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun phoneAccountFor(context: Context, sim: SimChoice): PhoneAccountHandle? {
        val slot = sim.slotIndex() ?: return null
        if (!hasPermission(context, Manifest.permission.READ_PHONE_STATE)) return null
        return try {
            context.getSystemService(TelecomManager::class.java)
                .callCapablePhoneAccounts
                .getOrNull(slot)
        } catch (e: SecurityException) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun subscriptionIdFor(context: Context, sim: SimChoice): Int? {
        val slot = sim.slotIndex() ?: return null
        if (!hasPermission(context, Manifest.permission.READ_PHONE_STATE)) return null
        return try {
            context.getSystemService(SubscriptionManager::class.java)
                ?.getActiveSubscriptionInfoForSimSlotIndex(slot)
                ?.subscriptionId
        } catch (e: SecurityException) {
            null
        }
    }

    private fun SimChoice.slotIndex(): Int? = when (this) {
        SimChoice.SYSTEM -> null
        SimChoice.SIM1 -> 0
        SimChoice.SIM2 -> 1
    }

    private fun startSafely(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Tidak ada aplikasi yang bisa membuka ini.", Toast.LENGTH_SHORT).show()
        }
    }
}
