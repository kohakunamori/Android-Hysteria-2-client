package us.leaf3stones.hy2droid.data.model

import java.io.Serializable

/**
 * Route rule modes for domain filtering
 */
enum class RouteMode {
    /** Direct mode - bypass proxy for matched domains */
    DIRECT,
    /** Proxy mode - use proxy for matched domains */
    PROXY,
    /** Block mode - block access to matched domains */
    BLOCK
}

/**
 * Route rule for domain-based traffic control
 */
data class RouteRule(
    /** Rule ID */
    val id: String = java.util.UUID.randomUUID().toString(),
    
    /** Domain or domain pattern (supports wildcard: *.example.com) */
    val domain: String = "",
    
    /** Route mode: DIRECT (bypass), PROXY (use proxy), or BLOCK */
    val mode: RouteMode = RouteMode.PROXY,
    
    /** Rule description/comment */
    val description: String = "",
    
    /** Whether this rule is enabled */
    val enabled: Boolean = true
) : Serializable {
    
    /**
     * Check if domain matches this rule
     */
    fun matches(domain: String): Boolean {
        if (!enabled) return false
        
        val pattern = this.domain.lowercase()
        val testDomain = domain.lowercase()
        
        return when {
            // Exact match
            pattern == testDomain -> true
            
            // Wildcard match (*.example.com matches sub.example.com)
            pattern.startsWith("*.") -> {
                val baseDomain = pattern.substring(2)
                testDomain.endsWith(baseDomain) || testDomain == baseDomain
            }
            
            // Suffix match (.example.com matches *.example.com)
            pattern.startsWith(".") -> {
                testDomain.endsWith(pattern)
            }
            
            else -> false
        }
    }
}

/**
 * Route rules configuration for a Hysteria config
 */
data class RouteRulesConfig(
    /** Configuration ID this route rules belong to */
    val configId: String = "",
    
    /** Default mode for domains not matching any rule */
    val defaultMode: RouteMode = RouteMode.PROXY,
    
    /** List of route rules */
    val rules: List<RouteRule> = emptyList(),
    
    /** Whether to enable route rules */
    val enabled: Boolean = false
) : Serializable {
    
    /**
     * Find matching rule for a domain
     */
    fun findMatchingRule(domain: String): RouteRule? {
        if (!enabled) return null
        return rules.firstOrNull { it.matches(domain) }
    }
    
    /**
     * Get route mode for a domain
     */
    fun getRouteMode(domain: String): RouteMode {
        if (!enabled) return RouteMode.PROXY
        return findMatchingRule(domain)?.mode ?: defaultMode
    }
    
    /**
     * Generate Hysteria ACL configuration for Android CLIENT
     * 
     * Important: In Hysteria client mode, the default behavior is to proxy ALL traffic
     * through the Hysteria tunnel. ACL is used to specify exceptions.
     * 
     * Internal outbounds:
     * - direct - bypass Hysteria, connect directly
     * - reject - block the connection
     * - (no rule) - goes through Hysteria tunnel (default behavior)
     * 
     * Rule order matters: first match wins!
     * 
     * Strategy for "only nyaneko.cn goes through proxy":
     * Since we can't explicitly mark proxy, we use negative matching:
     * 1. Don't add nyaneko.cn to any rule (so it goes through proxy by default)
     * 2. Add direct rules for everything else AFTER
     * 3. This ensures nyaneko.cn doesn't match direct rules
     * 
     * CRITICAL: PROXY mode rules must come FIRST, implemented as "no rule" (fallthrough)
     * Then direct/reject rules, so they don't override the proxy domains
     */
    fun generateACL(): String {
        if (!enabled) return ""
        
        val sb = StringBuilder()
        
        // When default mode is PROXY, we need to ensure proxy domains don't match direct rules
        // Strategy: Don't add proxy domain rules - let them fall through
        // But we need to track them to ensure they're not added as direct
        val proxyDomains = rules.filter { it.enabled && it.mode == RouteMode.PROXY }
            .map { rule ->
                if (rule.domain.startsWith("*.")) {
                    "suffix:${rule.domain.substring(2)}"
                } else {
                    rule.domain
                }
            }.toSet()
        
        android.util.Log.d("RouteRules", "generateACL: enabled rules count=${rules.filter { it.enabled }.size}")
        android.util.Log.d("RouteRules", "generateACL: proxyDomains=$proxyDomains")
        
        // Block rules - highest priority
        val blockRules = rules.filter { it.enabled && it.mode == RouteMode.BLOCK }
        android.util.Log.d("RouteRules", "generateACL: blockRules count=${blockRules.size}")
        blockRules.forEach { rule ->
            val domain = if (rule.domain.startsWith("*.")) {
                "suffix:${rule.domain.substring(2)}"
            } else {
                rule.domain
            }
            sb.appendLine("reject($domain)")
        }
        
        // Direct rules - but skip if domain is in proxy list
        val directRules = rules.filter { it.enabled && it.mode == RouteMode.DIRECT }
        android.util.Log.d("RouteRules", "generateACL: directRules count=${directRules.size}")
        directRules.forEach { rule ->
            val domain = if (rule.domain.startsWith("*.")) {
                "suffix:${rule.domain.substring(2)}"
            } else {
                rule.domain
            }
            // Don't add if this domain should go through proxy
            if (!proxyDomains.contains(domain)) {
                sb.appendLine("direct($domain)")
            }
        }
        
        // Default mode determines what happens to unmatched traffic
        android.util.Log.d("RouteRules", "generateACL: defaultMode=$defaultMode")
        when (defaultMode) {
            RouteMode.DIRECT -> {
                // Direct all unmatched traffic
                // ONLY add direct(all) if there are no proxy rules
                // Otherwise proxy rules won't work
                if (proxyDomains.isEmpty()) {
                    android.util.Log.d("RouteRules", "generateACL: adding direct(all) - no proxy domains")
                    sb.appendLine("direct(all)")
                } else {
                    android.util.Log.d("RouteRules", "generateACL: NOT adding direct(all) - proxy domains exist: $proxyDomains")
                }
                // If there are proxy rules, don't add direct(all)
                // Let unmatched traffic (proxy domains) go through tunnel
            }
            RouteMode.BLOCK -> {
                // Block all unmatched traffic
                sb.appendLine("reject(all)")
            }
            RouteMode.PROXY -> {
                // Proxy all unmatched traffic (default Hysteria behavior)
                // Don't add direct(all), let unmatched traffic go through tunnel
            }
        }
        
        val result = sb.toString()
        android.util.Log.d("RouteRules", "generateACL: result length=${result.length}, content=\n$result")
        return result
    }
    
    /**
     * Validate route rules
     */
    fun validate(): RouteRulesValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Check for empty domains
        rules.forEachIndexed { index, rule ->
            if (rule.enabled && rule.domain.isBlank()) {
                errors.add("规则 ${index + 1}: 域名不能为空")
            }
            
            // Validate domain format
            if (rule.enabled && rule.domain.isNotBlank()) {
                val domain = rule.domain
                if (!isValidDomainPattern(domain)) {
                    errors.add("规则 ${index + 1}: 域名格式无效: $domain")
                }
            }
        }
        
        // Check for duplicate domains
        val enabledRules = rules.filter { it.enabled }
        val domains = enabledRules.map { it.domain }
        val duplicates = domains.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (duplicates.isNotEmpty()) {
            warnings.add("存在重复的域名规则: ${duplicates.keys.joinToString(", ")}")
        }
        
        // Check for conflicting rules
        enabledRules.forEachIndexed { i, rule1 ->
            enabledRules.drop(i + 1).forEach { rule2 ->
                if (rule1.domain == rule2.domain && rule1.mode != rule2.mode) {
                    errors.add("冲突规则: ${rule1.domain} 同时设置为 ${rule1.mode} 和 ${rule2.mode}")
                }
            }
        }
        
        return RouteRulesValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Validate domain pattern format
     */
    private fun isValidDomainPattern(domain: String): Boolean {
        // Basic domain validation
        if (domain.isBlank()) return false
        
        // Allow wildcard patterns
        val cleanDomain = domain.removePrefix("*.").removePrefix(".")
        
        // Simple validation: check for valid characters
        val domainRegex = Regex("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)*\$")
        return cleanDomain.matches(domainRegex)
    }
    
    companion object {
        /**
         * Create default route rules with common examples
         */
        fun createDefault(configId: String) = RouteRulesConfig(
            configId = configId,
            defaultMode = RouteMode.PROXY,
            rules = listOf(
                RouteRule(
                    domain = "*.cn",
                    mode = RouteMode.DIRECT,
                    description = "中国大陆域名直连",
                    enabled = false
                ),
                RouteRule(
                    domain = "*.baidu.com",
                    mode = RouteMode.DIRECT,
                    description = "百度直连",
                    enabled = false
                ),
                RouteRule(
                    domain = "*.ad.com",
                    mode = RouteMode.BLOCK,
                    description = "拦截广告域名",
                    enabled = false
                )
            ),
            enabled = false
        )
        
        /**
         * Common whitelist domains (China mainland)
         */
        fun getCommonWhitelistDomains() = listOf(
            "*.cn" to "中国大陆域名",
            "*.com.cn" to "中国商业域名",
            "*.gov.cn" to "中国政府域名",
            "*.baidu.com" to "百度",
            "*.qq.com" to "腾讯",
            "*.taobao.com" to "淘宝",
            "*.alipay.com" to "支付宝",
            "*.jd.com" to "京东",
            "*.weibo.com" to "微博",
            "*.163.com" to "网易",
            "*.bilibili.com" to "哔哩哔哩",
            "*.douyin.com" to "抖音",
            "*.xiaomi.com" to "小米"
        )
        
        /**
         * Common blacklist domains (ads & tracking)
         */
        fun getCommonBlacklistDomains() = listOf(
            "*.doubleclick.net" to "Google 广告",
            "*.googleadservices.com" to "Google 广告服务",
            "*.googlesyndication.com" to "Google 广告联盟",
            "*.facebook.com/tr" to "Facebook 追踪",
            "*.google-analytics.com" to "Google 分析",
            "ad.*.com" to "通用广告域名",
            "ads.*.com" to "通用广告服务",
            "tracker.*.com" to "通用追踪器"
        )
    }
}

/**
 * Route rules validation result
 */
data class RouteRulesValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    val errorMessage: String
        get() = errors.joinToString("\n")
    
    val warningMessage: String
        get() = warnings.joinToString("\n")
}

/**
 * Preset route rule configurations
 */
object RouteRulePresets {
    /**
     * Most traffic direct preset - suitable for "only proxy specific domains" scenario
     * Includes: China mainland, private IPs, LAN addresses
     * User can add custom PROXY rules on top of this preset
     */
    fun mostDirect(configId: String) = RouteRulesConfig(
        configId = configId,
        defaultMode = RouteMode.DIRECT,  // Default to direct for unmatched domains
        rules = listOf(
            RouteRule(
                domain = "geoip:private",
                mode = RouteMode.DIRECT,
                description = "私有IP地址（局域网）",
                enabled = true
            ),
            RouteRule(
                domain = "geoip:cn",
                mode = RouteMode.DIRECT,
                description = "中国大陆IP地址",
                enabled = true
            ),
            RouteRule(
                domain = "geosite:cn",
                mode = RouteMode.DIRECT,
                description = "中国大陆网站",
                enabled = true
            )
        ),
        enabled = true
    )
    
    /**
     * China mainland direct preset
     */
    fun chinaDirect(configId: String) = RouteRulesConfig(
        configId = configId,
        defaultMode = RouteMode.PROXY,
        rules = RouteRulesConfig.getCommonWhitelistDomains().map { (domain, desc) ->
            RouteRule(
                domain = domain,
                mode = RouteMode.DIRECT,
                description = desc,
                enabled = true
            )
        },
        enabled = true
    )
    
    /**
     * Block ads and tracking preset
     */
    fun blockAds(configId: String) = RouteRulesConfig(
        configId = configId,
        defaultMode = RouteMode.PROXY,
        rules = RouteRulesConfig.getCommonBlacklistDomains().map { (domain, desc) ->
            RouteRule(
                domain = domain,
                mode = RouteMode.BLOCK,
                description = desc,
                enabled = true
            )
        },
        enabled = true
    )
    
    /**
     * Combined preset: China direct + Block ads
     */
    fun chinaDirectAndBlockAds(configId: String) = RouteRulesConfig(
        configId = configId,
        defaultMode = RouteMode.PROXY,
        rules = RouteRulesConfig.getCommonWhitelistDomains().map { (domain, desc) ->
            RouteRule(
                domain = domain,
                mode = RouteMode.DIRECT,
                description = desc,
                enabled = true
            )
        } + RouteRulesConfig.getCommonBlacklistDomains().map { (domain, desc) ->
            RouteRule(
                domain = domain,
                mode = RouteMode.BLOCK,
                description = desc,
                enabled = true
            )
        },
        enabled = true
    )
}
