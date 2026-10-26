package com.example.thailandweather.core.util

import com.example.thailandweather.core.model.Place
import java.text.SimpleDateFormat
import java.util.*

object WeatherUtils {

    /**
     * Translates WMO weather interpretation codes into human-readable descriptions.
     */
    fun getWeatherDescription(code: Int, isThai: Boolean = false): String {
        return when (code) {
            0 -> if (isThai) "ท้องฟ้าแจ่มใส" else "Clear sky"
            1 -> if (isThai) "ท้องฟ้าโปร่งเป็นส่วนใหญ่" else "Mainly clear"
            2 -> if (isThai) "มีเมฆเป็นบางส่วน" else "Partly cloudy"
            3 -> if (isThai) "มีเมฆมาก" else "Overcast"
            45, 48 -> if (isThai) "มีหมอกหนา" else "Fog and depositing rime fog"
            51, 53, 55 -> if (isThai) "ฝนละอองโปรยปราย" else "Drizzle"
            56, 57 -> if (isThai) "ฝนละอองเยือกแข็ง" else "Freezing Drizzle"
            61 -> if (isThai) "ฝนตกเล็กน้อย" else "Slight rain"
            63 -> if (isThai) "ฝนตกปานกลาง" else "Moderate rain"
            65 -> if (isThai) "ฝนตกหนัก" else "Heavy rain"
            66, 67 -> if (isThai) "ฝนเยือกแข็ง" else "Freezing Rain"
            71, 73, 75 -> if (isThai) "หิมะตก" else "Snow fall"
            77 -> if (isThai) "เกล็ดหิมะ" else "Snow grains"
            80, 81 -> if (isThai) "ฝนซู่กระจาย" else "Rain showers"
            82 -> if (isThai) "ฝนฟ้าคะนองรุนแรง" else "Violent rain showers"
            85, 86 -> if (isThai) "หิมะโปรยปราย" else "Snow showers"
            95 -> if (isThai) "พายุฝนฟ้าคะนอง" else "Thunderstorm"
            96, 99 -> if (isThai) "พายุฝนฟ้าคะนองและมีลูกเห็บ" else "Thunderstorm with hail"
            else -> if (isThai) "สภาพอากาศปกติ" else "Fair weather"
        }
    }

    /**
     * Converts Celsius to Fahrenheit.
     */
    fun celsiusToFahrenheit(c: Double): Double = (c * 9.0 / 5.0) + 32.0

    /**
     * Formats temperature according to user preference.
     */
    fun formatTemperature(tempCelsius: Double, isCelsius: Boolean = true): String {
        val value = if (isCelsius) tempCelsius else celsiusToFahrenheit(tempCelsius)
        val unit = if (isCelsius) "°C" else "°F"
        return "${Math.round(value)}$unit"
    }

    /**
     * Formats wind speed according to user preference.
     */
    fun formatWindSpeed(kmh: Double, isKmh: Boolean = true): String {
        return if (isKmh) {
            "${String.format(Locale.US, "%.1f", kmh)} km/h"
        } else {
            val mph = kmh * 0.621371
            "${String.format(Locale.US, "%.1f", mph)} mph"
        }
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Curated Thai representative seed locations.
     */
    val DEFAULT_SEED_PLACES = listOf(
        Place(
            id = "th_bangkok",
            name = "Bangkok",
            thaiName = "กรุงเทพมหานคร",
            province = "Bangkok",
            district = "Phra Nakhon",
            latitude = 13.7563,
            longitude = 100.5018,
            isDefault = true,
            customGroup = "Central & Capital",
            isCoastal = false,
            proximityRadiusKm = 25,
            displayOrder = 0
        ),
        Place(
            id = "th_chiang_mai",
            name = "Chiang Mai",
            thaiName = "เชียงใหม่",
            province = "Chiang Mai",
            district = "Mueang Chiang Mai",
            latitude = 18.7883,
            longitude = 98.9853,
            isDefault = false,
            customGroup = "Northern Highlands",
            isCoastal = false,
            proximityRadiusKm = 25,
            displayOrder = 1
        ),
        Place(
            id = "th_phuket",
            name = "Phuket",
            thaiName = "ภูเก็ต",
            province = "Phuket",
            district = "Mueang Phuket",
            latitude = 7.8804,
            longitude = 98.3923,
            isDefault = false,
            customGroup = "Andaman Coast",
            isCoastal = true,
            proximityRadiusKm = 25,
            displayOrder = 2
        ),
        Place(
            id = "th_koh_samui",
            name = "Koh Samui",
            thaiName = "เกาะสมุย",
            province = "Surat Thani",
            district = "Koh Samui",
            latitude = 9.5357,
            longitude = 99.9357,
            isDefault = false,
            customGroup = "Gulf of Thailand Islands",
            isCoastal = true,
            proximityRadiusKm = 25,
            displayOrder = 3
        ),
        Place(
            id = "th_pattaya",
            name = "Pattaya",
            thaiName = "พัทยา",
            province = "Chon Buri",
            district = "Bang Lamung",
            latitude = 12.9276,
            longitude = 100.8771,
            isDefault = false,
            customGroup = "Eastern Seaboard",
            isCoastal = true,
            proximityRadiusKm = 25,
            displayOrder = 4
        ),
        Place(
            id = "th_khon_kaen",
            name = "Khon Kaen",
            thaiName = "ขอนแก่น",
            province = "Khon Kaen",
            district = "Mueang Khon Kaen",
            latitude = 16.4419,
            longitude = 102.8360,
            isDefault = false,
            customGroup = "Northeastern Isan",
            isCoastal = false,
            proximityRadiusKm = 25,
            displayOrder = 5
        ),
        Place(
            id = "th_krabi",
            name = "Krabi",
            thaiName = "กระบี่",
            province = "Krabi",
            district = "Mueang Krabi",
            latitude = 8.0863,
            longitude = 98.9063,
            isDefault = false,
            customGroup = "Andaman Coast",
            isCoastal = true,
            proximityRadiusKm = 25,
            displayOrder = 6
        ),
        Place(
            id = "th_hua_hin",
            name = "Hua Hin",
            thaiName = "หัวหิน",
            province = "Prachuap Khiri Khan",
            district = "Hua Hin",
            latitude = 12.5684,
            longitude = 99.9577,
            isDefault = false,
            customGroup = "Royal Coast",
            isCoastal = true,
            proximityRadiusKm = 25,
            displayOrder = 7
        )
    )
}
