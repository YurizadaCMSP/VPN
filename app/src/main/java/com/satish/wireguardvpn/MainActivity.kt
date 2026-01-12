package com.satish.wireguardvpn

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.satish.wireguardvpn.ui.theme.WireguardVPNTheme
import com.satish.wireguardvpn.vpn.MyWireGuardService
import com.satish.wireguardvpn.vpn.NetworkRulesViewModel

class MainActivity : ComponentActivity() {

    private val vpnPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        startService(Intent(this, MyWireGuardService::class.java).apply {
            action = MyWireGuardService.ACTION_START_TUNNEL
        })
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("MainActivity", "Permissão de notificação concedida.")
            } else {
                Log.d("MainActivity", "Permissão de notificação negada.")
            }
            startVpn()
        }

    private fun startVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermission.launch(intent)
        } else {
            startService(Intent(this, MyWireGuardService::class.java).apply {
                action = MyWireGuardService.ACTION_START_TUNNEL
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WireguardVPNTheme {
                val vm = remember { NetworkRulesViewModel(application) }
                TelaInicial(
                    estaConectado = vm.vpnRunning.collectAsState().value,
                    aoAlterar = {
                        if (!vm.vpnRunning.value) {
                            if (ContextCompat.checkSelfPermission(
                                    this, Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                startVpn()
                            } else {
                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            stopService(Intent(this, MyWireGuardService::class.java))
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTelaInicial() {
    WireguardVPNTheme {
        TelaInicial(estaConectado = false, aoAlterar = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTelaInicialConectado() {
    WireguardVPNTheme {
        TelaInicial(estaConectado = true, aoAlterar = {})
    }
}

@Composable
private fun TelaInicial(
    estaConectado: Boolean,
    aoAlterar: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "VPN PlayVicioRP",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = if (estaConectado) "Status: Conectado" else "Status: Desconectado",
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            Button(onClick = aoAlterar) {
                Text(if (estaConectado) "Desconectar" else "Conectar")
            }
        }
    }
}
