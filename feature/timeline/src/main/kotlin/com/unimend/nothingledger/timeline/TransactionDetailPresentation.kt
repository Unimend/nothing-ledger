package com.unimend.nothingledger.timeline

import com.unimend.nothingledger.domain.AccountRepository
import com.unimend.nothingledger.domain.CategoryRepository
import com.unimend.nothingledger.domain.TransactionRepository
import com.unimend.nothingledger.model.Account
import com.unimend.nothingledger.model.Category
import com.unimend.nothingledger.model.Transaction
import com.unimend.nothingledger.model.TransactionId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart

public enum class MissingDetailReference {
    SOURCE_ACCOUNT,
    DESTINATION_ACCOUNT,
    CATEGORY,
    PARENT_CATEGORY,
}

/** Read-only presentation data; mutations remain in the transaction domain system. */
public data class TransactionDetailPresentation(
    public val transaction: Transaction,
    public val sourceAccount: Account?,
    public val destinationAccount: Account?,
    public val category: Category?,
    public val categoryPath: List<String>,
    public val missingReferences: Set<MissingDetailReference>,
) {
    public val sourceAccountName: String? get() = sourceAccount?.name
    public val destinationAccountName: String? get() = destinationAccount?.name
    public val categoryName: String? get() = category?.name
    public val categoryLabel: String? get() = categoryPath.takeIf(List<String>::isNotEmpty)?.joinToString(" / ")
    public val referencesComplete: Boolean get() = missingReferences.isEmpty()
}

public sealed interface TransactionDetailState {
    public data class Loading(public val id: TransactionId) : TransactionDetailState
    public data class Ready(public val detail: TransactionDetailPresentation) : TransactionDetailState
    public data class Missing(public val id: TransactionId) : TransactionDetailState
    public data class Failure(public val id: TransactionId, public val message: String) : TransactionDetailState
}

public class ObserveTransactionDetail(
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
) {
    public operator fun invoke(id: TransactionId): Flow<TransactionDetailState> =
        combine(
            transactions.observeActive(),
            accounts.observeAll(),
            categories.observeAll(),
        ) { transactionList, accountList, categoryList ->
            val transaction = transactionList.find { it.id == id }
                ?: return@combine TransactionDetailState.Missing(id)
            TransactionDetailState.Ready(
                buildTransactionDetailPresentation(transaction, accountList, categoryList),
            )
        }.onStart { emit(TransactionDetailState.Loading(id)) }
            .catch { error ->
                emit(TransactionDetailState.Failure(id, error.message ?: "Transaction detail unavailable"))
            }
}

public fun buildTransactionDetailPresentation(
    transaction: Transaction,
    accounts: List<Account>,
    categories: List<Category>,
): TransactionDetailPresentation {
    val accountsById = accounts.associateBy(Account::id)
    val categoriesById = categories.associateBy(Category::id)
    val source = transaction.sourceAccountId?.let(accountsById::get)
    val destination = transaction.destinationAccountId?.let(accountsById::get)
    val category = transaction.categoryId?.let(categoriesById::get)
    val parent = category?.parentId?.let(categoriesById::get)
    val missing = buildSet {
        if (transaction.sourceAccountId != null && source == null) add(MissingDetailReference.SOURCE_ACCOUNT)
        if (transaction.destinationAccountId != null && destination == null) add(MissingDetailReference.DESTINATION_ACCOUNT)
        if (transaction.categoryId != null && category == null) add(MissingDetailReference.CATEGORY)
        if (category?.parentId != null && parent == null) add(MissingDetailReference.PARENT_CATEGORY)
    }
    return TransactionDetailPresentation(
        transaction = transaction,
        sourceAccount = source,
        destinationAccount = destination,
        category = category,
        categoryPath = listOfNotNull(parent?.name, category?.name),
        missingReferences = missing,
    )
}
