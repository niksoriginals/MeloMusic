# Melo v0.0.2

## Playback fix (critical)
- Fixed YouTube playback breaking after the latest player rotation (403 / Source error).
- Fixed poToken roles: the player request now gets the session-bound token and the stream URL gets the video-id bound token.
- Stream validation now uses byte-range GET requests — googlevideo rejects HEAD requests even for valid URLs.
- Added bounded-range streaming so the first media request always carries a valid Range header.
- Switched the primary client to WEB_REMIX, which serves full-file streams with poToken URLs.

## Remote configuration
- Player configs and the BotGuard payload are now fetched from the public MeloYtcipher repository at runtime, so future player rotations are fixed without an APK update.
- Automated upstream player-config sync removed; configs are now fully self-maintained.
