# v530 Bluetooth audio / seated motion

- BT menu replaces the former top-level WiFi button: +30 seconds, -30 seconds, pause, play.
- Wi-Fi reconnect/status is available at SET > Wi-Fi.
- Seek requires the Bluetooth media session to advertise ACTION_SEEK_TO and a valid position. Unsupported players receive an explicit message; next-track or unbounded fast-forward is never substituted. A sent operation is not reported as verified movement.
- Playing position is extrapolated only from an update <=30 seconds old. Requests clamp to zero / known duration.
- Seated meshes pin head, neck and shoulder attachment; breathing displacement is bounded below that region. Transition flow gets the same guard in reused storage. Face overlays remain at their original attachment.
- Regression checks cover relative bounds, negative commands, fixed upper mesh, retained lower movement and transition bounds. Android build passes.
- Actual worn animation quality and remote-player seek response require real-use confirmation.

Installed glass v530 and phone v183. Native UI audit PASS for BT button and SET Wi-Fi entry. Current Bluetooth session reports NOT_ADVERTISED for ACTION_SEEK_TO, so actual 30-second seeking cannot be confirmed/enabled for this connection. No playback operation was sent.
