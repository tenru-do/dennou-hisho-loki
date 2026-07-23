# Publishing checklist

Before making the repository public:

- [ ] Confirm no API keys or OAuth credentials are committed.
- [ ] Confirm no personal calendar/mail logs are committed.
- [ ] Confirm no ADB dumps, screenshots, generated APKs, or debug keystores are committed.
- [ ] Decide the open-source license.
- [ ] Replace `com.example.*` packages if you want a stable public package identity.
- [ ] Decide whether to include the mascot asset or replace it with a clearly licensed asset.
- [ ] Add reproducible Gradle builds or document the manual build process.
- [ ] Add screenshots using dummy data only.
- [ ] Add a disclaimer that this is not an official Rokid/Google/Gemini project.
- [ ] Keep `RELEASE_NOTES.md` visible from the README.
- [ ] Keep the alpha/prototype/no-warranty disclaimer visible before public release.
- [ ] Clearly state that this is not suitable for safety-critical, medical, legal, financial, emergency, or production use.
- [ ] Clearly state that Google Workspace support is not full Workspace API integration.
- [ ] Clearly state that mail support currently depends on Android notification access.
- [ ] Clearly state that local Wi-Fi/tethering/network restrictions may prevent phone-to-glasses communication.
- [ ] Keep the Gemini 429/quota/billing warning visible in the README.
- [ ] Keep the "prototype / needs tuning" warning visible before public release.
- [ ] Test a fresh clone build/install on a clean machine.
