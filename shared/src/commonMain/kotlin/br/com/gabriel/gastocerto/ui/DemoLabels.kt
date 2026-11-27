package br.com.gabriel.gastocerto.ui

import br.com.gabriel.gastocerto.usecase.DemoLabels
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.demo_user
import gastocerto.shared.generated.resources.demo_spouse
import gastocerto.shared.generated.resources.demo_father
import gastocerto.shared.generated.resources.demo_spouse_card
import gastocerto.shared.generated.resources.demo_power
import gastocerto.shared.generated.resources.demo_water
import gastocerto.shared.generated.resources.demo_internet
import gastocerto.shared.generated.resources.demo_salary
import gastocerto.shared.generated.resources.demo_groceries
import gastocerto.shared.generated.resources.demo_pharmacy
import gastocerto.shared.generated.resources.demo_restaurant
import gastocerto.shared.generated.resources.demo_clothes
import gastocerto.shared.generated.resources.demo_app_subscription
import gastocerto.shared.generated.resources.demo_television
import gastocerto.shared.generated.resources.demo_notebook
import org.jetbrains.compose.resources.getString

/** The demo data texts in the device language (string resources en / pt-BR). */
suspend fun demoLabels(): DemoLabels = DemoLabels(
    user = getString(Res.string.demo_user),
    spouse = getString(Res.string.demo_spouse),
    father = getString(Res.string.demo_father),
    spouseCard = getString(Res.string.demo_spouse_card),
    power = getString(Res.string.demo_power),
    water = getString(Res.string.demo_water),
    internet = getString(Res.string.demo_internet),
    salary = getString(Res.string.demo_salary),
    groceries = getString(Res.string.demo_groceries),
    pharmacy = getString(Res.string.demo_pharmacy),
    restaurant = getString(Res.string.demo_restaurant),
    clothes = getString(Res.string.demo_clothes),
    appSubscription = getString(Res.string.demo_app_subscription),
    television = getString(Res.string.demo_television),
    notebook = getString(Res.string.demo_notebook),
)
