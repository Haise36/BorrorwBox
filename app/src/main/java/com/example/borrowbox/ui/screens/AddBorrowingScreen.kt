package com.example.borrowbox.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.borrowbox.data.model.Borrowing
import com.example.borrowbox.data.model.BorrowingConstants
import com.example.borrowbox.utils.formatTimestamp
import com.example.borrowbox.viewmodel.MainViewModel
import com.example.borrowbox.ui.components.FormSectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBorrowingScreen(
    vm: MainViewModel,
    borrowingId: Long,
    onBack: () -> Unit,
    onTakePhoto: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val items by vm.allBorrowings.collectAsState()
    val allBorrowers by vm.allBorrowers.collectAsState()
    val borrowingToEdit = remember(borrowingId, items) { items.find { it.id == borrowingId } }

    var name by remember(borrowingToEdit) { mutableStateOf(borrowingToEdit?.itemName ?: "") }
    var borrower by remember(borrowingToEdit) { mutableStateOf(borrowingToEdit?.borrowerName ?: "") }
    var category by remember(borrowingToEdit) { mutableStateOf(borrowingToEdit?.category ?: "Others") }
    var contactNumber by remember(borrowingToEdit) { mutableStateOf(borrowingToEdit?.contactNumber ?: "") }
    var conditionLent by remember(borrowingToEdit) { mutableStateOf(borrowingToEdit?.conditionLent ?: "Good") }
    var dateBorrowed by remember(borrowingToEdit) { mutableStateOf(borrowingToEdit?.dateBorrowed ?: System.currentTimeMillis()) }
    var returnDate by remember(borrowingToEdit) { mutableStateOf(borrowingToEdit?.expectedReturnDate ?: (System.currentTimeMillis() + 86400000)) }
    var notes by remember(borrowingToEdit) { mutableStateOf(borrowingToEdit?.notes ?: "") }
    var photoUri by remember(borrowingToEdit) { mutableStateOf(borrowingToEdit?.photoUri?.let { Uri.parse(it) }) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var conditionExpanded by remember { mutableStateOf(false) }
    var borrowerExpanded by remember { mutableStateOf(false) }
    var showBorrowedPicker by remember { mutableStateOf(false) }
    var showReturnPicker by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val matchingBorrowers = remember(borrower, allBorrowers) {
        if (borrower.isBlank()) emptyList()
        else allBorrowers.filter { it.name.contains(borrower, ignoreCase = true) && !it.name.equals(borrower, ignoreCase = true) }
    }

    // Observe camera result from savedStateHandle
    val cameraResultState = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("photoUri", null)
        ?.collectAsState()
    val cameraResult = cameraResultState?.value

    LaunchedEffect(cameraResult) {
        cameraResult?.let {
            photoUri = Uri.parse(it)
            navController.currentBackStackEntry?.savedStateHandle?.set("photoUri", null)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) photoUri = uri
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onTakePhoto()
        } else {
            showPermissionDeniedDialog = true
        }
    }

    val isFormValid = name.isNotBlank() && borrower.isNotBlank() && returnDate >= dateBorrowed

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (borrowingId == 0L) "New Borrowing" else "Edit Borrowing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { p ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Photo Picker Surface
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        FilledTonalIconButton(
                            onClick = { showPhotoOptions = true },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Change Photo")
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PhotoCamera, 
                                contentDescription = null, 
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { showPhotoOptions = true },
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add Item Photo")
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FormSectionLabel("ITEM DETAILS")
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Item Name") },
                    leadingIcon = { Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            BorrowingConstants.categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = conditionExpanded,
                        onExpandedChange = { conditionExpanded = !conditionExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = conditionLent,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Condition Lent") },
                            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(
                            expanded = conditionExpanded,
                            onDismissRequest = { conditionExpanded = false }
                        ) {
                            BorrowingConstants.conditions.forEach { cond ->
                                DropdownMenuItem(
                                    text = { Text(cond) },
                                    onClick = {
                                        conditionLent = cond
                                        conditionExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                FormSectionLabel("BORROWER")

                // Borrower Autocomplete Dropdown Box
                ExposedDropdownMenuBox(
                    expanded = borrowerExpanded && matchingBorrowers.isNotEmpty(),
                    onExpandedChange = { borrowerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = borrower,
                        onValueChange = { 
                            borrower = it
                            borrowerExpanded = true
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        label = { Text("Borrower's Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = borrowerExpanded && matchingBorrowers.isNotEmpty(),
                        onDismissRequest = { borrowerExpanded = false }
                    ) {
                        matchingBorrowers.forEach { suggestedBorrower ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(suggestedBorrower.name, fontWeight = FontWeight.Bold)
                                        if (!suggestedBorrower.contactNumber.isNull_or_blank()) {
                                            Text(suggestedBorrower.contactNumber ?: "", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                },
                                onClick = {
                                    borrower = suggestedBorrower.name
                                    if (!suggestedBorrower.contactNumber.isNull_or_blank()) {
                                        contactNumber = suggestedBorrower.contactNumber ?: ""
                                    }
                                    borrowerExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contact Info (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))
                FormSectionLabel("DATES")

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatePickerCard(
                        label = "Borrowed",
                        date = dateBorrowed,
                        modifier = Modifier.weight(1f),
                        onClick = { showBorrowedPicker = true }
                    )
                    DatePickerCard(
                        label = "Expected Return",
                        date = returnDate,
                        modifier = Modifier.weight(1f),
                        onClick = { showReturnPicker = true },
                        isError = returnDate < dateBorrowed
                    )
                }

                Spacer(Modifier.height(8.dp))
                FormSectionLabel("NOTES")

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    label = { Text("Notes (Optional)") },
                    leadingIcon = { Icon(Icons.Default.StickyNote2, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = isFormValid && !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(if (borrowingId == 0L) "Save Borrowing" else "Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            title = { Text("Add Photo") },
            text = { Text("How would you like to add a photo of your item?") },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showPhotoOptions = false
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Take Photo")
                    }
                    OutlinedButton(
                        onClick = {
                            showPhotoOptions = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Choose from Gallery")
                    }
                    if (photoUri != null) {
                        TextButton(
                            onClick = {
                                photoUri = null
                                showPhotoOptions = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Remove Photo")
                        }
                    }
                    TextButton(
                        onClick = { showPhotoOptions = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Camera Permission Required") },
            text = { Text("Camera permission is required to take a photo of your item.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionDeniedDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConfirmationRow("Item", name)
                    ConfirmationRow("Borrower", borrower)
                    ConfirmationRow("Due Date", formatTimestamp(returnDate))
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSaving,
                    onClick = {
                        isSaving = true
                        vm.save(
                            Borrowing(
                                id = borrowingId,
                                itemName = name.trim(),
                                borrowerName = borrower.trim(),
                                category = category,
                                contactNumber = contactNumber.takeIf { it.isNotBlank() },
                                conditionLent = conditionLent.takeIf { it.isNotBlank() },
                                dateBorrowed = dateBorrowed,
                                expectedReturnDate = returnDate,
                                notes = notes.trim(),
                                photoUri = photoUri?.toString(),
                                isReturned = borrowingToEdit?.isReturned ?: false,
                                returnedDate = borrowingToEdit?.returnedDate,
                                isPinned = borrowingToEdit?.isPinned ?: false,
                                isArchived = borrowingToEdit?.isArchived ?: false,
                                conditionReturned = borrowingToEdit?.conditionReturned,
                                returnPhotoUri = borrowingToEdit?.returnPhotoUri
                            )
                        )
                        onBack()
                    }
                ) { Text("Confirm & Save") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBorrowedPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateBorrowed)
        DatePickerDialog(
            onDismissRequest = { showBorrowedPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateBorrowed = it }
                    showBorrowedPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showBorrowedPicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showReturnPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = returnDate)
        DatePickerDialog(
            onDismissRequest = { showReturnPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { returnDate = it }
                    showReturnPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showReturnPicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
fun ConfirmationRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DatePickerCard(label: String, date: Long, onClick: () -> Unit, modifier: Modifier = Modifier, isError: Boolean = false) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isError) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarMonth, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp),
                    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatTimestamp(date), 
                    style = MaterialTheme.typography.bodyMedium, 
                    fontWeight = FontWeight.Bold,
                    color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
