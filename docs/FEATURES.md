# ✨ Melo (v0.0.1) — Complete Features Guide

> An exhaustive feature matrix of **Melo**, a modern, ad-free Android music app built with Kotlin, Jetpack Compose, AndroidX Media3 (ExoPlayer), and Hilt.

---

## 📄 Table of Contents
- [🎵 Audio Playback & Streaming](#-audio-playback--streaming)
- [🎤 Synchronized Lyrics & AI Translation](#-synchronized-lyrics--ai-translation)
- [🔍 Discovery & Song Identifier (Melo Find)](#-discovery--song-identifier-melo-find)
- [📚 Library, Offline Downloads & Local Media](#-library-offline-downloads--local-media)
- [🎨 Design, Dynamic Themes & Glassmorphism](#-design-dynamic-themes--glassmorphism)
- [🌐 Integrations & Extensions](#-integrations--extensions)
- [⚙️ Audio Customization & Equalizer](#️-audio-customization--equalizer)

---

## 🎵 Audio Playback & Streaming

* **Ad-Free YouTube Music Catalog**: Stream millions of songs, albums, singles, and live performances without ads or subscriptions.
* **Lossless FLAC Audio Hub**: Dedicated section for high-fidelity 16-bit and 24-bit uncompressed audio streaming.
* **Background Audio Playback**: Uninterrupted playback when your screen is turned off or when switching to other apps.
* **Seamless Audio Crossfade**: Smoothly fade between tracks with customizable crossfade duration (0s to 12s).
* **Gapless Playback**: Zero-delay transition between album tracks.
* **Pause on Disconnect / Mute**: Automatically pauses music when headphones unplug, Bluetooth disconnects, or system volume is muted.
* **Smart Audio Caching**: Caches played tracks locally so repeat plays don't consume mobile data.
* **Format Support**: Plays Opus, AAC, MP3, FLAC, M4A, OGG, and WAV.

---

## 🎤 Synchronized Lyrics & AI Translation

* **Real-Time Word-by-Word Lyrics**: Karaoke-style word highlighting synced in real time with audio playback.
* **Multi-Provider Lyric Engine**: Automatically tries active lyric providers in sequence until a match is found:
  1. `LRCLIB` (Open-source time-synced lyrics)
  2. `Paxsenix` (Time-synced and word-by-word lyrics)
  3. `KuGou` (International & Asian lyric database)
  4. `YouTubeSubtitle` (Official video subtitles & timed captions)
  5. `YouTubeMusic` (Official YouTube Music synchronized lyrics)
* **AI-Powered Lyric Translation**: Real-time translation of song lyrics into any target language using OpenAI / OpenRouter models.
* **Romanized Lyrics**: Automatic romanization of non-Latin text (Japanese Rōmaji, Korean Pinyin/Hangul, Chinese Pinyin).
* **Shareable Lyric Card Generator**: Create beautiful image cards of your favorite lyric quotes with customizable background cards and fonts.

---

## 🔍 Discovery & Song Identifier (Melo Find)

* **Melo Find (Song Identifier)**: Record background music in your room via microphone to instantly identify song title, artist, and album.
* **Quick Settings Tile**: One-tap song recognition directly from your Android notification bar.
* **Global Top 100 Charts**: Explore daily top trending charts, viral hits, and top artists worldwide or by country.
* **Moods & Genres Hub**: Browse curated categories including *Chill, Focus, Workout, Party, Romance, Gaming, Commute, Energy*.
* **Melo Brain Neural Queue**: AI-driven recommendation engine that auto-queues matching tracks based on your listening momentum.
* **Speed Dial & Quick Picks**: Instant grid access to your most frequently played songs, artists, and mixes.

---

## 📚 Library, Offline Downloads & Local Media

* **Offline Download Manager**: Download songs, full albums, and playlists to your phone storage for offline playback.
* **Local Device Audio Scanner**: Scans internal storage and SD cards to play local MP3, FLAC, and AAC files.
* **Spotify Playlist Importer**: One-click import of Spotify playlists and Liked Songs directly into Melo.
* **Custom Playlist Builder**: Create, rename, reorder, export, and import custom local playlists.
* **Listening History & Analytics**: Track played songs, total listening duration, and top played artists.

---

## 🎨 Design, Dynamic Themes & Glassmorphism

* **Material 3 Expressive Design**: Built 100% with Jetpack Compose following modern Material Design principles.
* **Dynamic Color Engine**: Extracts color palettes from current album cover art to dynamically tint player UI and controls.
* **Pure Black OLED Mode**: OLED-optimized true black dark mode to save battery.
* **Apple Music Style Canvas**: Live animated video backgrounds and visual artwork during playback.
* **Glassmorphism (Liquid Glass)**: Customizable frosted glass blur, lens height, depth vibrancy, surface translucency, and depth effects.
* **Customizable Layouts**: Toggle between Big and Small grid thumbnail sizes for home and library pages.

---

## 🌐 Integrations & Extensions

* **Listen Together (Party Mode)**: Host or join real-time synchronized music listening sessions over local network / Wi-Fi.
* **Discord Rich Presence (RPC)**: Displays your currently playing song, artist, album art, and progress bar directly on your Discord profile status.
* **ListenBrainz Scrobbler**: Automatic scrobbling to track listening history on ListenBrainz.
* **Universal Link Sharing (Odesli / Song.link)**: Share songs via universal links so friends on Spotify, Apple Music, or Tidal can open them.
* **Home Screen Widgets**: Turntable & standard music control widgets for Android home screen.

---

## ⚙️ Audio Customization & Equalizer

* **Built-in Equalizer**: Built-in 5-band / 10-band equalizer with presets (*Bass Boost, Treble Boost, Vocal, Rock, Pop, Classical*).
* **Audio Normalization (ReplayGain)**: Loudness equalization across tracks to prevent volume jumps.
* **Pitch & Speed Controls**: Adjust playback speed (0.5x to 2.0x) and pitch in real time.
