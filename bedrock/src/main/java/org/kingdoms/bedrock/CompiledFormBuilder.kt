package org.kingdoms.bedrock

import org.geysermc.cumulus.form.CustomForm
import org.kingdoms.bedrock.components.FormComponentObject
import org.kingdoms.locale.compiler.MessageObject
import org.kingdoms.locale.provider.MessageBuilder

class CompiledFormBuilder(
    private val title: MessageObject,
    private val options: List<FormComponentObject>
) {

    fun build(settings: MessageBuilder): CustomForm {
        val form = CustomForm.builder()
        form.title(title.buildPlain(settings))

        for (option in options) {
            // https://github.com/GeyserMC/Cumulus/blob/5ab85db38ed0705ccf6a406c24f090d571ec09a5/src/main/java/org/geysermc/cumulus/CustomForm.java#L82-L84
            // I still don't know what the fuck shouldAdd mean in form.optionalComponent(component, true)
            // I have no clue what the hell optional components are. Built in if statements?
            form.component(option.build(settings))
        }

        return form.build()
    }
}