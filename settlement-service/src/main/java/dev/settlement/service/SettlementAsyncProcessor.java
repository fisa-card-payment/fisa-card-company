package dev.settlement.service;

import dev.settlement.client.VanBatchResultNotifier;
import dev.settlement.dto.PayoutOutcome;
import dev.settlement.dto.ReconcileOutcome;
import dev.settlement.dto.VanSseNotifyResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * CSV 스테이징 이후 원장 대사·입금·SSE까지 비동기로 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementAsyncProcessor {

    private final LedgerReconciliationService ledgerReconciliationService;
    private final SettlementPayoutService settlementPayoutService;
    private final VanBatchResultNotifier vanBatchResultNotifier;

    @Async
    public void continueAfterStaging(long fileId, String batchDate) {
        try {
            ReconcileOutcome reconcile = ledgerReconciliationService.reconcile(fileId);
            PayoutOutcome payout = reconcile.matched()
                    ? settlementPayoutService.settleComparedFile(fileId)
                    : PayoutOutcome.skipped("원장 대사 불일치로 입금 생략");

            VanSseNotifyResult van = vanBatchResultNotifier.notifyBatchOutcome(batchDate, reconcile, payout);
            log.info("[정산·비동기] fileId={} compare={} settlement={} vanNotify={}",
                    fileId, reconcile.fileStatus(), payout.settlementStatus(), van.code());
        } catch (Exception e) {
            log.error("[정산·비동기] fileId={} 실패: {}", fileId, e.getMessage(), e);
            if (batchDate != null && !batchDate.isBlank()) {
                vanBatchResultNotifier.notifyBatchFailure(batchDate, e.getMessage());
            }
        }
    }
}
