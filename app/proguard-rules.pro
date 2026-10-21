# Minify ist ausgeschaltet. Falls du es später aktivierst, hier Regeln ergänzen.

# Credential Manager (Anmelden mit Google)
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}
