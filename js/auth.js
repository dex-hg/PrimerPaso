"use strict";

const setAuthStatus = (form, message, isError = false) => {
    const statusElement = form.querySelector(".auth-form-status");

    if (!statusElement) {
        return;
    }

    statusElement.textContent = message;
    statusElement.classList.toggle("is-error", isError);
};

const updatePasswordConfirmation = (confirmationField) => {
    const passwordSelector = confirmationField.dataset.confirmPassword;
    const passwordField = passwordSelector ? document.querySelector(passwordSelector) : null;
    const passwordsMatch = !confirmationField.value || confirmationField.value === passwordField?.value;

    confirmationField.setCustomValidity(passwordsMatch ? "" : "Las contraseñas no coinciden.");
};

const initializePasswordConfirmations = () => {
    const confirmationFields = document.querySelectorAll("[data-confirm-password]");

    confirmationFields.forEach((confirmationField) => {
        const passwordSelector = confirmationField.dataset.confirmPassword;
        const passwordField = passwordSelector ? document.querySelector(passwordSelector) : null;
        const updateConfirmation = () => updatePasswordConfirmation(confirmationField);

        confirmationField.addEventListener("input", updateConfirmation);
        passwordField?.addEventListener("input", updateConfirmation);
    });
};

const validateStep = (form, stepPanel) => {
    const confirmationFields = stepPanel.querySelectorAll("[data-confirm-password]");
    const fields = [...stepPanel.querySelectorAll("input, select, textarea")];

    confirmationFields.forEach(updatePasswordConfirmation);
    form.classList.add("was-validated");

    const invalidField = fields.find((field) => !field.checkValidity());

    if (invalidField) {
        invalidField.reportValidity();
        return false;
    }

    return true;
};

const showRegistrationStep = (form, stepNumber) => {
    const stepPanels = form.querySelectorAll("[data-step-panel]");
    const stepIndicators = document.querySelectorAll("[data-step-indicator]");

    stepPanels.forEach((stepPanel) => {
        const panelStep = Number(stepPanel.dataset.stepPanel);
        stepPanel.hidden = panelStep !== stepNumber;
    });

    stepIndicators.forEach((stepIndicator) => {
        const indicatorStep = Number(stepIndicator.dataset.stepIndicator);
        const isActive = indicatorStep === stepNumber;

        stepIndicator.classList.toggle("is-active", isActive);
        stepIndicator.classList.toggle("is-complete", indicatorStep < stepNumber);

        if (isActive) {
            stepIndicator.setAttribute("aria-current", "step");
        } else {
            stepIndicator.removeAttribute("aria-current");
        }
    });

    form.dataset.currentStep = String(stepNumber);
    form.classList.remove("was-validated");
    document.querySelector(".registration-heading")?.scrollIntoView({ behavior: "smooth", block: "start" });
};

const initializeMultiStepForms = () => {
    const multiStepForms = document.querySelectorAll("[data-multi-step]");

    multiStepForms.forEach((form) => {
        const stepPanels = form.querySelectorAll("[data-step-panel]");
        const totalSteps = stepPanels.length;

        form.dataset.currentStep = "1";

        form.querySelectorAll("[data-next-step]").forEach((nextButton) => {
            nextButton.addEventListener("click", () => {
                const currentStep = Number(form.dataset.currentStep || "1");
                const currentPanel = form.querySelector(`[data-step-panel="${currentStep}"]`);

                if (!currentPanel || !validateStep(form, currentPanel)) {
                    setAuthStatus(form, "Revisa los campos indicados.", true);
                    return;
                }

                setAuthStatus(form, "");
                showRegistrationStep(form, Math.min(currentStep + 1, totalSteps));
            });
        });

        form.querySelectorAll("[data-previous-step]").forEach((previousButton) => {
            previousButton.addEventListener("click", () => {
                const currentStep = Number(form.dataset.currentStep || "1");
                setAuthStatus(form, "");
                showRegistrationStep(form, Math.max(currentStep - 1, 1));
            });
        });

        form.addEventListener("submit", (event) => {
            event.preventDefault();

            const currentStep = Number(form.dataset.currentStep || "1");
            const currentPanel = form.querySelector(`[data-step-panel="${currentStep}"]`);

            if (!currentPanel || !validateStep(form, currentPanel)) {
                setAuthStatus(form, "Revisa los campos indicados.", true);
                return;
            }

            setAuthStatus(form, "Formulario completado. La conexión con el sistema se añadirá en una siguiente etapa.");
        });
    });
};

const initializeDemoForms = () => {
    const demoForms = document.querySelectorAll("[data-demo-form]");

    demoForms.forEach((form) => {
        form.addEventListener("submit", (event) => {
            event.preventDefault();
            form.classList.add("was-validated");

            if (!form.checkValidity()) {
                setAuthStatus(form, "Revisa los campos indicados.", true);
                return;
            }

            setAuthStatus(form, "Formulario listo. El inicio de sesión se conectará en una siguiente etapa.");
        });
    });
};

const setAuthCurrentYear = () => {
    document.querySelectorAll("[data-current-year]").forEach((yearElement) => {
        yearElement.textContent = String(new Date().getFullYear());
    });
};

document.addEventListener("DOMContentLoaded", () => {
    initializePasswordConfirmations();
    initializeMultiStepForms();
    initializeDemoForms();
    setAuthCurrentYear();
});
