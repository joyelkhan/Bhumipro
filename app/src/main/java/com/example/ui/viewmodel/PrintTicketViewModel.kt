package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.BengaliFormatters
import com.example.domain.report.ReportTemplate
import com.example.domain.report.ReportTicket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.printTicketDataStore by preferencesDataStore(name = "print_ticket_prefs")

data class PrintTicketUiState(
    val selectedTemplate: ReportTemplate = ReportTemplate.STANDARD,
    val ticket: ReportTicket = ReportTicket(),
    val statusMessage: String? = null
)

class PrintTicketViewModel(application: Application) : AndroidViewModel(application) {
    private object Keys {
        val TEMPLATE_ID = stringPreferencesKey("template_id")
        val COMPANY_NAME = stringPreferencesKey("company_name")
        val TICKET_TITLE = stringPreferencesKey("ticket_title")
        val FOOTER_LINE = stringPreferencesKey("footer_line")
        val SURVEYOR_NAME = stringPreferencesKey("surveyor_name")
        val SURVEYOR_MOBILE = stringPreferencesKey("surveyor_mobile")
        val ADDRESS = stringPreferencesKey("address")
        val REGISTRATION_NO = stringPreferencesKey("registration_no")
        val DISTRICT_FIELD = stringPreferencesKey("district_field")
        val LOGO_URI = stringPreferencesKey("logo_uri")
        val SIGNATURE_URI = stringPreferencesKey("signature_uri")
        val DESIGNATION = stringPreferencesKey("designation")
        val QUALIFICATIONS = stringPreferencesKey("qualifications")
        val CONTACT_LINES = stringPreferencesKey("contact_lines")
        val SURVEY_TYPE = stringPreferencesKey("survey_type")
        val UPAZILA_FIELD = stringPreferencesKey("upazila_field")
        val REPORT_NAME = stringPreferencesKey("report_name")
        val SLOGAN = stringPreferencesKey("slogan")
        val REPORT_NOTE = stringPreferencesKey("report_note")
        val QR_LINK = stringPreferencesKey("qr_link")
        val WEBSITE = stringPreferencesKey("website")
    }

    private val _uiState = MutableStateFlow(PrintTicketUiState())
    val uiState: StateFlow<PrintTicketUiState> = _uiState.asStateFlow()
    private var hasLocalEdits = false

    init {
        viewModelScope.launch {
            loadFromStore()
        }
    }

    private suspend fun loadFromStore() {
        val prefs = getApplication<Application>().printTicketDataStore.data.first()
        if (hasLocalEdits) return
        val templateId = prefs[Keys.TEMPLATE_ID] ?: ReportTemplate.STANDARD.id
        val template = ReportTemplate.entries.firstOrNull { it.id == templateId } ?: ReportTemplate.STANDARD
        val ticket = ReportTicket(
            companyName = prefs[Keys.COMPANY_NAME] ?: "নাসেরএক্স ল্যাবস (NaserX Labs)",
            title = prefs[Keys.TICKET_TITLE] ?: "ডিজিটাল ডিমার্কেশন",
            footerLine = prefs[Keys.FOOTER_LINE] ?: "ভূমি প্রো (BhumiPro) দিয়ে প্রস্তুত",
            surveyorName = prefs[Keys.SURVEYOR_NAME] ?: "Abu Naser Khan • NaserX Labs",
            surveyorMobile = prefs[Keys.SURVEYOR_MOBILE] ?: "",
            address = prefs[Keys.ADDRESS] ?: "",
            registrationNo = prefs[Keys.REGISTRATION_NO] ?: "",
            districtField = prefs[Keys.DISTRICT_FIELD] ?: "",
            logoUri = prefs[Keys.LOGO_URI],
            signatureUri = prefs[Keys.SIGNATURE_URI],
            designation = prefs[Keys.DESIGNATION] ?: "সার্ভেয়ার ও ভূমি পরিমাপক",
            qualifications = prefs[Keys.QUALIFICATIONS] ?: "",
            contactLines = prefs[Keys.CONTACT_LINES] ?: "",
            surveyType = prefs[Keys.SURVEY_TYPE] ?: "ডিজিটাল ডিমার্কেশন",
            upazilaField = prefs[Keys.UPAZILA_FIELD] ?: "",
            reportName = prefs[Keys.REPORT_NAME] ?: "",
            slogan = prefs[Keys.SLOGAN] ?: "",
            reportNote = prefs[Keys.REPORT_NOTE] ?: "",
            qrLink = prefs[Keys.QR_LINK] ?: "https://naserxlabs.bhumipro.app/verify",
            website = prefs[Keys.WEBSITE] ?: "naserxlabs.com",
            dateText = BengaliFormatters.bengaliToday()
        )
        _uiState.value = PrintTicketUiState(selectedTemplate = template, ticket = ticket)
    }

    fun selectTemplate(template: ReportTemplate) {
        hasLocalEdits = true
        _uiState.value = _uiState.value.copy(selectedTemplate = template)
        viewModelScope.launch {
            getApplication<Application>().printTicketDataStore.edit {
                it[Keys.TEMPLATE_ID] = template.id
            }
        }
    }

    fun updateTicket(transform: (ReportTicket) -> ReportTicket) {
        hasLocalEdits = true
        val updated = transform(_uiState.value.ticket)
        _uiState.value = _uiState.value.copy(ticket = updated)
    }

    fun saveTicket() {
        val current = _uiState.value
        viewModelScope.launch {
            getApplication<Application>().printTicketDataStore.edit { prefs ->
                prefs[Keys.TEMPLATE_ID] = current.selectedTemplate.id
                prefs[Keys.COMPANY_NAME] = current.ticket.companyName
                prefs[Keys.TICKET_TITLE] = current.ticket.title
                prefs[Keys.FOOTER_LINE] = current.ticket.footerLine
                prefs[Keys.SURVEYOR_NAME] = current.ticket.surveyorName
                prefs[Keys.SURVEYOR_MOBILE] = current.ticket.surveyorMobile
                prefs[Keys.ADDRESS] = current.ticket.address
                prefs[Keys.REGISTRATION_NO] = current.ticket.registrationNo
                prefs[Keys.DISTRICT_FIELD] = current.ticket.districtField
                if (current.ticket.logoUri != null) {
                    prefs[Keys.LOGO_URI] = current.ticket.logoUri
                } else {
                    prefs.remove(Keys.LOGO_URI)
                }
                if (current.ticket.signatureUri != null) {
                    prefs[Keys.SIGNATURE_URI] = current.ticket.signatureUri
                } else {
                    prefs.remove(Keys.SIGNATURE_URI)
                }
                prefs[Keys.DESIGNATION] = current.ticket.designation
                prefs[Keys.QUALIFICATIONS] = current.ticket.qualifications
                prefs[Keys.CONTACT_LINES] = current.ticket.contactLines
                prefs[Keys.SURVEY_TYPE] = current.ticket.surveyType
                prefs[Keys.UPAZILA_FIELD] = current.ticket.upazilaField
                prefs[Keys.REPORT_NAME] = current.ticket.reportName
                prefs[Keys.SLOGAN] = current.ticket.slogan
                prefs[Keys.REPORT_NOTE] = current.ticket.reportNote
                prefs[Keys.QR_LINK] = current.ticket.qrLink
                prefs[Keys.WEBSITE] = current.ticket.website
            }
            _uiState.value = _uiState.value.copy(statusMessage = "প্রিন্ট ডিফল্ট সেভ হয়েছে")
        }
    }

    fun clearAllTicket() {
        viewModelScope.launch {
            getApplication<Application>().printTicketDataStore.edit { it.clear() }
            _uiState.value = PrintTicketUiState(
                selectedTemplate = ReportTemplate.STANDARD,
                ticket = ReportTicket(),
                statusMessage = "সব ডিফল্ট মুছে ফেলা হয়েছে"
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}
