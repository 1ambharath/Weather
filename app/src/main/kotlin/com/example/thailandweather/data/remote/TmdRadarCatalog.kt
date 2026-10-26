package com.example.thailandweather.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class TmdRadarStation(
    val id: String,
    val name: String,
    val nameThai: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val rangeKm: Int = 240,
    val imageUrl: String,
    val loopUrl: String,
    val status: String = "Operational"
)

@Serializable
data class SatelliteProduct(
    val id: String,
    val name: String,
    val type: String, // IR, VIS, WATER_VAPOUR, COMPOSITE
    val description: String,
    val imageUrl: String,
    val timestamp: Long
)

object TmdRadarCatalog {

    val STATIONS = listOf(
        TmdRadarStation(
            id = "bkk_suvarnabhumi",
            name = "Bangkok (Suvarnabhumi)",
            nameThai = "สถานีเรดาร์สุวรรณภูมิ กรุงเทพฯ",
            region = "Central",
            latitude = 13.6900,
            longitude = 100.7501,
            imageUrl = "https://tmdradar.tmd.go.th/radar/bkk/latest.png",
            loopUrl = "https://tmdradar.tmd.go.th/radar/bkk/loop.gif"
        ),
        TmdRadarStation(
            id = "chiang_mai",
            name = "Chiang Mai (Omkoi)",
            nameThai = "สถานีเรดาร์เชียงใหม่ (อมก๋อย)",
            region = "North",
            latitude = 17.7981,
            longitude = 98.4322,
            imageUrl = "https://tmdradar.tmd.go.th/radar/cmi/latest.png",
            loopUrl = "https://tmdradar.tmd.go.th/radar/cmi/loop.gif"
        ),
        TmdRadarStation(
            id = "phitsanulok",
            name = "Phitsanulok",
            nameThai = "สถานีเรดาร์พิษณุโลก",
            region = "Lower North",
            latitude = 16.8211,
            longitude = 100.2659,
            imageUrl = "https://tmdradar.tmd.go.th/radar/psl/latest.png",
            loopUrl = "https://tmdradar.tmd.go.th/radar/psl/loop.gif"
        ),
        TmdRadarStation(
            id = "khon_kaen",
            name = "Khon Kaen",
            nameThai = "สถานีเรดาร์ขอนแก่น",
            region = "Northeast",
            latitude = 16.4322,
            longitude = 102.8236,
            imageUrl = "https://tmdradar.tmd.go.th/radar/kkn/latest.png",
            loopUrl = "https://tmdradar.tmd.go.th/radar/kkn/loop.gif"
        ),
        TmdRadarStation(
            id = "sattahip",
            name = "Sattahip (Chonburi)",
            nameThai = "สถานีเรดาร์สัตหีบ ชลบุรี",
            region = "East",
            latitude = 12.6667,
            longitude = 100.9000,
            imageUrl = "https://tmdradar.tmd.go.th/radar/stp/latest.png",
            loopUrl = "https://tmdradar.tmd.go.th/radar/stp/loop.gif"
        ),
        TmdRadarStation(
            id = "hua_hin",
            name = "Hua Hin",
            nameThai = "สถานีเรดาร์หัวหิน ประจวบคีรีขันธ์",
            region = "Upper South",
            latitude = 12.5684,
            longitude = 99.9577,
            imageUrl = "https://tmdradar.tmd.go.th/radar/hhn/latest.png",
            loopUrl = "https://tmdradar.tmd.go.th/radar/hhn/loop.gif"
        ),
        TmdRadarStation(
            id = "surat_thani",
            name = "Surat Thani (Samui/Phangan)",
            nameThai = "สถานีเรดาร์สุราษฎร์ธานี",
            region = "South Gulf",
            latitude = 9.1382,
            longitude = 99.3217,
            imageUrl = "https://tmdradar.tmd.go.th/radar/srt/latest.png",
            loopUrl = "https://tmdradar.tmd.go.th/radar/srt/loop.gif"
        ),
        TmdRadarStation(
            id = "phuket",
            name = "Phuket",
            nameThai = "สถานีเรดาร์ภูเก็ต",
            region = "South Andaman",
            latitude = 8.1132,
            longitude = 98.3065,
            imageUrl = "https://tmdradar.tmd.go.th/radar/pkt/latest.png",
            loopUrl = "https://tmdradar.tmd.go.th/radar/pkt/loop.gif"
        ),
        TmdRadarStation(
            id = "songkhla",
            name = "Songkhla (Hat Yai)",
            nameThai = "สถานีเรดาร์สงขลา (หาดใหญ่)",
            region = "Lower South",
            latitude = 7.1894,
            longitude = 100.5954,
            imageUrl = "https://tmdradar.tmd.go.th/radar/ska/latest.png",
            loopUrl = "https://tmdradar.tmd.go.th/radar/ska/loop.gif"
        )
    )

    val SATELLITE_PRODUCTS = listOf(
        SatelliteProduct(
            id = "himawari_asia_ir",
            name = "Himawari-9 Southeast Asia IR",
            type = "IR",
            description = "Infrared cloud-top temperature and convective storm tracking over Thailand and Indochina.",
            imageUrl = "https://www.tmd.go.th/programs/uploads/maps/latest_ir.jpg",
            timestamp = System.currentTimeMillis()
        ),
        SatelliteProduct(
            id = "himawari_asia_vis",
            name = "Himawari-9 Visible Composite",
            type = "VIS",
            description = "High-resolution visible daylight satellite imagery showing cloud formations and tropical depressions.",
            imageUrl = "https://www.tmd.go.th/programs/uploads/maps/latest_vis.jpg",
            timestamp = System.currentTimeMillis()
        ),
        SatelliteProduct(
            id = "tmd_composite_radar",
            name = "Thailand National Radar Composite",
            type = "COMPOSITE",
            description = "Nationwide composite reflectivity merging all 9 TMD Doppler radar installations.",
            imageUrl = "https://tmdradar.tmd.go.th/radar/composite/latest.png",
            timestamp = System.currentTimeMillis()
        )
    )
}
