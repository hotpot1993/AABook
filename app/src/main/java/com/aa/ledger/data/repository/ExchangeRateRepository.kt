package com.aa.ledger.data.repository

import com.aa.ledger.data.local.dao.ExchangeRateDao
import com.aa.ledger.data.local.entity.ExchangeRateEntity
import com.aa.ledger.data.remote.ExchangeRateApi
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// 精选货币分组
data class CurrencyGroup(val region: String, val currencies: List<Pair<String, String>>)

@Singleton
class ExchangeRateRepository @Inject constructor(
    private val exchangeRateDao: ExchangeRateDao,
    private val api: ExchangeRateApi
) {
    companion object {
        // 精选 20 种常用货币，按区域分组
        val CURATED_CURRENCIES: List<CurrencyGroup> = listOf(
            CurrencyGroup("亚洲", listOf(
                "CNY" to "人民币",
                "HKD" to "港币",
                "MOP" to "澳门元",
                "JPY" to "日元",
                "KRW" to "韩元",
                "SGD" to "新加坡元",
                "THB" to "泰铢",
                "MYR" to "马来西亚林吉特",
                "INR" to "印度卢比",
                "IDR" to "印尼盾",
                "VND" to "越南盾",
                "AED" to "阿联酋迪拉姆"
            )),
            CurrencyGroup("欧洲", listOf(
                "EUR" to "欧元",
                "GBP" to "英镑",
                "CHF" to "瑞士法郎",
                "SEK" to "瑞典克朗",
                "RUB" to "俄罗斯卢布",
                "TRY" to "土耳其里拉"
            )),
            CurrencyGroup("美洲", listOf(
                "USD" to "美元",
                "CAD" to "加拿大元"
            )),
            CurrencyGroup("大洋洲与非洲", listOf(
                "AUD" to "澳元",
                "ZAR" to "南非兰特"
            ))
        )

        // 扁平化列表（用于快速查找）
        val CURATED_CURRENCY_CODES: Set<String> = CURATED_CURRENCIES
            .flatMap { it.currencies.map { c -> c.first } }.toSet()

        // 默认汇率（当无网络且无缓存时使用）
        val DEFAULT_RATES = mapOf(
            "CNY" to 1.0,
            "HKD" to 1.1,
            "MOP" to 1.13,
            "USD" to 0.14,
            "EUR" to 0.13,
            "JPY" to 20.5,
            "KRW" to 186.0,
            "TWD" to 4.5,
            "HKD" to 1.1,
            "GBP" to 0.11,
            "AUD" to 0.21,
            "SGD" to 0.19,
            "THB" to 5.0,
            "MYR" to 0.66,
            "PHP" to 7.8,
            "VND" to 3500.0,
            "IDR" to 2200.0,
            "INR" to 11.6
        )

        val CURRENCY_NAMES = mapOf(
            // 亚洲
            "CNY" to "人民币",
            "CNH" to "离岸人民币",
            "HKD" to "港币",
            "MOP" to "澳门元",
            "TWD" to "新台币",
            "JPY" to "日元",
            "KRW" to "韩元",
            "KPW" to "朝鲜元",
            "MNT" to "蒙古图格里克",
            "SGD" to "新加坡元",
            "MYR" to "马来西亚林吉特",
            "THB" to "泰铢",
            "IDR" to "印尼盾",
            "PHP" to "菲律宾比索",
            "VND" to "越南盾",
            "LAK" to "老挝基普",
            "KHR" to "柬埔寨瑞尔",
            "MMK" to "缅甸元",
            "BND" to "文莱元",
            "INR" to "印度卢比",
            "PKR" to "巴基斯坦卢比",
            "BDT" to "孟加拉塔卡",
            "LKR" to "斯里兰卡卢比",
            "NPR" to "尼泊尔卢比",
            "BTN" to "不丹努尔特鲁姆",
            "MVR" to "马尔代夫拉菲亚",
            // 中亚 / 中东
            "KZT" to "哈萨克斯坦坚戈",
            "UZS" to "乌兹别克斯坦索姆",
            "TMT" to "土库曼斯坦马纳特",
            "KGS" to "吉尔吉斯斯坦索姆",
            "TJS" to "塔吉克斯坦索莫尼",
            "AFN" to "阿富汗尼",
            "IRR" to "伊朗里亚尔",
            "IQD" to "伊拉克第纳尔",
            "SYP" to "叙利亚镑",
            "LBP" to "黎巴嫩镑",
            "JOD" to "约旦第纳尔",
            "ILS" to "以色列新谢克尔",
            "SAR" to "沙特里亚尔",
            "AED" to "阿联酋迪拉姆",
            "KWD" to "科威特第纳尔",
            "BHD" to "巴林第纳尔",
            "QAR" to "卡塔尔里亚尔",
            "OMR" to "阿曼里亚尔",
            "YER" to "也门里亚尔",
            // 欧洲
            "EUR" to "欧元",
            "GBP" to "英镑",
            "CHF" to "瑞士法郎",
            "SEK" to "瑞典克朗",
            "NOK" to "挪威克朗",
            "DKK" to "丹麦克朗",
            "ISK" to "冰岛克朗",
            "PLN" to "波兰兹罗提",
            "CZK" to "捷克克朗",
            "HUF" to "匈牙利福林",
            "RON" to "罗马尼亚列伊",
            "BGN" to "保加利亚列弗",
            "RSD" to "塞尔维亚第纳尔",
            "HRK" to "克罗地亚库纳",
            "ALL" to "阿尔巴尼亚列克",
            "MDL" to "摩尔多瓦列伊",
            "UAH" to "乌克兰格里夫纳",
            "BYN" to "白俄罗斯卢布",
            "TRY" to "土耳其里拉",
            "RUB" to "俄罗斯卢布",
            "GEL" to "格鲁吉亚拉里",
            "AMD" to "亚美尼亚德拉姆",
            "AZN" to "阿塞拜疆马纳特",
            // 北美
            "USD" to "美元",
            "CAD" to "加拿大元",
            "MXN" to "墨西哥比索",
            "BMD" to "百慕大元",
            "BSD" to "巴哈马元",
            "BBD" to "巴巴多斯元",
            "BZD" to "伯利兹元",
            "CRC" to "哥斯达黎加科朗",
            "DOP" to "多米尼加比索",
            "GTQ" to "危地马拉格查尔",
            "HNL" to "洪都拉斯伦皮拉",
            "HTG" to "海地古德",
            "JMD" to "牙买加元",
            "NIO" to "尼加拉瓜科多巴",
            "PAB" to "巴拿马巴波亚",
            "TTD" to "特立尼达多巴哥元",
            "XCD" to "东加勒比元",
            "CUP" to "古巴比索",
            // 南美
            "BRL" to "巴西雷亚尔",
            "ARS" to "阿根廷比索",
            "CLP" to "智利比索",
            "COP" to "哥伦比亚比索",
            "PEN" to "秘鲁索尔",
            "UYU" to "乌拉圭比索",
            "PYG" to "巴拉圭瓜拉尼",
            "BOB" to "玻利维亚诺",
            "VES" to "委内瑞拉玻利瓦尔",
            "SRD" to "苏里南元",
            "GYD" to "圭亚那元",
            // 大洋洲
            "AUD" to "澳元",
            "NZD" to "新西兰元",
            "FJD" to "斐济元",
            "PGK" to "巴布亚新几内亚基那",
            "SBD" to "所罗门群岛元",
            "TOP" to "汤加潘加",
            "VUV" to "瓦努阿图瓦图",
            "WST" to "萨摩亚塔拉",
            // 非洲
            "ZAR" to "南非兰特",
            "EGP" to "埃及镑",
            "NGN" to "尼日利亚奈拉",
            "KES" to "肯尼亚先令",
            "GHS" to "加纳塞地",
            "MAD" to "摩洛哥迪拉姆",
            "TND" to "突尼斯第纳尔",
            "DZD" to "阿尔及利亚第纳尔",
            "LYD" to "利比亚第纳尔",
            "SDG" to "苏丹镑",
            "SSP" to "南苏丹镑",
            "ETB" to "埃塞俄比亚比尔",
            "TZS" to "坦桑尼亚先令",
            "UGX" to "乌干达先令",
            "RWF" to "卢旺达法郎",
            "BIF" to "布隆迪法郎",
            "AOA" to "安哥拉宽扎",
            "ZMW" to "赞比亚克瓦查",
            "MWK" to "马拉维克瓦查",
            "MZN" to "莫桑比克梅蒂卡尔",
            "BWP" to "博茨瓦纳普拉",
            "NAD" to "纳米比亚元",
            "ZWL" to "津巴布韦元",
            "MUR" to "毛里求斯卢比",
            "SCR" to "塞舌尔卢比",
            "SOS" to "索马里先令",
            "CDF" to "刚果法郎",
            "XAF" to "中非金融合作法郎",
            "XOF" to "西非金融合作法郎",
            "XPF" to "太平洋法郎",
            "GMD" to "冈比亚达拉西",
            "GNF" to "几内亚法郎",
            "SLL" to "塞拉利昂利昂",
            "LRD" to "利比里亚元",
            "CVE" to "佛得角埃斯库多",
            "STN" to "圣多美多布拉",
            "MRU" to "毛里塔尼亚乌吉亚",
            // 其他
            "XAG" to "银（盎司）",
            "XAU" to "金（盎司）",
            "XPT" to "铂（盎司）",
            "XPD" to "钯（盎司）",
            "SDR" to "特别提款权",
            "BTC" to "比特币",
            "XDR" to "IMF特别提款权"
        )
    }

    fun getAllRates(): Flow<List<ExchangeRateEntity>> = exchangeRateDao.getAllRates()

    suspend fun getRate(currencyCode: String): Double {
        return exchangeRateDao.getRateByCode(currencyCode)?.rateToCny
            ?: DEFAULT_RATES[currencyCode]
            ?: 1.0
    }

    /**
     * 从网络刷新汇率缓存。成功返回 true；失败或首次运行则回退到默认汇率。
     */
    suspend fun refreshRates(): Boolean {
        return try {
            // 尝试从 API 获取实时汇率（人民币 CNY 为基准）
            val response = api.getRates()
            if (response.result == "success") {
                val rates = response.conversion_rates.map { (code, rate) ->
                    // exchangerate-api 返回的是 1 CNY = X 外币，需要取倒数转为 1 外币 = ? CNY
                    val rateToCny = if (rate != 0.0) 1.0 / rate else 0.0
                    ExchangeRateEntity(
                        currencyCode = code,
                        rateToCny = rateToCny,
                        currencyName = CURRENCY_NAMES[code] ?: code,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                exchangeRateDao.clearAllRates()
                exchangeRateDao.insertRates(rates)
                return true
            }
            fallbackToDefaults()
        } catch (e: Exception) {
            fallbackToDefaults()
        }
    }

    /**
     * 无网络或 API 失败时，使用内置默认汇率填充缓存（仅在首次时填充）
     */
    private suspend fun fallbackToDefaults(): Boolean {
        if (exchangeRateDao.getLatestUpdateTime() == null) {
            val rates = DEFAULT_RATES.map { (code, rate) ->
                ExchangeRateEntity(
                    currencyCode = code,
                    rateToCny = rate,
                    currencyName = CURRENCY_NAMES[code] ?: code,
                    updatedAt = System.currentTimeMillis()
                )
            }
            exchangeRateDao.clearAllRates()
            exchangeRateDao.insertRates(rates)
        }
        return false
    }

    /**
     * 获取汇率最后更新时间（天）
     */
    suspend fun getRateAgeInDays(): Int {
        val latestTime = exchangeRateDao.getLatestUpdateTime() ?: return Int.MAX_VALUE
        val diffMs = System.currentTimeMillis() - latestTime
        return TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
    }

    fun getCurrencyName(code: String): String {
        return CURRENCY_NAMES[code] ?: code
    }
}
