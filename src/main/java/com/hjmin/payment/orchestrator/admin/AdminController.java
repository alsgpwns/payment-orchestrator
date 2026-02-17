package com.hjmin.payment.orchestrator.admin;

import com.hjmin.payment.orchestrator.app.PaymentOrchestratorService;
import com.hjmin.payment.orchestrator.infra.TransactionRepository;
import com.hjmin.payment.orchestrator.infra.TxEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final TransactionRepository txRepo;
    private final PaymentOrchestratorService orchestratorService;
    private final TxEventRepository eventRepo;

    public AdminController(TransactionRepository txRepo, TxEventRepository eventRepo, PaymentOrchestratorService orchestratorService) {
        this.txRepo = txRepo;
        this.eventRepo = eventRepo;
        this.orchestratorService = orchestratorService;
    }

    @GetMapping("/transactions")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        var pageable = PageRequest.of(page, 20);
        var txPage = txRepo.findAllByOrderByCreatedAtDesc(pageable);

        model.addAttribute("txPage", txPage);
        return "admin/transactions";
    }

    @GetMapping("/transactions/{txId}")
    public String detail(@PathVariable UUID txId, Model model) {
        var tx = txRepo.findById(txId).orElseThrow();
        var events = eventRepo.findByTxIdOrderByCreatedAtAsc(txId);

        model.addAttribute("tx", tx);
        model.addAttribute("events", events);
        return "admin/transaction-detail";
    }

    @GetMapping("/authorize")
    public String authorizeForm(Model model) {
        model.addAttribute("form", new AuthorizeForm("", "M001", 9000, "KRW"));
        return "admin/authorize";
    }

    @PostMapping("/authorize")
    public String authorizeSubmit(@ModelAttribute("form") AuthorizeForm form, Model model) {
        String idemKey = form.idempotencyKey();
        if (idemKey == null || idemKey.isBlank()) {
            idemKey = "admin-" + UUID.randomUUID();
        }

        var tx = orchestratorService.authorize(idemKey, form.merchantId(), form.amount(), form.currency());

        model.addAttribute("tx", tx);
        model.addAttribute("events", eventRepo.findByTxIdOrderByCreatedAtAsc(tx.getId()));
        model.addAttribute("usedIdempotencyKey", idemKey); // 화면에 찍고 싶으면
        return "admin/authorize-result";
    }


    public record AuthorizeForm(String idempotencyKey, String merchantId, long amount, String currency) {}

}
