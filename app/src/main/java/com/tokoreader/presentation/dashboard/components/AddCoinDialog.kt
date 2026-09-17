package com.tokoreader.presentation.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tokoreader.data.remote.rest.SymbolInfo
import com.tokoreader.presentation.components.CryptoCoinAvatar
import com.tokoreader.presentation.components.CryptoUtils
import com.tokoreader.ui.theme.ErrorRed
import com.tokoreader.ui.theme.SuccessGreen

@Composable
fun AddCoinDialog(
    availableSymbols: List<SymbolInfo>,
    customSymbols: Set<String>,
    onAddCoin: (String) -> Unit,
    onRemoveCoin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedQuoteFilter by remember { mutableStateOf("ALL") } // "ALL", "IDR", "USDT"
    val focusManager = LocalFocusManager.current

    // Strict Tokocrypto list filtering
    val filteredList = remember(searchQuery, selectedQuoteFilter, availableSymbols) {
        val query = searchQuery.trim().uppercase()
        availableSymbols.filter { sym ->
            val matchQuery = query.isEmpty() || 
                sym.symbol.contains(query) || 
                (sym.baseAsset?.contains(query) == true)
            
            val matchQuote = when (selectedQuoteFilter) {
                "IDR" -> sym.symbol.endsWith("IDR") || sym.quoteAsset == "IDR" || sym.quoteAsset == "BIDR"
                "USDT" -> sym.symbol.endsWith("USDT") || sym.quoteAsset == "USDT"
                else -> true
            }
            matchQuery && matchQuote
        }.take(50)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Tambah Koin",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SuccessGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Security,
                                        contentDescription = "Verified",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Tokocrypto Spot",
                                        color = SuccessGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Katalog resmi perdagangan Tokocrypto",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari koin (cth: TKO, BTC, SOL, ETH)", color = Color(0xFF64748B), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Cari",
                            tint = Color(0xFF94A3B8)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SuccessGreen,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Pair Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL" to "Semua", "IDR" to "Pair IDR", "USDT" to "Pair USDT").forEach { (filterKey, label) ->
                        val isSelected = selectedQuoteFilter == filterKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedQuoteFilter = filterKey },
                            label = { Text(label, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SuccessGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            ),
                            border = BorderStroke(1.dp, if (isSelected) SuccessGreen else Color(0xFF334155))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Coin list
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Koin tidak ditemukan di katalog Tokocrypto",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Hanya koin resmi exchange Tokocrypto yang diizinkan",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.symbol }) { symInfo ->
                            val isAdded = customSymbols.contains(symInfo.symbol)
                            val (base, quote) = CryptoUtils.splitSymbol(symInfo.symbol)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isAdded) SuccessGreen.copy(alpha = 0.5f) else Color(0xFF334155))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CryptoCoinAvatar(symbol = base, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "$base/$quote",
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(if (quote == "IDR") Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = quote,
                                                        color = if (quote == "IDR") Color(0xFF38BDF8) else Color(0xFF34D399),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = CryptoUtils.getCoinName(base),
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    if (isAdded) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(SuccessGreen.copy(alpha = 0.2f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Filled.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Tersimpan", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = { onRemoveCoin(symInfo.symbol) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Hapus",
                                                    tint = ErrorRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF0F172A))
                                                .border(1.dp, SuccessGreen, RoundedCornerShape(6.dp))
                                                .clickable { onAddCoin(symInfo.symbol) }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Add, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("+ Tambah", color = SuccessGreen, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
