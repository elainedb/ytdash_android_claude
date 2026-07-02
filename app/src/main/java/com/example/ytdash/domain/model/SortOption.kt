package com.example.ytdash.domain.model

/**
 * Display labels are deliberately worded to END with the keyword the Maestro flows regex-match
 * on (`(?i)date.*(desc|newest)` for AC-SORT-01) — Maestro's `text:` selector is a full-string
 * match, so the label must literally end with "Newest"/"Desc" etc, no trailing words after it.
 */
enum class SortOption(val label: String) {
    DATE_NEWEST("Date — Newest"),
    DATE_OLDEST("Date — Oldest"),
    TITLE_A_TO_Z("Title — A to Z"),
    TITLE_Z_TO_A("Title — Z to A"),
}
