package com.subramanya.artha.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.subramanya.artha.ArthaApplication
import com.subramanya.artha.domain.recurring.RecurringFireEngine
import java.util.concurrent.TimeUnit

/**
 * WorkManager periodic worker that fires all active recurring rules whose
 * [nextRunDate] is in the past. Runs daily; WorkManager may defer by a flex
 * window but the rule checks the exact timestamp rather than relying on exact
 * scheduling precision.
 *
 * Fire logic:
 *  - Queries [com.subramanya.artha.data.repository.RecurringRuleRepository.dueBy]
 *    for rules with nextRunDate <= now.
 *  - For each rule, [RecurringFireEngine.fire] materialises a transaction and
 *    computes the next nextRunDate.
 *  - Saves the transaction (source=RECURRING) and updates the rule unconditionally
 *    (both autoConfirm=true and false save the transaction; the user can review/
 *    delete from the Ledger).
 *  - Rules whose template can't be decoded (legacy plain-text) are skipped —
 *    the user must re-save them through the form to set a structured template.
 *
 * Scheduling: [schedule] is called once on application start. WorkManager uses
 * KEEP so an already-scheduled job isn't rescheduled on every launch.
 */
class RecurringFireWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ArthaApplication
        val recurringRepo = app.recurringRuleRepository
        val txnRepo = app.transactionRepository
        val now = System.currentTimeMillis()

        val dueRules = recurringRepo.dueBy(now)
        for (rule in dueRules) {
            val fireResult = RecurringFireEngine.fire(rule, now) ?: continue
            txnRepo.insertTransaction(fireResult.transaction)
            recurringRepo.upsert(
                rule.copy(
                    nextRunDate = fireResult.nextRunDate,
                    lastRunDate = now,
                ),
            )
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "recurring_fire"

        /**
         * Enqueues a daily periodic job. Safe to call on every app launch —
         * [ExistingPeriodicWorkPolicy.KEEP] is a no-op if the job is already scheduled.
         * The 15-minute flex window lets WorkManager batch with other work to save battery.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecurringFireWorker>(1, TimeUnit.DAYS, 15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
