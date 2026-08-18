package com.massago.mitra.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.massago.mitra.data.repository.AuthRepository
import com.massago.mitra.ui.theme.EmeraldDark
import com.massago.mitra.ui.theme.EmeraldLight
import com.massago.mitra.ui.theme.EmeraldPrimary
import com.massago.mitra.ui.theme.TextMuted
import com.massago.mitra.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterMitraScreen(
    onNavigateBack: () -> Unit,
    onRegistrationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authRepo = AuthRepository.instance

    var fullName by remember { mutableStateOf("") }
    var nik by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("Pria") }
    var selectedBank by remember { mutableStateOf("BCA") }
    var accountNumber by remember { mutableStateOf("") }
    var accountHolder by remember { mutableStateOf("") }

    val selectedSpecialties = remember {
        mutableStateListOf("Pijat Tradisional", "Refleksi Kaki")
    }

    val availableSpecialties = listOf(
        "Pijat Tradisional",
        "Refleksi Kaki",
        "Deep Tissue & Sport Massage",
        "Lulur & Scrub Relaksasi",
        "Bekam & Kerokan Higienis",
        "Pijat Ibu Hamil Certified"
    )

    val bankList = listOf("BCA", "Mandiri", "BRI", "BNI", "BSI", "CIMB Niaga")

    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    if (showSuccessDialog) {
        Dialog(onDismissRequest = {
            showSuccessDialog = false
            onRegistrationSuccess()
        }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Pendaftaran Terkirim! 🎉",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Data Anda telah masuk ke sistem dan sedang dalam proses verifikasi tim Admin MassaGo (1x24 jam). Anda akan menerima notifikasi WhatsApp setelah akun aktif.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showSuccessDialog = false
                            onRegistrationSuccess()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Masuk ke Dasbor Mitra", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pendaftaran Mitra Baru", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = EmeraldPrimary.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💆‍♂️", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Bergabung Menjadi Mitra Terapis", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmeraldDark)
                        Text("Dapatkan penghasilan fleksibel & bonus harian bersama MassaGo.", fontSize = 11.5.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 1: DATA PRIBADI
            Text("1. Data Pribadi & Identitas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldDark)
            Spacer(modifier = Modifier.height(10.dp))

            Text("Nama Lengkap (Sesuai KTP) *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Contoh: Budi Santoso") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("Nomor NIK KTP (16 Digit) *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = nik,
                onValueChange = { if (it.length <= 16) nik = it.filter { c -> c.isDigit() } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("3201xxxxxxxxxxxx") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = EmeraldPrimary) },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("Jenis Kelamin *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("Pria", "Wanita").forEach { gender ->
                    val isSelected = selectedGender == gender
                    Surface(
                        modifier = Modifier.weight(1f).clickable { selectedGender = gender },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) EmeraldLight else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) EmeraldPrimary else Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = if (gender == "Pria") "👨 Pria" else "👩 Wanita", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: KEAHLIAN PIJAT
            Text("2. Spesialisasi Keahlian Pijat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Pilih semua keahlian yang Anda kuasai untuk menerima order yang cocok.", fontSize = 11.5.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                availableSpecialties.forEach { spec ->
                    val isChecked = selectedSpecialties.contains(spec)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedSpecialties.remove(spec) else selectedSpecialties.add(spec)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isChecked) EmeraldLight.copy(alpha = 0.5f) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isChecked) EmeraldPrimary else Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedSpecialties.add(spec) else selectedSpecialties.remove(spec)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(spec, fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 3: REKENING BANK PENARIKAN
            Text("3. Rekening Bank (Penarikan Saldo)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldDark)
            Spacer(modifier = Modifier.height(10.dp))

            Text("Pilihan Bank *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bankList.take(3).forEach { bank ->
                    val isSelected = selectedBank == bank
                    Surface(
                        modifier = Modifier.weight(1f).clickable { selectedBank = bank },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) EmeraldLight else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) EmeraldPrimary else Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = bank,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Nomor Rekening *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it.filter { c -> c.isDigit() } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Contoh: 1234567890") },
                leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = EmeraldPrimary) },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("Nama Pemilik Rekening *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = accountHolder,
                onValueChange = { accountHolder = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Harus sesuai nama di buku tabungan") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 4: PASSWORD AKUN MITRA
            Text("4. Buat Password Akun Mitra", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Password ini akan digunakan untuk login harian tanpa perlu verifikasi OTP berulang kali.", fontSize = 11.5.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(10.dp))

            var password by remember { mutableStateOf("") }
            var confirmPassword by remember { mutableStateOf("") }
            var isPasswordVisible by remember { mutableStateOf(false) }
            var formError by remember { mutableStateOf<String?>(null) }

            Text("Password Akun *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; formError = null },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Minimal 6 karakter") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary) },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("Konfirmasi Password *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; formError = null },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ketik ulang password") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = EmeraldPrimary) },
                visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
            )

            if (formError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = formError!!,
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            val isFormValid = fullName.isNotBlank() && nik.length >= 10 && accountNumber.isNotBlank() && selectedSpecialties.isNotEmpty() && password.length >= 6

            Button(
                onClick = {
                    if (password.length < 6) {
                        formError = "Password minimal harus 6 karakter"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        formError = "Konfirmasi password tidak cocok"
                        return@Button
                    }

                    if (isFormValid) {
                        isSubmitting = true
                        coroutineScope.launch {
                            authRepo.registerTherapist(
                                context = context,
                                name = fullName,
                                gender = selectedGender,
                                nik = nik,
                                bankName = selectedBank,
                                accountNumber = accountNumber,
                                accountHolder = accountHolder.ifBlank { fullName },
                                specialties = selectedSpecialties.toList(),
                                password = password
                            )
                            isSubmitting = false
                            showSuccessDialog = true
                        }
                    }
                },
                enabled = isFormValid && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    text = if (isSubmitting) "Mendaftarkan..." else "Kirim Pendaftaran Mitra 🚀",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
