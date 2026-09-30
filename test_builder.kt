import com.spotify.sdk.android.auth.AuthorizationRequest
fun test() {
    val b = AuthorizationRequest.Builder("id", AuthorizationResponse.Type.CODE, "uri")
    b.setCustomParam("code_challenge_method", "S256")
}
