#!/bin/bash

# Script to rename Roboto font files to lowercase with underscores
# Run this from the project root directory

cd app/src/main/res/font

echo "🔄 Renaming Roboto fonts to lowercase..."

# Rename main Roboto fonts
mv Roboto-Black.ttf roboto_black.ttf 2>/dev/null
mv Roboto-BlackItalic.ttf roboto_black_italic.ttf 2>/dev/null
mv Roboto-Bold.ttf roboto_bold.ttf 2>/dev/null
mv Roboto-BoldItalic.ttf roboto_bold_italic.ttf 2>/dev/null
mv Roboto-ExtraBold.ttf roboto_extrabold.ttf 2>/dev/null
mv Roboto-ExtraBoldItalic.ttf roboto_extrabold_italic.ttf 2>/dev/null
mv Roboto-ExtraLight.ttf roboto_extralight.ttf 2>/dev/null
mv Roboto-ExtraLightItalic.ttf roboto_extralight_italic.ttf 2>/dev/null
mv Roboto-Italic.ttf roboto_italic.ttf 2>/dev/null
mv Roboto-Light.ttf roboto_light.ttf 2>/dev/null
mv Roboto-LightItalic.ttf roboto_light_italic.ttf 2>/dev/null
mv Roboto-Medium.ttf roboto_medium.ttf 2>/dev/null
mv Roboto-MediumItalic.ttf roboto_medium_italic.ttf 2>/dev/null
mv Roboto-Regular.ttf roboto_regular.ttf 2>/dev/null
mv Roboto-SemiBold.ttf roboto_semibold.ttf 2>/dev/null
mv Roboto-SemiBoldItalic.ttf roboto_semibold_italic.ttf 2>/dev/null
mv Roboto-Thin.ttf roboto_thin.ttf 2>/dev/null
mv Roboto-ThinItalic.ttf roboto_thin_italic.ttf 2>/dev/null

# Rename Condensed variants
mv Roboto_Condensed-Black.ttf roboto_condensed_black.ttf 2>/dev/null
mv Roboto_Condensed-BlackItalic.ttf roboto_condensed_black_italic.ttf 2>/dev/null
mv Roboto_Condensed-Bold.ttf roboto_condensed_bold.ttf 2>/dev/null
mv Roboto_Condensed-BoldItalic.ttf roboto_condensed_bold_italic.ttf 2>/dev/null
mv Roboto_Condensed-ExtraBold.ttf roboto_condensed_extrabold.ttf 2>/dev/null
mv Roboto_Condensed-ExtraBoldItalic.ttf roboto_condensed_extrabold_italic.ttf 2>/dev/null
mv Roboto_Condensed-ExtraLight.ttf roboto_condensed_extralight.ttf 2>/dev/null
mv Roboto_Condensed-ExtraLightItalic.ttf roboto_condensed_extralight_italic.ttf 2>/dev/null
mv Roboto_Condensed-Italic.ttf roboto_condensed_italic.ttf 2>/dev/null
mv Roboto_Condensed-Light.ttf roboto_condensed_light.ttf 2>/dev/null
mv Roboto_Condensed-LightItalic.ttf roboto_condensed_light_italic.ttf 2>/dev/null
mv Roboto_Condensed-Medium.ttf roboto_condensed_medium.ttf 2>/dev/null
mv Roboto_Condensed-MediumItalic.ttf roboto_condensed_medium_italic.ttf 2>/dev/null
mv Roboto_Condensed-Regular.ttf roboto_condensed_regular.ttf 2>/dev/null
mv Roboto_Condensed-SemiBold.ttf roboto_condensed_semibold.ttf 2>/dev/null
mv Roboto_Condensed-SemiBoldItalic.ttf roboto_condensed_semibold_italic.ttf 2>/dev/null
mv Roboto_Condensed-Thin.ttf roboto_condensed_thin.ttf 2>/dev/null
mv Roboto_Condensed-ThinItalic.ttf roboto_condensed_thin_italic.ttf 2>/dev/null

# Rename Semi-Condensed variants
mv Roboto_SemiCondensed-Black.ttf roboto_semicondensed_black.ttf 2>/dev/null
mv Roboto_SemiCondensed-BlackItalic.ttf roboto_semicondensed_black_italic.ttf 2>/dev/null
mv Roboto_SemiCondensed-Bold.ttf roboto_semicondensed_bold.ttf 2>/dev/null
mv Roboto_SemiCondensed-BoldItalic.ttf roboto_semicondensed_bold_italic.ttf 2>/dev/null
mv Roboto_SemiCondensed-ExtraBold.ttf roboto_semicondensed_extrabold.ttf 2>/dev/null
mv Roboto_SemiCondensed-ExtraBoldItalic.ttf roboto_semicondensed_extrabold_italic.ttf 2>/dev/null
mv Roboto_SemiCondensed-ExtraLight.ttf roboto_semicondensed_extralight.ttf 2>/dev/null
mv Roboto_SemiCondensed-ExtraLightItalic.ttf roboto_semicondensed_extralight_italic.ttf 2>/dev/null
mv Roboto_SemiCondensed-Italic.ttf roboto_semicondensed_italic.ttf 2>/dev/null
mv Roboto_SemiCondensed-Light.ttf roboto_semicondensed_light.ttf 2>/dev/null
mv Roboto_SemiCondensed-LightItalic.ttf roboto_semicondensed_light_italic.ttf 2>/dev/null
mv Roboto_SemiCondensed-Medium.ttf roboto_semicondensed_medium.ttf 2>/dev/null
mv Roboto_SemiCondensed-MediumItalic.ttf roboto_semicondensed_medium_italic.ttf 2>/dev/null
mv Roboto_SemiCondensed-Regular.ttf roboto_semicondensed_regular.ttf 2>/dev/null
mv Roboto_SemiCondensed-SemiBold.ttf roboto_semicondensed_semibold.ttf 2>/dev/null
mv Roboto_SemiCondensed-SemiBoldItalic.ttf roboto_semicondensed_semibold_italic.ttf 2>/dev/null
mv Roboto_SemiCondensed-Thin.ttf roboto_semicondensed_thin.ttf 2>/dev/null
mv Roboto_SemiCondensed-ThinItalic.ttf roboto_semicondensed_thin_italic.ttf 2>/dev/null

echo "✅ Font renaming complete!"
echo "📝 All Roboto fonts are now lowercase with underscores"
