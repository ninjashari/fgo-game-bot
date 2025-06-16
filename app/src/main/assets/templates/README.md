# FGO Bot Template Asset Library

This directory contains template images used for computer vision recognition in the FGO Bot automation system. The templates are organized by category for efficient loading and management.

## Directory Structure

```
templates/
├── ui_elements/
│   ├── buttons/
│   │   ├── attack_button.png
│   │   ├── skill_buttons/
│   │   │   ├── skill_1.png
│   │   │   ├── skill_2.png
│   │   │   └── skill_3.png
│   │   ├── menu_buttons/
│   │   │   ├── home_button.png
│   │   │   ├── back_button.png
│   │   │   └── settings_button.png
│   │   └── confirmation_buttons/
│   │       ├── ok_button.png
│   │       ├── cancel_button.png
│   │       └── yes_button.png
│   ├── screens/
│   │   ├── battle_screen/
│   │   │   ├── command_selection.png
│   │   │   ├── skill_selection.png
│   │   │   └── np_selection.png
│   │   ├── formation_screen/
│   │   │   ├── servant_selection.png
│   │   │   └── support_selection.png
│   │   └── quest_selection/
│   │       ├── quest_list.png
│   │       └── difficulty_selection.png
│   └── menus/
│       ├── main_menu.png
│       ├── battle_menu.png
│       └── options_menu.png
├── servants/
│   ├── portraits/
│   │   ├── saber/
│   │   ├── archer/
│   │   ├── lancer/
│   │   ├── rider/
│   │   ├── caster/
│   │   ├── assassin/
│   │   └── berserker/
│   ├── class_icons/
│   │   ├── saber_icon.png
│   │   ├── archer_icon.png
│   │   └── [other_class_icons].png
│   └── ascension_icons/
│       ├── ascension_1.png
│       ├── ascension_2.png
│       └── ascension_3.png
├── cards/
│   ├── command_cards/
│   │   ├── buster_card.png
│   │   ├── arts_card.png
│   │   └── quick_card.png
│   ├── servant_cards/
│   │   ├── card_frame.png
│   │   └── card_border.png
│   └── ce_cards/
│       ├── ce_frame.png
│       └── ce_border.png
├── enemies/
│   ├── portraits/
│   │   ├── enemy_1.png
│   │   └── enemy_2.png
│   └── class_icons/
│       ├── enemy_saber.png
│       └── enemy_archer.png
├── craft_essences/
│   ├── thumbnails/
│   │   ├── ce_1.png
│   │   └── ce_2.png
│   └── effects/
│       ├── effect_1.png
│       └── effect_2.png
└── status/
    ├── hp_bars/
    │   ├── hp_full.png
    │   ├── hp_half.png
    │   └── hp_low.png
    ├── np_gauges/
    │   ├── np_0.png
    │   ├── np_50.png
    │   └── np_100.png
    └── buff_icons/
        ├── attack_up.png
        ├── defense_up.png
        └── np_charge.png
```

## Template Requirements

### Image Specifications
- **Format**: PNG with transparency support
- **Resolution**: Variable based on category
  - UI Elements: 64x64 to 200x200 pixels
  - Servant Portraits: 128x128 to 256x256 pixels
  - Cards: 100x150 to 200x300 pixels
  - Status Icons: 32x32 to 64x64 pixels
- **Color Depth**: 24-bit RGB or 32-bit RGBA
- **Compression**: Optimized PNG compression

### Quality Standards
- **Clarity**: High contrast, sharp edges
- **Background**: Transparent or consistent background
- **Lighting**: Consistent lighting conditions
- **Cropping**: Precise cropping with minimal padding
- **Uniqueness**: Distinctive features for reliable matching

### Confidence Thresholds
Different template categories use different confidence thresholds:
- **UI Elements**: 0.75 (general interface elements)
- **Servants**: 0.80 (servant recognition)
- **Cards**: 0.82 (command card detection)
- **Critical Elements**: 0.85 (important buttons, confirmations)

## Template Naming Convention

Templates should follow this naming convention:
- Use lowercase with underscores: `attack_button.png`
- Include descriptive names: `saber_artoria_portrait.png`
- Add resolution suffix if multiple sizes: `skill_button_64x64.png`
- Use version numbers for updates: `attack_button_v2.png`

## Template Creation Guidelines

### Capturing Templates
1. Use consistent game settings (resolution, UI scale)
2. Capture during stable lighting conditions
3. Avoid motion blur or compression artifacts
4. Include minimal surrounding context
5. Ensure templates are representative of typical usage

### Processing Templates
1. Crop precisely to the target element
2. Remove unnecessary background elements
3. Optimize file size while maintaining quality
4. Test template matching reliability
5. Validate across different screen resolutions

### Testing Templates
1. Test with TemplateMatchingEngine
2. Verify confidence scores meet thresholds
3. Test across multiple screenshots
4. Check for false positives/negatives
5. Validate multi-scale matching performance

## Performance Considerations

### Template Optimization
- **Size**: Keep templates as small as possible while maintaining uniqueness
- **Quantity**: Limit templates to essential elements only
- **Caching**: Frequently used templates are cached automatically
- **Loading**: Templates are loaded on-demand to minimize memory usage

### Memory Management
- Template cache limited to 100 templates (configurable)
- LRU eviction policy for cache management
- Automatic cleanup of unused templates
- Memory usage monitoring and optimization

## Integration with Code

### Loading Templates
```kotlin
// Load template using TemplateAssetManager
val template = templateAssetManager.getTemplate("attack_button")

// Use with TemplateMatchingEngine
val result = templateMatchingEngine.matchTemplate(screenshot, template)
```

### Template Categories
Templates are automatically categorized based on directory structure:
- `ui_elements/*` → UI element templates (confidence: 0.75)
- `servants/*` → Servant recognition (confidence: 0.80)
- `cards/*` → Card detection (confidence: 0.82)
- `status/*` → Status indicators (confidence: 0.78)

### Custom Confidence Thresholds
```kotlin
// Override default confidence for specific templates
val result = templateMatchingEngine.matchTemplate(
    screenshot, 
    template, 
    confidence = 0.90 // Custom threshold
)
```

## Maintenance

### Regular Updates
- Update templates when game UI changes
- Add new templates for new content
- Remove obsolete templates
- Optimize template performance

### Quality Assurance
- Regular testing of template matching accuracy
- Performance benchmarking
- Memory usage monitoring
- False positive/negative analysis

### Version Control
- Track template changes in version control
- Document template updates and reasons
- Maintain backward compatibility when possible
- Test template changes thoroughly

## Contributing

When adding new templates:
1. Follow the directory structure and naming conventions
2. Ensure templates meet quality standards
3. Test template matching performance
4. Update this documentation if needed
5. Submit templates with appropriate metadata

For questions or issues with templates, please refer to the main project documentation or create an issue in the project repository. 