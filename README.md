# Voice Of Melody — Content Tracker

An Android app built to help an Instagram music page (Voice Of Melody) track its posting history and content ideas in one place — no more scrolling back through the feed to remember what's already been posted.

## Features

- **Dashboard** — quick overview of posting activity
- **Songs Tracker** — log every song post: title, movie, singers, language, and notes
- **Idea Vault** — capture content ideas before they turn into posts
- **Backup & Restore** — export/import data as JSON or CSV via clipboard, so nothing is lost if the app is reinstalled
- **Light / Dark / System theme** support

## Tech Stack

- Kotlin
- Jetpack Compose
- Room (local SQLite database)
- Material 3

## How this was built

This app was built using [Google AI Studio](https://ai.studio) and Android Studio, with AI agent assistance handling most of the implementation (a "vibe-coded" project). It was built for a real use case — helping a family member manage content for their Instagram page — and then reviewed and hardened afterward (removing an unnecessary PIN lock feature, checking for security issues, and cleaning up the build for public release).

Being upfront about this: it's a practical, functional app rather than a from-scratch deep-dive into Android architecture, and it's shared here as an honest example of a real AI-assisted build.

## Run Locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for reference)
5. Run the app on an emulator or physical device
