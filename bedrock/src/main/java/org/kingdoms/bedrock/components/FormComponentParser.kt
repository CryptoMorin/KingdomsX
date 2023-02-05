package org.kingdoms.bedrock.components

import org.geysermc.cumulus.component.*
import org.geysermc.cumulus.component.util.ComponentType
import org.kingdoms.utils.config.ConfigSection

class FormComponentParser {
    companion object {
        @JvmStatic
        fun parse(value: ConfigSection) {
            val typeStr = value.getString("component-type")
            val type = if (typeStr == null) ComponentType.LABEL else try {
                // Their ComponentType.fromName() is inconsistent.
                ComponentType.valueOf(typeStr.uppercase().replace(' ', '_').replace('-', '_'))
            } catch (x: IllegalArgumentException) {
                ComponentType.LABEL
            }

            val component: Component = when (type) {
                ComponentType.LABEL -> LabelComponent.of(value.getString("text").orEmpty())

                ComponentType.INPUT -> InputComponent.of(
                    value.getString("text").orEmpty(),
                    value.getString("placeholder").orEmpty(),
                    value.getString("default-text").orEmpty()
                )

                ComponentType.DROPDOWN -> DropdownComponent.of(
                    value.getString("text").orEmpty(),
                    value.getStringList("options"),
                    value.getInt("default-option")
                )

                ComponentType.SLIDER -> SliderComponent.of(
                    value.getString("text").orEmpty(),
                    value.getFloat("min"), value.getFloat("max"), value.getFloat("step"), value.getFloat("defaultValue")
                )

                ComponentType.STEP_SLIDER -> StepSliderComponent.of(
                    value.getString("text").orEmpty(),
                    value.getStringList("steps"), value.getInt("default-step")
                )

                ComponentType.TOGGLE -> ToggleComponent.of(value.getString("text").orEmpty(), value.getBoolean("default-value"))

                else -> LabelComponent.of("Unknown component type $type")
            }
        }
    }
}