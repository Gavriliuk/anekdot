package fr.anekdot

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@Serializable
data class JokeContent(
    val text: String
)

@Serializable
data class JokeResponse(
    val p: JokeContent
)

interface AnekdotApi {
    @GET("?a=")
    suspend fun getRandomJoke(
        @Query("maxlen") maxLength: Int? = null
    ) : JokeResponse
}

// Этот метод понадобился для поддержки Android 6 (SDK 23)
fun getOkHttpClient(context: Context): OkHttpClient {
    // 1. Загружаем сертификат из res/raw
    val cf = CertificateFactory.getInstance("X.509")
    val certInputStream = context.resources.openRawResource(R.raw.isrg_root_x1)
    val certificate = certInputStream.use { cf.generateCertificate(it) }

    // 2. Создаем KeyStore и добавляем туда этот сертификат
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
        load(null, null)
        setCertificateEntry("isrg_root_x1", certificate)
    }

    // 3. Создаем TrustManager, который верит нашему KeyStore
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
        init(keyStore)
    }

    val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, tmf.trustManagers, null)
    }

    // 4. Собираем клиент
    return OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, tmf.trustManagers[0] as X509TrustManager)
        .build()
}

object RetrofitInstance {
    private val _jsonConfig = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val _contentType = "application/json".toMediaType()

    val api: AnekdotApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://anekdot.fr/")
            .client(getOkHttpClient(App.getContext()))
            .addConverterFactory(_jsonConfig.asConverterFactory(_contentType))
            .build()
            .create(AnekdotApi::class.java)
    }
}

