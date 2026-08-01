package com.isyarat.pintar

object IsyaratData {
    val levels = listOf(
        Level(
            id = 1,
            name = "Level 1: Ayah Saya Sedang Bekerja",
            isUnlocked = true,
            icon = "level_satu",
            questions = listOf(
                Question(1, "AYAH", listOf("sign_ayah"), listOf("sign_ayah", "sign_ibu", "sign_saya", "sign_kakak")),
                Question(2, "SAYA", listOf("sign_saya"), listOf("sign_saya", "sign_ibu", "sign_ayah", "sign_kakak")),
                Question(3, "SEDANG", listOf("sign_sedang"), listOf("sign_sedang", "sign_belajar", "sign_bekerja", "sign_makan")),
                Question(4, "BEKERJA", listOf("sign_bekerja"), listOf("sign_bekerja", "sign_belajar", "sign_memasak", "sign_ayah"))
            )
        ),
        Level(
            id = 2,
            name = "Level 2: Ibu Sedang Memasak di Dapur",
            isUnlocked = false,
            icon = "level_dua",
            questions = listOf(
                Question(5, "IBU", listOf("sign_ibu"), listOf("sign_ayah", "sign_ibu", "sign_saya", "sign_kakak")),
                Question(6, "SEDANG", listOf("sign_sedang"), listOf("sign_sedang", "sign_belajar", "sign_bekerja", "sign_makan")),
                Question(7, "MEMASAK", listOf("sign_memasak"), listOf("sign_memasak", "sign_makan", "sign_goreng", "sign_ibu")),
                Question(8, "DI", listOf("sign_di"), listOf("sign_di", "sign_kamar", "sign_dapur", "sign_pagi")),
                Question(9, "DAPUR", listOf("sign_dapur"), listOf("sign_dapur", "sign_kamar", "sign_sarapan", "sign_makan"))
            )
        ),
        Level(
            id = 3,
            name = "Level 3: Pagi Tadi Saya Makan Ayam dan Sayur",
            isUnlocked = false,
            icon = "level_tiga",
            questions = listOf(
                Question(10, "PAGI", listOf("sign_pagi"), listOf("sign_pagi", "sign_tadi", "sign_sarapan", "sign_ayam")),
                Question(11, "TADI", listOf("sign_tadi"), listOf("sign_tadi", "sign_pagi", "sign_saya", "sign_makan")),
                Question(12, "SAYA", listOf("sign_saya"), listOf("sign_saya", "sign_ibu", "sign_ayah", "sign_kakak")),
                Question(13, "MAKAN", listOf("sign_makan"), listOf("sign_makan", "sign_sarapan", "sign_ayam", "sign_sayur")),
                Question(14, "AYAM", listOf("sign_ayam"), listOf("sign_ayam", "sign_sayur", "sign_makan", "sign_goreng")),
                Question(15, "DAN", listOf("sign_dan"), listOf("sign_dan", "sign_di", "sign_ayam", "sign_sayur")),
                Question(16, "SAYUR", listOf("sign_sayur"), listOf("sign_sayur", "sign_goreng", "sign_ayam", "sign_makan"))
            )
        ),
        Level(
            id = 4,
            name = "Level 4: Kakak Sedang Belajar di Kamar",
            isUnlocked = false,
            icon = "level_empat",
            questions = listOf(
                Question(17, "KAKAK", listOf("sign_kakak"), listOf("sign_kakak", "sign_ayah", "sign_ibu", "sign_saya")),
                Question(18, "SEDANG", listOf("sign_sedang"), listOf("sign_sedang", "sign_belajar", "sign_bekerja", "sign_makan")),
                Question(19, "BELAJAR", listOf("sign_belajar"), listOf("sign_belajar", "sign_bekerja", "sign_memasak", "sign_makan")),
                Question(20, "DI", listOf("sign_di"), listOf("sign_di", "sign_kamar", "sign_dapur", "sign_pagi")),
                Question(21, "KAMAR", listOf("sign_kamar"), listOf("sign_kamar", "sign_dapur", "sign_di", "sign_pagi"))
            )
        ),
        Level(
            id = 5,
            name = "Level 5: Saya Sayang Ayah Ibu dan Kakak",
            isUnlocked = false,
            icon = "level_lima",
            questions = listOf(
                Question(22, "SAYA", listOf("sign_saya"), listOf("sign_saya", "sign_ibu", "sign_ayah", "sign_kakak")),
                Question(23, "SAYANG", listOf("sign_sayang"), listOf("sign_sayang", "sign_ibu", "sign_ayah", "sign_kakak")),
                Question(24, "AYAH", listOf("sign_ayah"), listOf("sign_ayah", "sign_ibu", "sign_saya", "sign_kakak")),
                Question(25, "IBU", listOf("sign_ibu"), listOf("sign_ayah", "sign_ibu", "sign_saya", "sign_kakak")),
                Question(26, "DAN", listOf("sign_dan"), listOf("sign_dan", "sign_di", "sign_ayah", "sign_kakak")),
                Question(27, "KAKAK", listOf("sign_kakak"), listOf("sign_kakak", "sign_ayah", "sign_ibu", "sign_saya"))
            )
        )
    )
}
