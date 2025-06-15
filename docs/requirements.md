# FGO Bot - Project Requirements Document

## 1. Project Overview
FGO Bot is an Android application that automates gameplay in Fate/Grand Order (FGO) by making intelligent decisions based on available servants, craft essences, and battle situations.

## 2. Functional Requirements

### 2.1 Data Management
- [x] Integrate with Atlas Academy API for game data (API interface ready)
- [x] Maintain local database using Room Database (SQLite) with:
  - [x] Servants table (stats, skills, NP effects, rarity, class)
  - [x] Craft Essences table (effects, stats, rarity, type)
  - [x] Command Cards table (effects, damage multipliers, card type)
  - [x] User's inventory table (owned servants/CEs with levels)
  - [x] Quest information table (enemy data, requirements, rewards)
  - [x] Team configurations table (saved team setups)
  - [x] Battle logs table (performance tracking, win/loss records)
- [ ] Implement in-memory caching layer for frequently accessed data
- [ ] Implement data synchronization system with Atlas Academy API
- [ ] Cache frequently accessed data using LruCache
- [ ] Handle offline mode with local database fallback
- [ ] Database indexing for performance optimization
- [ ] Background data sync to minimize UI blocking

### 2.2 Team Building
- [ ] Analyze quest requirements
- [ ] Select optimal team composition
- [ ] Choose appropriate Craft Essences
- [ ] Select support servant
- [ ] Choose Mystic Code
- [ ] Save and load team configurations

### 2.3 Battle Automation
- [ ] Screen capture and analysis
- [ ] Battle state detection
- [ ] Command card selection
- [ ] Skill usage optimization
- [ ] NP timing optimization
- [ ] Error recovery system
- [ ] Battle logging

### 2.4 User Interface
- [x] Basic UI framework with Jetpack Compose
- [x] Reusable UI components (ServantCard, TeamConfigCard)
- [ ] Team management interface
- [ ] Quest selection screen
- [ ] Battle monitoring
- [ ] Settings configuration
- [ ] Progress tracking
- [ ] Error reporting
- [ ] Log viewer

### 2.5 Security & Privacy
- [ ] Secure storage of user data using Android Keystore
- [ ] API key management with local.properties
- [ ] Anti-tampering measures for bot detection avoidance
- [ ] Privacy compliance (no user registration/login required)
- [ ] Local data encryption for sensitive information
- [ ] Secure communication with Atlas Academy API

## 3. Non-Functional Requirements

### 3.1 Performance
- Screen capture latency < 100ms
- Decision making time < 500ms
- App startup time < 3 seconds
- Memory usage < 200MB
- Battery impact < 5% per hour

### 3.2 Reliability
- 99.9% uptime
- Automatic error recovery
- Data backup system
- Crash reporting

### 3.3 Usability
- Intuitive UI/UX
- Clear error messages
- Help documentation
- Tutorial system

### 3.4 Compatibility
- Android 8.0 and above
- Screen resolution support
- Different device sizes
- Various Android manufacturers

### 3.5 Maintainability
- Modular architecture
- Clean code practices
- Comprehensive documentation
- Unit test coverage > 80%

## 4. Technical Requirements

### 4.1 Development Environment
- Android Studio
- Kotlin 1.8+
- Gradle 7.0+
- JDK 11+

### 4.2 Dependencies
- [x] AndroidX libraries
- [x] Room Database (SQLite wrapper)
- [x] Room KTX (Kotlin extensions)
- [x] Room compiler (annotation processing)
- [x] Retrofit (API communication)
- [x] Gson (JSON parsing)
- [x] OkHttp (HTTP client)
- [x] Coroutines (async operations)
- [x] Jetpack Compose (UI framework)
- [ ] OpenCV (image processing)
- [ ] ML Kit (text recognition)
- [ ] TensorFlow Lite (custom models)
- [ ] Collection KTX (caching utilities)

### 4.3 Testing Requirements
- [x] Unit tests (foundation implemented)
- [x] Testing framework setup (JUnit, Room testing, Coroutines testing)
- [ ] Integration tests
- [ ] UI tests
- [ ] Performance tests
- [ ] Security tests

## 5. Project Constraints
- Must comply with FGO terms of service
- Must respect rate limits
- Must handle network issues
- Must be battery efficient
- Must be user-friendly

## 6. Implementation Status

### 6.1 Completed Components (Phase 1)
- [x] **Project Structure**: Clean Architecture with MVVM pattern
- [x] **Database Layer**: Complete Room database with 5 entities and DAOs
- [x] **API Layer**: Atlas Academy API interface and response models
- [x] **Error Handling**: Comprehensive exception hierarchy
- [x] **Logging System**: Structured logging with performance tracking
- [x] **UI Foundation**: Jetpack Compose with reusable components
- [x] **Testing Framework**: Unit tests for core components
- [x] **Build System**: Gradle 8.9 with proper dependencies
- [x] **Accessibility Service**: Basic service for automation

### 6.2 Code Metrics
- **Total Kotlin Files**: 24
- **Database Entities**: 5 (Servant, CraftEssence, Quest, TeamConfig, BattleLog)
- **DAOs**: 5 with comprehensive CRUD operations
- **API Models**: 3 response models with proper serialization
- **UI Components**: 2 reusable Compose components
- **Test Files**: 2 unit test classes

### 6.3 Architecture Overview
```
app/src/main/java/com/fgobot/
├── core/ (3 files)
│   ├── FGOAccessibilityService.kt
│   ├── error/FGOBotException.kt
│   └── logging/FGOLogger.kt
├── data/ (14 files)
│   ├── api/ (4 files)
│   └── database/ (10 files)
├── presentation/ (3 files)
│   ├── MainActivity.kt
│   └── components/ (2 files)
└── domain/ (ready for business logic)
```

## 7. Success Criteria
- Successful team composition
- Accurate battle decisions
- Reliable automation
- Positive user feedback
- Stable performance
- Low error rate 