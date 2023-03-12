package org.kingdoms.peacetreaties.terms

import org.kingdoms.constants.namespace.Namespace
import org.kingdoms.locale.KingdomsLang
import org.kingdoms.locale.SimpleMessenger
import org.kingdoms.locale.messenger.DefaultedMessenger
import org.kingdoms.locale.messenger.LanguageEntryMessenger
import org.kingdoms.locale.messenger.Messenger
import org.kingdoms.locale.provider.MessageBuilder
import org.kingdoms.managers.chat.ChatInputManager
import org.kingdoms.peacetreaties.managers.StandardPeaceTreatyEditor
import org.kingdoms.utils.MathUtils
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

open class StandardTermProvider(
    private val namespace: Namespace,
    private inline val constructor: () -> Term,
    private val requiresData: Boolean = false,
    private val longTerm: Boolean = false,
) : TermProvider {
    constructor(namespace: Namespace, constructor: () -> Term) : this(namespace, constructor, false, false)

    override fun requiresData(options: TermGroupingOptions): Boolean {
        return requiresData && options.getConfigOf(this)?.getBoolean("amount") ?: true
    }

    override fun isLongTerm(): Boolean = longTerm

    override fun construct(): Term = constructor()

    override fun prompt(options: TermGroupingOptions, editor: StandardPeaceTreatyEditor): CompletionStage<Term> =
        if (requiresData) throw UnsupportedOperationException() else CompletableFuture.completedFuture(construct())

    override fun getNamespace(): Namespace = namespace

    companion object {
        @JvmStatic
        fun standardAmountPrompt(
            provider: TermProvider, options: TermGroupingOptions, editor: StandardPeaceTreatyEditor,
            handler: (Number) -> Term?
        ): CompletionStage<Term> {
            val config = options.getConfigOf(provider)
            val ctx = editor.peaceTreaty.placeholderContextProvider

            if (!provider.requiresData(options)) {
                val amount = MathUtils.eval(config.getMathExpression("amount"), ctx)
                return CompletableFuture.completedFuture(handler(amount))
            }

            fun lang(x: String, default: Messenger): Messenger {
                return DefaultedMessenger(
                    LanguageEntryMessenger("terms", provider.namespace.key.replace('_', '_'), x),
                    default
                )
            }


            val min: Double? = config.getMathExpression("min").nullIfDefault()?.let { MathUtils.eval(it, ctx) }
            val max: Double? = config.getMathExpression("max").nullIfDefault()?.let { MathUtils.eval(it, ctx) }
            val messenger = SimpleMessenger(editor.player, MessageBuilder().inheritPlaceholders(ctx))

            messenger.settings.raw("min", min).raw("max", max)
            messenger.sendMessage(lang("ENTER", KingdomsLang.VALUE_ENTER))

            return ChatInputManager.awaitInput(editor.player) { input: String ->
                val money: Double = try {
                    input.toDouble()
                } catch (ex: NumberFormatException) {
                    KingdomsLang.INVALID_NUMBER.sendError(editor.player, "arg", input)
                    return@awaitInput null
                }

                if (min != null && money < min) {
                    messenger.sendError(lang("MIN", KingdomsLang.VALUE_MIN))
                    return@awaitInput null
                }
                if (max != null && money > max) {
                    messenger.sendError(lang("MAX", KingdomsLang.VALUE_MAX))
                    return@awaitInput null
                }

                handler(money)
            }
        }
    }
}