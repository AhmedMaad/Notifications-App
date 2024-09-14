package com.maad.maadnotification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.maad.maadnotification.ui.theme.MaadNotificationTheme

class MainActivity : ComponentActivity() {

    private lateinit var receiver: PhoneCallReceiver
    private var showPermissionDeniedDialog = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissionLauncher = handlePermissionResponse(this)
        createNotificationChannel(this)
        registerForIncomingPhoneCalls()

        setContent {

            MaadNotificationTheme {

                if (showPermissionDeniedDialog.value)
                    PermissionDeniedDialog {
                        showPermissionDeniedDialog.value = false
                    }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                permissionLauncher
                                    .launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            else
                                sendNotification(context)
                        }
                    ) {
                        Text(text = "Notify me")
                    }
                }

            }

        }

    }

    private fun handlePermissionResponse(
        c: Context,
    ): ActivityResultLauncher<String> {
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted)
                sendNotification(c)
            else
            //Show a dialog to explain that the feature is unavailable
            //with "cancel" and "allow" options
                showPermissionDeniedDialog.value = true
        }
        return permissionLauncher
    }

    private fun createNotificationChannel(context: Context) {
        val name = "General Channel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("1", name, importance)
        channel.description = "displays general notifications for the app"
        // Register the channel with the system
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification(context: Context) {

        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.compose_quote)

        val link = "https://developer.android.com/compose".toUri()
        val i = Intent(Intent.ACTION_VIEW, link)

        val pendingIntent = PendingIntent
            .getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, "1")
            .setContentIntent(pendingIntent)

            .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New Notification")
            .setContentText("Smell the rose, you are using compose!")
            .setAutoCancel(true)

        //To make the notification appear
        NotificationManagerCompat.from(context).notify(99, builder.build())
    }

    private fun registerForIncomingPhoneCalls() {
        receiver = PhoneCallReceiver()
        val filter = IntentFilter()
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

}

@Composable
fun PermissionDeniedDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        //Like "isCancelable" in old Android.
        onDismissRequest = { },
        confirmButton = {
            TextButton(onClick = {
                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                i.data = Uri
                    .fromParts("package", "com.maad.maadnotification", null)
                context.startActivity(i)
                onDismiss()
            }) {
                Text(text = "Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(text = "Cancel")
            }
        },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_warning),
                contentDescription = "Warning"
            )
        },
        title = {
            Text(text = "We need permission to send notifications")
        },
        text = {
            Text(text = stringResource(id = R.string.alert_content))
        }
    )

}