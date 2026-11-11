package com.arabiyyah.app.ui.navigation

sealed class Screen(val route: String) {
    object UnitList : Screen("unit_list")
    object UnitDetail : Screen("unit_detail/{unitNum}") {
        fun createRoute(unitNum: Int): String = "unit_detail/$unitNum"
    }
    object Vocabulary : Screen("vocabulary?unitNum={unitNum}") {
        fun createRoute(unitNum: Int? = null): String {
            return if (unitNum != null) "vocabulary?unitNum=$unitNum" else "vocabulary"
        }
    }
    object Verbs : Screen("verbs?unitNum={unitNum}") {
        fun createRoute(unitNum: Int? = null): String {
            return if (unitNum != null) "verbs?unitNum=$unitNum" else "verbs"
        }
    }
    object Collocations : Screen("collocations?unitNum={unitNum}") {
        fun createRoute(unitNum: Int? = null): String {
            return if (unitNum != null) "collocations?unitNum=$unitNum" else "collocations"
        }
    }
}
