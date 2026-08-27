package dev.hloth.zaragoza_tarjeta_bus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.hloth.zgztransport.Card

class CardViewModel : ViewModel() {
    var card by mutableStateOf<Card?>(null)
    var loading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
}
