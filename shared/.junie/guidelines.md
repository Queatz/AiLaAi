# Project Guidelines

This document outlines the core guidelines for development in the Junie project.

## Project Structure

    android/      # Android mobile application
    backend/      # Backend server application
    shared/       # Shared code and resources
        models/   # Data models and entities
        api/      # API interfaces and implementations
        config/   # Configuration and settings
        content/  # Content and shared resources
        push/     # Push notifications system
        reminders/# Reminders system
        widgets/  # Content widgets system
    web/         # Web application

## Build Process

1. Verify cross-platform compatibility

## Code Style Guidelines

1. Kotlin
    - Follow official Kotlin coding conventions
    - Use data classes for models
    - Use KotlinX DateTime library for date and time handling
    - Implement proper null safety
    - Use sealed classes for finite state handling
    - Aggressively reuse code when possible
    - Always create new components in their own files
