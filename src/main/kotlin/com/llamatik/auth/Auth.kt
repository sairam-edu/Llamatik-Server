package com.llamatik.auth

import io.ktor.util.hex
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

val hashKey = hex(System.getenv("SECRET_KEY"))

val hmacKey = SecretKeySpec(hashKey, "HmacSHA1")

// A single pre-initialized Mac instance used as a template.
// Mac.getInstance() triggers a JCA provider lookup and allocates a new engine object on
// every call; hmac.init() re-derives the sub-keys from the secret.  Both operations are
// measurably expensive under load.  By calling clone() on a ready-to-use template we skip
// the provider lookup and key-schedule setup entirely — clone() copies already-computed
// state in O(1) instead of recomputing it.  Mac is not thread-safe, so each call gets its
// own clone rather than sharing the template directly.
private val hmacTemplate: Mac = Mac.getInstance("HmacSHA1").also { it.init(hmacKey) }

fun hash(password: String): String {
    val hmac = hmacTemplate.clone() as Mac
    return hex(hmac.doFinal(password.toByteArray(Charsets.UTF_8)))
}
