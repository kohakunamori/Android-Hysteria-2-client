package us.leaf3stones.hy2droid.data.model

import java.io.Serializable

/**
 * Enhanced Hysteria 2.6.5 Configuration Model
 * Based on official documentation: https://v2.hysteria.network/docs/advanced/Full-Client-Config/
 */
data class HysteriaConfigV2(
    // ==================== Configuration Identity ====================
    /** Unique configuration ID */
    val id: String = java.util.UUID.randomUUID().toString(),
    
    /** Configuration name for identification */
    val name: String = "默认配置",
    
    // ==================== Basic Configuration ====================
    /** Server address (host:port or host:port1-port2 for port hopping) */
    val server: String = "",
    
    /** Authentication password (use username:password for userpass auth) */
    val auth: String = "",
    
    // ==================== TLS Configuration ====================
    /** Server Name Indication for TLS verification */
    val tlsSni: String = "",
    
    /** Disable TLS certificate verification (DANGEROUS - only for testing) */
    val tlsInsecure: Boolean = false,
    
    /** Pin SHA256 certificate fingerprint for verification */
    val tlsPinSHA256: String = "",
    
    // ==================== Obfuscation Configuration ====================
    /** Enable Salamander obfuscation to bypass DPI */
    val obfsEnabled: Boolean = false,
    
    /** Obfuscation password (must match server) */
    val obfsPassword: String = "",
    
    // ==================== Bandwidth Configuration ====================
    /** Upload bandwidth in Mbps (0 = use BBR instead of Brutal) */
    val bandwidthUp: Int = 100,
    
    /** Download bandwidth in Mbps (0 = use BBR instead of Brutal) */
    val bandwidthDown: Int = 200,
    
    // ==================== QUIC Parameters ====================
    /** Maximum idle timeout (default: 30s) */
    val quicMaxIdleTimeout: Int = 30,
    
    /** Keep-alive period (default: 10s) */
    val quicKeepAlivePeriod: Int = 10,
    
    /** Disable Path MTU Discovery */
    val quicDisablePathMTUDiscovery: Boolean = false,
    
    // ==================== Proxy Configuration ====================
    /** SOCKS5 listen address */
    val socks5Listen: String = "127.0.0.1:1080",
    
    /** HTTP proxy listen address */
    val httpListen: String = "127.0.0.1:1081",
    
    /** Enable both SOCKS5 and HTTP on same port (v2.4.1+) */
    val dualModeProxy: Boolean = false,
    
    // ==================== Performance Configuration ====================
    /** Enable Fast Open to reduce RTT */
    val fastOpen: Boolean = true,
    
    /** Lazy mode - connect only when needed */
    val lazy: Boolean = false,
    
    // ==================== Port Hopping (Advanced) ====================
    /** Port hopping interval in seconds (0 = disabled) */
    val portHopInterval: Int = 0,
    
    // ==================== Route Rules (ACL) ====================
    /** Route rules for domain-based traffic control */
    val routeRules: RouteRulesConfig? = null,
) : Serializable {
    
    /**
     * Generate complete Hysteria configuration YAML
     */
    fun generateConfig(): String {
        android.util.Log.d("HysteriaConfigV2", "=== Generating config for ${this.name} ===")
        android.util.Log.d("HysteriaConfigV2", "Server: $server")
        android.util.Log.d("HysteriaConfigV2", "Port Hop Interval: $portHopInterval")
        android.util.Log.d("HysteriaConfigV2", "Route Rules: enabled=${routeRules?.enabled}, rules count=${routeRules?.rules?.size}")
        
        val sb = StringBuilder()
        
        // Server
        sb.appendLine("server: $server")
        sb.appendLine()
        
        // Auth
        sb.appendLine("auth: $auth")
        sb.appendLine()
        
        // TLS Configuration
        if (tlsSni.isNotBlank() || tlsInsecure || tlsPinSHA256.isNotBlank()) {
            sb.appendLine("tls:")
            if (tlsSni.isNotBlank()) {
                sb.appendLine("  sni: $tlsSni")
            }
            if (tlsInsecure) {
                sb.appendLine("  insecure: true")
            }
            if (tlsPinSHA256.isNotBlank()) {
                sb.appendLine("  pinSHA256: $tlsPinSHA256")
            }
            sb.appendLine()
        }
        
        // Obfuscation
        if (obfsEnabled && obfsPassword.isNotBlank()) {
            sb.appendLine("obfs:")
            sb.appendLine("  type: salamander")
            sb.appendLine("  salamander:")
            sb.appendLine("    password: $obfsPassword")
            sb.appendLine()
        }
        
        // Bandwidth (0 means use BBR)
        if (bandwidthUp > 0 || bandwidthDown > 0) {
            sb.appendLine("bandwidth:")
            sb.appendLine("  up: $bandwidthUp mbps")
            sb.appendLine("  down: $bandwidthDown mbps")
            sb.appendLine()
        }
        
        // QUIC Parameters
        sb.appendLine("quic:")
        sb.appendLine("  maxIdleTimeout: ${quicMaxIdleTimeout}s")
        sb.appendLine("  keepAlivePeriod: ${quicKeepAlivePeriod}s")
        if (quicDisablePathMTUDiscovery) {
            sb.appendLine("  disablePathMTUDiscovery: true")
        }
        sb.appendLine()
        
        // Port Hopping
        android.util.Log.d("HysteriaConfigV2", "Port hopping: portHopInterval=$portHopInterval")
        if (portHopInterval > 0) {
            sb.appendLine("transport:")
            sb.appendLine("  type: udp")
            sb.appendLine("  udp:")
            sb.appendLine("    hopInterval: ${portHopInterval}s")
            sb.appendLine()
            android.util.Log.d("HysteriaConfigV2", "Port hopping enabled with interval ${portHopInterval}s")
        }
        
        // Proxy Configuration
        val effectiveSocks5 = if (dualModeProxy) socks5Listen else socks5Listen
        val effectiveHttp = if (dualModeProxy) socks5Listen else httpListen
        
        sb.appendLine("socks5:")
        sb.appendLine("  listen: $effectiveSocks5")
        sb.appendLine()
        
        sb.appendLine("http:")
        sb.appendLine("  listen: $effectiveHttp")
        sb.appendLine()
        
        // Performance
        if (fastOpen) {
            sb.appendLine("fastOpen: true")
            sb.appendLine()
        }
        
        if (lazy) {
            sb.appendLine("lazy: true")
            sb.appendLine()
        }
        
        // Route Rules (ACL) - DISABLED: All traffic goes through Hysteria tunnel
        // No ACL rules generated to ensure all traffic is proxied
        
        return sb.toString()
    }
    
    /**
     * Validate configuration
     */
    fun validate(): ConfigValidationResult {
        val errors = mutableListOf<String>()
        
        // Basic validation
        if (server.isBlank()) {
            errors.add("服务器地址不能为空")
        }
        if (auth.isBlank()) {
            errors.add("认证密码不能为空")
        }
        
        // Port validation - support both single port and port range
        if (server.contains(":")) {
            val portPart = server.substringAfterLast(":")
            if (portPart.contains("-")) {
                // Port range format (e.g., 30000-50000)
                val portRange = portPart.split("-")
                if (portRange.size != 2) {
                    errors.add("端口范围格式错误，应为 port1-port2")
                } else {
                    val startPort = portRange[0].toIntOrNull()
                    val endPort = portRange[1].toIntOrNull()
                    if (startPort == null || endPort == null) {
                        errors.add("端口范围必须是数字")
                    } else if (startPort !in 1..65535 || endPort !in 1..65535) {
                        errors.add("端口号必须在 1-65535 之间")
                    } else if (startPort >= endPort) {
                        errors.add("起始端口必须小于结束端口")
                    }
                }
            } else {
                // Single port format
                val port = portPart.toIntOrNull()
                if (port == null || port !in 1..65535) {
                    errors.add("服务器端口号无效 (1-65535)")
                }
            }
        }
        
        // Bandwidth validation
        if (bandwidthUp < 0 || bandwidthDown < 0) {
            errors.add("带宽值不能为负数")
        }
        if (bandwidthUp > 10000 || bandwidthDown > 10000) {
            errors.add("带宽值过大，请检查 (建议不超过10000 Mbps)")
        }
        
        // QUIC timeout validation
        if (quicMaxIdleTimeout < 5 || quicMaxIdleTimeout > 300) {
            errors.add("最大空闲超时应在 5-300 秒之间")
        }
        if (quicKeepAlivePeriod < 5 || quicKeepAlivePeriod > 60) {
            errors.add("保活周期应在 5-60 秒之间")
        }
        
        // Obfuscation validation
        if (obfsEnabled && obfsPassword.isBlank()) {
            errors.add("启用混淆时必须设置混淆密码")
        }
        
        // Port hopping validation
        if (portHopInterval < 0) {
            errors.add("端口跳跃间隔不能为负数")
        }
        
        return ConfigValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
    
    companion object {
        /**
         * Create default configuration with recommended settings
         */
        fun createDefault() = HysteriaConfigV2(
            server = "",
            auth = "",
            bandwidthUp = 100,
            bandwidthDown = 200,
            quicMaxIdleTimeout = 30,
            quicKeepAlivePeriod = 10,
            fastOpen = true,
            lazy = false
        )
    }
}

/**
 * Configuration validation result
 */
data class ConfigValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    val errorMessage: String
        get() = errors.joinToString("\n")
}

/**
 * Configuration list item for multiple configs management
 */
data class ConfigListItem(
    val id: String,
    val name: String,
    val server: String,
    val lastUsed: Long = System.currentTimeMillis()
) : Serializable
