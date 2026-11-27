package br.com.gabriel.gastocerto.ui

import androidx.compose.runtime.Composable
import br.com.gabriel.gastocerto.domain.model.Entry
import br.com.gabriel.gastocerto.domain.model.Expense
import br.com.gabriel.gastocerto.domain.model.Income
import br.com.gabriel.gastocerto.domain.model.IncomeType
import br.com.gabriel.gastocerto.domain.model.Installment
import br.com.gabriel.gastocerto.domain.model.InvestmentAccount
import gastocerto.shared.generated.resources.Res
import gastocerto.shared.generated.resources.entry_contribution_title
import gastocerto.shared.generated.resources.entry_sale_title
import org.jetbrains.compose.resources.stringResource

/**
 * How an entry is named on screen, in the user's language: the planned contribution and the investment
 * sale (RN11) store only the brokerage name, and installments get their `k/N` label (RN02).
 */
@Composable
fun Entry.title(): String = when {
    this is Installment -> "$description ${label()}"
    this is Expense && paymentMethod is InvestmentAccount && originId == paymentMethod.id ->
        stringResource(Res.string.entry_contribution_title, description)
    this is Income && type == IncomeType.INVESTMENT_SALE -> stringResource(Res.string.entry_sale_title, description)
    else -> description
}
