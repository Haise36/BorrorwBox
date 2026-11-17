package com.example.borrowbox.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.borrowbox.data.local.BorrowBoxDatabase
import com.example.borrowbox.data.local.BorrowerEntity
import com.example.borrowbox.data.local.CategoryAnalytics
import com.example.borrowbox.data.local.ItemAnalytics
import com.example.borrowbox.data.repository.AuthRepository
import com.example.borrowbox.data.repository.BorrowingRepository
import com.example.borrowbox.data.repository.SettingsRepository
import com.example.borrowbox.data.repository.ThemeConfig
import com.example.borrowbox.data.model.Borrowing
import com.example.borrowbox.data.model.BorrowingStatus
import com.example.borrowbox.data.model.User
import com.example.borrowbox.data.model.UserRole
import com.example.borrowbox.data.model.calculateStatus
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class DashboardSummary(
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val returnedCount: Int = 0,
    val overdueCount: Int = 0,
    val dueSoonCount: Int = 0,
    val dueTodayCount: Int = 0
)

data class BorrowingAnalytics(
    val totalLoans: Int = 0,
    val activeLoans: Int = 0,
    val returnedLoans: Int = 0,
    val overdueLoans: Int = 0,
    val dueSoonLoans: Int = 0,
    val topCategory: String = "N/A",
    val topItem: String = "N/A",
    val returnRatePercentage: Int = 0,
    val averageDurationDays: Double = 0.0
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = BorrowBoxDatabase.getInstance(app)
    private val repository = BorrowingRepository(db.borrowingDao(), db.borrowerDao())
    private val settingsRepository = SettingsRepository(app)
    private val authRepository = AuthRepository(db.userDao(), app)

    val currentUser: StateFlow<User?> = authRepository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val currentUserId: Flow<Long?> = currentUser.map { user ->
        if (user?.role == UserRole.ADMIN) null else user?.id
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DUE_DATE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _statusFilter = MutableStateFlow<BorrowingStatus?>(null)
    val statusFilter: StateFlow<BorrowingStatus?> = _statusFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    val themeConfig = settingsRepository.themeConfig.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeConfig.SYSTEM)
    val notificationsEnabled = settingsRepository.notificationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val reminderDays = settingsRepository.reminderDays.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)
    
    val appLockEnabled = settingsRepository.appLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val biometricEnabled = settingsRepository.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
        
    val appPin = settingsRepository.appPin.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val pinSalt = settingsRepository.pinSalt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "default_pin_salt")

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var lastDeletedBorrowing: Borrowing? = null

    val activeBorrowings = currentUserId.flatMapLatest { userId ->
        repository.getActiveBorrowingsForUser(userId)
    }.combine(_searchQuery) { items, query -> Pair(items, query) }
    .combine(_sortOrder) { (items, query), sort -> Triple(items, query, sort) }
    .combine(_statusFilter) { (items, query, sort), status -> Quad(items, query, sort, status) }
    .combine(_categoryFilter) { (items, query, sort, status), category ->
        items.filter { borrowing ->
            val matchesSearch = (query.isBlank() || borrowing.itemName.contains(query, ignoreCase = true) || borrowing.borrowerName.contains(query, ignoreCase = true))
            val matchesCategory = (category == null || borrowing.category == category)
            val itemStatus = borrowing.calculateStatus()
            val matchesStatus = when (status) {
                null -> true
                BorrowingStatus.ACTIVE -> itemStatus != BorrowingStatus.OVERDUE
                else -> itemStatus == status
            }
            matchesSearch && matchesCategory && matchesStatus
        }.sortedWith(getComparator(sort))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val historyBorrowings = currentUserId.flatMapLatest { userId ->
        repository.getBorrowingHistoryForUser(userId)
    }.combine(_searchQuery) { items, query ->
        if (query.isBlank()) items else items.filter { it.itemName.contains(query, ignoreCase = true) || it.borrowerName.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allBorrowings = currentUserId.flatMapLatest { userId ->
        repository.getAllBorrowingsForUser(userId)
    }.combine(_searchQuery) { items, query -> Pair(items, query) }
    .combine(_sortOrder) { (items, query), sort -> Triple(items, query, sort) }
    .combine(_categoryFilter) { (items, query, sort), category ->
        items.filter { 
            (query.isBlank() || it.itemName.contains(query, ignoreCase = true) || it.borrowerName.contains(query, ignoreCase = true)) &&
            (category == null || it.category == category)
        }.sortedWith(getComparator(sort))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allBorrowers = repository.getAllBorrowers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dashboardSummary = currentUserId.flatMapLatest { userId ->
        combine(
            listOf(
                repository.countTotal(userId),
                repository.countActive(userId),
                repository.countReturned(userId),
                repository.countOverdue(System.currentTimeMillis(), userId),
                repository.countDueInRange(getEndOfDay(), getEndOfDay() + 3 * 86_400_000L, userId),
                repository.countDueInRange(getStartOfDay(), getEndOfDay(), userId)
            )
        ) { array ->
            DashboardSummary(
                totalCount = array[0] as Int,
                activeCount = array[1] as Int,
                returnedCount = array[2] as Int,
                overdueCount = array[3] as Int,
                dueSoonCount = array[4] as Int,
                dueTodayCount = array[5] as Int
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardSummary())

    val analytics = currentUserId.flatMapLatest { userId ->
        combine(
            listOf(
                repository.countTotal(userId),
                repository.countActive(userId),
                repository.countReturned(userId),
                repository.countOverdue(System.currentTimeMillis(), userId),
                repository.countDueInRange(getEndOfDay(), getEndOfDay() + 3 * 86_400_000L, userId),
                repository.getMostPopularCategory(userId),
                repository.getMostBorrowedItem(userId),
                repository.getBorrowingHistoryForUser(userId)
            )
        ) { array ->
            val total = array[0] as Int
            val active = array[1] as Int
            val returned = array[2] as Int
            val overdue = array[3] as Int
            val soon = array[4] as Int
            val categoryData = array[5] as CategoryAnalytics?
            val itemData = array[6] as ItemAnalytics?
            @Suppress("UNCHECKED_CAST")
            val historyList = array[7] as List<Borrowing>

            val totalReturned = historyList.count { it.isReturned }
            val rate = if (total == 0) 0 else (totalReturned * 100 / total)
            val avgDuration = if (historyList.isEmpty()) 0.0 else historyList.map { (it.returnedDate ?: it.expectedReturnDate) - it.dateBorrowed }.average() / 86_400_000.0

            BorrowingAnalytics(
                totalLoans = total,
                activeLoans = active,
                returnedLoans = returned,
                overdueLoans = overdue,
                dueSoonLoans = soon,
                topCategory = categoryData?.category ?: "N/A",
                topItem = itemData?.itemName ?: "N/A",
                returnRatePercentage = rate,
                averageDurationDays = avgDuration
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BorrowingAnalytics())

    val urgentBorrowings = currentUserId.flatMapLatest { userId ->
        repository.observeUrgent(5, userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getBorrowingsForUser(name: String) = currentUserId.flatMapLatest { userId ->
        repository.observeByBorrower(name, userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateSearch(query: String) { _searchQuery.value = query }
    fun updateSort(order: SortOrder) { _sortOrder.value = order }
    fun updateStatusFilter(status: BorrowingStatus?) { _statusFilter.value = status }
    fun updateCategoryFilter(category: String?) { _categoryFilter.value = category }

    fun setTheme(config: ThemeConfig) = viewModelScope.launch { settingsRepository.setThemeConfig(config) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    fun setReminderTiming(days: Int) = viewModelScope.launch { settingsRepository.setReminderDays(days) }
    fun setAppLock(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAppLockEnabled(enabled) }
    fun setBiometric(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBiometricEnabled(enabled) }
    fun setPin(pin: String?) = viewModelScope.launch { 
        settingsRepository.setAppPin(pin)
        if (pin != null) {
            _uiEvent.emit(UiEvent.ShowSnackbar("PIN updated successfully."))
        }
    }

    fun save(borrowing: Borrowing) = viewModelScope.launch {
        val user = currentUser.value
        val borrowingToSave = if (borrowing.id == 0L && borrowing.userId == 0L && user != null) {
            borrowing.copy(userId = user.id)
        } else {
            borrowing
        }
        repository.saveBorrowing(borrowingToSave)
        _uiEvent.emit(UiEvent.ShowSnackbar(if (borrowing.id == 0L) "Borrowing recorded!" else "Information updated."))
    }

    fun delete(borrowing: Borrowing) = viewModelScope.launch { 
        lastDeletedBorrowing = borrowing
        repository.deleteBorrowing(borrowing) 
        _uiEvent.emit(UiEvent.ShowSnackbar("Borrowing record deleted."))
    }
    fun deleteBorrower(borrower: BorrowerEntity) = viewModelScope.launch {
        repository.deleteBorrower(borrower)
        _uiEvent.emit(UiEvent.ShowSnackbar("Borrower deleted."))
    }
    fun undoDeletion() = viewModelScope.launch {
        lastDeletedBorrowing?.let { repository.saveBorrowing(it) }
        lastDeletedBorrowing = null
    }
    fun markReturned(borrowing: Borrowing) = viewModelScope.launch { 
        repository.markAsReturned(borrowing)
        _uiEvent.emit(UiEvent.ShowSnackbar("Item marked as returned."))
    }
    fun toggleArchived(borrowing: Borrowing) = viewModelScope.launch { 
        repository.toggleArchived(borrowing)
        _uiEvent.emit(UiEvent.ShowSnackbar(if (borrowing.isArchived) "Record restored." else "Record archived."))
    }
    fun togglePinned(borrowing: Borrowing) = viewModelScope.launch { 
        repository.saveBorrowing(borrowing.copy(isPinned = !borrowing.isPinned))
        _uiEvent.emit(UiEvent.ShowSnackbar(if (borrowing.isPinned) "Pinned removed." else "Pinned to top."))
    }
    fun extendReturnDate(borrowing: Borrowing, newDate: Long) = viewModelScope.launch { 
        repository.saveBorrowing(borrowing.copy(expectedReturnDate = newDate))
        _uiEvent.emit(UiEvent.ShowSnackbar("Return date extended."))
    }

    fun logout() = viewModelScope.launch {
        authRepository.logout()
    }

    fun exportDataFile(context: Context, isJson: Boolean) = viewModelScope.launch {
        try {
            val userId = if (currentUser.value?.role == UserRole.ADMIN) null else currentUser.value?.id
            val items = repository.getAllBorrowingsSync(userId)
            
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = if (isJson) {
                File(exportDir, "borrowbox_export.json")
            } else {
                File(exportDir, "borrowbox_export.csv")
            }

            if (isJson) {
                val jsonArray = JSONArray()
                items.forEach { item ->
                    val obj = JSONObject().apply {
                        put("itemName", item.itemName)
                        put("borrowerName", item.borrowerName)
                        put("category", item.category)
                        put("contactNumber", item.contactNumber ?: "")
                        put("classSection", item.classSection ?: "")
                        put("returnLocation", item.returnLocation ?: "")
                        put("dateBorrowed", item.dateBorrowed)
                        put("expectedReturnDate", item.expectedReturnDate)
                        put("notes", item.notes)
                        put("isReturned", item.isReturned)
                        put("returnedDate", item.returnedDate ?: 0L)
                        put("photoUri", item.photoUri ?: "")
                        put("isPinned", item.isPinned)
                        put("isArchived", item.isArchived)
                        put("conditionLent", item.conditionLent ?: "")
                        put("conditionReturned", item.conditionReturned ?: "")
                    }
                    jsonArray.put(obj)
                }
                file.writeText(jsonArray.toString(2))
            } else {
                val sb = StringBuilder()
                sb.append("Item,Borrower,Category,Date Borrowed,Expected Return,Actual Return,Location,Pinned,Returned,Archived,Notes\n")
                val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                items.forEach { 
                    sb.append("${it.itemName},${it.borrowerName},${it.category},${df.format(Date(it.dateBorrowed))},${df.format(Date(it.expectedReturnDate))},${it.returnedDate?.let { d -> df.format(Date(d)) } ?: ""},${it.returnLocation ?: ""},${it.isPinned},${it.isReturned},${it.isArchived},\"${it.notes.replace("\n", " ")}\"\n")
                }
                file.writeText(sb.toString())
            }

            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (isJson) "application/json" else "text/csv"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Export File"))
        } catch (e: Exception) {
            _uiEvent.emit(UiEvent.ShowSnackbar("Failed to export data: ${e.message}"))
        }
    }

    suspend fun parseJsonImport(jsonString: String): Result<List<Borrowing>> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<Borrowing>()
            val userId = currentUser.value?.id ?: 0L
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val name = obj.getString("itemName")
                val borrower = obj.getString("borrowerName")
                if (name.isBlank() || borrower.isBlank()) continue
                list.add(
                    Borrowing(
                        userId = userId,
                        itemName = name,
                        borrowerName = borrower,
                        category = obj.optString("category", "Others"),
                        contactNumber = obj.optString("contactNumber").takeIf { it.isNotBlank() },
                        classSection = obj.optString("classSection").takeIf { it.isNotBlank() },
                        returnLocation = obj.optString("returnLocation").takeIf { it.isNotBlank() },
                        dateBorrowed = obj.optLong("dateBorrowed", System.currentTimeMillis()),
                        expectedReturnDate = obj.optLong("expectedReturnDate", System.currentTimeMillis() + 86400000),
                        notes = obj.optString("notes", ""),
                        isReturned = obj.optBoolean("isReturned", false),
                        returnedDate = obj.optLong("returnedDate").takeIf { it != 0L },
                        photoUri = obj.optString("photoUri").takeIf { it.isNotBlank() },
                        isPinned = obj.optBoolean("isPinned", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        conditionLent = obj.optString("conditionLent").takeIf { it.isNotBlank() },
                        conditionReturned = obj.optString("conditionReturned").takeIf { it.isNotBlank() }
                    )
                )
            }
            if (list.isEmpty()) Result.failure(Exception("No valid borrowing records found in JSON."))
            else Result.success(list)
        } catch (e: Exception) {
            Result.failure(Exception("Invalid JSON format: ${e.message}"))
        }
    }

    fun importBorrowings(items: List<Borrowing>) = viewModelScope.launch {
        try {
            items.forEach { repository.saveBorrowing(it) }
            _uiEvent.emit(UiEvent.ShowSnackbar("Imported ${items.size} record(s) successfully."))
        } catch (e: Exception) {
            _uiEvent.emit(UiEvent.ShowSnackbar("Failed to import records: ${e.message}"))
        }
    }

    private fun getComparator(sort: SortOrder): Comparator<Borrowing> = when (sort) {
        SortOrder.DUE_DATE -> compareBy<Borrowing> { !it.isPinned }.thenBy { it.expectedReturnDate }
        SortOrder.DATE_BORROWED -> compareBy<Borrowing> { !it.isPinned }.thenByDescending { it.dateBorrowed }
        SortOrder.ITEM_NAME -> compareBy<Borrowing> { !it.isPinned }.thenBy { it.itemName.lowercase() }
        SortOrder.BORROWER_NAME -> compareBy<Borrowing> { !it.isPinned }.thenBy { it.borrowerName.lowercase() }
        SortOrder.RECENTLY_ADDED -> compareByDescending<Borrowing> { it.id }
    }

    private fun getStartOfDay(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun getEndOfDay(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    fun generateGreeting(): String {
        val name = currentUser.value?.fullName?.split(" ")?.firstOrNull() ?: ""
        val suffix = if (name.isNotBlank()) ", $name" else ""
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> "Good morning$suffix"
            in 12..16 -> "Good afternoon$suffix"
            else -> "Good evening$suffix"
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

enum class SortOrder { DUE_DATE, DATE_BORROWED, ITEM_NAME, BORROWER_NAME, RECENTLY_ADDED }
