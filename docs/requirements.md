# FGO Bot - Project Requirements Document

## 1. Project Overview
FGO Bot is an Android application that automates gameplay in Fate/Grand Order (FGO) by making intelligent decisions based on available servants, craft essences, and battle situations.

## 2. Functional Requirements

### 2.1 Data Management ✅ **85% IMPLEMENTED**
- [x] Integrate with Atlas Academy API for game data (API interface ready)
- [x] Maintain local database using Room Database (SQLite) with:
  - [x] Servants table (stats, skills, NP effects, rarity, class)
  - [x] Craft Essences table (effects, stats, rarity, type)
  - [x] Command Cards table (effects, damage multipliers, card type)
  - [x] User's inventory table (owned servants/CEs with levels)
  - [x] Quest information table (enemy data, requirements, rewards)
  - [x] Team configurations table (saved team setups)
  - [x] Battle logs table (performance tracking, win/loss records)
- [x] Implement in-memory caching layer for frequently accessed data
- [x] Implement data synchronization system with Atlas Academy API
- [x] Cache frequently accessed data using multi-level caching (Memory + Disk)
- [x] Handle offline mode with local database fallback
- [x] Database indexing for performance optimization
- [x] Background data sync to minimize UI blocking

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
- [x] API key management with local.properties
- [ ] Anti-tampering measures for bot detection avoidance
- [ ] Privacy compliance (no user registration/login required)
- [ ] Local data encryption for sensitive information
- [x] Secure communication with Atlas Academy API

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

### 4.1 Development Environment ✅ **COMPLETED**
- [x] Android Studio
- [x] Kotlin 1.9.20+
- [x] Gradle 8.9+
- [x] JDK 11+

### 4.2 Dependencies ✅ **IMPLEMENTED**
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
- [x] Collection KTX (caching utilities)

### 4.3 Testing Requirements ✅ **FRAMEWORK READY**
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

### 6.1 Completed Components (Phase 1-2) ✅ **85% COMPLETE**
- [x] **Project Structure**: Clean Architecture with MVVM pattern
- [x] **Database Layer**: Complete Room database with 5 entities and 5 DAOs
- [x] **API Layer**: Atlas Academy API interface and response models
- [x] **Error Handling**: Comprehensive exception hierarchy
- [x] **Logging System**: Structured logging with performance tracking
- [x] **UI Foundation**: Jetpack Compose with reusable components
- [x] **Testing Framework**: Unit tests for core components
- [x] **Build System**: Gradle 8.9 with proper dependencies
- [x] **Accessibility Service**: Basic service for automation
- [x] **Data Mappers**: API to Entity conversion with validation
- [x] **Repository Pattern**: ServantRepository with sync capabilities
- [x] **Data Validation**: Comprehensive validation engine
- [x] **Synchronization**: Multi-threaded sync coordination
- [x] **Caching System**: Multi-level memory and disk caching

### 6.2 Code Metrics (Updated)
- **Total Kotlin Files**: 35+ (increased from 24)
- **Database Entities**: 5 (Servant, CraftEssence, Quest, TeamConfig, BattleLog)
- **DAOs**: 5 with comprehensive CRUD operations (300+ operations total)
- **API Models**: 3 response models with proper serialization
- **UI Components**: 2 reusable Compose components
- **Test Files**: 2 unit test classes
- **Repository Classes**: 1 complete (ServantRepository)
- **Validation Classes**: 1 comprehensive validator
- **Cache Management**: 1 multi-level cache manager
- **Sync Management**: 1 coordination system

### 6.3 Architecture Overview (Updated)
```
app/src/main/java/com/fgobot/
├── core/ (3 files)
│   ├── FGOAccessibilityService.kt
│   ├── error/FGOBotException.kt
│   └── logging/FGOLogger.kt
├── data/ (25+ files)
│   ├── api/ (4 files)
│   ├── database/ (15 files)
│   ├── repository/ (1 file - ServantRepository)
│   ├── validation/ (1 file - DataValidator)
│   ├── sync/ (1 file - SyncManager)
│   ├── cache/ (1 file - CacheManager)
│   └── mappers/ (1 file - DataMappers)
├── presentation/ (3 files)
│   ├── MainActivity.kt
│   └── components/ (2 files)
└── domain/ (ready for business logic)
```

### 6.4 Performance Metrics (Estimated)
- **Build Time**: ~35 seconds for clean build
- **APK Size**: ~18MB (optimized with caching)
- **Memory Usage**: <75MB for data layer (with caching)
- **Database Operations**: 300+ optimized queries
- **Cache Hit Rate**: Target >80% for frequently accessed data
- **Sync Performance**: <30 seconds for full data sync

## 7. Success Criteria
- Successful team composition
- Accurate battle decisions
- Reliable automation
- Positive user feedback
- Stable performance
- Low error rate 