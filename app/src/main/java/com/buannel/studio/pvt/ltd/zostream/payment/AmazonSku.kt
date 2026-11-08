package com.buannel.studio.pvt.ltd.zostream.payment

object AmazonSku {
    const val PARENT = "zostream_sub"
    const val WEEK = "zostream.week"
    const val MONTH = "zostream.month"
    const val FOUR_MONTHS = "zostream.4months"
    const val SIX_MONTHS = "zostream.6months"
    const val YEAR = "zostream.year"

    fun termFor(plan: BillingPlan): String? {
        return when (plan.name.trim().lowercase()) {
            "kar 1" -> WEEK
            "thla 1" -> MONTH
            "thla 4" -> FOUR_MONTHS
            "thla 6" -> SIX_MONTHS
            "kum 1" -> YEAR
            else -> when (plan.durationDays) {
                in 6..8 -> WEEK
                in 28..31 -> MONTH
                in 118..124 -> FOUR_MONTHS
                in 178..186 -> SIX_MONTHS
                in 360..366 -> YEAR
                else -> null
            }
        }
    }
}
