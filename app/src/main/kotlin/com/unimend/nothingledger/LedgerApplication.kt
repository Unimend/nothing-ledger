package com.unimend.nothingledger

import android.app.Application
import com.unimend.nothingledger.database.LedgerDatabaseFactory
import com.unimend.nothingledger.database.LedgerDatabaseHandle

public class LedgerApplication : Application() {
    public val database: LedgerDatabaseHandle by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LedgerDatabaseFactory.create(this)
    }
}

