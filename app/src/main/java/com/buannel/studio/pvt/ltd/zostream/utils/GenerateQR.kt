package com.buannel.studio.pvt.ltd.zostream.utils

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.buannel.studio.pvt.ltd.zostream.R
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.style.Color
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.QrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.*

object QRUtils {
    @JvmStatic
    fun generateQR(imageView: ImageView, token: String) {

        val context = imageView.context

        val data = QrData.Text(token)

        val options = QrVectorOptions.Builder()
            .setPadding(.3f)
            .setLogo(
                QrVectorLogo(
                    drawable = ContextCompat.getDrawable(context, R.drawable.icon_transparent),
                    size = .30f,
                    padding = QrVectorLogoPadding.Natural(.2f),
                    shape = QrVectorLogoShape.Circle
                )
            )
            .setBackground(
                QrVectorBackground(
                    drawable = ContextCompat.getDrawable(context, R.drawable.qr_frame)
                )
            )
            .setColors(
                QrVectorColors(
                    dark = QrVectorColor
                        .Solid(Color(0xff345288)),
                    ball = QrVectorColor.Solid(
                        ContextCompat.getColor(context, R.color.black)
                    ),
                    frame = QrVectorColor.LinearGradient(
                        colors = listOf(
                            0f to Color.RED,
                            1f to Color.BLUE,
                        ),
                        orientation = QrVectorColor.LinearGradient
                            .Orientation.LeftDiagonal
                    )
                )
            )
            .setShapes(
                QrVectorShapes(
                    darkPixel = QrVectorPixelShape.RoundCorners(.5f),
                    ball = QrVectorBallShape.RoundCorners(.25f),
                    frame = QrVectorFrameShape.RoundCorners(.25f)
                )
            )
            .build()

        val drawable: Drawable = QrCodeDrawable(data, options)

        imageView.setImageDrawable(drawable)
    }
}