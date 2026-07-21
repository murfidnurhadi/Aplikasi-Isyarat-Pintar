package com.isyarat.pintar

object IsyaratData {
    val levels = listOf(
        Level(
            id = 1,
            name = "Level 1: Ayah Saya Sedang Bekerja",
            isUnlocked = true,
            questions = listOf(
                Question(1, "AYAH", "sign_ayah", listOf("sign_ayah", "sign_ibu", "sign_saya", "sign_kakak")),
                Question(2, "SAYA", "sign_saya", listOf("sign_saya", "sign_ibu", "sign_ayah", "sign_kakak")),
                Question(3, "SEDANG", "sign_sedang", listOf("sign_sedang", "sign_belajar", "sign_bekerja", "sign_makan")),
                Question(4, "BEKERJA", "sign_bekerja", listOf("sign_bekerja", "sign_belajar", "sign_memasak", "sign_ayah"))
            )
        ),
        Level(
            id = 2,
            name = "Level 2: Ibu Sedang Memasak di Dapur",
            questions = listOf(
                Question(5, "IBU", "sign_ibu", listOf("sign_ayah", "sign_ibu", "sign_saya", "sign_kakak")),
                Question(6, "SEDANG", "sign_sedang", listOf("sign_sedang", "sign_belajar", "sign_bekerja", "sign_makan")),
                Question(7, "MEMASAK", "sign_memasak", listOf("sign_memasak", "sign_makan", "sign_goreng", "sign_ibu")),
                Question(8, "DI", "sign_di", listOf("sign_di", "sign_kamar", "sign_dapur", "sign_pagi")),
                Question(9, "DAPUR", "sign_dapur", listOf("sign_dapur", "sign_kamar", "sign_sarapan", "sign_makan"))
            )
        ),
        Level(
            id = 3,
            name = "Level 3: Pagi Saya Sarapan Ayam Goreng",
            questions = listOf(
                Question(10, "PAGI", "sign_pagi", listOf("sign_pagi", "sign_sarapan", "sign_ayam", "sign_goreng")),
                Question(11, "SAYA", "sign_saya", listOf("sign_saya", "sign_ibu", "sign_ayah", "sign_kakak")),
                Question(12, "SARAPAN", "sign_sarapan", listOf("sign_sarapan", "sign_makan", "sign_pagi", "sign_goreng")),
                Question(13, "AYAM", "sign_ayam", listOf("sign_ayam", "sign_sayur", "sign_makan", "sign_goreng")),
                Question(14, "GORENG", "sign_goreng", listOf("sign_goreng", "sign_makan", "sign_ayam", "sign_sayur"))
            )
        ),
        Level(
            id = 4,
            name = "Level 4: Kakak Sedang Belajar di Kamar",
            questions = listOf(
                Question(15, "KAKAK", "sign_kakak", listOf("sign_kakak", "sign_ayah", "sign_ibu", "sign_saya")),
                Question(16, "SEDANG", "sign_sedang", listOf("sign_sedang", "sign_belajar", "sign_bekerja", "sign_makan")),
                Question(17, "BELAJAR", "sign_belajar", listOf("sign_belajar", "sign_bekerja", "sign_memasak", "sign_makan")),
                Question(18, "DI", "sign_di", listOf("sign_di", "sign_kamar", "sign_dapur", "sign_pagi")),
                Question(19, "KAMAR", "sign_kamar", listOf("sign_kamar", "sign_dapur", "sign_di", "sign_pagi"))
            )
        ),
        Level(
            id = 5,
            name = "Level 5: Saya Sarapan Pagi Makan Sayur",
            questions = listOf(
                Question(20, "SAYA", "sign_saya", listOf("sign_saya", "sign_ibu", "sign_ayah", "sign_kakak")),
                Question(21, "SARAPAN", "sign_sarapan", listOf("sign_sarapan", "sign_makan", "sign_pagi", "sign_sayur")),
                Question(22, "PAGI", "sign_pagi", listOf("sign_pagi", "sign_sarapan", "sign_ayam", "sign_makan")),
                Question(23, "MAKAN", "sign_makan", listOf("sign_makan", "sign_sarapan", "sign_ayam", "sign_sayur")),
                Question(24, "SAYUR", "sign_sayur", listOf("sign_sayur", "sign_goreng", "sign_ayam", "sign_makan"))
            )
        )
    )
}
