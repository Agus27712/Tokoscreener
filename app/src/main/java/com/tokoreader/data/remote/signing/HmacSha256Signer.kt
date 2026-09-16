package com.tokoreader.data.remote.signing

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacSha256Signer {
    fun sign(secret: String, payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)
        val hmacData = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return hmacData.joinToString("") { "%02x".format(it) }
    }
}
