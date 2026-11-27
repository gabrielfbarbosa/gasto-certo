package br.com.gabriel.gastocerto.ui

import androidx.compose.runtime.Composable
import br.com.gabriel.gastocerto.domain.model.CheckingAccount
import br.com.gabriel.gastocerto.domain.model.CreditCard
import br.com.gabriel.gastocerto.domain.model.ForeignCurrencyWallet
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import br.com.gabriel.gastocerto.domain.model.PaymentMethod
import br.com.gabriel.gastocerto.domain.model.Role
import br.com.gabriel.gastocerto.ui.registry.PaymentMethodType
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.kind_checking_account
import gastocerto.shared.generated.resources.kind_credit_card
import gastocerto.shared.generated.resources.kind_foreign_wallet
import gastocerto.shared.generated.resources.kind_investment
import gastocerto.shared.generated.resources.role_holder
import gastocerto.shared.generated.resources.role_third_party
import gastocerto.shared.generated.resources.role_user
import org.jetbrains.compose.resources.stringResource

// User-facing names of the domain enums and kinds (string resources en / pt-BR).

@Composable
fun Role.label(): String = stringResource(
    when (this) {
        Role.USER -> Res.string.role_user
        Role.HOLDER -> Res.string.role_holder
        Role.THIRD_PARTY -> Res.string.role_third_party
    },
)

@Composable
fun PaymentMethodType.label(): String = stringResource(
    when (this) {
        PaymentMethodType.CREDIT_CARD -> Res.string.kind_credit_card
        PaymentMethodType.CHECKING_ACCOUNT -> Res.string.kind_checking_account
        PaymentMethodType.FOREIGN_CURRENCY_WALLET -> Res.string.kind_foreign_wallet
        PaymentMethodType.INVESTMENT_ACCOUNT -> Res.string.kind_investment
    },
)

@Composable
fun PaymentMethod.kindLabel(): String = when (this) {
    is CreditCard -> PaymentMethodType.CREDIT_CARD
    is CheckingAccount -> PaymentMethodType.CHECKING_ACCOUNT
    is ForeignCurrencyWallet -> PaymentMethodType.FOREIGN_CURRENCY_WALLET
    is InvestmentAccount -> PaymentMethodType.INVESTMENT_ACCOUNT
}.label()
