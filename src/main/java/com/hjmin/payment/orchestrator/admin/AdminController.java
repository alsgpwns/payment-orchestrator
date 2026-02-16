package com.hjmin.payment.orchestrator.admin;

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
    private final TxEventRepository eventRepo;

    public AdminController(TransactionRepository txRepo, TxEventRepository eventRepo) {
        this.txRepo = txRepo;
        this.eventRepo = eventRepo;
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
}
