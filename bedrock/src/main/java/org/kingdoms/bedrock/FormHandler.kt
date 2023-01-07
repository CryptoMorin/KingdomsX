package org.kingdoms.bedrock

import org.geysermc.cumulus.component.*
import org.geysermc.cumulus.component.util.ComponentType
import org.geysermc.cumulus.form.CustomForm
import org.geysermc.cumulus.form.Form
import org.geysermc.floodgate.api.FloodgateApi
import org.kingdoms.utils.config.ConfigSection
import java.util.*


class FormHandler {
    fun parse(config: ConfigSection): CustomForm.Builder {
        val form = CustomForm.builder()

        form.title(config.getString("title").orEmpty())

        val options = config.getSection("options")
        for ((key, value) in options.sections) {
            val optional = value.getBoolean("optional")

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

            // https://github.com/GeyserMC/Cumulus/blob/5ab85db38ed0705ccf6a406c24f090d571ec09a5/src/main/java/org/geysermc/cumulus/CustomForm.java#L82-L84
            // I still don't know what the fuck shouldAdd mean
            // I have no clue what the hell optional components are. Built in if statements?
            if (optional) form.optionalComponent(component, true)
            else form.component(component)
        }

        return form
    }

    fun clone(oldBuilder: CustomForm.Builder): CustomForm.Builder {
        val oldForm = oldBuilder.build()
        return CustomForm.builder().apply {
            title(oldForm.title())
            for (component in oldForm.content()) component(component!!)
        }
    }

    fun sendForm(player: UUID, form: Form) {
        FloodgateApi.getInstance().getPlayer(player).sendForm(form)
    }
}