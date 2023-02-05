package org.kingdoms.bedrock.components

import org.geysermc.cumulus.component.*
import org.geysermc.cumulus.component.util.ComponentType
import org.kingdoms.locale.compiler.MessageObject
import org.kingdoms.locale.provider.MessageBuilder

abstract class FormComponentObject {
    abstract fun getType(): ComponentType
    abstract fun build(settings: MessageBuilder): Component
}

private class LabelObject(private val text: MessageObject) : FormComponentObject() {
    override fun getType(): ComponentType = ComponentType.LABEL

    override fun build(settings: MessageBuilder): Component =
        LabelComponent.of(text.buildPlain(settings))
}

private class InputObject(
    private val text: MessageObject,
    private val placeholder: MessageObject,
    private val defaultText: MessageObject
) : FormComponentObject() {
    override fun getType(): ComponentType = ComponentType.INPUT

    override fun build(settings: MessageBuilder): Component =
        InputComponent.of(
            text.buildPlain(settings),
            placeholder.buildPlain(settings),
            defaultText.buildPlain(settings)
        )
}

private class DropdownObject(
    private val text: MessageObject,
    private val options: List<MessageObject>,
    private val defaultOption: Int
) : FormComponentObject() {
    override fun getType(): ComponentType = ComponentType.DROPDOWN

    override fun build(settings: MessageBuilder): Component =
        DropdownComponent.of(
            text.buildPlain(settings),
            options.map { x -> x.buildPlain(settings) },
            defaultOption,
        )
}

private class SliderObject(
    private val text: MessageObject,
    private val min: Float, private val max: Float, private val step: Float,
    private val defaultValue: Float
) : FormComponentObject() {
    override fun getType(): ComponentType = ComponentType.SLIDER

    override fun build(settings: MessageBuilder): Component =
        SliderComponent.of(
            text.buildPlain(settings),
            min, max, step, defaultValue
        )
}

private class StepSliderObject(
    private val text: MessageObject,
    private val steps: List<MessageObject>,
    private val defaultStep: Int
) : FormComponentObject() {
    override fun getType(): ComponentType = ComponentType.STEP_SLIDER

    override fun build(settings: MessageBuilder): Component =
        StepSliderComponent.of(
            text.buildPlain(settings),
            steps.map { x -> x.buildPlain(settings) },
            defaultStep
        )
}

private class ToggleObject(
    private val text: MessageObject,
    private val defaultValue: Boolean
) : FormComponentObject() {
    override fun getType(): ComponentType = ComponentType.TOGGLE

    override fun build(settings: MessageBuilder): Component =
        ToggleComponent.of(text.buildPlain(settings), defaultValue)
}