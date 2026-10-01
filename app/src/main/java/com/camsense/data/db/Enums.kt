package com.camsense.data.db

// Stored by name (text) in Room, so never rename an entry. ModuleId lives in
// core and RiskLevel in scoring because the detection modules use them too.
enum class ScanStatus { RUNNING, COMPLETED, ABORTED }
enum class RunStatus { DONE, SKIPPED, FAILED }
enum class UserVerdict { UNREVIEWED, CHECKED_OK, CONFIRMED }
enum class EvidenceType { PHOTO, REPORT_JSON, REPORT_PDF }
