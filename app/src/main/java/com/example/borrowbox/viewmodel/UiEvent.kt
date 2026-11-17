package com.example.borrowbox.viewmodel

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data object NavigateBack : UiEvent()
}
