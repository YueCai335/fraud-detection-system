package com.yuecai.fraud.web;

import com.yuecai.fraud.modelclient.ModelServiceException;
import com.yuecai.fraud.prediction.PredictionService;
import com.yuecai.fraud.prediction.TransactionRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/** Single-transaction form (predict.jsp). Replaces PredictServlet + FraudSoapClient. */
@Controller
public class PredictPageController {

    private final PredictionService predictionService;

    public PredictPageController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @GetMapping("/predict")
    public String form(Model model) {
        model.addAttribute("activePage", "predict");
        return "predict";
    }

    @PostMapping("/predict")
    public String predict(@Valid @ModelAttribute("tx") TransactionRequest tx,
                          BindingResult binding,
                          Principal principal,
                          Model model) {
        model.addAttribute("activePage", "predict");
        if (binding.hasErrors()) {
            FieldError fe = binding.getFieldErrors().get(0);
            model.addAttribute("error", fe.getField() + " " + fe.getDefaultMessage());
            return "predict";
        }
        try {
            model.addAttribute("result", predictionService.predict(principal.getName(), tx));
        } catch (ModelServiceException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "predict";
    }
}
