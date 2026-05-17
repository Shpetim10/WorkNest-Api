package com.worknest.features.company.application.export;

import com.worknest.common.i18n.Language;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ExportLocalizationService {

    private static final String DEFAULT_LOCALE = "en";
    private static final Set<String> SUPPORTED_LOCALES = Set.of("en", "sq", "de", "it");
    private static final Map<String, Map<String, String>> LABELS = Map.of(
            "en", englishLabels(),
            "sq", albanianLabels(),
            "de", germanLabels(),
            "it", italianLabels()
    );

    public String resolveLocale(String requestedLocale, String acceptLanguage) {
        String direct = normalize(requestedLocale);
        if (SUPPORTED_LOCALES.contains(direct)) {
            return direct;
        }

        if (StringUtils.hasText(acceptLanguage)) {
            try {
                List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(acceptLanguage);
                for (Locale.LanguageRange range : ranges) {
                    String language = normalize(range.getRange());
                    if (SUPPORTED_LOCALES.contains(language)) {
                        return language;
                    }
                }
            } catch (IllegalArgumentException ignored) {
                return DEFAULT_LOCALE;
            }
        }

        return DEFAULT_LOCALE;
    }

    public Locale javaLocale(String locale) {
        return Language.getLocaleOrDefault(resolveLocale(locale, null));
    }

    public String sheet(String locale, String key) {
        return label(locale, "sheets." + key);
    }

    public String header(String locale, String key) {
        return label(locale, "headers." + key);
    }

    public String roleLabel(String locale, Object value) {
        return enumLabel(locale, "role", value);
    }

    public String employmentTypeLabel(String locale, Object value) {
        return enumLabel(locale, "employment", value);
    }

    public String statusLabel(String locale, Object value) {
        return enumLabel(locale, "status", value);
    }

    public String attendanceStateLabel(String locale, Object value) {
        return enumLabel(locale, "status", value);
    }

    public String leaveTypeLabel(String locale, Object value) {
        return enumLabel(locale, "leave", value);
    }

    public String payrollStatusLabel(String locale, Object value) {
        return enumLabel(locale, "status", value);
    }

    public String paymentMethodLabel(String locale, Object value) {
        return enumLabel(locale, "payment", value);
    }

    public String siteTypeLabel(String locale, Object value) {
        return enumLabel(locale, "siteType", value);
    }

    public String audienceLabel(String locale, Object value) {
        return enumLabel(locale, "audience", value);
    }

    public String priorityLabel(String locale, Object value) {
        return enumLabel(locale, "priority", value);
    }

    private String enumLabel(String locale, String prefix, Object value) {
        if (value == null) {
            return "";
        }

        String raw = value.toString();
        String key = prefix + "." + raw;
        Map<String, String> labels = LABELS.getOrDefault(resolveLocale(locale, null), LABELS.get(DEFAULT_LOCALE));
        String translated = labels.get(key);
        if (translated != null) {
            return translated;
        }

        return LABELS.get(DEFAULT_LOCALE).getOrDefault(key, titleCase(raw));
    }

    private String label(String locale, String key) {
        Map<String, String> labels = LABELS.getOrDefault(resolveLocale(locale, null), LABELS.get(DEFAULT_LOCALE));
        return labels.getOrDefault(key, LABELS.get(DEFAULT_LOCALE).getOrDefault(key, key));
    }

    private static String normalize(String locale) {
        if (!StringUtils.hasText(locale)) {
            return "";
        }

        int separator = locale.indexOf('-');
        String language = separator >= 0 ? locale.substring(0, separator) : locale;
        return language.trim().toLowerCase(Locale.ROOT);
    }

    private static String titleCase(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder builder = new StringBuilder(normalized.length());
        boolean capitalize = true;
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (Character.isWhitespace(current)) {
                capitalize = true;
                builder.append(current);
            } else if (capitalize) {
                builder.append(Character.toUpperCase(current));
                capitalize = false;
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private static Map<String, String> labels(String... pairs) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            labels.put(pairs[i], pairs[i + 1]);
        }
        return Map.copyOf(labels);
    }

    private static Map<String, String> englishLabels() {
        return labels(
                "sheets.employeeList", "Employee List",
                "sheets.staffList", "Staff List",
                "sheets.assignEmployees", "Assign Employees",
                "sheets.attendance", "Attendance",
                "sheets.leaveRequests", "Leave Requests",
                "sheets.payroll", "Payroll",
                "sheets.locations", "Locations",
                "sheets.departments", "Departments",
                "sheets.announcements", "Announcements",
                "sheets.auditLog", "Audit Log",

                "headers.name", "Name",
                "headers.role", "Role",
                "headers.employmentType", "Employment Type",
                "headers.email", "Email",
                "headers.department", "Department",
                "headers.location", "Location",
                "headers.jobTitle", "Job Title",
                "headers.status", "Status",
                "headers.employees", "Employees",
                "headers.assignedEmployees", "Assigned Employees",
                "headers.site", "Site",
                "headers.checkIn", "Check In",
                "headers.checkOut", "Check Out",
                "headers.dayStatus", "Day Status",
                "headers.worked", "Worked",
                "headers.warnings", "Warnings",
                "headers.type", "Type",
                "headers.dateRange", "Date Range",
                "headers.days", "Days",
                "headers.employeeName", "Employee Name",
                "headers.typeOfEmployee", "Type of Employee",
                "headers.payment", "Payment",
                "headers.basePay", "Base Pay",
                "headers.bonuses", "Bonuses",
                "headers.deductions", "Deductions",
                "headers.grossEarnings", "Gross Earnings",
                "headers.siteName", "Site Name",
                "headers.siteCode", "Site Code",
                "headers.siteType", "Site Type",
                "headers.country", "Country",
                "headers.createdAt", "Created At",
                "headers.description", "Description",
                "headers.title", "Title",
                "headers.targetAudience", "Target Audience",
                "headers.priority", "Priority",
                "headers.createdBy", "Created By",
                "headers.content", "Content",
                "headers.user", "User",
                "headers.action", "Action",
                "headers.details", "Details",
                "headers.timestamp", "Timestamp",

                "role.ADMIN", "Admin",
                "role.SUPERADMIN", "Super Admin",
                "role.STAFF", "Staff",
                "role.EMPLOYEE", "Employee",
                "employment.FULL_TIME", "Full Time",
                "employment.PART_TIME", "Part Time",
                "employment.CONTRACT", "Contract",
                "employment.INTERN", "Intern",
                "status.ACTIVE", "Active",
                "status.INACTIVE", "Inactive",
                "status.PENDING", "Pending",
                "status.ON_LEAVE", "On Leave",
                "status.TERMINATED", "Terminated",
                "status.PROBATION", "Probation",
                "status.PRESENT", "Present",
                "status.ABSENT", "Absent",
                "status.LATE", "Late",
                "status.HALF_DAY", "Half Day",
                "status.HOLIDAY", "Holiday",
                "status.MISSING_CHECKOUT", "Missing Checkout",
                "status.FLAGGED", "Flagged",
                "status.PENDING_REVIEW", "Pending Review",
                "status.APPROVED", "Approved",
                "status.REJECTED", "Rejected",
                "status.CORRECTED", "Corrected",
                "status.CANCELLED", "Cancelled",
                "status.DRAFT", "Draft",
                "status.CALCULATED", "Calculated",
                "status.FINALIZED", "Finalized",
                "status.PAID", "Paid",
                "status.DISABLED", "Disabled",
                "status.ARCHIVED", "Archived",
                "status.NOT_CHECKED_IN", "Not Checked In",
                "status.CHECKED_IN", "Checked In",
                "status.CHECKED_OUT", "Checked Out",
                "status.NONE", "None",
                "leave.VACATION", "Vacation",
                "leave.SICK", "Sick",
                "leave.PERSONAL", "Personal",
                "leave.UNPAID", "Unpaid",
                "leave.MATERNITY", "Maternity",
                "leave.PATERNITY", "Paternity",
                "leave.OTHER", "Other",
                "payment.FIXED_MONTHLY", "Fixed Monthly",
                "payment.HOURLY", "Hourly",
                "siteType.HQ", "HQ",
                "siteType.BRANCH", "Branch",
                "siteType.WAREHOUSE", "Warehouse",
                "siteType.STORE", "Store",
                "siteType.CLIENT_SITE", "Client Site",
                "siteType.FIELD_ZONE", "Field Zone",
                "audience.ALL_EMPLOYEES", "All Employees",
                "audience.DEPARTMENT", "Department",
                "audience.SPECIFIC_USERS", "Specific Users",
                "priority.NORMAL", "Normal",
                "priority.IMPORTANT", "Important"
        );
    }

    private static Map<String, String> albanianLabels() {
        return labels(
                "sheets.employeeList", "Lista e punonj\u00EBsve",
                "sheets.staffList", "Lista e stafit",
                "sheets.assignEmployees", "Caktimi i punonj\u00EBsve",
                "sheets.attendance", "Prezenca",
                "sheets.leaveRequests", "K\u00EBrkesat e lejeve",
                "sheets.payroll", "Pagat",
                "sheets.locations", "Lokacionet",
                "sheets.departments", "Departamentet",
                "sheets.announcements", "Njoftimet",
                "sheets.auditLog", "Audit log",

                "headers.name", "Emri",
                "headers.role", "Roli",
                "headers.employmentType", "Lloji i pun\u00EBsimit",
                "headers.email", "Email",
                "headers.department", "Departamenti",
                "headers.location", "Lokacioni",
                "headers.jobTitle", "Pozicioni",
                "headers.status", "Statusi",
                "headers.employees", "Punonj\u00EBsit",
                "headers.assignedEmployees", "Punonj\u00EBs t\u00EB caktuar",
                "headers.site", "Lokacioni",
                "headers.checkIn", "Hyrja",
                "headers.checkOut", "Dalja",
                "headers.dayStatus", "Statusi i dit\u00EBs",
                "headers.worked", "Puna",
                "headers.warnings", "Paralajm\u00EBrime",
                "headers.type", "Lloji",
                "headers.dateRange", "Periudha",
                "headers.days", "Dit\u00EBt",
                "headers.employeeName", "Emri i punonj\u00EBsit",
                "headers.typeOfEmployee", "Lloji i punonj\u00EBsit",
                "headers.payment", "Pagesa",
                "headers.basePay", "Paga baz\u00EB",
                "headers.bonuses", "Bonuse",
                "headers.deductions", "Zbritje",
                "headers.grossEarnings", "Fitimet bruto",
                "headers.siteName", "Emri i lokacionit",
                "headers.siteCode", "Kodi i lokacionit",
                "headers.siteType", "Lloji i lokacionit",
                "headers.country", "Shteti",
                "headers.createdAt", "Krijuar m\u00EB",
                "headers.description", "P\u00EBrshkrimi",
                "headers.title", "Titulli",
                "headers.targetAudience", "Audienca",
                "headers.priority", "Prioriteti",
                "headers.createdBy", "Krijuar nga",
                "headers.content", "P\u00EBrmbajtja",
                "headers.user", "P\u00EBrdoruesi",
                "headers.action", "Veprimi",
                "headers.details", "Detaje",
                "headers.timestamp", "Koha",

                "role.ADMIN", "Admin",
                "role.SUPERADMIN", "Super Admin",
                "role.STAFF", "Staf",
                "role.EMPLOYEE", "Punonj\u00EBs",
                "employment.FULL_TIME", "Me koh\u00EB t\u00EB plot\u00EB",
                "employment.PART_TIME", "Me koh\u00EB t\u00EB pjesshme",
                "employment.CONTRACT", "Kontrat\u00EB",
                "employment.INTERN", "Praktikant",
                "status.ACTIVE", "Aktiv",
                "status.INACTIVE", "Joaktiv",
                "status.PENDING", "N\u00EB pritje",
                "status.ON_LEAVE", "N\u00EB leje",
                "status.TERMINATED", "I p\u00EBrfunduar",
                "status.PROBATION", "Prov\u00EB",
                "status.PRESENT", "Prezent",
                "status.ABSENT", "Mungon",
                "status.LATE", "Von\u00EB",
                "status.HALF_DAY", "Gjysm\u00EB dite",
                "status.HOLIDAY", "Fest\u00EB",
                "status.MISSING_CHECKOUT", "Mungon dalja",
                "status.FLAGGED", "I sh\u00EBnuar",
                "status.PENDING_REVIEW", "N\u00EB shqyrtim",
                "status.APPROVED", "Aprovuar",
                "status.REJECTED", "Refuzuar",
                "status.CORRECTED", "Korrigjuar",
                "status.CANCELLED", "Anuluar",
                "status.DRAFT", "Draft",
                "status.CALCULATED", "Llogaritur",
                "status.FINALIZED", "Finalizuar",
                "status.PAID", "Paguar",
                "status.DISABLED", "I \u00E7aktivizuar",
                "status.ARCHIVED", "Arkivuar",
                "status.NOT_CHECKED_IN", "Nuk ka hyr\u00EB",
                "status.CHECKED_IN", "Ka hyr\u00EB",
                "status.CHECKED_OUT", "Ka dal\u00EB",
                "status.NONE", "Asnj\u00EB",
                "leave.VACATION", "Pushim",
                "leave.SICK", "Raport mjek\u00EBsor",
                "leave.PERSONAL", "Personale",
                "leave.UNPAID", "Pa pages\u00EB",
                "leave.MATERNITY", "Leje lindjeje",
                "leave.PATERNITY", "Leje at\u00EBsie",
                "leave.OTHER", "Tjet\u00EBr",
                "payment.FIXED_MONTHLY", "Mujore fikse",
                "payment.HOURLY", "Me or\u00EB",
                "siteType.HQ", "Zyra qendrore",
                "siteType.BRANCH", "Deg\u00EB",
                "siteType.WAREHOUSE", "Magazin\u00EB",
                "siteType.STORE", "Dyqan",
                "siteType.CLIENT_SITE", "Lokacion klienti",
                "siteType.FIELD_ZONE", "Zon\u00EB terreni",
                "audience.ALL_EMPLOYEES", "T\u00EB gjith\u00EB punonj\u00EBsit",
                "audience.DEPARTMENT", "Departamenti",
                "audience.SPECIFIC_USERS", "P\u00EBrdorues specifik\u00EB",
                "priority.NORMAL", "Normal",
                "priority.IMPORTANT", "E r\u00EBnd\u00EBsishme"
        );
    }

    private static Map<String, String> germanLabels() {
        return labels(
                "sheets.employeeList", "Mitarbeiterliste",
                "sheets.staffList", "Staff-Liste",
                "sheets.assignEmployees", "Mitarbeiter zuweisen",
                "sheets.attendance", "Anwesenheit",
                "sheets.leaveRequests", "Urlaubsanfragen",
                "sheets.payroll", "Gehaltsabrechnung",
                "sheets.locations", "Standorte",
                "sheets.departments", "Abteilungen",
                "sheets.announcements", "Ank\u00FCndigungen",
                "sheets.auditLog", "Audit-Log",

                "headers.name", "Name",
                "headers.role", "Rolle",
                "headers.employmentType", "Besch\u00E4ftigungsart",
                "headers.email", "E-Mail",
                "headers.department", "Abteilung",
                "headers.location", "Standort",
                "headers.jobTitle", "Position",
                "headers.status", "Status",
                "headers.employees", "Mitarbeiter",
                "headers.assignedEmployees", "Zugewiesene Mitarbeiter",
                "headers.site", "Standort",
                "headers.checkIn", "Check-in",
                "headers.checkOut", "Check-out",
                "headers.dayStatus", "Tagesstatus",
                "headers.worked", "Gearbeitet",
                "headers.warnings", "Warnungen",
                "headers.type", "Typ",
                "headers.dateRange", "Zeitraum",
                "headers.days", "Tage",
                "headers.employeeName", "Mitarbeitername",
                "headers.typeOfEmployee", "Mitarbeitertyp",
                "headers.payment", "Zahlung",
                "headers.basePay", "Grundlohn",
                "headers.bonuses", "Boni",
                "headers.deductions", "Abz\u00FCge",
                "headers.grossEarnings", "Bruttoverdienst",
                "headers.siteName", "Standortname",
                "headers.siteCode", "Standortcode",
                "headers.siteType", "Standorttyp",
                "headers.country", "Land",
                "headers.createdAt", "Erstellt am",
                "headers.description", "Beschreibung",
                "headers.title", "Titel",
                "headers.targetAudience", "Zielgruppe",
                "headers.priority", "Priorit\u00E4t",
                "headers.createdBy", "Erstellt von",
                "headers.content", "Inhalt",
                "headers.user", "Benutzer",
                "headers.action", "Aktion",
                "headers.details", "Details",
                "headers.timestamp", "Zeitstempel",

                "role.ADMIN", "Admin",
                "role.SUPERADMIN", "Super Admin",
                "role.STAFF", "Staff",
                "role.EMPLOYEE", "Mitarbeiter",
                "employment.FULL_TIME", "Vollzeit",
                "employment.PART_TIME", "Teilzeit",
                "employment.CONTRACT", "Vertrag",
                "employment.INTERN", "Praktikant",
                "status.ACTIVE", "Aktiv",
                "status.INACTIVE", "Inaktiv",
                "status.PENDING", "Ausstehend",
                "status.ON_LEAVE", "Im Urlaub",
                "status.TERMINATED", "Beendet",
                "status.PROBATION", "Probezeit",
                "status.PRESENT", "Anwesend",
                "status.ABSENT", "Abwesend",
                "status.LATE", "Versp\u00E4tet",
                "status.HALF_DAY", "Halber Tag",
                "status.HOLIDAY", "Feiertag",
                "status.MISSING_CHECKOUT", "Check-out fehlt",
                "status.FLAGGED", "Markiert",
                "status.PENDING_REVIEW", "In Pr\u00FCfung",
                "status.APPROVED", "Genehmigt",
                "status.REJECTED", "Abgelehnt",
                "status.CORRECTED", "Korrigiert",
                "status.CANCELLED", "Storniert",
                "status.DRAFT", "Entwurf",
                "status.CALCULATED", "Berechnet",
                "status.FINALIZED", "Abgeschlossen",
                "status.PAID", "Bezahlt",
                "status.DISABLED", "Deaktiviert",
                "status.ARCHIVED", "Archiviert",
                "status.NOT_CHECKED_IN", "Nicht eingecheckt",
                "status.CHECKED_IN", "Eingecheckt",
                "status.CHECKED_OUT", "Ausgecheckt",
                "status.NONE", "Keine",
                "leave.VACATION", "Urlaub",
                "leave.SICK", "Krankheit",
                "leave.PERSONAL", "Pers\u00F6nlich",
                "leave.UNPAID", "Unbezahlt",
                "leave.MATERNITY", "Mutterschaft",
                "leave.PATERNITY", "Vaterschaft",
                "leave.OTHER", "Andere",
                "payment.FIXED_MONTHLY", "Fest monatlich",
                "payment.HOURLY", "St\u00FCndlich",
                "siteType.HQ", "HQ",
                "siteType.BRANCH", "Filiale",
                "siteType.WAREHOUSE", "Lager",
                "siteType.STORE", "Gesch\u00E4ft",
                "siteType.CLIENT_SITE", "Kundenstandort",
                "siteType.FIELD_ZONE", "Au\u00DFendienstzone",
                "audience.ALL_EMPLOYEES", "Alle Mitarbeiter",
                "audience.DEPARTMENT", "Abteilung",
                "audience.SPECIFIC_USERS", "Bestimmte Benutzer",
                "priority.NORMAL", "Normal",
                "priority.IMPORTANT", "Wichtig"
        );
    }

    private static Map<String, String> italianLabels() {
        return labels(
                "sheets.employeeList", "Lista dipendenti",
                "sheets.staffList", "Lista staff",
                "sheets.assignEmployees", "Assegna dipendenti",
                "sheets.attendance", "Presenze",
                "sheets.leaveRequests", "Richieste ferie",
                "sheets.payroll", "Paghe",
                "sheets.locations", "Sedi",
                "sheets.departments", "Reparti",
                "sheets.announcements", "Annunci",
                "sheets.auditLog", "Audit log",

                "headers.name", "Nome",
                "headers.role", "Ruolo",
                "headers.employmentType", "Tipo di impiego",
                "headers.email", "Email",
                "headers.department", "Reparto",
                "headers.location", "Sede",
                "headers.jobTitle", "Qualifica",
                "headers.status", "Stato",
                "headers.employees", "Dipendenti",
                "headers.assignedEmployees", "Dipendenti assegnati",
                "headers.site", "Sede",
                "headers.checkIn", "Entrata",
                "headers.checkOut", "Uscita",
                "headers.dayStatus", "Stato giorno",
                "headers.worked", "Lavorato",
                "headers.warnings", "Avvisi",
                "headers.type", "Tipo",
                "headers.dateRange", "Periodo",
                "headers.days", "Giorni",
                "headers.employeeName", "Nome dipendente",
                "headers.typeOfEmployee", "Tipo di dipendente",
                "headers.payment", "Pagamento",
                "headers.basePay", "Paga base",
                "headers.bonuses", "Bonus",
                "headers.deductions", "Detrazioni",
                "headers.grossEarnings", "Lordo",
                "headers.siteName", "Nome sede",
                "headers.siteCode", "Codice sede",
                "headers.siteType", "Tipo sede",
                "headers.country", "Paese",
                "headers.createdAt", "Creato il",
                "headers.description", "Descrizione",
                "headers.title", "Titolo",
                "headers.targetAudience", "Destinatari",
                "headers.priority", "Priorita",
                "headers.createdBy", "Creato da",
                "headers.content", "Contenuto",
                "headers.user", "Utente",
                "headers.action", "Azione",
                "headers.details", "Dettagli",
                "headers.timestamp", "Data e ora",

                "role.ADMIN", "Admin",
                "role.SUPERADMIN", "Super Admin",
                "role.STAFF", "Staff",
                "role.EMPLOYEE", "Dipendente",
                "employment.FULL_TIME", "Tempo pieno",
                "employment.PART_TIME", "Part-time",
                "employment.CONTRACT", "Contratto",
                "employment.INTERN", "Stagista",
                "status.ACTIVE", "Attivo",
                "status.INACTIVE", "Inattivo",
                "status.PENDING", "In attesa",
                "status.ON_LEAVE", "In ferie",
                "status.TERMINATED", "Terminato",
                "status.PROBATION", "Prova",
                "status.PRESENT", "Presente",
                "status.ABSENT", "Assente",
                "status.LATE", "In ritardo",
                "status.HALF_DAY", "Mezza giornata",
                "status.HOLIDAY", "Festivo",
                "status.MISSING_CHECKOUT", "Uscita mancante",
                "status.FLAGGED", "Segnalato",
                "status.PENDING_REVIEW", "In revisione",
                "status.APPROVED", "Approvato",
                "status.REJECTED", "Rifiutato",
                "status.CORRECTED", "Corretto",
                "status.CANCELLED", "Annullato",
                "status.DRAFT", "Bozza",
                "status.CALCULATED", "Calcolato",
                "status.FINALIZED", "Finalizzato",
                "status.PAID", "Pagato",
                "status.DISABLED", "Disabilitato",
                "status.ARCHIVED", "Archiviato",
                "status.NOT_CHECKED_IN", "Non entrato",
                "status.CHECKED_IN", "Entrato",
                "status.CHECKED_OUT", "Uscito",
                "status.NONE", "Nessuno",
                "leave.VACATION", "Ferie",
                "leave.SICK", "Malattia",
                "leave.PERSONAL", "Personale",
                "leave.UNPAID", "Non retribuito",
                "leave.MATERNITY", "Maternita",
                "leave.PATERNITY", "Paternita",
                "leave.OTHER", "Altro",
                "payment.FIXED_MONTHLY", "Mensile fisso",
                "payment.HOURLY", "Orario",
                "siteType.HQ", "Sede centrale",
                "siteType.BRANCH", "Filiale",
                "siteType.WAREHOUSE", "Magazzino",
                "siteType.STORE", "Negozio",
                "siteType.CLIENT_SITE", "Sede cliente",
                "siteType.FIELD_ZONE", "Zona operativa",
                "audience.ALL_EMPLOYEES", "Tutti i dipendenti",
                "audience.DEPARTMENT", "Reparto",
                "audience.SPECIFIC_USERS", "Utenti specifici",
                "priority.NORMAL", "Normale",
                "priority.IMPORTANT", "Importante"
        );
    }
}
