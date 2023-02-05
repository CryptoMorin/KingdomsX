package org.kingdoms.bedrock

import org.geysermc.cumulus.form.CustomForm
import org.geysermc.cumulus.form.Form
import org.geysermc.floodgate.api.FloodgateApi
import java.util.*


class FormHandler {
    companion object {
        @JvmStatic
        fun clone(oldBuilder: CustomForm.Builder): CustomForm.Builder {
            val oldForm = oldBuilder.build()
            return CustomForm.builder().apply {
                title(oldForm.title())
                for (component in oldForm.content()) component(component!!)
            }
        }

        @JvmStatic
        fun sendForm(player: UUID, form: Form) {
            FloodgateApi.getInstance().getPlayer(player).sendForm(form)
        }
    }
}